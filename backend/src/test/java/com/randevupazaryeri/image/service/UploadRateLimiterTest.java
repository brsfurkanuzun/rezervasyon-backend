package com.randevupazaryeri.image.service;

import com.randevupazaryeri.common.exception.RateLimitExceededException;
import com.randevupazaryeri.config.UploadProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadRateLimiterTest {

    @Test
    void limitsUploadsPerUserWithinTheWindow() {
        UploadProperties properties = new UploadProperties();
        properties.getRateLimit().setMaxUploads(2);
        properties.getRateLimit().setWindow(Duration.ofMinutes(1));
        MutableClock clock = new MutableClock();
        UploadRateLimiter limiter = new UploadRateLimiter(properties, clock);
        UUID user = UUID.randomUUID();

        limiter.acquire(user);
        limiter.acquire(user);
        assertThatThrownBy(() -> limiter.acquire(user)).isInstanceOf(RateLimitExceededException.class);
        assertThatCode(() -> limiter.acquire(UUID.randomUUID())).doesNotThrowAnyException();

        clock.now = clock.now.plusSeconds(61);
        assertThatCode(() -> limiter.acquire(user)).doesNotThrowAnyException();
    }

    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
