package com.finityedge.attendancetracker.service;

import com.finityedge.attendancetracker.model.AttendanceLog;
import com.finityedge.attendancetracker.model.User;
import com.finityedge.attendancetracker.repository.AttendanceLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceLogRepository attendanceLogRepository;
    @Autowired
    private QRCodeService qrCodeService;

    public AttendanceLog checkInOut(User user, String qrCodeContent) {
        System.out.println("QR Code: " + qrCodeContent);
        if (!qrCodeService.validateQRCode(qrCodeContent)) {
            throw new IllegalArgumentException("Invalid QR code");
        }

        LocalDate today = LocalDate.now();
        Optional<AttendanceLog> existingLog = attendanceLogRepository.findByUserAndDate(user, today);

        System.out.println("Existing Log: " + existingLog);

        if (existingLog.isPresent()) {
            AttendanceLog log = existingLog.get();
            if (log.getCheckOutTime() == null) {
                log.setCheckOutTime(LocalDateTime.now());
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
}
