package com.qzshop.shopbe.operations;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LegacyOperationalStateMigratorIT {

    @Autowired JdbcTemplate jdbc;
    @Autowired LegacyOperationalStateMigrator migrator;

    @Test
    void importsLegacyProductsInventoryPricingCustomersAndOrders() {
        jdbc.execute("create table if not exists categories (id bigint primary key, name varchar(50))");
        jdbc.execute("create table if not exists products (id bigint primary key, category_id bigint, name varchar(100), unit varchar(10), default_price decimal(10,2), description varchar(255), is_active boolean)");
        jdbc.execute("create table if not exists store_products (id bigint primary key, store_id bigint, product_id bigint, price decimal(10,2))");
        jdbc.execute("create table if not exists store_inventory (id bigint primary key, store_id bigint, product_id bigint, stock_date date, remaining_qty int)");
        jdbc.execute("create table if not exists customers (id bigint primary key, name varchar(50), phone varchar(20), store_id bigint, points int, remark varchar(255), created_at timestamp)");
        jdbc.execute("create table if not exists orders (id bigint primary key, order_no varchar(30), store_id bigint, customer_id bigint, total_amount decimal(10,2), payment_method varchar(20), status varchar(20), remark varchar(255), created_at timestamp)");
        jdbc.execute("create table if not exists order_items (id bigint primary key, order_id bigint, product_id bigint, quantity int, weight decimal(8,2), unit_price decimal(10,2), amount decimal(10,2), need_slaughter boolean, slaughter_fee decimal(10,2))");
        jdbc.update("insert into categories values (701, '鸡')");
        jdbc.update("insert into products values (702, 701, '旧库土鸡', '斤', 19.80, '旧数据', true)");
        jdbc.update("insert into store_products values (703, 1, 702, 21.50)");
        jdbc.update("insert into store_inventory values (704, 1, 702, current_date, 9)");
        jdbc.update("insert into customers values (705, '旧会员', '13870000001', 1, 12, null, current_timestamp)");
        jdbc.update("insert into orders values (706, 'LEGACY-SO-1', 1, 705, 64.50, 'CASH', 'PAID', null, current_timestamp)");
        jdbc.update("insert into order_items values (707, 706, 702, 1, 3, 21.50, 64.50, true, 0)");

        OperationalStateService.State state = migrator.migrate().orElseThrow();

        assertThat(state.categories()).extracting(row -> row.get("name")).contains("旧库土鸡");
        assertThat(state.inventory()).extracting(row -> row.get("quantity")).contains(9);
        assertThat(state.pricing()).extracting(row -> row.get("price")).contains(21.5);
        assertThat(state.members()).extracting(row -> row.get("phone")).contains("13870000001");
        assertThat(state.orders()).extracting(row -> row.get("orderNo")).contains("LEGACY-SO-1");
        assertThat(state.processingTasks()).hasSize(1);
    }
}
