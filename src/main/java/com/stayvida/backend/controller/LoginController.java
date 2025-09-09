package com.stayvida.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
// import com.stayvida.backend.dto.GoogleLoginRequest;
import com.stayvida.backend.dto.LoginRequest;
import com.stayvida.backend.dto.LoginResponse;

import com.stayvida.backend.repository.LoginUserRepository;
import com.stayvida.backend.service.JwtUtil;

// import com.google.api.client.http.javanet.NetHttpTransport;

import org.springframework.beans.factory.annotation.Value;



@RestController
@RequestMapping("/api")
public class LoginController {
    @Autowired
    private LoginUserRepository loginRepo;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${google.client.id}")
    private String CLIENT_ID;

    @Value("${google.client.secret}")
    private String CLIENT_SECRET;
    // private String CLIENT_ID;


   @PostMapping("/login")
public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
    String name = loginRepo.findUserNameIfValid(request.getEmail(), request.getPassword());

    if (name != null) {
        String token = jwtUtil.generateToken(request.getEmail());
        return ResponseEntity.ok(new LoginResponse(
            true,
            token,
            name,
            request.getEmail(),
            "Login Successful" // ✅ message
        ));
    } else {
        return ResponseEntity.ok(new LoginResponse(
            false,
            null,
            null,
            null,
            "Login Credential Mismatch or Login Error" // ✅ messages
        ));
    }
}
}