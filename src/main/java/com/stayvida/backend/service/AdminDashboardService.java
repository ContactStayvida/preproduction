package com.stayvida.backend.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stayvida.backend.dto.Ad;
import com.stayvida.backend.dto.ExecutiveListDTO;
import com.stayvida.backend.dto.ExecutivePaymentResponse;
import com.stayvida.backend.dto.UpdateAmountRequest;
import com.stayvida.backend.dto.UserListDTO;
import com.stayvida.backend.repository.AdRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminDashboardService {

    private final JdbcTemplate jdbcTemplate;
    private final AdRepository adRepository;

    public AdminDashboardService(JdbcTemplate jdbcTemplate, AdRepository adRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.adRepository = adRepository;
    }

    public List<ExecutiveListDTO> getExecutiveList() {
        return fetchExecutiveList();
    }

    public List<ExecutiveListDTO> fetchExecutiveList() {

        String sql = """
                    SELECT
                        p.user_ID,
                        u.email,
                        u.role,
                        p.phone_number,
                        p.name,
                        e.referral_code,
                        e.is_enable
                    FROM executive e
                    LEFT JOIN profile p ON e.user_ID = p.user_ID
                    LEFT JOIN users u ON e.user_ID = u.user_ID
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            ExecutiveListDTO dto = new ExecutiveListDTO();
            dto.setUserId(rs.getInt("user_ID"));
            dto.setEmail(rs.getString("email"));
            dto.setRole(rs.getString("role"));
            dto.setPhoneNumber(rs.getString("phone_number"));
            dto.setName(rs.getString("name"));
            dto.setReferralCode(rs.getString("referral_code"));
            dto.setIsEnable(rs.getBoolean("is_enable"));
            return dto;
        });
    }

    public List<ExecutivePaymentResponse> getAllExecutivePayments() {

        String sql = """
                    SELECT sr_no, booking_ID, user_ID, referral_code,
                           payment_amount, payment_status,
                           created_at, updated_at
                    FROM executive_referral_payments
                    ORDER BY created_at DESC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {

            ExecutivePaymentResponse res = new ExecutivePaymentResponse();

            res.setSrNo(rs.getInt("sr_no"));
            res.setBookingId(rs.getString("booking_ID"));
            res.setUserId(rs.getInt("user_ID"));
            res.setReferralCode(rs.getString("referral_code"));
            res.setPaymentAmount(rs.getBigDecimal("payment_amount"));
            res.setPaymentStatus(rs.getString("payment_status"));

            Timestamp created = rs.getTimestamp("created_at");
            Timestamp updated = rs.getTimestamp("updated_at");

            res.setCreatedAt(created != null ? created.toLocalDateTime() : null);
            res.setUpdatedAt(updated != null ? updated.toLocalDateTime() : null);

            return res;
        });
    }

    public void updatePaymentStatus(String bookingId) {

        // 1. Check current status
        String checkSql = """
                    SELECT payment_status
                    FROM executive_referral_payments
                    WHERE booking_ID = ?
                """;

        List<String> statusList = jdbcTemplate.query(
                checkSql,
                (rs, rowNum) -> rs.getString("payment_status"),
                bookingId);

        if (statusList.isEmpty()) {
            throw new RuntimeException("No payment record found for booking ID: " + bookingId);
        }

        String currentStatus = statusList.get(0);

        // 2. Prevent double update
        if ("PAID".equalsIgnoreCase(currentStatus)) {
            throw new RuntimeException("Payment is already PAID for booking ID: " + bookingId);
        }

        // 3. Update status
        String updateSql = """
                    UPDATE executive_referral_payments
                    SET payment_status = 'PAID'
                    WHERE booking_ID = ?
                """;

        jdbcTemplate.update(updateSql, bookingId);
    }

    public String updateExecutiveStatus(int userId, boolean newStatus) {

        // 1. Check if executive exists
        String checkSql = """
                    SELECT is_enable
                    FROM executive
                    WHERE user_id = ?
                """;

        List<Boolean> result = jdbcTemplate.query(
                checkSql,
                (rs, rowNum) -> rs.getBoolean("is_enable"),
                userId);

        // 2. If not found
        if (result.isEmpty()) {
            throw new RuntimeException("Executive not found with user_id: " + userId);
        }

        boolean currentStatus = result.get(0);

        // 3. Prevent enabling already enabled user
        if (currentStatus && newStatus) {
            throw new RuntimeException("Executive is already enabled");
        }

        // 4. Prevent disabling already disabled user
        if (!currentStatus && !newStatus) {
            throw new RuntimeException("Executive is already disabled");
        }

        // 5. Update status
        String updateSql = """
                    UPDATE executive
                    SET is_enable = ?
                    WHERE user_id = ?
                """;

        jdbcTemplate.update(updateSql, newStatus, userId);

        return newStatus
                ? "Executive enabled successfully"
                : "Executive disabled successfully";
    }

    public String createExecutive(int userId) {

        // 1. Generate referral code
        String referralCode = "STAYVIDA_" + userId;

        // 2. Check if already exists (avoid PK conflict)
        String checkSql = "SELECT COUNT(*) FROM executive WHERE user_id = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, userId);

        if (count != null && count > 0) {
            return "Executive already exists for user_id: " + userId;
        }

        // 3. Insert into table
        String insertSql = "INSERT INTO executive (user_id, referral_code) VALUES (?, ?)";
        jdbcTemplate.update(insertSql, userId, referralCode);

        return "Executive created successfully";
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

    // public Map<String, Map<String, BigDecimal>> getLast6MonthRevenue() {

    // // Step 1: Prepare last 6 months with ZERO values
    // Map<String, Map<String, BigDecimal>> result = new LinkedHashMap<>();

    // LocalDate now = LocalDate.now().withDayOfMonth(1);

    // for (int i = 5; i >= 0; i--) {
    // LocalDate monthDate = now.minusMonths(i);
    // String key = monthDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));

    // Map<String, BigDecimal> emptyData = new HashMap<>();
    // // emptyData.put("totalCommission", BigDecimal.ZERO.setScale(2));
    // // emptyData.put("totalPlatformFee", BigDecimal.ZERO.setScale(2));
    // emptyData.put("totalRevenue", BigDecimal.ZERO.setScale(2));

    // result.put(key, emptyData);
    // }

    // // Step 2: Fetch actual data
    // String sql = """
    // SELECT
    // YEAR(createdAt) AS year,
    // MONTH(createdAt) AS month,
    // COALESCE(SUM(commision_Amount), 0) AS totalCommission,
    // COALESCE(SUM(platformFee), 0) AS totalPlatformFee
    // FROM bookings
    // WHERE createdAt >= DATE_SUB(CURRENT_DATE, INTERVAL 6 MONTH)
    // GROUP BY YEAR(createdAt), MONTH(createdAt)
    // """;

    // List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

    // // Step 3: Overlay real data
    // for (Map<String, Object> row : rows) {

    // int year = ((Number) row.get("year")).intValue();
    // int month = ((Number) row.get("month")).intValue();

    // String key = String.format("%04d-%02d", year, month);

    // BigDecimal totalCommission = ((BigDecimal)
    // row.get("totalCommission")).setScale(2, RoundingMode.HALF_UP);

    // BigDecimal totalPlatformFee = ((BigDecimal)
    // row.get("totalPlatformFee")).setScale(2, RoundingMode.HALF_UP);

    // BigDecimal totalRevenue = totalCommission.add(totalPlatformFee).setScale(2,
    // RoundingMode.HALF_UP);

    // Map<String, BigDecimal> monthlyData = new HashMap<>();
    // // monthlyData.put("totalCommission", totalCommission);
    // // monthlyData.put("totalPlatformFee", totalPlatformFee);
    // monthlyData.put("totalRevenue", totalRevenue);

    // result.put(key, monthlyData);
    // }

    // return result;
    // }

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

    /// new service dashbord

    public Map<String, Object> getDashboardData() {

        Map<String, Object> response = new HashMap<>();

        // ---------- LAST 6 MONTH REVENUE ----------
        List<Map<String, Object>> revenueData = new java.util.ArrayList<>();

        Map<String, BigDecimal> revenueMap = getLast6MonthRevenueFlat();

        revenueMap.forEach((month, revenue) -> {
            revenueData.add(Map.of(
                    "month", month,
                    "revenue", revenue));
        });

        response.put("revenueData", revenueData);

        return response;
    }

    public Map<String, Object> getDashStaticData() {
        Map<String, Object> response = new HashMap<>();
        // // ---------- STATS CARDS ----------
        List<Map<String, String>> statsData = List.of(
                Map.of(
                        "title", "Total Revenue",
                        "value", "₹" + getCurrentMonthRevenue().setScale(0, RoundingMode.HALF_UP)),
                Map.of(
                        "title", "Total Bookings",
                        "value", String.valueOf(totalBooking())),
                Map.of(
                        "title", "Active Hotels",
                        "value", String.valueOf(hotelCount())),
                Map.of(
                        "title", "Occupancy Rate",
                        "value", totalOccupancy().setScale(0, RoundingMode.HALF_UP) + "%"));

        response.put("statsData", statsData);
        return response;

    }

    public Map<String, Object> getDashBookingData() {
        Map<String, Object> response = new HashMap<>();
        // ---------- LAST 6 MONTH BOOKINGS ----------
        List<Map<String, Object>> bookingsData = new java.util.ArrayList<>();
        Map<String, Long> bookingMap = getLast6MonthBookings();

        bookingMap.forEach((month, bookings) -> {
            bookingsData.add(Map.of(
                    "month", month,
                    "bookings", bookings));
        });

        response.put("bookingsData", bookingsData);
        return response;

    }

    public Map<String, BigDecimal> getLast6MonthRevenueFlat() {

        Map<String, BigDecimal> result = new LinkedHashMap<>();

        LocalDate now = LocalDate.now().withDayOfMonth(1);

        for (int i = 5; i >= 0; i--) {
            LocalDate date = now.minusMonths(i);
            String month = date.format(DateTimeFormatter.ofPattern("MMM"));
            result.put(month, BigDecimal.ZERO);
        }

        String sql = """
                SELECT
                    MONTH(createdAt) AS month,
                    SUM(commision_Amount + platformFee) AS revenue
                FROM bookings
                WHERE createdAt >= DATE_SUB(CURRENT_DATE, INTERVAL 6 MONTH)
                GROUP BY MONTH(createdAt)
                """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        for (Map<String, Object> row : rows) {
            int monthNum = ((Number) row.get("month")).intValue();
            String monthName = LocalDate.of(2000, monthNum, 1)
                    .format(DateTimeFormatter.ofPattern("MMM"));

            BigDecimal revenue = ((BigDecimal) row.get("revenue"))
                    .setScale(0, RoundingMode.HALF_UP);

            result.put(monthName, revenue);
        }

        return result;
    }

    public Map<String, Long> getLast6MonthBookings() {

        Map<String, Long> result = new LinkedHashMap<>();

        LocalDate now = LocalDate.now().withDayOfMonth(1);

        for (int i = 5; i >= 0; i--) {
            LocalDate date = now.minusMonths(i);
            String month = date.format(DateTimeFormatter.ofPattern("MMM"));
            result.put(month, 0L);
        }

        String sql = """
                SELECT
                    MONTH(createdAt) AS month,
                    COUNT(*) AS bookings
                FROM bookings
                WHERE createdAt >= DATE_SUB(CURRENT_DATE, INTERVAL 6 MONTH)
                GROUP BY MONTH(createdAt)
                """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        for (Map<String, Object> row : rows) {
            int monthNum = ((Number) row.get("month")).intValue();
            String monthName = LocalDate.of(2000, monthNum, 1)
                    .format(DateTimeFormatter.ofPattern("MMM"));

            long bookings = ((Number) row.get("bookings")).longValue();
            result.put(monthName, bookings);
        }

        return result;
    }

    // only for admin
    @Transactional
    public void initializeCharges() {

        String sql = """
                INSERT INTO amount (type, value)
                SELECT ?, ?
                WHERE NOT EXISTS (
                    SELECT 1 FROM amount WHERE type = ?
                )
                """;

        jdbcTemplate.update(sql, "platform_charges", new BigDecimal("200.00"), "platform_charges");
        jdbcTemplate.update(sql, "Advance", new BigDecimal("0.30"), "Advance");
        jdbcTemplate.update(sql, "tax", new BigDecimal("0.18"), "tax");
        jdbcTemplate.update(sql, "commission", new BigDecimal("0.10"), "commission");
    }

    @Transactional
    public void updateCharges(List<UpdateAmountRequest> requests) {

        String sql = "UPDATE amount SET value = ? WHERE type = ?";

        for (UpdateAmountRequest r : requests) {
            jdbcTemplate.update(sql, r.getValue(), r.getType());
        }
    }

    public List<Map<String, Object>> getAllCharges() {

        String sql = """
                SELECT charges_ID, type, value
                FROM amount
                ORDER BY charges_ID
                """;

        return jdbcTemplate.queryForList(sql);
    }

    public void createAd(byte[] imageBytes, String hotelId) throws Exception {

        // compress image
        String base64Image = ImageCompressionUtil.processImageToBase64(imageBytes);

        // disable currently active ads
        adRepository.disableAllActiveAds();

        Ad ad = new Ad();
        ad.setAdId(generateAdId());
        ad.setBannerImage(base64Image);
        ad.setHotelId(hotelId);
        ad.setClickCount(0);
        ad.setActive(true);

        adRepository.createAd(ad);
    }

    public List<Ad> getAllAds() {
        return adRepository.getAllAds();
    }

    public Ad getCurrentAd() {
        return adRepository.getCurrentAd();
    }

    public void deleteAd(String adId) {
        adRepository.deleteAd(adId);
    }

    public void enableDisableAd(String adId, boolean active) {

        if (active) {

            // turn off every ad first
            adRepository.disableAllActiveAds();

            // then enable the requested ad
            adRepository.setAdActive(adId, true);

        } else {

            // only disable this ad
            adRepository.setAdActive(adId, false);
        }
    }

    private String generateAdId() {
        return "AD_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public void incrementClick(String adId) {

        adRepository.incrementClickCount(adId);
    }
}
