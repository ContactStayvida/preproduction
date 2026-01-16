package com.stayvida.backend.controller;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import com.stayvida.backend.dto.HotelVerificationUpdate;
import com.stayvida.backend.security.ApiResponse;
import com.stayvida.backend.service.AdminDashboardService;
import com.stayvida.backend.service.LookupService;
import com.stayvida.backend.service.OwnerDashboardService;
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

    @Autowired
    private OwnerDashboardService dashboardService;

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

    // ---------- ADD ----------
    @PostMapping("/amenity")
    public String addAmenity(@RequestParam String name) {
        service.addAmenity(name);
        return "Amenity added successfully";
    }

    // ---------- ADD ----------
    @PostMapping("/tag")
    public String addTag(@RequestParam String name) {
        service.addTag(name);
        return "Tag added successfully";
    }

    // fetch all room of thee hotel by hotel ID
    @GetMapping("/allrooms/{hotelId}")
    public ResponseEntity<?> getallrooms(@PathVariable String hotelId) {
        try {

            List<Map<String, Object>> data = dashboardService.getallrooms(hotelId);

            if (data == null || data.isEmpty()) {
                return ApiResponse.notFound("No rooms found");
            }

            // 🖼️ Convert images to Base64 data URLs
            data.forEach(room -> {
                Object imagesObj = room.get("images");

                if (imagesObj instanceof List<?> images) {
                    List<String> base64Images = images.stream()
                            .filter(Objects::nonNull)
                            .map(img -> {
                                String value = img.toString().trim();

                                // Already prefixed → keep it
                                if (value.startsWith("data:image")) {
                                    return value;
                                }

                                // Assume DB contains pure Base64
                                return "data:image/jpeg;base64," + value;
                            })
                            .toList();

                    room.put("images", base64Images);
                }
            });

            return ApiResponse.success(
                    data,
                    "All Rooms fetched successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.serverError("Failed to fetch rooms");
        }
    }

    // fetch all hotel
    @GetMapping("/allhotels")
    public ResponseEntity<?> getallhotels() {
        try {

            List<Map<String, Object>> data = dashboardService.getallhotels();

            if (data == null || data.isEmpty()) {
                return ApiResponse.notFound("No hotels found");
            }

            // 🖼️ Convert images to Base64 data URLs
            data.forEach(hotel -> {
                Object imagesObj = hotel.get("images");

                if (imagesObj != null) {
                    String value = imagesObj.toString().trim();

                    if (!value.startsWith("data:image")) {
                        value = "data:image/jpeg;base64," + value;
                    }

                    hotel.put("images", value);
                }

            });

            return ApiResponse.success(
                    data,
                    "All Hotels fetched successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.serverError("Failed to fetch hotels");
        }
    }

    // fetch only active bookings of all hotels
    @GetMapping("/active-bookings") // fetch all the booking not checked out or payment pending
    public ResponseEntity<?> getActiveBookings() {
        try {

            List<Map<String, Object>> data = dashboardService.getActiveBookings();

            if (data == null || data.isEmpty()) {
                return ApiResponse.notFound("No booking found");
            }

            return ApiResponse.success(
                    data,
                    "Active bookings fetched successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.serverError("Failed to fetch active bookings");
        }
    }

    // fetch all bookings of all hotels
    @GetMapping("/all-bookings")
    public ResponseEntity<?> getAllBookings() {
        try {
            List<Map<String, Object>> data = dashboardService.getAllBookings();

            if (data == null || data.isEmpty()) {
                return ApiResponse.notFound("No bookings found");
            }

            return ApiResponse.success(
                    data,
                    "All bookings fetched successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.serverError("Failed to fetch all bookings");
        }
    }

    @GetMapping("/{bookingId}/details") // admin dashboard all pages where booking is shown open booking fetch details
    public ResponseEntity<?> getBookingDetails(@PathVariable String bookingId) {
        try {
            Map<String, Object> data = dashboardService.getBookingDetails(bookingId);

            if (data == null) {
                return ApiResponse.notFound("Booking not found");
            }

            return ApiResponse.success(
                    data,
                    "Booking details fetched successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.serverError("Failed to fetch booking details");
        }
    }

    @PutMapping("/booking/{bookingId}/status") // owner dashboard dashbord page and booking page update booking status
    public ResponseEntity<?> updateBookingStatus(
            @PathVariable String bookingId,
            @RequestParam String status) {
        try {
            boolean updated = dashboardService.updateBookingStatus(bookingId, status);

            if (!updated) {
                return ApiResponse.notFound("Booking not found");
            }

            return ApiResponse.success(null, "Booking status updated successfully");

        } catch (IllegalArgumentException ex) {
            return ApiResponse.badRequest(ex.getMessage());

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.serverError("Failed to update booking status");
        }
    }

    @PatchMapping("/{roomId}/{hotelId}/status/{isEnable}") // enable-dissable room aka delete
    public ResponseEntity<?> updateRoomStatus(
            @PathVariable String roomId,
            @PathVariable int hotelId,
            @PathVariable boolean isEnable) {

        boolean updated = dashboardService.updateRoomStatusad(
                hotelId, roomId, isEnable);

        if (!updated) {
            return ApiResponse.notFound("Room not found or already in desired state");
        }

        return ApiResponse.success(
                Map.of(
                        "roomId", roomId,
                        "isEnable", isEnable,
                        "hotelId", hotelId),
                isEnable ? "Room enabled successfully" : "Room disabled successfully");
    }

    @GetMapping("/hotels-profile/{hotelId}")
    public ResponseEntity<?> getOwnerHotels(@PathVariable String hotelId) {
        try {

            List<Map<String, Object>> hotels = dashboardService.getHotelsByOwner(hotelId);

            if (hotels == null || hotels.isEmpty()) {
                return ApiResponse.notFound("No hotels found");
            }

            // ✅ NO URL PREFIXING – BASE64 ONLY
            hotels.forEach(hotel -> {
                Object imagesObj = hotel.get("images");

                if (imagesObj instanceof String base64 && !base64.isBlank()) {
                    // Single Base64 image
                    hotel.put("images", base64);
                } else if (imagesObj instanceof List<?> list) {
                    // Multiple Base64 images
                    hotel.put("images", list);
                } else {
                    hotel.put("images", List.of());
                }
            });

            return ApiResponse.success(hotels, "Hotels fetched successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.serverError("Failed to fetch hotels");
        }
    }
}
