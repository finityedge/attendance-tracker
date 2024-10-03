package com.finityedge.attendancetracker.controller;

import com.finityedge.attendancetracker.model.AttendanceLog;
import com.finityedge.attendancetracker.model.User;
import com.finityedge.attendancetracker.service.AttendanceService;
import com.finityedge.attendancetracker.service.QRCodeService;
import com.finityedge.attendancetracker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private UserService userService;

    @Autowired
    private QRCodeService qrCodeService;

    @GetMapping("/scan")
    public String scanQrCodePage(Model model) {
        String qrCodeContent = qrCodeService.generateQRCodeContent("OFFICE_ENTRANCE");
        try {
            String qrCodeImage = qrCodeService.generateQRCodeImage(qrCodeContent, 250, 250);
            model.addAttribute("qrCodeImage", qrCodeImage);
        } catch (Exception e) {
            // Handle exception
        }
        return "scan-qr";
    }

    @PostMapping("/check-in-out")
    @ResponseBody
    public ResponseEntity<?> checkInOut(@AuthenticationPrincipal UserDetails userDetails, @RequestBody String qrCodeContent) {
        User user = (User) userService.loadUserByUsername(userDetails.getUsername());
        try {
            AttendanceLog log = attendanceService.checkInOut(user, qrCodeContent);
            return ResponseEntity.ok(log);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/logs")
    public String getAttendanceLogs(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = (User) userService.getUserByUsername(userDetails.getUsername());
        List<AttendanceLog> logs = attendanceService.getUserAttendanceLogs(user);
        model.addAttribute("logs", logs);
        return "attendance-logs";
    }

    @GetMapping("/admin/logs")
    public String getAdminAttendanceLogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {
        List<AttendanceLog> logs = attendanceService.getAttendanceLogsBetweenDates(startDate, endDate);
        model.addAttribute("logs", logs);
        return "admin-attendance-logs";
    }
}
