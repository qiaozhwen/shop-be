package com.qzshop.shopbe.auth.token;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.qzshop.shopbe.auth.AuthProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final AuthProperties props;
    private final SecretKey key;

    public JwtService(AuthProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String issueStaff(long staffId, List<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
            .issuer(props.getJwt().getIssuer())
            .subject(String.valueOf(staffId))
            .id(UUID.randomUUID().toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(props.getJwt().getAccessTtl())))
            .claims(Map.of("typ", SubjectType.STAFF.name(), "roles", roles))
            .signWith(key)
            .compact();
    }

    public String issueBindPending(long socialAccountId, String provider) {
        Instant now = Instant.now();
        return Jwts.builder()
            .issuer(props.getJwt().getIssuer())
            .subject(String.valueOf(socialAccountId))
            .id(UUID.randomUUID().toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(props.getJwt().getBindPendingTtl())))
            .claims(Map.of("typ", SubjectType.BIND_PENDING.name(), "provider", provider))
            .signWith(key)
            .compact();
    }

    @SuppressWarnings("unchecked")
    public ParsedToken parse(String token) {
        Jws<Claims> jws = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
        Claims c = jws.getPayload();
        SubjectType type = SubjectType.valueOf(c.get("typ", String.class));
        long subjectId = Long.parseLong(c.getSubject());
        List<String> roles = type == SubjectType.STAFF
            ? (List<String>) c.getOrDefault("roles", List.of())
            : List.of();
        String provider = c.get("provider", String.class);
        return new ParsedToken(type, subjectId, roles, provider, c.getId(), c.getExpiration().toInstant());
    }
}
