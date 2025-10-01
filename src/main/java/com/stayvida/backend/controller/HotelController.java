package com.stayvida.backend.controller;

import com.stayvida.backend.dto.HotelSearchRequest;
import com.stayvida.backend.dto.HotelVerificationUpdate;
import com.stayvida.backend.dto.RoomDTO;
import com.stayvida.backend.model.Hotel;
import com.stayvida.backend.model.Register;
import com.stayvida.backend.repository.HotelRepository;
import com.stayvida.backend.repository.RegisterRepository;
import com.stayvida.backend.repository.RoomRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

String baseUrl = "https://sv-website-backend-1.onrender.com";  // ✅ Render backend URL

    @PostMapping("/search")
    public List<Map<String, Object>> searchHotels(@RequestBody HotelSearchRequest request) {
        List<Hotel> hotels = hotelRepository.searchHotels(  // ✅ Declare `hotels` here
                request.getLocation(),
                request.getCheckIn(),
                request.getCheckOut(),
                request.getAdults(),
                request.getChildren()
        );

        // Convert to a JSON-safe list with base64 image string
    return hotels.stream().map(hotel -> {
        Map<String, Object> map = new LinkedHashMap<>(); // ✅ Keeps key order

        // ✅ Insert keys in the exact order you want in JSON
        if (hotel.getImagePath() != null) {
            map.put("imageUrl", "http://localhost:8080/image/" + hotel.getImagePath());
        } else {
            map.put("imageUrl", null);
        }

        map.put("id", hotel.getId());
        map.put("location", hotel.getLocation());
        map.put("hotel", hotel.getHotel());
        map.put("max_adults", hotel.getAdults());
        map.put("max_children", hotel.getchildren());
        // map.put("price", hotel.getPrice());
        map.put("availability", hotel.isAvailability());
        map.put("rating", hotel.getRating());
        map.put("amenities", hotel.getAmenities());
        map.put("description", hotel.getdescription());

        return map;
    }).collect(Collectors.toList());
}

    
    @GetMapping("/{hotelId}/rooms")
    public List<RoomDTO> getRooms(@PathVariable int hotelId) {
        return roomRepository.getRoomsByHotelId(hotelId);
    }


@Autowired
    private RegisterRepository registerrepository;

    // Create new hotel (without image)
@PostMapping("/register")
public ResponseEntity<?> registerHotel(@RequestBody Register register) {
    try {
        int newHotelId = registerrepository.saveHotel(register);
        return ResponseEntity.ok("ID = " + newHotelId + "\nHotel registered successfully!");
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error registering hotel: " + e.getMessage());
    }
}


    // Upload image for an existing hotel
    @PostMapping("/upload-image")
    public ResponseEntity<String> uploadHotelImage(
            @RequestParam("hotelId") int hotelId,
            @RequestParam("image") MultipartFile file) {

        try {
            // 1️⃣ Get original filename
            String filename = file.getOriginalFilename();

            // 2️⃣ Ensure folder exists
            Path filePath = Paths.get(uploadDir, filename);
            Files.createDirectories(filePath.getParent());

            // 3️⃣ Save file
            Files.write(filePath, file.getBytes());

            // 4️⃣ Update DB
            registerrepository.updateHotelImage(hotelId, filename);

            return ResponseEntity.ok("Image uploaded successfully for hotel ID: " + hotelId);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error uploading image: " + e.getMessage());
        }
    }



     @PutMapping("/update-verification")
    public ResponseEntity<?> updateVerificationStatus(@RequestBody HotelVerificationUpdate request) {
        try {
            int rows = hotelRepository.updateVerificationStatus(request.getHotelId(), request.getStatus());
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