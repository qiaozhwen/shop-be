package com.qzshop.shopbe.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.qzshop.shopbe.entity.StoreEntity;

public interface StoreRepository extends JpaRepository<StoreEntity, Long> {
    List<StoreEntity> findByStatusNot(String status);
}
