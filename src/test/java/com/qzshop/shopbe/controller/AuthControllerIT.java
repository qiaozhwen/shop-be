package com.qzshop.shopbe.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.qzshop.shopbe.auth.staff.StaffEntity;
import com.qzshop.shopbe.auth.staff.StaffRepository;
import com.qzshop.shopbe.auth.token.JwtService;

@SpringBootTest
@ActiveProfiles("test")
class AuthControllerIT {

    @Autowired WebApplicationContext context;
    @Autowired StaffRepository staffRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;
    @Autowired ObjectMapper objectMapper;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        staffRepository.deleteAll();
        mvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
            .build();
    }

    @Test
    void hardCodedDemoCredentialsAreRejectedWithoutStaffRow() throws Exception {
        mvc.perform(post("/api/admin/auth/login")
                .contentType(APPLICATION_JSON)
                .content("{\"phone\":\"13800000000\",\"password\":\"123456\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void activeDatabaseStaffCanLogin() throws Exception {
        saveStaff("13900000001", "OldPass1", "ACTIVE");

        mvc.perform(post("/api/admin/auth/login")
                .contentType(APPLICATION_JSON)
                .content("{\"phone\":\"13900000001\",\"password\":\"OldPass1\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.subject.phone").value("13900000001"));
    }

    @Test
    void inactiveStaffCannotLogin() throws Exception {
        saveStaff("13900000002", "OldPass1", "DISABLED");

        mvc.perform(post("/api/admin/auth/login")
                .contentType(APPLICATION_JSON)
                .content("{\"phone\":\"13900000002\",\"password\":\"OldPass1\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordChangeVerifiesOldPasswordAndPersistsNewHash() throws Exception {
        StaffEntity staff = saveStaff("13900000003", "OldPass1", "ACTIVE");
        String token = jwtService.issueStaff(staff.getId(), java.util.List.of("ADMIN"));

        mvc.perform(post("/api/admin/auth/set-password")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content("{\"oldPassword\":\"OldPass1\",\"newPassword\":\"NewPass2\"}"))
            .andExpect(status().isOk());

        StaffEntity updated = staffRepository.findById(staff.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("NewPass2", updated.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("OldPass1", updated.getPassword())).isFalse();
    }

    @Test
    void passwordChangeRejectsWrongOldPassword() throws Exception {
        StaffEntity staff = saveStaff("13900000004", "OldPass1", "ACTIVE");
        String originalHash = staff.getPassword();
        String token = jwtService.issueStaff(staff.getId(), java.util.List.of("ADMIN"));

        mvc.perform(post("/api/admin/auth/set-password")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content("{\"oldPassword\":\"WrongPass\",\"newPassword\":\"NewPass2\"}"))
            .andExpect(status().isBadRequest());

        assertThat(staffRepository.findById(staff.getId()).orElseThrow().getPassword())
            .isEqualTo(originalHash);
    }

    @Test
    void fakeSmsAndSsoEndpointsCannotIssueTokens() throws Exception {
        mvc.perform(post("/api/admin/auth/sms/login")
                .contentType(APPLICATION_JSON)
                .content("{\"phone\":\"13900000005\",\"code\":\"123456\"}"))
            .andExpect(status().isNotImplemented())
            .andExpect(jsonPath("$.message").value("当前版本未启用短信或第三方登录"));

        mvc.perform(post("/api/admin/auth/sso/wechat/exchange")
                .contentType(APPLICATION_JSON)
                .content("{\"code\":\"MOCK_CODE\",\"state\":\"anything\"}"))
            .andExpect(status().isNotImplemented())
            .andExpect(jsonPath("$.message").value("当前版本未启用短信或第三方登录"));
    }

    @Test
    void refreshKeepsDatabaseRoleInsteadOfElevatingToAdmin() throws Exception {
        StaffEntity staff = saveStaff("13900000006", "OldPass1", "ACTIVE");
        staff.setRole("CASHIER");
        staffRepository.saveAndFlush(staff);

        String loginJson = mvc.perform(post("/api/admin/auth/login")
                .contentType(APPLICATION_JSON)
                .content("{\"phone\":\"13900000006\",\"password\":\"OldPass1\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode login = objectMapper.readTree(loginJson);

        mvc.perform(post("/api/auth/refresh")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    java.util.Map.of("refreshToken", login.get("refreshToken").asText()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roles[0]").value("CASHIER"))
            .andExpect(jsonPath("$.subject.roles[0]").value("CASHIER"));
    }

    private StaffEntity saveStaff(String phone, String password, String status) {
        StaffEntity staff = new StaffEntity();
        staff.setStoreId(1L);
        staff.setName("测试员工");
        staff.setPhone(phone);
        staff.setRole("ADMIN");
        staff.setStatus(status);
        staff.setPassword(passwordEncoder.encode(password));
        return staffRepository.saveAndFlush(staff);
    }
}
