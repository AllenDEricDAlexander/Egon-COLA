package top.egon.cola.component.ddc.management;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.management.model.DdcManagementConfig;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishTask;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DdcManagementContractBoundaryTest {

    @Test
    void publicContractsDoNotExposeAdminPersistenceOrRuntimeInfrastructure() throws IOException {
        StringBuilder sources = new StringBuilder();
        for (Path sourceRoot : contractRoots("src/main/java")) {
            try (var paths = Files.walk(sourceRoot)) {
                paths.filter(path -> path.toString().endsWith(".java"))
                        .map(this::read)
                        .forEach(source -> sources.append('\n').append(source));
            }
        }

        assertThat(sources.toString())
                .doesNotContain("component.ddc.admin")
                .doesNotContain("jakarta.persistence")
                .doesNotContain("org.redisson")
                .doesNotContain("DdcConfigItemEntity")
                .doesNotContain("DdcPublishTaskEntity")
                .doesNotContain("DdcPublishAckEntity");
        assertThat(DdcManagementClient.class).isInterface();
        assertThat(DdcManagementConfig.class.isRecord()).isTrue();
        assertThat(DdcManagementPublishTask.class.isRecord()).isTrue();
    }

    @Test
    void compiledContractsDoNotReferenceAdminPersistenceOrRuntimeInfrastructure()
            throws IOException {
        List<String> forbidden = List.of(
                "component/ddc/admin",
                "jakarta/persistence",
                "org/redisson",
                "DdcConfigItemEntity",
                "DdcPublishTaskEntity",
                "DdcPublishAckEntity"
        );

        for (Path classesRoot : contractRoots("target/classes")) {
            try (var paths = Files.walk(classesRoot)) {
                for (Path path : paths.filter(candidate ->
                        candidate.toString().endsWith(".class")).toList()) {
                    String constantPool = new String(Files.readAllBytes(path));
                    assertThat(constantPool)
                            .as("compiled contract %s", path)
                            .doesNotContain(forbidden);
                }
            }
        }
    }

    private List<Path> contractRoots(String root) {
        Path packageRoot = Path.of(root, "top/egon/cola/component/ddc");
        return List.of(
                packageRoot.resolve("management"),
                packageRoot.resolve("security")
        );
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
