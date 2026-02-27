package com.stayvida.backend.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stayvida.backend.model.Profile;
import com.stayvida.backend.security.ApiResponse;
import com.stayvida.backend.service.ProfileService;
import com.stayvida.backend.service.BookingService;
import com.stayvida.backend.service.OwnerDashboardService;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private OwnerDashboardService dashboardService;

    @PostMapping("/UserID")
    public ResponseEntity<?> createOrUpdate(
            // @PathVariable Integer UserID,
            @RequestBody Profile profile) {
        int UserID = (int) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        profile.setUserID(UserID);
        Profile saved = profileService.saveProfile(profile);

        return ApiResponse.success(saved, "Profile updated successfully");
    }

    @GetMapping("/UserID")
    public ResponseEntity<?> get() {

        int UserID = (int) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        Profile profile = profileService.getProfile(UserID);

        if (profile == null) {
            return ApiResponse.notFound("Profile not found for userID: " + UserID);
        }

        return ApiResponse.success(profile, "Profile fetched successfully");
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

    @PatchMapping("/Update") // UPDATE PROFILE
    public ResponseEntity<?> partialUpdate(@RequestBody Profile profile) {

        int userID;

        try {
            Object principal = SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal();

            if (principal == null || "anonymousUser".equals(principal)) {
                throw new IllegalStateException("User not authenticated");
            }

            userID = Integer.parseInt(principal.toString());

        } catch (Exception e) {
            return ApiResponse.unauthorized("JWT missing or invalid");
        }

        System.out.println("Variable 'userID' is type: " + ((Object) userID).getClass().getSimpleName() + " " + userID);

        profile.setUserID(userID);

        Profile updated = profileService.partialUpdate(userID, profile);

        if (updated == null) {
            return ApiResponse.notFound("Profile not found for userID: " + userID);
        }

        return ApiResponse.success(updated, "Profile updated successfully");
    }

    @GetMapping("/history")
    public ResponseEntity<?> getLatestBookings() {
        int UserID = (int) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        return ResponseEntity.ok(dashboardService.getLatestBookingsByUser(UserID));
    }
}
