package top.egon.cola.component.rpc.ddc;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DdcRpcModuleBoundaryTest {

    private static final List<String> PACKAGES = List.of(
            "",
            "contract",
            "contract/proto/v1",
            "client",
            "client/config",
            "client/registry",
            "client/management",
            "mapping",
            "registry",
            "security",
            "configdata",
            "autoconfigure"
    );

    @Test
    void declaresEveryPlannedPackageBoundary() {
        Path javaRoot = Path.of(
                "src/main/java/top/egon/cola/component/rpc/ddc"
        );

        assertThat(PACKAGES)
                .allSatisfy(packagePath -> assertThat(javaRoot
                        .resolve(packagePath)
                        .resolve("package-info.java"))
                        .exists());
    }

    @Test
    void dependsOnRpcAndDdcStartersButNotAdminOrGateway()
            throws IOException {
        String pom = Files.readString(Path.of("pom.xml"));

        assertThat(pom)
                .contains("egon-cola-component-rpc-starter")
                .contains("egon-cola-platform-dynamic-config-center-starter")
                .doesNotContain("dynamic-config-center-admin")
                .doesNotContain("platform-gateway");

        Path rpcStarterPom = Path.of("../egon-cola-component-rpc-starter/pom.xml");
        assertThat(Files.readString(rpcStarterPom))
                .doesNotContain("dynamic-config-center")
                .doesNotContain("rpc-ddc-adapter");
    }

    @Test
    void productionSourcesDoNotImportAdminOrGateway() throws IOException {
        Path sourceRoot = Path.of("src/main/java");
        try (var sources = Files.walk(sourceRoot)) {
            assertThat(sources
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(this::read)
                    .toList())
                    .allSatisfy(source -> assertThat(source)
                            .doesNotContain("component.ddc.admin")
                            .doesNotContain("platform.gateway"));
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
