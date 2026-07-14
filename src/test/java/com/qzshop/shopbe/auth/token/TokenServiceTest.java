package com.qzshop.shopbe.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.qzshop.shopbe.auth.AuthProperties;
import com.qzshop.shopbe.auth.staff.StaffEntity;
import com.qzshop.shopbe.auth.staff.StaffRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({TokenService.class, JwtService.class})
@TestPropertySource(properties = {
    "spring.sql.init.mode=never",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TokenServiceTest {

    @Autowired RefreshTokenRepository repo;
    @Autowired TokenService tokens;
    @Autowired StaffRepository staffRepository;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    private long staffId;

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        AuthProperties authProperties() {
            AuthProperties p = new AuthProperties();
            p.getJwt().setIssuer("test");
            p.getJwt().setSecret("0123456789-0123456789-0123456789-AB");
            p.getJwt().setAccessTtl(Duration.ofMinutes(10));
            p.getJwt().setRefreshTtl(Duration.ofDays(1));
            return p;
        }
    }

    @BeforeEach
    void clean() {
        repo.deleteAll();
        staffRepository.deleteAll();
        StaffEntity staff = new StaffEntity();
        staff.setStoreId(1L);
        staff.setName("令牌测试员工");
        staff.setPhone("13900008888");
        staff.setRole("ADMIN");
        staff.setStatus("ACTIVE");
        staffId = staffRepository.saveAndFlush(staff).getId();
    }

    @Test
    void issueStoresHashedRefreshAndReturnsRawToken() {
        TokenIssueResult r = tokens.issueForStaff(staffId, List.of("STAFF_DEFAULT"), null);
        assertThat(r.refreshToken()).isNotBlank();
        assertThat(repo.count()).isEqualTo(1);
        assertThat(repo.findAll().get(0).getTokenHash()).isNotEqualTo(r.refreshToken());
    }

    @Test
    void rotateInvalidatesOldAndIssuesNew() {
        TokenIssueResult first = tokens.issueForStaff(staffId, List.of(), null);
        TokenIssueResult second = tokens.rotate(first.refreshToken(), List.of());
        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThat(repo.findByTokenHash(TokenService.hash(first.refreshToken())).orElseThrow()
            .getRevokedAt()).isNotNull();
    }

    @Test
    void rotateRejectsRevokedAndKillsAllSiblings() {
        TokenIssueResult a = tokens.issueForStaff(staffId, List.of(), null);
        tokens.issueForStaff(staffId, List.of(), null);
        tokens.rotate(a.refreshToken(), List.of());
        assertThatThrownBy(() -> tokens.rotate(a.refreshToken(), List.of()))
            .isInstanceOf(TokenReplayException.class);
        assertThat(repo.findBySubjectTypeAndSubjectIdAndRevokedAtIsNull("STAFF", staffId)).isEmpty();
    }

    @Test
    void concurrentRotationAllowsOnlyOneSuccessAndRevokesIssuedSibling() throws Exception {
        TokenIssueResult original = tokens.issueForStaff(staffId, List.of(), null);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger replays = new AtomicInteger();

        var executor = Executors.newFixedThreadPool(2);
        try {
            var calls = java.util.stream.IntStream.range(0, 2)
                    .mapToObj(ignored -> executor.submit(() -> {
                        ready.countDown();
                        start.await(5, TimeUnit.SECONDS);
                        try {
                            tokens.rotate(original.refreshToken(), List.of());
                            successes.incrementAndGet();
                        } catch (TokenReplayException ex) {
                            replays.incrementAndGet();
                        }
                        return null;
                    }))
                    .toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (var call : calls) call.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertThat(successes).hasValue(1);
        assertThat(replays).hasValue(1);
        assertThat(repo.findBySubjectTypeAndSubjectIdAndRevokedAtIsNull("STAFF", staffId)).isEmpty();
    }

    @Test
    void rotationWaitsForTheStaffLifecycleLock() throws Exception {
        TokenIssueResult token = tokens.issueForStaff(staffId, List.of(), null);
        assertWaitsForStaffLock(() -> tokens.rotate(token.refreshToken(), List.of()));
    }

    @Test
    void revokeAllWaitsForTheStaffLifecycleLock() throws Exception {
        assertWaitsForStaffLock(() -> tokens.revokeAllForStaff(staffId));
    }

    @Test
    void revokeOneWaitsForTheStaffLifecycleLock() throws Exception {
        TokenIssueResult token = tokens.issueForStaff(staffId, List.of(), null);
        assertWaitsForStaffLock(() -> tokens.revokeOne(token.refreshToken()));
    }

    @Test
    void revokeAllRemovesActiveOnly() {
        tokens.issueForStaff(staffId, List.of(), null);
        tokens.issueForStaff(staffId, List.of(), null);
        int n = tokens.revokeAllForStaff(staffId);
        assertThat(n).isEqualTo(2);
        assertThat(repo.findBySubjectTypeAndSubjectIdAndRevokedAtIsNull("STAFF", staffId)).isEmpty();
    }

    private void assertWaitsForStaffLock(Runnable operation) throws Exception {
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var lockHolder = executor.submit(() -> {
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    jdbc.queryForObject("select id from staff where id = ? for update", Long.class, staffId);
                    locked.countDown();
                    try {
                        release.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(ex);
                    }
                });
            });
            assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();

            var lifecycleOperation = executor.submit(operation);
            Thread.sleep(250);
            assertThat(lifecycleOperation.isDone()).isFalse();

            release.countDown();
            lockHolder.get(5, TimeUnit.SECONDS);
            lifecycleOperation.get(5, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }
}
