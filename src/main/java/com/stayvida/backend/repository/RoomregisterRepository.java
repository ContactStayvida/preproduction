package com.stayvida.backend.repository;

// import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
// import com.stayvida.backend.dto.RegisterRoom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.UUID;

@Repository
public class RoomregisterRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String saveRoomWithJson(int hotelId, String roomType, String featuresJson,
                               String imagesJson, int price, int maxAdults,
                               int maxChildren, int bedCount) {
    String roomId = UUID.randomUUID().toString();
    Timestamp now = new Timestamp(System.currentTimeMillis());

    String sql = "INSERT INTO rooms (" +
            "room_ID, hotel_ID, room_Type, features, images, price, max_adults, max_children, bed_count, createdAt, updatedAt" +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    jdbcTemplate.update(sql, ps -> {
        ps.setString(1, roomId);
        ps.setInt(2, hotelId);
        ps.setString(3, roomType);
        ps.setString(4, featuresJson);
        ps.setString(5, imagesJson);
        ps.setDouble(6, price);
        ps.setInt(7, maxAdults);
        ps.setInt(8, maxChildren);
        ps.setInt(9, bedCount);
        ps.setTimestamp(10, now);
        ps.setTimestamp(11, now);
    });

    return roomId;
}
}