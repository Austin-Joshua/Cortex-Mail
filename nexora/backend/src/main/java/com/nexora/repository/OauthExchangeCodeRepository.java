package com.nexora.repository;

import com.nexora.model.OauthExchangeCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface OauthExchangeCodeRepository extends JpaRepository<OauthExchangeCode, String> {

    @Modifying
    @Query("DELETE FROM OauthExchangeCode o WHERE o.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
