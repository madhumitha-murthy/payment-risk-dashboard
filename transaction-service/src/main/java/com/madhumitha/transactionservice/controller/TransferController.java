package com.madhumitha.transactionservice.controller;

import com.madhumitha.transactionservice.dto.DailyRiskReport;
import com.madhumitha.transactionservice.dto.TransferRequest;
import com.madhumitha.transactionservice.model.Transaction;
import com.madhumitha.transactionservice.service.TransactionService;
import com.madhumitha.transactionservice.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TransferController {

    private final TransferService transferService;
    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> transfer(@Valid @RequestBody TransferRequest request) {
        Transaction tx = transferService.transfer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", tx.getId(),
                "senderId", tx.getSenderId(),
                "receiverId", tx.getReceiverId(),
                "amount", tx.getAmount(),
                "status", tx.getStatus().name(),
                "createdAt", tx.getCreatedAt().toString()
        ));
    }

    @GetMapping("/report")
    public ResponseEntity<List<DailyRiskReport>> report(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(transactionService.getRiskReport(from, to));
    }
}
