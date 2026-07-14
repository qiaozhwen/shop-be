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
}
