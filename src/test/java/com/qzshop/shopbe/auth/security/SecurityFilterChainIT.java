package com.qzshop.shopbe.auth.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.qzshop.shopbe.auth.token.JwtService;

@SpringBootTest
@ActiveProfiles("test")
class SecurityFilterChainIT {

    @Autowired WebApplicationContext ctx;
    @Autowired JwtService jwt;

    private MockMvc mvc() {
        return MockMvcBuilders.webAppContextSetup(ctx)
            .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
            .build();
    }

    @Test
    void businessEndpointWithoutTokenIs401() throws Exception {
        mvc().perform(get("/api/stores")).andExpect(status().isUnauthorized());
    }

    @Test
    void businessEndpointWithStaffTokenIsAllowed() throws Exception {
        String token = jwt.issueStaff(1L, java.util.List.of("STAFF_DEFAULT"));
        mvc().perform(get("/api/stores").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void devFrontendCorsPreflightIsAllowed() throws Exception {
        mvc().perform(options("/api/admin/auth/login")
                .header("Origin", "http://127.0.0.1:5174")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type,authorization"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5174"));
    }

    @Test
    void adminEndpointWithoutTokenIs401() throws Exception {
        mvc().perform(get("/api/admin/anything"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpointWithBindPendingTokenIs403() throws Exception {
        String token = jwt.issueBindPending(1L, "WECHAT");
        mvc().perform(get("/api/admin/anything").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpointWithStaffTokenReaches404() throws Exception {
        String token = jwt.issueStaff(1L, java.util.List.of("STAFF_DEFAULT"));
        mvc().perform(get("/api/admin/anything").header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
    }
}
