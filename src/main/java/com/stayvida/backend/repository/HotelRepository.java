package com.stayvida.backend.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
// import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
// import org.springframework.lang.NonNull;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stayvida.backend.model.Hotel;

// import java.sql.ResultSet;
// import java.sql.SQLException;
import java.time.LocalDate;
// import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Repository
public class HotelRepository {

   @Autowired
    private JdbcTemplate jdbcTemplate;

    // 🧭 Search hotels by location, availability & fetch average rating dynamically
    public List<Hotel> searchHotels(String destination, String checkIn, String checkOut, int adultCapacity, int childrenCapacity) {
        String sql = """
            SELECT h.*, 
                   (SELECT MIN(price) FROM rooms r WHERE r.hotel_ID = h.hotel_ID) AS lowest_price,
                   (SELECT AVG(rating_Value) FROM rating rt WHERE rt.hotel_ID = h.hotel_ID) AS avg_rating
            FROM hotels h 
            WHERE h.destination = ? AND h.status = 'Verified'
        """;

        LocalDate checkInDate = LocalDate.parse(checkIn);
        LocalDate checkOutDate = LocalDate.parse(checkOut);

        List<Hotel> allHotels = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Hotel hotel = new Hotel();
            hotel.setId(rs.getInt("hotel_ID"));
            hotel.setName(rs.getString("name"));
            hotel.setType(rs.getString("type"));
            hotel.setDestination(rs.getString("destination"));

            // ⬇️ Use average rating (from rating table) instead of hotels.rating
            Double avgRating = rs.getDouble("avg_rating");
            if (rs.wasNull()) avgRating = 0.0;
            hotel.setRating(avgRating);

            hotel.setForEvent(rs.getBoolean("isForEvent"));
            hotel.setPrice(rs.getDouble("lowest_price"));
            hotel.setImage(rs.getString("images"));
            hotel.setAmenities(parseJsonArray(rs.getString("amenities")));
            return hotel;
        }, destination);

        // Filter only available hotels
        List<Hotel> availableHotels = new ArrayList<>();
        for (Hotel hotel : allHotels) {
            if (isHotelAvailable(hotel.getId(), checkInDate, checkOutDate)) {
                availableHotels.add(hotel);
            }
        }

        return availableHotels;
    }

    // 📘 Helper to parse JSON string → List<String>
    private List<String> parseJsonArray(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
      // ✅ Check hotel availability
    public boolean isHotelAvailable(int hotelId, LocalDate checkIn, LocalDate checkOut) {
       String sql = """
    SELECT COUNT(*)
    FROM rooms r
    WHERE r.hotel_ID = ?
      AND NOT EXISTS (
            SELECT 1
            FROM bookings b
            WHERE b.room_ID = r.room_ID
              AND b.booking_Status NOT IN ('Cancelled', 'Checkout')
              AND (
                    (? > b.checkIn AND ? < b.checkOut)      -- condition 1
                 OR (? > b.checkIn AND ? < b.checkOut)      -- condition 2
              )
      )
""";


        Integer availableRoomCount = jdbcTemplate.queryForObject(
                sql, Integer.class,
                hotelId,
                checkOut, checkIn,
                checkOut, checkIn
        );

        return availableRoomCount != null && availableRoomCount > 0;
    }



    public int updateVerificationStatus(int hotelId, String status, String remark) {
        String sql = "UPDATE hotels SET status = ?, remark = ? WHERE hotel_ID = ?";
        return jdbcTemplate.update(sql, status, remark, hotelId);
    }

    // Add this method to get top 3 hotels by rating
    public List<Hotel> getTop3HotelsByRating() {
        String sql = "SELECT h.*, (SELECT MIN(r.price)     FROM rooms r WHERE r.hotel_ID = h.hotel_ID) AS lowest_price,(SELECT AVG(rating_Value) FROM rating rt WHERE rt.hotel_ID = h.hotel_ID) AS avg_rating  FROM hotels h WHERE h.status = 'Verified' ORDER BY avg_rating DESC LIMIT 3 ";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Hotel hotel = new Hotel();
            hotel.setId(rs.getInt("hotel_ID"));
            hotel.setName(rs.getString("name"));
            hotel.setType(rs.getString("type"));
            hotel.setDestination(rs.getString("destination"));
            hotel.setRating(rs.getDouble("avg_rating"));
            hotel.setForEvent(rs.getBoolean("isForEvent"));
            hotel.setPrice(rs.getDouble("lowest_price")); // adjust if column name differs
            hotel.setImage(rs.getString("images"));
            hotel.setAmenities(parseJsonArray(rs.getString("amenities")));
            return hotel;
        });
    }



}
