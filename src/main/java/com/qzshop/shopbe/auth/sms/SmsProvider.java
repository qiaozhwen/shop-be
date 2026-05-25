package com.qzshop.shopbe.auth.sms;

public interface SmsProvider {
    void send(String phone, String code, SmsPurpose purpose);
}
