package com.qzshop.shopbe.auth.token;

import java.time.Duration;
import java.util.List;

public record TokenIssueResult(
    String accessToken,
    Duration accessTtl,
    String refreshToken,
    Duration refreshTtl,
    long staffId,
    List<String> roles
) {}
