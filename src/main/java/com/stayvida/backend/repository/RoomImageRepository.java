package com.stayvida.backend.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RoomImageRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void insertRoomImage(int hotelId, int roomId, String imageUrl) {
        String sql = "INSERT INTO dbo.room_images (hotel_ID, room_ID, image_url) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, hotelId, roomId, imageUrl);
    }
}