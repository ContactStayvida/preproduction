package com.stayvida.backend.service;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.stayvida.backend.dto.UserListDTO;
import com.stayvida.backend.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    public UserService(UserRepository userRepository, JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<UserListDTO> getUserList() {
        return userRepository.fetchUserList();
    }

    public int findOrCreateUser(String email) {

        String checkSql = "SELECT user_ID FROM users WHERE email = ?";

        List<Integer> users = jdbcTemplate.query(
                checkSql,
                (rs, rowNum) -> rs.getInt("user_ID"),
                email);

        if (!users.isEmpty()) {
            return users.get(0);
        }

        String insertSql = """
                INSERT INTO users (email, role,password, createdAt, updatedAt)
                VALUES (?, 'user','login_at_hotel', NOW(), NOW())
                """;

        jdbcTemplate.update(insertSql, email);

        Integer userId = jdbcTemplate.queryForObject(
                "SELECT user_ID FROM users WHERE email = ?",
                Integer.class,
                email);

        return userId;
    }
}
