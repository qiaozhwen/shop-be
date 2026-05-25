package com.qzshop.shopbe.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.qzshop.shopbe.dto.StoreRequest;
import com.qzshop.shopbe.entity.StoreEntity;
import com.qzshop.shopbe.service.StoreService;

class StoreControllerTest {

    private MockMvc mockMvc;

    private FakeStoreService storeService;

    @BeforeEach
    void setUp() {
        storeService = new FakeStoreService();
        StoreController controller = new StoreController(storeService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getStoresReturnsOpenStoresFromApiPath() throws Exception {
        StoreEntity store = store(1L, "虹口店", "OPEN");
        storeService.allStores = List.of(store);

        mockMvc.perform(get("/api/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("虹口店"))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    void getStoreByIdReturns404WhenStoreDoesNotExist() throws Exception {
        storeService.storeById = Optional.empty();

        mockMvc.perform(get("/api/stores/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Store not found"));
    }

    @Test
    void createStoreReturnsCreatedStoreWithId() throws Exception {
        StoreEntity created = store(10L, "徐汇店", "OPEN");
        storeService.createdStore = created;

        mockMvc.perform(post("/api/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "徐汇店",
                                  "address": "漕溪北路 1 号",
                                  "phone": "021-10000000"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("徐汇店"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void createStoreReturnsBadRequestWhenNameIsBlank() throws Exception {
        mockMvc.perform(post("/api/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "   ",
                                  "address": "漕溪北路 1 号",
                                  "phone": "021-10000000"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStoreReturnsUpdatedStoreAndLeavesMissingFieldsToService() throws Exception {
        StoreEntity updated = store(3L, "静安新店", "OPEN");
        storeService.updatedStore = Optional.of(updated);

        mockMvc.perform(put("/api/stores/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "静安新店"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("静安新店"));
    }

    @Test
    void updateStoreAllowsPartialRequestWithoutName() throws Exception {
        StoreEntity updated = store(3L, "静安店", "OPEN");
        updated.setAddress("南京西路 1 号");
        storeService.updatedStore = Optional.of(updated);

        mockMvc.perform(put("/api/stores/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "address": "南京西路 1 号"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.address").value("南京西路 1 号"));
    }

    @Test
    void updateStoreRejectsBlankNameWhenNameIsProvided() throws Exception {
        mockMvc.perform(put("/api/stores/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "   "
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStoreReturns404WhenStoreDoesNotExist() throws Exception {
        storeService.updatedStore = Optional.empty();

        mockMvc.perform(put("/api/stores/404")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "不存在"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Store not found"));
    }

    @Test
    void deleteStoreSoftDeletesExistingStore() throws Exception {
        storeService.closeResult = true;

        mockMvc.perform(delete("/api/stores/5"))
                .andExpect(status().isNoContent());

        assertThat(storeService.closedStoreId).isEqualTo(5L);
    }

    @Test
    void deleteStoreReturns404WhenStoreDoesNotExist() throws Exception {
        storeService.closeResult = false;

        mockMvc.perform(delete("/api/stores/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Store not found"));
    }

    private StoreEntity store(Long id, String name, String status) {
        StoreEntity store = new StoreEntity();
        store.setId(id);
        store.setName(name);
        store.setStatus(status);
        return store;
    }

    private static class FakeStoreService extends StoreService {
        private List<StoreEntity> allStores = List.of();
        private Optional<StoreEntity> storeById = Optional.empty();
        private StoreEntity createdStore;
        private Optional<StoreEntity> updatedStore = Optional.empty();
        private boolean closeResult;
        private Long closedStoreId;

        FakeStoreService() {
            super(null);
        }

        @Override
        public List<StoreEntity> getAllStores() {
            return allStores;
        }

        @Override
        public Optional<StoreEntity> getStoreById(Long id) {
            return storeById;
        }

        @Override
        public StoreEntity createStore(StoreRequest request) {
            return createdStore;
        }

        @Override
        public Optional<StoreEntity> updateStore(Long id, StoreRequest request) {
            return updatedStore;
        }

        @Override
        public boolean closeStore(Long id) {
            closedStoreId = id;
            return closeResult;
        }
    }
}
