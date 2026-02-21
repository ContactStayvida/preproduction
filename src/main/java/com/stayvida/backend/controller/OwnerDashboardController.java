package com.stayvida.backend.controller;

import java.math.BigDecimal;
import java.util.*;
// import org.springframework.beans.factory.annotation.Value;`

import com.stayvida.backend.service.BookingFlowService;
import com.stayvida.backend.service.BookingService;
// import com.stayvida.backend.service.CloudinaryService;
import com.stayvida.backend.service.ImageCompressionUtil;
import com.stayvida.backend.service.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

// import com.fasterxml.jackson.databind.ObjectMapper;
import com.stayvida.backend.dto.BookingRequest;
import com.stayvida.backend.dto.BookingResponse;
import com.stayvida.backend.dto.LockRoomRequest;
import com.stayvida.backend.dto.LockRoomResponse;
import com.stayvida.backend.security.ApiResponse;
import com.stayvida.backend.service.OwnerDashboardService;
import com.stayvida.backend.service.WalletService;
import com.stayvida.backend.exception.BookingExceptions.RoomLockException;
import com.stayvida.backend.exception.BookingExceptions.OtpRequiredException;

@RestController
@RequestMapping("/owner/dashboard")
public class OwnerDashboardController {

    @Autowired
    private OwnerDashboardService dashboardService;
    @Autowired
    private JwtUtil jwtUtil;

    private final BookingService bookingService;
    private final BookingFlowService bookingFlowService;
    private final WalletService walletService;

    public OwnerDashboardController(BookingService bookingService, BookingFlowService bookingFlowService,
            WalletService walletService) {
        this.bookingService = bookingService;
        this.bookingFlowService = bookingFlowService;
        this.walletService = walletService;
    }

    @GetMapping("/monthly-bookings") // owner dashbord dashbord page
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

    @GetMapping("/active-bookings") // owner dashbord dashbord page fetch all the booking not checked out or payment
                                    // pending
    public ResponseEntity<?> getActiveBookings() {
        try {
            int ownerId = (int) SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal();

            List<Map<String, Object>> data = dashboardService.getActiveBookingsForOwner(ownerId);
            System.out.println("OWNER ID USED: " + ownerId);
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

    @PutMapping("/booking/{bookingId}/status") // owner dashboard dashbord page and booking page update booking status
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

    @GetMapping("/all-bookings") // owner dashboard booking page fetch all bookings
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

    @GetMapping("/upcoming-bookings") // owner dashboard dashboard page fetch recent bookings only
                                      // five by checkin date
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

    @GetMapping("/{bookingId}/details") // owner dashboard all pages where booking is shown open booking fetch details
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

    @PatchMapping("/{roomId}/{hotelId}/status/{isEnable}") // enable-dissable room aka delete
    public ResponseEntity<?> updateRoomStatus(
            @PathVariable String roomId,
            @PathVariable String hotelId,
            @PathVariable boolean isEnable) {
        int ownerId = (int) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        boolean updated = dashboardService.updateRoomStatus(
                ownerId, roomId, isEnable);

        if (!updated) {
            return ApiResponse.notFound("Room not found or already in desired state");
        }

        return ApiResponse.success(
                Map.of(
                        "roomId", roomId,
                        "isEnable", isEnable,
                        "ownerId", ownerId),
                isEnable ? "Room enabled successfully" : "Room disabled successfully");
    }

    @PutMapping("/rooms/{roomId}") // update room (NO IMAGES)
    public ResponseEntity<?> updateRoom(
            @PathVariable String roomId,
            @RequestParam(required = false) String room_NO,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) String features, // JSON string
            @RequestParam(required = false) Integer price,
            @RequestParam(required = false) Integer maxAdults,
            @RequestParam(required = false) Integer maxChildren,
            @RequestParam(required = false) Integer bedCount) {
        int ownerId = (int) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        try {

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

    // ROOMIMAGE UPDATE HELPER FETCH ALL ROOM IMAGE
    @GetMapping("/rooms/{roomId}/images")
    public ResponseEntity<?> getRoomImagesBase64(@PathVariable String roomId) {

        int ownerId = (int) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        Map<Integer, String> images = dashboardService.getRoomImagesBase64WithId(roomId, ownerId);

        if (images.isEmpty()) {
            return ApiResponse.notFound("No images found for this room");
        }

        return ApiResponse.success(
                images,
                "Room images fetched successfully");
    }

    // delete image from room
    @DeleteMapping("/rooms/remove-image")
    public ResponseEntity<?> removeRoomImage(@RequestBody Map<String, Object> request) {
        try {
            int ownerId = (int) SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal();
            String roomId = (String) request.get("roomId");
            Integer imageIndex = (Integer) request.get("imageIndex"); // optional, 1-based
            String base64Image = (String) request.get("base64Image"); // optional alternative

            if (roomId == null || (imageIndex == null && base64Image == null)) {
                return ApiResponse.badRequest("roomId and either imageIndex or base64Image are required");
            }

            boolean removed = dashboardService.removeRoomImage(roomId, imageIndex, base64Image, ownerId);

            if (!removed) {
                return ApiResponse.notFound("Image not found or already removed");
            }

            return ApiResponse.success(null, "Image removed successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.serverError("Failed to remove image: " + e.getMessage());
        }
    }

    // insert image in room
    @PostMapping("/rooms/{roomId}/update-image")
    public ResponseEntity<?> addRoomImages(
            @PathVariable String roomId,
            @RequestParam("images") MultipartFile[] images) {

        try {
            int ownerId = (int) SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal();
            if (images == null || images.length == 0) {
                return ApiResponse.badRequest("At least one image is required");
            }

            List<String> base64Images = new ArrayList<>();

            for (MultipartFile image : images) {
                if (image.isEmpty())
                    continue;

                // ✅ Compress EACH image separately
                String base64 = ImageCompressionUtil
                        .processImageToBase64(image.getBytes());

                base64Images.add(base64);
            }

            if (base64Images.isEmpty()) {
                return ApiResponse.badRequest("No valid images found");
            }

            boolean updated = dashboardService.appendRoomImages(roomId, base64Images, ownerId);

            if (!updated) {
                return ApiResponse.notFound("Room not found");
            }

            return ApiResponse.success(
                    Map.of("count", base64Images.size()),
                    "Images added successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.serverError("Failed to add images");
        }
    }

    @PutMapping(value = "/hotels/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateHotel(
            // @PathVariable int hotelId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) Boolean isForEvent,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String phone_NO,
            @RequestParam(required = false) String country_code,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(required = false) String amenities,
            @RequestParam(required = false) String longitude,
            @RequestParam(required = false) String latitude) {

        try {

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

            // ✅ BASE64 IMAGE HANDLING
            if (image != null && !image.isEmpty()) {

                String base64Image = ImageCompressionUtil
                        .processImageToBase64(image.getBytes());

                updates.put("images", base64Image);
            }

            if (updates.isEmpty()) {
                return ApiResponse.badRequest("No fields provided for update");
            }
            int ownerId = (int) SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal();

            boolean updated = dashboardService.updateHotel(ownerId, updates);

            if (!updated) {
                return ApiResponse.notFound("Hotel not found or not owned by you");
            }

            return ApiResponse.success(updates, "Hotel updated successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.serverError("Failed to update hotel: " + e.getMessage());
        }
    }

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

    @PostMapping("/lock-room")
    public ResponseEntity<LockRoomResponse> lockRoomod(
            @RequestBody LockRoomRequest request) {

        Integer ownerId = (int) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        LockRoomResponse response = bookingService.lockRoomod(ownerId, request);
        return ResponseEntity.ok(response);
    }

    // @PostMapping("/create")
    // public ResponseEntity<BookingResponse> createBooking(
    // @RequestBody BookingRequest request) {

    // Integer _ID"H-"+ ownerIdt()
    // .getAuthentication()
    // .getPrincipal();

    // BookingResponse response = bookingService.createBooking(userId, request);
    // return ResponseEntity.ok(response);
    // }

    @PostMapping("/create")
    public ResponseEntity<?> initiateBooking(@RequestBody BookingRequest request) {
        try {
            BookingResponse response = bookingFlowService.initiateBooking(request);
            return ApiResponse.success(response, "Booking created");

        } catch (OtpRequiredException e) {
            return ApiResponse.success(request.getEmail(), e.getMessage());

        } catch (RoomLockException e) {
            return ApiResponse.badRequest(e.getMessage());

        } catch (Exception e) {
            return ApiResponse.badRequest(e.getMessage());
        }
    }

    @GetMapping("/roomdetails/{roomID}")
    public ResponseEntity<Map<String, Object>> getRoomDetails(
            @PathVariable String roomID) {
        List<Map<String, Object>> data = dashboardService.getRoomDetails(roomID);
        return ApiResponse.success(data, "Room details fetched successfully");
    }

    @GetMapping("/wallet")
    public ResponseEntity<?> getWallet() {
        int ownerId = (int) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        String hotel_ID = "H-" + ownerId;
        return ApiResponse.success(walletService.getWallet(hotel_ID), "Wallet fetched successfully");
    }

    @PostMapping("/wallet/create")
    public ResponseEntity<?> createWalletAccount() {
        try {
            int ownerId = (int) SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal();
            String hotel_ID = "H-" + ownerId;

            walletService.createAccount(hotel_ID);

            return ApiResponse.success(
                    Map.of("hotelId", hotel_ID),
                    "Wallet account created successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.serverError("Failed to create wallet account");
        }
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> requestWithdraw(@RequestBody Map<String, Object> body) {

        int ownerId = (int) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        String hotelId = "H-" + ownerId;

        try {

            BigDecimal amount = new BigDecimal(body.get("amount").toString());

            walletService.requestWithdraw(
                    hotelId,
                    amount);

            return ResponseEntity.ok(
                    Map.of(
                            "hotelId", hotelId,
                            "amount", amount,
                            "status", "PENDING",
                            "message", "Withdraw request submitted successfully"));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Internal server error"));
        }
    }
}
