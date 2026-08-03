package top.egon.cola.component.gateway.test.mcp.provider;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.ddc.config.DdcProperties;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;

import static org.assertj.core.api.Assertions.assertThat;

class McpTestProviderConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withInitializer(
                            new ConfigDataApplicationContextInitializer()
                    )
                    .withUserConfiguration(TestConfiguration.class);

    @Test
    void bindsHostLocalDdcAndGatewayReportingEnvironment() {
        contextRunner.withSystemProperties(
                        "DDC_ADMIN_ENDPOINT=http://127.0.0.1:18110",
                        "DDC_OPENAPI_ACCESS_KEY=ddc-access",
                        "DDC_OPENAPI_SECRET=ddc-secret",
                        "DDC_REGISTRY_REDIS_PASSWORD=redis-secret",
                        "GATEWAY_ADMIN_BASE_URL=http://127.0.0.1:18140",
                        "GATEWAY_REPORT_ACCESS_KEY=gateway-access",
                        "GATEWAY_REPORT_SECRET_KEY=gateway-secret",
                        "GATEWAY_REPORT_STATE_FILE=target/mcp-provider-state.json"
                )
                .run(context -> {
                    DdcProperties ddc = context.getBean(DdcProperties.class);
                    GatewayReportingProperties reporting = context.getBean(
                            GatewayReportingProperties.class
                    );

                    assertThat(ddc.getAdmin().getEndpoint())
                            .isEqualTo("http://127.0.0.1:18110");
                    assertThat(ddc.getAdmin().getAccessKey())
                            .isEqualTo("ddc-access");
                    assertThat(ddc.getAdmin().getSecretKey())
                            .isEqualTo("ddc-secret");
                    assertThat(ddc.getRedis().getPassword())
                            .isEqualTo("redis-secret");
                    assertThat(reporting.getAdminBaseUrl())
                            .isEqualTo("http://127.0.0.1:18140");
                    assertThat(reporting.getAccessKey())
                            .isEqualTo("gateway-access");
                    assertThat(reporting.getSecretKey())
                            .isEqualTo("gateway-secret");
                    assertThat(reporting.getStateFile())
                            .isEqualTo("target/mcp-provider-state.json");
                });
    }

    @EnableConfigurationProperties({
            DdcProperties.class,
            GatewayReportingProperties.class
    })
    static class TestConfiguration {
    }
}
