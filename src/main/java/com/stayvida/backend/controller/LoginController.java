package com.stayvida.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.stayvida.backend.dto.LoginRequest;
import com.stayvida.backend.dto.LoginResponse;
import com.stayvida.backend.repository.LoginUserRepository;
import com.stayvida.backend.service.JwtUtil;

@RestController
@RequestMapping("/api")
public class LoginController {

    @Autowired
    private LoginUserRepository loginRepo;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        String result = loginRepo.findUserNameIfValid(request.getEmail(), request.getPassword());

        if (result != null) {
            // Split the result string (username,email,role)
            String[] parts = result.split(",");
            String username = parts[0];
            String email = parts[1];
            String role = parts[2];

            // Generate JWT token
            String token = jwtUtil.generateToken(email);

            // ✅ Correct order of parameters
            return ResponseEntity.ok(new LoginResponse(
                true,
                token,
                username,
                email,
                role,
                "Login Successful "+ role + ": " + username
            ));
        } else {
            return ResponseEntity.ok(new LoginResponse(
                false,
                null,
                null,
                null,
                "Login Credential Mismatch or Login Error",
                null
            ));
        }
    }
}
