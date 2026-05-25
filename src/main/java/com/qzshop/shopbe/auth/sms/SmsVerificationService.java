package com.qzshop.shopbe.auth.sms;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qzshop.shopbe.auth.AuthProperties;

@Service
public class SmsVerificationService {

    private static final SecureRandom RNG = new SecureRandom();
    private static final int MAX_ATTEMPTS = 5;

    private final SmsVerificationCodeRepository repo;
    private final SmsProvider provider;
    private final AuthProperties props;

    public SmsVerificationService(SmsVerificationCodeRepository repo,
                                  SmsProvider provider,
                                  AuthProperties props) {
        this.repo = repo;
        this.provider = provider;
        this.props = props;
    }

    @Transactional
    public void send(String phone, SmsPurpose purpose, String ip) {
        Duration cooldown = props.getSms().getResendCooldown();
        Optional<SmsVerificationCodeEntity> last = repo.findLatest(phone, purpose.name());
        if (last.isPresent() && last.get().getCreatedAt().plus(cooldown).isAfter(LocalDateTime.now())) {
            long left = Duration.between(LocalDateTime.now(), last.get().getCreatedAt().plus(cooldown)).getSeconds();
            throw new SmsThrottleException("resend cooldown", Math.max(1, left));
        }
        long sentToday = repo.countByPhoneSince(phone, LocalDateTime.now().minusHours(24));
        if (sentToday >= props.getSms().getDailyLimit()) {
            throw new SmsThrottleException("daily limit reached", 3600);
        }
        String code = String.format("%06d", RNG.nextInt(1_000_000));
        SmsVerificationCodeEntity row = new SmsVerificationCodeEntity();
        row.setPhone(phone);
        row.setPurpose(purpose.name());
        row.setCodeHash(sha256(code));
        row.setExpiresAt(LocalDateTime.now().plus(props.getSms().getCodeTtl()));
        row.setIp(ip);
        repo.save(row);
        provider.send(phone, code, purpose);
    }

    @Transactional(noRollbackFor = SmsCodeInvalidException.class)
    public void verifyAndConsume(String phone, SmsPurpose purpose, String code) {
        SmsVerificationCodeEntity row = repo.findLatest(phone, purpose.name())
            .orElseThrow(() -> new SmsCodeInvalidException("no code"));
        if (row.getConsumedAt() != null) throw new SmsCodeInvalidException("consumed");
        if (row.getExpiresAt().isBefore(LocalDateTime.now())) throw new SmsCodeInvalidException("expired");
        if (row.getAttempts() >= MAX_ATTEMPTS) throw new SmsCodeInvalidException("too many attempts");
        if (!row.getCodeHash().equals(sha256(code))) {
            row.setAttempts(row.getAttempts() + 1);
            repo.saveAndFlush(row);
            throw new SmsCodeInvalidException("mismatch");
        }
        row.setConsumedAt(LocalDateTime.now());
    }

    private static String sha256(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
