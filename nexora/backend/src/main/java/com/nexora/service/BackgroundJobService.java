package com.nexora.service;

import com.nexora.model.BackgroundJob;
import com.nexora.repository.BackgroundJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BackgroundJobService {

    private final BackgroundJobRepository jobRepository;

    @Transactional
    public BackgroundJob enqueue(Long userId, String jobType, String payload) {
        return enqueue(userId, jobType, payload, LocalDateTime.now());
    }

    @Transactional
    public BackgroundJob enqueue(Long userId, String jobType, String payload, LocalDateTime runAt) {
        BackgroundJob job = BackgroundJob.builder()
                .userId(userId)
                .jobType(jobType)
                .payload(payload)
                .status(BackgroundJob.Status.PENDING)
                .attempts(0)
                .runAt(runAt != null ? runAt : LocalDateTime.now())
                .build();
        return jobRepository.save(job);
    }

    @Transactional
    public List<BackgroundJob> claimDueJobs(int limit) {
        LocalDateTime now = LocalDateTime.now();
        List<BackgroundJob> due = jobRepository.findDueJobs(
                BackgroundJob.Status.PENDING, now, PageRequest.of(0, Math.max(1, limit)));
        List<BackgroundJob> claimed = new ArrayList<>();
        for (BackgroundJob job : due) {
            int updated = jobRepository.tryClaim(
                    job.getId(),
                    BackgroundJob.Status.PENDING,
                    BackgroundJob.Status.RUNNING,
                    now);
            if (updated == 1) {
                jobRepository.findById(job.getId()).ifPresent(claimed::add);
            }
        }
        return claimed;
    }

    @Transactional
    public void markDone(Long jobId) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(BackgroundJob.Status.DONE);
            job.setLockedAt(null);
            job.setLastError(null);
            jobRepository.save(job);
        });
    }

    @Transactional
    public void markFail(Long jobId, String error) {
        jobRepository.findById(jobId).ifPresent(job -> {
            int attempts = job.getAttempts() != null ? job.getAttempts() : 0;
            if (attempts >= 5) {
                job.setStatus(BackgroundJob.Status.FAILED);
                job.setLockedAt(null);
            } else {
                job.setStatus(BackgroundJob.Status.PENDING);
                job.setLockedAt(null);
                job.setRunAt(LocalDateTime.now().plusMinutes(Math.min(30, attempts * 2L)));
            }
            job.setLastError(error != null && error.length() > 2000 ? error.substring(0, 2000) : error);
            jobRepository.save(job);
        });
    }
}
