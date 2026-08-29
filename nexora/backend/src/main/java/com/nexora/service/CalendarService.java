package com.nexora.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import com.nexora.model.Email;
import com.nexora.model.User;
import com.nexora.repository.EmailRepository;
import com.nexora.security.TokenEncryptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarService {

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    private final TokenEncryptor tokenEncryptor;
    private final EmailRepository emailRepository;

    private static final String APPLICATION_NAME = "Cortex Mail";
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Async
    public void createDeadlineEvent(User user, Email email) {
        if (email.getDeadlineDetected() == null) return;
        if (Boolean.TRUE.equals(email.getIsDeadlineAddedToCalendar())) return;
        if (Boolean.FALSE.equals(user.getCalendarSyncEnabled())) return;

        try {
            if (user.getGmailAccessToken() == null) {
                log.warn("Cannot add event to calendar: user {} has no access token", user.getId());
                return;
            }

            String accessToken = tokenEncryptor.decrypt(user.getGmailAccessToken());
            String refreshToken = tokenEncryptor.decrypt(user.getGmailRefreshToken());
            Date expiry = user.getTokenExpiry() != null
                    ? Date.from(user.getTokenExpiry().atZone(ZoneId.systemDefault()).toInstant())
                    : new Date();

            AccessToken token = new AccessToken(accessToken, expiry);
            UserCredentials credentials = UserCredentials.newBuilder()
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setAccessToken(token)
                    .setRefreshToken(refreshToken)
                    .build();

            Calendar calendar = new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JSON_FACTORY,
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            Event event = new Event();
            event.setSummary(email.getSubject() != null ? email.getSubject() : "Cortex Mail reminder");
            event.setDescription("Reminder from Cortex Mail based on the date/time found in email from " +
                    (email.getSenderName() != null ? email.getSenderName() : email.getSenderEmail()));

            LocalDateTime deadline = email.getDeadlineDetected();
            EventDateTime start = new EventDateTime();
            EventDateTime end = new EventDateTime();

            if (deadline.getHour() == 0 && deadline.getMinute() == 0 && deadline.getSecond() == 0) {
                String dateStr = deadline.format(DateTimeFormatter.ISO_LOCAL_DATE);
                start.setDate(new com.google.api.client.util.DateTime(dateStr));
                end.setDate(new com.google.api.client.util.DateTime(dateStr));
            } else {
                String dateTimeStr = deadline.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                start.setDateTime(new com.google.api.client.util.DateTime(dateTimeStr));
                String endDateTimeStr = deadline.plusHours(1).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                end.setDateTime(new com.google.api.client.util.DateTime(endDateTimeStr));
            }

            event.setStart(start);
            event.setEnd(end);
            // Remind 30 minutes before when a specific time is known
            if (!(deadline.getHour() == 0 && deadline.getMinute() == 0)) {
                Event.Reminders reminders = new Event.Reminders();
                reminders.setUseDefault(false);
                com.google.api.services.calendar.model.EventReminder popup =
                        new com.google.api.services.calendar.model.EventReminder()
                                .setMethod("popup")
                                .setMinutes(30);
                reminders.setOverrides(java.util.List.of(popup));
                event.setReminders(reminders);
            }

            calendar.events().insert("primary", event).execute();

            email.setIsDeadlineAddedToCalendar(true);
            emailRepository.save(email);

            log.info("Successfully added email {} deadline to user {} calendar", email.getId(), user.getId());

        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
            log.warn("Google Calendar API returned error for user {}, skipping: {}", user.getId(), e.getDetails());
        } catch (GeneralSecurityException | IOException e) {
            log.error("Failed to insert Calendar event for user {}: {}", user.getId(), e.getMessage());
        }
    }
}
