package com.qzshop.shopbe.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.qzshop.shopbe.auth.staff.StaffRepository;
import com.qzshop.shopbe.dto.LoginRequest;

class AuthControllerTest {

    @Test
    void missingAccountStillPerformsPasswordHashComparison() {
        StaffRepository staffRepository = mock(StaffRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(staffRepository.findByPhone("13900000008")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("dummy-hash");
        AuthController controller = new AuthController(
                staffRepository,
                passwordEncoder,
                null,
                null);
        LoginRequest request = new LoginRequest();
        request.setPhone("13900000008");
        request.setPassword("WrongPass1");

        assertThatThrownBy(() -> controller.login(request))
                .isInstanceOf(AuthController.LoginFailedException.class);
        verify(passwordEncoder).matches("WrongPass1", "dummy-hash");
    }
}
