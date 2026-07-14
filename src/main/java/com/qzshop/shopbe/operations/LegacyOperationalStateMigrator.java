package com.qzshop.shopbe.operations;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Converts the original relational business tables into the versioned operational state. */
@Service
public class LegacyOperationalStateMigrator {

    private final JdbcTemplate jdbc;

    public LegacyOperationalStateMigrator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<OperationalStateService.State> migrate() {
        List<Map<String, Object>> products = query("""
                select p.id, p.name, p.unit, p.default_price, p.description, p.is_active,
                       c.name as species
                from products p left join categories c on c.id = p.category_id
                order by p.id
                """);
        if (products.isEmpty()) return Optional.empty();

        Map<Long, Map<String, Object>> productById = new HashMap<>();
        List<Map<String, Object>> categories = new ArrayList<>();
        long nextId = 1000;
        for (Map<String, Object> row : products) {
            long id = number(row.get("id"));
            nextId = Math.max(nextId, id);
            Map<String, Object> category = item(
                    "id", id,
                    "code", "LEGACY-P" + id,
                    "name", row.get("name"),
                    "species", text(row.get("species")),
                    "unit", "只".equals(text(row.get("unit"))) ? "PIECE" : "JIN",
                    "basePrice", decimal(row.get("default_price")),
                    "processingFee", 0,
                    "avgWeight", 0,
                    "enabled", !Boolean.FALSE.equals(row.get("is_active")),
                    "description", row.get("description"));
            categories.add(category);
            productById.put(id, category);
        }

        Map<Long, String> storeNames = names("select id, name from stores");
        List<Map<String, Object>> inventory = new ArrayList<>();
        for (Map<String, Object> row : query("select * from store_inventory order by id")) {
            long id = number(row.get("id"));
            long productId = number(row.get("product_id"));
            long storeId = number(row.get("store_id"));
            Map<String, Object> product = productById.get(productId);
            if (product == null) continue;
            nextId = Math.max(nextId, id);
            inventory.add(item(
                    "id", id,
                    "storeId", storeId,
                    "storeName", storeNames.getOrDefault(storeId, "门店" + storeId),
                    "categoryId", productId,
                    "categoryName", product.get("name"),
                    "batchNo", "LEGACY-" + date(row.get("stock_date")) + "-" + id,
                    "quantity", integer(row.get("remaining_qty")),
                    "avgWeight", 0,
                    "totalWeight", 0,
                    "health", "HEALTHY",
                    "inStockAt", date(row.get("stock_date")) + " 00:00:00"));
        }

        List<Map<String, Object>> pricing = new ArrayList<>();
        for (Map<String, Object> row : query("select * from store_products order by id")) {
            long id = number(row.get("id"));
            long productId = number(row.get("product_id"));
            long storeId = number(row.get("store_id"));
            Map<String, Object> product = productById.get(productId);
            if (product == null) continue;
            nextId = Math.max(nextId, id);
            pricing.add(item(
                    "id", id,
                    "categoryId", productId,
                    "categoryName", product.get("name"),
                    "storeId", storeId,
                    "storeName", storeNames.getOrDefault(storeId, "门店" + storeId),
                    "date", LocalDate.now().toString(),
                    "price", decimal(row.get("price")),
                    "processingFee", 0));
        }

        Map<Long, Map<String, Object>> customers = byId(query("select * from customers"));
        List<Map<String, Object>> members = new ArrayList<>();
        for (Map<String, Object> row : customers.values()) {
            long id = number(row.get("id"));
            long storeId = number(row.get("store_id"));
            nextId = Math.max(nextId, id);
            members.add(item(
                    "id", id,
                    "cardNo", "LEGACY-M" + id,
                    "name", row.get("name"),
                    "phone", row.get("phone"),
                    "level", "REGULAR",
                    "points", integer(row.get("points")),
                    "balance", 0,
                    "totalConsumption", 0,
                    "registerStoreName", storeNames.getOrDefault(storeId, "门店" + storeId),
                    "registeredAt", date(row.get("created_at")),
                    "remark", row.get("remark")));
        }

        Map<Long, List<Map<String, Object>>> itemsByOrder = new HashMap<>();
        for (Map<String, Object> row : query("select * from order_items order by id")) {
            long productId = number(row.get("product_id"));
            Map<String, Object> product = productById.get(productId);
            if (product == null) continue;
            double fee = decimal(row.get("slaughter_fee"));
            Map<String, Object> orderItem = item(
                    "id", number(row.get("id")),
                    "categoryId", productId,
                    "categoryName", product.get("name"),
                    "quantity", integer(row.get("quantity")),
                    "weight", decimal(row.get("weight")),
                    "unitPrice", decimal(row.get("unit_price")),
                    "processMethod", Boolean.FALSE.equals(row.get("need_slaughter")) ? "ALIVE" : "EVISCERATE",
                    "processFee", fee,
                    "subtotal", decimal(row.get("amount")) + fee * integer(row.get("quantity")));
            itemsByOrder.computeIfAbsent(number(row.get("order_id")), ignored -> new ArrayList<>()).add(orderItem);
            nextId = Math.max(nextId, number(row.get("id")));
        }

        List<Map<String, Object>> orders = new ArrayList<>();
        List<Map<String, Object>> processing = new ArrayList<>();
        for (Map<String, Object> row : query("select * from orders order by id")) {
            long id = number(row.get("id"));
            long storeId = number(row.get("store_id"));
            long customerId = number(row.get("customer_id"));
            List<Map<String, Object>> orderItems = itemsByOrder.getOrDefault(id, List.of());
            String createdAt = timestamp(row.get("created_at"));
            String status = text(row.get("status"));
            Map<String, Object> customer = customers.get(customerId);
            orders.add(item(
                    "id", id,
                    "orderNo", row.get("order_no"),
                    "storeId", storeId,
                    "storeName", storeNames.getOrDefault(storeId, "门店" + storeId),
                    "customerPhone", customer == null ? null : customer.get("phone"),
                    "items", orderItems,
                    "totalAmount", decimal(row.get("total_amount")),
                    "discount", 0,
                    "payable", decimal(row.get("total_amount")),
                    "payMethod", row.get("payment_method"),
                    "status", status,
                    "createdAt", createdAt,
                    "paidAt", "PENDING".equals(status) ? null : createdAt,
                    "remark", row.get("remark")));
            for (Map<String, Object> orderItem : orderItems) {
                if ("ALIVE".equals(orderItem.get("processMethod"))) continue;
                processing.add(item(
                        "id", ++nextId,
                        "taskNo", "LEGACY-PT-" + orderItem.get("id"),
                        "orderNo", row.get("order_no"),
                        "storeName", storeNames.getOrDefault(storeId, "门店" + storeId),
                        "categoryName", orderItem.get("categoryName"),
                        "quantity", orderItem.get("quantity"),
                        "weight", orderItem.get("weight"),
                        "methods", List.of(orderItem.get("processMethod")),
                        "status", "COMPLETED".equals(status) ? "DELIVERED" : "WAIT_SLAUGHTER",
                        "priority", "NORMAL",
                        "createdAt", createdAt));
            }
            nextId = Math.max(nextId, id);
        }

        return Optional.of(new OperationalStateService.State(
                nextId + 1000, categories, inventory, orders, processing,
                List.of(), List.of(), List.of(), members, List.of(), pricing));
    }

    private List<Map<String, Object>> query(String sql) {
        try {
            return jdbc.queryForList(sql).stream().map(row -> {
                Map<String, Object> normalized = new LinkedHashMap<>();
                row.forEach((key, value) -> normalized.put(key.toLowerCase(), value));
                return normalized;
            }).toList();
        } catch (DataAccessException ex) {
            return List.of();
        }
    }

    private Map<Long, String> names(String sql) {
        Map<Long, String> result = new HashMap<>();
        for (Map<String, Object> row : query(sql)) result.put(number(row.get("id")), text(row.get("name")));
        return result;
    }

    private Map<Long, Map<String, Object>> byId(List<Map<String, Object>> rows) {
        Map<Long, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) result.put(number(row.get("id")), row);
        return result;
    }

    private static Map<String, Object> item(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put(String.valueOf(pairs[i]), pairs[i + 1]);
        return result;
    }

    private static long number(Object value) {
        return value instanceof Number n ? n.longValue() : 0;
    }

    private static int integer(Object value) {
        return value instanceof Number n ? n.intValue() : 0;
    }

    private static double decimal(Object value) {
        return value instanceof Number n ? n.doubleValue() : 0;
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String date(Object value) {
        String text = timestamp(value);
        return text.length() >= 10 ? text.substring(0, 10) : LocalDate.now().toString();
    }

    private static String timestamp(Object value) {
        if (value instanceof Timestamp timestamp) return timestamp.toLocalDateTime().toString().replace('T', ' ');
        return text(value);
    }
}
