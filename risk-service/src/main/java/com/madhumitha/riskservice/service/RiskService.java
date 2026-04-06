package com.madhumitha.riskservice.service;

import com.madhumitha.riskservice.model.RiskAssessment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * RiskService bridges the Java Spring Boot microservice layer
 * to the Python LSTM Autoencoder anomaly detection API.
 *
 * Architecture:
 *   React Frontend
 *       → Transaction Service (Java Spring Boot)
 *           → Risk Service (Java Spring Boot)   ← YOU ARE HERE
 *               → Python FastAPI (LSTM Autoencoder anomaly detection)
 *
 * This service applies rule-based checks first (fast, no ML needed),
 * then calls the Python ML API for borderline cases.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskService {

    private final RestTemplate restTemplate;
    private final MongoTemplate mongoTemplate;

    @Value("${python.ml.api.url:http://localhost:8000}")
    private String pythonMlApiUrl;

    // Rule-based thresholds
    private static final double HIGH_AMOUNT_THRESHOLD = 10000.0;
    private static final double MEDIUM_AMOUNT_THRESHOLD = 1000.0;

    public RiskAssessment assessRisk(String senderId, Double amount, String transactionType) {
        RiskAssessment assessment = RiskAssessment.builder()
                .senderId(senderId)
                .amount(amount)
                .transactionType(transactionType)
                .build();

        // Step 1: Rule-based assessment (always runs)
        double ruleScore = computeRuleBasedScore(amount, transactionType);

        // Step 2: Try to call Python ML API for additional signal
        double mlScore = tryCallPythonMlApi(senderId, amount);

        // Step 3: Combine scores — use rule score directly if ML is unavailable
        double finalScore = mlScore > 0
                ? (ruleScore * 0.4) + (mlScore * 0.6)
                : ruleScore;

        assessment.setRiskScore(Math.min(1.0, finalScore));
        assessment.setRiskLevel(classifyRisk(finalScore));
        assessment.setAnomaly(finalScore > 0.7);
        assessment.setAssessmentSource(mlScore > 0 ? "ML_MODEL" : "RULE_BASED");

        // Persist assessment to MongoDB
        mongoTemplate.save(assessment);

        log.info("Risk assessed: senderId={}, amount={}, score={}, level={}",
                senderId, amount, assessment.getRiskScore(), assessment.getRiskLevel());

        return assessment;
    }

    private double computeRuleBasedScore(Double amount, String transactionType) {
        double score = 0.0;

        // Amount-based rules
        if (amount > HIGH_AMOUNT_THRESHOLD) score += 0.8;
        else if (amount > MEDIUM_AMOUNT_THRESHOLD) score += 0.3;
        else score += 0.1;

        // Transaction type rules
        if ("REFUND".equals(transactionType)) score += 0.2;

        return Math.min(1.0, score);
    }

    private double tryCallPythonMlApi(String senderId, Double amount) {
        try {
            // Call your existing Python LSTM anomaly detection API
            // The Python API expects a time-series window — here we send a simplified feature vector
            String url = pythonMlApiUrl + "/predict";
            Map<String, Object> request = Map.of(
                    "window", buildFeatureWindow(amount),
                    "threshold", 6.28
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            if (response != null) {
                Number anomalyScore = (Number) response.get("anomaly_score");
                Boolean isAnomaly = (Boolean) response.get("is_anomaly");
                log.info("Python ML API response: score={}, isAnomaly={}", anomalyScore, isAnomaly);
                return anomalyScore != null ? Math.min(1.0, anomalyScore.doubleValue() / 10.0) : 0.0;
            }
        } catch (Exception e) {
            log.warn("Python ML API unavailable, using rule-based only: {}", e.getMessage());
        }
        return 0.0;
    }

    private double[][] buildFeatureWindow(Double amount) {
        // Build a 30-step feature window (window_size=30, input_dim=1 simplified)
        // In production this would use real transaction history
        double[][] window = new double[30][1];
        for (int i = 0; i < 30; i++) {
            window[i][0] = amount / HIGH_AMOUNT_THRESHOLD; // normalise
        }
        return window;
    }

    private String classifyRisk(double score) {
        if (score > 0.7) return "HIGH";
        if (score > 0.4) return "MEDIUM";
        return "LOW";
    }

    public Map<String, Object> getStats() {
        long total = mongoTemplate.count(
                new org.springframework.data.mongodb.core.query.Query(), RiskAssessment.class);
        long highRisk = mongoTemplate.count(
                new org.springframework.data.mongodb.core.query.Query(
                        org.springframework.data.mongodb.core.query.Criteria.where("riskLevel").is("HIGH")),
                RiskAssessment.class);
        return Map.of("total_assessments", total, "high_risk_count", highRisk);
    }
}
