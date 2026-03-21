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
                    h.destination AS location,
                    rp.lowest_price,
                    h.images,
                    h.hotel_ID
                FROM hotels h
                LEFT JOIN (
                    SELECT hotel_ID, MIN(price) AS lowest_price
                    FROM rooms
                    GROUP BY hotel_ID
                ) rp ON h.hotel_ID = rp.hotel_ID
                WHERE h.status = 'Verified'
                ORDER BY rp.lowest_price ASC;
                                """;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

        Map<String, Map<String, Object>> locationMap = new LinkedHashMap<>();

        for (Map<String, Object> row : results) {

            String location = row.get("location").toString();
            Object priceObj = row.get("price");
            Object imageObj = row.get("images");

            locationMap.putIfAbsent(location, new LinkedHashMap<>());

            Map<String, Object> locationData = locationMap.get(location);

            locationData.putIfAbsent("location", location);
            locationData.putIfAbsent("lowestPrice", priceObj);
            locationData.putIfAbsent("hotelCount", 0);
            locationData.putIfAbsent("images", new ArrayList<String>());

            // update lowest price
            if (priceObj != null) {
                Number price = (Number) priceObj;
                Number current = (Number) locationData.get("lowestPrice");

                if (current == null || price.doubleValue() < current.doubleValue()) {
                    locationData.put("lowestPrice", price);
                }
            }

            // increment hotel count
            locationData.put("hotelCount", ((Integer) locationData.get("hotelCount")) + 1);

            // add image
            if (imageObj != null) {
                List<String> images = (List<String>) locationData.get("images");
                images.add("data:image/jpeg;base64," + imageObj.toString());
            }
        }

        return new ArrayList<>(locationMap.values());
    }
}
