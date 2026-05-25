package com.qzshop.shopbe.auth.sms;

import java.util.logging.Logger;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "auth.sms.provider", havingValue = "mock", matchIfMissing = true)
public class MockSmsProvider implements SmsProvider {

    private static final Logger LOG = Logger.getLogger(MockSmsProvider.class.getName());

    @Override
    public void send(String phone, String code, SmsPurpose purpose) {
        LOG.info(() -> "[MOCK SMS] phone=" + phone + " purpose=" + purpose + " code=" + code);
    }
}
