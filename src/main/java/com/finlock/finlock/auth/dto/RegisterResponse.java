package com.finlock.finlock.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class RegisterResponse {
    private UUID id;
    private String fullName;
    private String email;

}
