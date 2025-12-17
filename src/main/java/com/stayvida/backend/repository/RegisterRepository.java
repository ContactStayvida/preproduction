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

        // 🔍 Check if hotel already exists
        Integer existingHotelId = findHotelIdByOwnerId(register.getOwner_ID());
        if (existingHotelId != null) {
            return existingHotelId; // ❌ do not insert
        }

        updateUserRoleIfNeeded(register.getOwner_ID());

        String sql = "INSERT INTO hotels (" +
                "owner_ID, name, type, destination, isForEvent, description, phone_NO, country_code, tags, " +
                "amenities, images, longitude, latitude, status, onArrivalPayment, remark, createdAt, updatedAt" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setInt(1, register.getOwner_ID());
            ps.setString(2, register.getName());
            ps.setString(3, register.getType());
            ps.setString(4, register.getDestination());
            ps.setBoolean(5, register.isForEvent());
            ps.setString(6, register.getDescription());
            ps.setString(7, register.getPhone_NO());
            ps.setString(8, register.getCountry_code());

            try {
                ps.setString(9, objectMapper.writeValueAsString(register.getTags()));
                ps.setString(10, objectMapper.writeValueAsString(register.getAmenities()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            ps.setString(11, register.getImages());
            ps.setString(12, register.getLongitude());
            ps.setString(13, register.getLatitude());

            ps.setString(14, "Pending");
            ps.setBoolean(15, false);
            ps.setString(16, "New hotel added");

            return ps;
        }, keyHolder);

        return keyHolder.getKey().intValue();
    }

    private void updateUserRoleIfNeeded(int ownerId) {
        String sqlCheck = "SELECT role FROM users WHERE user_ID = ?";

        String role = jdbcTemplate.query(sqlCheck, rs -> {
            if (rs.next()) {
                return rs.getString("role");
            }
            return null;
        }, ownerId);

        if (role == null)
            return;

        if (role.equalsIgnoreCase("user")) {
            String sqlUpdate = "UPDATE users SET role = 'hotel_owner' WHERE user_ID = ?";
            jdbcTemplate.update(sqlUpdate, ownerId);
        }
    }

    public void updateHotelImage(int hotelId, String imageUrl) {
        String sql = "UPDATE hotels SET images = ?, updatedAt = NOW() WHERE hotel_ID = ?";
        jdbcTemplate.update(sql, imageUrl, hotelId);
    }

    public Integer findHotelIdByOwnerId(int ownerId) {
        String sql = "SELECT hotel_ID FROM hotels WHERE owner_ID = ? LIMIT 1";

        return jdbcTemplate.query(sql, rs -> {
            if (rs.next()) {
                return rs.getInt("hotel_ID");
            }
            return null;
        }, ownerId);
    }

}
