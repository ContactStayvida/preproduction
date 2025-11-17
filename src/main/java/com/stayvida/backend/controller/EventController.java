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


// ✅ New endpoint to fetch event + hotel details by event_ID and hotel_ID
@GetMapping("/details")
public Object getEventDetails(@RequestParam String eventID, @RequestParam Integer hotelID) {
    try {
        String sql = """
            SELECT 
                e.event_ID, e.eventType, e.amount, e.guestCount, e.amenities AS event_amenities, e.createdAt AS eventCreatedAt, e.updatedAt AS eventUpdatedAt,
                h.hotel_ID, h.owner_ID, h.name AS hotel_name, h.type AS hotel_type, h.destination, h.isForEvent, h.description, h.phone_NO,
                h.tags AS hotel_tags, h.images AS hotel_images, h.amenities AS hotel_amenities, h.longitude, h.latitude, h.status, 
                h.onArrivalPayment, h.remark, h.createdAt AS hotelCreatedAt, h.updatedAt AS hotelUpdatedAt
            FROM events e
            INNER JOIN hotels h ON e.hotel_ID = h.hotel_ID
            WHERE e.event_ID = ? AND h.hotel_ID = ?
        """;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, eventID, hotelID);

        if (results.isEmpty()) {
            return ApiResponse.notFound("No event or hotel found with the given IDs.");
        }

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> row = results.get(0);

        // Event data
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("event_ID", row.get("event_ID"));
        eventData.put("eventType", row.get("eventType"));
        eventData.put("amount", row.get("amount"));
        eventData.put("guestCount", row.get("guestCount"));
        eventData.put("createdAt", row.get("eventCreatedAt"));
        eventData.put("updatedAt", row.get("eventUpdatedAt"));

        // Parse event amenities JSON
        try {
            String eventAmenitiesStr = (String) row.get("event_amenities");
            List<String> eventAmenities = mapper.readValue(eventAmenitiesStr, List.class);
            eventData.put("amenities", eventAmenities);
        } catch (Exception ex) {
            eventData.put("amenities", new ArrayList<>());
        }

        // Hotel data
        Map<String, Object> hotelData = new HashMap<>();
        hotelData.put("hotel_ID", row.get("hotel_ID"));
        hotelData.put("owner_ID", row.get("owner_ID"));
        hotelData.put("name", row.get("hotel_name"));
        hotelData.put("type", row.get("hotel_type"));
        hotelData.put("destination", row.get("destination"));
        hotelData.put("isForEvent", row.get("isForEvent"));
        hotelData.put("description", row.get("description"));
        hotelData.put("phone_NO", row.get("phone_NO"));
        hotelData.put("longitude", row.get("longitude"));
        hotelData.put("latitude", row.get("latitude"));
        hotelData.put("status", row.get("status"));
        hotelData.put("onArrivalPayment", row.get("onArrivalPayment"));
        hotelData.put("remark", row.get("remark"));
        hotelData.put("createdAt", row.get("hotelCreatedAt"));
        hotelData.put("updatedAt", row.get("hotelUpdatedAt"));

        // Parse hotel tags
        try {
            String tagsStr = (String) row.get("hotel_tags");
            List<String> tags = mapper.readValue(tagsStr, List.class);
            hotelData.put("tags", tags);
        } catch (Exception ex) {
            hotelData.put("tags", new ArrayList<>());
        }

        // Parse hotel amenities
        try {
            String hotelAmenitiesStr = (String) row.get("hotel_amenities");
            List<String> hotelAmenities = mapper.readValue(hotelAmenitiesStr, List.class);
            hotelData.put("amenities", hotelAmenities);
        } catch (Exception ex) {
            hotelData.put("amenities", new ArrayList<>());
        }

        // Handle hotel images (add base URL prefix)
        try {
            String imageStr = (String) row.get("hotel_images");
            List<String> fullUrls = new ArrayList<>();

            if (imageStr != null && !imageStr.isEmpty()) {
                // Single image, just prepend baseUrl
                fullUrls.add(baseUrl + imageStr.trim());
            }

            hotelData.put("images", fullUrls);

        } catch (Exception ex) {
            hotelData.put("images", new ArrayList<>());
        }

        // Final response
        Map<String, Object> finalData = new HashMap<>();
        finalData.put("event", eventData);
        finalData.put("hotel", hotelData);

        return ApiResponse.success(finalData, "Event and hotel details fetched successfully.");

    } catch (Exception e) {
        return ApiResponse.serverError("Error while fetching event and hotel details: " + e.getMessage());
    }
}



}       
