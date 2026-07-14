package com.qzshop.shopbe.operations;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.qzshop.shopbe.auth.staff.StaffRepository;
import com.qzshop.shopbe.controller.FrontendApiController;
import com.qzshop.shopbe.dao.StoreRepository;

@SpringBootTest
@ActiveProfiles("test")
class OperationalPersistenceIT {

    @Autowired FrontendApiController controller;
    @Autowired JdbcTemplate jdbc;
    @Autowired StaffRepository staffRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired OperationalStateService stateStore;
    @Autowired StoreRepository storeRepository;

    @Test
    void masterDataMutationIsPersistedImmediately() {
        controller.createCategory(Map.of(
                "code", "PERSIST-001",
                "name", "持久化品类",
                "species", "鸡",
                "basePrice", 20));

        String payload = jdbc.queryForObject(
                "select payload from operational_state where id = 1",
                String.class);
        assertThat(payload).contains("PERSIST-001", "持久化品类");

        FrontendApiController restarted = new FrontendApiController(
                stateStore,
                staffRepository,
                passwordEncoder,
                storeRepository);
        Map<String, Object> page = data(restarted.listCategories(Map.of("keyword", "PERSIST-001")));
        assertThat(page.get("total")).isEqualTo(1);
    }

    @Test
    void inventoryAdjustmentRejectsNegativeStock() {
        Map<String, Object> created = data(controller.createInventory(Map.of(
                "storeId", 1,
                "storeName", "总店",
                "categoryId", 11,
                "categoryName", "三黄鸡",
                "batchNo", "NEGATIVE-STOCK",
                "quantity", 2,
                "avgWeight", 3.0)));

        Map<String, Object> response = controller.adjustInventory(
                number(created.get("id")), Map.of("delta", -3));

        assertThat(response.get("code")).isEqualTo(409);
    }

    @Test
    void purchaseCanOnlyBeReceivedOnce() {
        Map<String, Object> purchase = data(controller.createPurchase(Map.of(
                "supplierId", 51,
                "supplierName", "广丰养殖",
                "storeId", 1,
                "storeName", "总店",
                "categoryName", "三黄鸡",
                "quantity", 5,
                "totalWeight", 15.0,
                "unitPrice", 10,
                "batchNo", "RECEIVE-ONCE")));
        long id = number(purchase.get("id"));

        assertThat(controller.receivePurchase(id).get("code")).isEqualTo(200);
        assertThat(controller.receivePurchase(id).get("code")).isEqualTo(409);
    }

    @Test
    void lossCreationDecrementsMatchingInventory() {
        Map<String, Object> inventory = data(controller.createInventory(Map.of(
                "storeId", 1,
                "storeName", "总店",
                "categoryId", 11,
                "categoryName", "三黄鸡",
                "batchNo", "LOSS-BATCH",
                "quantity", 5,
                "avgWeight", 3.0)));

        Map<String, Object> response = controller.createLoss(Map.of(
                "inventoryId", inventory.get("id"),
                "storeId", 1,
                "categoryName", "三黄鸡",
                "batchNo", "LOSS-BATCH",
                "quantity", 2,
                "reason", "DEAD"));

        assertThat(response.get("code")).isEqualTo(200);
        Map<String, Object> adjusted = findInventory(number(inventory.get("id")));
        assertThat(adjusted.get("quantity")).isEqualTo(3);
    }

    @Test
    void staffCreatedInManagementModuleCanAuthenticateWithItsInitialPassword() {
        String phone = "13900007777";
        Map<String, Object> created = data(controller.createStaff(Map.of(
                "name", "新员工",
                "phone", phone,
                "role", "CASHIER",
                "storeId", 1,
                "password", "InitialPass1",
                "hireDate", "2026-07-14",
                "remark", "晚班",
                "enabled", true)));

        var staff = staffRepository.findByPhone(phone).orElseThrow();
        assertThat(staff.getRole()).isEqualTo("CASHIER");
        assertThat(staff.getStatus()).isEqualTo("ACTIVE");
        assertThat(passwordEncoder.matches("InitialPass1", staff.getPassword())).isTrue();
        assertThat(created.get("hireDate")).isEqualTo("2026-07-14");
        assertThat(created.get("remark")).isEqualTo("晚班");
    }

    @Test
    void salesOrderAtomicallyDecrementsInventoryAndRejectsOverselling() {
        Map<String, Object> inventory = data(controller.createInventory(Map.of(
                "storeId", 1,
                "storeName", "总店",
                "categoryId", 14,
                "categoryName", "白条鹅",
                "batchNo", "SALE-BATCH",
                "quantity", 3,
                "avgWeight", 7.5)));
        Map<String, Object> item = Map.of(
                "categoryId", 14,
                "categoryName", "白条鹅",
                "quantity", 2,
                "weight", 15.0,
                "unitPrice", 0.01,
                "processMethod", "ALIVE",
                "processFee", 0,
                "subtotal", 0.01);

        Map<String, Object> sale = controller.createOrder(Map.of(
                "storeId", 1,
                "payMethod", "CASH",
                "items", List.of(item)));

        assertThat(sale.get("code")).isEqualTo(200);
        Map<String, Object> storedSale = data(sale);
        assertThat(storedSale.get("totalAmount")).isEqualTo(330.0);
        assertThat(findInventory(number(inventory.get("id"))).get("quantity")).isEqualTo(1);

        Map<String, Object> rejected = controller.createOrder(Map.of(
                "storeId", 1,
                "payMethod", "CASH",
                "items", List.of(item)));
        assertThat(rejected.get("code")).isEqualTo(409);
        assertThat(findInventory(number(inventory.get("id"))).get("quantity")).isEqualTo(1);
    }

    @Test
    void masterDataRejectsDuplicateIdentifiersAndInvalidPricing() {
        assertThat(controller.createCategory(Map.of(
                "code", "P001", "name", "重复品类", "basePrice", 10)).get("code"))
                .isEqualTo(409);

        controller.createMember(Map.of("name", "会员甲", "phone", "13900006666", "level", "BRONZE"));
        assertThat(controller.createMember(Map.of(
                "name", "会员乙", "phone", "13900006666", "level", "BRONZE")).get("code"))
                .isEqualTo(409);

        controller.createStaff(Map.of(
                "name", "员工甲",
                "phone", "13900005555",
                "role", "HELPER",
                "storeId", 1,
                "password", "InitialPass1"));
        assertThat(controller.createStaff(Map.of(
                "name", "员工乙",
                "phone", "13900005555",
                "role", "HELPER",
                "storeId", 1,
                "password", "InitialPass1")).get("code"))
                .isEqualTo(409);

        assertThat(controller.updatePricing(101, Map.of("price", -1)).get("code"))
                .isEqualTo(400);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findInventory(long id) {
        Map<String, Object> page = data(controller.listInventory(Map.of("pageSize", "200")));
        return ((List<Map<String, Object>>) page.get("list")).stream()
                .filter(item -> number(item.get("id")) == id)
                .findFirst()
                .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(Map<String, Object> response) {
        return (Map<String, Object>) response.get("data");
    }

    private long number(Object value) {
        return ((Number) value).longValue();
    }
}
