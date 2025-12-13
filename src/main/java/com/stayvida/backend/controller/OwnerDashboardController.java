package com.stayvida.backend.controller;

import java.util.*;
import com.stayvida.backend.service.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.stayvida.backend.security.ApiResponse;
import com.stayvida.backend.service.OwnerDashboardService;

@RestController
@RequestMapping("/owner/dashboard")
public class OwnerDashboardController {

    @Autowired
    private OwnerDashboardService dashboardService;
    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/{ownerId}/monthly-bookings")
    public ResponseEntity<?> getMonthlyBookings(@PathVariable int ownerId) {
        try {
            Map<String, Object> data = dashboardService.getMonthlyBookingsForOwner(ownerId);
            return ApiResponse.success(data, "Monthly bookings fetched successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.serverError("Failed to fetch monthly bookings");
        }
    }

    @GetMapping("/{ownerId}/active-bookings")
    public ResponseEntity<?> getActiveBookings(@PathVariable int ownerId) {
        try {
            List<Map<String, Object>> data = dashboardService.getActiveBookingsForOwner(ownerId);

            // ⭐ Case 1: No bookings found
            if (data == null || data.isEmpty()) {
                return ApiResponse.notFound("No booking found");
            }

            // ⭐ Case 2: Bookings found
            return ApiResponse.success(
                    data,
                    "Active bookings fetched successfully");

        } catch (Exception e) {
            e.printStackTrace();

            // ⭐ Case 3: Error occurred
            return ApiResponse.serverError("Failed to fetch active bookings");
        }
    }

    @PutMapping("/booking/{bookingId}/status")
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

    @GetMapping("/{ownerId}/all-bookings")
    public ResponseEntity<?> getAllBookings(@PathVariable int ownerId) {
        try {
            List<Map<String, Object>> data = dashboardService.getAllBookingsForOwner(ownerId);

            if (data == null || data.isEmpty()) {
                return ApiResponse.notFound("No bookings found");
            }

            return ApiResponse.success(data, "All bookings fetched successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.serverError("Failed to fetch all bookings");
        }
    }

    @GetMapping("/{ownerId}/upcoming-bookings")
    public ResponseEntity<?> getUpcomingBookings(@PathVariable int ownerId) {
        try {
            List<Map<String, Object>> data = dashboardService.getUpcomingBookings(ownerId);

            if (data == null || data.isEmpty()) {
                return ApiResponse.notFound("No upcoming bookings found");
            }

            return ApiResponse.success(data, "Upcoming bookings fetched successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.serverError("Failed to fetch upcoming bookings");
        }
    }

    @GetMapping("/{bookingId}/details")
    public ResponseEntity<?> getBookingDetails(@PathVariable String bookingId) {
        try {
            Map<String, Object> data = dashboardService.getBookingDetails(bookingId);

            if (data == null) {
                return ApiResponse.notFound("Booking not found");
            }

            return ApiResponse.success(data, "Booking details fetched successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.serverError("Failed to fetch booking details");
        }
    }

    @DeleteMapping("/{roomId}/{hotelId}/delete-room")
    public ResponseEntity<?> deleteRoom(
            @PathVariable String roomId,
            @PathVariable int hotelId,
            @RequestHeader("Authorization") String authHeader) {

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ApiResponse.unauthorized("Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.validateToken(token)) {
                return ApiResponse.unauthorized("Invalid token");
            }

            int ownerId = (int) jwtUtil.extractClaim(token, "ID"); // extract ownerId from JWT

            boolean deleted = dashboardService.deleteRoom(ownerId, roomId, hotelId);

            if (!deleted) {
                return ApiResponse.notFound("Room not found");
            }

            Map<String, Object> response = Map.of(
                    "roomId", roomId,
                    "deleted", true,
                    "hotelId", hotelId,
                    "ownerId", ownerId);

            return ApiResponse.success(response, "Room deleted successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.serverError("Failed to delete room");
        }
    }
}
