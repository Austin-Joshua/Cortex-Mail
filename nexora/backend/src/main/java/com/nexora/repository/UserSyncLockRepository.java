package com.nexora.repository;

import com.nexora.model.UserSyncLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSyncLockRepository extends JpaRepository<UserSyncLock, Long> {
}
