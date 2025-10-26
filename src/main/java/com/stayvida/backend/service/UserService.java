package com.stayvida.backend.service;

import com.stayvida.backend.model.User;
import com.stayvida.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public void registerUser(String email, String username, String rawPassword, String role) {
        // Optional pre-check
        if (userRepository.findByEmail(email) != null) {
            throw new RuntimeException("USER_ALREADY_EXISTS");
        }

        if (role == null || role.isEmpty()) {
            role = "USER"; // Default role
        }

        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);

        try {
            userRepository.save(user); // Attempt to save user
        } catch (DuplicateKeyException e) {
            // Catch database unique constraint violation
            throw new RuntimeException("USER_ALREADY_EXISTS");
        }
    }
}
