package com.stayvida.backend.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;

@Repository
public class LoginUserRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

        public String findUserNameIfValid(String email, String rawPassword) {
        try {
            String sql = "SELECT * FROM users WHERE email = ?";

            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                String hashedPassword = rs.getString("password");
                String username = rs.getString("username");
                String emailFromDb = rs.getString("email");
                String role = rs.getString("role");

                if (passwordEncoder.matches(rawPassword, hashedPassword)) {
                    // return username
                    return username + "," + emailFromDb + "," + role;
                    

                } else {
                    return null; // Password mismatch
                }
            }, email);

        } catch (EmptyResultDataAccessException e) {
            return null; // No user found
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
