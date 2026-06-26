package com.finlock.finlock.transaction.service;

import com.finlock.finlock.auth.entity.User;
import com.finlock.finlock.auth.repository.UserRepository;
import com.finlock.finlock.common.exception.*;
import com.finlock.finlock.common.lock.DistributedLockService;
import com.finlock.finlock.transaction.dto.TransferRequest;
import com.finlock.finlock.transaction.dto.TransferResponse;
import com.finlock.finlock.transaction.entity.Transaction;
import com.finlock.finlock.transaction.repository.TransactionRepository;
import com.finlock.finlock.wallet.entity.Wallet;
import com.finlock.finlock.wallet.repository.WalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final DistributedLockService lockService;

    @Transactional
    public TransferResponse transfer(User sender, TransferRequest request, String idempotencyKey) {

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

        //Find receipt user by email
        User recipient = userRepository.findByEmail(request.getRecipientEmail())
                .orElseThrow(() -> new RecipientNotFoundException(
                        "No user found with email: " + request.getRecipientEmail()
        ));

        // Block self-transfer before touching any wallet data
        if (sender.getId().equals(recipient.getId())) {
            throw new SelfTransferException("You cannot transfer money to yourself");
        }

        // Find sender & recipient wallet for the given currency
        Wallet senderWallet =  walletRepository.findByUserIdAndCurrency(sender.getId(), request.getCurrency())
                .orElseThrow(() -> new WalletNotFoundException(
                        "Recipient doesn't have a wallet for currency: " + request.getCurrency()
                ));

        Wallet recipientWallet = walletRepository.findByUserIdAndCurrency(recipient.getId(), request.getCurrency())
                .orElseThrow(() -> new WalletNotFoundException(
                        "Recipient doesn't have a wallet for currency: " + request.getCurrency()
                ));

        String firstId = senderWallet.getId().toString();
        String secondId = recipientWallet.getId().toString();
        boolean senderFirst = firstId.compareTo(secondId) < 0;

        String lockKeyA = senderFirst ? firstId: secondId;
        String lockKeyB = senderFirst ? secondId: firstId;

        String lockTokenA = lockService.tryLock(lockKeyA, Duration.ofSeconds(5));

        if (lockTokenA == null) {
            throw new TransferInProgressException("Another transfer is already in progress. Please try again.");

        }

        String lockTokenB = lockService.tryLock(lockKeyB, Duration.ofSeconds(5));
        if (lockTokenB == null) {
            lockService.unlock(lockKeyA, lockTokenA);
            throw new TransferInProgressException("Another transfer is already in progress. Please try again");
        }

        try {
            // Balance check
            if (senderWallet.getBalance().compareTo(request.getAmount()) <0) {
                throw new InsufficientBalanceException(
                        "Insufficient balance. Current balance: " + senderWallet.getBalance()
                );
            }

            //  Move the money
            senderWallet.setBalance(senderWallet.getBalance().subtract(request.getAmount()));
            recipientWallet.setBalance(recipientWallet.getBalance().add(request.getAmount()));

            walletRepository.save(senderWallet);
            walletRepository.save(recipientWallet);

            // Record the transfer as an immutable transaction log entry
            Transaction transaction = Transaction.builder()
                    .fromWallet(senderWallet)
                    .toWallet(recipientWallet)
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .status("COMPLETED")
                    .idempotencyKey(idempotencyKey)
                    .build();

            Transaction saved = transactionRepository.save(transaction);

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
            lockService.unlock(lockKeyB, lockTokenB);
            lockService.unlock(lockKeyA, lockTokenA);
        }


    }
}
