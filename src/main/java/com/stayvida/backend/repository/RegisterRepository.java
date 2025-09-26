package com.stayvida.backend.repository;

import com.stayvida.backend.model.Hotel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.stayvida.backend.model.Register;
@Repository
public class RegisterRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Insert new hotel (without image)
    public int saveHotel(Register register) {
        String sql = "INSERT INTO dbo.hotel (name, location, description) VALUES (?, ?, ?)";
        return jdbcTemplate.update(sql, register.getName(), register.getLocation(), register.getDescription());
    }

    // Update hotel image
    public void updateHotelImage(int hotelId, String imageUrl) {
        String sql = "UPDATE dbo.hotel SET image_url = ? WHERE hotel_id = ?";
        jdbcTemplate.update(sql, imageUrl, hotelId);
    }
}
