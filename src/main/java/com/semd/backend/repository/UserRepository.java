package com.semd.backend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.semd.backend.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {
    boolean existsByUsername(String username);
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameOrEmailOrPhoneNumber(String username, String email, String phoneNumber);
    boolean existsByUsernameOrEmailOrPhoneNumber(String username, String email, String phoneNumber);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    Page<User> findAllByIsActive(Boolean isActive, Pageable pageable);
    Page<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    // ── THÊM MỚI cho Billing ──────────────────────────────
    Optional<User> findByPhoneNumber(String phoneNumber);
}