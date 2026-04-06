package com.madhumitha.intelligenceservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madhumitha.intelligenceservice.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * IntelligenceService bridges the payment platform to the Groq LLM API (free tier).
 *
 * Two capabilities:
 *   1. explainTransaction — fetches a flagged transaction from transaction-service,
 *      sends its details to Llama 3.1 via Groq, returns a plain-English explanation
 *      of why it was flagged.
 *
 *   2. queryTransactions — takes a natural language query ("show me refunds over SGD 5000"),
 *      asks the LLM to extract structured filters (status, riskLevel, type, amountMin, amountMax),
 *      then calls the transaction-service search endpoint with those filters.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntelligenceService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${groq.api.url}")
    private String groqApiUrl;

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.model}")
    private String groqModel;

    @Value("${transaction.service.url}")
    private String transactionServiceUrl;

    // -------------------------------------------------------------------------
    // 1. Transaction Explainer
    // -------------------------------------------------------------------------

    public ExplanationResponse explainTransaction(String transactionId) {
        TransactionData transaction = fetchTransaction(transactionId);

        String prompt = buildExplanationPrompt(transaction);
        String explanation = callLlm(prompt, 250);

        log.info("Explanation generated for transaction {}: riskLevel={}", transactionId, transaction.getRiskLevel());

        return ExplanationResponse.builder()
                .transactionId(transactionId)
                .riskLevel(transaction.getRiskLevel())
                .riskScore(transaction.getRiskScore())
                .explanation(explanation)
                .build();
    }

    private String buildExplanationPrompt(TransactionData tx) {
        return String.format(
            "You are a payment risk analyst. A transaction has been flagged by an ML-powered risk system. " +
            "Explain in 2-3 clear sentences why this transaction was flagged, based on the following data:\n\n" +
            "- Amount: %s %.2f\n" +
            "- Transaction Type: %s\n" +
            "- Risk Score: %.3f (0.0 = no risk, 1.0 = maximum risk)\n" +
            "- Risk Level: %s\n" +
            "- Assessment Source: %s\n" +
            "- Status: %s\n\n" +
            "Be specific about which factors contributed to the risk classification. " +
            "Write as a single paragraph, no bullet points.",
            tx.getCurrency() != null ? tx.getCurrency() : "SGD",
            tx.getAmount(),
            tx.getType(),
            tx.getRiskScore() != null ? tx.getRiskScore() : 0.0,
            tx.getRiskLevel(),
            tx.getAssessmentSource() != null ? tx.getAssessmentSource() : "RULE_BASED",
            tx.getStatus()
        );
    }

    // -------------------------------------------------------------------------
    // 2. Natural Language Query
    // -------------------------------------------------------------------------

    public QueryResponse queryTransactions(String naturalLanguageQuery) {
        String filterPrompt = buildFilterExtractionPrompt(naturalLanguageQuery);
        String filtersJson = callLlm(filterPrompt, 100);

        log.info("LLM parsed NL query '{}' to filters: {}", naturalLanguageQuery, filtersJson);

        Map<String, String> filters = parseFiltersFromJson(filtersJson);
        List<TransactionData> results = searchTransactions(filters);

        return QueryResponse.builder()
                .naturalLanguageQuery(naturalLanguageQuery)
                .parsedFilters(filtersJson.trim())
                .results(results)
                .totalFound(results.size())
                .build();
    }

    private String buildFilterExtractionPrompt(String query) {
        return String.format(
            "Extract search filters from this natural language query about payment transactions. " +
            "Return ONLY a valid JSON object with these optional fields (include only fields mentioned in the query):\n" +
            "- \"status\": one of COMPLETED, FLAGGED, PENDING, REJECTED\n" +
            "- \"riskLevel\": one of HIGH, MEDIUM, LOW\n" +
            "- \"type\": one of TRANSFER, PAYMENT, REFUND\n" +
            "- \"amountMin\": number\n" +
            "- \"amountMax\": number\n\n" +
            "Query: \"%s\"\n\n" +
            "Respond with ONLY the JSON object, no explanation. Example: {\"status\":\"FLAGGED\",\"riskLevel\":\"HIGH\"}",
            query
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseFiltersFromJson(String json) {
        try {
            String cleaned = json.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("```[a-z]*\\n?", "").replace("```", "").trim();
            }
            return objectMapper.readValue(cleaned, Map.class);
        } catch (Exception e) {
            log.warn("Could not parse LLM filter response '{}', using empty filters: {}", json, e.getMessage());
            return Map.of();
        }
    }

    private List<TransactionData> searchTransactions(Map<String, String> filters) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(transactionServiceUrl + "/api/transactions/search");

        filters.forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                builder.queryParam(key, value);
            }
        });

        String url = builder.toUriString();
        log.info("Calling transaction-service search: {}", url);

        try {
            ResponseEntity<TransactionData[]> response = restTemplate.getForEntity(url, TransactionData[].class);
            return response.getBody() != null ? Arrays.asList(response.getBody()) : List.of();
        } catch (Exception e) {
            log.error("Failed to call transaction search endpoint: {}", e.getMessage());
            return List.of();
        }
    }

    // -------------------------------------------------------------------------
    // Shared helpers
    // -------------------------------------------------------------------------

    private TransactionData fetchTransaction(String transactionId) {
        String url = transactionServiceUrl + "/api/transactions/" + transactionId;
        TransactionData tx = restTemplate.getForObject(url, TransactionData.class);
        if (tx == null) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }
        return tx;
    }

    String callLlm(String prompt, int maxTokens) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        LlmRequest request = LlmRequest.builder()
                .model(groqModel)
                .maxTokens(maxTokens)
                .temperature(0.3)
                .messages(List.of(
                        LlmRequest.Message.builder()
                                .role("user")
                                .content(prompt)
                                .build()
                ))
                .build();

        HttpEntity<LlmRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<LlmResponse> response = restTemplate.exchange(
                    groqApiUrl, HttpMethod.POST, entity, LlmResponse.class);

            if (response.getBody() != null) {
                return response.getBody().extractText();
            }
        } catch (Exception e) {
            log.error("Groq API call failed: {}", e.getMessage());
            throw new RuntimeException("AI service temporarily unavailable. Please try again.", e);
        }
        return "";
    }
}
