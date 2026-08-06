package dev.brunovasconcellos.vbank.service;

import dev.brunovasconcellos.vbank.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;

@Service
public class RateLimitService {
    private final JdbcTemplate jdbcTemplate;

    public RateLimitService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void consume(String scope, String subject, int maximum, Duration window) {
        String key = scope + ":" + sha256(subject == null ? "unknown" : subject);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime resetBefore = now.minus(window);
        Integer count = jdbcTemplate.queryForObject("""
                INSERT INTO rate_limit_bucket(bucket_key, window_started_at, request_count)
                VALUES (?, ?, 1)
                ON CONFLICT (bucket_key) DO UPDATE SET
                  window_started_at = CASE WHEN rate_limit_bucket.window_started_at < ? THEN EXCLUDED.window_started_at ELSE rate_limit_bucket.window_started_at END,
                  request_count = CASE WHEN rate_limit_bucket.window_started_at < ? THEN 1 ELSE rate_limit_bucket.request_count + 1 END
                RETURNING request_count
                """, Integer.class, key, now, resetBefore, resetBefore);
        if (count != null && count > maximum) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "SERVICE_UNAVAILABLE", "Muitas tentativas. Aguarde alguns minutos e tente novamente.");
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }
}
