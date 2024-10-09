package com.finityedge.attendancetracker.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
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
    private static final String QR_CODE_PREFIX = "ATND:";
    private static final long QR_CODE_VALIDITY_MINUTES = 30;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public String generateQRCode() throws WriterException, IOException {
        // Format: ATND:2024-03-20T14:30:00
        String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        String content = QR_CODE_PREFIX + timestamp;

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 200, 200);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        byte[] pngData = pngOutputStream.toByteArray();
        return Base64.getEncoder().encodeToString(pngData);
    }

    public boolean validateQRCode(String qrCodeContent) {
        try {
            if (!qrCodeContent.startsWith(QR_CODE_PREFIX)) {
                return false;
            }

            String timestamp = qrCodeContent.substring(QR_CODE_PREFIX.length());
            LocalDateTime generationTime = LocalDateTime.parse(timestamp, DATE_TIME_FORMATTER);
            LocalDateTime expiryTime = generationTime.plusMinutes(QR_CODE_VALIDITY_MINUTES);

            return LocalDateTime.now().isBefore(expiryTime);
        } catch (Exception e) {
            return false;
        }
    }
}
