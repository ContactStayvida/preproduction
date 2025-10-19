package com.stayvida.backend.repository;

// import com.stayvida.backend.dto.SignupRequest;
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
        user.setId(rs.getLong("id"));
        user.setEmail(rs.getString("email"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        return user;
    };

    public User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try {
            return jdbcTemplate.queryForObject(sql, userRowMapper, email);
        } catch (Exception e) {
            return null; // not found
        }
    }

    public void save(User newUser) {
        String sql = "INSERT INTO users (email, username, password,role,createdAT,updatedAt) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,newUser.getEmail(), newUser.getUsername(), newUser.getPassword(),newUser.getRole(),java.time.LocalDateTime.now(),java.time.LocalDateTime.now());
    }
}
