package com.stayvida.backend.repository;
// import com.stayvida.backend.dto.RatingRequest;
// import com.stayvida.backend.model.Rating;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
// import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

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

    // Fetch monthly booking count for a hotel (check-in only)
    public int getMonthlyBookingCount(int hotelId) {
        String sql = """
            SELECT COUNT(*) FROM bookings 
            WHERE hotel_ID = ?
              AND MONTH(checkIn) = MONTH(CURRENT_DATE())
              AND YEAR(checkIn) = YEAR(CURRENT_DATE())
              AND booking_Status <> 'Cancelled'
        """;

        return jdbcTemplate.queryForObject(sql, Integer.class, hotelId);
    }

    // Fetch all rooms of a hotel
    public List<Map<String, Object>> getRoomsByHotelId(int hotelId) {
        String sql = """
            SELECT room_ID, room_Type 
            FROM rooms 
            WHERE hotel_ID = ?
        """;
        return jdbcTemplate.queryForList(sql, hotelId);
    }

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
}
