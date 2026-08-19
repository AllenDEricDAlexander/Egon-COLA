package top.egon.cola.component.gateway.mcp;

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

class McpCorePackageBoundaryTest {

    private static final Path SOURCE_ROOT = Path.of(
            "src/main/java/top/egon/cola/component/gateway/mcp"
    );

    @Test
    void rootContainsOnlyPackageInfoAfterMigration() throws IOException {
        assertTrue(Files.isDirectory(SOURCE_ROOT));
        try (Stream<Path> files = Files.list(SOURCE_ROOT)) {
            List<String> directJavaFiles = files
                    .filter(path -> path.getFileName().toString().endsWith(
                            ".java"
                    ))
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();

            assertEquals(List.of("package-info.java"), directJavaFiles);
        }
    }

    @Test
    void capabilityPackagesExposeRoleDirectories() throws IOException {
        Set<String> required = Set.of(
                "common/protocol",
                "common/transport",
                "common/security",
                "common/telemetry",
                "app/domain",
                "app/service",
                "completion/service",
                "prompt/domain",
                "prompt/service",
                "remote/domain",
                "remote/service",
                "resource/domain",
                "resource/service",
                "resource/adapter",
                "rule/domain",
                "rule/service",
                "server/domain",
                "server/service",
                "subscription/service",
                "task/domain",
                "task/service",
                "tool/service"
        );
        for (String relativePath : required) {
            assertTrue(
                    Files.isDirectory(SOURCE_ROOT.resolve(relativePath)),
                    () -> "missing MCP target package " + relativePath
            );
        }

        for (String oldPackage : List.of(
                "app",
                "completion",
                "prompt",
                "remote",
                "resource",
                "rule",
                "server",
                "subscription",
                "task",
                "tool"
        )) {
            try (Stream<Path> files = Files.list(
                    SOURCE_ROOT.resolve(oldPackage)
            )) {
                List<String> directJavaFiles = files
                        .filter(path -> path.getFileName().toString()
                                .endsWith(".java"))
                        .map(path -> path.getFileName().toString())
                        .sorted()
                        .toList();
                assertEquals(
                        List.of(),
                        directJavaFiles,
                        () -> "legacy MCP runtime files remain in "
                                + oldPackage
                );
            }
        }

        for (String oldPackage : List.of(
                "protocol",
                "security",
                "telemetry",
                "transport",
                "server/handler"
        )) {
            assertFalse(
                    Files.exists(SOURCE_ROOT.resolve(oldPackage)),
                    () -> "old MCP package remains " + oldPackage
            );
        }
    }

    @Test
    void commonPackagesDoNotDependOnCapabilityPackages()
            throws IOException {
        Path commonRoot = SOURCE_ROOT.resolve("common");
        assertTrue(Files.isDirectory(commonRoot));
        try (Stream<Path> files = Files.walk(commonRoot)) {
            List<String> forbiddenImports = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(this::lines)
                    .filter(this::isCapabilityImport)
                    .toList();

            assertEquals(List.of(), forbiddenImports);
        }
    }

    private Stream<String> lines(Path path) {
        try {
            return Files.readAllLines(path).stream();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private boolean isCapabilityImport(String line) {
        if (!line.startsWith(
                "import top.egon.cola.component.gateway.mcp."
        )) {
            return false;
        }
        return List.of(
                ".app.",
                ".completion.",
                ".prompt.",
                ".remote.",
                ".resource.",
                ".rule.",
                ".server.",
                ".subscription.",
                ".task.",
                ".tool."
        ).stream().anyMatch(line::contains);
    }
}
