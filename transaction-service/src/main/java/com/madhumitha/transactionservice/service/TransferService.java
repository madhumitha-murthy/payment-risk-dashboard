package com.madhumitha.transactionservice.service;

import com.madhumitha.transactionservice.dto.TransferRequest;
import com.madhumitha.transactionservice.model.Account;
import com.madhumitha.transactionservice.model.Transaction;
import com.madhumitha.transactionservice.model.enums.RiskLevel;
import com.madhumitha.transactionservice.model.enums.TransactionStatus;
import com.madhumitha.transactionservice.model.enums.TransactionType;
import com.madhumitha.transactionservice.repository.AccountRepository;
import com.madhumitha.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public Transaction transfer(TransferRequest request) {
        Account source = accountRepository.findByAccountNumber(request.getSourceAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Source account not found: " + request.getSourceAccountNumber()));

        Account destination = accountRepository.findByAccountNumber(request.getDestinationAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Destination account not found: " + request.getDestinationAccountNumber()));

        if (source.getAccountNumber().equals(destination.getAccountNumber())) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        if (source.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds: available " + source.getBalance()
                            + ", requested " + request.getAmount());
        }

        // Debit source, credit destination — @Version ensures optimistic locking
        source.setBalance(source.getBalance().subtract(request.getAmount()));
        destination.setBalance(destination.getBalance().add(request.getAmount()));
        accountRepository.save(source);
        accountRepository.save(destination);

        Transaction tx = Transaction.builder()
                .senderId(source.getAccountNumber())
                .receiverId(destination.getAccountNumber())
                .amount(request.getAmount())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .riskLevel(RiskLevel.LOW)
                .riskScore(0.0)
                .description(request.getDescription())
                .sourceAccount(source)
                .destinationAccount(destination)
                .createdAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .build();

        Transaction saved = transactionRepository.save(tx);
        log.info("Transfer completed: {} -> {}, amount={}", source.getAccountNumber(),
                destination.getAccountNumber(), request.getAmount());
        return saved;
    }
}
