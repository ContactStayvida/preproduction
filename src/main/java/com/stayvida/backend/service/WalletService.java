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
            String type, // "CR" or "DR" or "WITHDRAW" only
            String via,
            String transactionId) {

        createAccount(hotelId);

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

        } else if ("DR".equalsIgnoreCase(type)
                || "WITHDRAW".equalsIgnoreCase(type)) {

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

    @Transactional // withdraw request
    public void requestWithdraw(
            String hotelId,
            BigDecimal amount) {

        // Optional: check current balance to avoid useless requests
        BigDecimal balance = jdbcTemplate.queryForObject("""
                    SELECT balance FROM balance WHERE hotel_id=?
                """, BigDecimal.class, hotelId);

        if (balance.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance to request withdraw");
        }

        jdbcTemplate.update("""
                    INSERT INTO withdraw_request
                    (hotel_id, txn_date, type, amount, status)
                    VALUES (?, NOW(), 'WITHDRAW', ?, 'PENDING')
                """,
                hotelId,
                amount);
    }

    @Transactional
    public void processWithdraw(long requestId, String decision, String transactionId) {

        Map<String, Object> request = jdbcTemplate.queryForMap("""
                    SELECT hotel_id, amount, status
                    FROM withdraw_request
                    WHERE sr=?
                """, requestId);

        String currentStatus = (String) request.get("status");

        if (!"PENDING".equalsIgnoreCase(currentStatus)) {
            throw new RuntimeException("Request already processed");
        }

        if ("APPROVE".equalsIgnoreCase(decision)) {

            String hotelId = (String) request.get("hotel_id");
            BigDecimal amount = (BigDecimal) request.get("amount");

            // Execute real withdraw (this updates ledger + balance)
            wallet(hotelId, null, amount, "WITHDRAW", "BANK", transactionId);

            jdbcTemplate.update("""
                        UPDATE withdraw_request
                        SET status='APPROVED'
                        WHERE sr=?
                    """, requestId);

        } else if ("REJECT".equalsIgnoreCase(decision)) {

            jdbcTemplate.update("""
                        UPDATE withdraw_request
                        SET status='REJECTED'
                        WHERE sr=?
                    """, requestId);

        } else {
            throw new IllegalArgumentException("Invalid decision type");
        }
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

    public List<Map<String, Object>> getWithdrawRequests(String status) {

        if (status == null || status.isBlank()) {

            return jdbcTemplate.queryForList("""
                        SELECT sr,
                               hotel_id,
                               txn_date,
                               amount,
                               status,
                               remark
                        FROM withdraw_request
                        ORDER BY sr DESC
                    """);

        } else {

            return jdbcTemplate.queryForList("""
                        SELECT sr,
                               hotel_id,
                               txn_date,
                               amount,
                               status,
                               remark
                        FROM withdraw_request
                        WHERE status = ?
                        ORDER BY sr DESC
                    """, status);
        }
    }

    // OWNER VERSION
    public List<Map<String, Object>> getWithdrawRequests(String status, String hotelId) {

        if (status == null || status.isBlank()) {

            return jdbcTemplate.queryForList("""
                        SELECT sr,
                               hotel_id,
                               txn_date,
                               amount,
                               status,
                               remark
                        FROM withdraw_request WHERE hotel_id = ?
                        ORDER BY sr DESC
                    """, hotelId);

        } else {

            return jdbcTemplate.queryForList("""
                        SELECT sr,
                               hotel_id,
                               txn_date,
                               amount,
                               status,
                               remark
                        FROM withdraw_request
                        WHERE status = ? and hotel_id = ?
                        ORDER BY sr DESC
                    """, status, hotelId);
        }
    }

}
