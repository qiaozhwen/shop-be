package com.qzshop.shopbe.auth.sms;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SmsVerificationCodeRepository extends JpaRepository<SmsVerificationCodeEntity, Long> {

    @Query("select c from SmsVerificationCodeEntity c " +
           "where c.phone = :phone and c.purpose = :purpose " +
           "order by c.createdAt desc")
    List<SmsVerificationCodeEntity> findRecent(@Param("phone") String phone,
                                               @Param("purpose") String purpose,
                                               Pageable page);

    @Query("select count(c) from SmsVerificationCodeEntity c " +
           "where c.phone = :phone and c.createdAt >= :since")
    long countByPhoneSince(@Param("phone") String phone, @Param("since") LocalDateTime since);

    default Optional<SmsVerificationCodeEntity> findLatest(String phone, String purpose) {
        var list = findRecent(phone, purpose, Pageable.ofSize(1));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
