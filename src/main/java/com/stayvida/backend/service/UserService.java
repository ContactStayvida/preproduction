package com.stayvida.backend.service;
import com.stayvida.backend.model.User;

// import org.checkerframework.checker.units.qual.A
import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.stayvida.backend.repository.*;
// import com.stayvida.backend.dto.SignupRequest;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    // @Autowired
    // private SignupRequest sigb;

    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public void registerUser(String email, String username, String rawPassword, String role) {
        if (userRepository.findByEmail(email) != null) {
            throw new RuntimeException("Email already in use");
        }

        if (role == null || role.isEmpty()) {
            role = "USER"; // Default role
        }

        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role); // Default role

        userRepository.save(user);
    }
}
