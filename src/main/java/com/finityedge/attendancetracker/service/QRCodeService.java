package com.finityedge.attendancetracker.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;

@Service
public class QRCodeService {

    @Value("${qrcode.secret}")
    private String qrCodeSecret;

    public String generateQRCodeContent(String locationId) {
        long timestamp = Instant.now().getEpochSecond();
        return String.format("ATND:%s:%d:%s", locationId, timestamp, qrCodeSecret);
    }

    public String generateQRCodeImage(String content, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height);

        BufferedImage image = toBufferedImage(bitMatrix);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();

        return Base64.getEncoder().encodeToString(imageBytes);
    }

    private BufferedImage toBufferedImage(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        return image;
    }

    public boolean validateQRCode(String qrCodeContent) {
        String[] parts = qrCodeContent.split(":");
        if (parts.length != 4 || !parts[0].equals("ATND")) {
            return false;
        }

        long timestamp = Long.parseLong(parts[2]);
        long currentTime = Instant.now().getEpochSecond();
        long timeDifference = currentTime - timestamp;

        // Check if the QR code is not older than 5 minutes
//        if (timeDifference > 300) {
//            return false;
//        }

        return parts[3].equals(qrCodeSecret);
    }

}
