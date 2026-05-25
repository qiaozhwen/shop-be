package com.qzshop.shopbe.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.qzshop.shopbe.auth.AuthProperties;

import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    private JwtService svc;

    @BeforeEach
    void setUp() {
        AuthProperties props = new AuthProperties();
        props.getJwt().setIssuer("test-iss");
        props.getJwt().setSecret("0123456789-0123456789-0123456789-AB");
        props.getJwt().setAccessTtl(Duration.ofSeconds(60));
        props.getJwt().setBindPendingTtl(Duration.ofSeconds(30));
        svc = new JwtService(props);
    }

    @Test
    void issuesAndParsesStaffToken() {
        String token = svc.issueStaff(123L, List.of("STAFF_DEFAULT"));
        ParsedToken p = svc.parse(token);
        assertThat(p.type()).isEqualTo(SubjectType.STAFF);
        assertThat(p.subjectId()).isEqualTo(123L);
        assertThat(p.roles()).containsExactly("STAFF_DEFAULT");
    }

    @Test
    void issuesAndParsesBindPendingToken() {
        String token = svc.issueBindPending(99L, "WECHAT");
        ParsedToken p = svc.parse(token);
        assertThat(p.type()).isEqualTo(SubjectType.BIND_PENDING);
        assertThat(p.subjectId()).isEqualTo(99L);
        assertThat(p.provider()).isEqualTo("WECHAT");
        assertThat(p.roles()).isEmpty();
    }

    @Test
    void rejectsTamperedToken() {
        String token = svc.issueStaff(1L, List.of());
        String tampered = token.substring(0, token.length() - 2) + "AA";
        assertThatThrownBy(() -> svc.parse(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        AuthProperties shortProps = new AuthProperties();
        shortProps.getJwt().setIssuer("x");
        shortProps.getJwt().setSecret("0123456789-0123456789-0123456789-AB");
        shortProps.getJwt().setAccessTtl(Duration.ofMillis(1));
        JwtService shortSvc = new JwtService(shortProps);
        String token = shortSvc.issueStaff(1L, List.of());
        Thread.sleep(20);
        assertThatThrownBy(() -> shortSvc.parse(token)).isInstanceOf(JwtException.class);
    }
}
