package top.egon.cola.component.dtp.admin;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AdminResourceConfigurationTest {

    @Test
    void logbackUsesActualTraceMdcKeys() throws IOException {
        String logback = classpathText("logback-spring.xml");

        assertThat(logback).contains("traceId=%X{traceId}");
        assertThat(logback).contains("requestId=%X{requestId}");
        assertThat(logback).doesNotContain("%X{trace-id}");
        assertThat(logback).doesNotContain("com.nmys" + ".view");
    }

    @Test
    void testProfileDoesNotContainCopiedDatabaseBlock() throws IOException {
        String applicationTest = classpathText("application-test.yml");

        assertThat(applicationTest).doesNotContain("password: " + "123456");
        assertThat(applicationTest).doesNotContain("frame_" + "archetype");
        assertThat(applicationTest).doesNotContain("Retail_" + "HikariCP");
    }

    @Test
    void dockerEntrypointExecsJavaProcess() throws IOException {
        String dockerfile = sourceFileText("Dockerfile");

        assertThat(dockerfile).contains("exec java ${JAVA_OPTS}");
    }

    private String classpathText(String resourceName) throws IOException {
        return new ClassPathResource(resourceName).getContentAsString(StandardCharsets.UTF_8);
    }

    private String sourceFileText(String relativePath) throws IOException {
        Path modulePath = Path.of(relativePath);
        if (Files.exists(modulePath)) {
            return Files.readString(modulePath);
        }
        return Files.readString(Path.of("egon-cola-components/egon-cola-component-dynamic-thread-pool/egon-cola-component-dynamic-thread-pool-admin", relativePath));
    }
}
