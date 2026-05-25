package com.qzshop.shopbe.auth.sms;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

class MockSmsProviderTest {

    @Test
    void doesNotThrowAndLogsAtInfo() {
        Logger logger = Logger.getLogger(MockSmsProvider.class.getName());
        List<LogRecord> records = new ArrayList<>();
        Handler h = new Handler() {
            @Override public void publish(LogRecord r) { records.add(r); }
            @Override public void flush() {}
            @Override public void close() {}
        };
        logger.addHandler(h);
        try {
            new MockSmsProvider().send("13800000000", "123456", SmsPurpose.SMS_LOGIN);
            assertThat(records).anyMatch(r -> r.getMessage().contains("13800000000"));
        } finally {
            logger.removeHandler(h);
        }
    }
}
