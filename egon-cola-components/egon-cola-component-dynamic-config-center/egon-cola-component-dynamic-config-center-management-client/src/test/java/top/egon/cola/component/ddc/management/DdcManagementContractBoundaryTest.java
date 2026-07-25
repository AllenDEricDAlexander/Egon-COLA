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
        Path sourceRoot = Path.of("src/main/java");
        String sources;
        try (var paths = Files.walk(sourceRoot)) {
            sources = paths.filter(path -> path.toString().endsWith(".java"))
                    .map(this::read)
                    .reduce("", (left, right) -> left + "\n" + right);
        }

        assertThat(sources)
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

        try (var paths = Files.walk(Path.of("target/classes"))) {
            for (Path path : paths.filter(candidate ->
                    candidate.toString().endsWith(".class")).toList()) {
                String constantPool = new String(Files.readAllBytes(path));
                assertThat(constantPool)
                        .as("compiled contract %s", path)
                        .doesNotContain(forbidden);
            }
        }
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
