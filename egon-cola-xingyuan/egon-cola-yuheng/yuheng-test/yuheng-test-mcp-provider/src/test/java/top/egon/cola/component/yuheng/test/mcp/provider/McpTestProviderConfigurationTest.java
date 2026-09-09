package top.egon.cola.component.yuheng.test.mcp.provider;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.rpc.tianshu.autoconfigure.DdcRpcProperties;

import static org.assertj.core.api.Assertions.assertThat;

class McpTestProviderConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withInitializer(
                            new ConfigDataApplicationContextInitializer()
                    )
                    .withUserConfiguration(TestConfiguration.class);

    @Test
    void bindsHostLocalDdcEnvironment() {
        contextRunner.withSystemProperties(
                        "DDC_RPC_TARGET=dns:///127.0.0.1:19110",
                        "DDC_RPC_RUNTIME_ACCESS_KEY=ddc-access",
                        "DDC_RPC_RUNTIME_SECRET_KEY=ddc-secret",
                        "DDC_REGISTRY_REDIS_PASSWORD=redis-secret",
                        "MCP_TEST_PROVIDER_INSTANCE_ID=mcp-provider-local-1"
                )
                .run(context -> {
                    DdcProperties ddc = context.getBean(DdcProperties.class);
                    DdcRpcProperties rpc = context.getBean(
                            DdcRpcProperties.class
                    );
                    assertThat(rpc.getTarget())
                            .isEqualTo("dns:///127.0.0.1:19110");
                    assertThat(rpc.getAuth().getRuntime().getAccessKey())
                            .isEqualTo("ddc-access");
                    assertThat(rpc.getAuth().getRuntime().getSecretKey())
                            .isEqualTo("ddc-secret");
                    assertThat(ddc.getRedis().getPassword())
                            .isEqualTo("redis-secret");
                    assertThat(ddc.getInstance().getId())
                            .isEqualTo("mcp-provider-local-1");
                });
    }

    @EnableConfigurationProperties({
            DdcProperties.class,
            DdcRpcProperties.class
    })
    static class TestConfiguration {
    }
}
