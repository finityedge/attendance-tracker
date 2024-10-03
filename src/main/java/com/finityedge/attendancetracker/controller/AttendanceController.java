package com.finityedge.attendancetracker.controller;

import com.finityedge.attendancetracker.model.AttendanceLog;
import com.finityedge.attendancetracker.model.User;
import com.finityedge.attendancetracker.service.AttendanceService;
import com.finityedge.attendancetracker.service.QRCodeService;
import com.finityedge.attendancetracker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
    public String scanQrCodePage(Model model, Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        model.addAttribute("isAdmin", isAdmin);
        return "scan-qr";
    }

    @PostMapping("/check-in-out")
    @ResponseBody
    public ResponseEntity<?> checkInOut(@AuthenticationPrincipal UserDetails userDetails, @RequestBody Map<String, String> payload) {
        User user = (User) userService.getUserByUsername(userDetails.getUsername());
        String qrCode = payload.get("qrCode");
        try {
            AttendanceLog log = attendanceService.checkInOut(user, qrCode);
            return ResponseEntity.ok(Map.of("message", "Check-in/out successful", "log", log));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
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
