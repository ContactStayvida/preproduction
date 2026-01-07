package com.stayvida.backend.repository;

// import com.stayvida.backend.dto.RatingRequest;
// import com.stayvida.backend.model.Rating;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
// import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
// import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Repository
public class OwnerDashboardRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Fetch verified hotels of owner
    public List<Map<String, Object>> getOwnerVerifiedHotels(int ownerId) {
        String sql = "SELECT hotel_ID, name FROM hotels WHERE owner_ID = ? AND status = 'Verified'";
        return jdbcTemplate.queryForList(sql, ownerId);
    }

    public BigDecimal getadvanceamount() {
        String sql = "Select value from amount where type = 'Advance'";
        return jdbcTemplate.queryForObject(sql, BigDecimal.class);
    }

    // Fetch monthly booking count for a hotel (check-in only)
    public int getMonthlyBookingCount(String hotelId) {
        String sql = """
                    SELECT COUNT(*) FROM bookings
                    WHERE hotel_ID = ?
                      AND MONTH(checkIn) = MONTH(CURRENT_DATE())
                      AND YEAR(checkIn) = YEAR(CURRENT_DATE())
                      AND booking_Status <> 'Cancelled'
                """;

        return jdbcTemplate.queryForObject(sql, Integer.class, hotelId);
    }

    // // Fetch all rooms of a hotel
    // public List<Map<String, Object>> getRoomsByHotelId(int hotelId) {
    // String sql = """
    // SELECT room_ID, room_Type
    // FROM rooms
    // WHERE hotel_ID = ?
    // """;
    // return jdbcTemplate.queryForList(sql, hotelId);
    // }

    // Fetch monthly booking count for a room (check-in only)
    public int getMonthlyBookingCountForRoom(String roomId) {
        String sql = """
                    SELECT COUNT(*) FROM bookings
                    WHERE room_ID = ?
                      AND MONTH(checkIn) = MONTH(CURRENT_DATE())
                      AND YEAR(checkIn) = YEAR(CURRENT_DATE())
                      AND booking_Status <> 'Cancelled'
                """;

        return jdbcTemplate.queryForObject(sql, Integer.class, roomId);
    }

    // Total guests for a hotel this month (check-in only)
    public int getTotalGuestsForMonth(String hotelId) {
        String sql = """
                    SELECT COALESCE(SUM(adults + children),0)
                    FROM bookings
                    WHERE hotel_ID = ?
                    AND MONTH(checkIn) = MONTH(CURRENT_DATE())
                    AND YEAR(checkIn) = YEAR(CURRENT_DATE())
                    AND booking_Status <> 'Cancelled'
                """;
        return jdbcTemplate.queryForObject(sql, Integer.class, hotelId);
    }

    public double getMonthlyRevenueForOwner(int ownerId) {
        String sql = """
                    SELECT COALESCE(SUM(
                        b.payment_amount
                        - commision_Amount
                        - b.platformFee
                    ), 0)
                    FROM bookings b
                    JOIN hotels h ON b.hotel_ID = h.hotel_ID
                    WHERE h.owner_ID = ?
                      AND h.status = 'Verified'
                      AND MONTH(b.checkIn) = MONTH(CURRENT_DATE())
                      AND YEAR(b.checkIn) = YEAR(CURRENT_DATE())
                      AND b.booking_Status <> 'Cancelled'
                """;

        return jdbcTemplate.queryForObject(sql, Double.class, ownerId);
    }
    // ================================
    // LAST MONTH COMPARISONS
    // ================================

    // Last month's revenue
    public double getLastMonthRevenueForOwner(int ownerId) {
        String sql = """
                    SELECT COALESCE(SUM(
                        b.payment_amount
                        - commision_Amount
                        - b.platformFee
                    ), 0)
                    FROM bookings b
                    JOIN hotels h ON b.hotel_ID = h.hotel_ID
                    WHERE h.owner_ID = ?
                      AND h.status = 'Verified'
                      AND MONTH(b.checkIn) = MONTH(CURRENT_DATE() - INTERVAL 1 MONTH)
                      AND YEAR(b.checkIn) = YEAR(CURRENT_DATE() - INTERVAL 1 MONTH)
                      AND b.booking_Status <> 'Cancelled'
                """;
        return jdbcTemplate.queryForObject(sql, Double.class, ownerId);
    }

    // Last month booking count
    public int getLastMonthBookingCount(int ownerId) {
        String sql = """
                    SELECT COUNT(*)
                    FROM bookings b
                    JOIN hotels h ON b.hotel_ID = h.hotel_ID
                    WHERE h.owner_ID = ?
                      AND h.status = 'Verified'
                      AND MONTH(b.checkIn) = MONTH(CURRENT_DATE() - INTERVAL 1 MONTH)
                      AND YEAR(b.checkIn) = YEAR(CURRENT_DATE() - INTERVAL 1 MONTH)
                      AND b.booking_Status <> 'Cancelled'
                """;
        return jdbcTemplate.queryForObject(sql, Integer.class, ownerId);
    }

    // Last month guests
    public int getLastMonthGuestCount(int ownerId) {
        String sql = """
                    SELECT COALESCE(SUM(adults + children),0)
                    FROM bookings b
                    JOIN hotels h ON b.hotel_ID = h.hotel_ID
                    WHERE h.owner_ID = ?
                      AND h.status = 'Verified'
                      AND MONTH(b.checkIn) = MONTH(CURRENT_DATE() - INTERVAL 1 MONTH)
                      AND YEAR(b.checkIn) = YEAR(CURRENT_DATE() - INTERVAL 1 MONTH)
                      AND b.booking_Status <> 'Cancelled'
                """;
        return jdbcTemplate.queryForObject(sql, Integer.class, ownerId);
    }

    // ================================
    // ROOMS OCCUPIED TODAY
    // ================================
    public int getRoomsOccupiedToday(int ownerId) {
        String sql = """
                    SELECT COUNT(*)
                    FROM bookings b
                    JOIN hotels h ON b.hotel_ID = h.hotel_ID
                    WHERE h.owner_ID = ?
                      AND b.booking_Status NOT IN ('Cancelled')
                      AND b.checkIn <= CURRENT_DATE()
                      AND b.checkOut >= CURRENT_DATE()
                """;

        return jdbcTemplate.queryForObject(sql, Integer.class, ownerId);
    }

    public int getTotalRoomCount(int ownerId) {
        String sql = """
                    SELECT COUNT(*)
                    FROM rooms r
                    JOIN hotels h ON r.hotel_ID = h.hotel_ID
                    WHERE h.owner_ID = ?
                """;

        return jdbcTemplate.queryForObject(sql, Integer.class, ownerId);

    }
}
