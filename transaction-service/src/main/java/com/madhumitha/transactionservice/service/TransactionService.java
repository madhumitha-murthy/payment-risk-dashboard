package com.madhumitha.transactionservice.service;

import com.madhumitha.transactionservice.model.Transaction;
import com.madhumitha.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final RestTemplate restTemplate;
    private final MongoTemplate mongoTemplate;

    @Value("${risk.service.url:http://risk-service:8083}")
    private String riskServiceUrl;

    public Transaction createTransaction(Transaction transaction) {
        // Call risk service to assess transaction risk
        try {
            String riskUrl = riskServiceUrl + "/api/risk/assess";
            Map<String, Object> riskRequest = Map.of(
                    "amount", transaction.getAmount(),
                    "senderId", transaction.getSenderId(),
                    "type", transaction.getType()
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> riskResponse = restTemplate.postForObject(
                    riskUrl, riskRequest, Map.class);

            if (riskResponse != null) {
                transaction.setRiskScore(((Number) riskResponse.get("riskScore")).doubleValue());
                transaction.setRiskLevel((String) riskResponse.get("riskLevel"));

                // Auto-flag high risk transactions
                if ("HIGH".equals(transaction.getRiskLevel())) {
                    transaction.setStatus("FLAGGED");
                    log.warn("Transaction flagged as HIGH risk: senderId={}, amount={}",
                            transaction.getSenderId(), transaction.getAmount());
                } else {
                    transaction.setStatus("COMPLETED");
                    transaction.setProcessedAt(LocalDateTime.now());
                }
            }
        } catch (Exception e) {
            log.error("Risk service unavailable, proceeding with default: {}", e.getMessage());
            transaction.setRiskLevel("UNKNOWN");
            transaction.setRiskScore(0.0);
            transaction.setStatus("COMPLETED");
            transaction.setProcessedAt(LocalDateTime.now());
        }

        if (transaction.getCreatedAt() == null) {
            transaction.setCreatedAt(LocalDateTime.now());
        }
        return transactionRepository.save(transaction);
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public Optional<Transaction> getTransactionById(String id) {
        return transactionRepository.findById(id);
    }

    public List<Transaction> getTransactionsBySender(String senderId) {
        return transactionRepository.findBySenderIdOrderByCreatedAtDesc(senderId);
    }

    public List<Transaction> getFlaggedTransactions() {
        return transactionRepository.findByStatus("FLAGGED");
    }

    public Map<String, Long> getTransactionStats() {
        return Map.of(
                "total", transactionRepository.count(),
                "completed", transactionRepository.countByStatus("COMPLETED"),
                "flagged", transactionRepository.countByStatus("FLAGGED"),
                "pending", transactionRepository.countByStatus("PENDING")
        );
    }

    public List<Transaction> searchTransactions(
            String status, String riskLevel, String type,
            Double amountMin, Double amountMax) {

        Query query = new Query();

        if (status != null && !status.isBlank())
            query.addCriteria(Criteria.where("status").is(status));
        if (riskLevel != null && !riskLevel.isBlank())
            query.addCriteria(Criteria.where("riskLevel").is(riskLevel));
        if (type != null && !type.isBlank())
            query.addCriteria(Criteria.where("type").is(type));
        if (amountMin != null || amountMax != null) {
            Criteria amountCriteria = Criteria.where("amount");
            if (amountMin != null) amountCriteria = amountCriteria.gte(amountMin);
            if (amountMax != null) amountCriteria = amountCriteria.lte(amountMax);
            query.addCriteria(amountCriteria);
        }

        return mongoTemplate.find(query, Transaction.class);
    }
}
