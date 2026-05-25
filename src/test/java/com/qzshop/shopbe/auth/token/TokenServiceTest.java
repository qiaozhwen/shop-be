package com.qzshop.shopbe.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

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
    void rotateRejectsRevokedAndKillsAllSiblings() {
        TokenIssueResult a = tokens.issueForStaff(8L, List.of(), null);
        TokenIssueResult b = tokens.issueForStaff(8L, List.of(), null);
        tokens.rotate(a.refreshToken(), List.of());
        assertThatThrownBy(() -> tokens.rotate(a.refreshToken(), List.of()))
            .isInstanceOf(TokenReplayException.class);
        assertThat(repo.findBySubjectTypeAndSubjectIdAndRevokedAtIsNull("STAFF", 8L)).isEmpty();
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
