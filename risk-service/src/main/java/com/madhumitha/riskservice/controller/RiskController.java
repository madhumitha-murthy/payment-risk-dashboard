package com.madhumitha.riskservice.controller;

import com.madhumitha.riskservice.model.RiskAssessment;
import com.madhumitha.riskservice.service.RiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RiskController {

    private final RiskService riskService;

    @PostMapping("/assess")
    public ResponseEntity<RiskAssessment> assessRisk(@RequestBody Map<String, Object> request) {
        String senderId = (String) request.get("senderId");
        Object rawAmount = request.get("amount");
        if (rawAmount == null) {
            throw new IllegalArgumentException("amount is required");
        }
        Double amount = ((Number) rawAmount).doubleValue();
        String type = (String) request.get("type");

        return ResponseEntity.ok(riskService.assessRisk(senderId, amount, type));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(riskService.getStats());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "risk-service"));
    }
}
