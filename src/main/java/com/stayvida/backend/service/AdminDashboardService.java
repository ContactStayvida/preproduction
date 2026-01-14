package com.stayvida.backend.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
public class AdminDashboardService {

    private final JdbcTemplate jdbcTemplate;

    public AdminDashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, BigDecimal> getCurrentMonthRevenue() {

        String sql = """
                SELECT
                    COALESCE(SUM(commision_Amount), 0) AS totalCommission,
                    COALESCE(SUM(platformFee), 0) AS totalPlatformFee
                FROM bookings
                WHERE MONTH(createdAt) = MONTH(CURRENT_DATE)
                  AND YEAR(createdAt) = YEAR(CURRENT_DATE)
                """;

        Map<String, Object> dbResult = jdbcTemplate.queryForMap(sql);

        Map<String, BigDecimal> result = new HashMap<>();

        result.put(
                "totalCommission",
                ((BigDecimal) dbResult.get("totalCommission"))
                        .setScale(2, RoundingMode.HALF_UP));

        result.put(
                "totalPlatformFee",
                ((BigDecimal) dbResult.get("totalPlatformFee"))
                        .setScale(2, RoundingMode.HALF_UP));
        result.put(
                "totalRevenue",
                ((BigDecimal) dbResult.get("totalCommission"))
                        .add((BigDecimal) dbResult.get("totalPlatformFee"))
                        .setScale(2, RoundingMode.HALF_UP));

        return result;
    }
}
