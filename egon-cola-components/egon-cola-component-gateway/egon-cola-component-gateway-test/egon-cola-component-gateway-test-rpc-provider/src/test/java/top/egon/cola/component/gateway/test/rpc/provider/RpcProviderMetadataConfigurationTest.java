package top.egon.cola.component.gateway.test.rpc.provider;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.env.MockEnvironment;
import top.egon.cola.component.gateway.contract.definition
        .GatewayDefinitionIdentity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RpcProviderMetadataConfigurationTest {

    @Test
    void contributesTheDefinitionIdentityPublishedByGatewayStarter() {
        GatewayDefinitionIdentity identity =
                new GatewayDefinitionIdentity(
                        "definition-1",
                        "1.0.0",
                        "build-1"
                );
        StaticListableBeanFactory beans =
                new StaticListableBeanFactory(Map.of(
                        "gatewayDefinitionIdentity",
                        identity
                ));
        RpcProviderMetadataConfiguration configuration =
                new RpcProviderMetadataConfiguration();

        Map<String, String> metadata = configuration.gatewayTestMetadata(
                beans.getBeanProvider(GatewayDefinitionIdentity.class),
                new MockEnvironment()
        ).contribute(null);

        assertThat(metadata).containsEntry(
                "gateway.definition-set-id",
                "definition-1"
        ).containsEntry(
                "gateway.artifact-version",
                "1.0.0"
        ).containsEntry(
                "gateway.build-id",
                "build-1"
        ).doesNotContainValue("pending-report");
    }
}
