package com.stayvida.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.env:production}")
    private String environment;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String to, String otp) {
        if ("development".equalsIgnoreCase(environment)) {
            System.out.println("🧪 Dev mode: Skipping email send. OTP = " + otp);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Your StayVida OTP");
        message.setText("Your OTP for login is: " + otp + "\n\nValid for 1 minute only.");
        mailSender.send(message);
    }
}
