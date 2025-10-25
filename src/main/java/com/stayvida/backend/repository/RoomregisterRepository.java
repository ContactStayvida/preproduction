package com.stayvida.backend.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Random;

@Repository
public class RoomregisterRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Random RANDOM = new Random();

    // Generate 4-character uppercase alphanumeric room ID
    private String generateRoomId() {
        StringBuilder sb = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            sb.append(ALPHANUM.charAt(RANDOM.nextInt(ALPHANUM.length())));
        }
        return sb.toString();
    }

    // Check if room ID already exists
    private boolean roomIdExists(String roomId) {
        String sql = "SELECT COUNT(*) FROM rooms WHERE room_ID = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, roomId);
        return count != null && count > 0;
    }

    // Generate unique room ID
    private String generateUniqueRoomId() {
        String roomId;
        do {
            roomId = generateRoomId();
        } while (roomIdExists(roomId));
        return roomId;
    }

    public String saveRoomWithJson(int hotelId, String roomType, String featuresJson,
                                   String imagesJson, int price, int maxAdults,
                                   int maxChildren, int bedCount) {
        String roomId = generateUniqueRoomId(); // unique 4-char ID
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
