package com.finlock.finlock.reconciliation;

import com.finlock.finlock.transaction.repository.TransactionRepository;
import com.finlock.finlock.wallet.entity.Wallet;
import com.finlock.finlock.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReconciliationJob {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    @Scheduled(fixedDelay = 300000)
    public void reconcile() {
        log.info("[RECONCILIATION] Starting reconciliation job...");

        long startTime = System.currentTimeMillis();
        AtomicInteger issuesFound = new AtomicInteger(0);

        List<Wallet> allWallets = walletRepository.findAll();

        allWallets.forEach(wallet -> {
          if (wallet.getBalance().compareTo(BigDecimal.ZERO) <0) {
              log.error("[RECONCILIATION] NEGATIVE BALANCE DETECTED — " +
                              "Wallet ID: {} | User ID: {} | Currency: {} | Balance: {}",
                      wallet.getId(),
                      wallet.getUser().getId(),
                      wallet.getCurrency(),
                      wallet.getBalance());
              issuesFound.incrementAndGet();
          }
        });

        long transactionCount = transactionRepository.count();
        log.info("[RECONCILIATION] Total transactions on record: {}", transactionCount);

        allWallets.forEach(wallet ->
            log.info("[RECONCILIATION] Wallet {} ({}) | Balance: {} {}",
                    wallet.getId(),
                    wallet.getUser().getId(),
                    wallet.getBalance(),
                    wallet.getCurrency())
        );

        long duration = System.currentTimeMillis() - startTime;

        if (issuesFound.get() == 0) {
            log.info("[RECONCILIATION] Complete. No issues found. " +
                    "Checked {} wallets in {}ms.", allWallets.size(), duration);
        } else {
            log.error("[RECONCILIATION] Complete. {} issue(s) found! " +
                    "Immediate investigation required.", issuesFound.get());
        }
    }
}
