package com.stayvida.backend.controller;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stayvida.backend.service.RazorpayPaymentService;

@RestController
@RequestMapping("/api/payments/razorpay")
public class RazorpayPaymentController {

    @Autowired
    private RazorpayPaymentService paymentService;

    @PostMapping("/order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> req) throws Exception {

        String bookingId = req.get("bookingId").toString();
        BigDecimal amount = new BigDecimal(req.get("amount").toString());

        return ResponseEntity.ok(paymentService.createOrder(bookingId, amount));
    }
}
