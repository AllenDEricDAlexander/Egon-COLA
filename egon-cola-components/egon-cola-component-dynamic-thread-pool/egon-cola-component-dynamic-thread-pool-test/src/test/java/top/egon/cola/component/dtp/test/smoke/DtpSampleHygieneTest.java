package top.egon.cola.component.dtp.test.smoke;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DtpSampleHygieneTest {

    private static final List<String> STALE_RESOURCE_TOKENS = List.of(
            "Home" + "Lab666",
            "password: " + "123456",
            "frame_" + "archetype",
            "Retail_" + "HikariCP",
            "com.nmys." + "view"
    );

    @Test
    void sampleResourcesDoNotContainCopiedSecretsOrStaleNames() throws IOException {
        Path moduleRoot = moduleRoot();
        Path resources = moduleRoot.resolve("src/main/resources");

        try (Stream<Path> files = Files.walk(resources)) {
            List<Path> resourceFiles = files
                    .filter(Files::isRegularFile)
                    .toList();
            for (Path resourceFile : resourceFiles) {
                String content = Files.readString(resourceFile);
                assertThat(content)
                        .as(resourceFile.toString())
                        .doesNotContain(STALE_RESOURCE_TOKENS);
            }
        }

        String devConfig = Files.readString(resources.resolve("application-dev.yml"));
        assertThat(devConfig).contains(
                "host: ${DTP_REDIS_HOST:127.0.0.1}",
                "port: ${DTP_REDIS_PORT:6379}",
                "password: ${DTP_REDIS_PASSWORD:}"
        );
    }

    @Test
    void redisTestsUseSampleApplicationPackage() throws IOException {
        Path moduleRoot = moduleRoot();
        Path testJava = moduleRoot.resolve("src/test/java");

        assertThat(testJava.resolve("ApiTest.java")).doesNotExist();
        assertThat(testJava.resolve("DynamicThreadPoolAdjustTest.java")).doesNotExist();

        assertPackagedRedisTest(testJava.resolve("top/egon/cola/component/dtp/test/ApiTest.java"));
        assertPackagedRedisTest(testJava.resolve("top/egon/cola/component/dtp/test/DynamicThreadPoolAdjustTest.java"));
    }

    private static void assertPackagedRedisTest(Path testFile) throws IOException {
        assertThat(testFile).exists();
        String content = Files.readString(testFile);
        assertThat(content).startsWith("package top.egon.cola.component.dtp.test;");
        assertThat(content).contains("@SpringBootTest(classes = Application.class)");
    }

    private static Path moduleRoot() {
        Path current = Paths.get("").toAbsolutePath();
        if (Files.exists(current.resolve("src/main/resources/application-dev.yml"))) {
            return current;
        }
        return current.resolve("egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-test");
    }
}
