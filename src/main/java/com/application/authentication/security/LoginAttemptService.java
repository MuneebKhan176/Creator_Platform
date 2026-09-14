package com.application.authentication.security;

import com.application.authentication.exceptions.ApiException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class LoginAttemptService {

    private static final String FAILURE_COUNT_PREFIX = "login:fails:";
    private static final String LOCKOUT_PREFIX = "login:locked:";

    // Failure count is remembered this long even without a lockout, so an
    // old, resolved burst of typos doesn't count against the user forever.
    private static final Duration FAILURE_WINDOW = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    public LoginAttemptService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void checkNotLocked(String email) {
        Long ttl = redisTemplate.getExpire(LOCKOUT_PREFIX + email, TimeUnit.SECONDS);
        if (ttl != null && ttl > 0) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed login attempts. Please try again in " + humanize(ttl) + ".",
                    ttl);
        }
    }

    public void recordFailure(String email) {
        String countKey = FAILURE_COUNT_PREFIX + email;
        Long count = redisTemplate.opsForValue().increment(countKey);
        if (count != null && count == 1) {
            redisTemplate.expire(countKey, FAILURE_WINDOW);
        }

        long lockoutSeconds = lockoutSecondsFor(count == null ? 1 : count);
        if (lockoutSeconds > 0) {
            redisTemplate.opsForValue().set(LOCKOUT_PREFIX + email, "1", Duration.ofSeconds(lockoutSeconds));
        }
    }

    public void recordSuccess(String email) {
        redisTemplate.delete(FAILURE_COUNT_PREFIX + email);
        redisTemplate.delete(LOCKOUT_PREFIX + email);
    }

    // Progressive: a handful of typos costs nothing; a sustained attack gets
    // slower with every attempt, rather than a flat wall an attacker can
    // simply wait out once and resume.
    private long lockoutSecondsFor(long failureCount) {
        if (failureCount >= 20) return 1800; // 30 min
        if (failureCount >= 12) return 600;  // 10 min
        if (failureCount >= 8) return 120;   // 2 min
        if (failureCount >= 5) return 30;    // 30 sec
        return 0;
    }

    private String humanize(long seconds) {
        return seconds < 60 ? seconds + " second(s)" : (seconds / 60) + " minute(s)";
    }
}