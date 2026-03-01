package com.stayvida.backend.repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ledgerRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> fetchLedger(String hotelId, String type) {

        StringBuilder sql = new StringBuilder("""
                    SELECT sr, hotel_id, booking_id, txn_date,
                           via, transaction_id, type,
                           amount, balance_after
                    FROM ledger
                    WHERE hotel_id = ?
                """);

        List<Object> params = new ArrayList<>();
        params.add(hotelId);

        if (type != null && !type.isBlank()) {

            if (!type.equals("CR") &&
                    !type.equals("DR") &&
                    !type.equals("WITHDRAW")) {

                throw new IllegalArgumentException("Invalid transaction type");
            }

            sql.append(" AND type = ?");
            params.add(type);
        }

        sql.append(" ORDER BY txn_date DESC");

        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public Map<String, Object> fetchFinancialSummary(String hotelId, int month, int year) {

        // 1. Fetch balance from balance table
        String balanceSql = "SELECT balance FROM balance WHERE hotel_id = ?";
        BigDecimal balance = jdbcTemplate.queryForObject(balanceSql, new Object[] { hotelId }, BigDecimal.class);

        // 2. Sum of CR transactions for the given month/year
        String incomeSql = """
                                    SELECT
                    COALESCE(SUM(CASE
                        WHEN type = 'CR' THEN amount
                        WHEN type = 'DR' THEN -amount
                        ELSE 0
                    END), 0) AS net_amount
                FROM ledger
                WHERE hotel_id = ?
                  AND MONTH(txn_date) = ?
                  AND YEAR(txn_date) = ?
                                """;
        BigDecimal totalIncome = jdbcTemplate.queryForObject(incomeSql, new Object[] { hotelId, month, year },
                BigDecimal.class);

        // 3. Sum of WITHDRAW transactions for the given month/year
        String withdrawSql = """
                    SELECT COALESCE(SUM(amount), 0)
                    FROM ledger
                    WHERE hotel_id = ?
                      AND type = 'WITHDRAW'
                      AND MONTH(txn_date) = ?
                      AND YEAR(txn_date) = ?
                """;
        BigDecimal totalWithdraw = jdbcTemplate.queryForObject(withdrawSql, new Object[] { hotelId, month, year },
                BigDecimal.class);

        // Return as map
        Map<String, Object> summary = new HashMap<>();
        summary.put("balance", balance);
        summary.put("totalIncome", totalIncome);
        summary.put("totalWithdraw", totalWithdraw);
        summary.put("month", month); // numeric month, will be replaced in service
        summary.put("year", year);

        return summary;
    }
}