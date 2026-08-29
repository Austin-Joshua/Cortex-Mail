package com.nexora.dto.response;

import com.nexora.dto.response.EmailResponse;
import com.nexora.dto.response.GmailLabelCountResponse;

import java.util.List;
import java.util.Map;

public class DashboardSummaryResponse {
    private List<EmailResponse> priorityEmails;
    private List<EmailResponse> upcomingDeadlines;
    private List<ActionItemResponse> pendingActions;
    private long unreadCount;
    private Map<String, Long> categoryCounts;
    private Map<String, GmailLabelCountResponse> gmailLabelCounts;
    private List<EmailResponse> todaysMeetings;

    public DashboardSummaryResponse() {}

    public DashboardSummaryResponse(List<EmailResponse> priorityEmails, List<EmailResponse> upcomingDeadlines,
                                    List<ActionItemResponse> pendingActions, long unreadCount,
                                    Map<String, Long> categoryCounts, List<EmailResponse> todaysMeetings) {
        this.priorityEmails = priorityEmails;
        this.upcomingDeadlines = upcomingDeadlines;
        this.pendingActions = pendingActions;
        this.unreadCount = unreadCount;
        this.categoryCounts = categoryCounts;
        this.todaysMeetings = todaysMeetings;
    }

    public List<EmailResponse> getPriorityEmails() {
        return priorityEmails;
    }

    public void setPriorityEmails(List<EmailResponse> priorityEmails) {
        this.priorityEmails = priorityEmails;
    }

    public List<EmailResponse> getUpcomingDeadlines() {
        return upcomingDeadlines;
    }

    public void setUpcomingDeadlines(List<EmailResponse> upcomingDeadlines) {
        this.upcomingDeadlines = upcomingDeadlines;
    }

    public List<ActionItemResponse> getPendingActions() {
        return pendingActions;
    }

    public void setPendingActions(List<ActionItemResponse> pendingActions) {
        this.pendingActions = pendingActions;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
    }

    public Map<String, Long> getCategoryCounts() {
        return categoryCounts;
    }

    public void setCategoryCounts(Map<String, Long> categoryCounts) {
        this.categoryCounts = categoryCounts;
    }

    public Map<String, GmailLabelCountResponse> getGmailLabelCounts() {
        return gmailLabelCounts;
    }

    public void setGmailLabelCounts(Map<String, GmailLabelCountResponse> gmailLabelCounts) {
        this.gmailLabelCounts = gmailLabelCounts;
    }

    public List<EmailResponse> getTodaysMeetings() {
        return todaysMeetings;
    }

    public void setTodaysMeetings(List<EmailResponse> todaysMeetings) {
        this.todaysMeetings = todaysMeetings;
    }

    public static DashboardSummaryResponseBuilder builder() {
        return new DashboardSummaryResponseBuilder();
    }

    public static class ActionItemResponse {
        private Long id;
        private Long emailId;
        private String emailSubject;
        private String senderName;
        private String actionType;
        private String actionDescription;
        private String deadline;
        private Boolean isCompleted;

        public ActionItemResponse() {}

        public ActionItemResponse(Long id, Long emailId, String emailSubject, String senderName,
                                  String actionType, String actionDescription, String deadline, Boolean isCompleted) {
            this.id = id;
            this.emailId = emailId;
            this.emailSubject = emailSubject;
            this.senderName = senderName;
            this.actionType = actionType;
            this.actionDescription = actionDescription;
            this.deadline = deadline;
            this.isCompleted = isCompleted;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getEmailId() {
            return emailId;
        }

        public void setEmailId(Long emailId) {
            this.emailId = emailId;
        }

        public String getEmailSubject() {
            return emailSubject;
        }

        public void setEmailSubject(String emailSubject) {
            this.emailSubject = emailSubject;
        }

        public String getSenderName() {
            return senderName;
        }

        public void setSenderName(String senderName) {
            this.senderName = senderName;
        }

        public String getActionType() {
            return actionType;
        }

        public void setActionType(String actionType) {
            this.actionType = actionType;
        }

        public String getActionDescription() {
            return actionDescription;
        }

        public void setActionDescription(String actionDescription) {
            this.actionDescription = actionDescription;
        }

        public String getDeadline() {
            return deadline;
        }

        public void setDeadline(String deadline) {
            this.deadline = deadline;
        }

        public Boolean getIsCompleted() {
            return isCompleted;
        }

        public void setIsCompleted(Boolean isCompleted) {
            this.isCompleted = isCompleted;
        }

        public static ActionItemResponseBuilder builder() {
            return new ActionItemResponseBuilder();
        }

        public static class ActionItemResponseBuilder {
            private Long id;
            private Long emailId;
            private String emailSubject;
            private String senderName;
            private String actionType;
            private String actionDescription;
            private String deadline;
            private Boolean isCompleted;

            ActionItemResponseBuilder() {}

            public ActionItemResponseBuilder id(Long id) {
                this.id = id;
                return this;
            }

            public ActionItemResponseBuilder emailId(Long emailId) {
                this.emailId = emailId;
                return this;
            }

            public ActionItemResponseBuilder emailSubject(String emailSubject) {
                this.emailSubject = emailSubject;
                return this;
            }

            public ActionItemResponseBuilder senderName(String senderName) {
                this.senderName = senderName;
                return this;
            }

            public ActionItemResponseBuilder actionType(String actionType) {
                this.actionType = actionType;
                return this;
            }

            public ActionItemResponseBuilder actionDescription(String actionDescription) {
                this.actionDescription = actionDescription;
                return this;
            }

            public ActionItemResponseBuilder deadline(String deadline) {
                this.deadline = deadline;
                return this;
            }

            public ActionItemResponseBuilder isCompleted(Boolean isCompleted) {
                this.isCompleted = isCompleted;
                return this;
            }

            public ActionItemResponse build() {
                return new ActionItemResponse(this.id, this.emailId, this.emailSubject, this.senderName,
                        this.actionType, this.actionDescription, this.deadline, this.isCompleted);
            }
        }
    }

    public static class DashboardSummaryResponseBuilder {
        private List<EmailResponse> priorityEmails;
        private List<EmailResponse> upcomingDeadlines;
        private List<ActionItemResponse> pendingActions;
        private long unreadCount;
        private Map<String, Long> categoryCounts;
        private Map<String, GmailLabelCountResponse> gmailLabelCounts;
        private List<EmailResponse> todaysMeetings;

        DashboardSummaryResponseBuilder() {}

        public DashboardSummaryResponseBuilder priorityEmails(List<EmailResponse> priorityEmails) {
            this.priorityEmails = priorityEmails;
            return this;
        }

        public DashboardSummaryResponseBuilder upcomingDeadlines(List<EmailResponse> upcomingDeadlines) {
            this.upcomingDeadlines = upcomingDeadlines;
            return this;
        }

        public DashboardSummaryResponseBuilder pendingActions(List<ActionItemResponse> pendingActions) {
            this.pendingActions = pendingActions;
            return this;
        }

        public DashboardSummaryResponseBuilder unreadCount(long unreadCount) {
            this.unreadCount = unreadCount;
            return this;
        }

        public DashboardSummaryResponseBuilder categoryCounts(Map<String, Long> categoryCounts) {
            this.categoryCounts = categoryCounts;
            return this;
        }

        public DashboardSummaryResponseBuilder gmailLabelCounts(Map<String, GmailLabelCountResponse> gmailLabelCounts) {
            this.gmailLabelCounts = gmailLabelCounts;
            return this;
        }

        public DashboardSummaryResponseBuilder todaysMeetings(List<EmailResponse> todaysMeetings) {
            this.todaysMeetings = todaysMeetings;
            return this;
        }

        public DashboardSummaryResponse build() {
            DashboardSummaryResponse r = new DashboardSummaryResponse();
            r.priorityEmails = this.priorityEmails;
            r.upcomingDeadlines = this.upcomingDeadlines;
            r.pendingActions = this.pendingActions;
            r.unreadCount = this.unreadCount;
            r.categoryCounts = this.categoryCounts;
            r.gmailLabelCounts = this.gmailLabelCounts;
            r.todaysMeetings = this.todaysMeetings;
            return r;
        }
    }
}
