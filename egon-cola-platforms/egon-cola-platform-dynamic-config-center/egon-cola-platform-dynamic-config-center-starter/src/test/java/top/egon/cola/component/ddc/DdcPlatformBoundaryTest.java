package top.egon.cola.component.ddc;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.configuration.bootstrap.DdcConfigDataFetcher;
import top.egon.cola.component.ddc.configuration.client.DdcConfigClient;
import top.egon.cola.component.ddc.configuration.runtime.DdcLocalConfigState;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcPlatformBoundaryTest {

    @Test
    void starterPackagesFollowApprovedDomainTree() throws Exception {
        Path packageRoot = Path.of(
                "src/main/java/top/egon/cola/component/ddc"
        );
        try (var paths = Files.list(packageRoot)) {
            List<String> topLevelPackages = paths
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();

            assertThat(topLevelPackages).containsExactly(
                    "annotation",
                    "autoconfigure",
                    "configuration",
                    "error",
                    "lease",
                    "management",
                    "observability",
                    "registry",
                    "transport"
            );
        }
    }

    @Test
    void configurationContractsResolveFromDomainPackages() {
        assertThat(DdcConfigClient.class.getPackageName())
                .isEqualTo("top.egon.cola.component.ddc.configuration.client");
        assertThat(DdcConfigDataFetcher.class.getPackageName())
                .isEqualTo("top.egon.cola.component.ddc.configuration.bootstrap");
        assertThat(DdcLocalConfigState.class.getPackageName())
                .isEqualTo("top.egon.cola.component.ddc.configuration.runtime");
    }

    @Test
    void removedTypesAreNotPackagedAsCompatibilityShells() {
        assertMissing("top.egon.cola.component.ddc.client.DdcAdminClient");
        assertMissing("top.egon.cola.component.ddc.bootstrap.DdcBootstrapClient");
        assertMissing("top.egon.cola.component.ddc.common.DdcKeys");
    }

    @Test
    void starterDoesNotDependOnAdminOrTestPackages() throws Exception {
        List<String> classFiles = Files.walk(Path.of("target/classes"))
                .filter(path -> path.toString().endsWith(".class"))
                .map(Path::toString)
                .toList();

        assertThat(classFiles).noneMatch(path -> path.contains("/admin/"));
        assertThat(classFiles).noneMatch(path -> path.contains("/test/"));
    }

    @Test
    void managementContractsArePackagedByStarter() {
        String location = top.egon.cola.component.ddc.management.DdcManagementClient.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toString();

        assertThat(location)
                .contains("egon-cola-platform-dynamic-config-center-starter")
                .doesNotContain("management-client");
    }

    private void assertMissing(String className) {
        assertThatThrownBy(() -> Class.forName(className))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
