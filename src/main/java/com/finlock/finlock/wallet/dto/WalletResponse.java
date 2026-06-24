package com.finlock.finlock.wallet.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class WalletResponse {
    private UUID id;
    private String currency;
    private BigDecimal balance;
    private LocalDateTime createdAt;
}
