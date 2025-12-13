package com.stayvida.backend.repository;

import com.stayvida.backend.model.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProfileRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Profile createOrUpdate(Profile profile) {

        String sql = """
                    INSERT INTO profile (user_ID, name, phone_number, address, bio, gender)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        name = VALUES(name),
                        phone_number = VALUES(phone_number),
                        address = VALUES(address),
                        bio = VALUES(bio),
                        gender = VALUES(gender),
                        updated_at = CURRENT_TIMESTAMP
                """;

        jdbcTemplate.update(
                sql,
                profile.getUserID(),
                profile.getName(),
                profile.getPhoneNumber(),
                profile.getAddress(),
                profile.getBio(),
                profile.getGender()

        );

        // return full profile including email + role
        return getProfile(profile.getUserID());
    }

    public Profile getProfile(Integer userId) {
        String sql = """
                    SELECT p.*, u.email, u.role
                    FROM profile p
                    JOIN users u ON p.user_ID = u.user_ID
                    WHERE p.user_ID = ?
                """;

        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                Profile p = new Profile();
                p.setUserID(rs.getInt("user_ID"));
                p.setName(rs.getString("name"));
                p.setPhoneNumber(rs.getString("phone_number"));
                p.setAddress(rs.getString("address"));
                p.setBio(rs.getString("bio"));
                p.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                p.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());

                // extra fields from users table
                p.setEmail(rs.getString("email"));
                p.setRole(rs.getString("role"));
                p.setGender(rs.getString("gender"));

                return p;
            }, userId);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return null; // return null when no data found
        }
    }

    public boolean profileExists(int userId) {
        String sql = "SELECT COUNT(*) FROM profile WHERE user_ID = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count != null && count > 0;
    }

}
