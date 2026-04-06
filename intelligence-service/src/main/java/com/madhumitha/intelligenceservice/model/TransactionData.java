package com.madhumitha.intelligenceservice.model;

import lombok.Data;

/**
 * Read-only view of a transaction fetched from the transaction-service API.
 * Intelligence service never writes transactions — it only reads them for analysis.
 */
@Data
public class TransactionData {
    private String id;
    private String senderId;
    private String receiverId;
    private Double amount;
    private String currency;
    private String type;
    private String status;
    private String riskLevel;
    private Double riskScore;
    private String assessmentSource;
    private String description;
    private String createdAt;
}
