package com.stayvida.backend.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.stayvida.backend.model.Register;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class RegisterRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Insert new hotel and return generated ID
    public int saveHotel(Register register) {
        String sql = "INSERT INTO hotels (hotel, location, max_adults, max_children, max_room, description) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, register.getHotel());
            ps.setString(2, register.getLocation());
            ps.setInt(3, register.getMaxAdults());
            ps.setInt(4, register.getMaxChildren());
            ps.setInt(5, register.getMaxRoom());
            ps.setString(6, register.getDescription());
            return ps;
        }, keyHolder);

        int hotelId = keyHolder.getKey().intValue();

        // ✅ Insert amenities into hotel_amenity
        insertHotelAmenities(hotelId, register.getAmenities());

        return hotelId; 
    }

    // Insert amenities into hotel_amenity
    private void insertHotelAmenities(int hotelId, List<String> amenities) {
        if (amenities == null || amenities.isEmpty()) return;

        String findAmenityIdSql = "SELECT amenity_id FROM amenity WHERE name = ?";
        String insertHotelAmenitySql = "INSERT INTO hotel_amenity (hotel_id, amenity_id) VALUES (?, ?)";

        for (String amenity : amenities) {
            try {
                Integer amenityId = jdbcTemplate.queryForObject(findAmenityIdSql, Integer.class, amenity);
                if (amenityId != null) {
                    jdbcTemplate.update(insertHotelAmenitySql, hotelId, amenityId);
                }
            } catch (Exception e) {
                // If amenity name doesn't exist, just skip it (optional: log warning)
                System.out.println("Amenity not found: " + amenity);
            }
        }
    }

    // Update hotel image
    public void updateHotelImage(int hotelId, String imageUrl) {
        String sql = "UPDATE hotels SET image_url = ? WHERE hotel_ID = ?";
        jdbcTemplate.update(sql, imageUrl, hotelId);
    }
}
