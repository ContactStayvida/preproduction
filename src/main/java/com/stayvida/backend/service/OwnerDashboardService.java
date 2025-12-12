package com.stayvida.backend.service;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.stayvida.backend.repository.OwnerDashboardRepository;

@Service
public class OwnerDashboardService {

    @Autowired
    private OwnerDashboardRepository repo;

    @Autowired
    private JdbcTemplate jdbcTemplate;  // ✅ REQUIRED

    private static final Set<String> VALID_BOOKING_STATUSES = Set.of(
        "CheckIn",
        "Confirmed",
        "Cancelled",
        "CheckedOut"
    );


    
    // ===============================
    // MONTHLY BOOKING METRICS
    // ===============================
    public Map<String, Object> getMonthlyBookingsForOwner(int ownerId) {

        Map<String, Object> response = new LinkedHashMap<>();

        List<Map<String, Object>> hotels = repo.getOwnerVerifiedHotels(ownerId);

        List<Map<String, Object>> hotelWiseList = new ArrayList<>();
        int totalMonthlyBookings = 0;
        int totalGuests = 0;

        for (Map<String, Object> hotel : hotels) {

            int hotelId = (int) hotel.get("hotel_ID");
            String hotelName = (String) hotel.get("name");

            int hotelBookings = repo.getMonthlyBookingCount(hotelId);
            totalMonthlyBookings += hotelBookings;

            int hotelGuests = repo.getTotalGuestsForMonth(hotelId);
            totalGuests += hotelGuests;

            List<Map<String, Object>> rooms = repo.getRoomsByHotelId(hotelId);
            List<Map<String, Object>> roomWiseList = new ArrayList<>();

            for (Map<String, Object> room : rooms) {
                String roomId = (String) room.get("room_ID");
                String roomType = (String) room.get("room_Type");

                int roomBookings = repo.getMonthlyBookingCountForRoom(roomId);

                Map<String, Object> roomData = new LinkedHashMap<>();
                roomData.put("roomId", roomId);
                roomData.put("roomType", roomType);
                roomData.put("monthlyBookings", roomBookings);

                roomWiseList.add(roomData);
            }

            Map<String, Object> hotelData = new LinkedHashMap<>();
            hotelData.put("hotelId", hotelId);
            hotelData.put("hotelName", hotelName);
            hotelData.put("monthlyBookings", hotelBookings);
            hotelData.put("totalGuests", hotelGuests);
            hotelData.put("rooms", roomWiseList);

            hotelWiseList.add(hotelData);
        }

        double totalRevenue = repo.getMonthlyRevenueForOwner(ownerId);

        double lastMonthRevenue = repo.getLastMonthRevenueForOwner(ownerId);
        int lastMonthBookings = repo.getLastMonthBookingCount(ownerId);
        int lastMonthGuests = repo.getLastMonthGuestCount(ownerId);

        String revenueChange = calculateChange(totalRevenue, lastMonthRevenue);
        String bookingChange = calculateChange(totalMonthlyBookings, lastMonthBookings);
        String guestChange = calculateChange(totalGuests, lastMonthGuests);

        int roomsOccupiedToday = repo.getRoomsOccupiedToday(ownerId);

        // Ordered response
        response.put("totalMonthlyBookings", totalMonthlyBookings);
        response.put("bookingDifference", bookingChange);

        response.put("roomsOccupied", roomsOccupiedToday);

        response.put("totalGuests", totalGuests);
        response.put("guestDifference", guestChange);

        response.put("totalRevenue", totalRevenue);
        response.put("revenueDifference", revenueChange);

        response.put("hotelWiseBookings", hotelWiseList);

        return response;
    }


    private String calculateChange(double current, double last) {

        if (last == 0 && current == 0) return "0% same as last month";

        if (last == 0) return "+100% increased from last month";

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
        p.phone_number
        FROM bookings b
        INNER JOIN profile p ON b.user_ID = p.user_ID
        INNER JOIN hotels h ON b.hotel_ID = h.hotel_ID
        WHERE h.owner_ID = ?
        AND b.booking_Status != 'CheckOut'
        AND (
        -- 🔥 Priority conditions first
        b.booking_Status = 'Pending'
        OR b.payment_Status = 'Pending'

        -- ✔ If NOT pending, then apply normal conditions
        OR (
        b.booking_Status IN ('Confirmed', 'CheckIn')
        OR b.checkOut >= CURDATE()
        )
        )
        ORDER BY b.checkIn ASC
        """;

        return jdbcTemplate.query(sql, new Object[]{ownerId}, (rs, rowNum) -> {

            Map<String, Object> map = new LinkedHashMap<>();

            double totalAmount = rs.getDouble("totalAmount");
            double taxAmount = rs.getDouble("tax_amount");
            double platformFee = rs.getDouble("platformFee");
            double paid = rs.getDouble("payment_amount");

            double platformBase = totalAmount - taxAmount - platformFee;
            double commission = platformBase * 0.20;
            double payableAfterCut = platformBase - commission;
            double paymentLeft = totalAmount - paid;
            

            map.put("booking_ID", rs.getString("booking_ID"));
            map.put("user_ID", rs.getInt("user_ID"));
            map.put("hotel_ID", rs.getInt("hotel_ID"));
            map.put("room_ID", rs.getString("room_ID"));
            map.put("booking_Status", rs.getString("booking_Status"));
            map.put("checkIn", rs.getDate("checkIn"));
            map.put("checkOut", rs.getDate("checkOut"));
            map.put("payment_Status", rs.getString("payment_Status"));
            map.put("payment_amount", paid);
            map.put("payment_left", paymentLeft);
            map.put("is_refundable", rs.getBoolean("is_refundable"));
            map.put("totalAmount", totalAmount);
            map.put("tax_amount", taxAmount);
            map.put("platformFee", platformFee);
            map.put("commision",commission);
            map.put("name", rs.getString("name"));
            map.put("phone_number", rs.getString("phone_number"));
            map.put("Gross Amount", totalAmount);
            map.put("Net Amount", payableAfterCut);

            return map;
        });
    }

    public boolean updateBookingStatus(String bookingId, String newStatus) {

    // Validate allowed values
    List<String> allowed = Arrays.asList("CheckIn", "Confirmed", "Cancelled", "CheckedOut");

    if (!allowed.contains(newStatus)) {
        throw new IllegalArgumentException("Invalid booking status: " + newStatus);
    }

    String sql = "UPDATE bookings SET booking_Status = ?, updatedAt = NOW() WHERE booking_ID = ?";

    int rows = jdbcTemplate.update(sql, newStatus, bookingId);

    return rows > 0;
}
public List<Map<String, Object>> getAllBookingsForOwner(int ownerId) {

    String sql = """
        SELECT 
            b.booking_ID,
            b.user_ID,
            b.hotel_ID,
            b.room_ID,
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
            p.phone_number
        FROM bookings b
        INNER JOIN profile p ON b.user_ID = p.user_ID
        INNER JOIN hotels h ON b.hotel_ID = h.hotel_ID
        WHERE h.owner_ID = ?
        ORDER BY b.checkIn DESC
    """;

    return jdbcTemplate.query(sql, new Object[]{ownerId}, (rs, rowNum) -> {

        Map<String, Object> map = new LinkedHashMap<>();

        double totalAmount = rs.getDouble("totalAmount");
        double taxAmount = rs.getDouble("tax_amount");
        double platformFee = rs.getDouble("platformFee");
        double paid = rs.getDouble("payment_amount");

        double platformBase = totalAmount - taxAmount - platformFee;
        double commission = platformBase * 0.20;
        double payableAfterCut = platformBase - commission;

        double paymentLeft = totalAmount - paid;

        map.put("booking_ID", rs.getString("booking_ID"));
        map.put("user_ID", rs.getInt("user_ID"));
        map.put("hotel_ID", rs.getInt("hotel_ID"));
        map.put("room_ID", rs.getString("room_ID"));
        map.put("booking_Status", rs.getString("booking_Status"));
        map.put("checkIn", rs.getDate("checkIn"));
        map.put("checkOut", rs.getDate("checkOut"));
        map.put("payment_Status", rs.getString("payment_Status"));
        map.put("payment_amount", paid);
        map.put("payment_left", paymentLeft);
        map.put("is_refundable", rs.getBoolean("is_refundable"));

        map.put("totalAmount", totalAmount);
        map.put("tax_amount", taxAmount);
        map.put("platformFee", platformFee);
        map.put("commision", commission);
        map.put("Gross Amount", totalAmount);
        map.put("Net Amount", payableAfterCut);

        map.put("name", rs.getString("name"));
        map.put("phone_number", rs.getString("phone_number"));

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

    return jdbcTemplate.query(sql, new Object[]{ownerId}, (rs, rowNum) -> {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("booking_ID", rs.getString("booking_ID"));
        map.put("room_ID", rs.getString("room_ID"));
        map.put("name", rs.getString("name"));
        map.put("checkIn", rs.getDate("checkIn"));
        map.put("booking_Status", rs.getString("booking_Status"));

        return map;
    });
}


}
