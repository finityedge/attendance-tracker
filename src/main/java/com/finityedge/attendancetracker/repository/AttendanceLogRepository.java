package com.finityedge.attendancetracker.repository;

import com.finityedge.attendancetracker.model.AttendanceLog;
import com.finityedge.attendancetracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, UUID> {
    Optional<AttendanceLog> findByUserAndDate(User user, LocalDate date);
    List<AttendanceLog> findByUserOrderByDateDesc(User user);
    List<AttendanceLog> findByDateBetweenOrderByDateAsc(LocalDate startDate, LocalDate endDate);
}
