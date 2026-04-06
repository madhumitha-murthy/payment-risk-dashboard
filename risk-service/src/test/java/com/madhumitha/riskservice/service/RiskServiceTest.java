package com.madhumitha.riskservice.service;

import com.madhumitha.riskservice.model.RiskAssessment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private RiskService riskService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(riskService, "pythonMlApiUrl", "http://localhost:8000");
        // Python ML API is unavailable in tests — service falls back to rule-based scoring
        when(restTemplate.postForObject(any(), any(), any())).thenReturn(null);
        when(mongoTemplate.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void amountOver10000ShouldReturnHighRiskLevel() {
        RiskAssessment result = riskService.assessRisk("user_001", 15000.0, "TRANSFER");

        assertThat(result.getRiskLevel()).isEqualTo("HIGH");
        assertThat(result.getRiskScore()).isGreaterThan(0.7);
    }

    @Test
    void amountUnder1000ShouldReturnLowRiskLevel() {
        RiskAssessment result = riskService.assessRisk("user_001", 500.0, "PAYMENT");

        assertThat(result.getRiskLevel()).isEqualTo("LOW");
        assertThat(result.getRiskScore()).isLessThan(0.4);
    }

    @Test
    void refundTypeShouldIncreaseRiskScore() {
        RiskAssessment transfer = riskService.assessRisk("user_001", 500.0, "TRANSFER");
        RiskAssessment refund   = riskService.assessRisk("user_001", 500.0, "REFUND");

        assertThat(refund.getRiskScore()).isGreaterThan(transfer.getRiskScore());
    }

    @Test
    void mlApiUnavailableShouldFallbackToRuleBasedAndStillReturnResult() {
        // restTemplate returns null (ML API down) — set up in @BeforeEach
        RiskAssessment result = riskService.assessRisk("user_001", 12000.0, "TRANSFER");

        assertThat(result).isNotNull();
        assertThat(result.getAssessmentSource()).isEqualTo("RULE_BASED");
        assertThat(result.getRiskLevel()).isNotBlank();
    }

    @Test
    void riskScoreShouldNeverExceedOne() {
        // Extremely high amount + REFUND type — worst case scenario
        RiskAssessment result = riskService.assessRisk("user_001", 999999.0, "REFUND");

        assertThat(result.getRiskScore()).isLessThanOrEqualTo(1.0);
    }
}
