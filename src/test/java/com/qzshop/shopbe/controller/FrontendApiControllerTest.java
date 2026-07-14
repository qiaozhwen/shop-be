package com.qzshop.shopbe.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FrontendApiControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FrontendApiController()).build();
    }

    @Test
    void dashboardSummaryMatchesFrontendShape() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.todaySales").exists())
                .andExpect(jsonPath("$.data.lowStockCount").isNumber())
                .andExpect(jsonPath("$.data.salesTrend", hasSize(7)))
                .andExpect(jsonPath("$.data.categoryRanking").isArray())
                .andExpect(jsonPath("$.data.stockByStore").isArray());
    }

    @Test
    void categoryListReturnsPageResult() throws Exception {
        mockMvc.perform(get("/api/poultry-categories?page=1&pageSize=2&keyword=鸡"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list", hasSize(2)));

        mockMvc.perform(post("/api/poultry-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "CAT-001",
                                  "name": "鲜禽品类",
                                  "species": "鸡",
                                  "unit": "JIN",
                                  "basePrice": 18,
                                  "processingFee": 3,
                                  "avgWeight": 3,
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1001))
                .andExpect(jsonPath("$.data.name").value("鲜禽品类"));

        mockMvc.perform(put("/api/poultry-categories/1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"basePrice\":19}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.basePrice").value(19));

        mockMvc.perform(delete("/api/poultry-categories/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void creatingOrderGeneratesProcessingTaskForProcessedItem() throws Exception {
        mockMvc.perform(post("/api/sales-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": 1,
                                  "customerPhone": "13800001111",
                                  "payMethod": "WECHAT",
                                  "items": [
                                    {
                                      "categoryId": 11,
                                      "categoryName": "鲜禽品类",
                                      "quantity": 1,
                                      "weight": 3.2,
                                      "unitPrice": 18.8,
                                      "processMethod": "EVISCERATE",
                                      "processFee": 3,
                                      "subtotal": 63.16
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.totalAmount").value(63.16))
                .andExpect(jsonPath("$.data.items[0].id").exists());

        mockMvc.perform(get("/api/processing-tasks?keyword=三黄鸡&pageSize=50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    void advancingProcessingTaskMovesToNextStatus() throws Exception {
        mockMvc.perform(post("/api/sales-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": 1,
                                  "storeName": "门店",
                                  "payMethod": "WECHAT",
                                  "items": [
                                    {
                                      "categoryId": 11,
                                      "categoryName": "鲜禽品类",
                                      "quantity": 1,
                                      "weight": 3.2,
                                      "unitPrice": 18.8,
                                      "processMethod": "EVISCERATE",
                                      "processFee": 3,
                                      "subtotal": 63.16
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/processing-tasks/1003/advance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("SLAUGHTERING"))
                .andExpect(jsonPath("$.data.startedAt").exists());
    }

    @Test
    void operationalCrudEndpointsMatchFrontendModules() throws Exception {
        mockMvc.perform(get("/api/inventory?page=1&pageSize=5&storeId=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total").value(2));

        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": 1,
                                  "storeName": "门店",
                                  "categoryId": 11,
                                  "categoryName": "鲜禽品类",
                                  "batchNo": "BTEST",
                                  "quantity": 5,
                                  "avgWeight": 3.1,
                                  "health": "HEALTHY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.totalWeight").value(15.5));

        mockMvc.perform(post("/api/inventory/1001/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"delta\":-1,\"reason\":\"盘点调整\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(4));

        mockMvc.perform(get("/api/suppliers?page=1&pageSize=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list").isArray());

        mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"测试供应商\",\"contact\":\"王总\",\"phone\":\"13900009999\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("测试供应商"))
                .andExpect(jsonPath("$.data.level").value("C"))
                .andExpect(jsonPath("$.data.enabled").value(true));

        mockMvc.perform(put("/api/suppliers/1002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contact\":\"黄经理\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contact").value("黄经理"));

        mockMvc.perform(delete("/api/suppliers/1002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/members?page=1&pageSize=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list").isArray());

        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"测试会员\",\"phone\":\"13811112222\",\"level\":\"REGULAR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.points").value(0))
                .andExpect(jsonPath("$.data.registeredAt").exists());

        mockMvc.perform(put("/api/members/1003")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"level\":\"PLATINUM\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.level").value("PLATINUM"));

        mockMvc.perform(get("/api/staff?page=1&pageSize=5&storeId=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list").isArray());

        mockMvc.perform(post("/api/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"测试员工\",\"phone\":\"13822223333\",\"role\":\"CASHIER\",\"storeId\":1,\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("测试员工"));

        mockMvc.perform(put("/api/staff/1004")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"));

        mockMvc.perform(delete("/api/staff/1004"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/pricing?page=1&pageSize=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total").value(3));
    }

    @Test
    void procurementAndLossActionsUpdateOperationalData() throws Exception {
        mockMvc.perform(post("/api/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "supplierId": 51,
                                  "supplierName": "供应商",
                                  "storeId": 1,
                                  "storeName": "门店",
                                  "categoryName": "鲜禽品类",
                                  "quantity": 12,
                                  "totalWeight": 54.0,
                                  "unitPrice": 20.0,
                                  "batchNo": "BTEST-PO"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.amount").value(1080.0));

        mockMvc.perform(post("/api/purchases/1001/receive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RECEIVED"))
                .andExpect(jsonPath("$.data.receivedAt").exists());

        mockMvc.perform(get("/api/inventory?keyword=BTEST-PO&pageSize=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].quantity").value(12));

        mockMvc.perform(post("/api/losses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": 1,
                                  "storeName": "门店",
                                  "categoryName": "鲜禽品类",
                                  "batchNo": "B20260601-01",
                                  "quantity": 1,
                                  "reason": "DEAD",
                                  "handler": "刘师傅",
                                  "occurredAt": "2026-07-03 09:00:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists());

        mockMvc.perform(get("/api/losses?keyword=刘师傅&pageSize=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));
    }
}
