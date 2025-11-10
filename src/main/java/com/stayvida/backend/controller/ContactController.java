package com.stayvida.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 📩 POST: Submit Contact Form
    @PostMapping("/submit")
    public Map<String, Object> submitContactForm(@RequestBody Map<String, String> formData) {
        String fullName = formData.get("fullName");
        String email = formData.get("email");
        String phone = formData.get("phoneNumber");
        String subject = formData.get("subject");
        String message = formData.get("message");

        String sql = "INSERT INTO contact_form (full_NAME, email_ID, phone_NUMBER, subject, message) VALUES (?, ?, ?, ?, ?)";
        int rows = jdbcTemplate.update(sql, fullName, email, phone, subject, message);

        Map<String, Object> response = new HashMap<>();
        response.put("status", rows > 0 ? "success" : "failed");
        response.put("message", rows > 0 ? "Form submitted successfully!" : "Error submitting form");
        return response;
    }

    // 📅 GET: View All Contact Submissions (Grouped by Date)
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
}
