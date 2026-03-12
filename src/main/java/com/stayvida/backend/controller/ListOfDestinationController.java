package com.stayvida.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/locations")
public class ListOfDestinationController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/list")
    public List<Map<String, Object>> getAllLocations() {

        String sql = """
                    SELECT
                        MIN(h.destination) AS location,
                        MIN(r.price) AS lowest_price,
                        GROUP_CONCAT(DISTINCT h.images SEPARATOR '|||') AS all_images,
                        COUNT(DISTINCT h.hotel_ID) AS hotel_count
                    FROM hotels h
                    LEFT JOIN rooms r ON h.hotel_ID = r.hotel_ID
                    WHERE h.status = 'Verified'
                    GROUP BY LOWER(LEFT(h.destination, 3))
                    ORDER BY lowest_price ASC
                """;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
        List<Map<String, Object>> response = new ArrayList<>();

        for (Map<String, Object> row : results) {

            Map<String, Object> locationData = new LinkedHashMap<>();

            locationData.put("location", row.get("location"));
            locationData.put("lowestPrice", row.get("lowest_price"));
            locationData.put("hotelCount", row.get("hotel_count"));

            // 👇 Convert images to array
            List<String> images = new ArrayList<>();
            Object allImagesObj = row.get("all_images");

            if (allImagesObj != null) {
                String[] imageArray = allImagesObj.toString().split("\\|\\|\\|");

                for (String img : imageArray) {
                    images.add("data:image/jpeg;base64," + img);
                }
            }

            locationData.put("images", images);
            response.add(locationData);
        }

        return response;
    }
}
