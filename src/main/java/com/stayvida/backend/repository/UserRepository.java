package com.stayvida.backend.repository;

import com.stayvida.backend.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
        User user = new User();
        // ✅ Make sure this matches your actual DB column name
        user.setId(rs.getInt("user_ID"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setRole(rs.getString("role"));
        return user;
    };

    public User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try {
            return jdbcTemplate.query(sql, userRowMapper, email)
                    .stream()
                    .findFirst()
                    .orElse(null);

        } catch (Exception e) {
            System.out.println("⚠️ findByEmail error: " + e.getMessage());
            return null;
        }

    }

    public void saveOrUpdate(User user) {
        // Check if user already exists
        User existingUser = findByEmail(user.getEmail());

        if (existingUser != null) {
            // 🚫 User already exists → don’t insert or update
            System.out.println("ℹ️ User already exists: " + user.getEmail());
            user.setId(existingUser.getuserID());
            return;
        }

        // ➕ Insert new user
        String insertSql = """
                INSERT INTO users (email, password, role, createdAt, updatedAt)
                VALUES ( ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(insertSql,
                user.getEmail(),
                user.getPassword(),
                user.getRole(),
                java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now());

        // ✅ Fetch the inserted record to update user ID
        User savedUser = findByEmail(user.getEmail());
        if (savedUser != null) {
            user.setId(savedUser.getuserID());
        }
    }

}
