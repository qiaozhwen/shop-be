package com.qzshop.shopbe.auth.staff;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qzshop.shopbe.auth.AuthProperties;

@Service
public class LoginAttemptService {

    private final StaffRepository repo;
    private final AuthProperties props;

    public LoginAttemptService(StaffRepository repo, AuthProperties props) {
        this.repo = repo;
        this.props = props;
    }

    public void ensureNotLocked(StaffEntity staff) {
        LocalDateTime until = staff.getLockedUntil();
        if (until != null && until.isAfter(LocalDateTime.now())) {
            long left = Duration.between(LocalDateTime.now(), until).getSeconds();
            throw new StaffLockedException(Math.max(1, left));
        }
    }

    @Transactional
    public void recordFailure(Long staffId) {
        StaffEntity s = repo.findById(staffId).orElseThrow();
        int next = s.getFailedLoginCount() + 1;
        if (next >= props.getStaff().getMaxFailedAttempts()) {
            s.setFailedLoginCount(0);
            s.setLockedUntil(LocalDateTime.now().plus(props.getStaff().getLockDuration()));
        } else {
            s.setFailedLoginCount(next);
        }
    }

    @Transactional
    public void recordSuccess(Long staffId) {
        StaffEntity s = repo.findById(staffId).orElseThrow();
        s.setFailedLoginCount(0);
        s.setLockedUntil(null);
        s.setLastLoginAt(LocalDateTime.now());
    }
}
