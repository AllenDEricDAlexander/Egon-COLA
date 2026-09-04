package top.egon.cola.component.gateway.runtime;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayRuntimePackageBoundaryTest {

    private static final Path SOURCE_ROOT = Path.of(
            "src/main/java/top/egon/cola/component/gateway/runtime"
    );

    @Test
    void containsOnlySharedCapabilities() throws IOException {
        assertTrue(Files.isDirectory(SOURCE_ROOT));
        Set<String> allowed = Set.of(
                "config", "provider", "security", "traffic", "transport",
                "observability", "http", "rpc", "operation", "rule"
        );
        try (Stream<Path> files = Files.walk(SOURCE_ROOT)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                assertTrue(allowed.contains(SOURCE_ROOT.relativize(file).getName(0).toString()),
                        () -> "Unexpected runtime capability: " + file);
                String source = Files.readString(file);
                assertFalse(source.contains("@SpringBootApplication"), file.toString());
                assertFalse(source.contains("import top.egon.cola.component.gateway.engine."), file.toString());
                assertFalse(source.contains("import top.egon.cola.component.gateway.mcp."), file.toString());
                assertFalse(source.contains("import top.egon.cola.component.gateway.admin."), file.toString());
            }
        }
    }

    @Test
    void doesNotDependOnExecutableModules() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"));
        for (String artifact : List.of(
                "egon-cola-platform-gateway-engine", "egon-cola-platform-gateway-mcp-engine",
                "egon-cola-platform-gateway-mcp-core", "egon-cola-platform-gateway-admin",
                "spring-boot-maven-plugin"
        )) {
            assertFalse(pom.contains("<artifactId>" + artifact + "</artifactId>"), artifact);
        }
    }
}
