package com.finlock.finlock.wallet.controller;

import com.finlock.finlock.auth.entity.User;
import com.finlock.finlock.common.response.ApiResponse;
import com.finlock.finlock.wallet.dto.CreateWalletRequest;
import com.finlock.finlock.wallet.dto.DepositRequest;
import com.finlock.finlock.wallet.dto.WithdrawRequest;
import com.finlock.finlock.wallet.dto.WalletResponse;
import com.finlock.finlock.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<WalletResponse>>> getMyWallets(
            @AuthenticationPrincipal User user) {

        List<WalletResponse> wallets = walletService.getMyWallets(user);

        return ResponseEntity.ok(
                ApiResponse.success("Wallets retrieved successfully", wallets)
        );
    }

    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<WalletResponse>> deposit(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody DepositRequest request) {
        WalletResponse response = walletService.deposit(user, request);

        return ResponseEntity.ok(
                ApiResponse.success("Deposit successful", response)
        );
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<WalletResponse>> withdraw(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody WithdrawRequest request) {

        WalletResponse response = walletService.withdraw(user, request);

        return ResponseEntity.ok(
                ApiResponse.success("Withdrawal successful", response)
        );
    }
}
