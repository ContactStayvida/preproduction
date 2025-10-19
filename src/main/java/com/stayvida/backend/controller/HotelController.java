package com.stayvida.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stayvida.backend.dto.HotelDTO;
import com.stayvida.backend.dto.HotelSearchRequest;
import com.stayvida.backend.dto.HotelVerificationUpdate;
// import com.stayvida.backend.dto.RegisterRoom;
// import com.stayvida.backend.dto.RoomDTO;
import com.stayvida.backend.model.Hotel;
import com.stayvida.backend.model.Register;
import com.stayvida.backend.repository.HotelRepository;
import com.stayvida.backend.repository.RegisterRepository;
// import com.stayvida.backend.repository.RoomImageRepository;
import com.stayvida.backend.repository.RoomRepository;
import com.stayvida.backend.repository.RoomregisterRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

String baseUrl = "http://localhost:8080/image/";  // ✅ Render backend URL

    @PostMapping("/search")
public List<Map<String, Object>> searchHotels(@RequestBody HotelSearchRequest request) {
    List<Hotel> hotels = hotelRepository.searchHotels(
            request.getLocation(),
            request.getCheckIn(),
            request.getCheckOut(),
            request.getAdults(),
            request.getChildren()
    );

    if (hotels.isEmpty()) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("message", "No hotels available");
        return List.of(msg);
    }

    return hotels.stream().map(hotel -> {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", hotel.getId());
        map.put("name", hotel.getName());
        map.put("type", hotel.getType());
        map.put("destination", hotel.getDestination());
        map.put("rating", hotel.getRating());
        // map.put("description", hotel.getDescription());
        // map.put("phoneNo", hotel.getPhoneNo());
        // map.put("tags", hotel.getTags());
        map.put("amenities", hotel.getAmenities());
        map.put("imageUrl", hotel.getImage() != null ? baseUrl + hotel.getImage() : null);
        // map.put("longitude", hotel.getLongitude());
        // map.put("latitude", hotel.getLatitude());
        map.put("isForEvent", hotel.isForEvent());
        map.put("price", hotel.getPrice());
        // map.put("onArrivalPayment", hotel.isOnArrivalPayment());
        // map.put("status", hotel.getStatus());
        // map.put("remark", hotel.getRemark());
        return map;
    }).collect(Collectors.toList());
}


    
    @GetMapping("/{hotelId}/rooms")
public HotelDTO getHotelWithRooms(@PathVariable int hotelId) {
    return roomRepository.getRoomsByHotelId(hotelId);
}


@Autowired
    private RegisterRepository registerRepository;;



    // ✅ Create new hotel
    // ✅ Register hotel with image upload
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerHotel(
            @RequestPart("data") String hotelJson,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {

        try {
            // 🧩 Convert JSON to Register object
            ObjectMapper mapper = new ObjectMapper();
            Register register = mapper.readValue(hotelJson, Register.class);

            String imageFileName = null;

            // 🖼️ If image is provided, save it
            if (imageFile != null && !imageFile.isEmpty()) {
                imageFileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
                Path filePath = Paths.get(uploadDir, imageFileName);
                Files.createDirectories(filePath.getParent());
                Files.write(filePath, imageFile.getBytes());
                register.setImages(imageFileName); // store filename in DB
            }

            // 💾 Save hotel details (and image filename)
            int newHotelId = registerRepository.saveHotel(register);

            return ResponseEntity.ok("Hotel registered successfully with ID: " + newHotelId);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid JSON or file: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error registering hotel: " + e.getMessage());
        }
    }
    // ✅ Register room with multiple images


@PostMapping("/register_room_with_images")
public ResponseEntity<?> registerRoomWithImages(
        @RequestParam("hotelId") int hotelId,
        @RequestParam("roomType") String roomType,
        @RequestParam("features") String featuresJson, // comma-separated or JSON string
        @RequestParam("maxAdults") int maxAdults,
        @RequestParam("maxChildren") int maxChildren,
        @RequestParam("bedCount") int bedCount,
        @RequestParam("price") int price,
        @RequestParam("images") MultipartFile[] files
) {
    try {
        // 1️⃣ Save images to server and collect filenames
        List<String> imageFilenames = new ArrayList<>();
        String uploadDir = "C:/uploaded_images";
        Files.createDirectories(Paths.get(uploadDir));

        for (MultipartFile file : files) {
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path path = Paths.get(uploadDir, filename);
            Files.write(path, file.getBytes());
            imageFilenames.add(filename);
        }

        // 2️⃣ Convert features and filenames to JSON
        ObjectMapper mapper = new ObjectMapper();
        String featuresJsonStr = featuresJson; // if already JSON string
        String imagesJsonStr = mapper.writeValueAsString(imageFilenames);

        // 3️⃣ Save to DB
        String roomId = roomRegisterRepository.saveRoomWithJson(
                hotelId, roomType, featuresJsonStr, imagesJsonStr,
                price, maxAdults, maxChildren, bedCount
        );

        return ResponseEntity.ok("Room ID = " + roomId + "\nRoom registered successfully!");
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error registering room: " + e.getMessage());
    }
}



     @PutMapping("/update-verification")
public ResponseEntity<?> updateVerificationStatus(@RequestBody HotelVerificationUpdate request) {
    try {
        int rows = hotelRepository.updateVerificationStatus(
                request.getHotelId(),
                request.getStatus(),
                request.getRemark() // pass remark
        );
        if (rows > 0) {
            return ResponseEntity.ok("Hotel verification status updated successfully!");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Hotel not found");
        }
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error updating verification status: " + e.getMessage());
    }
}



}