package com.stayvida.backend.controller;

import com.stayvida.backend.service.ImageCompressionUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/test")
public class ImageUploadController {

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("image") MultipartFile image) {
        try {
            byte[] originalBytes = image.getBytes();

            String base64 = ImageCompressionUtil
                    .processImageToBase64(originalBytes);

            return ResponseEntity.ok(
                    Map.of(
                            "base64", base64));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
