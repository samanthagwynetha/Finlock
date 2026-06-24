package com.finlock.finlock.wallet.service;

import com.finlock.finlock.auth.entity.User;
import com.finlock.finlock.common.exception.WalletAlreadyExistsException;
import com.finlock.finlock.wallet.dto.WalletResponse;
import com.finlock.finlock.wallet.dto.CreateWalletRequest;
import com.finlock.finlock.wallet.entity.Wallet;
import com.finlock.finlock.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;

    public WalletResponse createWallet(User user, CreateWalletRequest request) {

        if(walletRepository.existsByUserIdAndCurrency(user.getId(), request.getCurrency())){
            throw new WalletAlreadyExistsException(
                    "Wallet already exists for currency: " + request.getCurrency()
            );
        }

        BigDecimal startingBalance = request.getInitialBalance() != null
                ? request.getInitialBalance()
                : BigDecimal.ZERO;

        Wallet wallet = Wallet.builder()
                .user(user)
                .currency(request.getCurrency())
                .balance(startingBalance)
                .build();

        Wallet saved = walletRepository.save(wallet);

        return WalletResponse.builder()
                .id(saved.getId())
                .currency(saved.getCurrency())
                .balance(saved.getBalance())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    public List<WalletResponse> getMyWallets(User user){
        List<Wallet> wallets = walletRepository.findByUserId(user.getId());

        return wallets.stream()
                .map(wallet -> WalletResponse.builder()
                        .id(wallet.getId())
                        .currency(wallet.getCurrency())
                        .balance(wallet.getBalance())
                        .createdAt(wallet.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

    }

}
