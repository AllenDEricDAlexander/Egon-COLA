package top.egon.cola.component.bytecode.agent;

import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class AgentYamlParser {

    Map<String, String> parse(Path path) throws IOException {
        LoadSettings settings = LoadSettings.builder()
                .setLabel("Egon COLA bytecode Agent configuration")
                .build();
        Object document = new Load(settings).loadFromString(Files.readString(path));
        if (document == null) {
            return Map.of();
        }
        if (!(document instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Agent YAML root must be a mapping");
        }
        Map<String, String> flattened = new LinkedHashMap<>();
        flatten(map, flattened);
        return Map.copyOf(flattened);
    }

    private void flatten(Map<?, ?> values, Map<String, String> target) {
        values.forEach((rawKey, rawValue) -> {
            String key = AgentArguments.normalizeKey(String.valueOf(rawKey));
            if (rawValue instanceof Map<?, ?> nested) {
                flatten(nested, target);
            } else if (rawValue instanceof List<?> list) {
                target.put(key, list.stream().map(String::valueOf)
                        .collect(Collectors.joining(",")));
            } else if (rawValue != null) {
                target.put(key, String.valueOf(rawValue));
            }
        });
    }
}
