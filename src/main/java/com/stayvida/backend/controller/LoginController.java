package com.stayvida.backend.controller;

import com.stayvida.backend.dto.LoginRequest;
import com.stayvida.backend.dto.LoginResponse;
import com.stayvida.backend.model.User;
import com.stayvida.backend.repository.UserRepository;
import com.stayvida.backend.service.EmailService;
import com.stayvida.backend.service.JwtUtil;
import com.stayvida.backend.service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/otplogin")
public class LoginController {

    @Autowired private UserRepository userRepo;
    @Autowired private OtpService otpService;
    @Autowired private EmailService emailService;
    @Autowired private JwtUtil jwtUtil;

    // Step 1️⃣: Generate and send OTP
    @PostMapping("/get-otp")
    public ResponseEntity<?> sendOtp(@RequestBody LoginRequest request) {
        String email = request.getEmail();
        String username = request.getUsername();

        if (email == null || username == null) {
            return ResponseEntity.badRequest().body("Email and Username are required");
        }

        String otp = otpService.generateOtp(email);
        emailService.sendOtpEmail(email, otp);
        return ResponseEntity.ok("OTP sent successfully to " + email);
    }

    // Step 2️⃣: Verify OTP and login / signup
    @PostMapping("/verify-otp")
    public ResponseEntity<LoginResponse> verifyOtp(@RequestBody LoginRequest request) {
        String email = request.getEmail();
        String username = request.getUsername();
        String otp = request.getOtp();

        // ❌ Invalid OTP
        if (!otpService.validateOtp(email, otp)) {
            return ResponseEntity.badRequest().body(
                new LoginResponse(false, null, null, email, null, "Invalid or expired OTP")
            );
        }

        // 🔍 Check existing user
        User user = userRepo.findByEmail(email);
        boolean isNew = (user == null);

        if (isNew) {
            user = new User();
            user.setEmail(email);
        }

        // ✅ Always update latest info
        user.setUsername(username);
        user.setPassword("OTP_LOGIN");
        user.setRole("user");

        userRepo.saveOrUpdate(user);

        System.out.println(isNew ? "✅ New user created: " + email : "✅ Existing user updated: " + email);

        // 🧾 Generate JWT
        String token = jwtUtil.generateToken(email);

        // 🎯 Return success response
        return ResponseEntity.ok(new LoginResponse(
                true,
                token,
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                "Login successful!"
        ));
    }
}
