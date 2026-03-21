package com.stayvida.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public Map uploadImage(MultipartFile file, String fileNameWithoutExt) throws Exception {

        // Extract extension from original file
        String original = file.getOriginalFilename();
        String ext = original.substring(original.lastIndexOf(".") + 1);

        // Remove extension if accidentally included
        fileNameWithoutExt = fileNameWithoutExt.replaceFirst("[.][^.]+$", "");

        return cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "stayvida", // SAVE INSIDE FOLDER
                        "public_id", fileNameWithoutExt, // FILE NAME ONLY
                        "resource_type", "image",
                        "overwrite", true));
    }
}
