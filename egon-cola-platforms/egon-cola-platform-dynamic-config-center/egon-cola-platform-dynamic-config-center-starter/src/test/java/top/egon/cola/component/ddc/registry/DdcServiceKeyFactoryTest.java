package top.egon.cola.component.ddc.registry;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.config.DdcProperties;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;

import static org.assertj.core.api.Assertions.assertThat;

class DdcServiceKeyFactoryTest {

    @Test
    void buildsExplicitGatewayTargetWithoutChangingLocalScope() {
        DdcProperties properties = new DdcProperties();
        properties.setBizCode("retail-biz");
        properties.setAppCode("orders-app");
        properties.setEnv("test");
        properties.setNamespace("orders");
        DdcServiceKeyFactory factory = new DdcServiceKeyFactory(properties);

        var local = factory.fromScope(
                DdcServiceKind.RPC_PROVIDER,
                "orders-rpc",
                "default",
                "1.0.0",
                "grpc"
        );
        var gateway = factory.fromTargetScope(
                "platform-biz",
                "gateway-app",
                "test",
                "shared",
                DdcServiceKind.INTERNAL_GATEWAY,
                "egon-gateway-rpc",
                "default",
                "1.0.0",
                "grpc"
        );

        assertThat(local.bizCode()).isEqualTo("retail-biz");
        assertThat(local.appCode()).isEqualTo("orders-app");
        assertThat(gateway.bizCode()).isEqualTo("platform-biz");
        assertThat(gateway.appCode()).isEqualTo("gateway-app");
        assertThat(gateway.env()).isEqualTo("test");
        assertThat(gateway.namespace()).isEqualTo("shared");
    }
}
