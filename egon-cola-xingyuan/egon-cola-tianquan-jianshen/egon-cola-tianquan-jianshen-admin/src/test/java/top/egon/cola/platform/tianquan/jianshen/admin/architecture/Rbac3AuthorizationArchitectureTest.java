package top.egon.cola.platform.tianquan.jianshen.admin.architecture;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.decision.controller.Rbac3AboutController;

import java.lang.reflect.Modifier;
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
                .contains("new CurrentRbac3User().require()");
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

    @Test
    void authorizationAndRegistrationUseTheApprovedLayerRoots() throws Exception {
        assertThat(productionSources())
                .filteredOn(path -> path.toString().endsWith(".java"))
                .allSatisfy(path -> {
                    String relative = adminSourceRoot().relativize(path).toString();
                    assertThat(relative)
                            .as("approved package root for %s", path)
                            .doesNotStartWith("iam/resource/report/")
                            .doesNotStartWith("iam/permission/")
                            .doesNotStartWith("iam/policy/")
                            .doesNotStartWith("iam/authorizationstate/");
                });
        assertThat(productionSources())
                .flatMap(path -> Stream.of(".admin.management.", ".admin.participation.",
                                ".admin.simulation.", ".admin.runtime.")
                        .filter(read(path)::contains)
                        .map(symbol -> path + " contains " + symbol)
                        .toList())
                .as("legacy authorization references")
                .isEmpty();
    }

    @Test
    void permissionAnnotatedControllersRemainProxyable() throws Exception {
        assertThat(Modifier.isFinal(Rbac3AboutController.class.getModifiers()))
                .isFalse();
        for (Path source : productionSources()) {
            String content = read(source);
            if (source.getFileName().toString().endsWith("Controller.java")
                    && (content.contains("@RequiresPermission")
                    || content.contains("@RequiresServiceScope"))) {
                assertThat(content)
                        .as("permission-annotated controller %s", source)
                        .doesNotContain("public final class");
            }
        }
    }

    @Test
    void transactionalBeansRemainProxyable() throws Exception {
        for (Path source : productionSources()) {
            String content = read(source);
            if (content.contains("@Transactional")) {
                assertThat(content)
                        .as("transactional bean %s", source)
                        .doesNotContain("public final class");
            }
        }
    }

    private List<Path> productionSources() throws Exception {
        try (Stream<Path> files = Files.walk(adminSourceRoot())) {
            return files.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }

    private Path adminSourceRoot() {
        return Path.of(System.getProperty("basedir"), "src/main/java",
                "top/egon/cola/platform/tianquan/jianshen/admin");
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
