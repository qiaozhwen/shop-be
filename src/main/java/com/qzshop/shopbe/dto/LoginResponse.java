package com.qzshop.shopbe.dto;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import com.qzshop.shopbe.auth.token.TokenIssueResult;

public class LoginResponse {
    private String accessToken;
    private long accessTokenExpiresIn;
    private String refreshToken;
    private long refreshExpiresIn;
    private long refreshTokenExpiresIn;
    private String tokenType = "Bearer";
    private long staffId;
    private String staffName;
    private List<String> roles;
    private Map<String, Object> subject;

    public LoginResponse(TokenIssueResult tokens, String staffName) {
        this.accessToken = tokens.accessToken();
        this.accessTokenExpiresIn = tokens.accessTtl().getSeconds();
        this.refreshToken = tokens.refreshToken();
        this.refreshExpiresIn = tokens.refreshTtl().getSeconds();
        this.refreshTokenExpiresIn = tokens.refreshTtl().getSeconds();
        this.staffId = tokens.staffId();
        this.staffName = staffName;
        this.roles = tokens.roles();
        this.subject = subject(tokens.staffId(), null, staffName, tokens.roles(), true, List.of());
    }

    public LoginResponse(TokenIssueResult tokens, Map<String, Object> subject) {
        this.accessToken = tokens.accessToken();
        this.accessTokenExpiresIn = tokens.accessTtl().getSeconds();
        this.refreshToken = tokens.refreshToken();
        this.refreshExpiresIn = tokens.refreshTtl().getSeconds();
        this.refreshTokenExpiresIn = tokens.refreshTtl().getSeconds();
        this.staffId = tokens.staffId();
        this.staffName = String.valueOf(subject.getOrDefault("nickname", "员工"));
        this.roles = tokens.roles();
        this.subject = subject;
    }

    public String getAccessToken() { return accessToken; }
    public long getAccessExpiresIn() { return accessTokenExpiresIn; }
    public long getAccessTokenExpiresIn() { return accessTokenExpiresIn; }
    public String getRefreshToken() { return refreshToken; }
    public long getRefreshExpiresIn() { return refreshExpiresIn; }
    public long getRefreshTokenExpiresIn() { return refreshTokenExpiresIn; }
    public String getTokenType() { return tokenType; }
    public long getStaffId() { return staffId; }
    public String getStaffName() { return staffName; }
    public List<String> getRoles() { return roles; }
    public Map<String, Object> getSubject() { return subject; }

    public Map<String, Object> toMap() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("accessToken", accessToken);
        body.put("accessExpiresIn", accessTokenExpiresIn);
        body.put("accessTokenExpiresIn", accessTokenExpiresIn);
        body.put("refreshToken", refreshToken);
        body.put("refreshExpiresIn", refreshExpiresIn);
        body.put("refreshTokenExpiresIn", refreshTokenExpiresIn);
        body.put("tokenType", tokenType);
        body.put("staffId", staffId);
        body.put("staffName", staffName);
        body.put("roles", roles);
        body.put("subject", subject);
        return body;
    }

    public static Map<String, Object> subject(long id,
                                              String phone,
                                              String nickname,
                                              List<String> roles,
                                              boolean hasPassword,
                                              List<String> boundProviders) {
        Map<String, Object> subject = new LinkedHashMap<>();
        subject.put("type", "STAFF");
        subject.put("id", id);
        subject.put("phone", phone == null ? "13800000000" : phone);
        subject.put("nickname", nickname == null ? "员工" : nickname);
        subject.put("roles", roles == null || roles.isEmpty() ? List.of("STAFF_DEFAULT") : roles);
        subject.put("hasPassword", hasPassword);
        subject.put("boundProviders", boundProviders == null ? List.of() : boundProviders);
        return subject;
    }
}
