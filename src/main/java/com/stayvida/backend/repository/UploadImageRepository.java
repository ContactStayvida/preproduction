package com.stayvida.backend.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UploadImageRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Update only image_url for a given hotelId
    public void updateHotelImage(int hotelId, String imageUrl) {
        String sql = "UPDATE dbo.hotel SET image_url = ? WHERE hotel_id = ?";
        jdbcTemplate.update(sql, imageUrl, hotelId);
    }
}
