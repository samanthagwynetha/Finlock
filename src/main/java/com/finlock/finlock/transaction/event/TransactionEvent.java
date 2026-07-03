package com.finlock.finlock.transaction.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {
    private UUID transactionId;
    private String senderEmail;
    private String recipientEmail;
    private BigDecimal amount;
    private String currency;
    private String status;
    private LocalDateTime occurredAt;

}
