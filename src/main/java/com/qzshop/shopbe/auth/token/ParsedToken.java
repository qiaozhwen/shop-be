package com.qzshop.shopbe.auth.token;

import java.time.Instant;
import java.util.List;

public record ParsedToken(
    SubjectType type,
    long subjectId,
    List<String> roles,
    String provider,
    String jti,
    Instant expiresAt
) {}
