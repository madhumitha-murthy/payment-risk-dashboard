package com.madhumitha.intelligenceservice.model;

import lombok.Data;

import java.util.List;

/**
 * OpenAI-compatible response format returned by Groq API.
 */
@Data
public class LlmResponse {

    private List<Choice> choices;

    @Data
    public static class Choice {
        private Message message;
    }

    @Data
    public static class Message {
        private String role;
        private String content;
    }

    public String extractText() {
        if (choices == null || choices.isEmpty()) return "";
        Message msg = choices.get(0).getMessage();
        return msg != null ? msg.getContent() : "";
    }
}
