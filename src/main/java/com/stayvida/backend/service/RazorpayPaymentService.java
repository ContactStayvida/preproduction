package com.stayvida.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.stayvida.backend.Config.RazorpayConfig;

@Service
public class RazorpayPaymentService {

        private final JdbcTemplate jdbcTemplate;
        private final RazorpayConfig razorpayConfig;

        // ✅ Constructor injection (CORRECT)
        public RazorpayPaymentService(
                        JdbcTemplate jdbcTemplate,
                        RazorpayConfig razorpayConfig) {

                this.jdbcTemplate = jdbcTemplate;
                this.razorpayConfig = razorpayConfig;
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

                String paymentId = "PAY-" + System.currentTimeMillis();

                JSONObject split = new JSONObject();
                split.put("razorpay_order_id", orderId);
                int userId = (int) SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                                .getPrincipal();

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
}
