package com.stayvida.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/events")
public class ListOfEventController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${app.base.url}")
    private String baseUrl; // 🌐 Prefix for image URLs

    @GetMapping("/list")
    public Object getAllEventLocations() {
        String sql = """
            SELECT 
                h.destination AS location,
                e.eventType AS event_type,
                MIN(e.amount) AS lowest_price,
                GROUP_CONCAT(DISTINCT h.images) AS all_images
            FROM hotels h
            JOIN events e ON h.hotel_ID = e.hotel_ID
            WHERE h.status = 'Verified'
            GROUP BY h.destination, e.eventType
            ORDER BY h.destination, lowest_price ASC
        """;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

        // 📦 Group by location
        Map<String, List<Map<String, Object>>> groupedByLocation = new LinkedHashMap<>();

        for (Map<String, Object> row : results) {
            String location = (String) row.get("location");
            String eventType = (String) row.get("event_type");
            Double lowestPrice = row.get("lowest_price") != null
                    ? ((Number) row.get("lowest_price")).doubleValue()
                    : null;

            // 🖼️ Since each hotel has one image, just split GROUP_CONCAT
            List<String> imageList = new ArrayList<>();
            String imageConcat = (String) row.get("all_images");
            if (imageConcat != null) {
                for (String img : imageConcat.split(",")) {
                    String trimmed = img.trim();
                    if (!trimmed.isEmpty()) {
                        imageList.add(baseUrl + trimmed);
                    }
                }
            }

            // 🧩 Event data
            Map<String, Object> eventData = new LinkedHashMap<>();
            eventData.put("eventType", eventType);
            eventData.put("lowestPrice", lowestPrice);
            eventData.put("images", imageList);

            groupedByLocation.computeIfAbsent(location, k -> new ArrayList<>()).add(eventData);
        }

        // 🏁 Final formatted response
        List<Map<String, Object>> response = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : groupedByLocation.entrySet()) {
            Map<String, Object> loc = new LinkedHashMap<>();
            loc.put("location", entry.getKey());
            loc.put("events", entry.getValue());
            response.add(loc);
        }

        return response;
    }
}
