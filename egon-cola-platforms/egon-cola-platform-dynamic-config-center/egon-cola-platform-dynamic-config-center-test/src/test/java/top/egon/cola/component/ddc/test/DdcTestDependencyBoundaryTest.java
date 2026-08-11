package top.egon.cola.component.ddc.test;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DdcTestDependencyBoundaryTest {

    @Test
    void adminImplementationIsLimitedToAcceptanceTestScope() throws Exception {
        Path basedir = Path.of(System.getProperty("basedir"));
        String pom = Files.readString(basedir.resolve("pom.xml"));

        assertThat(pom).contains(
                "<artifactId>egon-cola-platform-dynamic-config-center-admin</artifactId>\n"
                        + "            <scope>test</scope>"
        );
        try (var files = Files.walk(basedir.resolve("target/classes"))) {
            assertThat(files
                    .filter(path -> path.toString().endsWith(".class"))
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.contains("DdcConfigService"))
                    .toList()).isEmpty();
        }
    }
}
