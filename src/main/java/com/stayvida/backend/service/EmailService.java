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
                    <body style="font-family:Arial;background:#f5f5f5;padding:20px;">

                        <table width="100%%" cellpadding="0" cellspacing="0">
                            <tr>
                                <td align="center">

                                    <table width="650" cellpadding="0" cellspacing="0"
                                        style="background:white;border-radius:10px;overflow:hidden;box-shadow:0 3px 10px rgba(0,0,0,0.1);">

                                        <tr>
                                            <td
                                                style="background:#28a745;color:white;text-align:center;padding:25px;font-size:26px;font-weight:bold;">
                                                🎉 ✔ Booking Confirmed
                                            </td>
                                        </tr>
                                        <td style="padding:25px;color:#333;font-size:15px;">

                                            <table width="100%%">
                                                <tr>
                                                    <td></td>
                                                    <td align="right" style="font-size:14px;font-weight:bold;color:#28a745;">
                                                        📌 Booking ID: %s
                                                    </td>
                                                </tr>
                                            </table>

                                            <p>Hi <b>%s</b>,</p>

                                            <p>✨ Your booking has been successfully confirmed. Please find your details below.</p>

                                            <hr style="border:none;border-top:1px solid #eee;margin:20px 0;">

                                            <h3 style="margin-bottom:10px;">🏨 Hotel Information</h3>

                                            <table width="100%%" cellpadding="6"
                                                style="background:#fafafa;border-left:4px solid #28a745;border-radius:6px;">
                                                <tr>
                                                    <td>🏢 <b>Property:</b> %s</td>
                                                </tr>
                                                <tr>
                                                    <td>🚪 <b>Room No:</b> %d</td>
                                                </tr>
                                                <tr>
                                                    <td>📅 <b>Check-In:</b> %s</td>
                                                </tr>
                                                <tr>
                                                    <td>📅 <b>Check-Out:</b> %s</td>
                                                </tr>
                                            </table>
                                            <br>
                                            <h3 style="margin-bottom:10px;">👤 Guest Details</h3>
                                            <table width="100%%" cellpadding="6"
                                                style="background:#fafafa;border-left:4px solid #28a745;border-radius:6px;">
                                                <tr>
                                                    <td>🧑 <b>Name:</b> %s</td>
                                                </tr>
                                                <tr>
                                                    <td>📞 <b>Phone:</b> %s</td>
                                                </tr>
                                            </table>
                                            <hr style="border:none;border-top:1px solid #eee;margin:25px 0;">
                                            <h3 style="margin-bottom:10px;">💳 Payment Summary</h3>
                                            <table width="100%%" cellpadding="6">
                                                <tr>
                                                    <td>🧾 Platform Fee</td>
                                                    <td align="right">₹%.2f</td>
                                                </tr>
                                                <tr>
                                                    <td>📊 Taxes</td>
                                                    <td align="right">%.0f%%</td>
                                                </tr>
                                                <tr style="font-weight:bold;font-size:18px;">
                                                    <td>💰 Total Amount</td>
                                                    <td align="right">₹%.2f</td>
                                                </tr>
                                                <tr>
                                                    <td style="color:green;">✅ Paid Amount</td>
                                                    <td align="right" style="color:green;">₹%.2f</td>
                                                </tr>
                                                <tr>
                                                    <td style="color:red;">⏳ Pending Amount</td>
                                                    <td align="right" style="color:red;">₹%.2f</td>
                                                </tr>
                                                <tr>
                                                    <td>💳 <b>Payment Type</b></td>
                                                    <td align="right">%s</td>
                                                </tr>
                                                <tr>
                                                    <td>📌 <b>Payment Status</b></td>
                                                    <td align="right">%s</td>
                                                </tr>
                                            </table>
                                            <div style="text-align:center;margin-top:30px;">
                                                <a href=""
                                                    style="background:#28a745;color:white;padding:14px 28px;text-decoration:none;border-radius:6px;font-weight:bold;display:inline-block;">
                                                    🔎 View Booking
                                                </a>
                                            </div>
                                        </td>
                            </tr>
                        </table>
                        </td>
                        </tr>
                        </table>
                    </body>
                    </html>
                                        """
                    .formatted(
                            booking.getBookingId(),
                            booking.getGuestName(),
                            booking.getHotelName(),
                            booking.getRoomNo(),
                            booking.getCheckIn(),
                            booking.getCheckOut(),
                            booking.getGuestName(),
                            booking.getPhone(),
                            booking.getPlatformFee().doubleValue(),
                            booking.getTaxAmount().doubleValue() * 100,
                            booking.getTotalAmount().doubleValue(),
                            booking.getPaidAmount().doubleValue(),
                            booking.getPendingAmount().doubleValue(),
                            booking.getPaymentType(),
                            booking.getPaymentStatus()

                    );

            helper.setText(html, true);

            mailSender.send(message);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
