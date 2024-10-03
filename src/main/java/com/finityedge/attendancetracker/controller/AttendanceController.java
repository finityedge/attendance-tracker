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

//    @GetMapping("/scan")
//    public String scanQrCodePage(Model model, Authentication authentication) {
//        boolean isAdmin = authentication.getAuthorities().stream()
//                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
//        model.addAttribute("isAdmin", isAdmin);
//        return "scan-qr";
//    }

    @PostMapping("/check-in-out")
    @ResponseBody
    public ResponseEntity<?> checkInOut(@AuthenticationPrincipal UserDetails userDetails, @RequestBody Map<String, String> payload) {
        User user = (User) userService.getUserByUsername(userDetails.getUsername());
        String qrCode = payload.get("qrCode");
        System.out.println("QR Code: " + qrCode);
        try {
            AttendanceLog log = attendanceService.checkInOut(user, qrCode);
            return ResponseEntity.ok(Map.of("message", "Check-in/out successful", "log", log));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/scan")
    public String scanQrCodePage(Model model, Authentication authentication) {
        boolean isAdmin = authentication != null &&
                authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        model.addAttribute("isAdmin", isAdmin);
        assert authentication != null;
        model.addAttribute("username", authentication.getName()); // TODO: Change to actual admin username (if needed)
        return "scan-qr";
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
            Model model,
            Authentication authentication) {
        boolean isAdmin = authentication != null &&
                authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            return "redirect:/attendance/logs";
        }

        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1); // First day of current month
        }
        if (endDate == null) {
            endDate = LocalDate.now(); // Current date
        }

        List<AttendanceLog> logs = attendanceService.getAttendanceLogsBetweenDates(startDate, endDate);
        model.addAttribute("isAdmin", true);
        model.addAttribute("logs", logs);
        model.addAttribute("username", authentication.getName()); // TODO: Change to actual admin username (if needed
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        return "admin-attendance-logs";
    }
}
