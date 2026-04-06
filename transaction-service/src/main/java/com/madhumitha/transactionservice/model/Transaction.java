package com.madhumitha.transactionservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "transactions")
public class Transaction {

    @Id
    private String id;

    @NotBlank(message = "Sender ID is required")
    @Indexed
    private String senderId;

    @NotBlank(message = "Receiver ID is required")
    private String receiverId;

    @Positive(message = "Amount must be positive")
    private Double amount;

    @NotBlank(message = "Currency is required")
    @Builder.Default
    private String currency = "SGD";

    @NotBlank(message = "Transaction type is required")
    private String type; // TRANSFER, PAYMENT, REFUND

    @Builder.Default
    private String status = "PENDING"; // PENDING, COMPLETED, FLAGGED, REJECTED

    private String riskLevel; // LOW, MEDIUM, HIGH
    private Double riskScore;

    private String description;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime processedAt;
}
