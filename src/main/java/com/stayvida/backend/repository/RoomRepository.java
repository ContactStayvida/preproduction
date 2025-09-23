package com.stayvida.backend.repository;
import com.stayvida.backend.dto.RoomDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class RoomRepository  {

        @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<RoomDTO> getRoomsByHotelId(int hotelId) {
        String sql = "SELECT hotel_ID, room_ID, adults_MAX, children_MAX, type, price " +
                     "FROM stayvida.dbo.hotel_Room WHERE hotel_ID = ?";

        return jdbcTemplate.query(sql, new Object[]{hotelId}, new RowMapper<RoomDTO>() {
            @Override
            public RoomDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new RoomDTO(
                        rs.getInt("hotel_ID"),
                        rs.getInt("room_ID"),
                        rs.getInt("adults_MAX"),
                        rs.getInt("children_MAX"),
                        rs.getString("type"),
                        rs.getDouble("price")
                );
            }
        });
    }
}
