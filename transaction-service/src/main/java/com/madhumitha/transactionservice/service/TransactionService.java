package com.madhumitha.transactionservice.service;

import com.madhumitha.transactionservice.dto.DailyRiskReport;
import com.madhumitha.transactionservice.model.Transaction;
import com.madhumitha.transactionservice.model.enums.RiskLevel;
import com.madhumitha.transactionservice.model.enums.TransactionStatus;
import com.madhumitha.transactionservice.model.enums.TransactionType;
import com.madhumitha.transactionservice.repository.TransactionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final RestTemplate restTemplate;
    private final EntityManager entityManager;

    @Value("${risk.service.url:http://risk-service:8083}")
    private String riskServiceUrl;

    public Transaction createTransaction(Transaction transaction) {
        try {
            String riskUrl = riskServiceUrl + "/api/risk/assess";
            Map<String, Object> riskRequest = Map.of(
                    "amount", transaction.getAmount(),
                    "senderId", transaction.getSenderId(),
                    "type", transaction.getType().name()
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> riskResponse = restTemplate.postForObject(
                    riskUrl, riskRequest, Map.class);

            if (riskResponse != null) {
                transaction.setRiskScore(((Number) riskResponse.get("riskScore")).doubleValue());
                String level = (String) riskResponse.get("riskLevel");
                transaction.setRiskLevel(RiskLevel.valueOf(level));

                if (RiskLevel.HIGH.equals(transaction.getRiskLevel())) {
                    transaction.setStatus(TransactionStatus.FLAGGED);
                    log.warn("Transaction flagged as HIGH risk: senderId={}, amount={}",
                            transaction.getSenderId(), transaction.getAmount());
                } else {
                    transaction.setStatus(TransactionStatus.COMPLETED);
                    transaction.setProcessedAt(LocalDateTime.now());
                }
            }
        } catch (Exception e) {
            log.error("Risk service unavailable, proceeding with default: {}", e.getMessage());
            transaction.setRiskLevel(RiskLevel.UNKNOWN);
            transaction.setRiskScore(0.0);
            transaction.setStatus(TransactionStatus.COMPLETED);
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

    public Optional<Transaction> getTransactionById(Long id) {
        return transactionRepository.findById(id);
    }

    public List<Transaction> getTransactionsBySender(String senderId) {
        return transactionRepository.findBySenderIdOrderByCreatedAtDesc(senderId);
    }

    public List<Transaction> getFlaggedTransactions() {
        return transactionRepository.findByStatus(TransactionStatus.FLAGGED);
    }

    public Map<String, Long> getTransactionStats() {
        return Map.of(
                "total", transactionRepository.count(),
                "completed", transactionRepository.countByStatus(TransactionStatus.COMPLETED),
                "flagged", transactionRepository.countByStatus(TransactionStatus.FLAGGED),
                "pending", transactionRepository.countByStatus(TransactionStatus.PENDING)
        );
    }

    public List<Transaction> searchTransactions(
            String status, String riskLevel, String type,
            Double amountMin, Double amountMax) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Transaction> cq = cb.createQuery(Transaction.class);
        Root<Transaction> root = cq.from(Transaction.class);
        List<Predicate> predicates = new ArrayList<>();

        if (status != null && !status.isBlank()) {
            predicates.add(cb.equal(root.get("status"), TransactionStatus.valueOf(status)));
        }
        if (riskLevel != null && !riskLevel.isBlank()) {
            predicates.add(cb.equal(root.get("riskLevel"), RiskLevel.valueOf(riskLevel)));
        }
        if (type != null && !type.isBlank()) {
            predicates.add(cb.equal(root.get("type"), TransactionType.valueOf(type)));
        }
        if (amountMin != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), BigDecimal.valueOf(amountMin)));
        }
        if (amountMax != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("amount"), BigDecimal.valueOf(amountMax)));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        TypedQuery<Transaction> query = entityManager.createQuery(cq);
        return query.getResultList();
    }

    public Page<Transaction> getAllTransactionsPaged(Pageable pageable) {
        return transactionRepository.findAll(pageable);
    }

    public List<DailyRiskReport> getRiskReport(LocalDate from, LocalDate to) {
        List<Object[]> rows = transactionRepository.aggregateByRiskLevelAndDate(
                from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        return rows.stream()
                .map(r -> new DailyRiskReport(
                        (RiskLevel) r[0],
                        (LocalDate) r[1],
                        (Long) r[2],
                        (BigDecimal) r[3]))
                .toList();
    }
}
