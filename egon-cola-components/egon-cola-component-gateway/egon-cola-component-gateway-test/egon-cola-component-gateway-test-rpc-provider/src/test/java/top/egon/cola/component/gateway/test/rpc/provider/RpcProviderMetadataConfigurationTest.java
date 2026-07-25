package top.egon.cola.component.gateway.test.rpc.provider;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RpcProviderMetadataConfigurationTest {

    @Test
    void contributesOnlyDeploymentMetadata() {
        RpcProviderMetadataConfiguration configuration =
                new RpcProviderMetadataConfiguration();

        Map<String, String> metadata = configuration.gatewayTestMetadata(
                new MockEnvironment()
        ).contribute(null);

        assertThat(metadata).containsEntry(
                "gateway.zone",
                "zone-a"
        ).containsEntry(
                "gateway.weight",
                "100"
        ).doesNotContainKeys(
                "gateway.definition-set-id",
                "gateway.artifact-version",
                "gateway.build-id"
        );
    }
}
