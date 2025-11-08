package com.stayvida.backend.controller;

import com.stayvida.backend.security.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
// import java.util.UUID;

@RestController
@RequestMapping("/api/events")
public class EventController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/add")
    public Object addEvent(@RequestBody Map<String, Object> request) {
        try {
            // 🆔 Generate new UUID for event_ID
            String event_ID = generateEventID();

            // 🧩 Extract and validate request fields
            if (request.get("eventType") == null || request.get("hotel_ID") == null || request.get("amount") == null) {
                return ApiResponse.badRequest("Missing required fields: eventType, hotel_ID, amount.");
            }

            String eventType = request.get("eventType").toString().trim(); // plain string like "Wedding"
            double amount = Double.parseDouble(request.get("amount").toString());
            int hotel_ID = Integer.parseInt(request.get("hotel_ID").toString());
            int guestCount = request.get("guestCount") != null ? Integer.parseInt(request.get("guestCount").toString()) : 0;

            Timestamp now = Timestamp.from(Instant.now());

            // 🔍 Check if same event type already exists for this hotel
            String checkSql = "SELECT COUNT(*) FROM events WHERE hotel_ID = ? AND eventType = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, hotel_ID, eventType);

            if (count != null && count > 0) {
                return ApiResponse.badRequest("Event of this type already exists for the selected hotel.");
            }

            // ✅ Insert new event
            String insertSql = """
                INSERT INTO events (event_ID, eventType, amount, hotel_ID, guestCount, createdAt, updatedAt)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

            int rows = jdbcTemplate.update(
                    insertSql,
                    event_ID,
                    eventType,
                    amount,
                    hotel_ID,
                    guestCount,
                    now,
                    now
            );

            if (rows > 0) {
                return ApiResponse.created(
                        Map.of("event_ID", event_ID),
                        "Event added successfully."
                );
            } else {
                return ApiResponse.serverError("Failed to insert event.");
            }

        } catch (Exception e) {
            return ApiResponse.serverError(e.getMessage());
        }
    }

    private String generateEventID() {
    String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    StringBuilder sb = new StringBuilder(4);
    java.util.Random random = new java.util.Random();
    for (int i = 0; i < 4; i++) {
        sb.append(chars.charAt(random.nextInt(chars.length())));
    }
    return sb.toString();
}

}
