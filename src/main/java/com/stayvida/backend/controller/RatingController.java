package com.stayvida.backend.controller;

import com.stayvida.backend.model.Rating;
import com.stayvida.backend.repository.RatingRepository;
import com.stayvida.backend.security.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rating")
public class RatingController {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    RatingRepository ratingRepository;

    @PostMapping("/create")
    public Object createRating(@RequestBody Map<String, Object> body) {
        try {
            // ✅ Extract JSON data
            String bookingId = (String) body.get("booking_id");
            // int userId = (int) body.get("user_id");
            int userId = (int) SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal();
            String hotelId = (String) body.get("hotel_id");
            double ratingValue = Double.parseDouble(body.get("rating_value").toString());
            String comment = (String) body.get("comment");

            System.out.println("This is user id " + userId);
            System.out.println(((Object) userId).getClass().getSimpleName());

            // ✅ SQL query
            String sql = "INSERT INTO rating (booking_ID, user_ID, hotel_ID, rating_value, comment, rated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

            // ✅ Execute query
            int rows = jdbcTemplate.update(sql, bookingId, userId, hotelId, ratingValue, comment,
                    Timestamp.from(Instant.now()));

            if (rows > 0) {
                return ApiResponse.created(
                        Map.of(
                                "booking_ID", bookingId,
                                "hotel_ID", hotelId,
                                "rating_value", ratingValue,
                                "comment", comment),
                        "Rating created successfully");
            } else {
                return ApiResponse.badRequest("Failed to insert rating");
            }

        } catch (org.springframework.dao.DuplicateKeyException e) {
            return ApiResponse.badRequest("Rating already exists for this booking and hotel");
        } catch (Exception e) {
            return ApiResponse.serverError("Error creating rating: " + e.getMessage());
        }
    }

    // @GetMapping("/hotel/{hotelId}")
    // public ResponseEntity<?> getRatingsByHotel(@PathVariable String hotelId) {
    // List<Rating> ratings = ratingRepository.findAllByHotelId(hotelId);
    // Double avgRating = ratingRepository.findAverageRatingByHotelId(hotelId);
    // if (avgRating == null)
    // avgRating = 0.0;

    // Map<String, Object> response = new HashMap<>();
    // response.put("ratings", ratings);
    // response.put("averageRating", avgRating);

    // return ResponseEntity.ok(response);
    // }

    @GetMapping("/hotel")
    public ResponseEntity<?> getRatings(
            @RequestParam(required = false) String hotelId) {

        List<Rating> ratings;
        Map<String, Object> response = new HashMap<>();

        if (hotelId == null || hotelId.isBlank()) {

            // Fetch all ratings
            ratings = ratingRepository.findAll();
            response.put("ratings", ratings);

        } else {

            // Fetch ratings for specific hotel
            ratings = ratingRepository.findAllByHotelId(hotelId);
            Double avgRating = ratingRepository.findAverageRatingByHotelId(hotelId);

            if (avgRating == null) {
                avgRating = 0.0;
            }

            response.put("ratings", ratings);
            response.put("averageRating", avgRating);
        }

        return ResponseEntity.ok(response);
    }

}
