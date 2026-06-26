package com.finlock.finlock.auth.controller;

import com.finlock.finlock.auth.dto.LoginRequest;
import com.finlock.finlock.auth.dto.LoginResponse;
import com.finlock.finlock.auth.dto.RegisterRequest;
import com.finlock.finlock.auth.dto.RegisterResponse;
import com.finlock.finlock.auth.service.AuthService;
import com.finlock.finlock.common.response.ApiResponse;
import com.finlock.finlock.common.exception.RateLimitExceededException;
import com.finlock.finlock.common.ratelimit.RateLimiterService;
import java.time.Duration;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RateLimiterService rateLimiterService;

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
            @Valid @RequestBody LoginRequest request) {

        boolean allowed = rateLimiterService.tryConsume(
                "login:" + request.getEmail(),
                5,
                Duration.ofMinutes(1)
        );

        if (!allowed) {
            throw new RateLimitExceededException(
                    "Too many login attempts. Please try again in a minute."
            );
        }

        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successfully", response));
    }
}
