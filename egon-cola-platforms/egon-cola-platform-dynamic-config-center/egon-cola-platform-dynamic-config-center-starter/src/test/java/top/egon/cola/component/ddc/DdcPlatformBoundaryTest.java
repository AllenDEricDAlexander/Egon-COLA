package top.egon.cola.component.ddc;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.api.client.DdcConfigClient;
import top.egon.cola.component.ddc.api.client.DdcManagementClient;
import top.egon.cola.component.ddc.client.config.HttpDdcConfigClient;
import top.egon.cola.component.ddc.client.http.DdcOpenApiRequestFactory;
import top.egon.cola.component.ddc.client.management.HttpDdcManagementClient;
import top.egon.cola.component.ddc.client.registry.HttpDdcServiceRegistryClient;
import top.egon.cola.component.ddc.configdata.DdcConfigDataFetcher;
import top.egon.cola.component.ddc.format.DdcChecksum;
import top.egon.cola.component.ddc.listener.config.DdcConfigChangeListener;
import top.egon.cola.component.ddc.listener.registry.DdcRegistrySubscriptionCoordinator;
import top.egon.cola.component.ddc.model.instance.DdcInstanceIdentity;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.service.binding.DdcFieldBindingService;
import top.egon.cola.component.ddc.service.lifecycle.DdcRuntimeCoordinator;
import top.egon.cola.component.ddc.redis.DdcRedisClientFactory;
import top.egon.cola.component.ddc.state.DdcLocalConfigState;

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
        assertThat(HttpDdcConfigClient.class.getPackageName())
                .isEqualTo("top.egon.cola.component.ddc.client.config");
        assertThat(HttpDdcManagementClient.class.getPackageName())
                .isEqualTo("top.egon.cola.component.ddc.client.management");
        assertThat(HttpDdcServiceRegistryClient.class.getPackageName())
                .isEqualTo("top.egon.cola.component.ddc.client.registry");
        assertThat(DdcOpenApiRequestFactory.class.getPackageName())
                .isEqualTo("top.egon.cola.component.ddc.client.http");
        assertThat(DdcConfigDataFetcher.class.getPackageName())
                .isEqualTo("top.egon.cola.component.ddc.configdata");
        assertThat(DdcFieldBindingService.class.getPackageName())
                .isEqualTo("top.egon.cola.component.ddc.service.binding");
        assertThat(DdcRuntimeCoordinator.class.getPackageName())
                .isEqualTo("top.egon.cola.component.ddc.service.lifecycle");
        assertThat(DdcRedisClientFactory.class.getPackageName())
                .isEqualTo("top.egon.cola.component.ddc.redis");
        assertThat(DdcConfigChangeListener.class.getPackageName())
                .isEqualTo("top.egon.cola.component.ddc.listener.config");
        assertThat(DdcRegistrySubscriptionCoordinator.class.getPackageName())
                .isEqualTo("top.egon.cola.component.ddc.listener.registry");
        assertThat(DdcLocalConfigState.class.getPackageName())
                .isEqualTo("top.egon.cola.component.ddc.state");
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
