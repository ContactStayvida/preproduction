package com.stayvida.backend.repository;

import com.stayvida.backend.dto.RatingRequest;
import com.stayvida.backend.model.Rating;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class RatingRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<Rating> ratingRowMapper = (rs, rowNum) -> {
        Rating rating = new Rating();
        rating.setRating_ID(rs.getInt("rating_ID"));
        rating.setUser_ID(rs.getInt("user_ID"));
        rating.setUser_Name(rs.getString("name"));
        rating.setHotel_ID(rs.getString("hotel_ID"));
        rating.setBooking_ID(rs.getString("booking_ID"));
        rating.setRating_Value(rs.getDouble("rating_Value"));
        rating.setComment(rs.getString("comment"));
        rating.setRated_at(rs.getTimestamp("rated_at").toLocalDateTime());
        return rating;
    };

    // Fetch all ratings for a hotel
    public List<Rating> findAllByHotelId(String hotelId) {
        String sql = "SELECT r.*, p.name FROM rating r INNER JOIN profile p ON r.user_ID = p.user_ID WHERE r.hotel_ID = ? ";
        return jdbcTemplate.query(sql, ratingRowMapper, hotelId);
    }

    // Fetch average rating rounded to 1 decimal
    public Double findAverageRatingByHotelId(String hotelId) {
        String sql = "SELECT ROUND(AVG(rating_Value),1) FROM rating WHERE r.hotel_ID = ?";
        return jdbcTemplate.queryForObject(sql, Double.class, hotelId);
    }

    public Double findAverageRating() {
        String sql = "SELECT ROUND(AVG(rating_Value),1) FROM rating";
        return jdbcTemplate.queryForObject(sql, Double.class);
    }

    public List<Rating> findAll() {
        String sql = "SELECT r.*,p.name FROM rating r INNER JOIN profile p ON r.user_ID = p.user_ID";
        return jdbcTemplate.query(sql, ratingRowMapper);
    }

    // Save a new rating using RatingRequest
    public void saveRating(RatingRequest request) {
        String sql = """
                    INSERT INTO ratings (user_ID, hotel_ID, booking_ID, rating_Value, comment, rated_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                """;

        java.time.LocalDateTime timestamp = request.getRated_at() != null
                ? request.getRated_at()
                : java.time.LocalDateTime.now(); // fallback

        jdbcTemplate.update(sql,
                request.getUserId().intValue(),
                request.getHotelId(),
                request.getBookingId(),
                request.getRatingValue(),
                request.getComment(),
                Timestamp.valueOf(timestamp));
    }
}
