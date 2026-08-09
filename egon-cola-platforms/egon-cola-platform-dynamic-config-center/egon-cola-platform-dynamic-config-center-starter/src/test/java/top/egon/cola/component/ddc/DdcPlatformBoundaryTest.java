package top.egon.cola.component.ddc;

import org.junit.jupiter.api.Test;
import org.springframework.lang.Nullable;
import top.egon.cola.component.ddc.api.client.DdcConfigClient;
import top.egon.cola.component.ddc.api.client.DdcManagementClient;
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
                    "client",
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
                .isEqualTo("top.egon.cola.component.ddc.api.client");
        assertThat(DdcInstanceIdentity.class.getPackageName())
                .isEqualTo("top.egon.cola.component.ddc.model.instance");
        assertThat(DdcLeaseSession.class.getPackageName())
                .isEqualTo("top.egon.cola.component.ddc.model.lease");
        assertThat(DdcServiceKey.class.getPackageName())
                .isEqualTo("top.egon.cola.component.ddc.model.registry");
        assertThat(DdcChecksum.class.getPackageName())
                .isEqualTo("top.egon.cola.component.ddc.format");
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
        assertMissing("top.egon.cola.component.ddc.configdata.DdcConfigDataFetcher");
        assertMissing("top.egon.cola.component.ddc.configdata.DdcConfigDataLoader");
        assertMissing("top.egon.cola.component.ddc.configdata.DdcConfigDataLocationResolver");
        assertMissing("top.egon.cola.component.ddc.configdata.DdcConfigDataResource");
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
