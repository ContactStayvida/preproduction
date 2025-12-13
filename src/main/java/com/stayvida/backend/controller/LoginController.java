package com.stayvida.backend.controller;

import com.stayvida.backend.dto.LoginRequest;
import com.stayvida.backend.dto.LoginResponse;
import com.stayvida.backend.model.User;
import com.stayvida.backend.repository.ProfileRepository;
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

    @Autowired
    private UserRepository userRepo;
    @Autowired
    private OtpService otpService;
    @Autowired
    private EmailService emailService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private ProfileRepository profileRepo;
    Long nullId = null;

    // Step 1️⃣: Generate and send OTP
    @PostMapping("/get-otp")
    public ResponseEntity<?> sendOtp(@RequestBody LoginRequest request) {
        String email = request.getEmail();

        if (email == null) {
            return ResponseEntity.badRequest().body("Email is required");
        }

        String otp = otpService.generateOtp(email);
        emailService.sendOtpEmail(email, otp);
        return ResponseEntity.ok("OTP sent successfully to " + email);
    }

    // Step 2️⃣: Verify OTP and login / signup
    @PostMapping("/verify-otp")
    public ResponseEntity<LoginResponse> verifyOtp(@RequestBody LoginRequest request) {
        String email = request.getEmail();
        String otp = request.getOtp();

        // ❌ Invalid OTP
        if (!otpService.validateOtp(email, otp)) {
            return ResponseEntity.badRequest().body(new LoginResponse(
                    false,
                    null,
                    0,
                    email,
                    null,
                    "Invalid or expired OTP"));
        }

        // 🔍 Check if user exists
        User user = userRepo.findByEmail(email);
        boolean isNew = (user == null);

        // 🆕 Insert new user only if not exists
        if (isNew) {
            user = new User();
            user.setEmail(email);
            user.setPassword("OTP_LOGIN");
            user.setRole("user");
            userRepo.saveOrUpdate(user);
        } else {
            System.out.println("Existing user login: " + email);
        }

        // ⭐ ADD THIS: Check if profile exists
        boolean profileExists = profileRepo.profileExists(user.getuserID());

        // 🧾 Generate JWT
        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getuserID(),
                user.getRole());

        // 🎯 Return success response
        LoginResponse response = new LoginResponse(
                true,
                token,
                user.getuserID(),
                user.getEmail(),
                user.getRole(),
                isNew ? "Signup successful!" : "Login successful!");

        // ⭐ ADD THIS: include result in response
        response.setProfileExists(profileExists);

        return ResponseEntity.ok(response);
    }

}
