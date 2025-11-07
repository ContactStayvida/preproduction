package com.stayvida.backend.controller;

import com.stayvida.backend.security.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/rating")
public class RatingController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/create")
    public Object createRating(@RequestBody Map<String, Object> body) {
        try {
            // ✅ Extract JSON data
            String bookingId = (String) body.get("booking_id");
            int userId = (int) body.get("user_id");
            int hotelId = (int) body.get("hotel_id");
            double ratingValue = Double.parseDouble(body.get("rating_value").toString());
            String comment = (String) body.get("comment");

            // ✅ SQL query
            String sql = "INSERT INTO rating (booking_ID, user_ID, hotel_ID, rating_value, comment, rated_at) " +
                         "VALUES (?, ?, ?, ?, ?, ?)";

            // ✅ Execute query
            int rows = jdbcTemplate.update(sql, bookingId, userId, hotelId, ratingValue, comment, Timestamp.from(Instant.now()));

            if (rows > 0) {
                return ApiResponse.created(
                        Map.of(
                                "booking_ID", bookingId,
                                "hotel_ID", hotelId,
                                "rating_value", ratingValue,
                                "comment", comment
                        ),
                        "Rating created successfully"   
                );
            } else {
                return ApiResponse.badRequest("Failed to insert rating");
            }

        } catch (org.springframework.dao.DuplicateKeyException e) {
            return ApiResponse.badRequest("Rating already exists for this booking and hotel");
        } catch (Exception e) {
            return ApiResponse.serverError("Error creating rating: " + e.getMessage());
        }
    }
}
