package com.qzshop.shopbe.auth.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qzshop.shopbe.auth.AuthProperties;

@Service
public class TokenService {

    private static final String SUBJECT_STAFF = "STAFF";
    private static final SecureRandom RNG = new SecureRandom();

    private final RefreshTokenRepository repo;
    private final JwtService jwt;
    private final AuthProperties props;

    public TokenService(RefreshTokenRepository repo, JwtService jwt, AuthProperties props) {
        this.repo = repo;
        this.jwt = jwt;
        this.props = props;
    }

    @Transactional
    public TokenIssueResult issueForStaff(long staffId, List<String> roles, String deviceInfo) {
        String access = jwt.issueStaff(staffId, roles);
        String refresh = newRefresh();
        RefreshTokenEntity row = new RefreshTokenEntity();
        row.setTokenHash(hash(refresh));
        row.setSubjectType(SUBJECT_STAFF);
        row.setSubjectId(staffId);
        row.setDeviceInfo(deviceInfo);
        row.setExpiresAt(LocalDateTime.now().plus(props.getJwt().getRefreshTtl()));
        repo.save(row);
        return new TokenIssueResult(
            access, props.getJwt().getAccessTtl(),
            refresh, props.getJwt().getRefreshTtl(),
            staffId, roles
        );
    }

    @Transactional
    public TokenIssueResult rotate(String refreshToken, List<String> roles) {
        Optional<RefreshTokenEntity> opt = repo.findByTokenHash(hash(refreshToken));
        if (opt.isEmpty()) {
            throw new TokenReplayException("refresh token not found");
        }
        RefreshTokenEntity row = opt.get();
        if (row.getRevokedAt() != null) {
            repo.revokeAllActive(row.getSubjectType(), row.getSubjectId());
            throw new TokenReplayException("refresh token replay detected");
        }
        if (row.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TokenReplayException("refresh token expired");
        }
        row.setRevokedAt(LocalDateTime.now());
        repo.save(row);
        return issueForStaff(row.getSubjectId(), roles, row.getDeviceInfo());
    }

    @Transactional
    public int revokeAllForStaff(long staffId) {
        return repo.revokeAllActive(SUBJECT_STAFF, staffId);
    }

    @Transactional
    public boolean revokeOne(String refreshToken) {
        Optional<RefreshTokenEntity> opt = repo.findByTokenHash(hash(refreshToken));
        if (opt.isEmpty() || opt.get().getRevokedAt() != null) return false;
        opt.get().setRevokedAt(LocalDateTime.now());
        return true;
    }

    private static String newRefresh() {
        byte[] b = new byte[32];
        RNG.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    static String hash(String token) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
