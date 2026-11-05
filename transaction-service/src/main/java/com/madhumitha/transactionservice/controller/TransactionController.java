package com.madhumitha.transactionservice.controller;

import com.madhumitha.transactionservice.dto.TransferRequest;
import com.madhumitha.transactionservice.model.Transaction;
import com.madhumitha.transactionservice.model.enums.TransactionStatus;
import com.madhumitha.transactionservice.model.enums.TransactionType;
import com.madhumitha.transactionservice.service.TransactionService;
import com.madhumitha.transactionservice.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        Transaction tx = Transaction.builder()
                .senderId(request.senderId())
                .receiverId(request.receiverId())
                .amount(BigDecimal.valueOf(request.amount()))
                .currency(request.currency() != null ? request.currency() : "SGD")
                .type(TransactionType.valueOf(request.type()))
                .status(TransactionStatus.PENDING)
                .description(request.description())
                .build();

        Transaction saved = transactionService.createTransaction(tx);
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponse.from(saved));
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        if (page == 0 && size == 100) {
            // Default: return all (backwards compatible)
            return ResponseEntity.ok(transactionService.getAllTransactions().stream()
                    .map(TransactionResponse::from).toList());
        }
        Page<Transaction> paged = transactionService.getAllTransactionsPaged(
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(paged.getContent().stream()
                .map(TransactionResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable Long id) {
        return transactionService.getTransactionById(id)
                .map(tx -> ResponseEntity.ok(TransactionResponse.from(tx)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/sender/{senderId}")
    public ResponseEntity<List<TransactionResponse>> getBySender(@PathVariable String senderId) {
        return ResponseEntity.ok(transactionService.getTransactionsBySender(senderId).stream()
                .map(TransactionResponse::from).toList());
    }

    @GetMapping("/flagged")
    public ResponseEntity<List<TransactionResponse>> getFlagged() {
        return ResponseEntity.ok(transactionService.getFlaggedTransactions().stream()
                .map(TransactionResponse::from).toList());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(transactionService.getTransactionStats());
    }

    @GetMapping("/search")
    public ResponseEntity<List<TransactionResponse>> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Double amountMin,
            @RequestParam(required = false) Double amountMax) {
        return ResponseEntity.ok(transactionService.searchTransactions(status, riskLevel, type, amountMin, amountMax)
                .stream().map(TransactionResponse::from).toList());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "transaction-service"));
    }

    // --- JSON contract record: same field names as the old MongoDB model ---
    record TransactionResponse(
            String id,
            String senderId,
            String receiverId,
            Double amount,
            String currency,
            String type,
            String status,
            String riskLevel,
            Double riskScore,
            String description,
            String createdAt,
            String processedAt
    ) {
        static TransactionResponse from(Transaction tx) {
            return new TransactionResponse(
                    tx.getId() != null ? tx.getId().toString() : null,
                    tx.getSenderId(),
                    tx.getReceiverId(),
                    tx.getAmount() != null ? tx.getAmount().doubleValue() : null,
                    tx.getCurrency(),
                    tx.getType() != null ? tx.getType().name() : null,
                    tx.getStatus() != null ? tx.getStatus().name() : null,
                    tx.getRiskLevel() != null ? tx.getRiskLevel().name() : null,
                    tx.getRiskScore(),
                    tx.getDescription(),
                    tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : null,
                    tx.getProcessedAt() != null ? tx.getProcessedAt().toString() : null
            );
        }
    }

    record CreateTransactionRequest(
            String senderId,
            String receiverId,
            double amount,
            String currency,
            String type,
            String description
    ) {}
}
