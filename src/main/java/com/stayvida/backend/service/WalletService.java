package com.stayvida.backend.service;

import java.math.BigDecimal;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

    private final JdbcTemplate jdbcTemplate;

    public WalletService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void wallet(
            String hotelId,
            String bookingId,
            BigDecimal amount,
            String type, // "CR" or "DR"
            String via,
            String transactionId) {

        // 1️⃣ ensure wallet exists
        jdbcTemplate.update("""
                    INSERT INTO balance(hotel_id, balance)
                    VALUES (?, 0)
                    ON DUPLICATE KEY UPDATE hotel_id=hotel_id
                """, hotelId);

        // 2️⃣ mutate balance safely
        if ("CR".equalsIgnoreCase(type)) {

            jdbcTemplate.update("""
                        UPDATE balance
                        SET balance = balance + ?
                        WHERE hotel_id = ?
                    """, amount, hotelId);

        } else if ("DR".equalsIgnoreCase(type)) {

            int updated = jdbcTemplate.update("""
                        UPDATE balance
                        SET balance = balance - ?
                        WHERE hotel_id = ?
                        AND balance >= ?
                    """, amount, hotelId, amount);

            if (updated == 0) {
                throw new RuntimeException("Insufficient hotel balance");
            }

        } else {
            throw new IllegalArgumentException("Invalid transaction type");
        }

        // 3️⃣ read new balance snapshot
        BigDecimal newBalance = jdbcTemplate.queryForObject("""
                    SELECT balance FROM balance WHERE hotel_id=?
                """, BigDecimal.class, hotelId);

        // 4️⃣ append ledger entry (history is immutable)
        jdbcTemplate.update("""
                    INSERT INTO ledger
                    (hotel_id, booking_id, via, transaction_id, type, amount, balance_after)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                hotelId,
                bookingId,
                via,
                transactionId,
                type,
                amount,
                newBalance);
    }
}
