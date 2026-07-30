package top.egon.cola.component.gateway.test.deployment;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayTestApplicationPackagingTest {

    private static final Path TEST_MODULES = Path.of(
            "egon-cola-platforms",
            "egon-cola-platform-gateway",
            "egon-cola-platform-gateway-test"
    );

    @Test
    void executableTestApplicationsPreserveTheirThinMainArtifacts()
            throws IOException {
        for (String module : List.of(
                "egon-cola-platform-gateway-test-http-provider",
                "egon-cola-platform-gateway-test-webflux-http-provider",
                "egon-cola-platform-gateway-test-rpc-provider",
                "egon-cola-platform-gateway-test-rpc-consumer")) {
            assertThat(Files.readString(projectFile(
                    TEST_MODULES.resolve(module).resolve("pom.xml")
            ))).as(module).contains("<classifier>exec</classifier>");
        }
    }

    private static Path projectFile(Path projectPath) {
        Path current = Path.of(System.getProperty("user.dir"))
                .toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(projectPath);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException(
                "cannot locate project file " + projectPath
        );
    }
}
