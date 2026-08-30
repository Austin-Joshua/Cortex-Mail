package com.nexora.dto.request;

public class TemplateRequest {
    private String name;
    private String subject;
    private String body;
    private String htmlBody;
    private String category;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getHtmlBody() { return htmlBody; }
    public void setHtmlBody(String htmlBody) { this.htmlBody = htmlBody; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
