package com.stayvida.backend.repository;

import com.stayvida.backend.dto.RegisterRoom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.Random;

@Repository
public class RoomregisterRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public int saveRoom(RegisterRoom registerRoom) {
        // Generate random 3-digit room_ID (100–999)
        int roomId = new Random().nextInt(900) + 100;

        String sql = "INSERT INTO hotel_room (room_ID, hotel_ID, adults_max, children_max, type, price) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, roomId); // set generated room_ID
            ps.setInt(2, registerRoom.getHotelId());
            ps.setInt(3, registerRoom.getAdultsMax());
            ps.setInt(4, registerRoom.getChildrenMax());
            ps.setString(5, registerRoom.getType());
            ps.setInt(6, registerRoom.getPrice());
            return ps;
        });

        return roomId; // return generated room_ID
    }
}
