package top.egon.cola.component.yuheng.test.rpc.provider;

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
                "yuheng.zone",
                "zone-a"
        ).containsEntry(
                "yuheng.weight",
                "100"
        ).doesNotContainKeys(
                "yuheng.definition-set-id",
                "yuheng.artifact-version",
                "yuheng.build-id"
        );
    }
}
