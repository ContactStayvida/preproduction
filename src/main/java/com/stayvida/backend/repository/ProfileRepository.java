package com.stayvida.backend.repository;

import com.stayvida.backend.model.Profile;

import java.sql.Timestamp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProfileRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Profile createOrUpdate(Profile profile) {
        Timestamp now = new Timestamp(System.currentTimeMillis());

        String sql = """
                    INSERT INTO profile (user_ID, name, phone_number)
                    VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        name = VALUES(name),
                        phone_number = VALUES(phone_number),
                        updated_at = ?
                """;

        jdbcTemplate.update(
                sql,
                profile.getUserID(),
                profile.getName(),
                profile.getPhoneNumber(),
                now

        );

        // return full profile including email + role
        return getProfile(profile.getUserID());
    }

    public Profile getProfile(Integer userId) {

        String sql = """
                    SELECT u.user_ID,
                           u.email,
                           u.role,
                           p.name,
                           p.phone_number,
                           p.created_at,
                           p.updated_at
                    FROM users u
                    LEFT JOIN profile p ON u.user_ID = p.user_ID
                    WHERE u.user_ID = ?
                """;

        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {

                Profile p = new Profile();
                p.setUserID(rs.getInt("user_ID"));

                // These ALWAYS exist
                p.setEmail(rs.getString("email"));
                p.setRole(rs.getString("role"));

                // Profile fields may be null
                String name = rs.getString("name");
                String phone = rs.getString("phone_number");

                if (name == null) {
                    p.setName("Random Pappu");
                } else {
                    p.setName(name);
                }

                if (phone == null) {
                    p.setPhoneNumber("80085 80085");
                } else {
                    p.setPhoneNumber(phone);
                }

                Timestamp created = rs.getTimestamp("created_at");
                if (created != null) {
                    p.setCreatedAt(created.toLocalDateTime());
                }

                Timestamp updated = rs.getTimestamp("updated_at");
                if (updated != null) {
                    p.setUpdatedAt(updated.toLocalDateTime());
                }

                return p;
            }, userId);

        } catch (EmptyResultDataAccessException e) {
            return null; // means user itself not found
        }
    }

    public boolean profileExists(int userId) {
        String sql = "SELECT COUNT(*) FROM profile WHERE user_ID = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count != null && count > 0;
    }

    public Profile partialUpdate(int userID, Profile profile) {
        Timestamp now = new Timestamp(System.currentTimeMillis());

        String sql = """
                UPDATE profile
                SET
                    name = COALESCE(?, name),
                    phone_number = COALESCE(?, phone_number),
                    updated_at = ?
                WHERE user_ID = ?
                """;

        int updatedRows = jdbcTemplate.update(
                sql,
                profile.getName(),
                profile.getPhoneNumber(),

                now,
                userID);

        if (updatedRows == 0) {
            return null;
        }

        return getProfile(profile.getUserID());
    }

}
