package com.stayvida.backend.controller;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import com.stayvida.backend.service.CloudinaryService;
import com.stayvida.backend.service.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stayvida.backend.security.ApiResponse;
import com.stayvida.backend.service.OwnerDashboardService;

@RestController
@RequestMapping("/owner/dashboard")
public class OwnerDashboardController {

    @Autowired
    private OwnerDashboardService dashboardService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private CloudinaryService cloudinaryService;

    @GetMapping("/monthly-bookings")
    public ResponseEntity<?> getMonthlyBookings() {
        try {
            int ownerId = (int) SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal();

            Map<String, Object> data = dashboardService.getMonthlyBookingsForOwner(ownerId);

            return ApiResponse.success(
                    data,
                    "Monthly bookings fetched successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.serverError("Failed to fetch monthly bookings");
        }
    }

    @GetMapping("/active-bookings")
    public ResponseEntity<?> getActiveBookings() {
        try {
            int ownerId = (int) SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal();

            List<Map<String, Object>> data = dashboardService.getActiveBookingsForOwner(ownerId);

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

    @PutMapping("/booking/{bookingId}/status")
    public ResponseEntity<?> updateBookingStatus(
            @PathVariable String bookingId,
            @RequestParam String status) {
        try {
            int ownerId = (int) SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal();
            boolean updated = dashboardService.updateBookingStatus(bookingId, status, ownerId);

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

    @GetMapping("/all-bookings")
    public ResponseEntity<?> getAllBookings() {
        try {
            int ownerId = (int) SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal();

            List<Map<String, Object>> data = dashboardService.getAllBookingsForOwner(ownerId);

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

    @GetMapping("/upcoming-bookings")
    public ResponseEntity<?> getUpcomingBookings() {
        try {
            int ownerId = (int) SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal();

            List<Map<String, Object>> data = dashboardService.getUpcomingBookings(ownerId);

            if (data == null || data.isEmpty()) {
                return ApiResponse.notFound("No upcoming bookings found");
            }

            return ApiResponse.success(
                    data,
                    "Upcoming bookings fetched successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.serverError("Failed to fetch upcoming bookings");
        }
    }

    @GetMapping("/{bookingId}/details")
    public ResponseEntity<?> getBookingDetails(@PathVariable String bookingId) {
        try {
            int ownerId = (int) SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal();

            Map<String, Object> data = dashboardService.getBookingDetails(bookingId, ownerId);

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

    @PatchMapping("/{roomId}/{hotelId}/status/{isEnable}") // enable-dissable
    public ResponseEntity<?> updateRoomStatus(
            @PathVariable String roomId,
            @PathVariable int hotelId,
            @PathVariable boolean isEnable) {

        int ownerId = (int) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        boolean updated = dashboardService.updateRoomStatus(
                ownerId, roomId, hotelId, isEnable);

        if (!updated) {
            return ApiResponse.notFound("Room not found or already in desired state");
        }

        return ApiResponse.success(
                Map.of(
                        "roomId", roomId,
                        "hotelId", hotelId,
                        "isEnable", isEnable,
                        "ownerId", ownerId),
                isEnable ? "Room enabled successfully" : "Room disabled successfully");
    }

    @PutMapping("/rooms/{roomId}")
    public ResponseEntity<?> updateRoomWithImages(
            @PathVariable String roomId,
            @RequestParam(required = false) String room_NO,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) String features, // JSON string
            @RequestParam(required = false) MultipartFile[] images,
            @RequestParam(required = false) Integer price,
            @RequestParam(required = false) Integer maxAdults,
            @RequestParam(required = false) Integer maxChildren,
            @RequestParam(required = false) Integer bedCount) {
        try {
            int ownerId = (int) SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal();

            Map<String, Object> updates = new HashMap<>();

            if (room_NO != null)
                updates.put("room_NO", room_NO);
            if (roomType != null)
                updates.put("roomType", roomType);
            if (features != null)
                updates.put("features", features);
            if (price != null)
                updates.put("price", price);
            if (maxAdults != null)
                updates.put("maxAdults", maxAdults);
            if (maxChildren != null)
                updates.put("maxChildren", maxChildren);
            if (bedCount != null)
                updates.put("bedCount", bedCount);

            // Handle images upload if provided
            if (images != null && images.length > 0) {
                List<String> imageNames = new ArrayList<>();
                List<String> imageUrls = new ArrayList<>();

                for (MultipartFile file : images) {
                    if (file.isEmpty())
                        continue;

                    String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

                    // Upload to Cloudinary
                    Map uploadResult = cloudinaryService.uploadImage(file, fileName);

                    imageNames.add(fileName);
                    imageUrls.add(uploadResult.get("secure_url").toString());
                }

                if (!imageNames.isEmpty()) {
                    // Store JSON string of image names in DB
                    ObjectMapper mapper = new ObjectMapper();
                    updates.put("images", mapper.writeValueAsString(imageNames));

                    // Optional: include URLs in response
                    updates.put("imageUrls", imageUrls);
                }
            }

            if (updates.isEmpty()) {
                return ApiResponse.badRequest("No fields provided for update");
            }

            boolean updated = dashboardService.updateRoom(roomId, ownerId, updates);

            if (!updated) {
                return ApiResponse.notFound("Room not found or not owned by you");
            }

            return ApiResponse.success(updates, "Room updated successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.serverError("Failed to update room: " + e.getMessage());
        }
    }

    @PutMapping("/hotels/{hotelId}")
    public ResponseEntity<?> updateHotel(
            @PathVariable int hotelId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type, // 'Hotel','Resort','Villa','Guest House'
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) Boolean isForEvent,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String phone_NO,
            @RequestParam(required = false) String country_code,
            @RequestParam(required = false) String tags, // JSON string
            @RequestParam(required = false) MultipartFile image, // Single file
            @RequestParam(required = false) String amenities, // JSON string
            @RequestParam(required = false) String longitude,
            @RequestParam(required = false) String latitude) {
        try {
            int ownerId = (int) SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal();

            Map<String, Object> updates = new HashMap<>();

            if (name != null)
                updates.put("name", name);
            if (type != null)
                updates.put("type", type);
            if (destination != null)
                updates.put("destination", destination);
            if (isForEvent != null)
                updates.put("isForEvent", isForEvent);
            if (description != null)
                updates.put("description", description);
            if (phone_NO != null)
                updates.put("phone_NO", phone_NO);
            if (country_code != null)
                updates.put("country_code", country_code);
            if (tags != null)
                updates.put("tags", tags);
            if (amenities != null)
                updates.put("amenities", amenities);
            if (longitude != null)
                updates.put("longitude", longitude);
            if (latitude != null)
                updates.put("latitude", latitude);

            // Handle image if provided
            if (image != null && !image.isEmpty()) {
                String fileName = UUID.randomUUID() + "_" + image.getOriginalFilename();
                Map uploadResult = cloudinaryService.uploadImage(image, fileName);
                updates.put("images", fileName); // Save filename in DB
                updates.put("imageUrl", uploadResult.get("secure_url").toString()); // optional for response
            }

            if (updates.isEmpty()) {
                return ApiResponse.badRequest("No fields provided for update");
            }

            boolean updated = dashboardService.updateHotel(hotelId, ownerId, updates);

            if (!updated) {
                return ApiResponse.notFound("Hotel not found or not owned by you");
            }

            return ApiResponse.success(updates, "Hotel updated successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.serverError("Failed to update hotel: " + e.getMessage());
        }
    }

    @Value("${cloudinary.urlPrefix}")
    private String cloudinaryPrefix;

    @GetMapping("/hotels-profile")
    public ResponseEntity<?> getOwnerHotels() {
        try {
            int ownerId = (int) SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal();

            List<Map<String, Object>> hotels = dashboardService.getHotelsByOwner(ownerId);

            if (hotels == null || hotels.isEmpty()) {
                return ApiResponse.notFound("No hotels found");
            }

            // 🔗 Add Cloudinary prefix to images
            hotels.forEach(hotel -> {
                Object imagesObj = hotel.get("images");
                if (imagesObj instanceof List<?> images) {
                    List<String> fullUrls = images.stream()
                            .filter(Objects::nonNull)
                            .map(img -> img.toString().startsWith("http")
                                    ? img.toString()
                                    : cloudinaryPrefix + img)
                            .toList();
                    hotel.put("images", fullUrls);
                }
            });

            return ApiResponse.success(hotels, "Hotels fetched successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.serverError("Failed to fetch hotels");
        }
    }

    @GetMapping("/allrooms")
    public ResponseEntity<?> getallrooms() {
        try {
            int ownerId = (int) SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal();

            List<Map<String, Object>> data = dashboardService.getallrooms(ownerId);

            if (data == null || data.isEmpty()) {
                return ApiResponse.notFound("No rooms found");
            }

            // 🔗 Add Cloudinary prefix to images
            data.forEach(room -> {
                Object imagesObj = room.get("images");
                if (imagesObj instanceof List<?> images) {
                    List<String> fullUrls = images.stream()
                            .filter(Objects::nonNull)
                            .map(img -> img.toString().startsWith("http")
                                    ? img.toString()
                                    : cloudinaryPrefix + img)
                            .toList();
                    room.put("images", fullUrls);
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

}
