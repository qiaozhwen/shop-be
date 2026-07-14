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
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.qzshop.shopbe.auth.AuthProperties;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({TokenService.class, JwtService.class})
@TestPropertySource(properties = {
    "spring.sql.init.mode=never",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class TokenServiceTest {

    @Autowired RefreshTokenRepository repo;
    @Autowired TokenService tokens;

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
    void clean() { repo.deleteAll(); }

    @Test
    void issueStoresHashedRefreshAndReturnsRawToken() {
        TokenIssueResult r = tokens.issueForStaff(7L, List.of("STAFF_DEFAULT"), null);
        assertThat(r.refreshToken()).isNotBlank();
        assertThat(repo.count()).isEqualTo(1);
        assertThat(repo.findAll().get(0).getTokenHash()).isNotEqualTo(r.refreshToken());
    }

    @Test
    void rotateInvalidatesOldAndIssuesNew() {
        TokenIssueResult first = tokens.issueForStaff(7L, List.of(), null);
        TokenIssueResult second = tokens.rotate(first.refreshToken(), List.of());
        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThat(repo.findByTokenHash(TokenService.hash(first.refreshToken())).orElseThrow()
            .getRevokedAt()).isNotNull();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rotateRejectsRevokedAndKillsAllSiblings() {
        TokenIssueResult a = tokens.issueForStaff(8L, List.of(), null);
        TokenIssueResult b = tokens.issueForStaff(8L, List.of(), null);
        tokens.rotate(a.refreshToken(), List.of());
        assertThatThrownBy(() -> tokens.rotate(a.refreshToken(), List.of()))
            .isInstanceOf(TokenReplayException.class);
        assertThat(repo.findBySubjectTypeAndSubjectIdAndRevokedAtIsNull("STAFF", 8L)).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentRotationAllowsOnlyOneSuccessAndRevokesIssuedSibling() throws Exception {
        TokenIssueResult original = tokens.issueForStaff(10L, List.of(), null);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger replays = new AtomicInteger();

        try (var executor = Executors.newFixedThreadPool(2)) {
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
        }

        assertThat(successes).hasValue(1);
        assertThat(replays).hasValue(1);
        assertThat(repo.findBySubjectTypeAndSubjectIdAndRevokedAtIsNull("STAFF", 10L)).isEmpty();
    }

    @Test
    void revokeAllRemovesActiveOnly() {
        tokens.issueForStaff(9L, List.of(), null);
        tokens.issueForStaff(9L, List.of(), null);
        int n = tokens.revokeAllForStaff(9L);
        assertThat(n).isEqualTo(2);
        assertThat(repo.findBySubjectTypeAndSubjectIdAndRevokedAtIsNull("STAFF", 9L)).isEmpty();
    }
}
