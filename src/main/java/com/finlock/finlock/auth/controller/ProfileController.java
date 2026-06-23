package com.finlock.finlock.auth.controller;

import com.finlock.finlock.auth.entity.User;
import com.finlock.finlock.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    @GetMapping("/me")
    public ApiResponse<Map<String, String>> me(@AuthenticationPrincipal User user) {
        return ApiResponse.success("Authenticated user", Map.of(
                "email", user.getEmail(),
                "fullName", user.getFullName()
        ));
    }
}
