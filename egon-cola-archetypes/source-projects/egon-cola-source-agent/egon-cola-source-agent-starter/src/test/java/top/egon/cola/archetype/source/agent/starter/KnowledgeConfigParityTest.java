package top.egon.cola.archetype.source.agent.starter;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The four configuration files declare the same keys (Spec B §15, Rule 7).
 *
 * <p>A profile file overlays the base file, so a key the profile omits keeps whatever the base file
 * said — the deployment would read a value written for another environment without anything
 * failing. The keys a profile declares are therefore the ones a reader can trust it to have decided
 * on, and this gate keeps that set the same in all four files: a new key lands in every profile in
 * the same commit, and no profile silently inherits one.
 *
 * <p>Keys are compared as full paths, not by name: what a deployment sets is `rag.retrieval.max-top-k`
 * rather than a `max-top-k` that could sit anywhere. Values are deliberately not compared — the same
 * key holding an environment variable in one profile and a literal in another is the point of having
 * profiles.
 */
class KnowledgeConfigParityTest {

    private static final Path RESOURCES = Path.of(".").toAbsolutePath().normalize().getParent()
            .resolve("egon-cola-source-agent-starter/src/main/resources");

    private static final List<String> PROFILES = List.of("dev", "test", "prod");

    /** A line that declares a key: its indentation, its name, and then whatever follows. */
    private static final Pattern KEY = Pattern.compile("^(\\s*)([A-Za-z0-9._-]+):(?:\\s.*)?$");

    @Test
    void keeps_profile_key_sets_equal() throws IOException {
        Set<String> baseline = keyPaths("application-dev.yml");
        assertTrue(baseline.size() > 40, "the knowledge configuration is smaller than the contract: " + baseline.size());
        for (String profile : PROFILES.subList(1, PROFILES.size())) {
            assertEquals(baseline, keyPaths("application-" + profile + ".yml"),
                    () -> "application-" + profile + ".yml declares a different key set");
        }
    }

    @Test
    void declares_every_profile_key_in_the_base_file() throws IOException {
        Set<String> base = keyPaths("application.yml");
        for (String profile : PROFILES) {
            Set<String> declared = keyPaths("application-" + profile + ".yml");
            Set<String> onlyInProfile = new LinkedHashSet<>(declared);
            onlyInProfile.removeAll(base);
            assertTrue(onlyInProfile.isEmpty(),
                    () -> "application-" + profile + ".yml declares keys the base file does not: " + onlyInProfile);
        }
    }

    /** The full dotted paths of the keys a configuration file declares, in file order. */
    private static Set<String> keyPaths(String file) throws IOException {
        Set<String> paths = new LinkedHashSet<>();
        Deque<String> opened = new ArrayDeque<>();
        Deque<Integer> indents = new ArrayDeque<>();
        for (String line : Files.readAllLines(RESOURCES.resolve(file))) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            Matcher key = KEY.matcher(line);
            if (!key.matches()) {
                continue;
            }
            int indent = key.group(1).length();
            while (!opened.isEmpty() && indents.peek() >= indent) {
                opened.pop();
                indents.pop();
            }
            opened.push(key.group(2));
            indents.push(indent);
            List<String> path = new ArrayList<>(opened).reversed();
            paths.add(String.join(".", path));
        }
        return paths;
    }
}
