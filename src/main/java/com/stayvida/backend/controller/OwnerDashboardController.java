package com.stayvida.backend.controller;

import java.util.*;

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
            "Active bookings fetched successfully"
        );

    } catch (Exception e) {
        e.printStackTrace();

        // ⭐ Case 3: Error occurred
        return ApiResponse.serverError("Failed to fetch active bookings");
    }
}


}
