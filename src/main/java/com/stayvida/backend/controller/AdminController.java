package com.stayvida.backend.controller;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stayvida.backend.dto.HotelVerificationUpdate;
import com.stayvida.backend.dto.UserListDTO;
import com.stayvida.backend.model.Amenity;
import com.stayvida.backend.model.Feature;
import com.stayvida.backend.model.Register;
import com.stayvida.backend.model.Tag;
import com.stayvida.backend.security.ApiResponse;
import com.stayvida.backend.service.AdminDashboardService;
import com.stayvida.backend.service.ImageCompressionUtil;
import com.stayvida.backend.service.LookupService;
import com.stayvida.backend.service.OwnerDashboardService;
import com.stayvida.backend.service.UserService;
import com.stayvida.backend.repository.HotelRepository;
import com.stayvida.backend.repository.RegisterRepository;
import com.stayvida.backend.repository.RoomregisterRepository;
import com.stayvida.backend.service.WalletService;
import com.stayvida.backend.service.ledgerService;

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
    private WalletService walletService;
    @Autowired
    private ledgerService ledgerservice;

    @Autowired
    private OwnerDashboardService dashboardService;
    @Autowired
    private RegisterRepository registerRepository;
    @Autowired
    private RoomregisterRepository roomRegisterRepository; // 🔹 inject here
    @Autowired
    private final UserService userService;

    public AdminController(UserService userService, WalletService walletService, ledgerService ledgerService) {
        this.userService = userService;
        this.walletService = walletService;
        this.ledgerservice = ledgerService;
    }

    // @GetMapping("/monthly-revenue")
    // public Map<String, Object> getMonthlyRevenue() {
    // return Map.of(
    // "totalRevenue (Current Month)",
    // adminDashboardService.getCurrentMonthRevenue(),
    // "totalBooking", adminDashboardService.totalBooking(),
    // "last 6 MonthRevenue", adminDashboardService.getLast6MonthRevenue(),
    // "hotelCount", adminDashboardService.hotelCount(),
    // "totalOccupancy", adminDashboardService.totalOccupancy());
    // }

    @GetMapping("/monthly-revenue")
    public Map<String, Object> getDashboardData() {
        return adminDashboardService.getDashboardData();
    }

    @GetMapping("/static-data")
    public Map<String, Object> getDashStaticData() {
        return adminDashboardService.getDashStaticData();
    }

    @GetMapping("/booking-data")
    public Map<String, Object> getDashBookingData() {
        return adminDashboardService.getDashBookingData();
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

    // // ---------- ADD ----------
    // @PostMapping("/feature")
    // public String addFeature(@RequestParam String name) {
    // service.addFeature(name);
    // return "Feature added successfully";
    // }

    // // ---------- ADD ----------
    // @PostMapping("/amenity")
    // public String addAmenity(@RequestParam String name) {
    // service.addAmenity(name);
    // return "Amenity added successfully";
    // }

    // // ---------- ADD ----------
    // @PostMapping("/tag")
    // public String addTag(@RequestParam String name) {
    // service.addTag(name);
    // return "Tag added successfully";
    // }

    @PostMapping("/features")
    public ResponseEntity<Map<String, Object>> addFeatures(
            @RequestBody List<String> names) {

        Map<String, List<String>> result = service.addFeatures(names);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("inserted", result.get("inserted"));
        response.put("duplicates", result.get("duplicates"));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/amenity")
    public ResponseEntity<Map<String, Object>> addAmenities(
            @RequestBody List<String> names) {

        Map<String, List<String>> result = service.addAmenities(names);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("inserted", result.get("inserted"));
        response.put("duplicates", result.get("duplicates"));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/tags")
    public ResponseEntity<Map<String, Object>> addTags(@RequestBody List<String> names) {

        Map<String, List<String>> result = service.addTags(names);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("inserted", result.get("inserted"));
        response.put("duplicates", result.get("duplicates"));

        return ResponseEntity.ok(response);
    }

    // FETCH ALL FEATURES AMENITIES AND TAGS

    @GetMapping("/features")
    public List<Feature> getFeatures() {
        return service.getFeatures();
    }

    @GetMapping("/amenities")
    public List<Amenity> getAmenities() {
        return service.getAmenities();
    }

    @GetMapping("/tags")
    public List<Tag> getTags() {
        return service.getTags();
    }

    // fetch all room of the hotel by hotel ID
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

    @GetMapping("/pendinghotels")
    public ResponseEntity<?> getpendinghotels() {
        try {

            List<Map<String, Object>> data = dashboardService.getpendinghotels();

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

    // fetch booking details
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

    // update booking status
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

    // enable-dissable room aka delete
    @PatchMapping("/{roomId}/{hotelId}/status/{isEnable}") // enable-dissable room aka delete
    public ResponseEntity<?> updateRoomStatus(
            @PathVariable String roomId,
            @PathVariable String hotelId,
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

    // fetch hotel profile
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

    /// ==============================///
    /// add hotel ///
    /// ==============================///
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> registerHotel(
            @RequestPart("data") String hotelJson,
            @RequestPart("OwnerId") int OwnerId,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            Register register = mapper.readValue(hotelJson, Register.class);

            register.setOwner_ID(OwnerId);
            register.setHotel_ID("H-" + OwnerId);

            String imageBase64 = null;

            if (imageFile != null && !imageFile.isEmpty()) {
                imageBase64 = ImageCompressionUtil
                        .processImageToBase64(imageFile.getBytes());

                // ✅ store BASE64 directly
                register.setImages(imageBase64);
            }

            String existingHotelId = registerRepository.findHotelIdByOwnerId(register.getOwner_ID());

            String hotelId = registerRepository.saveHotel(register);

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("hotelId", hotelId);
            // responseBody.put("imageBase64", imageBase64);

            if (existingHotelId != null) {
                return ApiResponse.success(
                        responseBody,
                        "Hotel already exists. You can add or edit rooms.");
            }

            return ApiResponse.created(responseBody, "Hotel registered successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.serverError("Error registering hotel: " + e.getMessage());
        }
    }

    /// ==============================///
    /// add room ///
    /// ==============================///
    @PostMapping("/register_room_with_images")
    public ResponseEntity<Map<String, Object>> registerRoomWithImages(

            @RequestParam("hotelId") String hotelId,
            @RequestParam("roomType") String roomType,
            @RequestParam("roomNumber") String roomNumber,
            @RequestParam("features") String featuresJson,
            @RequestParam("maxAdults") int maxAdults,
            @RequestParam("maxChildren") int maxChildren,
            @RequestParam("bedCount") int bedCount,
            @RequestParam("price") int price,
            @RequestParam("images") MultipartFile[] files) {

        try {

            // 🔴 DUPLICATE CHECK (before upload)

            if (roomRegisterRepository.roomExists(hotelId, roomNumber)) {
                return ApiResponse.badRequest(
                        "Room already exists for this hotel with room number: " + roomNumber);
            }

            if (files == null || files.length == 0) {
                return ApiResponse.badRequest("At least one image file is required");
            }

            List<String> imageNames = new ArrayList<>();
            List<String> imageUrls = new ArrayList<>();

            List<String> imageBase64List = new ArrayList<>();

            for (MultipartFile file : files) {
                if (file.isEmpty())
                    continue;

                String base64 = ImageCompressionUtil
                        .processImageToBase64(file.getBytes());

                imageBase64List.add(base64);
            }

            if (imageBase64List.isEmpty()) {
                return ApiResponse.badRequest("No valid images uploaded");
            }

            ObjectMapper mapper = new ObjectMapper();
            String imagesJsonStr = mapper.writeValueAsString(imageBase64List);

            String roomId = roomRegisterRepository.saveRoomWithJson(
                    hotelId, roomNumber, roomType, featuresJson,
                    imagesJsonStr, price, maxAdults, maxChildren, bedCount);

            return ApiResponse.created(
                    Map.of(
                            "roomId", roomId,
                            "hotelId", hotelId,
                            "roomNumber", roomNumber),
                    "Room registered successfully");

        } catch (Exception e) {

            // 🔒 SAFETY: DB-level unique constraint catch
            if (e.getMessage().contains("unique_room_per_hotel")) {
                return ApiResponse.badRequest(
                        "Room already exists for this hotel with room number: " + roomNumber);
            }

            return ApiResponse.serverError("Error registering room: " + e.getMessage());
        }
    }

    /// ==============================///
    /// ====================================/// Updates
    /// ///======================================================
    /// ==============================///

    @PutMapping("/rooms/{roomId}/{hotelId}") // update room (NO IMAGES)
    public ResponseEntity<?> updateRoom(
            @PathVariable String roomId,
            @PathVariable String hotelId,
            @RequestParam(required = false) String room_NO,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) String features, // JSON string
            @RequestParam(required = false) Integer price,
            @RequestParam(required = false) Integer maxAdults,
            @RequestParam(required = false) Integer maxChildren,
            @RequestParam(required = false) Integer bedCount) {

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

            boolean updated = dashboardService.updateRoom(roomId, hotelId, updates);

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
    public ResponseEntity<?> getRoomImagesBase64(@PathVariable String roomId,
            @PathVariable String hotelId) {

        Map<Integer, String> images = dashboardService.getRoomImagesBase64WithId(roomId, hotelId);

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
            String roomId = (String) request.get("roomId");
            Integer imageIndex = (Integer) request.get("imageIndex"); // optional, 1-based
            String base64Image = (String) request.get("base64Image"); // optional alternative

            if (roomId == null || (imageIndex == null && base64Image == null)) {
                return ApiResponse.badRequest("roomId and either imageIndex or base64Image are required");
            }
            boolean removed = dashboardService.removeRoomImage(roomId, imageIndex, base64Image);

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

            boolean updated = dashboardService.appendRoomImages(roomId, base64Images);

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

    @PutMapping(value = "/hotels/{hotelId}/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateHotel(
            @PathVariable String hotelId,
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

            boolean updated = dashboardService.updateHotel(hotelId, updates);

            if (!updated) {
                return ApiResponse.notFound("Hotel not found or not owned by you");
            }

            return ApiResponse.success(updates, "Hotel updated successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.serverError("Failed to update hotel: " + e.getMessage());
        }
    }

    @GetMapping("/roomdetails/{roomID}")
    public ResponseEntity<Map<String, Object>> getRoomDetails(
            @PathVariable String roomID) {
        List<Map<String, Object>> data = dashboardService.getRoomDetails(roomID);
        return ApiResponse.success(data, "Room details fetched successfully");
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserListDTO>> getUsers() {
        return ResponseEntity.ok(userService.getUserList());
    }

    @PutMapping("/{requestId}")
    public ResponseEntity<?> processWithdraw(
            @PathVariable long requestId,
            @RequestBody Map<String, Object> body) {

        try {

            String decision = body.get("decision").toString();
            String transactionId = body.get("transactionId").toString();
            String remark = body.get("remark").toString();

            walletService.processWithdraw(requestId, decision, transactionId, remark);

            return ResponseEntity.ok(
                    Map.of(
                            "requestId", requestId,
                            "decision", decision.toUpperCase(),
                            "transactionId", transactionId,
                            "Remark", remark,
                            "message", "Withdraw request processed " + decision.toUpperCase()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    @GetMapping("/fetch_requests")
    public ResponseEntity<?> getWithdrawRequests(
            @RequestParam(required = false) String status) {

        try {

            List<Map<String, Object>> requests = walletService.getWithdrawRequests(status);

            if (requests == null || requests.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(Map.of("error", "No requests found"));
            }

            return ResponseEntity.ok(
                    Map.of(
                            "count", requests.size(),
                            "data", requests));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Something went wrong"));
        }
    }

    @GetMapping("/fetch_requests/{id}")
    public ResponseEntity<?> getWithdrawRequests(
            @RequestParam(required = false) String status,
            @PathVariable int id) {

        try {

            List<Map<String, Object>> requests = walletService.getWithdrawRequestsadmin(status, id);

            if (requests == null || requests.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(Map.of("error", "No requests found"));
            }

            return ResponseEntity.ok(
                    Map.of(
                            "count", requests.size(),
                            "data", requests));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Something went wrong"));
        }
    }

    @GetMapping("/details")
    public ResponseEntity<?> getAllBankDetails(@RequestParam String hotelId) {

        List<Map<String, Object>> bankDetails = walletService.getAllBankDetails(hotelId);

        if (bankDetails == null || bankDetails.isEmpty()) {

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "error");
            response.put("message", "No bank details found for hotel");
            response.put("hotel_id", hotelId);

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        return ResponseEntity.ok(bankDetails);
    }

    @GetMapping("/hotels/{hotelId}/ledger")
    public ResponseEntity<?> getLedger(
            @PathVariable String hotelId,
            @RequestParam(required = false) String type) {

        try {
            List<Map<String, Object>> ledger = ledgerservice.getLedgerByHotel(hotelId, type);

            if (ledger.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(Map.of("error", "No ledger records found"));
            }

            return ResponseEntity.ok(
                    Map.of(
                            "count", ledger.size(),
                            "data", ledger));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Something went wrong"));
        }
    }
}
