package com.stayvida.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
// import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    private String uploadDir;  // 📂 Folder for images

    @Value("${app.base.url}")
    private String baseUrl;    // ✅ Base URL for image access

    @Autowired
    private HotelRepository hotelRepository;
    @Autowired
    private RoomRepository roomRepository;
    // @Autowired
    // private RoomImageRepository roomImageRepository;
   @Autowired
    private RoomregisterRepository roomRegisterRepository; // 🔹 inject here


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

        // ✅ Fetch hotels from repository
        List<Hotel> hotels = hotelRepository.searchHotels(
                request.getLocation(),
                request.getCheckIn(),
                request.getCheckOut(),
                request.getAdults(),
                request.getChildren()
        );

        if (hotels == null || hotels.isEmpty()) {
            return ApiResponse.success(List.of(), "No hotels found for the given criteria");
        }

        // 🧩 Add prefix (baseUrl) to image URLs
        List<Map<String, Object>> responseHotels = hotels.stream().map(hotel -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", hotel.getId());
            map.put("name", hotel.getName());
            map.put("type", hotel.getType());
            map.put("destination", hotel.getDestination());
            map.put("rating", hotel.getRating());
            // map.put("amenities", hotel.getAmenities());
            map.put("imageUrl", hotel.getImage() != null ? baseUrl + encodeURL(hotel.getImage()) : null);
            map.put("isForEvent", hotel.isForEvent());
            map.put("price", hotel.getPrice());
            return map;
        }).collect(Collectors.toList());

        // ✅ Return hotels with prefixed image URLs
        return ApiResponse.success(responseHotels, "Hotels fetched successfully");


    } catch (Exception e) {
        e.printStackTrace();
        return ApiResponse.serverError("An unexpected error occurred: " + e.getMessage());
    }
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

        // 🟢 Map hotel data
        List<Map<String, Object>> result = hotels.stream().map(hotel -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", hotel.getId());
            map.put("name", hotel.getName());
            map.put("type", hotel.getType());
            map.put("destination", hotel.getDestination());
            map.put("rating", hotel.getRating());
            map.put("amenities", hotel.getAmenities());
            map.put("imageUrl", hotel.getImage() != null ? baseUrl + encodeURL(hotel.getImage()) : null);
            map.put("isForEvent", hotel.isForEvent());
            map.put("price", hotel.getPrice());
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
@GetMapping("/{hotelId}/rooms")
public ResponseEntity<Map<String, Object>> getHotelWithAvailableRooms(
        @PathVariable int hotelId,
        @RequestParam(required = false) String checkIn,
        @RequestParam(required = false) String checkOut
) {
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
                        .map(img -> img.startsWith("http") ? img : baseUrl + encodePath(img))
                        .toList()
            );
        }

        // 🟩 Add prefix to each room image
        if (result.getRooms() != null && !result.getRooms().isEmpty()) {
            result.getRooms().forEach(room -> {
                if (room.getRoomImages() != null && !room.getRoomImages().isEmpty()) {
                    room.setRoomImages(
                        room.getRoomImages().stream()
                                .filter(img -> img != null && !img.isEmpty())
                                .map(img -> img.startsWith("http") ? img : baseUrl + encodePath(img))
                                .toList()
                    );
                }
            });
        }

        return ApiResponse.success(result, "Hotel rooms fetched successfully");

    } catch (Exception e) {
        e.printStackTrace();
        return ApiResponse.serverError("Unable to fetch rooms: " + e.getMessage());
    }
}






@Autowired
private RegisterRepository registerRepository;

// ✅ Create new hotel
// ✅ Register hotel with image upload
@PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<Map<String, Object>> registerHotel(
        @RequestPart("data") String hotelJson,
        @RequestPart(value = "image", required = false) MultipartFile imageFile) {

    try {
        ObjectMapper mapper = new ObjectMapper();
        Register register = mapper.readValue(hotelJson, Register.class);

        String imageFileName = null;

        if (imageFile != null && !imageFile.isEmpty()) {
            imageFileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
            Path filePath = Paths.get(uploadDir, imageFileName);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, imageFile.getBytes());
            register.setImages(imageFileName);
        }

        int newHotelId = registerRepository.saveHotel(register);

        return ApiResponse.created(Map.of("hotelId", newHotelId), "Hotel registered successfully");

    } catch (IOException e) {
        return ApiResponse.badRequest("Invalid JSON or image file: " + e.getMessage());
    } catch (SecurityException e) {
        return ApiResponse.unauthorized("Unauthorized access");
    } catch (Exception e) {
        return ApiResponse.serverError("Error registering hotel: " + e.getMessage());
    }
}


@PostMapping("/register_room_with_images")
public ResponseEntity<Map<String, Object>> registerRoomWithImages(
        @RequestParam("hotelId") int hotelId,
        @RequestParam("roomType") String roomType,
        @RequestParam("features") String featuresJson, // JSON or comma-separated string
        @RequestParam("maxAdults") int maxAdults,
        @RequestParam("maxChildren") int maxChildren,
        @RequestParam("bedCount") int bedCount,
        @RequestParam("price") int price,
        @RequestParam("images") MultipartFile[] files
) {
    try {
        // 1️⃣ Validate input
        if (files == null || files.length == 0) {
            return ApiResponse.badRequest("At least one image file is required");
        }

        // 2️⃣ Save images to server
        List<String> imageFilenames = new ArrayList<>();
        // String uploadDir = "C:/uploaded_images";
        Files.createDirectories(Paths.get(uploadDir));

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path path = Paths.get(uploadDir, filename);
            Files.write(path, file.getBytes());
            imageFilenames.add(filename);
        }

        if (imageFilenames.isEmpty()) {
            return ApiResponse.badRequest("No valid images uploaded");
        }

        // 3️⃣ Convert image list and features to JSON
        ObjectMapper mapper = new ObjectMapper();
        String imagesJsonStr = mapper.writeValueAsString(imageFilenames);
        String featuresJsonStr = featuresJson;

        // 4️⃣ Save room details in DB
        String roomId = roomRegisterRepository.saveRoomWithJson(
                hotelId, roomType, featuresJsonStr, imagesJsonStr,
                price, maxAdults, maxChildren, bedCount
        );

        // ✅ Return success directly
        return ApiResponse.created(
                Map.of(
                        "roomId", roomId,
                        "hotelId", hotelId,
                        "uploadedImages", imageFilenames
                ),
                "Room registered successfully"
        );

    } catch (IOException e) {
        return ApiResponse.badRequest("Invalid image file: " + e.getMessage());
    } catch (SecurityException e) {
        return ApiResponse.unauthorized("Unauthorized access");
    } catch (Exception e) {
        return ApiResponse.serverError("Error registering room: " + e.getMessage());
    }
}



     @PutMapping("/update-verification")
public ResponseEntity<?> updateVerificationStatus(@RequestBody HotelVerificationUpdate request) {
    try {
        if (request.getHotelId() <= 0 || request.getStatus() == null || request.getStatus().isEmpty()) {
            return ApiResponse.badRequest("Invalid input: hotelId and status are required");
        }

        int rows = hotelRepository.updateVerificationStatus(
                request.getHotelId(),
                request.getStatus(),
                request.getRemark()
        );

        if (rows > 0) {
            return ApiResponse.success(Map.of(
                    "hotelId", request.getHotelId(),
                    "status", request.getStatus(),
                    "remark", request.getRemark()
            ), "Hotel verification status updated successfully!");
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


private String encodeURL(String urlPath) {
    try {
        return URLEncoder.encode(urlPath, StandardCharsets.UTF_8.toString())
                .replace("+", "%20"); // fix space encoding
    } catch (Exception e) {
        return urlPath;
    }
}   

private String encodePath(String path) {
    try {
        return URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
                .replace("+", "%20");
    } catch (Exception e) {
        return path;
    }
}




}