package com.nexora.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "oauth_exchange_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OauthExchangeCode {

    @Id
    @Column(length = 64)
    private String code;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
