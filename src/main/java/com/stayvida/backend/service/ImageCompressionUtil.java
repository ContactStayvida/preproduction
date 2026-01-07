package com.stayvida.backend.service;

import net.coobird.thumbnailator.Thumbnails;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class ImageCompressionUtil {

    private static final long MIN_COMPRESS_SIZE = 500 * 1024; // 500 KB
    private static final long TARGET_MIN_SIZE = 200 * 1024; // 200 KB

    public static String processImageToBase64(byte[] originalBytes)
            throws Exception {

        // 🔹 If image < 500 KB → do NOT compress
        if (originalBytes.length < MIN_COMPRESS_SIZE) {
            return Base64.getEncoder().encodeToString(originalBytes);
        }

        float quality = 0.9f;
        byte[] compressed = originalBytes;

        while (compressed.length > MIN_COMPRESS_SIZE && quality > 0.4f) {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            Thumbnails.of(new ByteArrayInputStream(originalBytes))
                    .size(1024, 1024)
                    .outputQuality(quality)
                    .outputFormat("jpg")
                    .toOutputStream(baos);

            compressed = baos.toByteArray();
            quality -= 0.05f;
        }

        // 🔹 Safety: never go below 200 KB
        if (compressed.length < TARGET_MIN_SIZE) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(originalBytes))
                    .size(1024, 1024)
                    .outputQuality(0.6)
                    .outputFormat("jpg")
                    .toOutputStream(baos);

            compressed = baos.toByteArray();
        }

        return Base64.getEncoder().encodeToString(compressed);
    }
}
