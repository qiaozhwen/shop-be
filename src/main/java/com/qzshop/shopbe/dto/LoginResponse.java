package com.qzshop.shopbe.dto;

import java.util.List;
import java.util.Map;

import com.qzshop.shopbe.auth.token.TokenIssueResult;

public class LoginResponse {
    private String accessToken;
    private long accessExpiresIn;
    private String refreshToken;
    private long refreshExpiresIn;
    private String tokenType = "Bearer";
    private long staffId;
    private String staffName;
    private List<String> roles;

    public LoginResponse(TokenIssueResult tokens, String staffName) {
        this.accessToken = tokens.accessToken();
        this.accessExpiresIn = tokens.accessTtl().getSeconds();
        this.refreshToken = tokens.refreshToken();
        this.refreshExpiresIn = tokens.refreshTtl().getSeconds();
        this.staffId = tokens.staffId();
        this.staffName = staffName;
        this.roles = tokens.roles();
    }

    public String getAccessToken() { return accessToken; }
    public long getAccessExpiresIn() { return accessExpiresIn; }
    public String getRefreshToken() { return refreshToken; }
    public long getRefreshExpiresIn() { return refreshExpiresIn; }
    public String getTokenType() { return tokenType; }
    public long getStaffId() { return staffId; }
    public String getStaffName() { return staffName; }
    public List<String> getRoles() { return roles; }

    public Map<String, Object> toMap() {
        return Map.of(
            "accessToken", accessToken,
            "accessExpiresIn", accessExpiresIn,
            "refreshToken", refreshToken,
            "refreshExpiresIn", refreshExpiresIn,
            "tokenType", tokenType,
            "staffId", staffId,
            "staffName", staffName,
            "roles", roles
        );
    }
}
