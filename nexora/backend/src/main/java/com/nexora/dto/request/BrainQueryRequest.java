package com.nexora.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class BrainQueryRequest {
    @NotBlank(message = "Query cannot be empty")
    @Size(max = 2000, message = "Query is too long")
    private String query;

    public BrainQueryRequest() {}

    public BrainQueryRequest(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
