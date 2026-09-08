package com.nexora.dto.response;

import com.nexora.model.User.UserRole;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String tokenType;
    private Long userId;
    private String email;
    private String name;
    private String profilePictureUrl;
    private UserRole userRole;
    private boolean onboardingComplete;
    private Boolean calendarSyncEnabled;
    private LocalDateTime lastSyncedAt;
    private Integer quietHoursStart;
    private Integer quietHoursEnd;
    private String mutedCategories;
    private Boolean digestEnabled;
    private Integer digestHour;
}
