package top.egon.cola.component.bytecode.agent;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

final class AgentArguments {

    private AgentArguments() {
    }

    static Map<String, String> parse(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return Map.of();
        }
        Map<String, String> values = new LinkedHashMap<>();
        String currentKey = null;
        for (String token : arguments.split(",")) {
            int separator = token.indexOf('=');
            if (separator > 0) {
                currentKey = normalizeKey(token.substring(0, separator));
                values.put(currentKey, token.substring(separator + 1).trim());
            } else if (currentKey != null && !token.isBlank()) {
                values.compute(currentKey, (key, value) -> value + "," + token.trim());
            } else if (!token.isBlank()) {
                throw new IllegalArgumentException("Invalid Agent argument: " + token.trim());
            }
        }
        return Map.copyOf(values);
    }

    static String normalizeKey(String key) {
        return key.trim().replace('_', '-').toLowerCase(Locale.ROOT);
    }
}
