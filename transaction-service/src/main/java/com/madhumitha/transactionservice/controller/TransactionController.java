package com.madhumitha.transactionservice.controller;

import com.madhumitha.transactionservice.model.Transaction;
import com.madhumitha.transactionservice.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<Transaction> createTransaction(@Valid @RequestBody Transaction transaction) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.createTransaction(transaction));
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable String id) {
        return transactionService.getTransactionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/sender/{senderId}")
    public ResponseEntity<List<Transaction>> getBySender(@PathVariable String senderId) {
        return ResponseEntity.ok(transactionService.getTransactionsBySender(senderId));
    }

    @GetMapping("/flagged")
    public ResponseEntity<List<Transaction>> getFlagged() {
        return ResponseEntity.ok(transactionService.getFlaggedTransactions());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(transactionService.getTransactionStats());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Transaction>> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Double amountMin,
            @RequestParam(required = false) Double amountMax) {
        return ResponseEntity.ok(transactionService.searchTransactions(status, riskLevel, type, amountMin, amountMax));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "transaction-service"));
    }
}
