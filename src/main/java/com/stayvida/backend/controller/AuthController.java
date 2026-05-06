package com.stayvida.backend.controller;

import com.stayvida.backend.dto.AuthRequest;
import com.stayvida.backend.service.SupabaseAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth") // superbase auth routes for admin dashboard
public class AuthController {

    @Autowired
    private SupabaseAuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        return authService.signUp(request);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        return authService.signIn(request);
    }
}
