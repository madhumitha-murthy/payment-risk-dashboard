package com.madhumitha.intelligenceservice.controller;

import com.madhumitha.intelligenceservice.model.ExplanationResponse;
import com.madhumitha.intelligenceservice.model.QueryResponse;
import com.madhumitha.intelligenceservice.service.IntelligenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/intelligence")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class IntelligenceController {

    private final IntelligenceService intelligenceService;

    @GetMapping("/explain/{transactionId}")
    public ResponseEntity<ExplanationResponse> explain(@PathVariable String transactionId) {
        return ResponseEntity.ok(intelligenceService.explainTransaction(transactionId));
    }

    @PostMapping("/query")
    public ResponseEntity<QueryResponse> query(@RequestBody Map<String, String> body) {
        String q = body.get("query");
        if (q == null || q.isBlank()) {
            throw new IllegalArgumentException("query field is required");
        }
        return ResponseEntity.ok(intelligenceService.queryTransactions(q));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "intelligence-service"));
    }
}
