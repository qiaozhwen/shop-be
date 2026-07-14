package com.qzshop.shopbe.auth.staff;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;
    private final String phone;
    private final String password;
    private final String name;
    private final long storeId;

    public BootstrapAdminInitializer(
            StaffRepository staffRepository,
            PasswordEncoder passwordEncoder,
            @Value("${auth.bootstrap-admin.phone:}") String phone,
            @Value("${auth.bootstrap-admin.password:}") String password,
            @Value("${auth.bootstrap-admin.name:系统管理员}") String name,
            @Value("${auth.bootstrap-admin.store-id:1}") long storeId) {
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
        this.phone = phone == null ? "" : phone.trim();
        this.password = password == null ? "" : password;
        this.name = name == null || name.isBlank() ? "系统管理员" : name.trim();
        this.storeId = storeId;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (phone.isBlank() && password.isBlank()) {
            return;
        }
        if (phone.isBlank() || password.isBlank()) {
            throw new IllegalStateException("bootstrap administrator phone and password must be supplied together");
        }
        if (password.length() < 8) {
            throw new IllegalStateException("bootstrap administrator password must contain at least 8 characters");
        }
        if (staffRepository.findByPhone(phone).isPresent()) {
            return;
        }

        StaffEntity admin = new StaffEntity();
        admin.setStoreId(storeId);
        admin.setName(name);
        admin.setNickname(name);
        admin.setPhone(phone);
        admin.setRole("ADMIN");
        admin.setStatus("ACTIVE");
        admin.setPassword(passwordEncoder.encode(password));
        staffRepository.save(admin);
    }
}
