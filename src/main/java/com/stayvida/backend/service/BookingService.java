package com.stayvida.backend.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
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
        String hotelId = room.get("hotel_ID").toString();
        String roomId = ((String) room.get("room_ID")).toString();
        Integer roomNo = ((Number) room.get("room_NO")).intValue();
        BigDecimal price = (BigDecimal) room.get("price");

        // 2️⃣ Fetch charges
        Map<String, BigDecimal> charges = fetchCharges();
        // 3️⃣ Price calculation
        BigDecimal platformCharges = charges.get("platform_charges"); // platform charges
        BigDecimal taxRate = charges.get("tax"); // tax rate
        platformCharges = platformCharges.add(platformCharges.multiply(taxRate));// platform charges with tax

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

        response.setHotelId(hotelId);
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

    // owner dashbord version
    @Transactional
    public LockRoomResponse lockRoomod(Integer ownerId, LockRoomRequest request) {

        String hotelId = gethotelId(ownerId);

        // 1️⃣ Validate the specific room
        String roomSql = """
                SELECT r.room_ID, r.room_NO, r.hotel_ID, r.room_Type, r.price
                FROM rooms r
                WHERE r.hotel_ID = ?
                  AND r.room_ID = ?
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
                """;

        List<Map<String, Object>> rooms = jdbcTemplate.queryForList(
                roomSql,
                hotelId,
                request.getRoomId(),
                request.getCheckIn(),
                request.getCheckOut());

        if (rooms.isEmpty()) {
            throw new RuntimeException("Room is not available or already locked");
        }

        Map<String, Object> room = rooms.get(0);

        String roomId = room.get("room_ID").toString();
        Integer roomNo = ((Number) room.get("room_NO")).intValue();
        BigDecimal price = (BigDecimal) room.get("price");

        // 2️⃣ Fetch charges
        Map<String, BigDecimal> charges = fetchCharges();

        BigDecimal platformCharges = charges.get("platform_charges");
        BigDecimal taxRate = charges.get("tax");
        platformCharges = platformCharges.add(platformCharges.multiply(taxRate));

        // 3️⃣ Lock expiry (3 minutes, IST)
        ZoneId istZone = ZoneId.of("Asia/Kolkata");
        LocalDateTime lockExpiry = ZonedDateTime.now(istZone).plusMinutes(3).toLocalDateTime();

        // 4️⃣ Insert lock
        String lockSql = """
                INSERT INTO room_locks (room_id, lock_expiry)
                VALUES (?, ?)
                """;

        jdbcTemplate.update(lockSql, roomId, lockExpiry);

        // 5️⃣ Build response
        LockRoomResponse response = new LockRoomResponse();
        response.setHotelId(hotelId);
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
        BigDecimal platformCharges = charges.get("platform_charges"); // platform charges
        BigDecimal taxRate = charges.get("tax"); // tax rate
        platformCharges = platformCharges.add(platformCharges.multiply(taxRate))
                .setScale(2, RoundingMode.HALF_UP);// platform charges with tax
        BigDecimal commissionRate = charges.get("commission"); // commission rate
        LocalDate checkInDate = LocalDate.parse(request.getCheckIn());
        LocalDate checkOutDate = LocalDate.parse(request.getCheckOut());

        long totalDays = ChronoUnit.DAYS.between(checkInDate, checkOutDate);

        if (totalDays <= 0) {
            throw new RuntimeException("Invalid check-in/check-out dates");
        }
        roomPrice = roomPrice.multiply(new BigDecimal(totalDays));
        BigDecimal commissionAmount = roomPrice.multiply(commissionRate); // commission amount
        commissionAmount = commissionAmount.add(commissionAmount.multiply(taxRate)); // commission amount with tax
        BigDecimal totalAmount = roomPrice.add(platformCharges);
        BigDecimal totalAmount_ADV = roomPrice.multiply(charges.get("Advance")).add(platformCharges);
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
                        createdAt, updatedAt,commision_Amount
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(),?)
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
                taxRate, // in table the column name is still tax_amount
                request.getPaymentType(),
                request.getName(),
                request.getCountryCode(),
                request.getPhoneNo(),
                commissionAmount);
        System.out.println(commissionAmount);
        // 5️⃣ Remove room lock
        jdbcTemplate.update(
                "DELETE FROM room_locks WHERE room_id = ?",
                request.getLockRoomId());

        // 6️⃣ Response
        BookingResponse response = new BookingResponse();
        response.setBookingId(bookingId);
        response.setBookingStatus("Pending");
        response.setPaymentStatus("Pending");
        response.setDuration(totalDays);
        response.setRoomPrice(roomPrice);
        response.setPlatformCharges(platformCharges);
        response.setTaxAmount(taxRate);
        response.setAdvanceRate(charges.get("Advance"));
        response.setTotalAmount_ADV(totalAmount_ADV);
        response.setTotalAmount(totalAmount);
        response.setCheckIn(request.getCheckIn());
        response.setCheckOut(request.getCheckOut());
        response.setCreatedAt(LocalDateTime.now());

        return response;
    }

    // owner dashbord booking service
    @Transactional
    public BookingResponse createBookingod(String Email, BookingRequest request) {
        BigDecimal roomPrice = null;
        String hotelId = null;
        String roomId = null;
        Integer roomNo = null;
        Integer userId = validateUserbyEmail(Email);
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
        BigDecimal platformCharges = charges.get("platform_charges"); // platform charges
        BigDecimal taxRate = charges.get("tax"); // tax rate
        platformCharges = platformCharges.add(platformCharges.multiply(taxRate))
                .setScale(2, RoundingMode.HALF_UP);// platform charges with tax
        BigDecimal commissionRate = charges.get("commission"); // commission rate
        LocalDate checkInDate = LocalDate.parse(request.getCheckIn());
        LocalDate checkOutDate = LocalDate.parse(request.getCheckOut());

        long totalDays = ChronoUnit.DAYS.between(checkInDate, checkOutDate);

        if (totalDays <= 0) {
            throw new RuntimeException("Invalid check-in/check-out dates");
        }
        roomPrice = roomPrice.multiply(new BigDecimal(totalDays));
        BigDecimal commissionAmount = roomPrice.multiply(commissionRate); // commission amount
        commissionAmount = commissionAmount.add(commissionAmount.multiply(taxRate)); // commission amount with tax
        BigDecimal totalAmount = roomPrice.add(platformCharges);
        BigDecimal totalAmount_ADV = roomPrice.multiply(charges.get("Advance")).add(platformCharges);
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
                        createdAt, updatedAt,commision_Amount
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(),?)
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
                "CheckIn",
                roomPrice,
                platformCharges,
                taxRate, // in table the column name is still tax_amount
                request.getPaymentType(),
                request.getName(),
                request.getCountryCode(),
                request.getPhoneNo(),
                commissionAmount);
        System.out.println(commissionAmount);
        // 5️⃣ Remove room lock
        jdbcTemplate.update(
                "DELETE FROM room_locks WHERE room_id = ?",
                request.getLockRoomId());

        // 6️⃣ Response
        BookingResponse response = new BookingResponse();
        response.setBookingId(bookingId);
        response.setBookingStatus("Pending");
        response.setPaymentStatus("Pending");
        response.setDuration(totalDays);
        response.setRoomPrice(roomPrice);
        response.setPlatformCharges(platformCharges);
        response.setTaxAmount(taxRate);
        response.setAdvanceRate(charges.get("Advance"));
        response.setTotalAmount_ADV(totalAmount_ADV);
        response.setTotalAmount(totalAmount);
        response.setCheckIn(request.getCheckIn());
        response.setCheckOut(request.getCheckOut());
        response.setCreatedAt(LocalDateTime.now());

        return response;
    }

    public String gethotelId(Integer ownerId) {
        String sql = "SELECT hotel_ID FROM hotels WHERE owner_ID = ?";
        return jdbcTemplate.queryForObject(sql, String.class, ownerId);
    }

    public Integer validateUserbyEmail(String email) {

        try {

            String sql = "SELECT user_ID FROM users WHERE email = ?";
            return jdbcTemplate.queryForObject(sql, Integer.class, email);

        } catch (Exception e) {

            throw new RuntimeException(
                    "User not found, register first with this email : " + email + " Or check your email is right");
        }

    }

}
