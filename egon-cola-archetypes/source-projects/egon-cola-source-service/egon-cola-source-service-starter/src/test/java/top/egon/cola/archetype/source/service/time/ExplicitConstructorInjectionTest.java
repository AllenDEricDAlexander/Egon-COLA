package top.egon.cola.archetype.source.service.time;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reads source producers and verifies Lombok's compiled constructor and Qualifier propagation. */
class ExplicitConstructorInjectionTest {

    @Test
    void dependencyConstructorsExplicitlyDeclareIntentAndRetainFieldQualifiers() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        while (root != null && !Files.isRegularFile(root.resolve("README.md"))) {
            root = root.getParent();
        }
        assertTrue(root != null, "source project root");
        Path sourceProjectRoot = root;
        int checked = 0;
        try (var files = Files.walk(sourceProjectRoot)) {
            for (Path path : files.filter(file -> file.toString().endsWith(".java")
                    && file.toString().replace('\\', '/').contains("/src/main/java/")
                    && !sourceProjectRoot.relativize(file).toString().replace('\\', '/').startsWith("target/")
                    && !sourceProjectRoot.relativize(file).toString().replace('\\', '/').contains("/target/")).toList()) {
                String source = Files.readString(path);
                boolean lombokConstructor = source.contains("@RequiredArgsConstructor");
                boolean manualDependencyConstructor = Pattern.compile("@(Component|Service|Repository|RestController|Configuration)(?:\\W|$)")
                        .matcher(source).find()
                        && Pattern.compile("public\\s+" + path.getFileName().toString().replace(".java", "") + "\\s*\\(\\s*[^)]")
                        .matcher(source).find();
                if (!lombokConstructor && !manualDependencyConstructor) {
                    continue;
                }
                if (lombokConstructor) {
                    assertTrue(source.contains("@RequiredArgsConstructor(onConstructor_ = @Autowired)"), path.toString());
                }
                var declaration = Pattern.compile("(?m)^package ([\\w.]+);").matcher(source);
                assertTrue(declaration.find(), path.toString());
                String type = declaration.group(1) + "." + path.getFileName().toString().replace(".java", "");
                Class<?> bean = Class.forName(type, false, getClass().getClassLoader());
                var injected = Arrays.stream(bean.getDeclaredConstructors())
                        .filter(constructor -> constructor.isAnnotationPresent(Autowired.class)).toList();
                assertTrue(injected.size() == 1, type);
                Set<String> expected = new HashSet<>();
                for (var field : bean.getDeclaredFields()) {
                    var qualifier = field.getAnnotation(Qualifier.class);
                    if (qualifier != null) {
                        expected.add(qualifier.value());
                    }
                }
                Set<String> actual = new HashSet<>();
                for (var parameter : injected.get(0).getParameters()) {
                    var qualifier = parameter.getAnnotation(Qualifier.class);
                    if (qualifier != null) {
                        actual.add(qualifier.value());
                    }
                }
                assertTrue(actual.containsAll(expected), type + " missing constructor Qualifier");
                checked++;
            }
        }
        assertFalse(checked == 0, "must inspect real source Beans");
    }
}
