package top.egon.cola.component.gateway.test.process;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayProcessHarnessTest {

    @Test
    void builderProducesImmutableRedactedDiagnosticSpec() {
        GatewayProcessSpec spec = GatewayProcessSpec.builder(
                        "admin",
                        "example.AdminApplication"
                )
                .argument("server.port", 18080)
                .argument("gateway.admin.secret-key", "do-not-log")
                .environment("DATABASE_PASSWORD", "do-not-log-either")
                .startupTimeout(Duration.ofSeconds(30))
                .build();

        assertThat(spec.arguments()).contains(
                "--gateway.admin.secret-key=do-not-log"
        );
        assertThat(spec.redactedArguments())
                .contains("--gateway.admin.secret-key=******")
                .doesNotContain("--gateway.admin.secret-key=do-not-log");
        assertThat(spec.redactedEnvironment())
                .containsEntry("DATABASE_PASSWORD", "******");
        assertThat(spec.startupTimeout()).isEqualTo(Duration.ofSeconds(30));
    }
}
