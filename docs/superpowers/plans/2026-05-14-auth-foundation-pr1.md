# Auth Foundation (PR1) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 落地 staff auth 模块的"地基"——DB schema 改造、JWT/Refresh/SMS/锁定计数等领域服务、Spring Security 三链骨架，但**不暴露任何登录端点**；现有 `/api/stores` 必须继续匿名可用。

**Architecture:** 按 spec §2 分包到 `com.qzshop.shopbe.auth.{token,sms,security,password}`，沿用现有 Spring Boot 3.2 + JPA + PostgreSQL 风格。JWT 用 jjwt 0.12，BCrypt 由 Spring Security 自带，`AuthProperties` 强类型读取配置。所有秘密走环境变量（`AUTH_JWT_SECRET` 启动校验长度 ≥ 32 字节）。3 条 `SecurityFilterChain`：publicChain 暂时只放行 `/api/auth/refresh`（PR2 才加登录端点）、adminChain 拦 `/api/admin/**` 要求 `typ=STAFF`、defaultChain 兜底 `permitAll` 保住现有 Store 接口。

**Tech Stack:** Spring Boot 3.2、Spring Security 6、jjwt 0.12.6、PostgreSQL、JPA、JUnit 5 + Mockito + AssertJ + Spring Security Test。

**Spec：** `docs/superpowers/specs/2026-05-13-staff-auth-design.md`（§3 数据模型 / §6 Security / §7 配置 / §8 测试 / §10 安全基线）。

---

## File Structure (PR1 范围)

新增/修改的 Java 文件全部位于 `src/main/java/com/qzshop/shopbe/auth/...` 与对应 `src/test/java/...`：

```
build.gradle                                     [改] 加 spring-security/jjwt/security-test
schema.sql                                       [改] alter staff + 4 张新表
src/main/resources/application.properties        [改] auth.* 配置项（env 占位）
src/main/java/com/qzshop/shopbe/auth/
  AuthProperties.java                            [新] @ConfigurationProperties("auth")
  password/PasswordHasher.java                   [新] BCrypt 薄封装（接口 + 实现）
  token/
    JwtService.java                              [新] 签发/解析/typ 区分
    SubjectType.java                             [新] enum STAFF/BIND_PENDING（后期可扩）
    TokenIssueResult.java                        [新] record(access, accessExp, refresh, refreshExp)
    RefreshTokenEntity.java                      [新] @Entity refresh_tokens
    RefreshTokenRepository.java                  [新]
    TokenService.java                            [新] 编排 JWT + refresh 旋转
    TokenReplayException.java                    [新]
  sms/
    SmsPurpose.java                              [新] enum
    SmsProvider.java                             [新] interface
    MockSmsProvider.java                         [新]
    SmsVerificationCodeEntity.java               [新] @Entity sms_verification_codes
    SmsVerificationCodeRepository.java           [新]
    SmsVerificationService.java                  [新] 发送/校验/节流
    SmsThrottleException.java                    [新]
    SmsCodeInvalidException.java                 [新]
  staff/
    StaffEntity.java                             [新] @Entity staff（PR1 只用于 LoginAttemptService）
    StaffRepository.java                         [新] 仅 findByPhone / save
    LoginAttemptService.java                     [新] 失败计数 + 锁定窗口
  security/
    StaffPrincipal.java                          [新] record(staffId, phone, roles)
    JwtAuthenticationFilter.java                 [新]
    JsonAuthEntryPoint.java                      [新] 401 JSON
    JsonAccessDeniedHandler.java                 [新] 403 JSON
    SecurityConfig.java                          [新] 3 条 SecurityFilterChain
src/test/java/com/qzshop/shopbe/auth/
  token/JwtServiceTest.java                      [新]
  token/TokenServiceTest.java                    [新] @DataJpaTest
  sms/MockSmsProviderTest.java                   [新]
  sms/SmsVerificationServiceTest.java            [新] @DataJpaTest
  staff/LoginAttemptServiceTest.java             [新] @DataJpaTest
  security/SecurityFilterChainIT.java            [新] @SpringBootTest + MockMvc
```

PR2/PR3 才会加：`auth/staff/StaffAuthService`、`auth/staff/StaffAuthController`、`auth/sso/**`。

---

## 通用约定

- **测试风格沿用现有 Store 模块：** 单测用 `MockitoExtension`+`@Mock`，仓储测用 `@DataJpaTest`，安全集成用 `@SpringBootTest` + `MockMvc`（不要用 `@WebMvcTest + @MockBean`，spec §8 已说明 Java 25 + Mockito 兼容性问题）。
- **DTO/异常响应：** 统一返 `{"message": "..."}` 文案；错误码遵循 spec §4.2 表。
- **每个 Task 都以 commit 收尾**；commit message 用 `feat(auth): ...` / `test(auth): ...`，全部带 `Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>` 尾。
- **每个 Task 完成后跑** `./gradlew test` **整体绿才 commit**。

---

## Task 1: 加依赖与配置占位

**Files:**
- Modify: `build.gradle`
- Modify: `src/main/resources/application.properties`

- [ ] **Step 1: 编辑 `build.gradle`，在 `dependencies {}` 块里追加**

```gradle
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
    runtimeOnly    'io.jsonwebtoken:jjwt-impl:0.12.6'
    runtimeOnly    'io.jsonwebtoken:jjwt-jackson:0.12.6'
    testImplementation 'org.springframework.security:spring-security-test'
```

- [ ] **Step 2: 编辑 `application.properties`，文件末尾追加 spec §7.2 配置（先只放 PR1 真正用得到的）**

```properties
# ── Auth ──
auth.jwt.issuer=qz-shop-be
auth.jwt.secret=${AUTH_JWT_SECRET:please-change-me-this-is-a-32byte-default-secret!!}
auth.jwt.access-ttl=PT30M
auth.jwt.refresh-ttl=P14D
auth.jwt.bind-pending-ttl=PT5M

auth.staff.max-failed-attempts=5
auth.staff.lock-duration=PT15M

auth.sms.provider=mock
auth.sms.code-ttl=PT5M
auth.sms.resend-cooldown=PT1M
auth.sms.daily-limit=10
```

> 默认 secret 仅为本地启动方便；生产必须设 `AUTH_JWT_SECRET` 环境变量。

- [ ] **Step 3: 跑构建确认依赖可拉**

Run: `./gradlew build -x test --quiet`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: 此时启动会因 Spring Security 自动配置把所有接口锁住，需要在 SecurityConfig 加好之前先验证不影响 dev 体验** → 不启动；直接 commit。

- [ ] **Step 5: Commit**

```bash
git add build.gradle src/main/resources/application.properties
git commit -m "build(auth): add spring-security + jjwt deps and auth.* config placeholders

PR1 task 1/10. Foundation for staff auth module per spec §7.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 2: DB schema 迁移

**Files:**
- Modify: `schema.sql`

- [ ] **Step 1: 在 `schema.sql` 中找到 `CREATE TABLE IF NOT EXISTS staff` 之后追加 ALTER。** 因为现有 `staff` 表 schema 与 spec 假设不一致（已有 `role`、没有 `phone/nickname/...`），用幂等 ALTER：

将原 `staff` 块替换为以下完整版（**同时新增 4 张表，整体追加到文件末尾**）：

```sql
-- ── Auth: staff 改造 ──
ALTER TABLE staff ADD COLUMN IF NOT EXISTS phone VARCHAR(20);
ALTER TABLE staff ADD COLUMN IF NOT EXISTS nickname VARCHAR(64);
ALTER TABLE staff ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(255);
ALTER TABLE staff ADD COLUMN IF NOT EXISTS failed_login_count INT NOT NULL DEFAULT 0;
ALTER TABLE staff ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP NULL;
ALTER TABLE staff ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP NULL;
ALTER TABLE staff ALTER COLUMN password DROP NOT NULL;
-- phone 变唯一非空：先回填测试数据再加约束（生产由 DBA 手动）
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'staff_phone_unique') THEN
    ALTER TABLE staff ADD CONSTRAINT staff_phone_unique UNIQUE (phone);
  END IF;
END $$;

-- ── Auth: 社交账号绑定 ──
CREATE TABLE IF NOT EXISTS staff_social_accounts (
  id          BIGSERIAL PRIMARY KEY,
  staff_id    BIGINT REFERENCES staff(id),
  provider    VARCHAR(32) NOT NULL,
  open_id     VARCHAR(128) NOT NULL,
  union_id    VARCHAR(128),
  raw_profile JSONB,
  created_at  TIMESTAMP NOT NULL DEFAULT now(),
  updated_at  TIMESTAMP NOT NULL DEFAULT now(),
  UNIQUE (provider, open_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_ssa_staff_provider
  ON staff_social_accounts(staff_id, provider) WHERE staff_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_ssa_staff ON staff_social_accounts(staff_id);

-- ── Auth: 短信验证码 ──
CREATE TABLE IF NOT EXISTS sms_verification_codes (
  id          BIGSERIAL PRIMARY KEY,
  phone       VARCHAR(20) NOT NULL,
  purpose     VARCHAR(32) NOT NULL,
  code_hash   VARCHAR(64) NOT NULL,
  expires_at  TIMESTAMP NOT NULL,
  consumed_at TIMESTAMP NULL,
  attempts    INT NOT NULL DEFAULT 0,
  created_at  TIMESTAMP NOT NULL DEFAULT now(),
  ip          VARCHAR(64)
);
CREATE INDEX IF NOT EXISTS idx_svc_phone_purpose
  ON sms_verification_codes(phone, purpose, created_at DESC);

-- ── Auth: refresh token ──
CREATE TABLE IF NOT EXISTS refresh_tokens (
  id           BIGSERIAL PRIMARY KEY,
  token_hash   VARCHAR(64) NOT NULL UNIQUE,
  subject_type VARCHAR(16) NOT NULL,
  subject_id   BIGINT NOT NULL,
  device_info  VARCHAR(255),
  expires_at   TIMESTAMP NOT NULL,
  revoked_at   TIMESTAMP NULL,
  created_at   TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_rt_subject ON refresh_tokens(subject_type, subject_id);

-- ── Auth: 登录审计 ──
CREATE TABLE IF NOT EXISTS login_audit_logs (
  id            BIGSERIAL PRIMARY KEY,
  staff_id      BIGINT,
  channel       VARCHAR(32) NOT NULL,
  account_input VARCHAR(128),
  result        VARCHAR(16) NOT NULL,
  fail_reason   VARCHAR(64),
  ip            VARCHAR(64),
  user_agent    VARCHAR(255),
  created_at    TIMESTAMP NOT NULL DEFAULT now()
);
```

> 使用 `ADD COLUMN IF NOT EXISTS` / `CREATE TABLE IF NOT EXISTS` 让脚本可重复执行。`staff_social_accounts (staff_id, provider)` 用部分唯一索引以允许 BIND_PENDING 阶段 `staff_id IS NULL`。

- [ ] **Step 2: 手动在本地 PG 执行该脚本验证不报错**

Run: `psql -U qiaozhen -d postgres -v ON_ERROR_STOP=1 -f schema.sql`
Expected: 无错误输出（重复执行也无错）。

> 如果本地没起 PG 可暂跳过，集成测试时再校验。

- [ ] **Step 3: Commit**

```bash
git add schema.sql
git commit -m "feat(auth): db schema for staff auth (alter staff + 4 new tables)

- staff: + phone unique / nickname / avatar_url / failed_login_count / locked_until / last_login_at; password nullable
- staff_social_accounts, sms_verification_codes, refresh_tokens, login_audit_logs
- All idempotent (IF NOT EXISTS / DO blocks) so existing dev DBs migrate cleanly

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 3: AuthProperties + 启动校验

**Files:**
- Create: `src/main/java/com/qzshop/shopbe/auth/AuthProperties.java`
- Modify: `src/main/java/com/qzshop/shopbe/ShopBeApplication.java`

- [ ] **Step 1: 创建 `AuthProperties.java`**

```java
package com.qzshop.shopbe.auth;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties("auth")
public class AuthProperties {

    private Jwt jwt = new Jwt();
    private Staff staff = new Staff();
    private Sms sms = new Sms();

    public Jwt getJwt() { return jwt; }
    public Staff getStaff() { return staff; }
    public Sms getSms() { return sms; }

    public static class Jwt {
        @NotBlank private String issuer;
        @NotBlank private String secret;
        private Duration accessTtl = Duration.ofMinutes(30);
        private Duration refreshTtl = Duration.ofDays(14);
        private Duration bindPendingTtl = Duration.ofMinutes(5);
        // getters/setters
        public String getIssuer() { return issuer; }
        public void setIssuer(String v) { issuer = v; }
        public String getSecret() { return secret; }
        public void setSecret(String v) { secret = v; }
        public Duration getAccessTtl() { return accessTtl; }
        public void setAccessTtl(Duration v) { accessTtl = v; }
        public Duration getRefreshTtl() { return refreshTtl; }
        public void setRefreshTtl(Duration v) { refreshTtl = v; }
        public Duration getBindPendingTtl() { return bindPendingTtl; }
        public void setBindPendingTtl(Duration v) { bindPendingTtl = v; }
    }

    public static class Staff {
        @Min(1) private int maxFailedAttempts = 5;
        private Duration lockDuration = Duration.ofMinutes(15);
        public int getMaxFailedAttempts() { return maxFailedAttempts; }
        public void setMaxFailedAttempts(int v) { maxFailedAttempts = v; }
        public Duration getLockDuration() { return lockDuration; }
        public void setLockDuration(Duration v) { lockDuration = v; }
    }

    public static class Sms {
        @NotBlank private String provider = "mock";
        private Duration codeTtl = Duration.ofMinutes(5);
        private Duration resendCooldown = Duration.ofSeconds(60);
        @Min(1) private int dailyLimit = 10;
        public String getProvider() { return provider; }
        public void setProvider(String v) { provider = v; }
        public Duration getCodeTtl() { return codeTtl; }
        public void setCodeTtl(Duration v) { codeTtl = v; }
        public Duration getResendCooldown() { return resendCooldown; }
        public void setResendCooldown(Duration v) { resendCooldown = v; }
        public int getDailyLimit() { return dailyLimit; }
        public void setDailyLimit(int v) { dailyLimit = v; }
    }

    @PostConstruct
    void validateSecret() {
        if (jwt.getSecret() == null || jwt.getSecret().getBytes().length < 32) {
            throw new IllegalStateException(
                "auth.jwt.secret must be at least 32 bytes (set AUTH_JWT_SECRET env)");
        }
    }
}
```

- [ ] **Step 2: 在 `ShopBeApplication` 类上加 `@ConfigurationPropertiesScan`**

修改 `ShopBeApplication.java`：

```java
package com.qzshop.shopbe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.qzshop.shopbe")
public class ShopBeApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShopBeApplication.class, args);
    }
}
```

- [ ] **Step 3: 启动一次确认配置加载**

Run: `./gradlew bootRun --quiet`
Expected: 启动到 `Started ShopBeApplication in ... seconds`，无 `IllegalStateException`。然后 Ctrl-C 停掉。

> 如果报 secret 长度不足，临时 export `AUTH_JWT_SECRET=please-change-me-this-is-a-32byte-default-secret!!`。

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/qzshop/shopbe/auth/AuthProperties.java src/main/java/com/qzshop/shopbe/ShopBeApplication.java
git commit -m "feat(auth): AuthProperties with startup validation for jwt secret length

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 4: JwtService + 单测

**Files:**
- Create: `src/main/java/com/qzshop/shopbe/auth/token/SubjectType.java`
- Create: `src/main/java/com/qzshop/shopbe/auth/token/JwtService.java`
- Create: `src/test/java/com/qzshop/shopbe/auth/token/JwtServiceTest.java`

- [ ] **Step 1: 写测试** `JwtServiceTest.java`

```java
package com.qzshop.shopbe.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.qzshop.shopbe.auth.AuthProperties;

import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    private JwtService svc;

    @BeforeEach
    void setUp() {
        AuthProperties props = new AuthProperties();
        props.getJwt().setIssuer("test-iss");
        props.getJwt().setSecret("0123456789-0123456789-0123456789-AB");
        props.getJwt().setAccessTtl(Duration.ofSeconds(60));
        props.getJwt().setBindPendingTtl(Duration.ofSeconds(30));
        svc = new JwtService(props);
    }

    @Test
    void issuesAndParsesStaffToken() {
        String token = svc.issueStaff(123L, List.of("STAFF_DEFAULT"));
        ParsedToken p = svc.parse(token);
        assertThat(p.type()).isEqualTo(SubjectType.STAFF);
        assertThat(p.subjectId()).isEqualTo(123L);
        assertThat(p.roles()).containsExactly("STAFF_DEFAULT");
    }

    @Test
    void issuesAndParsesBindPendingToken() {
        String token = svc.issueBindPending(99L, "WECHAT");
        ParsedToken p = svc.parse(token);
        assertThat(p.type()).isEqualTo(SubjectType.BIND_PENDING);
        assertThat(p.subjectId()).isEqualTo(99L);
        assertThat(p.provider()).isEqualTo("WECHAT");
        assertThat(p.roles()).isEmpty();
    }

    @Test
    void rejectsTamperedToken() {
        String token = svc.issueStaff(1L, List.of());
        String tampered = token.substring(0, token.length() - 2) + "AA";
        assertThatThrownBy(() -> svc.parse(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        AuthProperties shortProps = new AuthProperties();
        shortProps.getJwt().setIssuer("x");
        shortProps.getJwt().setSecret("0123456789-0123456789-0123456789-AB");
        shortProps.getJwt().setAccessTtl(Duration.ofMillis(1));
        JwtService shortSvc = new JwtService(shortProps);
        String token = shortSvc.issueStaff(1L, List.of());
        Thread.sleep(20);
        assertThatThrownBy(() -> shortSvc.parse(token)).isInstanceOf(JwtException.class);
    }
}
```

- [ ] **Step 2: 跑测试，确认红**

Run: `./gradlew test --tests JwtServiceTest --quiet`
Expected: 编译失败（JwtService/ParsedToken/SubjectType 还不存在）。

- [ ] **Step 3: 创建 `SubjectType.java`**

```java
package com.qzshop.shopbe.auth.token;

public enum SubjectType { STAFF, BIND_PENDING }
```

- [ ] **Step 4: 创建 `JwtService.java`**

```java
package com.qzshop.shopbe.auth.token;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.qzshop.shopbe.auth.AuthProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final AuthProperties props;
    private final SecretKey key;

    public JwtService(AuthProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String issueStaff(long staffId, List<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
            .issuer(props.getJwt().getIssuer())
            .subject(String.valueOf(staffId))
            .id(UUID.randomUUID().toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(props.getJwt().getAccessTtl())))
            .claims(Map.of("typ", SubjectType.STAFF.name(), "roles", roles))
            .signWith(key)
            .compact();
    }

    public String issueBindPending(long socialAccountId, String provider) {
        Instant now = Instant.now();
        return Jwts.builder()
            .issuer(props.getJwt().getIssuer())
            .subject(String.valueOf(socialAccountId))
            .id(UUID.randomUUID().toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(props.getJwt().getBindPendingTtl())))
            .claims(Map.of("typ", SubjectType.BIND_PENDING.name(), "provider", provider))
            .signWith(key)
            .compact();
    }

    @SuppressWarnings("unchecked")
    public ParsedToken parse(String token) {
        Jws<Claims> jws = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
        Claims c = jws.getPayload();
        SubjectType type = SubjectType.valueOf(c.get("typ", String.class));
        long subjectId = Long.parseLong(c.getSubject());
        List<String> roles = type == SubjectType.STAFF
            ? (List<String>) c.getOrDefault("roles", List.of())
            : List.of();
        String provider = c.get("provider", String.class);
        return new ParsedToken(type, subjectId, roles, provider, c.getId(), c.getExpiration().toInstant());
    }
}
```

- [ ] **Step 5: 创建 `ParsedToken.java`** (record，同包)

```java
package com.qzshop.shopbe.auth.token;

import java.time.Instant;
import java.util.List;

public record ParsedToken(
    SubjectType type,
    long subjectId,
    List<String> roles,
    String provider,
    String jti,
    Instant expiresAt
) {}
```

- [ ] **Step 6: 跑测试，确认全绿**

Run: `./gradlew test --tests JwtServiceTest --quiet`
Expected: `BUILD SUCCESSFUL` 4 tests passed。

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/qzshop/shopbe/auth/token/SubjectType.java \
        src/main/java/com/qzshop/shopbe/auth/token/JwtService.java \
        src/main/java/com/qzshop/shopbe/auth/token/ParsedToken.java \
        src/test/java/com/qzshop/shopbe/auth/token/JwtServiceTest.java
git commit -m "feat(auth): JwtService with STAFF / BIND_PENDING token types

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 5: RefreshToken Entity / Repository

**Files:**
- Create: `src/main/java/com/qzshop/shopbe/auth/token/RefreshTokenEntity.java`
- Create: `src/main/java/com/qzshop/shopbe/auth/token/RefreshTokenRepository.java`

- [ ] **Step 1: 创建 entity**

```java
package com.qzshop.shopbe.auth.token;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "subject_type", nullable = false, length = 16)
    private String subjectType;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "device_info")
    private String deviceInfo;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── getters / setters ──
    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String v) { tokenHash = v; }
    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String v) { subjectType = v; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long v) { subjectId = v; }
    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String v) { deviceInfo = v; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime v) { expiresAt = v; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime v) { revokedAt = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 2: 创建 repository**

```java
package com.qzshop.shopbe.auth.token;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    List<RefreshTokenEntity> findBySubjectTypeAndSubjectIdAndRevokedAtIsNull(String subjectType, Long subjectId);

    @Modifying
    @Query("update RefreshTokenEntity r set r.revokedAt = CURRENT_TIMESTAMP " +
           "where r.subjectType = :type and r.subjectId = :id and r.revokedAt is null")
    int revokeAllActive(@Param("type") String type, @Param("id") Long id);
}
```

- [ ] **Step 3: 编译通过即可（这个 task 没单测，TokenServiceTest 会覆盖）**

Run: `./gradlew compileJava --quiet`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/qzshop/shopbe/auth/token/RefreshTokenEntity.java \
        src/main/java/com/qzshop/shopbe/auth/token/RefreshTokenRepository.java
git commit -m "feat(auth): refresh_tokens entity + repository

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 6: TokenService（旋转 + 重放检测）+ @DataJpaTest

**Files:**
- Create: `src/main/java/com/qzshop/shopbe/auth/token/TokenIssueResult.java`
- Create: `src/main/java/com/qzshop/shopbe/auth/token/TokenReplayException.java`
- Create: `src/main/java/com/qzshop/shopbe/auth/token/TokenService.java`
- Create: `src/test/java/com/qzshop/shopbe/auth/token/TokenServiceTest.java`

- [ ] **Step 1: 写测试** `TokenServiceTest.java`

```java
package com.qzshop.shopbe.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.qzshop.shopbe.auth.AuthProperties;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({TokenService.class, JwtService.class})
class TokenServiceTest {

    @Autowired RefreshTokenRepository repo;
    @Autowired TokenService tokens;

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        AuthProperties authProperties() {
            AuthProperties p = new AuthProperties();
            p.getJwt().setIssuer("test");
            p.getJwt().setSecret("0123456789-0123456789-0123456789-AB");
            p.getJwt().setAccessTtl(Duration.ofMinutes(10));
            p.getJwt().setRefreshTtl(Duration.ofDays(1));
            return p;
        }
    }

    @BeforeEach
    void clean() { repo.deleteAll(); }

    @Test
    void issueStoresHashedRefreshAndReturnsRawToken() {
        TokenIssueResult r = tokens.issueForStaff(7L, List.of("STAFF_DEFAULT"), null);
        assertThat(r.refreshToken()).isNotBlank();
        assertThat(repo.count()).isEqualTo(1);
        assertThat(repo.findAll().get(0).getTokenHash()).isNotEqualTo(r.refreshToken());
    }

    @Test
    void rotateInvalidatesOldAndIssuesNew() {
        TokenIssueResult first = tokens.issueForStaff(7L, List.of(), null);
        TokenIssueResult second = tokens.rotate(first.refreshToken(), List.of());
        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThat(repo.findByTokenHash(TokenService.hash(first.refreshToken())).orElseThrow()
            .getRevokedAt()).isNotNull();
    }

    @Test
    void rotateRejectsRevokedAndKillsAllSiblings() {
        TokenIssueResult a = tokens.issueForStaff(8L, List.of(), null);
        TokenIssueResult b = tokens.issueForStaff(8L, List.of(), null);
        tokens.rotate(a.refreshToken(), List.of()); // a 被撤销
        assertThatThrownBy(() -> tokens.rotate(a.refreshToken(), List.of()))
            .isInstanceOf(TokenReplayException.class);
        // 触发重放 → 该 staff 全部 active 也被撤销
        assertThat(repo.findBySubjectTypeAndSubjectIdAndRevokedAtIsNull("STAFF", 8L)).isEmpty();
    }

    @Test
    void revokeAllRemovesActiveOnly() {
        tokens.issueForStaff(9L, List.of(), null);
        tokens.issueForStaff(9L, List.of(), null);
        int n = tokens.revokeAllForStaff(9L);
        assertThat(n).isEqualTo(2);
        assertThat(repo.findBySubjectTypeAndSubjectIdAndRevokedAtIsNull("STAFF", 9L)).isEmpty();
    }
}
```

- [ ] **Step 2: 跑测试确认红**

Run: `./gradlew test --tests TokenServiceTest --quiet`
Expected: 编译失败。

- [ ] **Step 3: 创建辅助类**

`TokenIssueResult.java`:
```java
package com.qzshop.shopbe.auth.token;

import java.time.Duration;
import java.util.List;

public record TokenIssueResult(
    String accessToken,
    Duration accessTtl,
    String refreshToken,
    Duration refreshTtl,
    long staffId,
    List<String> roles
) {}
```

`TokenReplayException.java`:
```java
package com.qzshop.shopbe.auth.token;

public class TokenReplayException extends RuntimeException {
    public TokenReplayException(String msg) { super(msg); }
}
```

- [ ] **Step 4: 创建 `TokenService.java`**

```java
package com.qzshop.shopbe.auth.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qzshop.shopbe.auth.AuthProperties;

@Service
public class TokenService {

    private static final String SUBJECT_STAFF = "STAFF";
    private static final SecureRandom RNG = new SecureRandom();

    private final RefreshTokenRepository repo;
    private final JwtService jwt;
    private final AuthProperties props;

    public TokenService(RefreshTokenRepository repo, JwtService jwt, AuthProperties props) {
        this.repo = repo;
        this.jwt = jwt;
        this.props = props;
    }

    @Transactional
    public TokenIssueResult issueForStaff(long staffId, List<String> roles, String deviceInfo) {
        String access = jwt.issueStaff(staffId, roles);
        String refresh = newRefresh();
        RefreshTokenEntity row = new RefreshTokenEntity();
        row.setTokenHash(hash(refresh));
        row.setSubjectType(SUBJECT_STAFF);
        row.setSubjectId(staffId);
        row.setDeviceInfo(deviceInfo);
        row.setExpiresAt(LocalDateTime.now().plus(props.getJwt().getRefreshTtl()));
        repo.save(row);
        return new TokenIssueResult(
            access, props.getJwt().getAccessTtl(),
            refresh, props.getJwt().getRefreshTtl(),
            staffId, roles
        );
    }

    @Transactional
    public TokenIssueResult rotate(String refreshToken, List<String> roles) {
        Optional<RefreshTokenEntity> opt = repo.findByTokenHash(hash(refreshToken));
        if (opt.isEmpty()) {
            throw new TokenReplayException("refresh token not found");
        }
        RefreshTokenEntity row = opt.get();
        if (row.getRevokedAt() != null) {
            // 重放：杀掉该 subject 全部 active
            repo.revokeAllActive(row.getSubjectType(), row.getSubjectId());
            throw new TokenReplayException("refresh token replay detected");
        }
        if (row.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TokenReplayException("refresh token expired");
        }
        row.setRevokedAt(LocalDateTime.now());
        repo.save(row);
        return issueForStaff(row.getSubjectId(), roles, row.getDeviceInfo());
    }

    @Transactional
    public int revokeAllForStaff(long staffId) {
        return repo.revokeAllActive(SUBJECT_STAFF, staffId);
    }

    @Transactional
    public boolean revokeOne(String refreshToken) {
        Optional<RefreshTokenEntity> opt = repo.findByTokenHash(hash(refreshToken));
        if (opt.isEmpty() || opt.get().getRevokedAt() != null) return false;
        opt.get().setRevokedAt(LocalDateTime.now());
        return true;
    }

    private static String newRefresh() {
        byte[] b = new byte[32];
        RNG.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    static String hash(String token) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
```

- [ ] **Step 5: 跑测试**

Run: `./gradlew test --tests TokenServiceTest --quiet`
Expected: 4 tests passed.

> 如果 `@DataJpaTest` 默认换内存 H2，本仓库的 H2 已是 runtime 依赖；`@AutoConfigureTestDatabase(replace = ANY)` 会用 H2 自动建表（`spring.jpa.hibernate.ddl-auto=create-drop` for tests）。如果集成测试需要 PG 兼容方言，可在 `src/test/resources/application.properties` 设 `spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect` 并 ensure ddl-auto=create-drop。

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/qzshop/shopbe/auth/token/TokenIssueResult.java \
        src/main/java/com/qzshop/shopbe/auth/token/TokenReplayException.java \
        src/main/java/com/qzshop/shopbe/auth/token/TokenService.java \
        src/test/java/com/qzshop/shopbe/auth/token/TokenServiceTest.java
git commit -m "feat(auth): TokenService with rotation + replay detection

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 7: SmsProvider 抽象 + MockSmsProvider + 单测

**Files:**
- Create: `src/main/java/com/qzshop/shopbe/auth/sms/SmsPurpose.java`
- Create: `src/main/java/com/qzshop/shopbe/auth/sms/SmsProvider.java`
- Create: `src/main/java/com/qzshop/shopbe/auth/sms/MockSmsProvider.java`
- Create: `src/test/java/com/qzshop/shopbe/auth/sms/MockSmsProviderTest.java`

- [ ] **Step 1: 写测试** `MockSmsProviderTest.java`

```java
package com.qzshop.shopbe.auth.sms;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

class MockSmsProviderTest {

    @Test
    void doesNotThrowAndLogsAtInfo() {
        Logger logger = Logger.getLogger(MockSmsProvider.class.getName());
        List<LogRecord> records = new ArrayList<>();
        Handler h = new Handler() {
            @Override public void publish(LogRecord r) { records.add(r); }
            @Override public void flush() {}
            @Override public void close() {}
        };
        logger.addHandler(h);
        try {
            new MockSmsProvider().send("13800000000", "123456", SmsPurpose.SMS_LOGIN);
            assertThat(records).anyMatch(r -> r.getMessage().contains("13800000000"));
        } finally {
            logger.removeHandler(h);
        }
    }
}
```

- [ ] **Step 2: 跑确认红**

Run: `./gradlew test --tests MockSmsProviderTest --quiet`
Expected: 编译失败。

- [ ] **Step 3: 创建 enum / 接口 / 实现**

`SmsPurpose.java`:
```java
package com.qzshop.shopbe.auth.sms;

public enum SmsPurpose { BIND_PHONE, SMS_LOGIN, RESET_PASSWORD, SET_PASSWORD }
```

`SmsProvider.java`:
```java
package com.qzshop.shopbe.auth.sms;

public interface SmsProvider {
    void send(String phone, String code, SmsPurpose purpose);
}
```

`MockSmsProvider.java`:
```java
package com.qzshop.shopbe.auth.sms;

import java.util.logging.Logger;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "auth.sms.provider", havingValue = "mock", matchIfMissing = true)
public class MockSmsProvider implements SmsProvider {

    private static final Logger LOG = Logger.getLogger(MockSmsProvider.class.getName());

    @Override
    public void send(String phone, String code, SmsPurpose purpose) {
        LOG.info(() -> "[MOCK SMS] phone=" + phone + " purpose=" + purpose + " code=" + code);
    }
}
```

- [ ] **Step 4: 跑测试**

Run: `./gradlew test --tests MockSmsProviderTest --quiet`
Expected: 1 test passed.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/qzshop/shopbe/auth/sms/SmsPurpose.java \
        src/main/java/com/qzshop/shopbe/auth/sms/SmsProvider.java \
        src/main/java/com/qzshop/shopbe/auth/sms/MockSmsProvider.java \
        src/test/java/com/qzshop/shopbe/auth/sms/MockSmsProviderTest.java
git commit -m "feat(auth): SmsProvider abstraction + MockSmsProvider (default profile)

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 8: SmsVerificationService（发送/校验/节流）+ @DataJpaTest

**Files:**
- Create: `src/main/java/com/qzshop/shopbe/auth/sms/SmsVerificationCodeEntity.java`
- Create: `src/main/java/com/qzshop/shopbe/auth/sms/SmsVerificationCodeRepository.java`
- Create: `src/main/java/com/qzshop/shopbe/auth/sms/SmsThrottleException.java`
- Create: `src/main/java/com/qzshop/shopbe/auth/sms/SmsCodeInvalidException.java`
- Create: `src/main/java/com/qzshop/shopbe/auth/sms/SmsVerificationService.java`
- Create: `src/test/java/com/qzshop/shopbe/auth/sms/SmsVerificationServiceTest.java`

- [ ] **Step 1: 写测试 `SmsVerificationServiceTest.java`**

```java
package com.qzshop.shopbe.auth.sms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.qzshop.shopbe.auth.AuthProperties;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({SmsVerificationService.class, SmsVerificationServiceTest.TestConfig.class})
class SmsVerificationServiceTest {

    static final List<String> sent = new ArrayList<>();

    @TestConfiguration
    static class TestConfig {
        @Bean AuthProperties authProperties() {
            AuthProperties p = new AuthProperties();
            p.getJwt().setIssuer("t");
            p.getJwt().setSecret("0123456789-0123456789-0123456789-AB");
            p.getSms().setCodeTtl(Duration.ofMinutes(5));
            p.getSms().setResendCooldown(Duration.ofSeconds(60));
            p.getSms().setDailyLimit(3);
            return p;
        }
        @Bean SmsProvider smsProvider() {
            return (phone, code, purpose) -> sent.add(phone + ":" + code);
        }
    }

    @Autowired SmsVerificationService svc;
    @Autowired SmsVerificationCodeRepository repo;

    @BeforeEach
    void reset() { repo.deleteAll(); sent.clear(); }

    @Test
    void sendsAndPersistsHashedCode() {
        svc.send("13800000001", SmsPurpose.SMS_LOGIN, "127.0.0.1");
        assertThat(repo.count()).isEqualTo(1);
        assertThat(repo.findAll().get(0).getCodeHash()).hasSize(64);
        assertThat(sent).hasSize(1);
    }

    @Test
    void resendCooldownTriggers429() {
        svc.send("13800000002", SmsPurpose.SMS_LOGIN, null);
        assertThatThrownBy(() -> svc.send("13800000002", SmsPurpose.SMS_LOGIN, null))
            .isInstanceOf(SmsThrottleException.class);
    }

    @Test
    void verifyConsumesCode() {
        svc.send("13800000003", SmsPurpose.BIND_PHONE, null);
        // 取出明文是不可能的；通过 fake provider 取
        String code = sent.get(0).split(":")[1];
        svc.verifyAndConsume("13800000003", SmsPurpose.BIND_PHONE, code);
        assertThat(repo.findAll().get(0).getConsumedAt()).isNotNull();
    }

    @Test
    void verifyWrongCodeIncrementsAttemptsAndFinallyInvalidates() {
        svc.send("13800000004", SmsPurpose.SMS_LOGIN, null);
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> svc.verifyAndConsume("13800000004", SmsPurpose.SMS_LOGIN, "000000"))
                .isInstanceOf(SmsCodeInvalidException.class);
        }
        // 第 6 次即使密码对了也已 invalidated
        String code = sent.get(0).split(":")[1];
        assertThatThrownBy(() -> svc.verifyAndConsume("13800000004", SmsPurpose.SMS_LOGIN, code))
            .isInstanceOf(SmsCodeInvalidException.class);
    }
}
```

- [ ] **Step 2: 跑确认红**

Run: `./gradlew test --tests SmsVerificationServiceTest --quiet`
Expected: 编译失败。

- [ ] **Step 3: 创建 entity**

```java
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
```

- [ ] **Step 4: 创建 repository**

```java
package com.qzshop.shopbe.auth.sms;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SmsVerificationCodeRepository extends JpaRepository<SmsVerificationCodeEntity, Long> {

    @Query("select c from SmsVerificationCodeEntity c " +
           "where c.phone = :phone and c.purpose = :purpose " +
           "order by c.createdAt desc")
    List<SmsVerificationCodeEntity> findRecent(@Param("phone") String phone,
                                               @Param("purpose") String purpose,
                                               Pageable page);

    @Query("select count(c) from SmsVerificationCodeEntity c " +
           "where c.phone = :phone and c.createdAt >= :since")
    long countByPhoneSince(@Param("phone") String phone, @Param("since") LocalDateTime since);

    default Optional<SmsVerificationCodeEntity> findLatest(String phone, String purpose) {
        var list = findRecent(phone, purpose, Pageable.ofSize(1));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
```

- [ ] **Step 5: 异常类**

```java
package com.qzshop.shopbe.auth.sms;
public class SmsThrottleException extends RuntimeException {
    private final long retryAfterSeconds;
    public SmsThrottleException(String msg, long retryAfterSeconds) { super(msg); this.retryAfterSeconds = retryAfterSeconds; }
    public long getRetryAfterSeconds() { return retryAfterSeconds; }
}
```

```java
package com.qzshop.shopbe.auth.sms;
public class SmsCodeInvalidException extends RuntimeException {
    public SmsCodeInvalidException(String msg) { super(msg); }
}
```

- [ ] **Step 6: 创建 `SmsVerificationService.java`**

```java
package com.qzshop.shopbe.auth.sms;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qzshop.shopbe.auth.AuthProperties;

@Service
public class SmsVerificationService {

    private static final SecureRandom RNG = new SecureRandom();
    private static final int MAX_ATTEMPTS = 5;

    private final SmsVerificationCodeRepository repo;
    private final SmsProvider provider;
    private final AuthProperties props;

    public SmsVerificationService(SmsVerificationCodeRepository repo,
                                  SmsProvider provider,
                                  AuthProperties props) {
        this.repo = repo;
        this.provider = provider;
        this.props = props;
    }

    @Transactional
    public void send(String phone, SmsPurpose purpose, String ip) {
        Duration cooldown = props.getSms().getResendCooldown();
        Optional<SmsVerificationCodeEntity> last = repo.findLatest(phone, purpose.name());
        if (last.isPresent() && last.get().getCreatedAt().plus(cooldown).isAfter(LocalDateTime.now())) {
            long left = Duration.between(LocalDateTime.now(), last.get().getCreatedAt().plus(cooldown)).getSeconds();
            throw new SmsThrottleException("resend cooldown", Math.max(1, left));
        }
        long sentToday = repo.countByPhoneSince(phone, LocalDateTime.now().minusHours(24));
        if (sentToday >= props.getSms().getDailyLimit()) {
            throw new SmsThrottleException("daily limit reached", 3600);
        }
        String code = String.format("%06d", RNG.nextInt(1_000_000));
        SmsVerificationCodeEntity row = new SmsVerificationCodeEntity();
        row.setPhone(phone);
        row.setPurpose(purpose.name());
        row.setCodeHash(sha256(code));
        row.setExpiresAt(LocalDateTime.now().plus(props.getSms().getCodeTtl()));
        row.setIp(ip);
        repo.save(row);
        provider.send(phone, code, purpose);
    }

    @Transactional
    public void verifyAndConsume(String phone, SmsPurpose purpose, String code) {
        SmsVerificationCodeEntity row = repo.findLatest(phone, purpose.name())
            .orElseThrow(() -> new SmsCodeInvalidException("no code"));
        if (row.getConsumedAt() != null) throw new SmsCodeInvalidException("consumed");
        if (row.getExpiresAt().isBefore(LocalDateTime.now())) throw new SmsCodeInvalidException("expired");
        if (row.getAttempts() >= MAX_ATTEMPTS) throw new SmsCodeInvalidException("too many attempts");
        if (!row.getCodeHash().equals(sha256(code))) {
            row.setAttempts(row.getAttempts() + 1);
            throw new SmsCodeInvalidException("mismatch");
        }
        row.setConsumedAt(LocalDateTime.now());
    }

    private static String sha256(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
```

- [ ] **Step 7: 跑测试**

Run: `./gradlew test --tests SmsVerificationServiceTest --quiet`
Expected: 4 tests passed.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/qzshop/shopbe/auth/sms/SmsVerificationCodeEntity.java \
        src/main/java/com/qzshop/shopbe/auth/sms/SmsVerificationCodeRepository.java \
        src/main/java/com/qzshop/shopbe/auth/sms/SmsThrottleException.java \
        src/main/java/com/qzshop/shopbe/auth/sms/SmsCodeInvalidException.java \
        src/main/java/com/qzshop/shopbe/auth/sms/SmsVerificationService.java \
        src/test/java/com/qzshop/shopbe/auth/sms/SmsVerificationServiceTest.java
git commit -m "feat(auth): SmsVerificationService with throttle / attempts / hashed storage

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 9: StaffEntity（精简版）+ LoginAttemptService + @DataJpaTest

> PR1 只需要写计数与锁定，业务字段（角色等）等 PR2 用到时按需扩展。

**Files:**
- Create: `src/main/java/com/qzshop/shopbe/auth/staff/StaffEntity.java`
- Create: `src/main/java/com/qzshop/shopbe/auth/staff/StaffRepository.java`
- Create: `src/main/java/com/qzshop/shopbe/auth/staff/LoginAttemptService.java`
- Create: `src/main/java/com/qzshop/shopbe/auth/staff/StaffLockedException.java`
- Create: `src/test/java/com/qzshop/shopbe/auth/staff/LoginAttemptServiceTest.java`

- [ ] **Step 1: 测试 `LoginAttemptServiceTest.java`**

```java
package com.qzshop.shopbe.auth.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.qzshop.shopbe.auth.AuthProperties;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({LoginAttemptService.class, LoginAttemptServiceTest.TestConfig.class})
class LoginAttemptServiceTest {

    @TestConfiguration
    static class TestConfig {
        @Bean AuthProperties authProperties() {
            AuthProperties p = new AuthProperties();
            p.getJwt().setIssuer("t");
            p.getJwt().setSecret("0123456789-0123456789-0123456789-AB");
            p.getStaff().setMaxFailedAttempts(3);
            p.getStaff().setLockDuration(Duration.ofMinutes(15));
            return p;
        }
    }

    @Autowired StaffRepository staffRepo;
    @Autowired LoginAttemptService svc;

    private StaffEntity s;

    @BeforeEach
    void prepare() {
        staffRepo.deleteAll();
        s = new StaffEntity();
        s.setPhone("13800000099");
        s.setStoreId(1L);
        s.setName("test");
        staffRepo.save(s);
    }

    @Test
    void recordFailIncrementsCount() {
        svc.recordFailure(s.getId());
        assertThat(staffRepo.findById(s.getId()).orElseThrow().getFailedLoginCount()).isEqualTo(1);
    }

    @Test
    void recordFailReachesLimitAndLocks() {
        svc.recordFailure(s.getId());
        svc.recordFailure(s.getId());
        svc.recordFailure(s.getId());
        StaffEntity refreshed = staffRepo.findById(s.getId()).orElseThrow();
        assertThat(refreshed.getLockedUntil()).isAfter(java.time.LocalDateTime.now());
        assertThat(refreshed.getFailedLoginCount()).isZero();
    }

    @Test
    void ensureNotLockedThrowsWhenLocked() {
        s.setLockedUntil(java.time.LocalDateTime.now().plusMinutes(10));
        staffRepo.save(s);
        assertThatThrownBy(() -> svc.ensureNotLocked(s)).isInstanceOf(StaffLockedException.class);
    }

    @Test
    void recordSuccessClearsCounters() {
        s.setFailedLoginCount(2);
        s.setLockedUntil(java.time.LocalDateTime.now().minusMinutes(1));
        staffRepo.save(s);
        svc.recordSuccess(s.getId());
        StaffEntity refreshed = staffRepo.findById(s.getId()).orElseThrow();
        assertThat(refreshed.getFailedLoginCount()).isZero();
        assertThat(refreshed.getLockedUntil()).isNull();
        assertThat(refreshed.getLastLoginAt()).isNotNull();
    }
}
```

- [ ] **Step 2: 跑确认红**

Run: `./gradlew test --tests LoginAttemptServiceTest --quiet`
Expected: 编译失败。

- [ ] **Step 3: 创建 `StaffEntity.java`**

> 字段对齐 schema 中现有列 + Task 2 新增列；`store_id NOT NULL` 是已有约束，PR1 测试里随便给 `1L` 满足即可。

```java
package com.qzshop.shopbe.auth.staff;

import java.time.LocalDateTime;

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
}
```

- [ ] **Step 4: `StaffRepository.java`**

```java
package com.qzshop.shopbe.auth.staff;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffRepository extends JpaRepository<StaffEntity, Long> {
    Optional<StaffEntity> findByPhone(String phone);
}
```

- [ ] **Step 5: `StaffLockedException.java`**

```java
package com.qzshop.shopbe.auth.staff;

public class StaffLockedException extends RuntimeException {
    private final long retryAfterSeconds;
    public StaffLockedException(long retryAfterSeconds) {
        super("Account locked");
        this.retryAfterSeconds = retryAfterSeconds;
    }
    public long getRetryAfterSeconds() { return retryAfterSeconds; }
}
```

- [ ] **Step 6: `LoginAttemptService.java`**

```java
package com.qzshop.shopbe.auth.staff;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qzshop.shopbe.auth.AuthProperties;

@Service
public class LoginAttemptService {

    private final StaffRepository repo;
    private final AuthProperties props;

    public LoginAttemptService(StaffRepository repo, AuthProperties props) {
        this.repo = repo;
        this.props = props;
    }

    public void ensureNotLocked(StaffEntity staff) {
        LocalDateTime until = staff.getLockedUntil();
        if (until != null && until.isAfter(LocalDateTime.now())) {
            long left = Duration.between(LocalDateTime.now(), until).getSeconds();
            throw new StaffLockedException(Math.max(1, left));
        }
    }

    @Transactional
    public void recordFailure(Long staffId) {
        StaffEntity s = repo.findById(staffId).orElseThrow();
        int next = s.getFailedLoginCount() + 1;
        if (next >= props.getStaff().getMaxFailedAttempts()) {
            s.setFailedLoginCount(0);
            s.setLockedUntil(LocalDateTime.now().plus(props.getStaff().getLockDuration()));
        } else {
            s.setFailedLoginCount(next);
        }
    }

    @Transactional
    public void recordSuccess(Long staffId) {
        StaffEntity s = repo.findById(staffId).orElseThrow();
        s.setFailedLoginCount(0);
        s.setLockedUntil(null);
        s.setLastLoginAt(LocalDateTime.now());
    }
}
```

- [ ] **Step 7: 跑测试**

Run: `./gradlew test --tests LoginAttemptServiceTest --quiet`
Expected: 4 tests passed.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/qzshop/shopbe/auth/staff/StaffEntity.java \
        src/main/java/com/qzshop/shopbe/auth/staff/StaffRepository.java \
        src/main/java/com/qzshop/shopbe/auth/staff/StaffLockedException.java \
        src/main/java/com/qzshop/shopbe/auth/staff/LoginAttemptService.java \
        src/test/java/com/qzshop/shopbe/auth/staff/LoginAttemptServiceTest.java
git commit -m "feat(auth): StaffEntity (auth view) + LoginAttemptService

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 10: Spring Security 三链 + JWT Filter + 集成测试

**Files:**
- Create: `src/main/java/com/qzshop/shopbe/auth/security/StaffPrincipal.java`
- Create: `src/main/java/com/qzshop/shopbe/auth/security/JsonAuthEntryPoint.java`
- Create: `src/main/java/com/qzshop/shopbe/auth/security/JsonAccessDeniedHandler.java`
- Create: `src/main/java/com/qzshop/shopbe/auth/security/JwtAuthenticationFilter.java`
- Create: `src/main/java/com/qzshop/shopbe/auth/security/SecurityConfig.java`
- Create: `src/test/java/com/qzshop/shopbe/auth/security/SecurityFilterChainIT.java`

- [ ] **Step 1: 测试 `SecurityFilterChainIT.java`** — 验证三件事：(a) `/api/stores` 仍匿名可达；(b) `/api/admin/me`（PR1 还没实现，但 controller 不存在 → 期望 404 而非 401，证明 adminChain 放行了认证后的请求；为简化 PR1，我们暂时只验证未带 token 时返回 401）；(c) BIND_PENDING token 调 admin 接口被拒。

```java
package com.qzshop.shopbe.auth.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    void storesEndpointStillPublic() throws Exception {
        mvc().perform(get("/api/stores")).andExpect(status().isOk());
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
            .andExpect(status().isNotFound()); // 通过认证，到了 mvc 层但找不到 handler
    }
}
```

> 测试依赖 `application-test.properties` 设可用的 secret 和数据源。

- [ ] **Step 2: 创建 `src/test/resources/application-test.properties`**

```properties
auth.jwt.secret=test-secret-test-secret-test-secret-32+
spring.datasource.url=jdbc:h2:mem:authit;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
spring.datasource.username=sa
spring.datasource.password=
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
```

- [ ] **Step 3: 跑确认红**

Run: `./gradlew test --tests SecurityFilterChainIT --quiet`
Expected: 编译失败 / 路由错误。

- [ ] **Step 4: `StaffPrincipal.java`**

```java
package com.qzshop.shopbe.auth.security;

import java.util.List;

public record StaffPrincipal(long staffId, List<String> roles) {}
```

- [ ] **Step 5: `JsonAuthEntryPoint.java`**

```java
package com.qzshop.shopbe.auth.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JsonAuthEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest req, HttpServletResponse resp, AuthenticationException ex)
            throws IOException {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resp.getWriter().write("{\"message\":\"Authentication required\"}");
    }
}
```

- [ ] **Step 6: `JsonAccessDeniedHandler.java`**

```java
package com.qzshop.shopbe.auth.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest req, HttpServletResponse resp, AccessDeniedException ex)
            throws IOException {
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resp.getWriter().write("{\"message\":\"Forbidden\"}");
    }
}
```

- [ ] **Step 7: `JwtAuthenticationFilter.java`**

```java
package com.qzshop.shopbe.auth.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.qzshop.shopbe.auth.token.JwtService;
import com.qzshop.shopbe.auth.token.ParsedToken;
import com.qzshop.shopbe.auth.token.SubjectType;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwt;

    public JwtAuthenticationFilter(JwtService jwt) { this.jwt = jwt; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                ParsedToken p = jwt.parse(header.substring(7));
                List<SimpleGrantedAuthority> auths = (p.type() == SubjectType.STAFF)
                    ? Stream(p.roles()).map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                          .toList() : List.of();
                String typeAuthority = "TYPE_" + p.type().name();
                var combined = new java.util.ArrayList<SimpleGrantedAuthority>(auths);
                combined.add(new SimpleGrantedAuthority(typeAuthority));
                StaffPrincipal principal = new StaffPrincipal(p.subjectId(), p.roles());
                var auth = new UsernamePasswordAuthenticationToken(principal, null, combined);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ignored) {
                // 不阻断，由后续 401/403 处理
            }
        }
        chain.doFilter(req, resp);
    }

    private static <T> java.util.stream.Stream<T> Stream(java.util.List<T> l) { return l.stream(); }
}
```

- [ ] **Step 8: `SecurityConfig.java`**

```java
package com.qzshop.shopbe.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final JsonAuthEntryPoint entryPoint;
    private final JsonAccessDeniedHandler deniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter,
                          JsonAuthEntryPoint entryPoint,
                          JsonAccessDeniedHandler deniedHandler) {
        this.jwtFilter = jwtFilter;
        this.entryPoint = entryPoint;
        this.deniedHandler = deniedHandler;
    }

    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean @Order(1)
    SecurityFilterChain publicChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/auth/refresh", "/api/admin/auth/login",
                             "/api/admin/auth/sms/send", "/api/admin/auth/sms/login",
                             "/api/admin/auth/sso/**", "/api/admin/auth/reset-password",
                             "/api/admin/auth/bind-phone")
            .csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a.anyRequest().permitAll());
        return http.build();
    }

    @Bean @Order(2)
    SecurityFilterChain adminChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/admin/**")
            .csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler))
            .authorizeHttpRequests(a -> a
                .anyRequest().hasAuthority("TYPE_STAFF"));
        return http.build();
    }

    @Bean @Order(99)
    SecurityFilterChain defaultChain(HttpSecurity http) throws Exception {
        http
            .csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a.anyRequest().permitAll());
        return http.build();
    }
}
```

- [ ] **Step 9: 跑测试**

Run: `./gradlew test --tests SecurityFilterChainIT --quiet`
Expected: 4 tests passed.

> 若 `adminEndpointWithStaffTokenReaches404` 实际返回 403，检查 `JwtAuthenticationFilter` 是否真的把 `TYPE_STAFF` authority 加进去了。
> 若 `storesEndpointStillPublic` 失败，确认 defaultChain 的 `@Order(99)` 真的低于 adminChain 且 `/api/stores` 不匹配 `/api/admin/**`。

- [ ] **Step 10: 跑全量测试确保没回归**

Run: `./gradlew test --quiet`
Expected: 全部绿。

- [ ] **Step 11: Commit**

```bash
git add src/main/java/com/qzshop/shopbe/auth/security/ \
        src/test/java/com/qzshop/shopbe/auth/security/ \
        src/test/resources/application-test.properties
git commit -m "feat(auth): SecurityConfig with 3 filter chains + JWT filter + JSON 401/403 handlers

- publicChain (@Order 1): permitAll on auth public endpoints
- adminChain (@Order 2): /api/admin/** requires TYPE_STAFF authority
- defaultChain (@Order 99): permitAll for legacy /api/stores etc.
- JwtAuthenticationFilter: parses Bearer token, fills SecurityContext with TYPE_STAFF/BIND_PENDING authority
- IT verifies: stores public, admin requires staff token, BIND_PENDING token rejected

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Self-Review Notes

- **Spec 覆盖**：PR1 范围（DB、JWT、TokenService、SmsProvider+SmsVerificationService、LoginAttempt、Security 三链）每条都有对应 task。`/api/admin/me` 等业务端点显式留给 PR2，spec §9 已说明。
- **类型一致性**：`StaffEntity.failedLoginCount` 用 `Integer`（DB `INT NOT NULL DEFAULT 0`，JPA setter 收 Integer）；`SmsVerificationService.MAX_ATTEMPTS=5` 与 spec §3.3 一致；`SubjectType` enum 与 JWT claim `typ` 字符串一致。
- **没有占位符**：每个步骤都有可执行命令或完整代码。
- **测试粒度**：每个核心组件都有红→绿→commit 节奏，集成测试单独覆盖 Security 三链交互。
- **PR1 显式不做**：`/api/admin/auth/**` 控制器、`/api/admin/me`、自助注册、SSO provider 实现——这些在 PR2、PR3。

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-05-14-auth-foundation-pr1.md`.

执行选项：

1. **Subagent-Driven（推荐）**：我每个 task 派发独立 subagent，task 之间我做 code review，节奏快、上下文干净。
2. **Inline Execution**：本会话内连续执行，按 task 节点 checkpoint。

请选择。
