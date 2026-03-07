package com.stayvida.backend.service;

import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Iterator;

public class ImageCompressionUtil {

    private static final long COMPRESS_THRESHOLD = 1024 * 1024; // 1 MB

    public static String processImageToBase64(byte[] originalBytes) throws Exception {

        // If image ≤ 1MB → return as-is
        if (originalBytes.length <= COMPRESS_THRESHOLD) {
            return Base64.getEncoder().encodeToString(originalBytes);
        }

        String format = getImageFormat(originalBytes);

        float quality = 0.9f;
        byte[] compressed = originalBytes;

        while (compressed.length > COMPRESS_THRESHOLD && quality > 0.5f) {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            Thumbnails.of(new ByteArrayInputStream(originalBytes))
                    .scale(1) // keep original resolution
                    .outputQuality(quality) // reduce quality gradually
                    .outputFormat(format) // keep same format
                    .toOutputStream(baos);

            compressed = baos.toByteArray();
            quality -= 0.05f;
        }

        return Base64.getEncoder().encodeToString(compressed);
    }

    private static String getImageFormat(byte[] imageBytes) throws Exception {

        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes))) {

            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);

            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                return reader.getFormatName().toLowerCase();
            }
        }

        return "jpg"; // fallback
    }
}