package com.qzshop.shopbe.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.qzshop.shopbe.auth.staff.StaffEntity;
import com.qzshop.shopbe.auth.staff.StaffRepository;
import com.qzshop.shopbe.dao.StoreRepository;
import com.qzshop.shopbe.operations.OperationalStateService;
import com.qzshop.shopbe.operations.LegacyOperationalStateMigrator;

@RestController
@RequestMapping("/api")
public class FrontendApiController {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final List<String> PROCESSING_FLOW = List.of(
            "WAIT_SLAUGHTER", "SLAUGHTERING", "PLUCKING", "EVISCERATING", "PACKING", "DELIVERED");

    private final AtomicLong ids = new AtomicLong(1000);
    private final List<Map<String, Object>> categories = new ArrayList<>();
    private final List<Map<String, Object>> inventory = new ArrayList<>();
    private final List<Map<String, Object>> orders = new ArrayList<>();
    private final List<Map<String, Object>> processingTasks = new ArrayList<>();
    private final List<Map<String, Object>> suppliers = new ArrayList<>();
    private final List<Map<String, Object>> purchases = new ArrayList<>();
    private final List<Map<String, Object>> losses = new ArrayList<>();
    private final List<Map<String, Object>> members = new ArrayList<>();
    private final List<Map<String, Object>> staff = new ArrayList<>();
    private final List<Map<String, Object>> pricing = new ArrayList<>();
    private final OperationalStateService stateStore;
    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;
    private final StoreRepository storeRepository;

    public FrontendApiController() {
        this.stateStore = null;
        this.staffRepository = null;
        this.passwordEncoder = null;
        this.storeRepository = null;
        seed();
    }

    public FrontendApiController(OperationalStateService stateStore,
                                 StaffRepository staffRepository,
                                 PasswordEncoder passwordEncoder,
                                 StoreRepository storeRepository) {
        this(stateStore, staffRepository, passwordEncoder, storeRepository, null, false);
    }

    @Autowired
    public FrontendApiController(OperationalStateService stateStore,
                                 StaffRepository staffRepository,
                                 PasswordEncoder passwordEncoder,
                                 StoreRepository storeRepository,
                                 LegacyOperationalStateMigrator legacyMigrator,
                                 @Value("${shop.operational.seed-demo:false}") boolean seedDemo) {
        this.stateStore = stateStore;
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
        this.storeRepository = storeRepository;
        stateStore.load().ifPresentOrElse(this::restore, () -> {
            var legacyState = legacyMigrator == null
                    ? java.util.Optional.<OperationalStateService.State>empty()
                    : legacyMigrator.migrate();
            if (legacyState.isPresent()) {
                restore(legacyState.orElseThrow());
            } else if (seedDemo) {
                seed();
            }
            persist();
        });
    }

    @GetMapping("/dashboard/summary")
    public synchronized Map<String, Object> dashboardSummary() {
        String today = LocalDate.now().toString();
        List<Map<String, Object>> todayOrders = orders.stream()
                .filter(order -> !List.of("CANCELED", "REFUNDED").contains(text(order.get("status"))))
                .filter(order -> text(order.get("createdAt")).startsWith(today))
                .toList();
        double sales = todayOrders.stream()
                .mapToDouble(order -> number(order.get("payable")).doubleValue())
                .sum();
        int stock = inventory.stream().mapToInt(item -> number(item.get("quantity")).intValue()).sum();
        int loss = losses.stream()
                .filter(item -> text(item.get("occurredAt")).startsWith(today))
                .mapToInt(item -> number(item.get("quantity")).intValue()).sum();
        long lowStock = inventory.stream()
                .filter(item -> number(item.get("quantity")).intValue() < 20)
                .map(item -> number(item.get("categoryId")).longValue())
                .distinct()
                .count();
        long pending = processingTasks.stream()
                .filter(task -> !List.of("DELIVERED", "CANCELED").contains(text(task.get("status"))))
                .count();
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int offset = -6; offset <= 0; offset++) {
            String date = LocalDate.now().plusDays(offset).toString();
            List<Map<String, Object>> daily = orders.stream()
                    .filter(order -> !List.of("CANCELED", "REFUNDED").contains(text(order.get("status"))))
                    .filter(order -> text(order.get("createdAt")).startsWith(date))
                    .toList();
            double dailySales = daily.stream().mapToDouble(order -> number(order.get("payable")).doubleValue()).sum();
            trend.add(item("date", date.substring(5), "sales", round(dailySales), "orders", daily.size()));
        }
        Map<String, Double> categorySales = new HashMap<>();
        for (Map<String, Object> order : orders) {
            if (List.of("CANCELED", "REFUNDED").contains(text(order.get("status")))) continue;
            for (Map<String, Object> orderItem : mapList(order.get("items"))) {
                categorySales.merge(
                        text(orderItem.get("categoryName")),
                        number(orderItem.get("subtotal")).doubleValue(),
                        Double::sum);
            }
        }
        List<Map<String, Object>> ranking = categorySales.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .map(entry -> item("name", entry.getKey(), "sales", round(entry.getValue())))
                .toList();
        List<Map<String, Object>> stockByStore = List.of(
                item("store", "总店·城北店", "quantity", stockForStore(1)),
                item("store", "城南旗舰店", "quantity", stockForStore(2)),
                item("store", "高新区便民店", "quantity", stockForStore(4)));
        return ok(item(
                "todaySales", round(sales),
                "todayOrders", todayOrders.size(),
                "poultryStock", stock,
                "todayLoss", loss,
                "lowStockCount", lowStock,
                "processingPending", pending,
                "memberCount", members.size(),
                "storeCount", storeRepository == null ? 4 : storeRepository.findByStatusNot("CLOSED").size(),
                "staffCount", staffRepository == null ? staff.size() : staffRepository.count(),
                "salesTrend", trend,
                "categoryRanking", ranking,
                "stockByStore", stockByStore));
    }

    @GetMapping("/poultry-categories")
    public synchronized Map<String, Object> listCategories(@RequestParam Map<String, String> params) {
        return ok(page(filter(categories, params.get("keyword"), "name", "code", "species"), params));
    }

    @PostMapping("/poultry-categories")
    public synchronized Map<String, Object> createCategory(@RequestBody Map<String, Object> body) {
        String code = text(body.get("code"));
        if (!code.isBlank() && categories.stream().anyMatch(row -> code.equals(row.get("code")))) {
            return fail(409, "品类编码已存在");
        }
        if (number(body.get("basePrice")).doubleValue() < 0) {
            return fail(400, "价格不能为负数");
        }
        Map<String, Object> item = copy(body);
        item.put("id", ids.incrementAndGet());
        categories.add(0, item);
        pricing.add(0, item(
                "id", item.get("id"),
                "categoryId", item.get("id"),
                "categoryName", item.get("name"),
                "storeId", 0,
                "storeName", "全部门店",
                "date", LocalDateTime.now().format(DATE),
                "price", number(item.get("basePrice")).doubleValue(),
                "processingFee", number(item.get("processingFee")).doubleValue()));
        persist();
        return ok(item);
    }

    @PutMapping("/poultry-categories/{id}")
    public synchronized Map<String, Object> updateCategory(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return update(categories, id, body, "品类不存在");
    }

    @DeleteMapping("/poultry-categories/{id}")
    public synchronized Map<String, Object> deleteCategory(@PathVariable long id) {
        remove(categories, id);
        return ok(null);
    }

    @GetMapping("/inventory")
    public synchronized Map<String, Object> listInventory(@RequestParam Map<String, String> params) {
        List<Map<String, Object>> list = new ArrayList<>(inventory);
        if (params.containsKey("storeId")) {
            list = list.stream().filter(item -> number(item.get("storeId")).longValue() == Long.parseLong(params.get("storeId"))).toList();
        }
        if (params.containsKey("categoryId")) {
            list = list.stream().filter(item -> number(item.get("categoryId")).longValue() == Long.parseLong(params.get("categoryId"))).toList();
        }
        return ok(page(filter(list, params.get("keyword"), "categoryName", "storeName", "batchNo"), params));
    }

    @PostMapping("/inventory")
    public synchronized Map<String, Object> createInventory(@RequestBody Map<String, Object> body) {
        if (number(body.get("quantity")).intValue() < 0 || number(body.get("avgWeight")).doubleValue() < 0) {
            return fail(400, "库存数量和重量不能为负数");
        }
        Map<String, Object> item = copy(body);
        item.put("id", ids.incrementAndGet());
        item.put("totalWeight", round(number(item.get("quantity")).doubleValue() * number(item.get("avgWeight")).doubleValue()));
        inventory.add(0, item);
        persist();
        return ok(item);
    }

    @PostMapping("/inventory/{id}/adjust")
    public synchronized Map<String, Object> adjustInventory(@PathVariable long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> item = find(inventory, id);
        if (item == null) return fail(404, "库存不存在");
        int next = number(item.get("quantity")).intValue() + number(body.get("delta")).intValue();
        if (next < 0) return fail(409, "库存不足");
        item.put("quantity", next);
        item.put("totalWeight", round(next * number(item.get("avgWeight")).doubleValue()));
        persist();
        return ok(item);
    }

    @DeleteMapping("/inventory/{id}")
    public synchronized Map<String, Object> deleteInventory(@PathVariable long id) {
        remove(inventory, id);
        return ok(null);
    }

    @GetMapping("/sales-orders")
    public synchronized Map<String, Object> listOrders(@RequestParam Map<String, String> params) {
        List<Map<String, Object>> list = new ArrayList<>(orders);
        if (params.containsKey("status")) {
            list = list.stream().filter(order -> params.get("status").equals(order.get("status"))).toList();
        }
        if (params.containsKey("storeId")) {
            list = list.stream().filter(order -> number(order.get("storeId")).longValue() == Long.parseLong(params.get("storeId"))).toList();
        }
        list = filter(list, params.get("keyword"), "orderNo", "storeName", "customerPhone");
        list = list.stream().sorted(Comparator.comparing(order -> text(order.get("createdAt")), Comparator.reverseOrder())).toList();
        return ok(page(list, params));
    }

    @GetMapping("/sales-orders/{id}")
    public synchronized Map<String, Object> getOrder(@PathVariable long id) {
        Map<String, Object> item = find(orders, id);
        return item == null ? fail(404, "订单不存在") : ok(item);
    }

    @PostMapping("/sales-orders")
    public synchronized Map<String, Object> createOrder(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.getOrDefault("items", List.of());
        if (items.isEmpty()) return fail(400, "订单明细不能为空");
        long storeId = number(body.getOrDefault("storeId", 1)).longValue();
        List<Map<String, Object>> pricedItems = new ArrayList<>();
        for (Map<String, Object> requestedItem : items) {
            long categoryId = number(requestedItem.get("categoryId")).longValue();
            Map<String, Object> category = find(categories, categoryId);
            if (category == null || Boolean.FALSE.equals(category.get("enabled"))) {
                return fail(400, "销售品类不存在或已停用");
            }
            int quantity = number(requestedItem.get("quantity")).intValue();
            double weight = number(requestedItem.get("weight")).doubleValue();
            if (quantity <= 0 || ("JIN".equals(text(category.get("unit"))) && weight <= 0)) {
                return fail(400, "销售数量和重量必须大于零");
            }
            Map<String, Object> price = pricing.stream()
                    .filter(row -> number(row.get("categoryId")).longValue() == categoryId)
                    .filter(row -> number(row.get("storeId")).longValue() == storeId)
                    .findFirst()
                    .orElseGet(() -> pricing.stream()
                            .filter(row -> number(row.get("categoryId")).longValue() == categoryId)
                            .filter(row -> number(row.get("storeId")).longValue() == 0)
                            .findFirst().orElse(null));
            double unitPrice = number(price == null ? category.get("basePrice") : price.get("price")).doubleValue();
            String method = text(requestedItem.getOrDefault("processMethod", "ALIVE"));
            double processingFee = "ALIVE".equals(method) ? 0
                    : number(price == null ? category.get("processingFee") : price.get("processingFee")).doubleValue();
            double goodsAmount = "PIECE".equals(text(category.get("unit")))
                    ? quantity * unitPrice : weight * unitPrice;
            Map<String, Object> priced = copy(requestedItem);
            priced.put("categoryName", category.get("name"));
            priced.put("unitPrice", round(unitPrice));
            priced.put("processFee", round(processingFee * quantity));
            priced.put("subtotal", round(goodsAmount + processingFee * quantity));
            pricedItems.add(priced);
        }
        double total = pricedItems.stream().mapToDouble(item -> number(item.get("subtotal")).doubleValue()).sum();
        double discount = number(body.getOrDefault("discount", 0)).doubleValue();
        if (discount < 0 || discount > total) return fail(400, "优惠金额无效");
        Map<Long, Integer> requested = new HashMap<>();
        for (Map<String, Object> orderItem : pricedItems) {
            int quantity = number(orderItem.get("quantity")).intValue();
            if (quantity <= 0) return fail(400, "销售数量必须大于零");
            requested.merge(number(orderItem.get("categoryId")).longValue(), quantity, Integer::sum);
        }
        for (Map.Entry<Long, Integer> entry : requested.entrySet()) {
            int available = inventory.stream()
                    .filter(stock -> number(stock.get("storeId")).longValue() == storeId)
                    .filter(stock -> number(stock.get("categoryId")).longValue() == entry.getKey())
                    .mapToInt(stock -> number(stock.get("quantity")).intValue())
                    .sum();
            if (available < entry.getValue()) return fail(409, "库存不足");
        }
        requested.forEach((categoryId, quantity) -> decrementInventory(storeId, categoryId, quantity));
        Map<String, Object> order = item(
                "id", ids.incrementAndGet(),
                "orderNo", "SO" + System.currentTimeMillis(),
                "storeId", storeId,
                "storeName", storeName(storeId),
                "items", withIds(pricedItems),
                "totalAmount", round(total),
                "discount", round(discount),
                "payable", round(total - discount),
                "payMethod", body.get("payMethod"),
                "memberId", body.get("memberId"),
                "customerPhone", body.get("customerPhone"),
                "status", body.get("payMethod") == null ? "PENDING" : "PAID",
                "createdAt", now(),
                "paidAt", body.get("payMethod") == null ? null : now(),
                "remark", body.get("remark"));
        orders.add(0, order);
        for (Map<String, Object> orderItem : pricedItems) {
            if (!"ALIVE".equals(orderItem.get("processMethod"))) {
                processingTasks.add(0, item(
                        "id", ids.incrementAndGet(),
                        "taskNo", "PT" + System.currentTimeMillis(),
                        "orderNo", order.get("orderNo"),
                        "storeName", order.get("storeName"),
                        "categoryName", orderItem.get("categoryName"),
                        "quantity", orderItem.get("quantity"),
                        "weight", orderItem.get("weight"),
                        "methods", List.of(orderItem.get("processMethod")),
                        "status", "WAIT_SLAUGHTER",
                        "priority", "NORMAL",
                        "createdAt", order.get("createdAt")));
            }
        }
        persist();
        return ok(order);
    }

    private void decrementInventory(long storeId, long categoryId, int quantity) {
        int remaining = quantity;
        for (Map<String, Object> stock : inventory) {
            if (remaining == 0) break;
            if (number(stock.get("storeId")).longValue() != storeId
                    || number(stock.get("categoryId")).longValue() != categoryId) continue;
            int current = number(stock.get("quantity")).intValue();
            int used = Math.min(current, remaining);
            int next = current - used;
            stock.put("quantity", next);
            stock.put("totalWeight", round(next * number(stock.get("avgWeight")).doubleValue()));
            remaining -= used;
        }
    }

    @PutMapping("/sales-orders/{id}/status")
    public synchronized Map<String, Object> updateOrderStatus(@PathVariable long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> item = find(orders, id);
        if (item == null) return fail(404, "订单不存在");
        item.put("status", body.get("status"));
        if ("COMPLETED".equals(body.get("status"))) item.put("completedAt", now());
        persist();
        return ok(item);
    }

    @GetMapping("/processing-tasks")
    public synchronized Map<String, Object> listProcessing(@RequestParam Map<String, String> params) {
        List<Map<String, Object>> list = new ArrayList<>(processingTasks);
        if (params.containsKey("status")) {
            list = list.stream().filter(task -> params.get("status").equals(task.get("status"))).toList();
        }
        if (Boolean.parseBoolean(params.getOrDefault("active", "false"))) {
            list = list.stream()
                    .filter(task -> !List.of("DELIVERED", "CANCELED").contains(text(task.get("status"))))
                    .toList();
        }
        return ok(page(filter(list, params.get("keyword"), "taskNo", "orderNo", "workerName", "categoryName"), params));
    }

    @PostMapping("/processing-tasks/{id}/advance")
    public synchronized Map<String, Object> advanceProcessing(@PathVariable long id) {
        Map<String, Object> item = find(processingTasks, id);
        if (item == null) return fail(404, "任务不存在");
        int idx = PROCESSING_FLOW.indexOf(text(item.get("status")));
        if (idx >= 0 && idx < PROCESSING_FLOW.size() - 1) {
            String next = PROCESSING_FLOW.get(idx + 1);
            item.put("status", next);
            if ("DELIVERED".equals(next)) item.put("finishedAt", now());
            else if (item.get("startedAt") == null) item.put("startedAt", now());
        }
        persist();
        return ok(item);
    }

    @PostMapping("/processing-tasks/{id}/assign")
    public synchronized Map<String, Object> assignProcessing(@PathVariable long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> item = find(processingTasks, id);
        if (item == null) return fail(404, "任务不存在");
        item.put("workerId", body.get("workerId"));
        item.put("workerName", body.get("workerName"));
        persist();
        return ok(item);
    }

    @GetMapping("/suppliers")
    public synchronized Map<String, Object> listSuppliers(@RequestParam Map<String, String> params) {
        return ok(page(filter(suppliers, params.get("keyword"), "name", "contact", "phone", "category"), params));
    }

    @PostMapping("/suppliers")
    public synchronized Map<String, Object> createSupplier(@RequestBody Map<String, Object> body) {
        Map<String, Object> item = copy(body);
        item.put("id", ids.incrementAndGet());
        item.putIfAbsent("level", "C");
        item.putIfAbsent("enabled", true);
        suppliers.add(0, item);
        persist();
        return ok(item);
    }

    @PutMapping("/suppliers/{id}")
    public synchronized Map<String, Object> updateSupplier(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return update(suppliers, id, body, "供应商不存在");
    }

    @DeleteMapping("/suppliers/{id}")
    public synchronized Map<String, Object> deleteSupplier(@PathVariable long id) {
        remove(suppliers, id);
        return ok(null);
    }

    @GetMapping("/purchases")
    public synchronized Map<String, Object> listPurchases(@RequestParam Map<String, String> params) {
        return ok(page(filter(purchases, params.get("keyword"), "orderNo", "supplierName", "storeName", "categoryName", "batchNo"), params));
    }

    @PostMapping("/purchases")
    public synchronized Map<String, Object> createPurchase(@RequestBody Map<String, Object> body) {
        if (number(body.get("quantity")).intValue() <= 0 || number(body.get("totalWeight")).doubleValue() <= 0) {
            return fail(400, "采购数量和重量必须大于零");
        }
        Map<String, Object> item = copy(body);
        item.put("id", ids.incrementAndGet());
        item.put("orderNo", "PO" + System.currentTimeMillis());
        item.put("amount", round(number(item.get("unitPrice")).doubleValue() * number(item.get("totalWeight")).doubleValue()));
        item.put("status", "SUBMITTED");
        item.put("createdAt", now());
        purchases.add(0, item);
        persist();
        return ok(item);
    }

    @PostMapping("/purchases/{id}/receive")
    public synchronized Map<String, Object> receivePurchase(@PathVariable long id) {
        Map<String, Object> item = find(purchases, id);
        if (item == null) return fail(404, "采购单不存在");
        if ("RECEIVED".equals(item.get("status"))) return fail(409, "采购单已入库");
        item.put("status", "RECEIVED");
        item.put("receivedAt", now());
        long storeId = number(item.get("storeId")).longValue();
        int quantity = number(item.get("quantity")).intValue();
        double totalWeight = number(item.get("totalWeight")).doubleValue();
        inventory.add(0, item(
                "id", ids.incrementAndGet(),
                "storeId", storeId,
                "storeName", item.get("storeName"),
                "categoryId", categoryIdByName(text(item.get("categoryName"))),
                "categoryName", item.get("categoryName"),
                "batchNo", item.get("batchNo"),
                "quantity", quantity,
                "avgWeight", round(totalWeight / Math.max(1, quantity)),
                "totalWeight", round(totalWeight),
                "health", "HEALTHY",
                "inStockAt", now(),
                "supplierName", item.get("supplierName")));
        persist();
        return ok(item);
    }

    @GetMapping("/losses")
    public synchronized Map<String, Object> listLosses(@RequestParam Map<String, String> params) {
        return ok(page(filter(losses, params.get("keyword"), "storeName", "categoryName", "handler", "batchNo"), params));
    }

    @PostMapping("/losses")
    public synchronized Map<String, Object> createLoss(@RequestBody Map<String, Object> body) {
        int quantity = number(body.get("quantity")).intValue();
        if (quantity <= 0) return fail(400, "损耗数量必须大于零");
        Map<String, Object> stock = body.get("inventoryId") == null
                ? inventory.stream()
                    .filter(row -> text(body.get("batchNo")).equals(text(row.get("batchNo"))))
                    .findFirst().orElse(null)
                : find(inventory, number(body.get("inventoryId")).longValue());
        if (stock == null) return fail(404, "库存不存在");
        int next = number(stock.get("quantity")).intValue() - quantity;
        if (next < 0) return fail(409, "库存不足");
        stock.put("quantity", next);
        stock.put("totalWeight", round(next * number(stock.get("avgWeight")).doubleValue()));
        Map<String, Object> item = copy(body);
        item.put("id", ids.incrementAndGet());
        item.putIfAbsent("occurredAt", now());
        losses.add(0, item);
        persist();
        return ok(item);
    }

    @GetMapping("/members")
    public synchronized Map<String, Object> listMembers(@RequestParam Map<String, String> params) {
        return ok(page(filter(members, params.get("keyword"), "name", "phone", "cardNo"), params));
    }

    @PostMapping("/members")
    public synchronized Map<String, Object> createMember(@RequestBody Map<String, Object> body) {
        String phone = text(body.get("phone"));
        if (!phone.isBlank() && members.stream().anyMatch(row -> phone.equals(row.get("phone")))) {
            return fail(409, "会员手机号已存在");
        }
        Map<String, Object> item = copy(body);
        item.put("id", ids.incrementAndGet());
        item.put("points", 0);
        item.put("balance", 0);
        item.put("totalConsumption", 0);
        item.put("registeredAt", LocalDateTime.now().format(DATE));
        members.add(0, item);
        persist();
        return ok(item);
    }

    @PutMapping("/members/{id}")
    public synchronized Map<String, Object> updateMember(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return update(members, id, body, "会员不存在");
    }

    @GetMapping("/staff")
    public synchronized Map<String, Object> listStaff(@RequestParam Map<String, String> params) {
        List<Map<String, Object>> list = staffRepository == null
                ? new ArrayList<>(staff)
                : staffRepository.findAll().stream().map(this::staffView).toList();
        if (params.containsKey("role")) list = list.stream().filter(item -> params.get("role").equals(item.get("role"))).toList();
        if (params.containsKey("storeId")) list = list.stream().filter(item -> number(item.get("storeId")).longValue() == Long.parseLong(params.get("storeId"))).toList();
        return ok(page(filter(list, params.get("keyword"), "name", "phone"), params));
    }

    @PostMapping("/staff")
    public synchronized Map<String, Object> createStaff(@RequestBody Map<String, Object> body) {
        if (staffRepository != null) {
            String phone = text(body.get("phone"));
            if (phone.isBlank()) return fail(400, "员工手机号不能为空");
            if (staffRepository.findByPhone(phone).isPresent()) return fail(409, "员工手机号已存在");
            String suppliedPassword = text(body.get("password"));
            String initialPassword = suppliedPassword.isBlank()
                    ? UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                    : suppliedPassword;
            if (initialPassword.length() < 8) return fail(400, "初始密码至少 8 位");

            StaffEntity entity = new StaffEntity();
            entity.setStoreId(number(body.getOrDefault("storeId", 1)).longValue());
            entity.setName(text(body.get("name")));
            entity.setNickname(text(body.get("name")));
            entity.setPhone(phone);
            entity.setRole(text(body.getOrDefault("role", "STAFF_DEFAULT")));
            entity.setStatus(Boolean.FALSE.equals(body.get("enabled")) ? "DISABLED" : "ACTIVE");
            if (!text(body.get("hireDate")).isBlank()) entity.setHireDate(LocalDate.parse(text(body.get("hireDate"))));
            entity.setRemark(text(body.get("remark")));
            entity.setPassword(passwordEncoder.encode(initialPassword));
            Map<String, Object> result = staffView(staffRepository.saveAndFlush(entity));
            if (suppliedPassword.isBlank()) result.put("initialPassword", initialPassword);
            return ok(result);
        }
        Map<String, Object> item = copy(body);
        item.put("id", ids.incrementAndGet());
        staff.add(0, item);
        persist();
        return ok(item);
    }

    @PutMapping("/staff/{id}")
    public synchronized Map<String, Object> updateStaff(@PathVariable long id, @RequestBody Map<String, Object> body) {
        if (staffRepository != null) {
            StaffEntity entity = staffRepository.findById(id).orElse(null);
            if (entity == null) return fail(404, "员工不存在");
            if (body.containsKey("name")) entity.setName(text(body.get("name")));
            if (body.containsKey("phone")) {
                String phone = text(body.get("phone"));
                if (staffRepository.findByPhone(phone).filter(other -> !other.getId().equals(id)).isPresent()) {
                    return fail(409, "员工手机号已存在");
                }
                entity.setPhone(phone);
            }
            if (body.containsKey("role")) entity.setRole(text(body.get("role")));
            if (body.containsKey("storeId")) entity.setStoreId(number(body.get("storeId")).longValue());
            if (body.containsKey("enabled")) entity.setStatus(Boolean.TRUE.equals(body.get("enabled")) ? "ACTIVE" : "DISABLED");
            if (body.containsKey("hireDate")) entity.setHireDate(LocalDate.parse(text(body.get("hireDate"))));
            if (body.containsKey("remark")) entity.setRemark(text(body.get("remark")));
            return ok(staffView(staffRepository.saveAndFlush(entity)));
        }
        return update(staff, id, body, "员工不存在");
    }

    @DeleteMapping("/staff/{id}")
    public synchronized Map<String, Object> deleteStaff(@PathVariable long id) {
        if (staffRepository != null) {
            StaffEntity entity = staffRepository.findById(id).orElse(null);
            if (entity == null) return fail(404, "员工不存在");
            entity.setStatus("DISABLED");
            staffRepository.save(entity);
            return ok(null);
        }
        remove(staff, id);
        return ok(null);
    }

    @GetMapping("/pricing")
    public synchronized Map<String, Object> listPricing(@RequestParam Map<String, String> params) {
        return ok(page(filter(pricing, params.get("keyword"), "categoryName"), params));
    }

    @PutMapping("/pricing/{id}")
    public synchronized Map<String, Object> updatePricing(@PathVariable long id, @RequestBody Map<String, Object> body) {
        for (String field : List.of("price", "processingFee", "promotionPrice")) {
            if (body.containsKey(field) && number(body.get(field)).doubleValue() < 0) {
                return fail(400, "价格不能为负数");
            }
        }
        return update(pricing, id, body, "价格不存在");
    }

    private void restore(OperationalStateService.State state) {
        categories.clear();
        inventory.clear();
        orders.clear();
        processingTasks.clear();
        suppliers.clear();
        purchases.clear();
        losses.clear();
        members.clear();
        staff.clear();
        pricing.clear();
        ids.set(state.nextId());
        categories.addAll(state.categories());
        inventory.addAll(state.inventory());
        orders.addAll(state.orders());
        processingTasks.addAll(state.processingTasks());
        suppliers.addAll(state.suppliers());
        purchases.addAll(state.purchases());
        losses.addAll(state.losses());
        members.addAll(state.members());
        staff.addAll(state.staff());
        pricing.addAll(state.pricing());
    }

    private void persist() {
        if (stateStore == null) return;
        try {
            stateStore.save(new OperationalStateService.State(
                    ids.get(), categories, inventory, orders, processingTasks,
                    suppliers, purchases, losses, members, staff, pricing));
        } catch (RuntimeException ex) {
            stateStore.load().ifPresent(this::restore);
            throw ex;
        }
    }

    private Map<String, Object> staffView(StaffEntity entity) {
        return item(
                "id", entity.getId(),
                "name", entity.getName(),
                "phone", entity.getPhone(),
                "role", entity.getRole(),
                "storeId", entity.getStoreId(),
                "storeName", storeName(entity.getStoreId()),
                "enabled", "ACTIVE".equals(entity.getStatus()),
                "status", entity.getStatus(),
                "hireDate", entity.getHireDate() == null ? null : entity.getHireDate().toString(),
                "remark", entity.getRemark());
    }

    private void seed() {
        categories.add(item("id", 11, "code", "P001", "name", "三黄鸡", "species", "鸡", "unit", "JIN", "basePrice", 18.8, "processingFee", 3, "avgWeight", 3.2, "enabled", true, "description", "本地散养，肉质紧实"));
        categories.add(item("id", 12, "code", "P002", "name", "老母鸡", "species", "鸡", "unit", "JIN", "basePrice", 28.0, "processingFee", 5, "avgWeight", 4.5, "enabled", true));
        categories.add(item("id", 13, "code", "P003", "name", "麻鸭", "species", "鸭", "unit", "JIN", "basePrice", 16.0, "processingFee", 4, "avgWeight", 5.0, "enabled", true));
        categories.add(item("id", 14, "code", "P004", "name", "白条鹅", "species", "鹅", "unit", "JIN", "basePrice", 22.0, "processingFee", 6, "avgWeight", 7.5, "enabled", true));
        categories.add(item("id", 15, "code", "P005", "name", "乳鸽", "species", "鸽", "unit", "PIECE", "basePrice", 35.0, "processingFee", 2, "avgWeight", 0.8, "enabled", true));

        inventory.add(item("id", 21, "storeId", 1, "storeName", "总店·城北店", "categoryId", 11, "categoryName", "三黄鸡", "batchNo", "B20260601-01", "quantity", 86, "avgWeight", 3.1, "totalWeight", 266.6, "health", "HEALTHY", "inStockAt", now(-2), "supplierName", "广丰养殖"));
        inventory.add(item("id", 22, "storeId", 1, "storeName", "总店·城北店", "categoryId", 13, "categoryName", "麻鸭", "batchNo", "B20260601-02", "quantity", 40, "avgWeight", 5.0, "totalWeight", 200, "health", "HEALTHY", "inStockAt", now(-1), "supplierName", "广丰养殖"));
        inventory.add(item("id", 23, "storeId", 2, "storeName", "城南旗舰店", "categoryId", 11, "categoryName", "三黄鸡", "batchNo", "B20260602-01", "quantity", 52, "avgWeight", 3.2, "totalWeight", 166.4, "health", "HEALTHY", "inStockAt", now(-1)));

        List<Map<String, Object>> orderItems = List.of(item("id", 311, "categoryId", 11, "categoryName", "三黄鸡", "quantity", 1, "weight", 3.2, "unitPrice", 18.8, "processMethod", "EVISCERATE", "processFee", 3, "subtotal", 63.16));
        orders.add(item("id", 31, "orderNo", "SO20260602001", "storeId", 1, "storeName", "总店·城北店", "cashierName", "陈小红", "customerPhone", "13800001111", "items", orderItems, "totalAmount", 63.16, "discount", 0, "payable", 63.16, "payMethod", "WECHAT", "status", "PAID", "createdAt", now(-1), "paidAt", now(-1)));
        orders.add(item("id", 32, "orderNo", "SO20260602002", "storeId", 2, "storeName", "城南旗舰店", "cashierName", "李店长", "memberName", "吴阿姨", "items", orderItems, "totalAmount", 126.32, "discount", 6.32, "payable", 120, "payMethod", "CASH", "status", "COMPLETED", "createdAt", now(-2), "completedAt", now(-2)));

        processingTasks.add(item("id", 41, "taskNo", "PT20260602001", "orderNo", "SO20260602001", "storeName", "总店·城北店", "categoryName", "三黄鸡", "quantity", 1, "weight", 3.2, "methods", List.of("EVISCERATE"), "workerId", 2, "workerName", "刘师傅", "status", "SLAUGHTERING", "priority", "NORMAL", "createdAt", now(-1), "startedAt", now(-1)));
        processingTasks.add(item("id", 42, "taskNo", "PT20260602002", "orderNo", "SO20260602002", "storeName", "城南旗舰店", "categoryName", "麻鸭", "quantity", 2, "weight", 9.8, "methods", List.of("CHOP", "PACK"), "status", "WAIT_SLAUGHTER", "priority", "URGENT", "createdAt", now(-1)));

        suppliers.add(item("id", 51, "name", "广丰养殖", "contact", "黄老板", "phone", "13900000001", "address", "广丰镇养殖基地", "category", "鸡鸭鹅", "level", "A", "enabled", true));
        suppliers.add(item("id", 52, "name", "清河禽业", "contact", "林经理", "phone", "13900000002", "address", "清河农业园", "category", "乳鸽鹌鹑", "level", "B", "enabled", true));

        purchases.add(item("id", 61, "orderNo", "PO20260601001", "supplierId", 51, "supplierName", "广丰养殖", "storeId", 1, "storeName", "总店·城北店", "categoryName", "三黄鸡", "quantity", 80, "totalWeight", 248.0, "unitPrice", 13.5, "amount", 3348.0, "batchNo", "B20260601-01", "status", "RECEIVED", "createdAt", now(-2), "receivedAt", now(-2)));
        purchases.add(item("id", 62, "orderNo", "PO20260602001", "supplierId", 52, "supplierName", "清河禽业", "storeId", 2, "storeName", "城南旗舰店", "categoryName", "乳鸽", "quantity", 120, "totalWeight", 96.0, "unitPrice", 22.0, "amount", 2112.0, "batchNo", "B20260602-03", "status", "SUBMITTED", "createdAt", now(-1)));

        losses.add(item("id", 71, "storeId", 1, "storeName", "总店·城北店", "categoryName", "三黄鸡", "batchNo", "B20260601-01", "quantity", 2, "reason", "DEAD", "handler", "刘师傅", "disposeMethod", "无害化", "occurredAt", now(-1), "remark", "到店途中损耗"));

        members.add(item("id", 81, "cardNo", "M0001", "name", "吴阿姨", "phone", "13800001234", "level", "GOLD", "points", 680, "balance", 120.5, "totalConsumption", 3288.0, "registerStoreName", "总店·城北店", "registeredAt", "2026-04-01"));
        members.add(item("id", 82, "cardNo", "M0002", "name", "周大姐", "phone", "13800005678", "level", "SILVER", "points", 260, "balance", 30, "totalConsumption", 980.0, "registerStoreName", "城南旗舰店", "registeredAt", "2026-05-03"));

        staff.add(item("id", 91, "name", "李店长", "phone", "13800000000", "role", "MANAGER", "storeId", 1, "storeName", "总店·城北店", "enabled", true, "hireDate", "2026-03-01"));
        staff.add(item("id", 92, "name", "陈小红", "phone", "13800002002", "role", "CASHIER", "storeId", 1, "storeName", "总店·城北店", "enabled", true, "hireDate", "2026-03-12"));
        staff.add(item("id", 93, "name", "刘师傅", "phone", "13800002001", "role", "BUTCHER", "storeId", 1, "storeName", "总店·城北店", "enabled", true, "hireDate", "2026-02-20"));

        pricing.add(item("id", 101, "categoryId", 11, "categoryName", "三黄鸡", "storeId", 1, "storeName", "总店·城北店", "date", LocalDateTime.now().format(DATE), "price", 18.8, "processingFee", 3));
        pricing.add(item("id", 102, "categoryId", 13, "categoryName", "麻鸭", "storeId", 1, "storeName", "总店·城北店", "date", LocalDateTime.now().format(DATE), "price", 16.0, "processingFee", 4, "promotionPrice", 15.5));
        pricing.add(item("id", 103, "categoryId", 15, "categoryName", "乳鸽", "storeId", 2, "storeName", "城南旗舰店", "date", LocalDateTime.now().format(DATE), "price", 35.0, "processingFee", 2));
    }

    private Map<String, Object> update(List<Map<String, Object>> list, long id, Map<String, Object> body, String message) {
        Map<String, Object> item = find(list, id);
        if (item == null) return fail(404, message);
        item.putAll(body);
        persist();
        return ok(item);
    }

    private Map<String, Object> find(List<Map<String, Object>> list, long id) {
        return list.stream().filter(item -> number(item.get("id")).longValue() == id).findFirst().orElse(null);
    }

    private void remove(List<Map<String, Object>> list, long id) {
        list.removeIf(item -> number(item.get("id")).longValue() == id);
        persist();
    }

    private List<Map<String, Object>> filter(List<Map<String, Object>> list, String keyword, String... fields) {
        if (keyword == null || keyword.isBlank()) return new ArrayList<>(list);
        String lower = keyword.toLowerCase();
        return list.stream().filter(item -> {
            for (String field : fields) {
                if (text(item.get(field)).toLowerCase().contains(lower)) return true;
            }
            return false;
        }).toList();
    }

    private Map<String, Object> page(List<Map<String, Object>> list, Map<String, String> params) {
        int page = Integer.parseInt(params.getOrDefault("page", "1"));
        int pageSize = Integer.parseInt(params.getOrDefault("pageSize", "10"));
        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(list.size(), from + pageSize);
        return item("list", from >= list.size() ? List.of() : list.subList(from, to), "total", list.size());
    }

    private Map<String, Object> ok(Object data) {
        return item("code", 200, "message", "ok", "data", data);
    }

    private Map<String, Object> fail(int code, String message) {
        return item("code", code, "message", message, "data", null);
    }

    private Map<String, Object> item(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }

    private Map<String, Object> copy(Map<String, Object> body) {
        return new LinkedHashMap<>(body);
    }

    private List<Map<String, Object>> withIds(List<Map<String, Object>> list) {
        return list.stream().map(source -> {
            Map<String, Object> item = copy(source);
            item.put("id", ids.incrementAndGet());
            return item;
        }).toList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> mapList(Object value) {
        return value instanceof List<?> ? (List<Map<String, Object>>) value : List.of();
    }

    private Number number(Object value) {
        if (value instanceof Number n) return n;
        if (value == null || text(value).isBlank()) return 0;
        return new BigDecimal(text(value));
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String now() {
        return LocalDateTime.now().format(DATE_TIME);
    }

    private String now(int offsetDays) {
        return LocalDateTime.now().plusDays(offsetDays).format(DATE_TIME);
    }

    private int stockForStore(long storeId) {
        return inventory.stream()
                .filter(item -> number(item.get("storeId")).longValue() == storeId)
                .mapToInt(item -> number(item.get("quantity")).intValue())
                .sum();
    }

    private String storeName(long storeId) {
        return switch ((int) storeId) {
            case 2 -> "城南旗舰店";
            case 3 -> "河西分店";
            case 4 -> "高新区便民店";
            default -> "总店·城北店";
        };
    }

    private long categoryIdByName(String name) {
        return categories.stream()
                .filter(item -> name.equals(item.get("name")))
                .map(item -> number(item.get("id")).longValue())
                .findFirst()
                .orElse(0L);
    }
}
