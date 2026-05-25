package com.qzshop.shopbe.auth.sms;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;

@Entity
@Table(name = "sms_verification_codes")
public class SmsVerificationCodeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 32)
    private String purpose;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(nullable = false)
    private Integer attempts = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(length = 64)
    private String ip;

    public Long getId() { return id; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { phone = v; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String v) { purpose = v; }
    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String v) { codeHash = v; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime v) { expiresAt = v; }
    public LocalDateTime getConsumedAt() { return consumedAt; }
    public void setConsumedAt(LocalDateTime v) { consumedAt = v; }
    public Integer getAttempts() { return attempts; }
    public void setAttempts(Integer v) { attempts = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getIp() { return ip; }
    public void setIp(String v) { ip = v; }
}
