package com.finlock.finlock.transaction.service;

import com.finlock.finlock.auth.entity.User;
import com.finlock.finlock.auth.repository.UserRepository;
import com.finlock.finlock.common.exception.*;
import com.finlock.finlock.common.lock.DistributedLockService;
import com.finlock.finlock.transaction.dto.TransactionHistoryResponse;
import com.finlock.finlock.transaction.dto.TransferRequest;
import com.finlock.finlock.transaction.dto.TransferResponse;
import com.finlock.finlock.transaction.entity.Transaction;
import com.finlock.finlock.transaction.event.TransactionEvent;
import com.finlock.finlock.transaction.event.TransactionEventProducer;
import com.finlock.finlock.transaction.repository.TransactionRepository;
import com.finlock.finlock.wallet.entity.Wallet;
import com.finlock.finlock.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionEventProducer eventProducer;

    // Optional — only injected when Redis is available (local/production with Redis)
    // On Render free tier (no Redis), this is null and locking is skipped gracefully
    @Autowired(required = false)
    private DistributedLockService lockService;

    @Transactional
    public TransferResponse transfer(User sender, TransferRequest request, String idempotencyKey) {

        // Idempotency check — always first
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            Transaction previous = existing.get();
            return TransferResponse.builder()
                    .transactionId(previous.getId())
                    .recipientEmail(previous.getToWallet().getUser().getEmail())
                    .amount(previous.getAmount())
                    .currency(previous.getCurrency())
                    .senderNewBalance(previous.getFromWallet().getBalance())
                    .status(previous.getStatus())
                    .createdAt(previous.getCreatedAt())
                    .build();
        }

        User recipient = userRepository.findByEmail(request.getRecipientEmail())
                .orElseThrow(() -> new RecipientNotFoundException(
                        "No user found with email: " + request.getRecipientEmail()
                ));

        if (sender.getId().equals(recipient.getId())) {
            throw new SelfTransferException("You cannot transfer money to yourself");
        }

        Wallet senderWallet = walletRepository.findByUserIdAndCurrency(sender.getId(), request.getCurrency())
                .orElseThrow(() -> new WalletNotFoundException(
                        "You don't have a wallet for currency: " + request.getCurrency()
                ));

        Wallet recipientWallet = walletRepository.findByUserIdAndCurrency(recipient.getId(), request.getCurrency())
                .orElseThrow(() -> new WalletNotFoundException(
                        "Recipient doesn't have a wallet for currency: " + request.getCurrency()
                ));

        // Consistent lock ordering to prevent deadlocks
        String firstId = senderWallet.getId().toString();
        String secondId = recipientWallet.getId().toString();
        boolean senderFirst = firstId.compareTo(secondId) < 0;
        String lockKeyA = senderFirst ? firstId : secondId;
        String lockKeyB = senderFirst ? secondId : firstId;

        // Acquire locks only if Redis is available
        String lockTokenA = null;
        String lockTokenB = null;

        if (lockService != null) {
            lockTokenA = lockService.tryLock(lockKeyA, Duration.ofSeconds(5));
            if (lockTokenA == null) {
                throw new TransferInProgressException(
                        "Another transfer is already in progress. Please try again.");
            }
            lockTokenB = lockService.tryLock(lockKeyB, Duration.ofSeconds(5));
            if (lockTokenB == null) {
                lockService.unlock(lockKeyA, lockTokenA);
                throw new TransferInProgressException(
                        "Another transfer is already in progress. Please try again.");
            }
        }

        try {
            if (senderWallet.getBalance().compareTo(request.getAmount()) < 0) {
                throw new InsufficientBalanceException(
                        "Insufficient balance. Current balance: " + senderWallet.getBalance()
                );
            }

            senderWallet.setBalance(senderWallet.getBalance().subtract(request.getAmount()));
            recipientWallet.setBalance(recipientWallet.getBalance().add(request.getAmount()));

            walletRepository.save(senderWallet);
            walletRepository.save(recipientWallet);

            Transaction transaction = Transaction.builder()
                    .fromWallet(senderWallet)
                    .toWallet(recipientWallet)
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .status("COMPLETED")
                    .idempotencyKey(idempotencyKey)
                    .build();

            Transaction saved = transactionRepository.save(transaction);

            eventProducer.publish(TransactionEvent.builder()
                    .transactionId(saved.getId())
                    .senderEmail(sender.getEmail())
                    .recipientEmail(recipient.getEmail())
                    .amount(saved.getAmount())
                    .currency(saved.getCurrency())
                    .status(saved.getStatus())
                    .occurredAt(saved.getCreatedAt())
                    .build());

            return TransferResponse.builder()
                    .transactionId(saved.getId())
                    .recipientEmail(recipient.getEmail())
                    .amount(saved.getAmount())
                    .currency(saved.getCurrency())
                    .senderNewBalance(senderWallet.getBalance())
                    .status(saved.getStatus())
                    .createdAt(saved.getCreatedAt())
                    .build();

        } finally {
            // Always release locks, even if an exception was thrown
            if (lockService != null) {
                if (lockTokenB != null) lockService.unlock(lockKeyB, lockTokenB);
                if (lockTokenA != null) lockService.unlock(lockKeyA, lockTokenA);
            }
        }
    }

    public List<TransactionHistoryResponse> getTransactionHistory(User user) {
        List<Wallet> userWallets = walletRepository.findByUserId(user.getId());

        Set<Transaction> allTransactions = new LinkedHashSet<>();
        for (Wallet wallet : userWallets) {
            allTransactions.addAll(
                    transactionRepository.findByFromWalletIdOrToWalletId(wallet.getId(), wallet.getId())
            );
        }

        return allTransactions.stream()
                .sorted(Comparator.comparing(Transaction::getCreatedAt).reversed())
                .map(tx -> {
                    boolean isSender = tx.getFromWallet().getUser().getId().equals(user.getId());
                    String direction = isSender ? "SENT" : "RECEIVED";
                    String counterpartyEmail = isSender
                            ? tx.getToWallet().getUser().getEmail()
                            : tx.getFromWallet().getUser().getEmail();

                    return TransactionHistoryResponse.builder()
                            .transactionId(tx.getId())
                            .direction(direction)
                            .counterpartyEmail(counterpartyEmail)
                            .amount(tx.getAmount())
                            .currency(tx.getCurrency())
                            .status(tx.getStatus())
                            .createdAt(tx.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }
}