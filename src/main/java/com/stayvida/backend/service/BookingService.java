package com.stayvida.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stayvida.backend.dto.BookingRequest;
import com.stayvida.backend.dto.BookingResponse;
import com.stayvida.backend.dto.LockRoomRequest;
import com.stayvida.backend.dto.LockRoomResponse;

@Service
public class BookingService {

    private final JdbcTemplate jdbcTemplate;

    public BookingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public LockRoomResponse lockRoom(LockRoomRequest request) {

        // 1️⃣ Find available room
        String roomSql = """
                    SELECT r.room_ID, r.room_NO, r.hotel_ID, r.room_Type, r.price
                    FROM rooms r
                    WHERE r.hotel_ID = ?
                      AND r.room_Type = ?
                      AND r.isEnable = true
                      AND NOT EXISTS (
                          SELECT 1 FROM bookings b
                          WHERE b.room_ID = r.room_ID
                            AND b.booking_Status NOT IN ('Cancelled', 'CheckedOut')
                            AND NOT (
                                ? >= b.checkOut OR ? <= b.checkIn
                            )
                      )
                      AND NOT EXISTS (
                          SELECT 1 FROM room_locks rl
                          WHERE rl.room_id = r.room_ID
                      )
                    ORDER BY r.room_NO ASC
                    LIMIT 1
                """;

        List<Map<String, Object>> rooms = jdbcTemplate.queryForList(
                roomSql,
                request.getHotelId(),
                request.getRoomType(),
                request.getCheckIn(),
                request.getCheckOut());

        if (rooms.isEmpty()) {
            throw new RuntimeException("No available room found");
        }

        Map<String, Object> room = rooms.get(0);

        String roomId = ((String) room.get("room_ID")).toString();
        Integer roomNo = ((Number) room.get("room_NO")).intValue();
        BigDecimal price = (BigDecimal) room.get("price");

        // 2️⃣ Fetch charges
        Map<String, BigDecimal> charges = fetchCharges();

        BigDecimal platformCharges = charges.get("platform_charges");
        BigDecimal taxRate = charges.get("tax");

        // 3️⃣ Price calculation
        // BigDecimal taxableAmount = price.add(platformCharges);
        // BigDecimal taxAmount = taxableAmount.multiply(taxRate);
        // BigDecimal totalPrice = taxableAmount.add(taxAmount);

        // 4️⃣ Lock expiry (3 minutes)
        // IST zone
        ZoneId istZone = ZoneId.of("Asia/Kolkata");

        // expiry in IST
        LocalDateTime lockExpiry = ZonedDateTime.now(istZone).plusMinutes(3).toLocalDateTime();

        // 5️⃣ Insert lock
        String lockSql = """
                    INSERT INTO room_locks (room_id, lock_expiry)
                    VALUES (?, ?)
                """;

        jdbcTemplate.update(lockSql, roomId, lockExpiry);

        // 6️⃣ Return response
        LockRoomResponse response = new LockRoomResponse();

        response.setHotelId(request.getHotelId());
        response.setRoomId(roomId);
        response.setRoomNo(roomNo);
        response.setRoomType(request.getRoomType());
        response.setCheckIn(request.getCheckIn());
        response.setCheckOut(request.getCheckOut());
        response.setRoomPrice(price);
        response.setPlatformCharges(platformCharges);
        response.setTaxRate(taxRate);
        response.setLockExpiry(lockExpiry);

        return response;

    }

    private Map<String, BigDecimal> fetchCharges() {

        String sql = "SELECT `type`, `value` FROM amount";

        return jdbcTemplate.query(sql, rs -> {
            Map<String, BigDecimal> map = new HashMap<>();
            while (rs.next()) {
                map.put(rs.getString("type"), rs.getBigDecimal("value"));
            }
            return map;
        });
    }

    // booking service
    @Transactional
    public BookingResponse createBooking(Integer userId, BookingRequest request) {
        BigDecimal roomPrice = null;
        String hotelId = null;
        String roomId = null;
        Integer roomNo = null;
        try {
            // 1️⃣ Validate room lock
            String lockSql = """
                        SELECT rl.room_id, rl.lock_expiry,
                               r.hotel_ID, r.room_NO, r.price
                        FROM room_locks rl
                        JOIN rooms r ON r.room_ID = rl.room_id
                        WHERE rl.room_id = ?
                    """;

            Map<String, Object> lock = jdbcTemplate.queryForMap(
                    lockSql, request.getLockRoomId());

            LocalDateTime lockExpiry = ((java.sql.Timestamp) lock.get("lock_expiry")).toLocalDateTime();

            if (lockExpiry.isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Room lock expired");
            }
            roomPrice = (BigDecimal) lock.get("price");
            hotelId = (String) lock.get("hotel_ID");
            roomId = (String) lock.get("room_id");
            roomNo = (Integer) lock.get("room_NO");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("room lock not found");
        }
        // 2️⃣ Fetch charges
        Map<String, BigDecimal> charges = fetchCharges();
        BigDecimal platformCharges = charges.get("platform_charges");
        BigDecimal taxAmount = request.getTaxAmount();

        // BigDecimal taxAmount = roomPrice.add(platformCharges).multiply(taxRate);

        // 3️⃣ Generate Booking ID
        String bookingId = "B-" + System.currentTimeMillis();

        // 4️⃣ Insert booking
        String insertBooking = """
                    INSERT INTO bookings (
                        booking_ID, user_ID, hotel_ID, room_ID, room_NO,
                        adults, children, checkIn , checkOut,
                        payment_Status, booking_Status,
                        totalAmount, platformFee, tax_amount,
                        payment_type,
                        name, countru_code, phone_no,
                        createdAt, updatedAt
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                """;

        jdbcTemplate.update(insertBooking,
                bookingId,
                userId,
                hotelId,
                roomId,
                roomNo,
                request.getAdults(),
                request.getChildren(),
                request.getCheckIn(),
                request.getCheckOut(),
                "Pending",
                "Pending",
                roomPrice,
                platformCharges,
                taxAmount,
                request.getPaymentType(),
                request.getName(),
                request.getCountryCode(),
                request.getPhoneNo());

        // 5️⃣ Remove room lock
        jdbcTemplate.update(
                "DELETE FROM room_locks WHERE room_id = ?",
                request.getLockRoomId());

        // 6️⃣ Response
        BookingResponse response = new BookingResponse();
        response.setBookingId(bookingId);
        response.setBookingStatus("Pending");
        response.setPaymentStatus("Pending");
        response.setRoomPrice(roomPrice);
        response.setPlatformCharges(platformCharges);
        response.setTaxAmount(taxAmount);
        response.setCheckIn(request.getCheckIn());
        response.setCheckOut(request.getCheckOut());
        response.setCreatedAt(LocalDateTime.now());

        return response;
    }

}
