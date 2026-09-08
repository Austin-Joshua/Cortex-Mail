package com.nexora.dto.request;

import com.nexora.model.User.UserRole;

public class ProfileUpdateRequest {
    private UserRole userRole;
    private Boolean calendarSyncEnabled;
    private Integer quietHoursStart;
    private Integer quietHoursEnd;
    private String mutedCategories;
    private Boolean digestEnabled;
    private Integer digestHour;

    public ProfileUpdateRequest() {}

    public ProfileUpdateRequest(UserRole userRole, Boolean calendarSyncEnabled) {
        this.userRole = userRole;
        this.calendarSyncEnabled = calendarSyncEnabled;
    }

    public UserRole getUserRole() {
        return userRole;
    }

    public void setUserRole(UserRole userRole) {
        this.userRole = userRole;
    }

    public Boolean getCalendarSyncEnabled() {
        return calendarSyncEnabled;
    }

    public void setCalendarSyncEnabled(Boolean calendarSyncEnabled) {
        this.calendarSyncEnabled = calendarSyncEnabled;
    }

    public Integer getQuietHoursStart() { return quietHoursStart; }
    public void setQuietHoursStart(Integer quietHoursStart) { this.quietHoursStart = quietHoursStart; }

    public Integer getQuietHoursEnd() { return quietHoursEnd; }
    public void setQuietHoursEnd(Integer quietHoursEnd) { this.quietHoursEnd = quietHoursEnd; }

    public String getMutedCategories() { return mutedCategories; }
    public void setMutedCategories(String mutedCategories) { this.mutedCategories = mutedCategories; }

    public Boolean getDigestEnabled() { return digestEnabled; }
    public void setDigestEnabled(Boolean digestEnabled) { this.digestEnabled = digestEnabled; }

    public Integer getDigestHour() { return digestHour; }
    public void setDigestHour(Integer digestHour) { this.digestHour = digestHour; }
}
