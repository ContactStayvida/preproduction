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
        user.setId(rs.getLong("user_ID")); 
        user.setEmail(rs.getString("email"));
        user.setUsername(rs.getString("username"));
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
    String sql = """
        INSERT INTO users (username, email, password, role, createdAt, updatedAt)
        VALUES (?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
            username = VALUES(username),
            password = VALUES(password),
            updatedAt = VALUES(updatedAt)
        """;

    jdbcTemplate.update(sql,
            user.getUsername(),
            user.getEmail(),
            user.getPassword(),
            user.getRole(),
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now());

    // ✅ Fetch latest record after saving (ensures correct ID)
    User savedUser = findByEmail(user.getEmail());
    if (savedUser != null) {
        user.setId(savedUser.getuserID());
    } else {
        System.out.println("⚠️ User not found after saveOrUpdate: " + user.getEmail());
    }
}

}
