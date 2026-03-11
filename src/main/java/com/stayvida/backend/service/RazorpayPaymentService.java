package com.stayvida.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.stayvida.backend.Config.RazorpayConfig;
import com.stayvida.backend.dto.BookingEmailDTO;
import com.stayvida.backend.dto.RazorpayVerifyRequest;
import com.stayvida.backend.repository.BookingRepository;

@Service
public class RazorpayPaymentService {

        private final JdbcTemplate jdbcTemplate;
        private final RazorpayConfig razorpayConfig;
        private final WalletService walletService;
        private final EmailService emailService;
        private final BookingRepository bookingRepository;

        // ✅ Constructor injection (CORRECT)
        public RazorpayPaymentService(
                        JdbcTemplate jdbcTemplate,
                        RazorpayConfig razorpayConfig,
                        WalletService walletService,
                        EmailService emailService,
                        BookingRepository bookingRepository) {

                this.jdbcTemplate = jdbcTemplate;
                this.razorpayConfig = razorpayConfig;
                this.walletService = walletService;
                this.emailService = emailService;
                this.bookingRepository = bookingRepository;
        }

        // STEP 1: Create Razorpay Order
        public Map<String, Object> createOrder(String bookingId, BigDecimal amount) {

                Map<String, Object> booking = jdbcTemplate.queryForMap(
                                "SELECT booking_ID, user_ID, payment_type, totalAmount FROM bookings WHERE booking_ID = ?",
                                bookingId);

                try {
                        RazorpayClient client = new RazorpayClient(
                                        razorpayConfig.getKey(),
                                        razorpayConfig.getSecret());

                        JSONObject options = new JSONObject();
                        options.put("amount", amount.multiply(BigDecimal.valueOf(100))); // paise
                        options.put("currency", "INR");
                        options.put("receipt", bookingId);

                        Order order = client.orders.create(options);
                        String razorpayOrderId = order.get("id");

                        insertPayment(bookingId, amount, razorpayOrderId);

                        return Map.of(
                                        "razorpayOrderId", razorpayOrderId,
                                        "amount", amount,
                                        "currency", "INR");

                } catch (Exception e) {
                        e.printStackTrace(); // 👈 ADD THIS
                        throw new RuntimeException("Razorpay order creation failed", e);
                }
        }

        private void insertPayment(String bookingId, BigDecimal amount, String orderId) {

                int userId = (int) SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                                .getPrincipal();

                String paymentId = "PAY-" + System.currentTimeMillis();

                JSONObject split = new JSONObject();
                split.put("razorpay_order_id", orderId);

                jdbcTemplate.update(
                                """
                                                INSERT INTO payments
                                                (user_ID, payment_ID, booking_ID, amount, payment_Method,currency, payment_Status, split_Details, createdAt)
                                                VALUES (?, ?, ?, ?, 'Razorpay','INR', 'Pending', ?, NOW())
                                                """,
                                userId,
                                paymentId,
                                bookingId,
                                amount,
                                split.toString());
        }

        private boolean verifySignature(String orderId, String paymentId, String signature) {
                try {
                        String payload = orderId + "|" + paymentId;

                        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
                        javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                                        razorpayConfig.getSecret().getBytes(),
                                        "HmacSHA256");

                        mac.init(secretKey);
                        byte[] hash = mac.doFinal(payload.getBytes());

                        String generated = bytesToHex(hash);
                        return generated.equals(signature);

                } catch (Exception e) {
                        throw new RuntimeException("Signature verification failed", e);
                }
        }

        private String bytesToHex(byte[] bytes) {
                StringBuilder hex = new StringBuilder();
                for (byte b : bytes) {
                        String s = Integer.toHexString(0xff & b);
                        if (s.length() == 1)
                                hex.append('0');
                        hex.append(s);
                }
                return hex.toString();
        }

        @Transactional
        public String verifyPayment(RazorpayVerifyRequest req) {
                try {
                        int userId = (int) SecurityContextHolder
                                        .getContext()
                                        .getAuthentication()
                                        .getPrincipal();

                        boolean valid = verifySignature(
                                        req.getRazorpayOrderId(),
                                        req.getRazorpayPaymentId(),
                                        req.getRazorpaySignature());

                        if (!valid) {
                                markPaymentFailed(req.getRazorpayOrderId());
                                throw new RuntimeException("Invalid Razorpay signature");
                        }

                        Map<String, Object> payment = jdbcTemplate.queryForMap("""
                                            SELECT payment_ID, booking_ID, amount, payment_Status
                                            FROM payments
                                            WHERE JSON_UNQUOTE(JSON_EXTRACT(split_Details,'$.razorpay_order_id')) = ?
                                        """, req.getRazorpayOrderId());

                        String status = (String) payment.get("payment_Status");

                        // ✅ already processed — idempotent return
                        if ("Success".equalsIgnoreCase(status)) {
                                return "already paid";
                        }

                        String paymentId = (String) payment.get("payment_ID");
                        String bookingId = (String) payment.get("booking_ID");
                        BigDecimal amount = (BigDecimal) payment.get("amount");

                        // 1️⃣ Update payment table
                        jdbcTemplate.update("""
                                            UPDATE payments
                                            SET payment_Status='Success',
                                                transaction_ID=?,
                                                updatedAt=NOW()
                                            WHERE payment_ID=?
                                        """, req.getRazorpayPaymentId(), paymentId);

                        // 2️⃣ Add to booking payment_amount
                        jdbcTemplate.update("""
                                            UPDATE bookings
                                            SET payment_amount = COALESCE(payment_amount,0) + ?
                                            WHERE booking_ID=?
                                        """, amount, bookingId);

                        // 3️⃣ Check if booking fully paid
                        Map<String, Object> booking = jdbcTemplate.queryForMap("""
                                            SELECT payment_amount,commision_Amount,hotel_ID,platformFee,
                                                   (totalAmount + platformFee) AS required
                                            FROM bookings
                                            WHERE booking_ID=?
                                        """, bookingId);

                        BigDecimal paid = (BigDecimal) booking.get("payment_amount");
                        BigDecimal required = (BigDecimal) booking.get("required");

                        if (paid.compareTo(required) >= 0) {
                                jdbcTemplate.update("""
                                                    UPDATE bookings
                                                    SET payment_Status='Completed', transaction_ID=?
                                                    WHERE booking_ID=?
                                                """, req.getRazorpayPaymentId(), bookingId);
                        }

                        String hotelId = (String) booking.get("hotel_ID");

                        BigDecimal commision = (BigDecimal) booking.get("commision_Amount");
                        BigDecimal Platformcharges = (BigDecimal) booking.get("platformFee");
                        BigDecimal platformCommision = commision.add(Platformcharges);

                        // 4️⃣ Add to wallet
                        walletService.wallet(
                                        hotelId,
                                        bookingId,
                                        amount,
                                        "CR",
                                        "Razorpay",
                                        req.getRazorpayPaymentId());
                        walletService.wallet(
                                        hotelId,
                                        bookingId,
                                        platformCommision,
                                        "DR",
                                        "PLatformcharges + COMMISION",
                                        req.getRazorpayPaymentId());

                        String email = (String) SecurityContextHolder
                                        .getContext()
                                        .getAuthentication()
                                        .getDetails();
                        System.out.println("Email: " + email);
                        BookingEmailDTO dto = bookingRepository.getBookingForEmail(bookingId);

                        emailService.sendBookingConfirmation(email, dto);

                        return "200: payment verified";

                } catch (Exception e) {
                        // log properly in real code
                        System.out.println("VERIFY ERROR >>> " + e.getMessage());
                        throw e; // let transaction roll back
                }
        }

        private void markPaymentFailed(String orderId) {
                jdbcTemplate.update("""
                                                                UPDATE payments
                                SET payment_Status='Failed', updatedAt=NOW()
                                WHERE JSON_UNQUOTE(JSON_EXTRACT(split_Details,'$.razorpay_order_id')) = ?
                                                            """, orderId);
        }

}
