package com.qzshop.shopbe.auth.staff;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface StaffRepository extends JpaRepository<StaffEntity, Long> {
    Optional<StaffEntity> findByPhone(String phone);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StaffEntity s where s.phone = :phone")
    Optional<StaffEntity> findByPhoneForUpdate(@Param("phone") String phone);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StaffEntity s where s.id = :id")
    Optional<StaffEntity> findByIdForUpdate(@Param("id") Long id);
}
