package com.nexora.dto.response;

public class GmailLabelCountResponse {
    private String id;
    private String name;
    private String type;
    private Long messagesTotal;
    private Long messagesUnread;
    private Long threadsTotal;
    private Long threadsUnread;

    public GmailLabelCountResponse() {}

    public GmailLabelCountResponse(String id, String name, String type,
                                   Long messagesTotal, Long messagesUnread,
                                   Long threadsTotal, Long threadsUnread) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.messagesTotal = messagesTotal;
        this.messagesUnread = messagesUnread;
        this.threadsTotal = threadsTotal;
        this.threadsUnread = threadsUnread;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getMessagesTotal() { return messagesTotal; }
    public void setMessagesTotal(Long messagesTotal) { this.messagesTotal = messagesTotal; }

    public Long getMessagesUnread() { return messagesUnread; }
    public void setMessagesUnread(Long messagesUnread) { this.messagesUnread = messagesUnread; }

    public Long getThreadsTotal() { return threadsTotal; }
    public void setThreadsTotal(Long threadsTotal) { this.threadsTotal = threadsTotal; }

    public Long getThreadsUnread() { return threadsUnread; }
    public void setThreadsUnread(Long threadsUnread) { this.threadsUnread = threadsUnread; }
}
