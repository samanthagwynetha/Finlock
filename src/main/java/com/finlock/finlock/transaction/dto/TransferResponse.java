package com.finlock.finlock.transaction.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TransferResponse {
    private UUID transactionId;
    private String recipientEmail;
    private BigDecimal amount;
    private String currency;
    private BigDecimal senderNewBalance;
    private String  status;
    private LocalDateTime createdAt;
}
