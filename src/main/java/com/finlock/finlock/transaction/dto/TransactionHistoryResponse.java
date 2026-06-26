package com.finlock.finlock.transaction.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TransactionHistoryResponse {
    private UUID transactionId;
    private String direction;
    private String counterpartyEmail;
    private BigDecimal amount;
    private String currency;
    private String status;
    private LocalDateTime createdAt;
}
