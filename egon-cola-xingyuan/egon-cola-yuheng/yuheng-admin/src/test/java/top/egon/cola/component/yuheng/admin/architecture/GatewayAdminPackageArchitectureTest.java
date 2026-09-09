package top.egon.cola.component.yuheng.admin.architecture;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.yuheng.contract.reporting.GatewayDefinitionSourceTypeEnum;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the Gateway Admin source boundary: domain types are top-level
 * declarations and the former interface/infrastructure package tree is gone.
 */
class GatewayAdminPackageArchitectureTest {

    private static final Pattern NESTED_TYPE_DECLARATION = Pattern.compile(
            "^\\s{1,}(?:(?:public|protected|private|static|final|abstract|"
                    + "sealed|non-sealed)\\s+)*(?:record|class|enum|interface)"
                    + "\\s+[A-Za-z_][A-Za-z0-9_]*"
    );

    private static final List<String> REMOVED_PACKAGE_SEGMENTS = List.of(
            "/interfaces/",
            "/infrastructure/",
            "/security/",
            "/domain/package-info.java",
            "/application/catalog/",
            "/application/credential/",
            "/application/observability/",
            "/application/projection/",
            "/application/release/",
            "/application/reporting/",
            "/application/routing/",
            "/application/scope/",
            "/mcp/application/",
            "/mcp/artifact/",
            "/mcp/interfaces/",
            "/mcp/persistence/"
    );

    @Test
    void productionDomainTypesAreTopLevelAndPackagesAreDomainFirst()
            throws IOException {
        Path sourceRoot = Path.of("src/main/java");
        List<String> nestedDeclarations = new ArrayList<>();
        List<String> removedPackages = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> inspect(path, nestedDeclarations,
                            removedPackages));
        }
        assertTrue(
                nestedDeclarations.isEmpty(),
                "Nested business type declarations found: "
                        + nestedDeclarations
        );
        assertTrue(
                removedPackages.isEmpty(),
                "Former Gateway Admin packages remain: " + removedPackages
        );
    }

    @Test
    void openApiReadBoundaryDoesNotDependOnSyncOrProviderAdapters()
            throws IOException {
        List<String> sources = productionSources();
        String queryService = sources.stream()
                .filter(source -> source.contains(
                        "class GatewayOpenApiQueryService"))
                .findFirst()
                .orElseThrow();
        String controller = sources.stream()
                .filter(source -> source.contains(
                        "class GatewayOpenApiController"))
                .findFirst()
                .orElseThrow();

        assertTrue(queryService.contains("@Transactional(readOnly = true)"));
        assertTrue(queryService.contains("GatewayOpenApiSnapshotRepository"));
        assertTrue(!queryService.contains("GatewayOpenApiSyncService"));
        assertTrue(!queryService.contains("GatewayProviderOpenApiClient"));
        assertTrue(!queryService.contains("DdcManagementClient"));
        assertTrue(!queryService.contains("upsertDiscovered"));
        assertTrue(!queryService.contains("markFailure"));
        assertTrue(controller.contains("CAP_gateway:read"));
        assertTrue(!controller.contains("JdbcTemplate"));
        assertTrue(!controller.contains("GatewayProviderOpenApiClient"));
    }

    @Test
    void productionSourceVocabularyAndTimeBoundaryAreExplicit()
            throws IOException {
        assertTrue(Arrays.stream(GatewayDefinitionSourceTypeEnum.values())
                .map(Enum::name)
                .toList()
                .equals(List.of("MANUAL", "RPC_DESCRIPTOR", "OPENAPI31")));
        List<String> sources = productionSources();
        assertTrue(sources.stream().noneMatch(source -> source.contains(
                "GatewayHttpOperationMapper")));
        assertTrue(sources.stream().noneMatch(source -> source.contains(
                "GatewayHttpSchemaCompiler")));
        assertTrue(sources.stream().noneMatch(source -> source.matches(
                "(?s).*\\bSTARTER\\b.*")));
        assertTrue(sources.stream().noneMatch(source -> source.contains(
                "java.util.Date")));
        assertTrue(sources.stream().noneMatch(source -> source.contains(
                "java.util.Calendar")));
    }

    private static List<String> productionSources() throws IOException {
        try (Stream<Path> paths = Files.walk(Path.of("src/main/java"))) {
            return paths.filter(path -> path.toString().endsWith(".java"))
                    .map(GatewayAdminPackageArchitectureTest::read)
                    .toList();
        }
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "Cannot inspect Gateway Admin source " + path,
                    failure
            );
        }
    }

    private static void inspect(
            Path path,
            List<String> nestedDeclarations,
            List<String> removedPackages) {
        String normalized = "/" + path.toString().replace('\\', '/');
        if (REMOVED_PACKAGE_SEGMENTS.stream().anyMatch(
                normalized::contains)) {
            removedPackages.add(path.toString());
        }
        try {
            List<String> lines = Files.readAllLines(path);
            boolean topLevelCarrier = normalized.contains(
                    "/openapi/domain/vo/")
                    || normalized.contains("/openapi/domain/po/");
            if (topLevelCarrier) {
                for (int index = 0; index < lines.size(); index++) {
                    if (NESTED_TYPE_DECLARATION.matcher(lines.get(index))
                            .find()) {
                        nestedDeclarations.add(
                                path + ":" + (index + 1)
                        );
                    }
                }
            }
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "Cannot inspect Gateway Admin source " + path,
                    failure
            );
        }
    }
}
