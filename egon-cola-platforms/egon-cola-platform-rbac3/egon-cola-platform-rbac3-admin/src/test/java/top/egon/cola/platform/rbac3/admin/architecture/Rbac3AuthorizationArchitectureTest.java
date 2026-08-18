package top.egon.cola.platform.rbac3.admin.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Static conformance checks for the annotation/about/global-catalog boundary. */
class Rbac3AuthorizationArchitectureTest {

    private static final List<String> LEGACY_MANIFEST_SYMBOLS = List.of(
            "ResourceManifest", "ManifestResource", "ManifestFacade",
            "ManifestController", "Rbac3Manifest");

    @Test
    void productionAuthorizationSourcesDoNotContainManifestRuntimeSymbols() throws Exception {
        assertThat(productionSources())
                .flatMap(path -> LEGACY_MANIFEST_SYMBOLS.stream()
                        .filter(symbol -> read(path).contains(symbol))
                        .map(symbol -> path + " contains " + symbol)
                        .toList())
                .as("legacy manifest symbols")
                .isEmpty();
    }

    @Test
    void globalCatalogPosDoNotUseTenantScopedInheritance() throws Exception {
        for (String name : List.of("ApplicationPO.java", "PermissionPO.java",
                "ResourcePO.java", "FieldDefinitionPO.java")) {
            Path source = adminSourceRoot().resolve("iam").resolve("application")
                    .resolve("domain").resolve("po").resolve(name);
            if (!Files.exists(source)) {
                source = findByName(name);
            }
            assertThat(read(source))
                    .as("global catalog PO %s", name)
                    .contains("extends GlobalAuditedPO")
                    .doesNotContain("@Column(name = \"tenant_id\")")
                    .doesNotContain("private Long tenantId");
        }
    }

    @Test
    void userControllerUsesCurrentAccessorInsteadOfExplicitPrincipalParameter() throws Exception {
        Path userController = adminSourceRoot().resolve("iam/user/controller/UserController.java");
        assertThat(read(userController))
                .doesNotContain("@AuthenticationPrincipal")
                .contains("CurrentRbac3Principal.requireCurrent()");
    }

    @Test
    void dataScopeHasNoQueryRewriterOrAnnotation() throws Exception {
        assertThat(productionSources())
                .flatMap(path -> Stream.of("@DataScope", "QueryRewriter")
                        .filter(symbol -> read(path).contains(symbol))
                        .map(symbol -> path + " contains " + symbol)
                        .toList())
                .isEmpty();
    }

    private List<Path> productionSources() throws Exception {
        try (Stream<Path> files = Files.walk(adminSourceRoot())) {
            return files.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }

    private Path adminSourceRoot() {
        return Path.of(System.getProperty("basedir"), "src/main/java",
                "top/egon/cola/platform/rbac3/admin");
    }

    private Path findByName(String name) throws Exception {
        try (Stream<Path> files = Files.walk(adminSourceRoot())) {
            return files.filter(path -> path.getFileName().toString().equals(name))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("missing source: " + name));
        }
    }

    private String read(Path source) {
        try {
            return Files.readString(source);
        } catch (Exception error) {
            throw new IllegalStateException("cannot read " + source, error);
        }
    }
}
