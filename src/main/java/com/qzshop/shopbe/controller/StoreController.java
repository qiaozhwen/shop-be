package com.qzshop.shopbe.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qzshop.shopbe.dto.StoreRequest;
import com.qzshop.shopbe.dto.CreateStore;
import com.qzshop.shopbe.entity.StoreEntity;
import com.qzshop.shopbe.service.StoreService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping
    public List<StoreEntity> getAllStores() {
        return storeService.getAllStores();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getStoreById(@PathVariable Long id) {
        return storeService.getStoreById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(this::storeNotFound);
    }

    @PostMapping
    public ResponseEntity<StoreEntity> createStore(@Validated(CreateStore.class) @RequestBody StoreRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storeService.createStore(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateStore(@PathVariable Long id, @Valid @RequestBody StoreRequest request) {
        return storeService.updateStore(id, request)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(this::storeNotFound);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStore(@PathVariable Long id) {
        if (storeService.closeStore(id)) {
            return ResponseEntity.noContent().build();
        }
        return storeNotFound();
    }

    private ResponseEntity<Map<String, String>> storeNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Store not found"));
    }
}
