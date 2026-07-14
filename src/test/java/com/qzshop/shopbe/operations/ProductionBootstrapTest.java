package com.qzshop.shopbe.operations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.qzshop.shopbe.auth.staff.StaffRepository;
import com.qzshop.shopbe.controller.FrontendApiController;
import com.qzshop.shopbe.dao.StoreRepository;

class ProductionBootstrapTest {

    @Test
    void freshProductionDatabaseStartsEmptyInsteadOfWritingDemoBusinessData() {
        OperationalStateService stateStore = mock(OperationalStateService.class);
        LegacyOperationalStateMigrator migrator = mock(LegacyOperationalStateMigrator.class);
        when(stateStore.load()).thenReturn(Optional.empty());
        when(migrator.migrate()).thenReturn(Optional.empty());

        FrontendApiController controller = new FrontendApiController(
                stateStore,
                mock(StaffRepository.class),
                mock(PasswordEncoder.class),
                mock(StoreRepository.class),
                migrator,
                false);

        @SuppressWarnings("unchecked")
        Map<String, Object> page = (Map<String, Object>) controller
                .listCategories(Map.of()).get("data");
        assertThat(page.get("total")).isEqualTo(0);
        verify(stateStore).save(any(OperationalStateService.State.class));
    }
}
