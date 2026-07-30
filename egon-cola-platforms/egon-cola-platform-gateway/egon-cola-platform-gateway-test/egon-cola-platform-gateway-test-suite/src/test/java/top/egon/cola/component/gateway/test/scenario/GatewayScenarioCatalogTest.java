package top.egon.cola.component.gateway.test.scenario;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayScenarioCatalogTest {

    @Test
    void catalogCoversEveryRequiredLiveCapabilityWithoutMockGateway() {
        assertThat(GatewayScenarioCatalog.all())
                .extracting(GatewayScenarioCatalog.Scenario::code)
                .containsExactly(
                        "starter-report",
                        "provider-discovery",
                        "http-forward",
                        "rpc-forward",
                        "http-to-rpc",
                        "load-balance",
                        "rule-release",
                        "traffic-governance",
                        "security-extension",
                        "trace-kafka",
                        "failure-recovery",
                        "openai-http-json",
                        "sse-stream",
                        "multipart-upload",
                        "binary-stream",
                        "realtime-websocket",
                        "transport-lifecycle-safety",
                        "rpc-slot-invariant"
                )
                .noneMatch(code -> code.contains("mock"));
        assertThat(GatewayScenarioCatalog.all())
                .allMatch(
                        GatewayScenarioCatalog.Scenario::realProcessRequired
                );
    }
}
