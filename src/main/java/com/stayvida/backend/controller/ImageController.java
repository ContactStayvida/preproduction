package com.stayvida.backend.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/image")
public class ImageController {

    private static final String IMAGE_FOLDER = "C:/uploaded_images/";

    @GetMapping("/{filename:.+}")
    public ResponseEntity<?> serveImage(@PathVariable String filename) {
        Path imagePath = Paths.get(IMAGE_FOLDER, filename);
        File file = imagePath.toFile();

        if (file.exists() && file.isFile() && file.canRead()) {
            Resource resource = new FileSystemResource(file);
            // Detect content type dynamically if needed
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG) // adjust for JPG if needed
                    .body(resource);
        } else {
            // Return plain text for missing file
            return ResponseEntity.status(404)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Image not found");
        }
    }
}
