package com.application.authentication.security;

import com.application.authentication.exceptions.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class RateLimitGuard {

    private final RateLimiterService rateLimiterService;

    private final int registerLimit;
    private final long registerWindowSeconds;
    private final int loginIpLimit;
    private final long loginIpWindowSeconds;
    private final int verifyLimit;
    private final long verifyWindowSeconds;
    private final int resendLimit;
    private final long resendWindowSeconds;
    private final int forgotPasswordLimit;
    private final long forgotPasswordWindowSeconds;

    public RateLimitGuard(RateLimiterService rateLimiterService,
                           @Value("${app.rate-limit.register.max}") int registerLimit,
                           @Value("${app.rate-limit.register.window-seconds}") long registerWindowSeconds,
                           @Value("${app.rate-limit.login-ip.max}") int loginIpLimit,
                           @Value("${app.rate-limit.login-ip.window-seconds}") long loginIpWindowSeconds,
                           @Value("${app.rate-limit.verify.max}") int verifyLimit,
                           @Value("${app.rate-limit.verify.window-seconds}") long verifyWindowSeconds,
                           @Value("${app.rate-limit.resend.max}") int resendLimit,
                           @Value("${app.rate-limit.resend.window-seconds}") long resendWindowSeconds,
                           @Value("${app.rate-limit.forgot-password.max}") int forgotPasswordLimit,
                           @Value("${app.rate-limit.forgot-password.window-seconds}") long forgotPasswordWindowSeconds) {
        this.rateLimiterService = rateLimiterService;
        this.registerLimit = registerLimit;
        this.registerWindowSeconds = registerWindowSeconds;
        this.loginIpLimit = loginIpLimit;
        this.loginIpWindowSeconds = loginIpWindowSeconds;
        this.verifyLimit = verifyLimit;
        this.verifyWindowSeconds = verifyWindowSeconds;
        this.resendLimit = resendLimit;
        this.resendWindowSeconds = resendWindowSeconds;
        this.forgotPasswordLimit = forgotPasswordLimit;
        this.forgotPasswordWindowSeconds = forgotPasswordWindowSeconds;
    }

    public void checkRegister(String ip) {
        enforce("ratelimit:register:ip:" + ip, registerLimit, registerWindowSeconds,
                "Too many registration attempts from this network. Please try again later.");
    }

    public void checkLoginIp(String ip) {
        enforce("ratelimit:login:ip:" + ip, loginIpLimit, loginIpWindowSeconds,
                "Too many login attempts from this network. Please try again later.");
    }

    public void checkVerify(String ip) {
        enforce("ratelimit:verify:ip:" + ip, verifyLimit, verifyWindowSeconds,
                "Too many verification attempts from this network. Please try again later.");
    }

    public void checkResend(String email) {
        enforce("ratelimit:resend:email:" + email, resendLimit, resendWindowSeconds,
                "Too many verification code requests for this email. Please try again later.");
    }

    public void checkForgotPassword(String email) {
        enforce("ratelimit:forgot-password:email:" + email, forgotPasswordLimit, forgotPasswordWindowSeconds,
                "Too many password reset requests for this email. Please try again later.");
    }

    private void enforce(String key, int limit, long windowSeconds, String message) {
        long count = rateLimiterService.increment(key, windowSeconds);
        if (count > limit) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, message, rateLimiterService.getTtlSeconds(key));
        }
    }
}