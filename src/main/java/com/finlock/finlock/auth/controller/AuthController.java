package com.finlock.finlock.auth.controller;

import com.finlock.finlock.auth.dto.LoginRequest;
import com.finlock.finlock.auth.dto.LoginResponse;
import com.finlock.finlock.auth.dto.RegisterRequest;
import com.finlock.finlock.auth.dto.RegisterResponse;
import com.finlock.finlock.auth.service.AuthService;
import com.finlock.finlock.common.response.ApiResponse;
import com.finlock.finlock.common.exception.RateLimitExceededException;
import com.finlock.finlock.common.ratelimit.RateLimiterService;
import com.finlock.finlock.audit.service.AuditLogService;
import java.time.Duration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RateLimiterService rateLimiterService;
    private final AuditLogService auditLogService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = httpRequest.getRemoteAddr();

        boolean allowed = rateLimiterService.tryConsume(
                "login:" + request.getEmail(),
                5,
                Duration.ofMinutes(1)
        );

        if (!allowed) {
            auditLogService.log(null, "LOGIN_RATE_LIMITED",
                    "Login rate limit exceeded for email: " + request.getEmail(), ipAddress);
            throw new RateLimitExceededException(
                    "Too many login attempts. Please try again in a minute."
            );
        }

        try {
            LoginResponse response = authService.login(request);

            auditLogService.log(null, "LOGIN_SUCCESS",
                    "Successful login for email: " + request.getEmail(), ipAddress);

            return ResponseEntity.ok(ApiResponse.success("Login successful", response));

        } catch (Exception e) {
            auditLogService.log(null, "LOGIN_FAILED",
                    "Failed login attempt for email: " + request.getEmail(), ipAddress);
            throw e;
        }
    }
}
