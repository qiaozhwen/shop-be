package com.qzshop.shopbe.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.qzshop.shopbe.auth.staff.LoginAttemptService;
import com.qzshop.shopbe.auth.staff.StaffEntity;
import com.qzshop.shopbe.auth.staff.StaffLockedException;
import com.qzshop.shopbe.auth.staff.StaffRepository;
import com.qzshop.shopbe.auth.security.StaffPrincipal;
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
    private final String dummyPasswordHash;

    public AuthController(StaffRepository staffRepo,
                          PasswordEncoder passwordEncoder,
                          LoginAttemptService loginAttempt,
                          TokenService tokenService) {
        this.staffRepo = staffRepo;
        this.passwordEncoder = passwordEncoder;
        this.loginAttempt = loginAttempt;
        this.tokenService = tokenService;
        this.dummyPasswordHash = passwordEncoder.encode("timing-only-password");
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        StaffEntity staff = staffRepo.findByPhone(req.getPhone()).orElse(null);

        if (staff == null || !"ACTIVE".equals(staff.getStatus())) {
            passwordEncoder.matches(req.getPassword(), dummyPasswordHash);
            throw new LoginFailedException("invalid credentials");
        }

        loginAttempt.ensureNotLocked(staff);

        String passwordHash = staff.getPassword();
        if (passwordHash == null || passwordHash.isBlank()) {
            passwordHash = dummyPasswordHash;
        }
        if (!passwordEncoder.matches(req.getPassword(), passwordHash)) {
            loginAttempt.recordFailure(staff.getId());
            throw new LoginFailedException("invalid credentials");
        }

        loginAttempt.recordSuccess(staff.getId());

        var result = tokenService.issueForStaff(
                staff.getId(),
                List.of(staff.getRole()),
                req.getDeviceInfo() != null ? req.getDeviceInfo() : "unknown");

        return ResponseEntity.ok(new LoginResponse(result,
                LoginResponse.subject(staff.getId(), staff.getPhone(), staff.getName(),
                        List.of(staff.getRole()), staff.getPassword() != null && !staff.getPassword().isBlank(), List.of())).toMap());
    }

    @PostMapping("/set-password")
    public ResponseEntity<Void> setPassword(@RequestBody Map<String, Object> body) {
        StaffPrincipal principal = currentPrincipal();
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String newPassword = String.valueOf(body.getOrDefault("newPassword", ""));
        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest().build();
        }

        StaffEntity staff = staffRepo.findById(principal.staffId()).orElse(null);
        if (staff == null || !"ACTIVE".equals(staff.getStatus())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String currentPassword = staff.getPassword();
        if (currentPassword != null && !currentPassword.isBlank()) {
            String oldPassword = String.valueOf(body.getOrDefault("oldPassword", ""));
            if (!passwordEncoder.matches(oldPassword, currentPassword)) {
                return ResponseEntity.badRequest().build();
            }
        }

        staff.setPassword(passwordEncoder.encode(newPassword));
        staffRepo.save(staff);
        tokenService.revokeAllForStaff(staff.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping({"/sms/send", "/sms/login", "/reset-password", "/bind-phone"})
    public ResponseEntity<Map<String, String>> unsupportedSecondaryAuthentication() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("message", "当前版本未启用短信或第三方登录"));
    }

    @PostMapping("/sso/{provider}/{action}")
    public ResponseEntity<Map<String, String>> unsupportedSso(@PathVariable String provider,
                                                               @PathVariable String action) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("message", "当前版本未启用短信或第三方登录"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) Map<String, Object> body) {
        if (body != null && body.get("refreshToken") != null) {
            tokenService.revokeOne(String.valueOf(body.get("refreshToken")));
        } else {
            StaffPrincipal principal = currentPrincipal();
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            tokenService.revokeAllForStaff(principal.staffId());
        }
        return ResponseEntity.ok().build();
    }

    private StaffPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof StaffPrincipal principal)) {
            return null;
        }
        return principal;
    }

    @RestController
    public static class AdminMeController {
        private final StaffRepository staffRepo;

        public AdminMeController(StaffRepository staffRepo) {
            this.staffRepo = staffRepo;
        }

        @GetMapping("/api/admin/me")
        public ResponseEntity<Map<String, Object>> me() {
            StaffPrincipal principal = principal();
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Authentication required"));
            }
            Optional<StaffEntity> staff = staffRepo.findById(principal.staffId());
            if (staff.isPresent()) {
                StaffEntity s = staff.get();
                if (!"ACTIVE".equals(s.getStatus())) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Authentication required"));
                }
                return ResponseEntity.ok(LoginResponse.subject(s.getId(), s.getPhone(), s.getName(),
                        List.of(s.getRole()), s.getPassword() != null && !s.getPassword().isBlank(), List.of()));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Authentication required"));
        }

        private StaffPrincipal principal() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof StaffPrincipal principal)) {
                return null;
            }
            return principal;
        }
    }

    @RestController
    public static class RefreshController {
        private final StaffRepository staffRepo;
        private final TokenService tokenService;

        public RefreshController(StaffRepository staffRepo, TokenService tokenService) {
            this.staffRepo = staffRepo;
            this.tokenService = tokenService;
        }

        @PostMapping("/api/auth/refresh")
        public ResponseEntity<Map<String, Object>> refresh(@RequestBody Map<String, Object> body) {
            String refreshToken = String.valueOf(body.getOrDefault("refreshToken", ""));
            try {
                var rotated = tokenService.rotate(refreshToken, staffId -> staffRepo.findById(staffId)
                        .filter(s -> "ACTIVE".equals(s.getStatus()))
                        .map(s -> List.of(s.getRole()))
                        .orElseThrow(() -> new LoginFailedException("invalid staff")));
                StaffEntity staff = staffRepo.findById(rotated.staffId())
                        .orElseThrow(() -> new LoginFailedException("invalid staff"));
                Map<String, Object> subject = LoginResponse.subject(staff.getId(), staff.getPhone(), staff.getName(),
                        List.of(staff.getRole()), staff.getPassword() != null && !staff.getPassword().isBlank(), List.of());
                return ResponseEntity.ok(new LoginResponse(rotated, subject).toMap());
            } catch (RuntimeException ex) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "refresh token invalid"));
            }
        }
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
