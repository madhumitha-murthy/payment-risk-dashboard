package com.madhumitha.riskservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "risk_assessments")
public class RiskAssessment {

    @Id
    private String id;

    private String senderId;
    private Double amount;
    private String transactionType;
    private Double riskScore;        // 0.0 - 1.0
    private String riskLevel;        // LOW, MEDIUM, HIGH
    private boolean isAnomaly;
    private String assessmentSource; // RULE_BASED, ML_MODEL

    @Builder.Default
    private LocalDateTime assessedAt = LocalDateTime.now();
}
