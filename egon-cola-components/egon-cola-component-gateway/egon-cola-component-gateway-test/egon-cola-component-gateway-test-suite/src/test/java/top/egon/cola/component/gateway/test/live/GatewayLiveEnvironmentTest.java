package top.egon.cola.component.gateway.test.live;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayLiveEnvironmentTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void allocatesUniqueScopeAndPerEngineDataDirectories() throws Exception {
        try (GatewayLiveEnvironment first = new GatewayLiveEnvironment(
                "http-topology",
                temporaryDirectory
        ); GatewayLiveEnvironment second = new GatewayLiveEnvironment(
                "http-topology",
                temporaryDirectory
        )) {
            assertThat(first.scope().suffix())
                    .isNotEqualTo(second.scope().suffix());
            assertThat(first.dataDirectory("gateway-engine-1"))
                    .isNotEqualTo(first.dataDirectory("gateway-engine-2"));
            assertThat(first.dataDirectory("gateway-engine-1"))
                    .isNotEqualTo(second.dataDirectory("gateway-engine-1"));
            assertThat(first.processOutputDirectory())
                    .isNotEqualTo(second.processOutputDirectory());
        }
    }
}
