package com.nexora.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_sync_locks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSyncLock {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "lock_owner")
    private String lockOwner;
}
