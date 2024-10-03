package com.finityedge.attendancetracker.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.finityedge.attendancetracker.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
}
