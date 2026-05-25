package com.qzshop.shopbe.auth.sms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import com.qzshop.shopbe.auth.AuthProperties;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({SmsVerificationService.class, SmsVerificationServiceTest.TestConfig.class})
@TestPropertySource(properties = {
    "spring.sql.init.mode=never",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class SmsVerificationServiceTest {

    static final List<String> sent = new ArrayList<>();

    @TestConfiguration
    static class TestConfig {
        @Bean AuthProperties authProperties() {
            AuthProperties p = new AuthProperties();
            p.getJwt().setIssuer("t");
            p.getJwt().setSecret("0123456789-0123456789-0123456789-AB");
            p.getSms().setCodeTtl(Duration.ofMinutes(5));
            p.getSms().setResendCooldown(Duration.ofSeconds(60));
            p.getSms().setDailyLimit(3);
            return p;
        }
        @Bean SmsProvider smsProvider() {
            return (phone, code, purpose) -> sent.add(phone + ":" + code);
        }
    }

    @Autowired SmsVerificationService svc;
    @Autowired SmsVerificationCodeRepository repo;

    @BeforeEach
    void reset() { repo.deleteAll(); sent.clear(); }

    @Test
    void sendsAndPersistsHashedCode() {
        svc.send("13800000001", SmsPurpose.SMS_LOGIN, "127.0.0.1");
        assertThat(repo.count()).isEqualTo(1);
        assertThat(repo.findAll().get(0).getCodeHash()).hasSize(64);
        assertThat(sent).hasSize(1);
    }

    @Test
    void resendCooldownTriggers429() {
        svc.send("13800000002", SmsPurpose.SMS_LOGIN, null);
        assertThatThrownBy(() -> svc.send("13800000002", SmsPurpose.SMS_LOGIN, null))
            .isInstanceOf(SmsThrottleException.class);
    }

    @Test
    void verifyConsumesCode() {
        svc.send("13800000003", SmsPurpose.BIND_PHONE, null);
        String code = sent.get(0).split(":")[1];
        svc.verifyAndConsume("13800000003", SmsPurpose.BIND_PHONE, code);
        assertThat(repo.findAll().get(0).getConsumedAt()).isNotNull();
    }

    @Test
    void verifyWrongCodeIncrementsAttemptsAndFinallyInvalidates() {
        svc.send("13800000004", SmsPurpose.SMS_LOGIN, null);
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> svc.verifyAndConsume("13800000004", SmsPurpose.SMS_LOGIN, "000000"))
                .isInstanceOf(SmsCodeInvalidException.class);
        }
        String code = sent.get(0).split(":")[1];
        assertThatThrownBy(() -> svc.verifyAndConsume("13800000004", SmsPurpose.SMS_LOGIN, code))
            .isInstanceOf(SmsCodeInvalidException.class);
    }
}
