package com.stayvida.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.stayvida.backend.security.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
public class EventController {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Value("${app.base.url}")
    private String baseUrl; // 🌐 Image URL prefix


    @PostMapping("/add")
    public Object createEvent(@RequestBody Map<String, Object> body) {
        try {
            String eventType = (String) body.get("eventType");
            Double amount = Double.valueOf(body.get("amount").toString());
            Integer hotelId = (Integer) body.get("hotel_ID");
            Integer guestCount = (Integer) body.get("guestCount");

            // Convert amenities array → JSON string
            ObjectMapper mapper = new ObjectMapper();
            String amenitiesJson = mapper.writeValueAsString(body.get("amenities"));

            // Generate random Event ID like A896
            String eventID = "A" + (int) (Math.random() * 900 + 100);

            // Current timestamp
            Timestamp currentTime = Timestamp.from(Instant.now());

            // ✅ Include createdAt and updatedAt
            String sql = "INSERT INTO events (event_ID, eventType, amount, hotel_ID, guestCount, amenities, createdAt, updatedAt) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.NO_GENERATED_KEYS);
                ps.setString(1, eventID);
                ps.setString(2, eventType);
                ps.setDouble(3, amount);
                ps.setInt(4, hotelId);
                ps.setInt(5, guestCount);
                ps.setString(6, amenitiesJson);
                ps.setTimestamp(7, currentTime); // createdAt
                ps.setTimestamp(8, currentTime); // updatedAt
                return ps;
            });

            Map<String, Object> data = new HashMap<>();
            data.put("event_ID", eventID);

            return ApiResponse.created(data, "Event added successfully.");

        } catch (Exception e) {
            return ApiResponse.serverError("Error while creating event: " + e.getMessage());
        }
    }
 
   // ✅ 2️⃣ Search Event Endpoint
    @PostMapping("/search")
public Object searchEvents(@RequestBody Map<String, Object> body) {
    try {
        String eventType = (String) body.get("eventType");
        String destination = (String) body.get("destination");
        Integer guestCount = (Integer) body.get("guestCount");

        String sql = """
            SELECT 
                e.event_ID, e.eventType, e.hotel_ID, e.guestCount, e.amenities AS event_amenities,
                h.name AS hotel_name, h.type AS hotel_type, h.destination, h.amenities AS hotel_amenities,
                h.images, h.status
            FROM events e
            INNER JOIN hotels h ON e.hotel_ID = h.hotel_ID
            WHERE e.eventType = ? 
            AND e.guestCount >= ?
            AND h.destination = ?
            AND h.isForEvent = TRUE
            AND h.status = 'Verified'
            """;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, eventType, guestCount, destination);

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> responseList = new ArrayList<>();

        for (Map<String, Object> row : results) {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("event_ID", row.get("event_ID"));
            eventData.put("eventType", row.get("eventType"));
            eventData.put("guestCount", row.get("guestCount"));

            // ✅ Parse event amenities JSON → List
            try {
                String eventAmenitiesStr = (String) row.get("event_amenities");
                List<String> eventAmenities = mapper.readValue(eventAmenitiesStr, List.class);
                eventData.put("eventAmenities", eventAmenities);
            } catch (Exception ex) {
                eventData.put("eventAmenities", new ArrayList<>());
            }

            Map<String, Object> hotelData = new HashMap<>();
            hotelData.put("hotel_ID", row.get("hotel_ID"));
            hotelData.put("name", row.get("hotel_name"));
            hotelData.put("type", row.get("hotel_type"));
            hotelData.put("destination", row.get("destination"));

            // ✅ Parse hotel amenities JSON → List
            try {
                String hotelAmenitiesStr = (String) row.get("hotel_amenities");
                List<String> hotelAmenities = mapper.readValue(hotelAmenitiesStr, List.class);
                hotelData.put("hotelAmenities", hotelAmenities);
            } catch (Exception ex) {
                hotelData.put("hotelAmenities", new ArrayList<>());
            }

                            // 🖼️ Extract only the first image
                            String image = (String) row.get("images");
                String firstImage = null;

                if (image != null && !image.isEmpty()) {
                     firstImage = baseUrl + image.trim().replace("\"", "");
                }

                hotelData.put("image", firstImage);

            Map<String, Object> finalData = new HashMap<>();
            finalData.put("event", eventData);
            finalData.put("hotel", hotelData);

            responseList.add(finalData);
        }

        return ApiResponse.success(responseList, "Events fetched successfully.");

    } catch (Exception e) {
        return ApiResponse.serverError("Error while searching events: " + e.getMessage());
    }
}
}
