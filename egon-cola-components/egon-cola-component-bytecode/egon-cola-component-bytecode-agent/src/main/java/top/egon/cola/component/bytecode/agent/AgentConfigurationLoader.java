package top.egon.cola.component.bytecode.agent;

import top.egon.cola.component.bytecode.bridge.BridgeCapability;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

public final class AgentConfigurationLoader {

    private static final String SYSTEM_PREFIX = "egon.cola.bytecode.";
    private static final String ENVIRONMENT_PREFIX = "EGON_COLA_BYTECODE_";

    private final Properties systemProperties;
    private final Map<String, String> environment;
    private final AgentYamlParser yamlParser;

    public AgentConfigurationLoader() {
        this(System.getProperties(), System.getenv());
    }

    AgentConfigurationLoader(Properties systemProperties, Map<String, String> environment) {
        this.systemProperties = new Properties();
        this.systemProperties.putAll(systemProperties);
        this.environment = Map.copyOf(environment);
        this.yamlParser = new AgentYamlParser();
    }

    public AgentConfiguration load(String arguments) {
        Map<String, String> argumentValues = AgentArguments.parse(arguments);
        Map<String, String> systemValues = readValues(
                key -> systemProperties.getProperty(SYSTEM_PREFIX + key));
        Map<String, String> environmentValues = readValues(
                key -> environment.get(ENVIRONMENT_PREFIX
                        + key.replace('-', '_').toUpperCase(Locale.ROOT)));
        AgentFailurePolicy malformedYamlPolicy = AgentFailurePolicy.parse(firstNonBlank(
                argumentValues.get("failure-policy"),
                systemValues.get("failure-policy"),
                environmentValues.get("failure-policy"),
                AgentFailurePolicy.SKIP_CLASS.name()));

        Map<String, String> values = defaults();
        values.putAll(environmentValues);
        values.putAll(systemValues);
        values.putAll(loadYaml(argumentValues, systemValues, environmentValues, malformedYamlPolicy));
        values.putAll(argumentValues);

        boolean enabled = Boolean.parseBoolean(values.get("enabled"));
        Set<BridgeCapability> features = parseFeatures(values.get("features"));
        List<String> includes = parseList(values.get("include"));
        List<String> excludes = parseList(values.get("exclude"));
        AgentFailurePolicy failurePolicy = AgentFailurePolicy.parse(values.get("failure-policy"));
        int failureCapacity = parseCapacity(values.get("failure-capacity"));
        return new AgentConfiguration(
                enabled,
                features,
                includes,
                excludes,
                failurePolicy,
                failureCapacity,
                digest(includes),
                digest(excludes)
        );
    }

    private Map<String, String> loadYaml(
            Map<String, String> arguments,
            Map<String, String> system,
            Map<String, String> environment,
            AgentFailurePolicy policy
    ) {
        String configPath = firstNonBlank(
                arguments.get("config"), system.get("config"), environment.get("config"));
        if (configPath == null) {
            return Map.of();
        }
        try {
            return yamlParser.parse(Path.of(configPath));
        } catch (Exception exception) {
            if (policy == AgentFailurePolicy.MARK_FATAL) {
                throw new IllegalArgumentException("Cannot load Agent YAML configuration", exception);
            }
            return Map.of();
        }
    }

    private Map<String, String> readValues(Function<String, String> reader) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String key : List.of(
                "enabled", "features", "include", "exclude", "failure-policy",
                "failure-capacity", "config")) {
            String value = reader.apply(key);
            if (value != null && !value.isBlank()) {
                values.put(key, value.trim());
            }
        }
        return values;
    }

    private Map<String, String> defaults() {
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("enabled", "false");
        defaults.put("features", "executor");
        defaults.put("include", "");
        defaults.put("exclude", "");
        defaults.put("failure-policy", "skip-class");
        defaults.put("failure-capacity", "32");
        return defaults;
    }

    private Set<BridgeCapability> parseFeatures(String value) {
        EnumSet<BridgeCapability> features = EnumSet.noneOf(BridgeCapability.class);
        for (String feature : parseList(value)) {
            features.add(BridgeCapability.valueOf(
                    feature.replace('-', '_').toUpperCase(Locale.ROOT)));
        }
        return features.isEmpty() ? Set.of() : Set.copyOf(features);
    }

    private List<String> parseList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String item : value.split("[,;]")) {
            if (!item.isBlank()) {
                values.add(item.trim());
            }
        }
        return List.copyOf(values);
    }

    private int parseCapacity(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("failure-capacity must be an integer", exception);
        }
    }

    private String digest(List<String> patterns) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(String.join("\n", patterns).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(bytes, 0, 8);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
