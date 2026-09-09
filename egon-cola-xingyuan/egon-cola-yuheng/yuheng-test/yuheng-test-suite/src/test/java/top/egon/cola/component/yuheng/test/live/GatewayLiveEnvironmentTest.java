package top.egon.cola.component.yuheng.test.live;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.net.URI;
import top.egon.cola.component.yuheng.contract.runtime.GatewayEngineRoleEnum;
import top.egon.cola.component.yuheng.test.process.GatewayProcessHarness;
import top.egon.cola.component.yuheng.test.process.GatewayProcessSpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class GatewayLiveEnvironmentTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void separatesRoleEndpointsAndDirectoriesWithoutStartingEngines() throws Exception {
        try (var environment = new GatewayLiveEnvironment("roles", temporaryDirectory)) {
            var api = describedEngine("api-1", GatewayEngineRoleEnum.API_RPC, 18081, 18083);
            var mcp = describedEngine("mcp-1", GatewayEngineRoleEnum.MCP, 18084, 18085);
            assertThat(environment.dataPlaneBaseUri(GatewayEngineRoleEnum.API_RPC, api))
                    .isNotEqualTo(environment.dataPlaneBaseUri(GatewayEngineRoleEnum.MCP, mcp));
            assertThat(environment.managementBaseUri(GatewayEngineRoleEnum.MCP, mcp).getPort()).isEqualTo(18085);
            assertThat(environment.dataDirectory(api.name())).isNotEqualTo(environment.dataDirectory(mcp.name()));
            assertThatThrownBy(() -> environment.dataPlaneBaseUri(GatewayEngineRoleEnum.MCP, api))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("wrong role");
        }
    }

    private GatewayProcessHarness.ChildProcess describedEngine(String name, GatewayEngineRoleEnum role,
                                                               int dataPort, int managementPort) {
        var spec = GatewayProcessSpec.engineBuilder(name, role,
                URI.create("http://127.0.0.1:" + dataPort), URI.create("http://127.0.0.1:" + managementPort)).build();
        return new GatewayProcessHarness.ChildProcess(name, spec, 1, mock(Process.class),
                temporaryDirectory.resolve(name + ".log"), temporaryDirectory.resolve(name + ".json"),
                temporaryDirectory.resolve(name + "-lkg"));
    }

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
            assertThat(first.dataDirectory("yuheng-biz-gateway-1"))
                    .isNotEqualTo(first.dataDirectory("yuheng-biz-gateway-2"));
            assertThat(first.dataDirectory("yuheng-biz-gateway-1"))
                    .isNotEqualTo(second.dataDirectory("yuheng-biz-gateway-1"));
            assertThat(first.processOutputDirectory())
                    .isNotEqualTo(second.processOutputDirectory());
        }
    }
}
