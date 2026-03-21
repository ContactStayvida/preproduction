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

    private static final long TARGET_SIZE = 1024 * 1024; // 1 MB
    private static final int MAX_WIDTH = 1920; // good for web display

    public static String processImageToBase64(byte[] originalBytes) throws Exception {

        String format = getImageFormat(originalBytes);

        byte[] resizedBytes = resizeIfNeeded(originalBytes, format);

        if (resizedBytes.length <= TARGET_SIZE) {
            return Base64.getEncoder().encodeToString(resizedBytes);
        }

        byte[] compressed = compressToTarget(resizedBytes, format);

        return Base64.getEncoder().encodeToString(compressed);
    }

    private static byte[] resizeIfNeeded(byte[] imageBytes, String format) throws Exception {

        ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
        var bufferedImage = ImageIO.read(bais);

        int width = bufferedImage.getWidth();

        // If image is already small enough, skip resizing
        if (width <= MAX_WIDTH) {
            return imageBytes;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Thumbnails.of(bufferedImage)
                .width(MAX_WIDTH)
                .keepAspectRatio(true)
                .outputFormat(format)
                .toOutputStream(baos);

        return baos.toByteArray();
    }

    private static byte[] compressToTarget(byte[] imageBytes, String format) throws Exception {

        float minQuality = 0.5f;
        float maxQuality = 1.0f;

        byte[] bestResult = imageBytes;

        for (int i = 0; i < 7; i++) { // ~7 iterations gets very close

            float midQuality = (minQuality + maxQuality) / 2;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            Thumbnails.of(new ByteArrayInputStream(imageBytes))
                    .scale(1)
                    .outputQuality(midQuality)
                    .outputFormat(format)
                    .toOutputStream(baos);

            byte[] result = baos.toByteArray();

            if (result.length > TARGET_SIZE) {
                maxQuality = midQuality;
            } else {
                bestResult = result;
                minQuality = midQuality;
            }
        }

        return bestResult;
    }

    private static String getImageFormat(byte[] imageBytes) throws Exception {

        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes))) {

            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);

            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                return reader.getFormatName().toLowerCase();
            }
        }

        return "jpg";
    }
}