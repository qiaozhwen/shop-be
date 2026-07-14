package com.qzshop.shopbe.auth.token;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Query("select r.subjectId from RefreshTokenEntity r where r.tokenHash = :tokenHash")
    Optional<Long> findSubjectIdByTokenHash(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RefreshTokenEntity r where r.tokenHash = :tokenHash")
    Optional<RefreshTokenEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    List<RefreshTokenEntity> findBySubjectTypeAndSubjectIdAndRevokedAtIsNull(String subjectType, Long subjectId);

    @Modifying
    @Query("update RefreshTokenEntity r set r.revokedAt = CURRENT_TIMESTAMP " +
           "where r.subjectType = :type and r.subjectId = :id and r.revokedAt is null")
    int revokeAllActive(@Param("type") String type, @Param("id") Long id);
}
