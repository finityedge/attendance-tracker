package com.finityedge.attendancetracker.service;

import com.finityedge.attendancetracker.model.AttendanceLog;
import com.finityedge.attendancetracker.model.User;
import com.finityedge.attendancetracker.repository.AttendanceLogRepository;
import com.finityedge.attendancetracker.repository.UserRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceLogRepository attendanceLogRepository;
    @Autowired
    private QRCodeService qrCodeService;
    @Autowired
    private UserRepository userRepository;

    public AttendanceLog checkInOut(User user, String qrCodeContent) {
        if (!qrCodeService.validateQRCode(qrCodeContent)) {
            throw new IllegalArgumentException("Invalid QR code");
        }

        LocalDate today = LocalDate.now();
        Optional<AttendanceLog> existingLog = attendanceLogRepository.findByUserAndDate(user, today);

        if (existingLog.isPresent()) {
            AttendanceLog log = existingLog.get();
            if (log.getCheckOutTime() == null) {
                log.setCheckOutTime(LocalDateTime.now());
            } else {
                throw new IllegalStateException("Already checked out for today");
            }
            return attendanceLogRepository.save(log);
        } else {
            AttendanceLog newLog = new AttendanceLog();
            newLog.setUser(user);
            newLog.setCheckInTime(LocalDateTime.now());
            newLog.setDate(today);
            return attendanceLogRepository.save(newLog);
        }
    }

    public List<AttendanceLog> getUserAttendanceLogs(User user) {
        return attendanceLogRepository.findByUserOrderByDateDesc(user);
    }

    public List<AttendanceLog> getAttendanceLogsBetweenDates(LocalDate startDate, LocalDate endDate) {
        return attendanceLogRepository.findByDateBetweenOrderByDateAsc(startDate, endDate);
    }

    public List<AttendanceLog> getFilteredAttendanceLogs(LocalDate startDate, LocalDate endDate, UUID employeeId) {
        if (employeeId != null) {
            User employee = userRepository.findById(employeeId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid employee ID"));
            return attendanceLogRepository.findByUserAndDateBetweenOrderByDateAsc(employee, startDate, endDate);
        }
        return attendanceLogRepository.findByDateBetweenOrderByDateAsc(startDate, endDate);
    }

    public void exportToExcel(List<AttendanceLog> logs, OutputStream outputStream) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Attendance Logs");

        // Create header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Employee");
        headerRow.createCell(1).setCellValue("Date");
        headerRow.createCell(2).setCellValue("Check-In Time");
        headerRow.createCell(3).setCellValue("Check-Out Time");
        headerRow.createCell(4).setCellValue("Duration");

        // Populate data rows
        int rowNum = 1;
        for (AttendanceLog log : logs) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(log.getUser().getFirstName() + " " + log.getUser().getLastName());
            row.createCell(1).setCellValue(log.getDate().toString());
            row.createCell(2).setCellValue(log.getCheckInTime().toString());
            row.createCell(3).setCellValue(log.getCheckOutTime() != null ? log.getCheckOutTime().toString() : "N/A");
            row.createCell(4).setCellValue(calculateDuration(log));
        }

        // Auto-size columns
        for (int i = 0; i < 5; i++) {
            sheet.autoSizeColumn(i);
        }

        workbook.write(outputStream);
        workbook.close();
    }

    private String calculateDuration(AttendanceLog log) {
        if (log.getCheckOutTime() == null) {
            return "N/A";
        }
        Duration duration = Duration.between(log.getCheckInTime(), log.getCheckOutTime());
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
