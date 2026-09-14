package com.application.authentication.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimiterService {

    // Atomically increments the counter and, only on the first hit in a
    // window, sets its expiry. Without the Lua script this would need two
    // round trips (INCR, then EXPIRE), leaving a race where two concurrent
    // requests could both see count==1 and either double-set or miss the TTL.
    private static final DefaultRedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>(
            "local current = redis.call('INCR', KEYS[1]) " +
            "if tonumber(current) == 1 then " +
            "  redis.call('EXPIRE', KEYS[1], ARGV[1]) " +
            "end " +
            "return current",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Increments the counter for {@code key} and returns the new count.
     * Callers compare the result against their configured limit.
     */
    public long increment(String key, long windowSeconds) {
        Long result = redisTemplate.execute(INCREMENT_SCRIPT, List.of(key), String.valueOf(windowSeconds));
        return result == null ? 0 : result;
    }

    public long getTtlSeconds(String key) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl == null || ttl < 0 ? 0 : ttl;
    }
}