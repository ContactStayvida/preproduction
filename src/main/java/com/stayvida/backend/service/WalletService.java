package com.stayvida.backend.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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

        createAccount(hotelId);

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

    public Map<String, Object> getWallet(String hotelId) {

        List<BigDecimal> result = jdbcTemplate.query("""
                    SELECT balance FROM balance WHERE hotel_id=?
                """, (rs, rowNum) -> rs.getBigDecimal(1), hotelId);

        if (result.isEmpty()) {
            throw new RuntimeException("Account does not exist");
        }

        BigDecimal balance = result.get(0);

        List<Map<String, Object>> ledger = jdbcTemplate.queryForList("""
                    SELECT sr,
                           booking_id,
                           via,
                           transaction_id,
                           type,
                           amount,
                           balance_after,
                           txn_date
                    FROM ledger
                    WHERE hotel_id=?
                    ORDER BY sr DESC
                    LIMIT 100
                """, hotelId);

        return Map.of(
                "hotelId", hotelId,
                "balance", balance,
                "transactions", ledger);
    }

    public void createAccount(String hotelId) {

        int inserted = jdbcTemplate.update("""
                    INSERT INTO balance (hotel_id, balance)
                    VALUES (?, 0)
                    ON DUPLICATE KEY UPDATE hotel_id = hotel_id
                """, hotelId);

        // optional: you can log if already exists
    }

}
