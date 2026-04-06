package com.madhumitha.intelligenceservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * OpenAI-compatible request format used by Groq API.
 */
@Data
@Builder
public class LlmRequest {

    private String model;

    @JsonProperty("max_tokens")
    private int maxTokens;

    private double temperature;

    private List<Message> messages;

    @Data
    @Builder
    public static class Message {
        private String role;
        private String content;
    }
}
