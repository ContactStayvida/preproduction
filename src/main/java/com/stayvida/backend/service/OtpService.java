package com.stayvida.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import com.stayvida.backend.model.User;
import com.stayvida.backend.repository.ProfileRepository;
import com.stayvida.backend.repository.UserRepository;
import com.stayvida.backend.service.JwtUtil;
// import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.stayvida.backend.dto.LoginResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class OtpService {
    @Autowired
    private UserRepository userRepo;
    // @Autowired
    // private OtpService otpService;
    // @Autowired
    // private EmailService emailService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private ProfileRepository profileRepo;

    private final Map<String, OtpData> otpStore = new HashMap<>();

    // Inject environment variable
    @Value("${app.env:production}")
    private String environment;

    public String generateOtp(String email) {
        if ("development".equalsIgnoreCase(environment)) {
            // 🔧 Hardcoded OTP for dev mode
            otpStore.put(email, new OtpData("1234", LocalDateTime.now().plusMinutes(10)));
            return "1234";
        } else {
            // 🛡️ Generate real OTP for production
            String otp = String.format("%06d", new Random().nextInt(999999));
            otpStore.put(email, new OtpData(otp, LocalDateTime.now().plusMinutes(1)));
            return otp;
        }
    }

    public boolean validateOtp(String email, String inputOtp) {
        OtpData otpData = otpStore.get(email);

        if ("development".equalsIgnoreCase(environment)) {
            // ✅ In dev mode, always allow "1234"
            return "1234".equals(inputOtp);
        }

        if (otpData == null)
            return false;
        if (LocalDateTime.now().isAfter(otpData.expiry))
            return false;

        boolean valid = otpData.otp.equals(inputOtp);
        if (valid)
            otpStore.remove(email);
        return valid;
    }

    public LoginResponse verifyLoginWithOtp(String email, String otp) {

        // 1️⃣ Validate OTP
        if (!validateOtp(email, otp)) {
            return new LoginResponse(
                    false,
                    null,
                    0,
                    email,
                    null,
                    "Invalid or expired OTP");
        }

        // 2️⃣ Check if user exists
        User user = userRepo.findByEmail(email);
        boolean isNew = (user == null);

        if (isNew) {
            user = new User();
            user.setEmail(email);
            user.setPassword("OTP_LOGIN");
            user.setRole("user");
            userRepo.saveOrUpdate(user);
        } else {
            System.out.println("Existing user login: " + email);
        }

        // 3️⃣ Check profile
        boolean profileExists = profileRepo.profileExists(user.getuserID());

        // 4️⃣ Generate JWT
        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getuserID(),
                user.getRole());

        // 5️⃣ Build response
        LoginResponse response = new LoginResponse(
                true,
                token,
                user.getuserID(),
                user.getEmail(),
                user.getRole(),
                isNew ? "Signup successful!" : "Login successful!");

        response.setProfileExists(profileExists);
        return response;
    }

    private static class OtpData {
        String otp;
        LocalDateTime expiry;

        OtpData(String otp, LocalDateTime expiry) {
            this.otp = otp;
            this.expiry = expiry;
        }
    }
}
