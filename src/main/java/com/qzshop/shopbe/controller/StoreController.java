package com.qzshop.shopbe.controller;

import java.util.List;
import java.util.LinkedHashMap;
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
import org.springframework.web.bind.annotation.RequestParam;
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
    public Map<String, Object> getAllStores(@RequestParam Map<String, String> params) {
        List<Map<String, Object>> stores = storeService.getAllStores().stream()
                .map(this::storeView)
                .filter(store -> matches(store, params.get("keyword")))
                .filter(store -> params.get("status") == null || params.get("status").equals(store.get("status")))
                .toList();
        int page = Integer.parseInt(params.getOrDefault("page", "1"));
        int pageSize = Integer.parseInt(params.getOrDefault("pageSize", "10"));
        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(stores.size(), from + pageSize);
        List<Map<String, Object>> list = from >= stores.size() ? List.of() : stores.subList(from, to);
        return ok(Map.of("list", list, "total", stores.size()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getStoreById(@PathVariable Long id) {
        return storeService.getStoreById(id)
                .<ResponseEntity<?>>map(store -> ResponseEntity.ok(storeView(store)))
                .orElseGet(this::storeNotFound);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createStore(@Validated(CreateStore.class) @RequestBody StoreRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storeView(storeService.createStore(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateStore(@PathVariable Long id, @Valid @RequestBody StoreRequest request) {
        return storeService.updateStore(id, request)
            .<ResponseEntity<?>>map(store -> ResponseEntity.ok(storeView(store)))
                .orElseGet(this::storeNotFound);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStore(@PathVariable Long id) {
        if (storeService.closeStore(id)) {
            return ResponseEntity.ok(ok(null));
        }
        return storeNotFound();
    }

    private Map<String, Object> ok(Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 200);
        body.put("message", "ok");
        body.put("data", data);
        return body;
    }

    private Map<String, Object> storeView(StoreEntity store) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", store.getId());
        view.put("code", store.getCode() == null
                ? "S" + String.format("%03d", store.getId() == null ? 0 : store.getId())
                : store.getCode());
        view.put("name", store.getName());
        view.put("address", store.getAddress());
        view.put("phone", store.getPhone());
        view.put("ownerName", store.getOwnerName());
        view.put("status", store.getStatus());
        view.put("remark", store.getRemark());
        view.put("openTime", store.getOpeningTime() == null ? null : store.getOpeningTime().toString());
        view.put("closeTime", store.getClosingTime() == null ? null : store.getClosingTime().toString());
        view.put("createdAt", store.getCreatedAt() == null ? null : store.getCreatedAt().toString());
        return view;
    }

    private boolean matches(Map<String, Object> store, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        String lower = keyword.toLowerCase();
        return List.of("name", "code", "address", "ownerName").stream()
                .map(key -> String.valueOf(store.getOrDefault(key, "")).toLowerCase())
                .anyMatch(value -> value.contains(lower));
    }

    private ResponseEntity<Map<String, String>> storeNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Store not found"));
    }
}
