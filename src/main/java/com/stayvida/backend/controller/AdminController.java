package com.stayvida.backend.controller;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;

import com.stayvida.backend.dto.HotelVerificationUpdate;
import com.stayvida.backend.security.ApiResponse;
import com.stayvida.backend.service.AdminDashboardService;
import com.stayvida.backend.service.LookupService;
import com.stayvida.backend.repository.HotelRepository;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired
    private AdminDashboardService adminDashboardService;
    @Autowired
    private HotelRepository hotelRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private LookupService service;

    // @GetMapping("/test")
    // public String testAdminAccess() {
    // return "✅ Supabase JWT authentication successful!";
    // }

    // public AdminController(AdminDashboardService adminDashboardService) {
    // this.adminDashboardService = adminDashboardService;
    // }
    // get monthly revenue
    @GetMapping("/monthly-revenue")
    public Map<String, BigDecimal> getMonthlyRevenue() {
        return adminDashboardService.getCurrentMonthRevenue();
    }

    // update verification status OF HOTEL
    @PutMapping("/update-verification")
    public ResponseEntity<?> updateVerificationStatus(@RequestBody HotelVerificationUpdate request) {
        try {
            if (request.getHotelId() == null || request.getStatus() == null || request.getStatus().isEmpty()) {
                return ApiResponse.badRequest("Invalid input: hotelId and status are required");
            }

            int rows = hotelRepository.updateVerificationStatus(
                    request.getHotelId(),
                    request.getStatus(),
                    request.getRemark());

            if (rows > 0) {
                return ApiResponse.success(Map.of(
                        "hotelId", request.getHotelId(),
                        "status", request.getStatus(),
                        "remark", request.getRemark()), "Hotel verification status updated successfully!");
            } else {
                return ApiResponse.badRequest("Hotel not found"); // or make a notFound() if you like
            }

        } catch (IllegalArgumentException e) {
            return ApiResponse.badRequest("Invalid data format: " + e.getMessage());
        } catch (SecurityException e) {
            return ApiResponse.unauthorized("Unauthorized to update verification status");
        } catch (Exception e) {
            return ApiResponse.serverError("Error updating verification status: " + e.getMessage());
        }
    }

    // contactus result
    @GetMapping("/all")
    public List<Map<String, Object>> getAllContacts() {
        String sql = "SELECT * FROM contact_form ORDER BY created_AT DESC";

        List<Map<String, Object>> allContacts = jdbcTemplate.query(sql, (ResultSet rs, int rowNum) -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("ID", rs.getInt("ID"));
            map.put("fullName", rs.getString("full_NAME"));
            map.put("email", rs.getString("email_ID"));
            map.put("phoneNumber", rs.getString("phone_NUMBER"));
            map.put("subject", rs.getString("subject"));
            map.put("message", rs.getString("message"));
            map.put("createdAt", rs.getTimestamp("created_AT"));
            return map;
        });

        // Group by date
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

        for (Map<String, Object> contact : allContacts) {
            Date createdAt = (Date) contact.get("createdAt");
            String dateKey = sdf.format(createdAt);

            grouped.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(contact);
        }

        // Convert grouped map to desired JSON structure
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : grouped.entrySet()) {
            Map<String, Object> dateGroup = new LinkedHashMap<>();
            dateGroup.put("date", entry.getKey());
            dateGroup.put("data", entry.getValue());
            dateGroup.put("count", entry.getValue().size());
            result.add(dateGroup);
        }

        return result;
    }

    // ---------- ADD ----------
    @PostMapping("/feature")
    public String addFeature(@RequestParam String name) {
        service.addFeature(name);
        return "Feature added successfully";
    }

    @PostMapping("/amenity")
    public String addAmenity(@RequestParam String name) {
        service.addAmenity(name);
        return "Amenity added successfully";
    }

    @PostMapping("/tag")
    public String addTag(@RequestParam String name) {
        service.addTag(name);
        return "Tag added successfully";
    }

}
