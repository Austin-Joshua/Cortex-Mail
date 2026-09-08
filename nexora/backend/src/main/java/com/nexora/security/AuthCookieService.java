package com.nexora.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AuthCookieService {

    @Value("${jwt.expiration-ms:900000}")
    private long accessExpirationMs;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    public void setAuthCookies(HttpServletRequest request, HttpServletResponse response,
                               String accessToken, String refreshToken) {
        boolean secure = isSecureRequest(request);
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(
                CookieNames.ACCESS_TOKEN, accessToken, accessExpirationMs, secure).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(
                CookieNames.REFRESH_TOKEN, refreshToken, refreshExpirationMs, secure).toString());
    }

    public void clearAuthCookies(HttpServletRequest request, HttpServletResponse response) {
        boolean secure = isSecureRequest(request);
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(
                CookieNames.ACCESS_TOKEN, "", 0, secure).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(
                CookieNames.REFRESH_TOKEN, "", 0, secure).toString());
    }

    public String readCookie(HttpServletRequest request, String name) {
        if (request == null || request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private ResponseCookie buildCookie(String name, String value, long maxAgeMs, boolean secure) {
        // Cross-site SPA (Vercel) → API (Render) requires SameSite=None + Secure.
        // Localhost stays Lax so cookies work on http://localhost.
        String sameSite = secure ? "None" : "Lax";
        return ResponseCookie.from(name, value != null ? value : "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite(sameSite)
                .maxAge(maxAgeMs <= 0 ? Duration.ZERO : Duration.ofMillis(maxAgeMs))
                .build();
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        if (request == null) {
            return true;
        }
        String host = request.getServerName();
        if (host == null) {
            return true;
        }
        String lower = host.toLowerCase();
        if ("localhost".equals(lower) || "127.0.0.1".equals(lower) || "[::1]".equals(lower)) {
            return false;
        }
        return request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }
}
