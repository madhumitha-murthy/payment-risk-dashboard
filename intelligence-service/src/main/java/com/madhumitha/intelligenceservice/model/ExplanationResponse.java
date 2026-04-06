package com.madhumitha.intelligenceservice.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExplanationResponse {
    private String transactionId;
    private String riskLevel;
    private Double riskScore;
    private String explanation;
}
