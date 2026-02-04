package com.stayvida.backend.service;

import org.springframework.stereotype.Service;

import com.stayvida.backend.dto.BookingRequest;
import com.stayvida.backend.dto.BookingResponse;
import com.stayvida.backend.exception.BookingExceptions.OtpRequiredException;

@Service
public class BookingFlowService {

    private final OtpService otpService;
    private final EmailService emailService;
    private final BookingService bookingService;

    public BookingFlowService(
            OtpService otpService,
            EmailService emailService,
            BookingService bookingService) {
        this.otpService = otpService;
        this.emailService = emailService;
        this.bookingService = bookingService;
    }

    public BookingResponse initiateBooking(BookingRequest request) {

        String email = request.getEmail();

        // 1️⃣ OTP NOT provided → generate & send OTP
        if (request.getOtp() == null || request.getOtp().isBlank()) {

            String otp = otpService.generateOtp(email);
            emailService.sendOtpEmail(email, otp);

            throw new OtpRequiredException(
                    "OTP sent to email. Please verify to continue booking.");
        }

        // 2️⃣ OTP provided → validate
        otpService.verifyLoginWithOtp(email, request.getOtp());
        boolean validOtp = otpService.validateOtp(email, request.getOtp());

        if (!validOtp) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        // 3️⃣ OTP verified → proceed to booking
        // bookingService will internally validate or create user
        return bookingService.createBookingod(email, request);
    }
}
