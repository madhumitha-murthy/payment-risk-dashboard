package com.madhumitha.intelligenceservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madhumitha.intelligenceservice.model.ExplanationResponse;
import com.madhumitha.intelligenceservice.model.QueryResponse;
import com.madhumitha.intelligenceservice.model.TransactionData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntelligenceServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private IntelligenceService intelligenceService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(intelligenceService, "groqApiUrl", "https://api.groq.com/openai/v1/chat/completions");
        ReflectionTestUtils.setField(intelligenceService, "groqApiKey", "test-key");
        ReflectionTestUtils.setField(intelligenceService, "groqModel", "llama-3.1-8b-instant");
        ReflectionTestUtils.setField(intelligenceService, "transactionServiceUrl", "http://localhost:8082");
    }

    @Test
    void explainTransaction_returnsExplanationWithTransactionDetails() {
        TransactionData tx = new TransactionData();
        tx.setId("tx123");
        tx.setAmount(15000.0);
        tx.setCurrency("SGD");
        tx.setType("TRANSFER");
        tx.setRiskScore(0.85);
        tx.setRiskLevel("HIGH");
        tx.setStatus("FLAGGED");
        tx.setAssessmentSource("ML_MODEL");

        when(restTemplate.getForObject(contains("/api/transactions/tx123"), eq(TransactionData.class)))
                .thenReturn(tx);

        IntelligenceService spyService = spy(intelligenceService);
        doReturn("This transaction was flagged due to high amount and ML anomaly score.")
                .when(spyService).callLlm(anyString(), anyInt());

        ExplanationResponse response = spyService.explainTransaction("tx123");

        assertThat(response.getTransactionId()).isEqualTo("tx123");
        assertThat(response.getRiskLevel()).isEqualTo("HIGH");
        assertThat(response.getRiskScore()).isEqualTo(0.85);
        assertThat(response.getExplanation()).isNotBlank();
    }

    @Test
    void explainTransaction_throwsWhenTransactionNotFound() {
        when(restTemplate.getForObject(anyString(), eq(TransactionData.class))).thenReturn(null);

        assertThatThrownBy(() -> intelligenceService.explainTransaction("nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Transaction not found");
    }

    @Test
    void queryTransactions_parsesFiltersAndReturnsResults() {
        TransactionData tx = new TransactionData();
        tx.setId("tx456");
        tx.setStatus("FLAGGED");
        tx.setRiskLevel("HIGH");

        IntelligenceService spyService = spy(intelligenceService);
        doReturn("{\"status\":\"FLAGGED\",\"riskLevel\":\"HIGH\"}")
                .when(spyService).callLlm(anyString(), anyInt());

        when(restTemplate.getForEntity(anyString(), eq(TransactionData[].class)))
                .thenReturn(ResponseEntity.ok(new TransactionData[]{tx}));

        QueryResponse response = spyService.queryTransactions("show me all flagged high risk transactions");

        assertThat(response.getNaturalLanguageQuery()).isEqualTo("show me all flagged high risk transactions");
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getTotalFound()).isEqualTo(1);
    }

    @Test
    void queryTransactions_handlesInvalidLlmResponseGracefully() {
        IntelligenceService spyService = spy(intelligenceService);
        doReturn("I cannot parse this query")
                .when(spyService).callLlm(anyString(), anyInt());

        when(restTemplate.getForEntity(anyString(), eq(TransactionData[].class)))
                .thenReturn(ResponseEntity.ok(new TransactionData[]{}));

        QueryResponse response = spyService.queryTransactions("some ambiguous query");

        assertThat(response.getResults()).isEmpty();
        assertThat(response.getTotalFound()).isEqualTo(0);
    }

    @Test
    void queryTransactions_handlesTransactionServiceDownGracefully() {
        IntelligenceService spyService = spy(intelligenceService);
        doReturn("{\"status\":\"FLAGGED\"}")
                .when(spyService).callLlm(anyString(), anyInt());

        when(restTemplate.getForEntity(anyString(), eq(TransactionData[].class)))
                .thenThrow(new RuntimeException("Connection refused"));

        QueryResponse response = spyService.queryTransactions("show flagged transactions");

        assertThat(response.getResults()).isEmpty();
        assertThat(response.getTotalFound()).isEqualTo(0);
    }
}
