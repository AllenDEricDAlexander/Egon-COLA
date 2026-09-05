package top.egon.cola.component.gateway.mcp.engine.bootstrap;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class McpGatewayEnginePackageBoundaryTest {

    @Test
    void cannotDependOnApiExecutableIngressOrCompiler() throws Exception {
        Path root = Path.of("src/main/java");
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertFalse(source.contains("import top.egon.cola.component.gateway.engine."), file.toString());
                for (String type : List.of("RpcGatewayServer", "RpcGatewaySlotRuntime",
                        "DefaultGatewayHttpDataPlaneHandler", "ApiRpcGatewayCompiledRulesDTO")) {
                    assertFalse(source.contains(type), file + ": " + type);
                }
            }
        }
        assertFalse(Files.readString(Path.of("pom.xml")).contains("<artifactId>egon-cola-platform-gateway-engine</artifactId>"));
        assertThrows(ClassNotFoundException.class, () -> Class.forName(
                "top.egon.cola.component.gateway.engine.GatewayEngineApplication"));
    }
}
