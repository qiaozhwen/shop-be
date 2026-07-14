package com.qzshop.shopbe.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class DeploymentConfigTest {

    @Test
    void automaticAndManualDeploymentsJoinTheSharedNetwork() throws Exception {
        String workflow = Files.readString(Path.of(".github/workflows/deploy.yml"));
        String manual = Files.readString(Path.of("deploy/deploy.sh"));

        assertThat(workflow).contains("docker network inspect shop-network");
        assertThat(workflow).contains("--network shop-network");
        assertThat(manual).contains("docker network inspect shop-network");
        assertThat(manual).contains("--network shop-network");
        assertThat(manual).doesNotContain("--network shop-net ");
    }

    @Test
    void composeAndHandoffAssetsCoverTheWholeCustomerStack() throws Exception {
        String compose = Files.readString(Path.of("docker-compose.yml"));
        String dockerfile = Files.readString(Path.of("Dockerfile"));
        String readme = Files.readString(Path.of("README.md"));
        String exampleEnv = Files.readString(Path.of(".env.example"));

        assertThat(compose).contains("postgres:", "shop-be:", "shop-fe:", "healthcheck:", "postgres-data:");
        assertThat(dockerfile).contains("HEALTHCHECK");
        assertThat(exampleEnv).contains("AUTH_JWT_SECRET=", "BOOTSTRAP_ADMIN_PHONE=", "BOOTSTRAP_ADMIN_PASSWORD=");
        assertThat(readme).contains("启动", "首次登录", "备份", "恢复", "升级", "回滚", "密钥轮换");
    }
}
