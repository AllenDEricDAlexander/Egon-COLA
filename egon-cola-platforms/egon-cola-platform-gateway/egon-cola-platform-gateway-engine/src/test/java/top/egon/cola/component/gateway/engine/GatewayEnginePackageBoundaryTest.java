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
                "http/domain",
                "http/service",
                "http/security",
                "http/cors",
                "http/proxy/domain",
                "http/proxy/service",
                "http/common/logging",
                "http/websocket/service",
                "http/websocket/adapter",
                "rpc/domain",
                "rpc/service",
                "rpc/security",
                "rule/domain",
                "rule/service"
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
                "rule"
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
            Path sharedRoot = SOURCE_ROOT.resolve("common/" + commonRoot);
            if (Files.isDirectory(sharedRoot)) {
                try (Stream<Path> files = Files.walk(sharedRoot)) {
                    assertTrue(files.noneMatch(path -> path.toString().endsWith(".java")),
                            () -> "shared runtime source remains in " + sharedRoot);
                }
            }
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
    void containsNoMcpRuntimeOrStoreDependencies() throws IOException {
        try (Stream<Path> files = Files.walk(SOURCE_ROOT)) {
            List<String> forbidden = files.filter(path -> path.toString().endsWith(".java"))
                    .flatMap(this::lines)
                    .filter(line -> line.startsWith("import ") && (line.contains(".mcp.")
                            || line.contains("javax.sql.") || line.contains("java.sql.")))
                    .toList();
            assertEquals(List.of(), forbidden);
        }
        String pom = Files.readString(Path.of("pom.xml"));
        for (String artifact : List.of("gateway-mcp-core", "gateway-mcp-engine",
                "spring-boot-starter-jdbc", "postgresql")) {
            assertFalse(pom.contains(artifact), artifact);
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
