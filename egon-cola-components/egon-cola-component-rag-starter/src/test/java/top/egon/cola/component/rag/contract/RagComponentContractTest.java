package top.egon.cola.component.rag.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Turns the component's structural promises into an executable gate.
 *
 * <p>These are the promises a reader cannot verify by reading one class: which packages exist, which
 * dependencies are absent, and which types the component must never use. They fail loudly if a later
 * change erodes them.
 */
class RagComponentContractTest {

    private static final Path MAIN_JAVA = Path.of("src/main/java/top/egon/cola/component/rag");

    private static final Path POM = Path.of("pom.xml");

    @Test
    void keeps_the_flat_component_package_structure() throws IOException {
        List<String> forbidden = List.of(".biz.", ".domain.", ".infrastructure.", ".adapter.",
                ".repository.", ".controller.", ".manage.");

        assertThat(packageNames()).isNotEmpty().noneMatch(
                name -> forbidden.stream().anyMatch(name::startsWith));
    }

    @Test
    void documents_every_main_package() throws IOException {
        Set<String> packagesWithoutDocumentation = new TreeSet<>();
        try (Stream<Path> directories = Files.walk(MAIN_JAVA)) {
            directories.filter(Files::isDirectory).forEach(directory -> {
                boolean hasJava = directory.toFile().list((dir, name) -> name.endsWith(".java")) != null
                        && Stream.of(directory.toFile().listFiles((dir, name) -> name.endsWith(".java")))
                        .findAny().isPresent();
                if (hasJava && !Files.exists(directory.resolve("package-info.java"))) {
                    packagesWithoutDocumentation.add(MAIN_JAVA.relativize(directory).toString());
                }
            });
        }

        assertThat(packagesWithoutDocumentation).isEmpty();
    }

    @Test
    void declares_no_forbidden_dependency() throws IOException {
        String pom = Files.readString(POM, StandardCharsets.UTF_8);

        assertThat(pom).doesNotContain("flyway-core", "mybatis", "spring-boot-starter-data-redis",
                "spring-boot-starter-amqp", "spring-boot-starter-jdbc", "spring-boot-starter-web",
                "spring-boot-starter-data-jpa", "mapstruct-plus");
    }

    @Test
    void reads_no_provider_configuration() throws IOException {
        String sources = mainSources();

        assertThat(sources).doesNotContain("api-key", "apiKey", "base-url", "baseUrl", "OpenAiApi");
    }

    @Test
    void uses_java_time_only() throws IOException {
        String sources = mainSources();

        assertThat(sources).doesNotContain("java.util.Date", "java.util.Calendar", "SimpleDateFormat",
                "System.currentTimeMillis");
    }

    @Test
    void propagates_qualifier_and_value_through_lombok() throws IOException {
        String lombokConfig = Files.readString(Path.of("lombok.config"), StandardCharsets.UTF_8);

        assertThat(lombokConfig)
                .contains("lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier")
                .contains("lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Value")
                .contains("config.stopBubbling = true");
    }

    private static List<String> packageNames() throws IOException {
        List<String> names = new ArrayList<>();
        try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.getFileName().toString().equals("package-info.java"))
                    .forEach(path -> {
                        Path relative = MAIN_JAVA.relativize(path);
                        names.add("." + relative.toString().replace('/', '.').replace('\\', '.'));
                    });
        }
        return names;
    }

    private static String mainSources() throws IOException {
        StringBuilder sources = new StringBuilder();
        try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
            for (Path path : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                sources.append(Files.readString(path, StandardCharsets.UTF_8));
            }
        }
        return sources.toString();
    }
}
