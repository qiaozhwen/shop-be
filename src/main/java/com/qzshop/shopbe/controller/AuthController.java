package com.qzshop.shopbe.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.qzshop.shopbe.auth.staff.LoginAttemptService;
import com.qzshop.shopbe.auth.staff.StaffEntity;
import com.qzshop.shopbe.auth.staff.StaffLockedException;
import com.qzshop.shopbe.auth.staff.StaffRepository;
import com.qzshop.shopbe.auth.token.TokenService;
import com.qzshop.shopbe.dto.LoginRequest;
import com.qzshop.shopbe.dto.LoginResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/auth")
public class AuthController {

    private final StaffRepository staffRepo;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttempt;
    private final TokenService tokenService;

    public AuthController(StaffRepository staffRepo,
                          PasswordEncoder passwordEncoder,
                          LoginAttemptService loginAttempt,
                          TokenService tokenService) {
        this.staffRepo = staffRepo;
        this.passwordEncoder = passwordEncoder;
        this.loginAttempt = loginAttempt;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        StaffEntity staff = staffRepo.findByPhone(req.getPhone())
                .orElseThrow(() -> new LoginFailedException("invalid credentials"));

        loginAttempt.ensureNotLocked(staff);

        if (!passwordEncoder.matches(req.getPassword(), staff.getPassword())) {
            loginAttempt.recordFailure(staff.getId());
            throw new LoginFailedException("invalid credentials");
        }

        loginAttempt.recordSuccess(staff.getId());

        var result = tokenService.issueForStaff(
                staff.getId(),
                List.of(staff.getRole()),
                req.getDeviceInfo() != null ? req.getDeviceInfo() : "unknown");

        return ResponseEntity.ok(new LoginResponse(result, staff.getName()).toMap());
    }

    public static class LoginFailedException extends RuntimeException {
        public LoginFailedException(String msg) { super(msg); }
    }

    @RestControllerAdvice
    public static class AuthExceptionHandler {
        @ExceptionHandler(LoginFailedException.class)
        public ResponseEntity<Map<String, String>> handleLoginFailed(LoginFailedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }

        @ExceptionHandler(StaffLockedException.class)
        public ResponseEntity<Map<String, Object>> handleLocked(StaffLockedException e) {
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(Map.of(
                        "error", "account locked",
                        "retryAfterSeconds", e.getRetryAfterSeconds()));
        }
    }
}
