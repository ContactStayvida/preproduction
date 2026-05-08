package com.stayvida.backend.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import com.stayvida.backend.service.JwtUtil;
import com.stayvida.backend.service.EmailService;
import com.stayvida.backend.repository.BookingRepository;

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
import org.springframework.web.bind.annotation.RequestHeader;

import com.stayvida.backend.dto.BookingEmailDTO;
import com.stayvida.backend.dto.BookingRequest;
import com.stayvida.backend.dto.BookingResponse;
import com.stayvida.backend.dto.LockRoomRequest;
import com.stayvida.backend.dto.LockRoomResponse;
import com.stayvida.backend.exception.BookingExceptions.RoomLockException;

@Service
public class BookingService {

    private final JdbcTemplate jdbcTemplate;
    private final WalletService walletService;
    private final EmailService emailService;
    private final BookingRepository bookingRepository;
    private final JwtUtil jwtUtil;

    public BookingService(JdbcTemplate jdbcTemplate, WalletService walletService, EmailService emailService,
            BookingRepository bookingRepository, JwtUtil jwtUtil) {

        this.jdbcTemplate = jdbcTemplate;
        this.walletService = walletService;
        this.emailService = emailService;
        this.bookingRepository = bookingRepository;
        this.jwtUtil = jwtUtil;
    }

    public Boolean validateCode(String code) {

        String sql = """
                    SELECT EXISTS (
                        SELECT 1
                        FROM executive
                        WHERE referral_code = ?
                        AND is_enable = TRUE
                    )
                """;

        Integer result = jdbcTemplate.queryForObject(sql, Integer.class, code);

        return result != null && result == 1;
    }

    @Transactional
    public LockRoomResponse lockRoom(LockRoomRequest request) {

        // 1️⃣ Find available room
        String roomSql = """
                    SELECT r.room_ID, r.room_NO, r.hotel_ID, r.room_Type, r.price
                    FROM rooms r
                    WHERE r.hotel_ID = ?
                      AND r.room_Type = ?
                      AND r.price = ?
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
                request.getPrice(),
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
    public LockRoomResponse lockRoomod(LockRoomRequest request) {

        String roomSql = """
                    SELECT
                        r.hotel_ID,
                        r.room_ID,
                        r.room_NO,
                        r.room_Type,
                        r.price
                    FROM rooms r
                    WHERE r.room_ID = ?
                      AND r.isEnable = true
                      AND NOT EXISTS (
                          SELECT 1
                          FROM bookings b
                          WHERE b.room_ID = r.room_ID
                            AND b.booking_Status NOT IN ('Cancelled','CheckedOut')
                            AND NOT (? >= b.checkOut OR ? <= b.checkIn)
                      )
                      AND NOT EXISTS (
                          SELECT 1
                          FROM room_locks rl
                          WHERE rl.room_id = r.room_ID
                            AND rl.lock_expiry > NOW()
                      )
                """;

        List<Map<String, Object>> rooms = jdbcTemplate.queryForList(
                roomSql,
                request.getRoomId(),
                request.getCheckOut(),
                request.getCheckIn());

        if (rooms.isEmpty()) {
            throw new RuntimeException("Room is not available");
        }

        Map<String, Object> room = rooms.get(0);

        String hotelId = room.get("hotel_ID").toString();
        String roomId = room.get("room_ID").toString();
        Integer roomNo = ((Number) room.get("room_NO")).intValue();
        String roomType = room.get("room_Type").toString();
        BigDecimal price = (BigDecimal) room.get("price");

        // lock expiry (3 minutes IST)
        ZoneId istZone = ZoneId.of("Asia/Kolkata");
        LocalDateTime lockExpiry = ZonedDateTime.now(istZone).plusMinutes(3).toLocalDateTime();

        String lockSql = """
                    INSERT INTO room_locks (room_id, lock_expiry)
                    VALUES (?, ?)
                """;

        jdbcTemplate.update(lockSql, roomId, lockExpiry);

        LockRoomResponse response = new LockRoomResponse();
        response.setHotelId(hotelId);
        response.setRoomId(roomId);
        response.setRoomNo(roomNo);
        response.setRoomType(roomType);
        response.setCheckIn(request.getCheckIn());
        response.setCheckOut(request.getCheckOut());
        response.setRoomPrice(price);
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

    public void createExecutivePayment(
            String bookingId,
            BigDecimal paymentAmount,
            String paymentStatus,
            String referralCode) {

        // 0. If referral code is null → just skip
        if (referralCode == null || referralCode.isBlank()) {
            return;
        }

        // 1. Fetch user_ID
        String fetchUserSql = "SELECT user_ID FROM executive WHERE referral_code = ?";

        List<Integer> userList = jdbcTemplate.query(
                fetchUserSql,
                (rs, rowNum) -> rs.getInt("user_ID"),
                referralCode);

        if (userList.isEmpty()) {
            return; // or log warning, but DON'T break booking
        }

        int userId = userList.get(0);

        // 2. Prevent duplicate booking entry
        String checkSql = "SELECT COUNT(*) FROM executive_referral_payments WHERE booking_ID = ?";

        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, bookingId);

        if (count != null && count > 0) {
            return;
        }

        // 3. Insert
        String insertSql = """
                    INSERT INTO executive_referral_payments
                    (booking_ID, user_ID, referral_code, payment_amount, payment_status)
                    VALUES (?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(
                insertSql,
                bookingId,
                userId,
                referralCode,
                paymentAmount,
                paymentStatus);
    }

    // booking service
    @Transactional
    public BookingResponse createBooking(Integer userId, BookingRequest request) {

        // @RequestHeader("Authorization") String authHeader
        BigDecimal roomPrice = null;
        String hotelId = null;
        String roomId = null;
        Integer roomNo = null;
        String paymentType = request.getPaymentType();
        String referralCode = request.getReferralCode();

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

        BigDecimal commissionAmount = null;
        BigDecimal totalAmount = null;
        roomPrice = roomPrice.multiply(new BigDecimal(totalDays));
        BigDecimal totalAmount_ADV = roomPrice.multiply(charges.get("Advance")).add(platformCharges);
        // 3️⃣ Generate Booking ID
        String bookingId = "B-" + System.currentTimeMillis();

        if (paymentType.equals("Advance")) {
            commissionAmount = roomPrice.multiply(commissionRate); // commission amount
            commissionAmount = commissionAmount.add(commissionAmount.multiply(taxRate)); // commission amount with tax
            totalAmount = roomPrice.add(platformCharges);
            totalAmount_ADV = roomPrice.multiply(charges.get("Advance")).add(platformCharges);
        } else if (paymentType.equals("OnArrival")) {
            commissionAmount = BigDecimal.ZERO; // commission amount
            totalAmount = roomPrice.add(platformCharges);
            totalAmount_ADV = BigDecimal.ZERO;
        }
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
                "Confirmed",
                roomPrice,
                platformCharges,
                taxRate, // in table the column name is still tax_amount
                request.getPaymentType(),
                request.getName(),
                request.getCountryCode(),
                request.getPhoneNo(),
                commissionAmount);
        // System.out.println(commissionAmount);
        BigDecimal amt = roomPrice.multiply(new BigDecimal("0.05"));
        if (referralCode != null && !referralCode.isEmpty()) {
            createExecutivePayment(bookingId, amt, "PENDING", referralCode);
        }

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
        response.setPaymentType(paymentType);
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
        String paymentType = request.getPaymentType();
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
            throw new RoomLockException("room lock not found");
        }
        // 2️⃣ Fetch charges
        Map<String, BigDecimal> charges = fetchCharges();
        LocalDate checkInDate = LocalDate.parse(request.getCheckIn());
        LocalDate checkOutDate = LocalDate.parse(request.getCheckOut());

        long totalDays = ChronoUnit.DAYS.between(checkInDate, checkOutDate);

        if (totalDays <= 0) {
            throw new RuntimeException("Invalid check-in/check-out dates");
        }
        roomPrice = roomPrice.multiply(new BigDecimal(totalDays));
        BigDecimal commissionAmount = BigDecimal.ZERO; // commission amount
        BigDecimal platformCharges = BigDecimal.ZERO;
        BigDecimal taxRate = BigDecimal.ZERO;
        BigDecimal totalAmount_ADV = BigDecimal.ZERO;

        // 3️⃣ Generate Booking ID
        String bookingId = "OFFLINE-B-" + System.currentTimeMillis();

        // 4️⃣ Insert booking
        String insertBooking = """
                    INSERT INTO bookings (
                        booking_ID, user_ID, hotel_ID, room_ID, room_NO,
                        adults, children, checkIn , checkOut,
                        payment_Status, booking_Status,
                        totalAmount, platformFee, tax_amount,
                        payment_type,
                        name, countru_code, phone_no,
                        createdAt, updatedAt,commision_Amount,payment_amount
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(),?,?)
                """;

        String PaymentStatus = null;
        BigDecimal payment_amount = null;
        if (paymentType.equals("Advance")) {
            totalAmount_ADV = roomPrice.multiply(charges.get("Advance"));
            PaymentStatus = "Pending";
            payment_amount = totalAmount_ADV;
        } else if (paymentType.equals("OnArrival")) {
            totalAmount_ADV = BigDecimal.ZERO;
            PaymentStatus = "Completed";
            payment_amount = roomPrice;
        }

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
                PaymentStatus,
                "CheckIn",
                roomPrice,
                platformCharges, // ZERO
                taxRate, // in table the column name is still tax_amount
                request.getPaymentType(),
                request.getName(),
                request.getCountryCode(),
                request.getPhoneNo(),
                commissionAmount,
                payment_amount);// ZERO
        // 5️⃣ Remove room lock
        jdbcTemplate.update(
                "DELETE FROM room_locks WHERE room_id = ?",
                request.getLockRoomId());

        // 6️⃣ Response
        BookingResponse response = new BookingResponse();
        response.setBookingId(bookingId);
        response.setBookingStatus("CheckIn");
        response.setPaymentStatus(PaymentStatus);
        response.setDuration(totalDays);
        response.setRoomPrice(roomPrice);
        response.setPlatformCharges(platformCharges);
        response.setTaxAmount(taxRate);
        response.setAdvanceRate(charges.get("Advance"));
        response.setPaymentType(paymentType);
        response.setTotalAmount_ADV(totalAmount_ADV);
        response.setTotalAmount(roomPrice);
        response.setCheckIn(request.getCheckIn());
        response.setCheckOut(request.getCheckOut());
        response.setCreatedAt(LocalDateTime.now());

        walletService.wallet(
                hotelId,
                bookingId,
                payment_amount,
                "CR",
                "Offline Payment",
                "Offline Payment");

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
