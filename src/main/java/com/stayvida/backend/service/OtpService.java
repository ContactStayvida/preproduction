package com.stayvida.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class OtpService {

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

        if (otpData == null) return false;
        if (LocalDateTime.now().isAfter(otpData.expiry)) return false;

        boolean valid = otpData.otp.equals(inputOtp);
        if (valid) otpStore.remove(email);
        return valid;
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
