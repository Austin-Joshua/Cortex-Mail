package com.nexora.dto.response;

import java.time.LocalDateTime;

public class BrainConversationResponse {
    private Long id;
    private String userQuery;
    private String aiResponse;
    private String referencedEmailIds;
    private LocalDateTime createdAt;

    public BrainConversationResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserQuery() { return userQuery; }
    public void setUserQuery(String userQuery) { this.userQuery = userQuery; }
    public String getAiResponse() { return aiResponse; }
    public void setAiResponse(String aiResponse) { this.aiResponse = aiResponse; }
    public String getReferencedEmailIds() { return referencedEmailIds; }
    public void setReferencedEmailIds(String referencedEmailIds) { this.referencedEmailIds = referencedEmailIds; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
