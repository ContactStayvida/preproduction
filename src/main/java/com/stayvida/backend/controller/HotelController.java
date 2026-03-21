package com.stayvida.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stayvida.backend.dto.Ad;
import com.stayvida.backend.dto.HotelDTO;
import com.stayvida.backend.dto.HotelSearchRequest;
import com.stayvida.backend.dto.HotelVerificationUpdate;
// import com.stayvida.backend.dto.RoomDTO;
// import com.stayvida.backend.dto.RegisterRoom;
// import com.stayvida.backend.dto.RoomDTO;
import com.stayvida.backend.model.Hotel;
import com.stayvida.backend.model.Register;
import com.stayvida.backend.repository.HotelRepository;
import com.stayvida.backend.repository.RegisterRepository;
// import com.stayvida.backend.repository.RoomImageRepository;
import com.stayvida.backend.repository.RoomRepository;
import com.stayvida.backend.repository.RoomregisterRepository;
import com.stayvida.backend.security.ApiResponse;
import com.stayvida.backend.service.CloudinaryService;
import com.stayvida.backend.service.ImageCompressionUtil;
// import com.stayvida.backend.service.RoomService;
import com.stayvida.backend.service.WalletService;
import com.stayvida.backend.service.AdminDashboardService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
// import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.stayvida.backend.model.Charges;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/hotels")
public class HotelController {

    @Value("${app.upload.dir}")
    private String uploadDir; // 📂 Folder for images

    @Value("${app.base.url}")
    private String baseUrl; // ✅ Base URL for image access

    @Autowired
    private HotelRepository hotelRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private AdminDashboardService adminDashboardService;
    // @Autowired
    // private RoomService roomService;

    // @Autowired
    // private RoomImageRepository roomImageRepository;
    @Autowired
    private RoomregisterRepository roomRegisterRepository; // 🔹 inject here
    @Autowired
    private WalletService walletService;

    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> searchHotels(
            @RequestBody(required = false) HotelSearchRequest request) {
        try {
            // 🧩 Check if body itself is missing
            if (request == null) {
                return ApiResponse.badRequest("Request body is missing");
            }

            // 🧩 Validate missing or empty fields safely
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("location", request.getLocation());
            fields.put("checkIn", request.getCheckIn());
            fields.put("checkOut", request.getCheckOut());
            fields.put("adults", request.getAdults());
            fields.put("children", request.getChildren());

            for (Map.Entry<String, Object> entry : fields.entrySet()) {
                Object value = entry.getValue();
                if (value == null ||
                        (value instanceof String && ((String) value).trim().isEmpty())) {
                    return ApiResponse.badRequest("Missing required field: " + entry.getKey());
                }
            }
            String location = request.getLocation();

            if (location != null) {
                location = location.trim().toLowerCase();
                if (location.length() > 3) {
                    location = location.substring(0, 3);
                }
            }

            // ✅ Fetch hotels from repository
            List<Hotel> hotels = hotelRepository.searchHotels(
                    location,
                    request.getCheckIn(),
                    request.getCheckOut(),
                    request.getAdults(),
                    request.getChildren());

            if (hotels == null || hotels.isEmpty()) {
                return ApiResponse.success(List.of(), "No hotels found for the given criteria");
            }
            List<Charges> charges = hotelRepository.getCharges();
            LocalDate checkInDate = LocalDate.parse(request.getCheckIn());
            LocalDate checkOutDate = LocalDate.parse(request.getCheckOut());
            long stayDuration = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
            List<Map<String, Object>> responseHotels = hotels.stream().map(hotel -> {
                Map<String, Object> map = new LinkedHashMap<>();
                double price = hotel.getPrice();
                map.put("id", hotel.getId());
                map.put("name", hotel.getName());
                map.put("type", hotel.getType());
                map.put("destination", hotel.getDestination());
                map.put("rating", hotel.getRating());
                // map.put("amenities", hotel.getAmenities());
                map.put("image",
                        hotel.getImage() != null
                                ? "data:image/jpeg;base64," + hotel.getImage()
                                : null);
                map.put("isForEvent", hotel.isForEvent());
                map.put("base price", price);
                map.put("stayDuration", stayDuration);
                map.put("Price for duration", price * stayDuration);
                // map.put("platformCharges", platformCharges);
                // map.put("taxPercent", taxPercent);
                return map;
            }).collect(Collectors.toList());

            // ✅ Return hotels with prefixed image URLs
            return ApiResponse.success(responseHotels, "Hotels fetched successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.serverError("An unexpected error occurred: " + e.getMessage());
        }
    }

    @GetMapping("/ad")
    public Ad getCurrentAd() {
        return adminDashboardService.getCurrentAd();
    }

    @PostMapping("/{adId}/click")
    public String registerClick(@PathVariable String adId) {

        adminDashboardService.incrementClick(adId);

        return "Click registered";
    }

    @GetMapping("/featurelist")
    public ResponseEntity<?> featureList() {
        try {
            List<Hotel> hotels = hotelRepository.getTop3HotelsByRating();

            // 🟡 No hotels found
            if (hotels.isEmpty()) {
                Map<String, Object> msg = new LinkedHashMap<>();
                msg.put("message", "No hotels available");
                return ApiResponse.success(msg, "No featured hotels found");
            }
            if (hotels == null || hotels.isEmpty()) {
                return ApiResponse.success(List.of(), "No hotels found for the given criteria");
            }
            List<Charges> charges = hotelRepository.getCharges();

            List<Map<String, Object>> result = hotels.stream().map(hotel -> {
                Map<String, Object> map = new LinkedHashMap<>();
                double price = hotel.getPrice();
                map.put("id", hotel.getId());
                map.put("name", hotel.getName());
                map.put("type", hotel.getType());
                map.put("destination", hotel.getDestination());
                map.put("rating", hotel.getRating());
                map.put("amenities", hotel.getAmenities());
                map.put("image",
                        hotel.getImage() != null
                                ? "data:image/jpeg;base64," + hotel.getImage()
                                : null);
                map.put("isForEvent", hotel.isForEvent());
                map.put("base price", price);
                return map;
            }).collect(Collectors.toList());

            // 🟢 Return 200 OK with list
            return ApiResponse.success(result, "Top featured hotels fetched successfully");

        } catch (IllegalArgumentException e) {
            // 🔴 Input or invalid arguments
            return ApiResponse.badRequest("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            // 🔴 Any server error (DB, runtime, etc.)
            return ApiResponse.serverError("Failed to fetch featured hotels: " + e.getMessage());
        }
    }

    @GetMapping("/{hotelId}/rooms") // open hotel to fetch rooms
    public ResponseEntity<Map<String, Object>> getHotelWithAvailableRooms(
            @PathVariable String hotelId,
            @RequestParam(required = false) String checkIn,
            @RequestParam(required = false) String checkOut) {
        if (checkIn == null || checkIn.isEmpty()) {
            return ApiResponse.badRequest("Missing required parameter: checkIn");
        }
        if (checkOut == null || checkOut.isEmpty()) {
            return ApiResponse.badRequest("Missing required parameter: checkOut");
        }

        try {

            HotelDTO result = roomRepository.getRoomsByHotelId(hotelId, checkIn, checkOut);
            if (result == null) {
                return ApiResponse.badRequest("Hotel not found");
            }

            // 🟩 Add prefix to hotel images
            if (result.getImages() != null && !result.getImages().isEmpty()) {
                result.setImages(
                        result.getImages().stream()
                                .filter(img -> img != null && !img.isEmpty())
                                .map(img -> "data:image/jpeg;base64," + img)
                                .toList());

            }

            // 🟩 Add prefix to each room image
            if (result.getRooms() != null && !result.getRooms().isEmpty()) {
                result.getRooms().forEach(room -> {
                    if (room.getRoomImages() != null && !room.getRoomImages().isEmpty()) {
                        room.setRoomImages(
                                room.getRoomImages().stream()
                                        .filter(img -> img != null && !img.isEmpty())
                                        .map(img -> "data:image/jpeg;base64," + img)
                                        .toList());
                    }
                });
            }

            // roomService.groupRoomsByType(result);

            return ApiResponse.success(result, "Hotel rooms fetched successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.serverError("Unable to fetch rooms: " + e.getMessage());
        }
    }

    @Autowired
    private RegisterRepository registerRepository;

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> registerHotel(
            @RequestPart("data") String hotelJson,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            Register register = mapper.readValue(hotelJson, Register.class);

            int ownerId = (int) SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal();

            register.setOwner_ID(ownerId);
            register.setHotel_ID("H-" + ownerId);
            Runtime runtime = Runtime.getRuntime();
            System.out.println("Max Memory: " + runtime.maxMemory() / (1024 * 1024) + " MB");
            System.out.println("Total Memory: " + runtime.totalMemory() / (1024 * 1024) + " MB");
            System.out.println("Free Memory: " + runtime.freeMemory() / (1024 * 1024) + " MB");

            String imageBase64 = null;

            if (imageFile != null && !imageFile.isEmpty()) {
                imageBase64 = ImageCompressionUtil
                        .processImageToBase64(imageFile.getBytes());

                // ✅ store BASE64 directly
                register.setImages(imageBase64);
            }

            String existingHotelId = registerRepository.findHotelIdByOwnerId(register.getOwner_ID());

            String hotelId = registerRepository.saveHotel(register);
            walletService.createAccount(hotelId);

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

    @PostMapping("/register_room_with_images")
    public ResponseEntity<Map<String, Object>> registerRoomWithImages(

            // @RequestParam("hotelId") String hotelId,
            @RequestParam("roomType") String roomType,
            @RequestParam("roomNumber") String roomNumber,
            @RequestParam("features") String featuresJson,
            @RequestParam("maxAdults") int maxAdults,
            @RequestParam("maxChildren") int maxChildren,
            @RequestParam("bedCount") int bedCount,
            @RequestParam("price") int price,
            @RequestParam("images") MultipartFile[] files) {
        try {

            int ownerId = (int) SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal();
            // 🔴 DUPLICATE CHECK (before upload)

            String hotelId = roomRegisterRepository.getHotelID(ownerId);

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

    // @PutMapping("/update-verification")
    // public ResponseEntity<?> updateVerificationStatus(@RequestBody
    // HotelVerificationUpdate request) {
    // try {
    // if (request.getHotelId() == null || request.getStatus() == null ||
    // request.getStatus().isEmpty()) {
    // return ApiResponse.badRequest("Invalid input: hotelId and status are
    // required");
    // }

    // int rows = hotelRepository.updateVerificationStatus(
    // request.getHotelId(),
    // request.getStatus(),
    // request.getRemark());

    // if (rows > 0) {
    // return ApiResponse.success(Map.of(
    // "hotelId", request.getHotelId(),
    // "status", request.getStatus(),
    // "remark", request.getRemark()), "Hotel verification status updated
    // successfully!");
    // } else {
    // return ApiResponse.badRequest("Hotel not found"); // or make a notFound() if
    // you like
    // }

    // } catch (IllegalArgumentException e) {
    // return ApiResponse.badRequest("Invalid data format: " + e.getMessage());
    // } catch (SecurityException e) {
    // return ApiResponse.unauthorized("Unauthorized to update verification
    // status");
    // } catch (Exception e) {
    // return ApiResponse.serverError("Error updating verification status: " +
    // e.getMessage());
    // }
    // }

}