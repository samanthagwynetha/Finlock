package com.finlock.finlock.transaction.controller;

import com.finlock.finlock.auth.entity.User;
import com.finlock.finlock.common.response.ApiResponse;
import com.finlock.finlock.transaction.dto.TransactionHistoryResponse;
import com.finlock.finlock.transaction.dto.TransferRequest;
import com.finlock.finlock.transaction.dto.TransferResponse;
import com.finlock.finlock.transaction.service.TransferService;
import com.finlock.finlock.audit.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {
    private final TransferService transferService;
    private final AuditLogService auditLogService;

    @PostMapping
    public ResponseEntity<ApiResponse<TransferResponse>> transfer (
            @AuthenticationPrincipal User sender,
            @Valid @RequestBody TransferRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            HttpServletRequest httpRequest) {

        String ipAdress = httpRequest.getRemoteAddr();

        try {
            TransferResponse response = transferService.transfer(sender, request, idempotencyKey);
            auditLogService.log(sender.getId(), "TRANSFER_COMPLETED",
                    String.format("Transferred %s %s to %s",
                            response.getAmount(), response.getCurrency(), response.getRecipientEmail()),
                    ipAdress);
            return ResponseEntity.ok(
                    ApiResponse.success("Transfer completed successfully", response)
            );
        } catch (Exception e) {
            auditLogService.log(sender.getId(), "TRANSFER_FAILED",
                    String.format("Failed transfer attempt to %s: %s",
                            request.getRecipientEmail(), e.getMessage()),
                    ipAdress);
            throw e;
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TransactionHistoryResponse>>> getHistory(
            @AuthenticationPrincipal User user) {

        List<TransactionHistoryResponse> history = transferService.getTransactionHistory(user);
        return ResponseEntity.ok(
                ApiResponse.success("Transaction history retrieved successfully", history)
        );
    }
}
