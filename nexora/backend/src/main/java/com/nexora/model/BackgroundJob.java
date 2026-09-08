package com.nexora.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "background_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackgroundJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "job_type", nullable = false)
    private String jobType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    @Builder.Default
    private String status = Status.PENDING;

    @Builder.Default
    private Integer attempts = 0;

    @Column(name = "run_at")
    private LocalDateTime runAt;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (runAt == null) {
            runAt = LocalDateTime.now();
        }
        if (status == null) {
            status = Status.PENDING;
        }
        if (attempts == null) {
            attempts = 0;
        }
    }

    public static final class Status {
        public static final String PENDING = "PENDING";
        public static final String RUNNING = "RUNNING";
        public static final String DONE = "DONE";
        public static final String FAILED = "FAILED";

        private Status() {}
    }

    public static final class Type {
        public static final String CLASSIFY_BATCH = "CLASSIFY_BATCH";
        public static final String INCREMENTAL_SYNC = "INCREMENTAL_SYNC";
        public static final String CREATE_CALENDAR_EVENT = "CREATE_CALENDAR_EVENT";
        public static final String GENERATE_DIGEST = "GENERATE_DIGEST";

        private Type() {}
    }
}
