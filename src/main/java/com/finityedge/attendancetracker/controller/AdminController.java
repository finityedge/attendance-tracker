package com.finityedge.attendancetracker.controller;

import com.finityedge.attendancetracker.service.QRCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private QRCodeService qrCodeService;

    @PostMapping("/generate-qr")
    @ResponseBody
    public ResponseEntity<?> generateQRCode() {
        try {
            String qrCodeContent = qrCodeService.generateQRCodeContent("OFFICE_ENTRANCE");
            String qrCodeImage = qrCodeService.generateQRCodeImage(qrCodeContent, 250, 250);
            return ResponseEntity.ok(Map.of("qrCodeImage", qrCodeImage));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to generate QR code"));
        }
    }
}
