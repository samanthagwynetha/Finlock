package com.finlock.finlock.transaction.service;

import com.finlock.finlock.auth.entity.User;
import com.finlock.finlock.auth.repository.UserRepository;
import com.finlock.finlock.common.exception.InsufficientBalanceException;
import com.finlock.finlock.common.exception.RecipientNotFoundException;
import com.finlock.finlock.common.exception.SelfTransferException;
import com.finlock.finlock.common.exception.WalletNotFoundException;
import com.finlock.finlock.transaction.dto.TransferRequest;
import com.finlock.finlock.transaction.dto.TransferResponse;
import com.finlock.finlock.transaction.entity.Transaction;
import com.finlock.finlock.transaction.repository.TransactionRepository;
import com.finlock.finlock.wallet.entity.Wallet;
import com.finlock.finlock.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public TransferResponse transfer(User sender, TransferRequest request) {

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
                .idempotencyKey(UUID.randomUUID().toString())
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
    }
}
