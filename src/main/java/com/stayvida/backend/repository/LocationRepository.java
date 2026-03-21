package com.stayvida.backend.repository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LocationRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<String> searchDestinations(String keyword) {
        String sql = """
                    SELECT DISTINCT destination
                    FROM hotels
                    WHERE LOWER(destination) LIKE LOWER(?)
                    AND status = 'Verified'
                    LIMIT 10
                """;

        return jdbcTemplate.query(
                sql,
                new Object[] { keyword + "%" },
                (rs, rowNum) -> rs.getString("destination"));
    }
}