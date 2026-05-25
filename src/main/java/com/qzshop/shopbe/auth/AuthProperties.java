package com.qzshop.shopbe.auth;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties("auth")
public class AuthProperties {

    private Jwt jwt = new Jwt();
    private Staff staff = new Staff();
    private Sms sms = new Sms();

    public Jwt getJwt() { return jwt; }
    public Staff getStaff() { return staff; }
    public Sms getSms() { return sms; }

    public static class Jwt {
        @NotBlank private String issuer;
        @NotBlank private String secret;
        private Duration accessTtl = Duration.ofMinutes(30);
        private Duration refreshTtl = Duration.ofDays(14);
        private Duration bindPendingTtl = Duration.ofMinutes(5);
        public String getIssuer() { return issuer; }
        public void setIssuer(String v) { issuer = v; }
        public String getSecret() { return secret; }
        public void setSecret(String v) { secret = v; }
        public Duration getAccessTtl() { return accessTtl; }
        public void setAccessTtl(Duration v) { accessTtl = v; }
        public Duration getRefreshTtl() { return refreshTtl; }
        public void setRefreshTtl(Duration v) { refreshTtl = v; }
        public Duration getBindPendingTtl() { return bindPendingTtl; }
        public void setBindPendingTtl(Duration v) { bindPendingTtl = v; }
    }

    public static class Staff {
        @Min(1) private int maxFailedAttempts = 5;
        private Duration lockDuration = Duration.ofMinutes(15);
        public int getMaxFailedAttempts() { return maxFailedAttempts; }
        public void setMaxFailedAttempts(int v) { maxFailedAttempts = v; }
        public Duration getLockDuration() { return lockDuration; }
        public void setLockDuration(Duration v) { lockDuration = v; }
    }

    public static class Sms {
        @NotBlank private String provider = "mock";
        private Duration codeTtl = Duration.ofMinutes(5);
        private Duration resendCooldown = Duration.ofSeconds(60);
        @Min(1) private int dailyLimit = 10;
        public String getProvider() { return provider; }
        public void setProvider(String v) { provider = v; }
        public Duration getCodeTtl() { return codeTtl; }
        public void setCodeTtl(Duration v) { codeTtl = v; }
        public Duration getResendCooldown() { return resendCooldown; }
        public void setResendCooldown(Duration v) { resendCooldown = v; }
        public int getDailyLimit() { return dailyLimit; }
        public void setDailyLimit(int v) { dailyLimit = v; }
    }

    @PostConstruct
    void validateSecret() {
        if (jwt.getSecret() == null || jwt.getSecret().getBytes().length < 32) {
            throw new IllegalStateException(
                "auth.jwt.secret must be at least 32 bytes (set AUTH_JWT_SECRET env)");
        }
    }
}
