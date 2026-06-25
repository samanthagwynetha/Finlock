package com.finlock.finlock.transaction.controller;

import com.finlock.finlock.auth.entity.User;
import com.finlock.finlock.common.response.ApiResponse;
import com.finlock.finlock.transaction.dto.TransferRequest;
import com.finlock.finlock.transaction.dto.TransferResponse;
import com.finlock.finlock.transaction.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {
    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<ApiResponse<TransferResponse>> transfer (
            @AuthenticationPrincipal User sender,
            @Valid @RequestBody TransferRequest request) {

        TransferResponse response = transferService.transfer(sender, request);

        return ResponseEntity.ok(
                ApiResponse.success("Transfer completed successfully", response)
        );
    }
}
