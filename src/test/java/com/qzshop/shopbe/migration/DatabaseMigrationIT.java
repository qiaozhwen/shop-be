package com.qzshop.shopbe.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.qzshop.shopbe.dao.StoreRepository;
import com.qzshop.shopbe.entity.StoreEntity;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@SpringBootTest(properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true"
})
@ActiveProfiles("test")
class DatabaseMigrationIT {

    @Autowired JdbcTemplate jdbc;
    @Autowired StoreRepository storeRepository;
    @Autowired EntityManager entityManager;

    @Test
    void flywayCreatesCurrentDomainTables() {
        List<String> tables = jdbc.queryForList(
                "select upper(table_name) from information_schema.tables where table_schema = 'PUBLIC'",
                String.class);

        assertThat(tables).contains(
                "STORES",
                "STAFF",
                "REFRESH_TOKENS",
                "SMS_VERIFICATION_CODES",
                "OPERATIONAL_STATE",
                "FLYWAY_SCHEMA_HISTORY");
    }

    @Test
    @Transactional
    void storeCodeAndRemarkSurviveReload() {
        StoreEntity store = new StoreEntity();
        store.setName("迁移测试门店");
        store.setStatus("OPEN");
        BeanWrapperImpl fields = new BeanWrapperImpl(store);
        fields.setPropertyValue("code", "MIG001");
        fields.setPropertyValue("remark", "重载后仍应存在");
        Long id = storeRepository.saveAndFlush(store).getId();

        entityManager.clear();

        StoreEntity reloaded = storeRepository.findById(id).orElseThrow();
        assertThat(reloaded).extracting("code").isEqualTo("MIG001");
        assertThat(reloaded).extracting("remark").isEqualTo("重载后仍应存在");
    }
}
