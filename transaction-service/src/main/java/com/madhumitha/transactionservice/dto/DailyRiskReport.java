package com.madhumitha.transactionservice.dto;

import com.madhumitha.transactionservice.model.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class DailyRiskReport {
    private RiskLevel riskLevel;
    private LocalDate date;
    private Long transactionCount;
    private BigDecimal totalAmount;
}
