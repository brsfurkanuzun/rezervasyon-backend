package com.randevupazaryeri.image.service;

import com.randevupazaryeri.common.exception.RateLimitExceededException;
import com.randevupazaryeri.config.UploadProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-user sliding-window upload limit, held in memory (per instance).
 * TODO: Add upload rate limiting before production scale — move this to a shared store
 * (e.g. Redis) once the API runs on more than one instance.
 */
@Component
public class UploadRateLimiter {

    private final UploadProperties properties;
    private final Clock clock;
    private final Map<UUID, Deque<Long>> hits = new ConcurrentHashMap<>();

    @Autowired
    public UploadRateLimiter(UploadProperties properties) {
        this(properties, Clock.systemUTC());
    }

    UploadRateLimiter(UploadProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public void acquire(UUID userId) {
        int max = properties.getRateLimit().getMaxUploads();
        if (max <= 0) {
            return;
        }
        long now = clock.millis();
        long windowStart = now - properties.getRateLimit().getWindow().toMillis();
        boolean[] allowed = {false};
        hits.compute(userId, (id, times) -> {
            Deque<Long> queue = times == null ? new ArrayDeque<>() : times;
            while (!queue.isEmpty() && queue.peekFirst() <= windowStart) {
                queue.pollFirst();
            }
            if (queue.size() < max) {
                queue.addLast(now);
                allowed[0] = true;
            }
            return queue;
        });
        if (!allowed[0]) {
            throw new RateLimitExceededException("Çok fazla fotoğraf yüklediniz. Lütfen biraz sonra tekrar deneyin.");
        }
    }
}
