package com.qzshop.shopbe.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.qzshop.shopbe.dao.StoreRepository;
import com.qzshop.shopbe.dto.StoreRequest;
import com.qzshop.shopbe.entity.StoreEntity;

@Service
public class StoreService {
    private static final String OPEN_STATUS = "OPEN";
    private static final String CLOSED_STATUS = "CLOSED";

    private final StoreRepository storeRepository;

    public StoreService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    public List<StoreEntity> getAllStores() {
        return storeRepository.findByStatusNot(CLOSED_STATUS);
    }

    public Optional<StoreEntity> getStoreById(Long id) {
        return storeRepository.findById(id);
    }

    public StoreEntity createStore(StoreRequest request) {
        StoreEntity store = new StoreEntity();
        store.setName(request.getName());
        store.setCode(request.getCode());
        store.setAddress(request.getAddress());
        store.setPhone(request.getPhone());
        store.setOwnerName(request.getOwnerName());
        store.setStatus(request.getStatus() == null ? OPEN_STATUS : request.getStatus());
        store.setRemark(request.getRemark());
        store.setOpeningTime(request.getOpeningTime());
        store.setClosingTime(request.getClosingTime());
        return storeRepository.save(store);
    }

    public Optional<StoreEntity> updateStore(Long id, StoreRequest request) {
        return storeRepository.findById(id).map(store -> {
            if (request.getName() != null) {
                store.setName(request.getName());
            }
            if (request.getCode() != null) {
                store.setCode(request.getCode());
            }
            if (request.getAddress() != null) {
                store.setAddress(request.getAddress());
            }
            if (request.getPhone() != null) {
                store.setPhone(request.getPhone());
            }
            if (request.getOwnerName() != null) {
                store.setOwnerName(request.getOwnerName());
            }
            if (request.getStatus() != null) {
                store.setStatus(request.getStatus());
            }
            if (request.getRemark() != null) {
                store.setRemark(request.getRemark());
            }
            if (request.getOpeningTime() != null) {
                store.setOpeningTime(request.getOpeningTime());
            }
            if (request.getClosingTime() != null) {
                store.setClosingTime(request.getClosingTime());
            }
            return storeRepository.save(store);
        });
    }

    public boolean closeStore(Long id) {
        return storeRepository.findById(id)
                .map(store -> {
                    store.setStatus(CLOSED_STATUS);
                    storeRepository.save(store);
                    return true;
                })
                .orElse(false);
    }
}
