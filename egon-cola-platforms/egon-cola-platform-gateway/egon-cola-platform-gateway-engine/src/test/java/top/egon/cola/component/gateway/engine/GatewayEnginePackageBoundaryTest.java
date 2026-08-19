package top.egon.cola.component.gateway.engine;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayEnginePackageBoundaryTest {

    private static final Path SOURCE_ROOT = Path.of(
            "src/main/java/top/egon/cola/component/gateway/engine"
    );

    @Test
    void rootContainsOnlyApplicationAndPackageInfoAfterMigration()
            throws IOException {
        assertTrue(Files.isDirectory(SOURCE_ROOT));
        try (Stream<Path> files = Files.list(SOURCE_ROOT)) {
            List<String> directJavaFiles = files
                    .filter(path -> path.getFileName().toString().endsWith(
                            ".java"
                    ))
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();

            assertEquals(
                    List.of("GatewayEngineApplication.java", "package-info.java"),
                    directJavaFiles
            );
        }
    }

    @Test
    void enginePackagesExposeBootstrapCommonAndFeatureRoles()
            throws IOException {
        Set<String> required = Set.of(
                "bootstrap/config",
                "bootstrap/lifecycle",
                "common/config",
                "common/provider/domain",
                "common/provider/service",
                "common/provider/adapter",
                "common/security/domain",
                "common/security/service",
                "common/security/adapter",
                "common/traffic/domain",
                "common/traffic/service",
                "common/traffic/adapter",
                "common/transport/domain",
                "common/transport/service",
                "common/observability/domain",
                "common/observability/service",
                "common/observability/adapter",
                "http/domain",
                "http/service",
                "http/adapter",
                "http/security",
                "http/cors",
                "http/proxy/domain",
                "http/proxy/service",
                "http/common/buffer",
                "http/common/logging",
                "http/websocket/domain",
                "http/websocket/service",
                "http/websocket/adapter",
                "rpc/domain",
                "rpc/service",
                "rpc/adapter",
                "rpc/security",
                "operation/service",
                "operation/adapter",
                "rule/domain",
                "rule/service",
                "rule/repository",
                "rule/adapter/json",
                "mcp/domain",
                "mcp/service",
                "mcp/adapter",
                "mcp/adapter/remote",
                "mcp/adapter/security"
        );
        for (String relativePath : required) {
            assertTrue(
                    Files.isDirectory(SOURCE_ROOT.resolve(relativePath)),
                    () -> "missing Engine target package " + relativePath
            );
        }

        for (String featureRoot : List.of(
                "http",
                "rpc",
                "operation",
                "rule",
                "mcp"
        )) {
            assertNoDirectJavaFiles(featureRoot);
        }

        for (String commonRoot : List.of(
                "provider",
                "security",
                "traffic",
                "transport",
                "observability"
        )) {
            assertNoDirectJavaFiles("common/" + commonRoot);
        }

        for (String oldPackage : List.of(
                "balance",
                "discovery",
                "security",
                "traffic",
                "transport",
                "observability",
                "cors",
                "websocket"
        )) {
            assertFalse(
                    Files.exists(SOURCE_ROOT.resolve(oldPackage)),
                    () -> "legacy Engine package remains " + oldPackage
            );
        }
    }

    @Test
    void commonPackagesDoNotDependOnConcreteFeatures() throws IOException {
        Path commonRoot = SOURCE_ROOT.resolve("common");
        assertTrue(Files.isDirectory(commonRoot));
        try (Stream<Path> files = Files.walk(commonRoot)) {
            List<String> forbiddenImports = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(this::lines)
                    .filter(this::isFeatureImport)
                    .toList();

            assertEquals(List.of(), forbiddenImports);
        }
    }

    private void assertNoDirectJavaFiles(String relativePath)
            throws IOException {
        Path root = SOURCE_ROOT.resolve(relativePath);
        assertTrue(Files.isDirectory(root));
        try (Stream<Path> files = Files.list(root)) {
            List<String> directJavaFiles = files
                    .filter(path -> path.getFileName().toString().endsWith(
                            ".java"
                    ))
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
            assertEquals(
                    List.of(),
                    directJavaFiles,
                    () -> "legacy Engine runtime files remain in "
                            + relativePath
            );
        }
    }

    private Stream<String> lines(Path path) {
        try {
            return Files.readAllLines(path).stream();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private boolean isFeatureImport(String line) {
        if (!line.startsWith(
                "import top.egon.cola.component.gateway.engine."
        )) {
            return false;
        }
        return List.of(
                ".http.",
                ".rpc.",
                ".mcp.",
                ".rule.",
                ".operation."
        ).stream().anyMatch(line::contains);
    }
}
