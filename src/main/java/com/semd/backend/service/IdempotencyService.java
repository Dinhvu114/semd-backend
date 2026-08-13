package com.semd.backend.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.function.Supplier;
import com.semd.backend.exception.IdempotencyConflictException;

@Service
public class IdempotencyService {

    private static final String PREFIX = "idempotency:";
    private static final Duration PROCESSING_TTL =
        Duration.ofMinutes(3);

    private static final Duration COMPLETED_TTL =
            Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    public IdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryAcquire(String key) {
        if (key == null || key.isBlank()) {
            return true;
        }

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(
                        PREFIX + key,
                        "PROCESSING",
                        PROCESSING_TTL);

        return Boolean.TRUE.equals(success);
    }

    public void markCompleted(String key) {
        if (key != null && !key.isBlank()) {
            redisTemplate.opsForValue()
                    .set(
                            PREFIX + key,
                            "COMPLETED",
                            COMPLETED_TTL);
        }
    }

    public void release(String key) {
        if (key != null && !key.isBlank()) {
            redisTemplate.delete(PREFIX + key);
        }
    }

    public String getStatus(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }

        return redisTemplate.opsForValue()
                .get(PREFIX + key);
    }

    public <T> T execute(
            String operation,
            Integer actorId,
            String clientKey,
            Supplier<T> action) {
        if (clientKey == null || clientKey.isBlank()) {
            return action.get();
        }

        String scopedKey = operation + ":" + actorId + ":" + clientKey;

        boolean acquired = tryAcquire(scopedKey);

        if (!acquired) {
            String status = getStatus(scopedKey);

            throw new IdempotencyConflictException(
                    "COMPLETED".equals(status)
                            ? "Yêu cầu đã được xử lý"
                            : "Yêu cầu đang được xử lý");
        }

        try {
            T result = action.get();

            markCompleted(scopedKey);

            return result;

        } catch (RuntimeException ex) {
            release(scopedKey);
            throw ex;
        }
    }
}