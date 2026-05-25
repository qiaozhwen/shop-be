package com.qzshop.shopbe.auth.token;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    List<RefreshTokenEntity> findBySubjectTypeAndSubjectIdAndRevokedAtIsNull(String subjectType, Long subjectId);

    @Modifying
    @Query("update RefreshTokenEntity r set r.revokedAt = CURRENT_TIMESTAMP " +
           "where r.subjectType = :type and r.subjectId = :id and r.revokedAt is null")
    int revokeAllActive(@Param("type") String type, @Param("id") Long id);
}
