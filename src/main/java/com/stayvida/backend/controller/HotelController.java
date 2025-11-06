package com.stayvida.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stayvida.backend.dto.HotelDTO;
import com.stayvida.backend.dto.HotelSearchRequest;
import com.stayvida.backend.dto.HotelVerificationUpdate;
import com.stayvida.backend.dto.RoomDTO;
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
// import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/hotels")
public class HotelController {

    private final String uploadDir = "C:/uploaded_images";  // 📂 Folder for images
    @Autowired
    private HotelRepository hotelRepository;
    @Autowired
    private RoomRepository roomRepository;
    // @Autowired
    // private RoomImageRepository roomImageRepository;
   @Autowired
    private RoomregisterRepository roomRegisterRepository; // 🔹 inject here

// String baseUrl = "http://localhost:8080/image/";  // ✅ Render backend URL

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
            // 🖼 Convert image filenames to Base64
        for (Hotel hotel : hotels) {
            if (hotel.getImage() != null) {
                String base64Image = encodeImageToBase64(hotel.getImage());
                // Optional: include MIME type for frontend <img src="">
                if (base64Image != null) {
                    hotel.setImage("data:image/jpeg;base64," + base64Image);
                }
            }
        }

        // ✅ Return response with Base64 images
        return ApiResponse.success(hotels, "Hotels fetched successfully");


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
            map.put("imageBase64", hotel.getImage() != null ? encodeImageToBase64(hotel.getImage()) : null);
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
            return ApiResponse.notFound("Hotel not found with ID: " + hotelId);
        }

        // 🖼 Convert hotel images to Base64
        if (result.getImages() != null && !result.getImages().isEmpty()) {
            List<String> base64Images = result.getImages().stream()
                    .map(this::encodeImageToBase64)
                    .filter(Objects::nonNull)
                    .map(base64 -> "data:image/jpeg;base64," + base64)
                    .toList();
            result.setImages(base64Images);
        }

        // 🏠 Convert each room’s image(s) to Base64 (if you have room images)
        if (result.getRooms() != null && !result.getRooms().isEmpty()) {
            for (RoomDTO room : result.getRooms()) {
                if (room.getRoomImages() != null && !room.getRoomImages().isEmpty()) {
                    List<String> base64RoomImages = room.getRoomImages().stream()
                            .map(this::encodeImageToBase64)
                            .filter(Objects::nonNull)
                            .map(base64 -> "data:image/jpeg;base64," + base64)
                            .toList();
                    room.setRoomImages(base64RoomImages);
                }
            }
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

        if (imageFile != null && !imageFile.isEmpty()) {
            // Convert image to Base64
            String base64Image = Base64.getEncoder().encodeToString(imageFile.getBytes());
            register.setImages(base64Image);
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
        @RequestParam("features") String featuresJson,
        @RequestParam("maxAdults") int maxAdults,
        @RequestParam("maxChildren") int maxChildren,
        @RequestParam("bedCount") int bedCount,
        @RequestParam("price") int price,
        @RequestParam("images") MultipartFile[] files
) {
    try {
        if (files == null || files.length == 0) {
            return ApiResponse.badRequest("At least one image file is required");
        }

        // Convert each image to Base64
        List<String> base64Images = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                base64Images.add(Base64.getEncoder().encodeToString(file.getBytes()));
            }
        }

        if (base64Images.isEmpty()) {
            return ApiResponse.badRequest("No valid images uploaded");
        }

        ObjectMapper mapper = new ObjectMapper();
        String imagesJsonStr = mapper.writeValueAsString(base64Images);

        String roomId = roomRegisterRepository.saveRoomWithJson(
                hotelId, roomType, featuresJson, imagesJsonStr,
                price, maxAdults, maxChildren, bedCount
        );

        return ApiResponse.created(
                Map.of(
                        "roomId", roomId,
                        "hotelId", hotelId,
                        "imageCount", base64Images.size()
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
private String encodeImageToBase64(String imageBase64) {
    if (imageBase64 == null || imageBase64.isEmpty()) {
        return null;
    }

    // If it already looks like a base64 string, just return it
    if (imageBase64.startsWith("data:image")) {
        return imageBase64; // already formatted
    }

    // If it looks like raw base64 (not a file path or URL)
    if (!imageBase64.contains("/") && imageBase64.length() > 100) {
        return "data:image/jpeg;base64," + imageBase64;
    }

    // Otherwise, treat it as a URL and return as-is
    return imageBase64;
}





}