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
                        h.destination,
                        r.price,
                        h.images,
                        h.hotel_ID
                    FROM hotels h
                    LEFT JOIN rooms r ON h.hotel_ID = r.hotel_ID
                    WHERE h.status = 'Verified'
                """;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

        Map<String, Map<String, Object>> locationMap = new LinkedHashMap<>();

        for (Map<String, Object> row : results) {

            String location = row.get("destination").toString();
            Integer price = (Integer) row.get("price");
            String image = row.get("images") != null ? row.get("images").toString() : null;

            locationMap.putIfAbsent(location, new LinkedHashMap<>());

            Map<String, Object> loc = locationMap.get(location);

            loc.put("location", location);
            loc.putIfAbsent("lowestPrice", price);
            loc.putIfAbsent("hotelCount", 0);
            loc.putIfAbsent("images", new ArrayList<String>());

            if (price != null && price < (Integer) loc.get("lowestPrice")) {
                loc.put("lowestPrice", price);
            }

            loc.put("hotelCount", (Integer) loc.get("hotelCount") + 1);

            if (image != null) {
                List<String> images = (List<String>) loc.get("images");
                images.add("data:image/jpeg;base64," + image);
            }
        }

        return new ArrayList<>(locationMap.values());
    }
}
