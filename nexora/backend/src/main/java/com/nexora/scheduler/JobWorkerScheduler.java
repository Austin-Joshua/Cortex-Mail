package com.nexora.scheduler;

import com.nexora.model.BackgroundJob;
import com.nexora.model.Email;
import com.nexora.model.User;
import com.nexora.repository.EmailRepository;
import com.nexora.repository.UserRepository;
import com.nexora.service.BackgroundJobService;
import com.nexora.service.CalendarService;
import com.nexora.service.NotificationService;
import com.nexora.service.PostSyncProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobWorkerScheduler {

    private final BackgroundJobService backgroundJobService;
    private final PostSyncProcessingService postSyncProcessingService;
    private final CalendarService calendarService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final EmailRepository emailRepository;

    @Scheduled(fixedDelay = 5000)
    public void processDueJobs() {
        List<BackgroundJob> jobs = backgroundJobService.claimDueJobs(5);
        for (BackgroundJob job : jobs) {
            try {
                handle(job);
                backgroundJobService.markDone(job.getId());
            } catch (Exception e) {
                log.warn("Job {} ({}) failed: {}", job.getId(), job.getJobType(), e.getMessage());
                backgroundJobService.markFail(job.getId(), e.getMessage());
            }
        }
    }

    private void handle(BackgroundJob job) {
        Long userId = job.getUserId();
        String type = job.getJobType();
        if (type == null) {
            throw new IllegalStateException("Missing job type");
        }
        switch (type) {
            case BackgroundJob.Type.INCREMENTAL_SYNC -> {
                if (userId != null) {
                    postSyncProcessingService.syncAndProcessBlocking(userId);
                }
            }
            case BackgroundJob.Type.CLASSIFY_BATCH -> {
                if (userId != null) {
                    boolean force = "force".equalsIgnoreCase(
                            job.getPayload() != null ? job.getPayload().trim() : "");
                    postSyncProcessingService.classifyAndRefineBlocking(userId, force);
                }
            }
            case BackgroundJob.Type.CREATE_CALENDAR_EVENT -> {
                if (userId == null || job.getPayload() == null) {
                    return;
                }
                Long emailId = Long.parseLong(job.getPayload().trim());
                User user = userRepository.findById(userId).orElse(null);
                Email email = emailRepository.findOwnedByIdAndUserId(emailId, userId).orElse(null);
                if (user != null && email != null) {
                    calendarService.createDeadlineEvent(user, email);
                }
            }
            case BackgroundJob.Type.GENERATE_DIGEST -> {
                if (userId != null) {
                    notificationService.generateDailyNotifications(userId);
                }
            }
            default -> log.warn("Unknown job type {} for job {}", type, job.getId());
        }
    }
}
