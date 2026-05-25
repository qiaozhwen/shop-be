package com.qzshop.shopbe.auth.sms;

public class SmsThrottleException extends RuntimeException {
    private final long retryAfterSeconds;

    public SmsThrottleException(String msg, long retryAfterSeconds) {
        super(msg);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() { return retryAfterSeconds; }
}
