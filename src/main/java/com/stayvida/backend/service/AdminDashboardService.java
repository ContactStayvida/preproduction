package com.stayvida.backend.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminDashboardService {

    private final JdbcTemplate jdbcTemplate;

    public AdminDashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public BigDecimal getCurrentMonthRevenue() {

        String sql = """
                SELECT
                    COALESCE(SUM(commision_Amount), 0) AS totalCommission,
                    COALESCE(SUM(platformFee), 0) AS totalPlatformFee
                FROM bookings
                WHERE MONTH(createdAt) = MONTH(CURRENT_DATE)
                  AND YEAR(createdAt) = YEAR(CURRENT_DATE)
                """;

        Map<String, Object> dbResult = jdbcTemplate.queryForMap(sql);

        return ((BigDecimal) dbResult.get("totalCommission"))
                .add((BigDecimal) dbResult.get("totalPlatformFee"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public Map<String, Map<String, BigDecimal>> getLast6MonthRevenue() {

        // Step 1: Prepare last 6 months with ZERO values
        Map<String, Map<String, BigDecimal>> result = new LinkedHashMap<>();

        LocalDate now = LocalDate.now().withDayOfMonth(1);

        for (int i = 5; i >= 0; i--) {
            LocalDate monthDate = now.minusMonths(i);
            String key = monthDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));

            Map<String, BigDecimal> emptyData = new HashMap<>();
            // emptyData.put("totalCommission", BigDecimal.ZERO.setScale(2));
            // emptyData.put("totalPlatformFee", BigDecimal.ZERO.setScale(2));
            emptyData.put("totalRevenue", BigDecimal.ZERO.setScale(2));

            result.put(key, emptyData);
        }

        // Step 2: Fetch actual data
        String sql = """
                SELECT
                    YEAR(createdAt) AS year,
                    MONTH(createdAt) AS month,
                    COALESCE(SUM(commision_Amount), 0) AS totalCommission,
                    COALESCE(SUM(platformFee), 0) AS totalPlatformFee
                FROM bookings
                WHERE createdAt >= DATE_SUB(CURRENT_DATE, INTERVAL 6 MONTH)
                GROUP BY YEAR(createdAt), MONTH(createdAt)
                """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        // Step 3: Overlay real data
        for (Map<String, Object> row : rows) {

            int year = ((Number) row.get("year")).intValue();
            int month = ((Number) row.get("month")).intValue();

            String key = String.format("%04d-%02d", year, month);

            BigDecimal totalCommission = ((BigDecimal) row.get("totalCommission")).setScale(2, RoundingMode.HALF_UP);

            BigDecimal totalPlatformFee = ((BigDecimal) row.get("totalPlatformFee")).setScale(2, RoundingMode.HALF_UP);

            BigDecimal totalRevenue = totalCommission.add(totalPlatformFee).setScale(2, RoundingMode.HALF_UP);

            Map<String, BigDecimal> monthlyData = new HashMap<>();
            // monthlyData.put("totalCommission", totalCommission);
            // monthlyData.put("totalPlatformFee", totalPlatformFee);
            monthlyData.put("totalRevenue", totalRevenue);

            result.put(key, monthlyData);
        }

        return result;
    }

    public long totalBooking() {
        String sql = """
                SELECT
                    COUNT(*) AS totalBooking
                FROM bookings
                WHERE MONTH(createdAt) = MONTH(CURRENT_DATE)
                AND YEAR(createdAt) = YEAR(CURRENT_DATE)
                """;

        Map<String, Object> dbResult = jdbcTemplate.queryForMap(sql);

        return ((Number) dbResult.get("totalBooking")).longValue();
    }

    public long hotelCount() {
        String sql = """
                SELECT
                    COUNT(*) AS totalHotel
                FROM hotels
                WHERE status = 'Verified'
                """;

        Map<String, Object> dbResult = jdbcTemplate.queryForMap(sql);

        return ((Number) dbResult.get("totalHotel")).longValue();
    }

    public BigDecimal totalOccupancy() {

        String sql = """
                SELECT COUNT(*) AS totalOccupancy
                FROM bookings
                WHERE checkIn <= CURRENT_DATE
                  AND checkOut >= CURRENT_DATE
                  AND booking_Status = 'CheckIn'
                """;

        long occupiedRooms = ((Number) jdbcTemplate.queryForMap(sql).get("totalOccupancy")).longValue();

        long totalRooms = totalRoomCount();

        BigDecimal occupancyPercentage = BigDecimal.ZERO;

        if (totalRooms > 0) {
            occupancyPercentage = BigDecimal.valueOf(occupiedRooms)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalRooms), 2, RoundingMode.HALF_UP);
        }

        return occupancyPercentage;
    }

    public long totalRoomCount() {

        String sql = """
                SELECT COUNT(*) AS totalRoom
                FROM rooms r INNER JOIN hotels h ON r.hotel_ID = h.hotel_ID
                WHERE h.status = 'Verified'
                """;

        return ((Number) jdbcTemplate.queryForMap(sql).get("totalRoom")).longValue();
    }

}
