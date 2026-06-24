package com.finlock.finlock.wallet.controller;

import com.finlock.finlock.auth.entity.User;
import com.finlock.finlock.common.response.ApiResponse;
import com.finlock.finlock.wallet.dto.CreateWalletRequest;
import com.finlock.finlock.wallet.dto.WalletResponse;
import com.finlock.finlock.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<ApiResponse<WalletResponse>> createWallet(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateWalletRequest request) {

        WalletResponse response = walletService.createWallet(user, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Wallet created successfully", response));
    }
}
