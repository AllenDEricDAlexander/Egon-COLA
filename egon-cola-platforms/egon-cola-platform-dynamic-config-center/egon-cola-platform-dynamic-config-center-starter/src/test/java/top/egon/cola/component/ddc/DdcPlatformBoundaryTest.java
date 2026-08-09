package top.egon.cola.component.ddc;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.api.client.DdcConfigClient;
import top.egon.cola.component.ddc.api.client.DdcManagementClient;
import top.egon.cola.component.ddc.configuration.bootstrap.DdcConfigDataFetcher;
import top.egon.cola.component.ddc.configuration.runtime.DdcLocalConfigState;
import top.egon.cola.component.ddc.format.DdcChecksum;
import top.egon.cola.component.ddc.model.instance.DdcInstanceIdentity;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcPlatformBoundaryTest {

    @Test
    void starterContainsApprovedRolePackagesDuringMigration() throws Exception {
        Path packageRoot = Path.of(
                "src/main/java/top/egon/cola/component/ddc"
        );
        try (var paths = Files.list(packageRoot)) {
            List<String> topLevelPackages = paths
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();

            assertThat(topLevelPackages).contains(
                    "annotation",
                    "api",
                    "autoconfigure",
                    "client",
                    "configdata",
                    "environment",
                    "error",
                    "format",
                    "listener",
                    "model",
                    "observability",
                    "redis",
                    "service",
                    "state"
            );
        }
    }

    @Test
    void configurationContractsResolveFromDomainPackages() {
        assertThat(DdcConfigClient.class.getPackageName())
                .isEqualTo("top.egon.cola.component.ddc.api.client");
        assertThat(DdcInstanceIdentity.class.getPackageName())
                .isEqualTo("top.egon.cola.component.ddc.model.instance");
        assertThat(DdcLeaseSession.class.getPackageName())
                .isEqualTo("top.egon.cola.component.ddc.model.lease");
        assertThat(DdcServiceKey.class.getPackageName())
                .isEqualTo("top.egon.cola.component.ddc.model.registry");
        assertThat(DdcChecksum.class.getPackageName())
                .isEqualTo("top.egon.cola.component.ddc.format");
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
        String location = DdcManagementClient.class
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
