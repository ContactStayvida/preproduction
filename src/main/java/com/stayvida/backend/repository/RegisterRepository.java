package com.stayvida.backend.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stayvida.backend.model.Register;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;

@Repository
public class RegisterRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Insert new hotel record
    public int saveHotel(Register register) {
        String sql = "INSERT INTO hotels (" +
                "owner_ID, name, type, destination, rating, isForEvent, description, phone_NO, tags, " +
                "amenities, images, longitude, latitude, status, onArrivalPayment, remark, createdAt, updatedAt" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, register.getOwner_ID());
            ps.setString(2, register.getName());
            ps.setString(3, register.getType());
            ps.setString(4, register.getDestination());
            ps.setObject(5, register.getRating());
            ps.setBoolean(6, register.isForEvent());
            ps.setString(7, register.getDescription());
            ps.setString(8, register.getPhone_NO());

            try {
                ps.setString(9, objectMapper.writeValueAsString(register.getTags()));
                ps.setString(10, objectMapper.writeValueAsString(register.getAmenities()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error converting JSON", e);
            }

            ps.setString(11, register.getImages());
            ps.setString(12, register.getLongitude());
            ps.setString(13, register.getLatitude());
            ps.setString(14, register.getStatus() != null ? register.getStatus() : "Pending");
            ps.setBoolean(15, register.isOnArrivalPayment());
            ps.setString(16, register.getRemark());

            return ps;
        }, keyHolder);

        return keyHolder.getKey().intValue();
    }

    // Update hotel image
    public void updateHotelImage(int hotelId, String imageUrl) {
        String sql = "UPDATE hotels SET images = ?, updatedAt = NOW() WHERE hotel_ID = ?";
        jdbcTemplate.update(sql, imageUrl, hotelId);
    }
}
