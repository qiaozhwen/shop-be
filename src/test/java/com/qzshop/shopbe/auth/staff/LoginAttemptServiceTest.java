package com.qzshop.shopbe.auth.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

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
@Import({LoginAttemptService.class, LoginAttemptServiceTest.TestConfig.class})
@TestPropertySource(properties = {
    "spring.sql.init.mode=never",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "auth.staff.max-failed-attempts=3",
    "auth.staff.lock-duration=PT15M"
})
class LoginAttemptServiceTest {

    @TestConfiguration
    static class TestConfig {
        @Bean AuthProperties authProperties() {
            AuthProperties p = new AuthProperties();
            p.getJwt().setIssuer("t");
            p.getJwt().setSecret("0123456789-0123456789-0123456789-AB");
            p.getStaff().setMaxFailedAttempts(3);
            p.getStaff().setLockDuration(Duration.ofMinutes(15));
            return p;
        }
    }

    @Autowired StaffRepository staffRepo;
    @Autowired LoginAttemptService svc;

    private StaffEntity s;

    @BeforeEach
    void prepare() {
        staffRepo.deleteAll();
        s = new StaffEntity();
        s.setPhone("13800000099");
        s.setStoreId(1L);
        s.setName("test");
        staffRepo.save(s);
    }

    @Test
    void recordFailIncrementsCount() {
        svc.recordFailure(s.getId());
        assertThat(staffRepo.findById(s.getId()).orElseThrow().getFailedLoginCount()).isEqualTo(1);
    }

    @Test
    void recordFailReachesLimitAndLocks() {
        svc.recordFailure(s.getId());
        svc.recordFailure(s.getId());
        svc.recordFailure(s.getId());
        StaffEntity refreshed = staffRepo.findById(s.getId()).orElseThrow();
        assertThat(refreshed.getLockedUntil()).isAfter(java.time.LocalDateTime.now());
        assertThat(refreshed.getFailedLoginCount()).isZero();
    }

    @Test
    void ensureNotLockedThrowsWhenLocked() {
        s.setLockedUntil(java.time.LocalDateTime.now().plusMinutes(10));
        staffRepo.save(s);
        assertThatThrownBy(() -> svc.ensureNotLocked(s)).isInstanceOf(StaffLockedException.class);
    }

    @Test
    void recordSuccessClearsCounters() {
        s.setFailedLoginCount(2);
        s.setLockedUntil(java.time.LocalDateTime.now().minusMinutes(1));
        staffRepo.save(s);
        svc.recordSuccess(s.getId());
        StaffEntity refreshed = staffRepo.findById(s.getId()).orElseThrow();
        assertThat(refreshed.getFailedLoginCount()).isZero();
        assertThat(refreshed.getLockedUntil()).isNull();
        assertThat(refreshed.getLastLoginAt()).isNotNull();
    }
}
