package com.stayvida.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stayvida.backend.repository.OwnerDashboardRepository;

@Service
public class OwnerDashboardService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private OwnerDashboardRepository repo;
    @Autowired
    private WalletService walletService;

    @Autowired
    private JdbcTemplate jdbcTemplate; // ✅ REQUIRED

    public OwnerDashboardService(WalletService walletService) {
        this.walletService = walletService;
    }

    private static final Set<String> VALID_BOOKING_STATUSES = Set.of(
            "CheckIn",
            "Confirmed",
            "Cancelled",
            "CheckedOut");

    // ===============================
    // MONTHLY BOOKING METRICS
    // ===============================
    public Map<String, Object> getMonthlyBookingsForOwner(int ownerId) {

        Map<String, Object> response = new LinkedHashMap<>();

        List<Map<String, Object>> hotels = repo.getOwnerVerifiedHotels(ownerId);

        // ✅ Safety check
        if (hotels.isEmpty()) {
            response.put("totalMonthlyBookings", 0);
            response.put("bookingDifference", "0% same as last month");
            response.put("roomsOccupied", 0);
            response.put("totalGuests", 0);
            response.put("guestDifference", "0% same as last month");
            response.put("totalRevenue", 0.0);
            response.put("revenueDifference", "0% same as last month");
            response.put("hotelId", null);
            response.put("hotelName", null);
            return response;
        }

        // ✅ ONLY ONE HOTEL
        Map<String, Object> hotel = hotels.get(0);

        String hotelId = (String) hotel.get("hotel_ID");
        String hotelName = (String) hotel.get("name");

        int monthlyBookings = repo.getMonthlyBookingCount(hotelId);
        int totalGuests = repo.getTotalGuestsForMonth(hotelId);

        double totalRevenue = repo.getMonthlyRevenueForOwner(ownerId);
        double lastMonthRevenue = repo.getLastMonthRevenueForOwner(ownerId);
        int lastMonthBookings = repo.getLastMonthBookingCount(ownerId);
        int lastMonthGuests = repo.getLastMonthGuestCount(ownerId);

        String bookingChange = calculateChange(monthlyBookings, lastMonthBookings);
        String guestChange = calculateChange(totalGuests, lastMonthGuests);
        String revenueChange = calculateChange(totalRevenue, lastMonthRevenue);

        int roomsOccupiedToday = repo.getRoomsOccupiedToday(ownerId);
        int totalrooms = repo.getTotalRoomCount(ownerId);
        int AvailableRooms;
        if (roomsOccupiedToday < totalrooms) {
            AvailableRooms = totalrooms - roomsOccupiedToday;
        } else {
            AvailableRooms = 0;
        }

        // ✅ FINAL RESPONSE (exactly what you want)
        response.put("hotelId", hotelId);
        response.put("hotelName", hotelName);

        response.put("totalMonthlyBookings", monthlyBookings);
        response.put("bookingDifference", bookingChange);

        response.put("roomsOccupied", roomsOccupiedToday);
        response.put("AvailableRooms", AvailableRooms);

        response.put("totalGuests", totalGuests);
        response.put("guestDifference", guestChange);

        response.put("totalRevenue", totalRevenue);// revinue after comission and platform charges
        response.put("revenueDifference", revenueChange);
        response.put("last monthy", lastMonthRevenue);

        return response;
    }

    private String calculateChange(double current, double last) {

        if (last == 0 && current == 0)
            return "0% same as last month";

        if (last == 0)
            return "+100% increased from last month";

        double change = ((current - last) / last) * 100.0;
        String percent = String.format("%.2f", Math.abs(change));

        return change >= 0
                ? "+" + percent + "% increased from last month"
                : "-" + percent + "% decreased from last month";
    }

    // ===============================
    // FETCH ACTIVE BOOKINGS
    // ===============================
    public List<Map<String, Object>> getActiveBookingsForOwner(int ownerId) {

        String sql = """
                                               SELECT
                    b.booking_ID,
                    b.user_ID,
                    b.hotel_ID,
                    b.room_ID,
                    r.room_NO,
                    b.booking_Status,
                    b.checkIn,
                    b.checkOut,
                    b.payment_Status,
                    b.payment_amount,
                    b.totalAmount,
                    b.tax_amount,
                    b.platformFee,
                    b.is_refundable,
                    b.name,
                    b.phone_no
                FROM bookings b
                JOIN hotels h ON b.hotel_ID = h.hotel_ID
                LEFT JOIN rooms r ON r.room_ID = b.room_ID
                LEFT JOIN profile p ON b.user_ID = p.user_ID
                WHERE h.owner_ID = ?
                AND (
                    (
                        b.booking_Status IN ('Pending', 'Confirmed', 'CheckIn')
                        AND b.checkOut >= CURDATE()
                    )
                    OR b.payment_Status = 'Pending'
                )
                ORDER BY b.checkIn ASC;
                """;

        return jdbcTemplate.query(sql, new Object[] { ownerId }, (rs, rowNum) -> {

            Map<String, Object> map = new LinkedHashMap<>();

            double totalAmount = rs.getDouble("totalAmount");// room amount with tax
            double taxAmount = rs.getDouble("tax_amount");// tax rate
            double platformFee = rs.getDouble("platformFee");// platform fee with tax
            double paid = rs.getDouble("payment_amount");// amount paid by customer
            // double platformBase = totalAmount - taxAmount - platformFee;
            // double commission = platformBase * 0.20;
            // double payableAfterCut = platformBase - commission;
            double paymentLeft = totalAmount - (paid - platformFee);// left to pay
            double grossAmount = totalAmount + platformFee;// total amount to be paid by customer including tax and all

            map.put("booking_ID", rs.getString("booking_ID")); // booking id
            map.put("user_ID", rs.getInt("user_ID")); // coustomer's user id
            map.put("hotel_ID", rs.getString("hotel_ID")); // hotel's ID
            map.put("room_ID", rs.getString("room_ID"));// room's ID
            map.put("RoomNumber", rs.getInt("room_NO")); // room number
            map.put("booking_Status", rs.getString("booking_Status")); // booking status
            map.put("checkIn", rs.getDate("checkIn"));// check in date
            map.put("checkOut", rs.getDate("checkOut"));// check out date
            map.put("payment_Status", rs.getString("payment_Status"));
            map.put("name", rs.getString("name"));// customer name
            map.put("gross amount", grossAmount);// total amount to be paid by customer including tax and all
            map.put("payment left", paymentLeft);// left to pay
            return map;
        });
    }

    // admin dashboard
    public List<Map<String, Object>> getActiveBookings() {

        String sql = """
                    SELECT
                    b.booking_ID,
                    b.user_ID,
                    b.hotel_ID,
                    b.room_ID,
                    r.room_NO,
                    b.booking_Status,
                    b.checkIn,
                    b.checkOut,
                    b.payment_Status,
                    b.payment_amount,
                    b.totalAmount,
                    b.tax_amount,
                    b.platformFee,
                    b.is_refundable,
                    b.name,
                    b.phone_no
                FROM bookings b
                JOIN hotels h ON b.hotel_ID = h.hotel_ID
                LEFT JOIN rooms r ON r.room_ID = b.room_ID
                LEFT JOIN profile p ON b.user_ID = p.user_ID
                WHERE (
                    b.booking_Status IN ('Pending', 'Confirmed', 'CheckIn')
                    AND b.checkOut >= CURDATE()
                )
                or payment_Status = 'Pending'
                ORDER BY b.checkIn ASC;
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {

            Map<String, Object> map = new LinkedHashMap<>();

            double totalAmount = rs.getDouble("totalAmount");// room amount with tax
            double taxAmount = rs.getDouble("tax_amount");// tax rate
            double platformFee = rs.getDouble("platformFee");// platform fee with tax
            double paid = rs.getDouble("payment_amount");// amount paid by customer
            // double platformBase = totalAmount - taxAmount - platformFee;
            // double commission = platformBase * 0.20;
            // double payableAfterCut = platformBase - commission;
            double paymentLeft = totalAmount - (paid - platformFee);// left to pay
            double grossAmount = totalAmount + platformFee;// total amount to be paid by customer including tax and all

            map.put("booking_ID", rs.getString("booking_ID")); // booking id
            map.put("user_ID", rs.getInt("user_ID")); // coustomer's user id
            map.put("hotel_ID", rs.getString("hotel_ID")); // hotel's ID
            map.put("room_ID", rs.getString("room_ID"));// room's ID
            map.put("RoomNumber", rs.getInt("room_NO")); // room number
            map.put("booking_Status", rs.getString("booking_Status")); // booking status
            map.put("checkIn", rs.getDate("checkIn"));// check in date
            map.put("checkOut", rs.getDate("checkOut"));// check out date
            map.put("payment_Status", rs.getString("payment_Status"));
            map.put("name", rs.getString("name"));// customer name
            map.put("gross amount", grossAmount);// total amount to be paid by customer including tax and all
            map.put("payment left", paymentLeft);// left to pay
            return map;
        });
    }

    public boolean updateBookingStatus(String bookingId, String newStatus, int ownerId) {

        // Validate allowed values
        List<String> allowed = Arrays.asList("CheckIn", "Confirmed", "Cancelled", "CheckedOut");

        if (!allowed.contains(newStatus)) {
            throw new IllegalArgumentException("Invalid booking status: " + newStatus);
        }

        String sql = "UPDATE bookings b INNER JOIN hotels h ON b.hotel_ID = h.hotel_ID SET b.booking_Status = ?, b.updatedAt = NOW() WHERE b.booking_ID = ? AND h.owner_ID = ?";

        int rows = jdbcTemplate.update(sql, newStatus, bookingId, ownerId);

        if (newStatus.equals("CheckedOut")) {

            String sql3 = "SELECT * FROM bookings WHERE booking_ID = ?";
            Map<String, Object> booking = jdbcTemplate.queryForMap(sql3, bookingId);
            BigDecimal totalAmount = (BigDecimal) booking.get("totalAmount");
            BigDecimal platformFee = (BigDecimal) booking.get("platformFee");
            BigDecimal paid = (BigDecimal) booking.get("payment_amount");// amount paid by customer
            BigDecimal paymentLeft = totalAmount
                    .subtract(paid.subtract(platformFee));// left to pay
            BigDecimal completpayment = paymentLeft.add(paid);
            int userId = (int) booking.get("user_ID");
            String hotelId = (String) booking.get("hotel_ID");

            String sql2 = "UPDATE bookings SET payment_Status= 'Completed', payment_amount = ? WHERE booking_ID = ?";
            jdbcTemplate.update(sql2, completpayment, bookingId);
            String paymentId = "PAY-" + System.currentTimeMillis();
            String sql4 = "INSERT INTO payments (user_ID,payment_ID, booking_ID,amount, payment_Method,payment_Status, currency,createdAt,updatedAt) VALUES (?,?, ?, ?, 'PayAtHotel','Success', 'INR', NOW(), NOW(),)";
            jdbcTemplate.update(sql4, userId, paymentId, bookingId, paymentLeft);

            walletService.wallet(
                    hotelId,
                    bookingId,
                    paymentLeft,
                    "CR",
                    "PayAtHotel",
                    "Success");

        }

        return rows > 0;
    }

    /// admin version

    public boolean updateBookingStatus(String bookingId, String newStatus) {

        // Validate allowed values
        List<String> allowed = Arrays.asList("CheckIn", "Confirmed", "Cancelled", "CheckedOut");

        if (!allowed.contains(newStatus)) {
            throw new IllegalArgumentException("Invalid booking status: " + newStatus);
        }

        String sql = "UPDATE bookings b INNER JOIN hotels h ON b.hotel_ID = h.hotel_ID SET b.booking_Status = ?, b.updatedAt = NOW() WHERE b.booking_ID = ?";

        int rows = jdbcTemplate.update(sql, newStatus, bookingId);

        if (newStatus.equals("CheckedOut")) {

            String sql3 = "SELECT * FROM bookings WHERE booking_ID = ?";
            Map<String, Object> booking = jdbcTemplate.queryForMap(sql3, bookingId);
            BigDecimal totalAmount = (BigDecimal) booking.get("totalAmount");
            BigDecimal platformFee = (BigDecimal) booking.get("platformFee");
            BigDecimal paid = (BigDecimal) booking.get("payment_amount");// amount paid by customer
            BigDecimal paymentLeft = totalAmount
                    .subtract(paid.subtract(platformFee));// left to pay
            BigDecimal completpayment = paymentLeft.add(paid);
            int userId = (int) booking.get("user_ID");
            String hotelId = (String) booking.get("hotel_ID");

            String sql2 = "UPDATE bookings SET payment_Status= 'Completed', payment_amount = ? WHERE booking_ID = ?";
            jdbcTemplate.update(sql2, completpayment, bookingId);
            String paymentId = "PAY-" + System.currentTimeMillis();
            String sql4 = "INSERT INTO payments (user_ID,payment_ID, booking_ID,amount, payment_Method,payment_Status, currency) VALUES (?,?, ?, ?, 'PayAtHotel','Success', 'INR')";
            jdbcTemplate.update(sql4, userId, paymentId, bookingId, paymentLeft);

            walletService.wallet(
                    hotelId,
                    bookingId,
                    paymentLeft,
                    "CR",
                    "PayAtHotel",
                    "Success");
        }

        return rows > 0;

    }

    public List<Map<String, Object>> getAllBookingsForOwner(int ownerId) {

        String sql = """
                   SELECT
                    b.booking_ID,
                    b.user_ID,
                    b.hotel_ID,
                    b.room_ID,
                    r.room_NO,
                    b.booking_Status,
                    b.checkIn,
                    b.checkOut,
                    b.payment_Status,
                    b.payment_amount,
                    b.totalAmount,
                    b.tax_amount,
                    b.platformFee,
                    b.is_refundable,
                    b.name,
                    b.phone_no
                FROM bookings b
                JOIN hotels h ON b.hotel_ID = h.hotel_ID
                LEFT JOIN rooms r ON r.room_ID = b.room_ID
                WHERE h.owner_ID = ?
                ORDER BY b.checkIn DESC
                """;

        return jdbcTemplate.query(sql, new Object[] { ownerId }, (rs, rowNum) -> {

            Map<String, Object> map = new LinkedHashMap<>();

            double totalAmount = rs.getDouble("totalAmount");// room amount
            double taxAmount = rs.getDouble("tax_amount");// tax amount
            double platformFee = rs.getDouble("platformFee");// platform fee
            double paid = rs.getDouble("payment_amount");// amount paid by customer
            double paymentLeft = totalAmount - (paid - platformFee);// left to pay
            double grossAmount = totalAmount + platformFee;// total amount to be paid by customer including tax and all

            map.put("booking_ID", rs.getString("booking_ID")); // booking id
            map.put("user_ID", rs.getInt("user_ID")); // coustomer's user id
            map.put("hotel_ID", rs.getString("hotel_ID")); // hotel's ID
            map.put("room_ID", rs.getString("room_ID"));// room's ID
            map.put("RoomNumber", rs.getInt("room_NO")); // room number
            map.put("booking_Status", rs.getString("booking_Status")); // booking status
            map.put("checkIn", rs.getDate("checkIn"));// check in date
            map.put("checkOut", rs.getDate("checkOut"));// check out date
            map.put("payment_Status", rs.getString("payment_Status"));
            map.put("name", rs.getString("name"));// customer name
            map.put("payment left", paymentLeft);// left to pay
            map.put("gross amount", grossAmount);// total amount to be paid by customer including tax and all
            return map;
        });
    }

    ///
    /// ADMIN DASHBOARD
    ///

    public List<Map<String, Object>> getAllBookings() {

        String sql = """
                   SELECT
                    b.booking_ID,
                    b.user_ID,
                    b.hotel_ID,
                    b.room_ID,
                    r.room_NO,
                    b.booking_Status,
                    b.checkIn,
                    b.checkOut,
                    b.payment_Status,
                    b.payment_amount,
                    b.totalAmount,
                    b.tax_amount,
                    b.platformFee,
                    b.is_refundable,
                    b.name,
                    b.phone_no
                FROM bookings b
                JOIN hotels h ON b.hotel_ID = h.hotel_ID
                LEFT JOIN rooms r ON r.room_ID = b.room_ID
                ORDER BY b.checkIn DESC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {

            Map<String, Object> map = new LinkedHashMap<>();

            double totalAmount = rs.getDouble("totalAmount");// room amount
            double taxAmount = rs.getDouble("tax_amount");// tax amount
            double platformFee = rs.getDouble("platformFee");// platform fee
            double paid = rs.getDouble("payment_amount");// amount paid by customer
            double paymentLeft = totalAmount - (paid - platformFee);// left to pay
            double grossAmount = totalAmount + platformFee;// total amount to be paid by customer including tax and all

            map.put("booking_ID", rs.getString("booking_ID")); // booking id
            map.put("user_ID", rs.getInt("user_ID")); // coustomer's user id
            map.put("hotel_ID", rs.getString("hotel_ID")); // hotel's ID
            map.put("room_ID", rs.getString("room_ID"));// room's ID
            map.put("RoomNumber", rs.getInt("room_NO")); // room number
            map.put("booking_Status", rs.getString("booking_Status")); // booking status
            map.put("checkIn", rs.getDate("checkIn"));// check in date
            map.put("checkOut", rs.getDate("checkOut"));// check out date
            map.put("payment_Status", rs.getString("payment_Status"));
            map.put("name", rs.getString("name"));// customer name
            map.put("payment left", paymentLeft);// left to pay
            map.put("gross amount", grossAmount);// total amount to be paid by customer including tax and all
            return map;
        });
    }

    public List<Map<String, Object>> getLatestBookingsByUser(int userId) {

        String sql = """
                SELECT
                    b.booking_ID,
                    b.user_ID,
                    b.hotel_ID,
                    b.room_ID,
                    r.room_NO,
                    b.booking_Status,
                    b.checkIn,
                    b.checkOut,
                    b.payment_Status,
                    b.payment_amount,
                    b.totalAmount,
                    b.tax_amount,
                    b.platformFee,
                    b.is_refundable,
                    b.name,
                    b.phone_no
                FROM bookings b
                JOIN hotels h ON b.hotel_ID = h.hotel_ID
                LEFT JOIN rooms r ON r.room_ID = b.room_ID
                WHERE b.user_ID = ?
                ORDER BY b.createdAt DESC
                LIMIT 2
                """;

        return jdbcTemplate.query(sql, new Object[] { userId }, (rs, rowNum) -> {

            Map<String, Object> map = new LinkedHashMap<>();

            double totalAmount = rs.getDouble("totalAmount");
            double taxAmount = rs.getDouble("tax_amount");
            double platformFee = rs.getDouble("platformFee");
            double paid = rs.getDouble("payment_amount");

            double paymentLeft = totalAmount - (paid - platformFee);
            double grossAmount = totalAmount + platformFee;

            map.put("booking_ID", rs.getString("booking_ID"));
            map.put("user_ID", rs.getInt("user_ID"));
            map.put("hotel_ID", rs.getString("hotel_ID"));
            map.put("room_ID", rs.getString("room_ID"));
            map.put("RoomNumber", rs.getInt("room_NO"));
            map.put("booking_Status", rs.getString("booking_Status"));
            map.put("checkIn", rs.getDate("checkIn"));
            map.put("checkOut", rs.getDate("checkOut"));
            map.put("payment_Status", rs.getString("payment_Status"));
            map.put("name", rs.getString("name"));
            map.put("payment left", paymentLeft);
            map.put("gross amount", grossAmount);

            return map;
        });
    }

    // ===============================
    // UPCOMING RECENT 5 BOOKINGS
    // ===============================
    public List<Map<String, Object>> getUpcomingBookings(int ownerId) {

        String sql = """
                    SELECT
                        b.booking_ID,
                        b.room_ID,
                        b.room_NO,
                        b.name,
                        b.checkIn,
                        b.booking_Status
                    FROM bookings b
                    INNER JOIN hotels h ON b.hotel_ID = h.hotel_ID
                    WHERE h.owner_ID = ?
                      AND b.checkIn >= CURDATE()
                    ORDER BY b.checkIn ASC
                    LIMIT 5
                """;

        return jdbcTemplate.query(sql, new Object[] { ownerId }, (rs, rowNum) -> {
            Map<String, Object> map = new LinkedHashMap<>();

            map.put("booking_ID", rs.getString("booking_ID"));
            map.put("room_ID", rs.getString("room_ID"));
            map.put("room_NO", rs.getInt("room_NO"));
            map.put("name", rs.getString("name"));
            map.put("checkIn", rs.getDate("checkIn"));
            map.put("booking_Status", rs.getString("booking_Status"));

            return map;
        });
    }

    // ===============================
    // OPEN BOOKING DETAILS
    // ===============================
    public Map<String, Object> getBookingDetails(String bookingId, int ownerId) {

        String sql = """
                    SELECT
                        b.booking_ID,
                        b.user_ID,
                        b.hotel_ID,
                        b.room_ID,
                        b.room_NO,
                        b.booking_Status,
                        b.checkIn,
                        b.checkOut,
                        b.payment_Status,
                        b.payment_amount,
                        b.payment_type,
                        b.totalAmount,
                        b.tax_amount,
                        b.platformFee,
                        b.is_refundable,
                        b.name,
                        b.phone_no,
                        h.name AS hotel_name,
                        r.room_Type

                    FROM bookings b
                    INNER JOIN hotels h ON b.hotel_ID = h.hotel_ID
                    INNER JOIN rooms r ON b.room_ID = r.room_ID
                    WHERE b.booking_ID = ? and h.owner_ID = ?
                """;

        List<Map<String, Object>> result = jdbcTemplate.query(sql, new Object[] { bookingId, ownerId },
                (rs, rowNum) -> {

                    Map<String, Object> map = new LinkedHashMap<>();

                    double totalAmount = rs.getDouble("totalAmount");// room amount
                    double taxAmount = rs.getDouble("tax_amount");// tax rate
                    double platformFee = rs.getDouble("platformFee");// platform fee
                    double paid = rs.getDouble("payment_amount");// amount paid by customer
                    double paymentLeft = totalAmount - (paid - platformFee);// left to pay
                    double grossAmount = totalAmount + platformFee;// total amount to be paid

                    map.put("name", rs.getString("name"));// customer name
                    map.put("phone_number", rs.getString("phone_no"));// customer phone number
                    map.put("booking_ID", rs.getString("booking_ID")); // booking id
                    map.put("user_ID", rs.getInt("user_ID")); // coustomer's user id
                    map.put("hotel_ID", rs.getString("hotel_ID")); // hotel's ID
                    map.put("hotel_name", rs.getString("hotel_name")); // hotel name
                    map.put("room_ID", rs.getString("room_ID"));// room's ID
                    map.put("RoomNumber", rs.getInt("room_NO")); // room number
                    map.put("booking_Status", rs.getString("booking_Status")); // booking status
                    map.put("checkIn", rs.getDate("checkIn"));// check in date
                    map.put("checkOut", rs.getDate("checkOut"));// check out date
                    map.put("payment_Status", rs.getString("payment_Status"));
                    map.put("payment_type", rs.getString("payment_type"));
                    map.put("is_refundable", rs.getBoolean("is_refundable"));
                    map.put("tax_amount", taxAmount);// tax rate
                    map.put("platformFee", platformFee);// platform fee
                    map.put("Room Price", totalAmount);// room price
                    map.put("amount paid by customer", paid);// amount paid by customer
                    map.put("payment left to pay customer", paymentLeft);// payment left
                    map.put("gross amount to be paid by customer", grossAmount);// payment amount
                    return map;
                });

        return result.isEmpty() ? null : result.get(0);
    }

    // admin version
    public Map<String, Object> getBookingDetails(String bookingId) {

        String sql = """
                    SELECT
                        b.booking_ID,
                        b.user_ID,
                        b.hotel_ID,
                        b.room_ID,
                        b.room_NO,
                        b.booking_Status,
                        b.checkIn,
                        b.checkOut,
                        b.payment_Status,
                        b.payment_amount,
                        b.payment_type,
                        b.totalAmount,
                        b.tax_amount,
                        b.platformFee,
                        b.is_refundable,
                        b.name,
                        b.phone_no,
                        h.name AS hotel_name,
                        r.room_Type

                    FROM bookings b
                    INNER JOIN hotels h ON b.hotel_ID = h.hotel_ID
                    INNER JOIN rooms r ON b.room_ID = r.room_ID
                    WHERE b.booking_ID = ?
                """;

        List<Map<String, Object>> result = jdbcTemplate.query(sql, new Object[] { bookingId },
                (rs, rowNum) -> {

                    Map<String, Object> map = new LinkedHashMap<>();

                    double totalAmount = rs.getDouble("totalAmount");// room amount
                    double taxAmount = rs.getDouble("tax_amount");// tax rate
                    double platformFee = rs.getDouble("platformFee");// platform fee
                    double paid = rs.getDouble("payment_amount");// amount paid by customer
                    double paymentLeft = totalAmount - (paid - platformFee);// left to pay
                    double grossAmount = totalAmount + platformFee;// total amount to be paid

                    map.put("name", rs.getString("name"));// customer name
                    map.put("phone_number", rs.getString("phone_no"));// customer phone number
                    map.put("booking_ID", rs.getString("booking_ID")); // booking id
                    map.put("user_ID", rs.getInt("user_ID")); // coustomer's user id
                    map.put("hotel_ID", rs.getString("hotel_ID")); // hotel's ID
                    map.put("hotel_name", rs.getString("hotel_name")); // hotel name
                    map.put("room_ID", rs.getString("room_ID"));// room's ID
                    map.put("RoomNumber", rs.getInt("room_NO")); // room number
                    map.put("booking_Status", rs.getString("booking_Status")); // booking status
                    map.put("checkIn", rs.getDate("checkIn"));// check in date
                    map.put("checkOut", rs.getDate("checkOut"));// check out date
                    map.put("payment_Status", rs.getString("payment_Status"));
                    map.put("payment_type", rs.getString("payment_type"));
                    map.put("is_refundable", rs.getBoolean("is_refundable"));
                    map.put("tax_amount", taxAmount);// tax rate
                    map.put("platformFee", platformFee);// platform fee
                    map.put("Room Price", totalAmount);// room price
                    map.put("amount paid by customer", paid);// amount paid by customer
                    map.put("payment left to pay customer", paymentLeft);// payment left
                    map.put("gross amount to be paid by customer", grossAmount);// payment amount
                    return map;
                });

        return result.isEmpty() ? null : result.get(0);
    }

    // tempory dissabled

    // public boolean deleteRoom(int ownerId, String roomId, int hotel_ID) {
    // String sql = "DELETE r FROM rooms r INNER JOIN hotels h ON r.hotel_ID =
    // h.hotel_ID WHERE h.owner_ID = ? AND r.room_ID = ? AND r.hotel_ID = ?;";
    // return jdbcTemplate.update(sql, ownerId, roomId, hotel_ID) > 0;
    // }

    public boolean updateRoom(String roomId, int ownerId, Map<String, Object> updates) {

        if (updates == null || updates.isEmpty()) {
            throw new IllegalArgumentException("No fields provided for update");
        }

        StringBuilder sql = new StringBuilder("""
                    UPDATE rooms r
                    INNER JOIN hotels h ON r.hotel_ID = h.hotel_ID
                    SET
                """);

        List<Object> params = new ArrayList<>();

        if (updates.containsKey("room_NO")) {
            sql.append(" r.room_NO = ?,");
            params.add(updates.get("room_NO"));
        }

        if (updates.containsKey("roomType")) {
            sql.append(" r.room_Type = ?,");
            params.add(updates.get("roomType"));
        }

        if (updates.containsKey("features")) {
            sql.append(" r.features = ?,");
            params.add(updates.get("features"));
        }

        if (updates.containsKey("price")) {
            sql.append(" r.price = ?,");
            params.add(updates.get("price"));
        }

        if (updates.containsKey("maxAdults")) {
            sql.append(" r.max_adults = ?,");
            params.add(updates.get("maxAdults"));
        }

        if (updates.containsKey("maxChildren")) {
            sql.append(" r.max_children = ?,");
            params.add(updates.get("maxChildren"));
        }

        if (updates.containsKey("bedCount")) {
            sql.append(" r.bed_count = ?,");
            params.add(updates.get("bedCount"));
        }

        // Remove trailing comma
        if (sql.charAt(sql.length() - 1) == ',') {
            sql.deleteCharAt(sql.length() - 1);
        }

        // Always update timestamp
        sql.append(", r.updatedAt = NOW()");

        // Ownership check
        sql.append(" WHERE r.room_ID = ? AND h.owner_ID = ?");
        params.add(roomId);
        params.add(ownerId);

        int rows = jdbcTemplate.update(sql.toString(), params.toArray());
        return rows > 0;
    }

    /// admin version
    public boolean updateRoom(String roomId, String hotelId, Map<String, Object> updates) {

        if (updates == null || updates.isEmpty()) {
            throw new IllegalArgumentException("No fields provided for update");
        }

        StringBuilder sql = new StringBuilder("""
                    UPDATE rooms r
                    INNER JOIN hotels h ON r.hotel_ID = h.hotel_ID
                    SET
                """);

        List<Object> params = new ArrayList<>();

        if (updates.containsKey("room_NO")) {
            sql.append(" r.room_NO = ?,");
            params.add(updates.get("room_NO"));
        }

        if (updates.containsKey("roomType")) {
            sql.append(" r.room_Type = ?,");
            params.add(updates.get("roomType"));
        }

        if (updates.containsKey("features")) {
            sql.append(" r.features = ?,");
            params.add(updates.get("features"));
        }

        if (updates.containsKey("price")) {
            sql.append(" r.price = ?,");
            params.add(updates.get("price"));
        }

        if (updates.containsKey("maxAdults")) {
            sql.append(" r.max_adults = ?,");
            params.add(updates.get("maxAdults"));
        }

        if (updates.containsKey("maxChildren")) {
            sql.append(" r.max_children = ?,");
            params.add(updates.get("maxChildren"));
        }

        if (updates.containsKey("bedCount")) {
            sql.append(" r.bed_count = ?,");
            params.add(updates.get("bedCount"));
        }

        // Remove trailing comma
        if (sql.charAt(sql.length() - 1) == ',') {
            sql.deleteCharAt(sql.length() - 1);
        }

        // Always update timestamp
        sql.append(", r.updatedAt = NOW()");

        // Ownership check
        sql.append(" WHERE r.room_ID = ? AND h.hotel_ID = ?");
        params.add(roomId);
        params.add(hotelId);

        int rows = jdbcTemplate.update(sql.toString(), params.toArray());
        return rows > 0;
    }

    // room image update helper
    public Map<Integer, String> getRoomImagesBase64WithId(String roomId, int ownerId) {

        String sql = "SELECT r.images FROM rooms r INNER JOIN hotels h ON r.hotel_ID = h.hotel_ID WHERE r.room_ID = ? AND h.owner_ID = ?";

        String imagesJson = jdbcTemplate.queryForObject(
                sql,
                String.class,
                roomId,
                ownerId);

        if (imagesJson == null || imagesJson.isBlank()) {
            return Map.of();
        }

        try {
            ObjectMapper mapper = new ObjectMapper();

            List<String> images = mapper.readValue(
                    imagesJson,
                    new TypeReference<List<String>>() {
                    });

            Map<Integer, String> result = new LinkedHashMap<>();

            int index = 1;
            for (String img : images) {
                if (img != null && !img.isBlank()) {
                    result.put(index++, img);
                }
            }

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of();
        }
    }

    // admin version

    public Map<Integer, String> getRoomImagesBase64WithId(String roomId, String hotelId) {

        String sql = "SELECT r.images FROM rooms r INNER JOIN hotels h ON r.hotel_ID = h.hotel_ID WHERE r.room_ID = ? AND h.hotel_ID = ?";

        String imagesJson = jdbcTemplate.queryForObject(
                sql,
                String.class,
                roomId,
                hotelId);

        if (imagesJson == null || imagesJson.isBlank()) {
            return Map.of();
        }

        try {
            ObjectMapper mapper = new ObjectMapper();

            List<String> images = mapper.readValue(
                    imagesJson,
                    new TypeReference<List<String>>() {
                    });

            Map<Integer, String> result = new LinkedHashMap<>();

            int index = 1;
            for (String img : images) {
                if (img != null && !img.isBlank()) {
                    result.put(index++, img);
                }
            }

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of();
        }
    }

    // delete room images
    public boolean removeRoomImage(String roomId, Integer imageIndex, String base64Image, int ownerId) {
        try {
            // 1️⃣ Fetch current JSON array
            String sqlFetch = "SELECT r.images FROM rooms r INNER JOIN hotels h ON r.hotel_ID = h.hotel_ID WHERE r.room_ID = ? AND h.owner_ID = ?";
            String imagesJson = jdbcTemplate.queryForObject(sqlFetch, String.class, roomId, ownerId);

            if (imagesJson == null || imagesJson.isBlank())
                return false;

            ObjectMapper mapper = new ObjectMapper();
            List<String> images = mapper.readValue(imagesJson, new TypeReference<List<String>>() {
            });

            boolean removed = false;

            if (imageIndex != null) {
                // 1-based index
                if (imageIndex >= 1 && imageIndex <= images.size()) {
                    images.remove(imageIndex - 1);
                    removed = true;
                }
            } else if (base64Image != null) {
                removed = images.removeIf(img -> img.equals(base64Image));
            }

            if (!removed)
                return false;

            // 2️⃣ Update DB with modified JSON array
            String updatedJson = mapper.writeValueAsString(images);
            String sqlUpdate = "UPDATE rooms SET images = ? WHERE room_ID = ?";
            int rows = jdbcTemplate.update(sqlUpdate, updatedJson, roomId);

            return rows > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // admin version
    public boolean removeRoomImage(String roomId, Integer imageIndex, String base64Image) {
        try {
            // 1️⃣ Fetch current JSON array
            String sqlFetch = "SELECT r.images FROM rooms r INNER JOIN hotels h ON r.hotel_ID = h.hotel_ID WHERE r.room_ID = ?";
            String imagesJson = jdbcTemplate.queryForObject(sqlFetch, String.class, roomId);

            if (imagesJson == null || imagesJson.isBlank())
                return false;

            ObjectMapper mapper = new ObjectMapper();
            List<String> images = mapper.readValue(imagesJson, new TypeReference<List<String>>() {
            });

            boolean removed = false;

            if (imageIndex != null) {
                // 1-based index
                if (imageIndex >= 1 && imageIndex <= images.size()) {
                    images.remove(imageIndex - 1);
                    removed = true;
                }
            } else if (base64Image != null) {
                removed = images.removeIf(img -> img.equals(base64Image));
            }

            if (!removed)
                return false;

            // 2️⃣ Update DB with modified JSON array
            String updatedJson = mapper.writeValueAsString(images);
            String sqlUpdate = "UPDATE rooms SET images = ? WHERE room_ID = ?";
            int rows = jdbcTemplate.update(sqlUpdate, updatedJson, roomId);

            return rows > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean appendRoomImages(String roomId, List<String> newImages, int ownerId) {

        try {
            String selectSql = "SELECT r.images FROM rooms r INNER JOIN hotels h ON r.hotel_ID = h.hotel_ID WHERE r.room_ID = ? AND h.owner_ID = ?";
            String imagesJson = jdbcTemplate.queryForObject(
                    selectSql, String.class, roomId, ownerId);

            ObjectMapper mapper = new ObjectMapper();
            List<String> images;

            if (imagesJson == null || imagesJson.isBlank()) {
                images = new ArrayList<>();
            } else {
                images = mapper.readValue(
                        imagesJson,
                        new TypeReference<List<String>>() {
                        });
            }

            // ✅ Add ALL images individually
            images.addAll(newImages);

            String updateSql = """
                    UPDATE rooms
                    SET images = ?, updatedAt = NOW()
                    WHERE room_ID = ?
                    """;

            int rows = jdbcTemplate.update(
                    updateSql,
                    mapper.writeValueAsString(images),
                    roomId);

            return rows > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // admin version
    public boolean appendRoomImages(String roomId, List<String> newImages) {

        try {
            String selectSql = "SELECT images FROM rooms WHERE room_ID = ?";
            String imagesJson = jdbcTemplate.queryForObject(
                    selectSql, String.class, roomId);

            ObjectMapper mapper = new ObjectMapper();
            List<String> images;

            if (imagesJson == null || imagesJson.isBlank()) {
                images = new ArrayList<>();
            } else {
                images = mapper.readValue(
                        imagesJson,
                        new TypeReference<List<String>>() {
                        });
            }

            // ✅ Add ALL images individually
            images.addAll(newImages);

            String updateSql = """
                    UPDATE rooms
                    SET images = ?, updatedAt = NOW()
                    WHERE room_ID = ?
                    """;

            int rows = jdbcTemplate.update(
                    updateSql,
                    mapper.writeValueAsString(images),
                    roomId);

            return rows > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateHotel(int ownerId, Map<String, Object> updates) {

        if (updates == null || updates.isEmpty()) {
            throw new IllegalArgumentException("No fields provided for update");
        }

        StringBuilder sql = new StringBuilder("""
                    UPDATE hotels h
                    SET
                """);

        List<Object> params = new ArrayList<>();

        if (updates.containsKey("name")) {
            sql.append(" h.name = ?,");
            params.add(updates.get("name"));
        }
        if (updates.containsKey("type")) {
            sql.append(" h.type = ?,");
            params.add(updates.get("type"));
        }
        if (updates.containsKey("destination")) {
            sql.append(" h.destination = ?,");
            params.add(updates.get("destination"));
        }
        if (updates.containsKey("isForEvent")) {
            sql.append(" h.isForEvent = ?,");
            params.add(updates.get("isForEvent"));
        }
        if (updates.containsKey("description")) {
            sql.append(" h.description = ?,");
            params.add(updates.get("description"));
        }
        if (updates.containsKey("phone_NO")) {
            sql.append(" h.phone_NO = ?,");
            params.add(updates.get("phone_NO"));
        }
        if (updates.containsKey("country_code")) {
            sql.append(" h.country_code = ?,");
            params.add(updates.get("country_code"));
        }
        if (updates.containsKey("tags")) {
            sql.append(" h.tags = ?,");
            params.add(updates.get("tags"));
        }
        if (updates.containsKey("amenities")) {
            sql.append(" h.amenities = ?,");
            params.add(updates.get("amenities"));
        }
        if (updates.containsKey("longitude")) {
            sql.append(" h.longitude = ?,");
            params.add(updates.get("longitude"));
        }
        if (updates.containsKey("latitude")) {
            sql.append(" h.latitude = ?,");
            params.add(updates.get("latitude"));
        }
        if (updates.containsKey("images")) {
            sql.append(" h.images = ?,");
            params.add(updates.get("images"));
        }

        // Remove trailing comma
        if (sql.charAt(sql.length() - 1) == ',') {
            sql.deleteCharAt(sql.length() - 1);
        }

        // Always update timestamp
        sql.append(", h.updatedAt = NOW()");

        // Ownership + hotel condition
        sql.append(" WHERE h.owner_ID = ?");
        params.add(ownerId);

        int rows = jdbcTemplate.update(sql.toString(), params.toArray());
        return rows > 0;
    }

    /// admin version
    public boolean updateHotel(String hotelId, Map<String, Object> updates) {

        if (updates == null || updates.isEmpty()) {
            throw new IllegalArgumentException("No fields provided for update");
        }

        StringBuilder sql = new StringBuilder("""
                    UPDATE hotels h
                    SET
                """);

        List<Object> params = new ArrayList<>();

        if (updates.containsKey("name")) {
            sql.append(" h.name = ?,");
            params.add(updates.get("name"));
        }
        if (updates.containsKey("type")) {
            sql.append(" h.type = ?,");
            params.add(updates.get("type"));
        }
        if (updates.containsKey("destination")) {
            sql.append(" h.destination = ?,");
            params.add(updates.get("destination"));
        }
        if (updates.containsKey("isForEvent")) {
            sql.append(" h.isForEvent = ?,");
            params.add(updates.get("isForEvent"));
        }
        if (updates.containsKey("description")) {
            sql.append(" h.description = ?,");
            params.add(updates.get("description"));
        }
        if (updates.containsKey("phone_NO")) {
            sql.append(" h.phone_NO = ?,");
            params.add(updates.get("phone_NO"));
        }
        if (updates.containsKey("country_code")) {
            sql.append(" h.country_code = ?,");
            params.add(updates.get("country_code"));
        }
        if (updates.containsKey("tags")) {
            sql.append(" h.tags = ?,");
            params.add(updates.get("tags"));
        }
        if (updates.containsKey("amenities")) {
            sql.append(" h.amenities = ?,");
            params.add(updates.get("amenities"));
        }
        if (updates.containsKey("longitude")) {
            sql.append(" h.longitude = ?,");
            params.add(updates.get("longitude"));
        }
        if (updates.containsKey("latitude")) {
            sql.append(" h.latitude = ?,");
            params.add(updates.get("latitude"));
        }
        if (updates.containsKey("images")) {
            sql.append(" h.images = ?,");
            params.add(updates.get("images"));
        }

        // Remove trailing comma
        if (sql.charAt(sql.length() - 1) == ',') {
            sql.deleteCharAt(sql.length() - 1);
        }

        // Always update timestamp
        sql.append(", h.updatedAt = NOW()");

        // Ownership + hotel condition
        sql.append(" WHERE h.hotel_ID = ?");
        params.add(hotelId);

        int rows = jdbcTemplate.update(sql.toString(), params.toArray());
        return rows > 0;
    }

    public List<Map<String, Object>> getHotelsByOwner(int ownerId) {

        String sql = """
                SELECT
                    hotel_ID,
                    name,
                    type,
                    destination,
                    description,
                    country_code,
                    phone_no,
                    tags,
                    images,
                    amenities,
                    longitude,
                    latitude,
                    status,
                    remark
                FROM hotels
                WHERE owner_ID = ?
                ORDER BY hotel_ID DESC
                """;

        ObjectMapper mapper = new ObjectMapper();

        return jdbcTemplate.query(sql, new Object[] { ownerId }, (rs, rowNum) -> {
            String phoneNo = rs.getString("country_code") + "-" + rs.getString("phone_no");
            Map<String, Object> map = new LinkedHashMap<>();

            map.put("hotel_ID", rs.getString("hotel_ID"));
            map.put("name", rs.getString("name"));
            map.put("type", rs.getString("type"));
            map.put("destination", rs.getString("destination"));
            map.put("description", rs.getString("description"));
            map.put("phone_no", phoneNo);
            map.put("longitude", rs.getString("longitude"));
            map.put("latitude", rs.getString("latitude"));
            map.put("status", rs.getString("status"));
            map.put("remark", rs.getString("remark"));

            // 🔹 Parse tags
            String tagsJson = rs.getString("tags");
            try {
                map.put("tags",
                        tagsJson != null ? mapper.readValue(tagsJson, List.class) : List.of());
            } catch (Exception e) {
                map.put("tags", List.of());
            }

            // 🔹 Parse amenities.
            String amenitiesJson = rs.getString("amenities");
            try {
                map.put("amenities",
                        amenitiesJson != null ? mapper.readValue(amenitiesJson, List.class) : List.of());
            } catch (Exception e) {
                map.put("amenities", List.of());
            }

            // 🔹 Parse images (filenames only)
            // 🔹 Parse images (single image per row)
            String image = rs.getString("images");

            if (image != null && !image.isBlank()) {
                map.put("images", List.of(image));
            } else {
                map.put("images", List.of());
            }

            return map;
        });
    }

    // admin version
    public List<Map<String, Object>> getHotelsByOwner(String hotelId) {

        String sql = """
                SELECT
                    h.hotel_ID,
                    h.name,
                    h.type,
                    h.destination,
                    h.description,
                    h.country_code,
                    h.phone_no,
                    h.tags,
                    h.images,
                    h.amenities,
                    h.longitude,
                    h.latitude,
                    h.status,
                    h.remark,
                    p.name as ownerName
                FROM hotels h left join profile p on h.owner_ID = p.user_ID
                WHERE h.hotel_ID = ?
                ORDER BY h.hotel_ID DESC
                """;

        ObjectMapper mapper = new ObjectMapper();

        return jdbcTemplate.query(sql, new Object[] { hotelId }, (rs, rowNum) -> {
            String phoneNo = rs.getString("country_code") + "-" + rs.getString("phone_no");
            Map<String, Object> map = new LinkedHashMap<>();

            map.put("hotel_ID", rs.getString("hotel_ID"));
            map.put("name", rs.getString("name"));
            map.put("ownerName", rs.getString("ownerName"));
            map.put("type", rs.getString("type"));
            map.put("destination", rs.getString("destination"));
            map.put("description", rs.getString("description"));
            map.put("phone_no", phoneNo);
            map.put("longitude", rs.getString("longitude"));
            map.put("latitude", rs.getString("latitude"));
            map.put("status", rs.getString("status"));
            map.put("remark", rs.getString("remark"));

            // 🔹 Parse tags
            String tagsJson = rs.getString("tags");
            try {
                map.put("tags",
                        tagsJson != null ? mapper.readValue(tagsJson, List.class) : List.of());
            } catch (Exception e) {
                map.put("tags", List.of());
            }

            // 🔹 Parse amenities.
            String amenitiesJson = rs.getString("amenities");
            try {
                map.put("amenities",
                        amenitiesJson != null ? mapper.readValue(amenitiesJson, List.class) : List.of());
            } catch (Exception e) {
                map.put("amenities", List.of());
            }

            // 🔹 Parse images (filenames only)
            // 🔹 Parse images (single image per row)
            String image = rs.getString("images");

            if (image != null && !image.isBlank()) {
                map.put("images", List.of(image));
            } else {
                map.put("images", List.of());
            }

            return map;
        });
    }

    public boolean updateRoomStatus(
            int ownerId, String roomId, boolean isEnable) {

        String sql = """
                    UPDATE rooms r
                    INNER JOIN hotels h ON r.hotel_ID = h.hotel_ID
                    SET r.isEnable = ?
                    WHERE h.owner_ID = ?
                      AND r.room_ID = ?
                      AND r.isEnable <> ?
                """;

        return jdbcTemplate.update(
                sql,
                isEnable,
                ownerId,
                roomId,
                isEnable) > 0;
    }

    // admin version
    public boolean updateRoomStatusad(
            String hotelId, String roomId, boolean isEnable) {

        String sql = """
                    UPDATE rooms r
                    INNER JOIN hotels h ON r.hotel_ID = h.hotel_ID
                    SET r.isEnable = ?
                    WHERE h.hotel_ID = ?
                      AND r.room_ID = ?
                      AND r.isEnable <> ?
                """;

        return jdbcTemplate.update(
                sql,
                isEnable,
                hotelId,
                roomId,
                isEnable) > 0;
    }

    public List<Map<String, Object>> getallrooms(int ownerId) {

        ObjectMapper mapper = new ObjectMapper();

        String sql = """
                SELECT
                    r.room_ID,
                    r.room_NO,
                    r.room_Type,
                    r.hotel_ID,
                    r.features,
                    r.price,
                    r.images,
                    r.isEnable,
                    r.createdAt,
                    CASE
                        WHEN EXISTS (
                            SELECT 1
                            FROM bookings b
                            WHERE b.room_ID = r.room_ID
                              AND b.checkIn <= CURRENT_DATE()
                              AND b.checkOut >= CURRENT_DATE()
                              AND b.booking_Status NOT IN ('Cancelled')
                        )
                        THEN 'Occupied'
                        ELSE 'Available'
                    END AS Status
                FROM rooms r
                JOIN (
                    SELECT r2.room_ID
                    FROM rooms r2
                    INNER JOIN hotels h2 ON h2.hotel_ID = r2.hotel_ID
                    WHERE h2.owner_ID = ? AND r2.isEnable = true
                    ORDER BY r2.room_NO DESC
                ) sorted ON sorted.room_ID = r.room_ID
                INNER JOIN hotels h ON h.hotel_ID = r.hotel_ID
                """;

        List<Map<String, Object>> result = jdbcTemplate.query(
                sql,
                new Object[] { ownerId },
                (rs, rowNum) -> {

                    Map<String, Object> map = new LinkedHashMap<>();

                    map.put("room_ID", rs.getString("room_ID"));
                    map.put("room_NO", rs.getInt("room_NO"));
                    map.put("room_Type", rs.getString("room_Type"));
                    map.put("hotel_ID", rs.getString("hotel_ID"));

                    String featuresJson = rs.getString("features");
                    if (featuresJson != null && !featuresJson.isEmpty()) {
                        try {
                            map.put("features", mapper.readValue(
                                    featuresJson,
                                    new TypeReference<List<String>>() {
                                    }));
                        } catch (Exception e) {
                            map.put("features", List.of());
                        }
                    } else {
                        map.put("features", List.of());
                    }

                    map.put("price", rs.getInt("price"));
                    map.put("Status", rs.getString("Status"));
                    map.put("isEnable", rs.getBoolean("isEnable"));
                    map.put("createdAt", rs.getTimestamp("createdAt").toLocalDateTime());

                    // ✅ Images parsing
                    String imagesJson = rs.getString("images");

                    if (imagesJson != null && !imagesJson.isBlank()) {
                        try {
                            List<String> images = mapper.readValue(
                                    imagesJson,
                                    new TypeReference<List<String>>() {
                                    });
                            map.put("images", images);
                        } catch (Exception e) {
                            map.put("images", List.of());
                        }
                    } else {
                        map.put("images", List.of());
                    }

                    return map;
                });

        // ✅ Ensure never null (extra safe)
        return result != null ? result : List.of();
    }

    // overloding get allrooms for admin dashbord
    public List<Map<String, Object>> getallrooms(String hotelId) {

        ObjectMapper mapper = new ObjectMapper();

        String sql = """
                    SELECT
                    r.room_ID,
                    r.room_NO,
                    r.room_Type,
                    r.hotel_ID,
                    r.features,
                    r.price,
                    r.images,
                    r.isEnable,
                    r.createdAt,
                    CASE
                        WHEN EXISTS (
                            SELECT 1
                            FROM bookings b
                            WHERE b.room_ID = r.room_ID
                              AND b.checkIn <= CURRENT_DATE()
                              AND b.checkOut >= CURRENT_DATE()
                              AND b.booking_Status NOT IN ('Cancelled')
                        )
                        THEN 'Occupied'
                        ELSE 'Available'
                    END AS Status
                FROM rooms r
                JOIN (
                    SELECT r2.room_ID
                    FROM rooms r2
                    WHERE r2.hotel_ID = ?
                    ORDER BY r2.room_NO DESC
                ) sorted ON sorted.room_ID = r.room_ID
                INNER JOIN hotels h ON h.hotel_ID = r.hotel_ID;
                """;

        return jdbcTemplate.query(sql, new Object[] { hotelId }, (rs, rowNum) -> {

            Map<String, Object> map = new LinkedHashMap<>();

            map.put("room_ID", rs.getString("room_ID"));
            map.put("room_NO", rs.getInt("room_NO"));
            map.put("room_Type", rs.getString("room_Type"));
            map.put("hotel_ID", rs.getString("hotel_ID"));
            String featuresJson = rs.getString("features");
            if (featuresJson != null && !featuresJson.isEmpty()) {
                try {
                    map.put("features", objectMapper.readValue(
                            featuresJson, new TypeReference<List<String>>() {
                            }));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            map.put("price", rs.getInt("price"));
            map.put("Status", rs.getString("Status"));
            map.put("isEnable", rs.getBoolean("isEnable"));
            map.put("createdAt", rs.getTimestamp("createdAt").toLocalDateTime());

            // ✅ Parse JSON array
            String imagesJson = rs.getString("images");

            if (imagesJson != null && !imagesJson.isBlank()) {
                try {
                    List<String> images = mapper.readValue(
                            imagesJson,
                            new TypeReference<List<String>>() {
                            });
                    map.put("images", images);
                } catch (Exception e) {
                    map.put("images", List.of()); // fallback
                }
            } else {
                map.put("images", List.of());
            }

            return map;
        });
    }

    public List<Map<String, Object>> getallhotels() {

        String sql = """
                                SELECT
                    h.hotel_ID,
                    h.owner_ID,
                    h.name,
                    h.destination,
                    h.images,
                    h.status,
                    COALESCE(r.avg_rating, 0) AS rating,
                    COALESCE(rm.totalRooms, 0) AS totalRooms
                FROM hotels h
                LEFT JOIN (
                    SELECT hotel_ID, AVG(rating_Value) AS avg_rating
                    FROM rating
                    GROUP BY hotel_ID
                ) r ON r.hotel_ID = h.hotel_ID
                LEFT JOIN (
                    SELECT hotel_ID, COUNT(room_ID) AS totalRooms
                    FROM rooms
                    GROUP BY hotel_ID
                ) rm ON rm.hotel_ID = h.hotel_ID
                ORDER BY h.createdAt DESC;

                                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> hotel = new HashMap<>();

            hotel.put("hotel_ID", rs.getString("hotel_ID"));
            hotel.put("owner_ID", rs.getInt("owner_ID"));
            hotel.put("name", rs.getString("name"));
            hotel.put("destination", rs.getString("destination"));
            hotel.put("images", rs.getString("images"));
            hotel.put("rating", rs.getDouble("rating"));
            hotel.put("totalRooms", rs.getInt("totalRooms"));
            hotel.put("status", rs.getString("status"));

            return hotel;
        });
    }

    public List<Map<String, Object>> getpendinghotels() {

        String sql = """
                                SELECT
                    h.hotel_ID,
                    h.owner_ID,
                    h.name,
                    h.destination,
                    h.images,
                    h.createdAt,
                    h.status,
                    COALESCE(r.avg_rating, 0) AS rating,
                    COALESCE(rm.totalRooms, 0) AS totalRooms
                FROM hotels h
                LEFT JOIN (
                    SELECT hotel_ID, AVG(rating_Value) AS avg_rating
                    FROM rating
                    GROUP BY hotel_ID
                ) r ON r.hotel_ID = h.hotel_ID
                LEFT JOIN (
                    SELECT hotel_ID, COUNT(room_ID) AS totalRooms
                    FROM rooms
                    GROUP BY hotel_ID
                ) rm ON rm.hotel_ID = h.hotel_ID
                WHERE h.status = 'Pending'
                ORDER BY h.createdAt DESC;

                                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> hotel = new LinkedHashMap<>();

            hotel.put("hotel_ID", rs.getString("hotel_ID"));
            hotel.put("owner_ID", rs.getInt("owner_ID"));
            hotel.put("name", rs.getString("name"));
            hotel.put("destination", rs.getString("destination"));
            hotel.put("images", rs.getString("images"));
            hotel.put("rating", rs.getDouble("rating"));
            hotel.put("totalRooms", rs.getInt("totalRooms"));
            hotel.put("createdAt", rs.getTimestamp("createdAt").toLocalDateTime());
            hotel.put("status", rs.getString("status"));

            return hotel;
        });
    }

    public List<Map<String, Object>> getRoomDetails(String roomID) {

        String sql = """
                SELECT
                    room_ID,
                    room_NO,
                    room_Type,
                    hotel_ID,
                    features,
                    price,
                    images,
                    isEnable,
                    createdAt,
                    CASE
                        WHEN EXISTS (
                            SELECT 1
                            FROM bookings b
                            WHERE b.room_ID = r.room_ID
                              AND b.checkIn <= CURRENT_DATE()
                              AND b.checkOut >= CURRENT_DATE()
                              AND b.booking_Status NOT IN ('Cancelled')
                        )
                        THEN 'Occupied'
                        ELSE 'Available'
                    END AS Status
                FROM rooms r
                WHERE r.room_ID = ?
                """;

        return jdbcTemplate.query(sql, new Object[] { roomID }, (rs, rowNum) -> {

            Map<String, Object> room = new HashMap<>();

            room.put("room_ID", rs.getString("room_ID"));
            room.put("room_NO", rs.getInt("room_NO"));
            room.put("room_Type", rs.getString("room_Type"));
            room.put("hotel_ID", rs.getString("hotel_ID"));
            room.put("price", rs.getInt("price"));
            room.put("Status", rs.getString("Status"));
            room.put("isEnable", rs.getBoolean("isEnable"));
            room.put("createdAt", rs.getTimestamp("createdAt").toLocalDateTime());

            // -------- Parse features JSON --------
            String featuresJson = rs.getString("features");
            if (featuresJson != null && !featuresJson.isBlank()) {
                try {
                    List<String> features = objectMapper.readValue(
                            featuresJson,
                            new TypeReference<List<String>>() {
                            });
                    room.put("features", features);
                } catch (Exception e) {
                    room.put("features", List.of());
                }
            } else {
                room.put("features", List.of());
            }

            // -------- Parse images JSON --------
            String imagesJson = rs.getString("images");
            if (imagesJson != null && !imagesJson.isBlank()) {
                try {
                    List<String> images = objectMapper.readValue(
                            imagesJson,
                            new TypeReference<List<String>>() {
                            });
                    room.put("images", images);
                } catch (Exception e) {
                    room.put("images", List.of());
                }
            } else {
                room.put("images", List.of());
            }

            return room;
        });
    }

    // for offline booking
    public List<Map<String, Object>> getAvailableRooms(
            String hotelId,
            String checkIn,
            String checkOut) {

        ObjectMapper mapper = new ObjectMapper();

        LocalDate inDate = LocalDate.parse(checkIn);
        LocalDate outDate = LocalDate.parse(checkOut);
        long totalNights = ChronoUnit.DAYS.between(inDate, outDate);

        String sql = """
                SELECT r.room_ID,
                       r.room_NO,
                       r.hotel_ID,
                       r.room_Type,
                       r.price,
                       r.images
                FROM rooms r
                WHERE r.hotel_ID = ?
                  AND r.isEnable = true
                  AND NOT EXISTS (
                        SELECT 1
                        FROM bookings b
                        WHERE b.room_ID = r.room_ID
                          AND b.booking_Status NOT IN ('Cancelled','CheckedOut')
                          AND NOT (
                                ? >= b.checkOut
                             OR ? <= b.checkIn
                          )
                  )
                  AND NOT EXISTS (
                        SELECT 1
                        FROM room_locks rl
                        WHERE rl.room_id = r.room_ID
                          AND rl.lock_expiry > NOW()
                  )
                ORDER BY r.room_NO ASC
                """;

        return jdbcTemplate.query(sql, new Object[] { hotelId, checkIn, checkOut }, (rs, rowNum) -> {

            Map<String, Object> map = new LinkedHashMap<>();

            BigDecimal price = rs.getBigDecimal("price");
            BigDecimal totalPrice = price.multiply(BigDecimal.valueOf(totalNights));

            map.put("room_ID", rs.getString("room_ID"));
            map.put("room_NO", rs.getInt("room_NO"));
            map.put("hotel_ID", rs.getString("hotel_ID"));
            map.put("room_Type", rs.getString("room_Type"));

            map.put("price_per_night", price);
            map.put("total_nights", totalNights);
            map.put("total_price", totalPrice);

            String imagesJson = rs.getString("images");

            if (imagesJson != null && !imagesJson.isBlank()) {
                try {
                    List<String> images = mapper.readValue(
                            imagesJson,
                            new TypeReference<List<String>>() {
                            });

                    List<String> formattedImages = images.stream()
                            .map(img -> "data:image/jpeg;base64," + img)
                            .toList();

                    map.put("images", formattedImages);
                } catch (Exception e) {
                    map.put("images", List.of());
                }
            } else {
                map.put("images", List.of());
            }

            return map;
        });
    }
}
