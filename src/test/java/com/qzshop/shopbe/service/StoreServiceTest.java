package com.qzshop.shopbe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.qzshop.shopbe.dao.StoreRepository;
import com.qzshop.shopbe.dto.StoreRequest;
import com.qzshop.shopbe.entity.StoreEntity;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private StoreService storeService;

    @Test
    void getAllStoresExcludesClosedStores() {
        StoreEntity openStore = store(1L, "虹口店", "OPEN");
        when(storeRepository.findByStatusNot("CLOSED")).thenReturn(List.of(openStore));

        List<StoreEntity> stores = storeService.getAllStores();

        assertThat(stores).containsExactly(openStore);
    }

    @Test
    void getStoreByIdReturnsOptionalStore() {
        StoreEntity store = store(1L, "虹口店", "OPEN");
        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));

        Optional<StoreEntity> result = storeService.getStoreById(1L);

        assertThat(result).contains(store);
    }

    @Test
    void createStoreDefaultsStatusToOpenWhenStatusIsMissing() {
        StoreRequest request = new StoreRequest();
        request.setName("徐汇店");
        request.setAddress("漕溪北路 1 号");
        request.setPhone("021-10000000");
        StoreEntity saved = store(10L, "徐汇店", "OPEN");
        when(storeRepository.save(any(StoreEntity.class))).thenReturn(saved);

        StoreEntity result = storeService.createStore(request);

        ArgumentCaptor<StoreEntity> captor = ArgumentCaptor.forClass(StoreEntity.class);
        verify(storeRepository).save(captor.capture());
        StoreEntity toSave = captor.getValue();
        assertThat(toSave.getName()).isEqualTo("徐汇店");
        assertThat(toSave.getAddress()).isEqualTo("漕溪北路 1 号");
        assertThat(toSave.getPhone()).isEqualTo("021-10000000");
        assertThat(toSave.getStatus()).isEqualTo("OPEN");
        assertThat(result).isSameAs(saved);
    }

    @Test
    void updateStoreOnlyUpdatesNonNullFields() {
        StoreEntity existing = store(2L, "老店名", "OPEN");
        existing.setAddress("旧地址");
        existing.setPhone("021-11111111");
        existing.setOwnerName("老店主");
        existing.setOpeningTime(LocalTime.of(6, 0));
        existing.setClosingTime(LocalTime.of(18, 0));
        StoreRequest request = new StoreRequest();
        request.setName("新店名");
        request.setClosingTime(LocalTime.of(20, 30));
        when(storeRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(storeRepository.save(existing)).thenReturn(existing);

        Optional<StoreEntity> result = storeService.updateStore(2L, request);

        assertThat(result).contains(existing);
        assertThat(existing.getName()).isEqualTo("新店名");
        assertThat(existing.getAddress()).isEqualTo("旧地址");
        assertThat(existing.getPhone()).isEqualTo("021-11111111");
        assertThat(existing.getOwnerName()).isEqualTo("老店主");
        assertThat(existing.getOpeningTime()).isEqualTo(LocalTime.of(6, 0));
        assertThat(existing.getClosingTime()).isEqualTo(LocalTime.of(20, 30));
        verify(storeRepository).save(existing);
    }

    @Test
    void updateStoreReturnsEmptyWhenStoreDoesNotExist() {
        StoreRequest request = new StoreRequest();
        request.setName("不存在");
        when(storeRepository.findById(404L)).thenReturn(Optional.empty());

        Optional<StoreEntity> result = storeService.updateStore(404L, request);

        assertThat(result).isEmpty();
    }

    @Test
    void closeStoreSetsStatusToClosed() {
        StoreEntity existing = store(3L, "虹口店", "OPEN");
        when(storeRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(storeRepository.save(existing)).thenReturn(existing);

        boolean result = storeService.closeStore(3L);

        assertThat(result).isTrue();
        assertThat(existing.getStatus()).isEqualTo("CLOSED");
        verify(storeRepository).save(existing);
    }

    @Test
    void closeStoreReturnsFalseWhenStoreDoesNotExist() {
        when(storeRepository.findById(404L)).thenReturn(Optional.empty());

        boolean result = storeService.closeStore(404L);

        assertThat(result).isFalse();
    }

    private StoreEntity store(Long id, String name, String status) {
        StoreEntity store = new StoreEntity();
        store.setId(id);
        store.setName(name);
        store.setStatus(status);
        return store;
    }
}
