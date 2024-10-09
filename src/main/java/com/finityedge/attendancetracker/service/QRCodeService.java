package com.finityedge.attendancetracker.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
public class QRCodeService {
    private static final Logger logger = LoggerFactory.getLogger(QRCodeService.class);
    private static final String QR_CODE_PREFIX = "ATND:";
    private static final long QR_CODE_VALIDITY_MINUTES = 30;

    public String generateQRCode() throws WriterException, IOException {
        // Use Unix timestamp (milliseconds) for consistent time handling
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String content = QR_CODE_PREFIX + timestamp;

        logger.info("Generating QR code with content: {}", content);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 300, 300);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        byte[] pngData = pngOutputStream.toByteArray();
        return Base64.getEncoder().encodeToString(pngData);
    }

    public boolean validateQRCode(String qrCodeContent) {
        logger.info("Validating QR code content: {}", qrCodeContent);

        try {
            if (!qrCodeContent.startsWith(QR_CODE_PREFIX)) {
                logger.warn("Invalid QR code prefix. Expected: {}, Got: {}",
                        QR_CODE_PREFIX, qrCodeContent.substring(0, Math.min(qrCodeContent.length(), 5)));
                return false;
            }

            String timestampStr = qrCodeContent.substring(QR_CODE_PREFIX.length());
            long timestamp = Long.parseLong(timestampStr);
            long currentTime = Instant.now().toEpochMilli();
            long validityPeriod = QR_CODE_VALIDITY_MINUTES * 60 * 1000; // Convert minutes to milliseconds

            boolean isValid = (currentTime - timestamp) <= validityPeriod;
            logger.info("QR code validation: timestamp={}, currentTime={}, diff={}, validityPeriod={}, isValid={}",
                    timestamp, currentTime, currentTime - timestamp, validityPeriod, isValid);

            return isValid;
        } catch (Exception e) {
            logger.error("Error validating QR code: {}", e.getMessage());
            return false;
        }
    }
}

