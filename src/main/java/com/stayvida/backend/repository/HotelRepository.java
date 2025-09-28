package com.stayvida.backend.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.lang.NonNull;
import com.stayvida.backend.model.Hotel;

import java.sql.ResultSet;
import java.sql.SQLException;
// import java.time.LocalDate;
import java.util.List;

@Repository
public class HotelRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Search hotels by location and check availability
    public List<Hotel> searchHotels(String destination, String checkIn, String checkOut, int adultCapacity, int childrenCapacity) {
        String sql =  "SELECT h.*, " +
            "   (SELECT MIN(price) FROM hotel_room r WHERE r.hotel_id = h.hotel_id) AS lowest_price " +
            "FROM hotels h " +
            "WHERE h.location = ?"+" AND h.varification_status = 'verified'";
        List<Hotel> hotels = jdbcTemplate.query(
    sql,
        new RowMapper<Hotel>() {
            @Override
            public Hotel mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
                Hotel hotel = new Hotel();
                hotel.setId(rs.getInt("hotel_ID"));
                hotel.setHotel(rs.getString("hotel"));
                hotel.setLocation(rs.getString("location"));
                // hotel.setPrice(rs.getInt("price"));
                hotel.setAdult(rs.getInt("max_adults"));
                hotel.setchildren(rs.getInt("max_children"));
                hotel.setImagePath(rs.getString("image_url")); // ✅ Only filename, not blob
                hotel.setRating(rs.getDouble("rating")); // Set rating
                hotel.setdescription(rs.getString("description")); // Set description

                return hotel;
            }
        },
        destination // <-- this is passed as a vararg (instead of Object[])
);

        // Check availability for each hotel
        for (Hotel hotel : hotels) {
            boolean isAvailable = isHotelAvailable(hotel.getId(), checkIn, checkOut);
            hotel.setAvailability(isAvailable);

                // 👇 here you pass hotel.getId() to the method
            List<String> amenities = getAmenitiesForHotel(hotel.getId());
            hotel.setAmenities(amenities);
        }

        return hotels;
    }

    public List<String> getAmenitiesForHotel(int hotelId) {
    String sql = "SELECT A.name " +
                 "FROM amenity AS A " +
                 "INNER JOIN hotel_amenity AS B ON A.amenity_id = B.amenity_id " +
                 "WHERE B.hotel_id = ?";

    return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("name"), hotelId);
}


public boolean isHotelAvailable(int hotelId, String checkIn, String checkOut) {
    String sql = "SELECT COUNT(*) FROM bookings " +
                 "WHERE hotel_id = ? " +
                 "AND check_in_date < ? " +
                 "AND check_out_date > ?";

    Integer count = jdbcTemplate.queryForObject(
        sql, Integer.class, hotelId, checkOut, checkIn
    );

    return count == null || count == 0;
}




    public int updateVerificationStatus(int hotelId, String status) {
        String sql = "UPDATE hotels SET varification_status = ? WHERE hotel_id = ?";
        return jdbcTemplate.update(sql, status, hotelId);
    }
}
