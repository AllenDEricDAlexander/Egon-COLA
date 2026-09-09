package top.egon.cola.component.tianshu;

import org.junit.jupiter.api.Test;
import org.springframework.lang.Nullable;
import top.egon.cola.component.tianshu.api.client.DdcConfigClient;
import top.egon.cola.component.tianshu.api.client.DdcManagementClient;
import top.egon.cola.component.tianshu.format.DdcChecksum;
import top.egon.cola.component.tianshu.listener.config.DdcConfigChangeListener;
import top.egon.cola.component.tianshu.listener.registry.DdcRegistrySubscriptionCoordinator;
import top.egon.cola.component.tianshu.model.instance.DdcInstanceIdentity;
import top.egon.cola.component.tianshu.model.lease.DdcLeaseSession;
import top.egon.cola.component.tianshu.model.registry.DdcServiceKey;
import top.egon.cola.component.tianshu.service.binding.DdcFieldBindingService;
import top.egon.cola.component.tianshu.service.lifecycle.DdcRuntimeCoordinator;
import top.egon.cola.component.tianshu.redis.DdcRedisClientFactory;
import top.egon.cola.component.tianshu.state.DdcLocalConfigState;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcPlatformBoundaryTest {

    @Test
    void starterContainsExactlyTheApprovedTopLevelRolePackages() throws Exception {
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
                    "api",
                    "autoconfigure",
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
    void everyJavaSourceDirectoryIsAnApprovedDocumentedPackage() throws Exception {
        Path packageRoot = Path.of(
                "src/main/java/top/egon/cola/component/ddc"
        );
        List<Path> directories;
        try (var paths = Files.walk(packageRoot)) {
            directories = paths
                    .filter(Files::isDirectory)
                    .sorted()
                    .toList();
        }

        List<String> sourcePackages = new ArrayList<>();
        for (Path directory : directories) {
            try (var children = Files.list(directory)) {
                if (children.noneMatch(path -> path.getFileName().toString().endsWith(".java"))) {
                    continue;
                }
            }

            String suffix = packageRoot.equals(directory)
                    ? ""
                    : packageRoot.relativize(directory).toString().replace('/', '.');
            sourcePackages.add(suffix);
            assertThat(directory.resolve("package-info.java"))
                    .as("package documentation for %s", suffix)
                    .exists();
        }

        assertThat(sourcePackages)
                .containsExactlyElementsOf(DdcPackageDocumentationTest.TARGET_PACKAGES.stream()
                        .sorted()
                        .toList());
    }

    @Test
    void localConfigurationStateDeclaresItsNullableMetadataContract() throws Exception {
        assertThat(DdcLocalConfigState.class.getMethod("version", String.class)
                .getAnnotation(Nullable.class)).isNotNull();
        assertThat(DdcLocalConfigState.class.getMethod("checksum", String.class)
                .getAnnotation(Nullable.class)).isNotNull();
        assertThat(DdcLocalConfigState.class
                .getMethod("updateChecksum", String.class, String.class)
                .getParameterAnnotations()[1])
                .anyMatch(annotation -> annotation.annotationType() == Nullable.class);
        assertThat(DdcLocalConfigState.class
                .getMethod("restoreMetadata", String.class, Long.class, String.class)
                .getParameterAnnotations()[1])
                .anyMatch(annotation -> annotation.annotationType() == Nullable.class);
        assertThat(DdcLocalConfigState.class
                .getMethod("restoreMetadata", String.class, Long.class, String.class)
                .getParameterAnnotations()[2])
                .anyMatch(annotation -> annotation.annotationType() == Nullable.class);
    }

    @Test
    void configurationContractsResolveFromDomainPackages() {
        assertThat(DdcConfigClient.class.getPackageName())
                .isEqualTo("top.egon.cola.component.tianshu.api.client");
        assertThat(DdcInstanceIdentity.class.getPackageName())
                .isEqualTo("top.egon.cola.component.tianshu.model.instance");
        assertThat(DdcLeaseSession.class.getPackageName())
                .isEqualTo("top.egon.cola.component.tianshu.model.lease");
        assertThat(DdcServiceKey.class.getPackageName())
                .isEqualTo("top.egon.cola.component.tianshu.model.registry");
        assertThat(DdcChecksum.class.getPackageName())
                .isEqualTo("top.egon.cola.component.tianshu.format");
        assertThat(DdcFieldBindingService.class.getPackageName())
                .isEqualTo("top.egon.cola.component.tianshu.service.binding");
        assertThat(DdcRuntimeCoordinator.class.getPackageName())
                .isEqualTo("top.egon.cola.component.tianshu.service.lifecycle");
        assertThat(DdcRedisClientFactory.class.getPackageName())
                .isEqualTo("top.egon.cola.component.tianshu.redis");
        assertThat(DdcConfigChangeListener.class.getPackageName())
                .isEqualTo("top.egon.cola.component.tianshu.listener.config");
        assertThat(DdcRegistrySubscriptionCoordinator.class.getPackageName())
                .isEqualTo("top.egon.cola.component.tianshu.listener.registry");
        assertThat(DdcLocalConfigState.class.getPackageName())
                .isEqualTo("top.egon.cola.component.tianshu.state");
    }

    @Test
    void removedTypesAreNotPackagedAsCompatibilityShells() {
        assertMissing("top.egon.cola.component.tianshu.client.DdcAdminClient");
        assertMissing("top.egon.cola.component.tianshu.bootstrap.DdcBootstrapClient");
        assertMissing("top.egon.cola.component.tianshu.common.DdcKeys");
        assertMissing("top.egon.cola.component.tianshu.configdata.DdcConfigDataFetcher");
        assertMissing("top.egon.cola.component.tianshu.configdata.DdcConfigDataLoader");
        assertMissing("top.egon.cola.component.tianshu.configdata.DdcConfigDataLocationResolver");
        assertMissing("top.egon.cola.component.tianshu.configdata.DdcConfigDataResource");
        assertMissing("top.egon.cola.component.tianshu.client.config.Http"
                + "DdcConfigClient");
        assertMissing("top.egon.cola.component.tianshu.client.registry.Http"
                + "DdcServiceRegistryClient");
        assertMissing("top.egon.cola.component.tianshu.client.management.Http"
                + "DdcManagementClient");
        assertMissing("top.egon.cola.component.tianshu.model.client.DdcClientTransportSecurity");
        assertMissing("top.egon.cola.component.tianshu.model.client.DdcManagementClientProperties");
        assertMissing("top.egon.cola.component.tianshu.error.http.DdcOpenApiRequestException");
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
                .contains("egon-cola-tianshu-starter")
                .doesNotContain("management-client");
    }

    private void assertMissing(String className) {
        assertThatThrownBy(() -> Class.forName(className))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
