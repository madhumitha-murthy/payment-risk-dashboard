package com.madhumitha.intelligenceservice.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QueryResponse {
    private String naturalLanguageQuery;
    private String parsedFilters;
    private List<TransactionData> results;
    private int totalFound;
}
