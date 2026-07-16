package top.egon.cola.component.bytecode.starter;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BytecodeDependencyBoundaryTest {

    @Test
    void keepsActuatorAndMicrometerOptionalAndDoesNotDeclareForbiddenIntegrations() throws Exception {
        String pom = Files.readString(Path.of("pom.xml"));

        assertTrue(optional(pom, "spring-boot-actuator"));
        assertFalse(pom.contains("spring-boot-starter-actuator"));
        assertTrue(optional(pom, "micrometer-core"));
        assertFalse(pom.contains("egon-cola-component-dynamic-thread-pool-starter"));
        assertFalse(pom.contains("redisson"));
        assertFalse(pom.contains("spring-web"));
        assertFalse(pom.contains("egon-cola-component-access-guard-starter"));
        assertTrue(optional(pom, "egon-cola-component-method-extension-starter"));
    }

    private boolean optional(String pom, String artifactId) {
        int artifact = pom.indexOf("<artifactId>" + artifactId + "</artifactId>");
        int dependencyEnd = pom.indexOf("</dependency>", artifact);
        return artifact >= 0 && dependencyEnd > artifact
                && pom.substring(artifact, dependencyEnd).contains("<optional>true</optional>");
    }
}
