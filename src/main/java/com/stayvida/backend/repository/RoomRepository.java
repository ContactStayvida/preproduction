package com.stayvida.backend.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stayvida.backend.dto.HotelDTO;
import com.stayvida.backend.dto.RoomDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RoomRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String BASE_URL = "http://localhost:8080/image/";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HotelDTO getRoomsByHotelId(int hotelId, String checkIn, String checkOut) {
        LocalDate checkInDate = LocalDate.parse(checkIn);
        LocalDate checkOutDate = LocalDate.parse(checkOut);

        // --- Fetch Hotel Info ---
        String hotelSql = """
                SELECT hotel_ID, name, description, rating, destination,
                       onArrivalPayment, isForEvent, tags, images
                FROM hotels
                WHERE hotel_ID = ?
                """;

        HotelDTO hotel = jdbcTemplate.queryForObject(hotelSql, new Object[]{hotelId}, (ResultSet rs, int rowNum) -> {
            HotelDTO dto = new HotelDTO();
            dto.setHotelId(rs.getInt("hotel_ID"));
            dto.setName(rs.getString("name"));
            dto.setDescription(rs.getString("description"));
            dto.setRating(rs.getDouble("rating"));
            dto.setDestination(rs.getString("destination"));
            dto.setOnArrivalPayment(rs.getBoolean("onArrivalPayment"));
            dto.setForEvent(rs.getBoolean("isForEvent"));

            // --- Parse tags ---
            String tagsJson = rs.getString("tags");
            if (tagsJson != null && !tagsJson.isEmpty()) {
                try {
                    dto.setTags(objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {}));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // --- Parse hotel images ---
            String imagesStr = rs.getString("images");
            if (imagesStr != null && !imagesStr.isEmpty()) {
                List<String> imgs = new ArrayList<>();
                try {
                    imgs = objectMapper.readValue(imagesStr, new TypeReference<List<String>>() {});
                } catch (Exception e) {
                    String[] parts = imagesStr.split("\\s*,\\s*");
                    for (String p : parts) {
                        if (!p.isEmpty()) imgs.add(p);
                    }
                }
                imgs = imgs.stream()
                        .filter(img -> img != null && !img.isEmpty())
                        .map(img -> BASE_URL + img)
                        .toList();
                dto.setImages(imgs);
            }
            return dto;
        });

        // --- Fetch Only Available Rooms ---
        String roomSql = """
            SELECT r.room_ID, r.hotel_ID, r.room_Type, r.features, r.images,
                   r.price, r.max_adults, r.max_children, r.bed_count
            FROM rooms r
            WHERE r.hotel_ID = ?
              AND (
                  -- Available if no active overlapping booking
                  NOT EXISTS (
                      SELECT 1 FROM bookings b
                      WHERE b.room_ID = r.room_ID
                        AND b.booking_Status <> 'Cancelled'
                        AND b.checkIn < ?
                        AND b.checkOut > ?
                  )
                  -- Or booking is cancelled within the range
                  OR EXISTS (
                      SELECT 1 FROM bookings b
                      WHERE b.room_ID = r.room_ID
                        AND (b.booking_Status = 'Cancelled' OR b.booking_Status = 'CheckedOut')
                        AND b.checkIn < ?
                        AND b.checkOut > ?
                  )
              )
            """;

        List<RoomDTO> rooms = jdbcTemplate.query(
                roomSql,
                new Object[]{hotelId, checkOutDate, checkInDate, checkOutDate, checkInDate},
                (ResultSet rs, int rowNum) -> {
                    RoomDTO room = new RoomDTO(
                            rs.getString("room_ID"),
                            rs.getInt("hotel_ID"),
                            rs.getString("room_Type"),
                            rs.getDouble("price"),
                            rs.getInt("max_adults"),
                            rs.getInt("max_children"),
                            rs.getInt("bed_count")
                    );

                    // --- Parse features ---
                    String featuresJson = rs.getString("features");
                    if (featuresJson != null && !featuresJson.isEmpty()) {
                        try {
                            room.setFeatures(objectMapper.readValue(featuresJson, new TypeReference<List<String>>() {}));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    // --- Parse images ---
                    String roomImagesJson = rs.getString("images");
                    if (roomImagesJson != null && !roomImagesJson.isEmpty()) {
                        try {
                            List<String> imgs = objectMapper.readValue(roomImagesJson, new TypeReference<List<String>>() {});
                            imgs = imgs.stream()
                                    .filter(img -> img != null && !img.isEmpty())
                                    .map(img -> BASE_URL + img)
                                    .toList();
                            room.setRoomImages(imgs);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    return room;
                });

        hotel.setRooms(rooms);
        return hotel;
    }
}
