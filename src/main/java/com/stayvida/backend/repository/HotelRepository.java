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


    // Main search method with availability check
    public List<Hotel> searchHotels(String destination, String checkIn, String checkOut, int adultCapacity, int childrenCapacity) {
        String sql = """
            SELECT h.*, 
                   (SELECT MIN(price) FROM rooms r WHERE r.hotel_ID = h.hotel_ID) AS lowest_price 
            FROM hotels h 
            WHERE h.destination = ? AND h.status = 'Verified'
        """;

        // Parse dates once
        LocalDate checkInDate = LocalDate.parse(checkIn);
        LocalDate checkOutDate = LocalDate.parse(checkOut);

        // Fetch all hotels matching destination
        List<Hotel> allHotels = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Hotel hotel = new Hotel();
            hotel.setId(rs.getInt("hotel_ID"));
            hotel.setName(rs.getString("name"));
            hotel.setType(rs.getString("type"));
            hotel.setDestination(rs.getString("destination"));
            hotel.setRating(rs.getDouble("rating"));
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
        // return allHotels;
    }

    // Helper to parse JSON string to List<String>
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

    public boolean isHotelAvailable(int hotelId, LocalDate checkIn, LocalDate checkOut) {
    String sql = """
        SELECT COUNT(*) 
        FROM rooms r
        WHERE r.hotel_ID = ?
        AND (
            -- Case 1: Room has no active overlapping booking
            NOT EXISTS (
                SELECT 1 
                FROM bookings b
                WHERE b.room_ID = r.room_ID
                  AND b.booking_Status <> 'Cancelled'
                  AND b.checkIn < ? 
                  AND b.checkOut > ?
            )
            -- Case 2: Room has a cancelled booking in that range (can be reused)
            OR EXISTS (
                SELECT 1 
                FROM bookings b
                WHERE b.room_ID = r.room_ID
                  AND b.booking_Status = 'Cancelled'
                  AND b.checkIn < ? 
                  AND b.checkOut > ?
            )
        )
    """;

    Integer availableRoomCount = jdbcTemplate.queryForObject(
        sql,
        Integer.class,
        hotelId,
        checkOut, checkIn,   // for NOT EXISTS
        checkOut, checkIn    // for EXISTS (cancelled)
    );

    return availableRoomCount != null && availableRoomCount > 0;
}


    public int updateVerificationStatus(int hotelId, String status, String remark) {
        String sql = "UPDATE hotels SET status = ?, remark = ? WHERE hotel_ID = ?";
        return jdbcTemplate.update(sql, status, remark, hotelId);
    }
}
