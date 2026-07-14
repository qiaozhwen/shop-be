package com.qzshop.shopbe.auth.staff;

import java.time.LocalDateTime;
import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;

@Entity
@Table(name = "staff")
public class StaffEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(unique = true, length = 20)
    private String phone;

    @Column(length = 64)
    private String nickname;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(length = 20)
    private String role = "STAFF_DEFAULT";

    @Column(length = 255)
    private String password;

    @Column(length = 20)
    private String status = "ACTIVE";

    @Column(name = "failed_login_count", nullable = false)
    private Integer failedLoginCount = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(length = 500)
    private String remark;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long v) { storeId = v; }
    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { phone = v; }
    public String getNickname() { return nickname; }
    public void setNickname(String v) { nickname = v; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String v) { avatarUrl = v; }
    public String getRole() { return role; }
    public void setRole(String v) { role = v; }
    public String getPassword() { return password; }
    public void setPassword(String v) { password = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
    public Integer getFailedLoginCount() { return failedLoginCount; }
    public void setFailedLoginCount(Integer v) { failedLoginCount = v; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime v) { lockedUntil = v; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime v) { lastLoginAt = v; }
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate v) { hireDate = v; }
    public String getRemark() { return remark; }
    public void setRemark(String v) { remark = v; }
}
