package com.stayvida.backend.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.stayvida.backend.model.Register;

import java.sql.PreparedStatement;

@Repository
public class RegisterRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Insert new hotel and return generated ID
    public int saveHotel(Register register) {
        String sql = "INSERT INTO hotels (hotel, location, max_adults, max_children, max_room, description) VALUES (?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"hotel_id"});
            ps.setString(1, register.getHotel());
            ps.setString(2, register.getLocation());
            ps.setInt(3, register.getMaxAdults());
            ps.setInt(4, register.getMaxChildren());
            ps.setInt(5, register.getMaxRoom());
            ps.setString(6, register.getDescription());
            return ps;
        }, keyHolder);

        return keyHolder.getKey().intValue();
    }

    // Update hotel image
    public void updateHotelImage(int hotelId, String imageUrl) {
        String sql = "UPDATE hotels SET image_url = ? WHERE hotel_ID = ?";
        jdbcTemplate.update(sql, imageUrl, hotelId);
    }
}
