package com.finityedge.attendancetracker.controller;

import com.finityedge.attendancetracker.model.AttendanceLog;
import com.finityedge.attendancetracker.model.User;
import com.finityedge.attendancetracker.service.AttendanceService;
import com.finityedge.attendancetracker.service.QRCodeService;
import com.finityedge.attendancetracker.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        boolean isAdmin = authentication != null &&
                authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("username", authentication.getName());
        return "scan-qr";
    }

    @PostMapping("/check-in-out")
    @ResponseBody
    public ResponseEntity<?> checkInOut(Authentication authentication, @RequestBody Map<String, String> payload) {
        User user = userService.getUserByUsername(authentication.getName());
        String qrCode = payload.get("qrCode");
        try {
            AttendanceLog log = attendanceService.checkInOut(user, qrCode);
            return ResponseEntity.ok(Map.of(
                    "message", "Attendance marked successfully",
                    "action", log.getCheckOutTime() == null ? "Check-in" : "Check-out",
                    "timestamp", log.getCheckOutTime() == null ? log.getCheckInTime().toString() : log.getCheckOutTime().toString()
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/admin/generate-qr")
    @ResponseBody
    public ResponseEntity<?> generateQRCode(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            return ResponseEntity.status(403).body(Map.of("error", "Unauthorized"));
        }
        try {
            String qrCodeImage = qrCodeService.generateQRCode();
            return ResponseEntity.ok(Map.of("qrCodeImage", qrCodeImage));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to generate QR code"));
        }
    }

    @GetMapping("/logs")
    public String getAttendanceLogs(@AuthenticationPrincipal UserDetails userDetails,
                                    Model model,
                                    Authentication authentication) {
        User user = (User) userService.getUserByUsername(userDetails.getUsername());
        List<AttendanceLog> logs = attendanceService.getUserAttendanceLogs(user);

        boolean isAdmin = authentication != null &&
                authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("username", user.getUsername());
        model.addAttribute("logs", logs);
        return "attendance-logs";
    }

    @GetMapping("/admin/logs")
    public String getAdminAttendanceLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) UUID employeeId,
            Model model,
            Authentication authentication) {
        boolean isAdmin = authentication != null &&
                authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            return "redirect:/attendance/logs";
        }

        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        List<AttendanceLog> logs = attendanceService.getFilteredAttendanceLogs(startDate, endDate, employeeId);
        List<User> users = userService.getAllUsers();

        model.addAttribute("isAdmin", true);
        model.addAttribute("logs", logs);
        model.addAttribute("users", users);
        model.addAttribute("username", authentication.getName());
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("selectedEmployeeId", employeeId);
        return "admin-attendance-logs";
    }

    @GetMapping("/admin/export")
    public void exportToExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) UUID employeeId,
            HttpServletResponse response) throws IOException {
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        List<AttendanceLog> logs = attendanceService.getFilteredAttendanceLogs(startDate, endDate, employeeId);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=attendance_logs.xlsx");

        attendanceService.exportToExcel(logs, response.getOutputStream());
    }
}
