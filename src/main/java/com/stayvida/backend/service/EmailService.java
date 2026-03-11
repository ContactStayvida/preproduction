package com.stayvida.backend.service;

import com.stayvida.backend.dto.BookingEmailDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

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

    public void sendBookingConfirmation(String to, BookingEmailDTO booking) {

        if ("development".equalsIgnoreCase(environment)) {
            System.out.println("🧪 Dev mode: Booking email skipped for " +
                    booking.getBookingId());
            return;
        }

        try {

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject("StayVida Booking Confirmation");

            String html = """
                     <html>
                     <body style='font-family:Arial;background:#f5f5f5;padding:20px;'>

                     <div style='max-width:650px;margin:auto;background:white;padding:20px;border-radius:10px;'>

                     <h2 style='color:#28a745;'>✔ Booking Confirmed</h2>
                     <p><b>Booking ID:</b> %s</p>

                     <hr>

                     <h3>Hotel Information</h3>

                     <p><b>Property:</b> %s</p>
                     <p><b>Room No:</b> %d</p>
                     <p><b>Check-In:</b> %s</p>
                     <p><b>Check-Out:</b> %s</p>

                     <hr>

                     <h3>Payment Summary</h3>

                     <p>Platform Fee: ₹%.2f</p>
                    <p>Taxes: %.0f%%</p>

                     <h3>Total Amount: ₹%.2f</h3>

                     <p style='color:green;'>Paid Amount: ₹%.2f</p>
                     <p style='color:red;'>Pending Amount: ₹%.2f</p>

                     <p><b>Payment Type:</b> %s</p>
                     <p><b>Payment Status:</b> %s</p>

                     <hr>

                     <h3>Guest Details</h3>

                     <p><b>Name:</b> %s</p>
                     <p><b>Phone:</b> %s</p>

                     </div>
                     </body>
                     </html>
                     """.formatted(

                    booking.getBookingId(),
                    booking.getHotelName(),
                    booking.getRoomNo(),
                    booking.getCheckIn(),
                    booking.getCheckOut(),
                    booking.getPlatformFee().doubleValue(),
                    booking.getTaxAmount().doubleValue() * 100,
                    booking.getTotalAmount().doubleValue(),
                    booking.getPaidAmount().doubleValue(),
                    booking.getPendingAmount().doubleValue(),
                    booking.getPaymentType(),
                    booking.getPaymentStatus(),
                    booking.getGuestName(),
                    booking.getPhone()

            );

            helper.setText(html, true);

            mailSender.send(message);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
