package com.qzshop.shopbe.auth.staff;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.qzshop.shopbe.ShopBeApplication;

class BootstrapAdminInitializerTest {

    private static final String PHONE = "13900009999";
    private static final String DATABASE = "jdbc:h2:mem:bootstrap-admin;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

    @Test
    void bootstrapCreatesAdministratorOnceAndNeverOverwritesExistingPassword() {
        String originalHash;
        try (ConfigurableApplicationContext first = start("InitialPass1")) {
            StaffRepository staffRepository = first.getBean(StaffRepository.class);
            PasswordEncoder encoder = first.getBean(PasswordEncoder.class);
            StaffEntity admin = staffRepository.findByPhone(PHONE).orElseThrow();

            assertThat(admin.getRole()).isEqualTo("ADMIN");
            assertThat(admin.getStatus()).isEqualTo("ACTIVE");
            assertThat(encoder.matches("InitialPass1", admin.getPassword())).isTrue();

            admin.setPassword(encoder.encode("ChangedPass2"));
            originalHash = staffRepository.saveAndFlush(admin).getPassword();
        }

        try (ConfigurableApplicationContext second = start("ReplacementPass3")) {
            StaffRepository staffRepository = second.getBean(StaffRepository.class);
            StaffEntity admin = staffRepository.findByPhone(PHONE).orElseThrow();

            assertThat(admin.getPassword()).isEqualTo(originalHash);
            assertThat(staffRepository.findAll())
                    .filteredOn(staff -> PHONE.equals(staff.getPhone()))
                    .hasSize(1);
        }
    }

    private ConfigurableApplicationContext start(String password) {
        return new SpringApplicationBuilder(ShopBeApplication.class)
                .web(WebApplicationType.SERVLET)
                .profiles("test")
                .run(
                        "--server.port=0",
                        "--spring.datasource.url=" + DATABASE,
                        "--spring.jpa.hibernate.ddl-auto=validate",
                        "--spring.flyway.enabled=true",
                        "--auth.bootstrap-admin.phone=" + PHONE,
                        "--auth.bootstrap-admin.password=" + password,
                        "--auth.bootstrap-admin.name=系统管理员",
                        "--auth.bootstrap-admin.store-id=1");
    }
}
