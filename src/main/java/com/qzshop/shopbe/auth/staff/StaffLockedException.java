package com.qzshop.shopbe.auth.staff;

public class StaffLockedException extends RuntimeException {
    private final long retryAfterSeconds;
    public StaffLockedException(long retryAfterSeconds) {
        super("Account locked");
        this.retryAfterSeconds = retryAfterSeconds;
    }
    public long getRetryAfterSeconds() { return retryAfterSeconds; }
}
