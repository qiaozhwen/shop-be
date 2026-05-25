package com.qzshop.shopbe.auth.sms;

public class SmsCodeInvalidException extends RuntimeException {
    public SmsCodeInvalidException(String msg) { super(msg); }
}
