package top.egon.cola.component.gateway.test.process;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record GatewayProcessSpec(
        String name,
        String mainClass,
        List<String> arguments,
        Map<String, String> environment,
        Duration startupTimeout
) {

    private static final List<String> SENSITIVE_MARKERS = List.of(
            "password",
            "secret",
            "master-key",
            "access-key",
            "token"
    );

    public GatewayProcessSpec {
        name = required(name, "name");
        mainClass = required(mainClass, "mainClass");
        arguments = List.copyOf(arguments);
        environment = Map.copyOf(environment);
        startupTimeout = Objects.requireNonNull(
                startupTimeout,
                "startupTimeout"
        );
        if (startupTimeout.isZero() || startupTimeout.isNegative()) {
            throw new IllegalArgumentException(
                    "startupTimeout must be positive"
            );
        }
    }

    public static Builder builder(String name, String mainClass) {
        return new Builder(name, mainClass);
    }

    public List<String> redactedArguments() {
        return arguments.stream()
                .map(GatewayProcessSpec::redact)
                .toList();
    }

    public Map<String, String> redactedEnvironment() {
        Map<String, String> redacted = new LinkedHashMap<>();
        environment.forEach((key, value) -> redacted.put(
                key,
                sensitive(key) ? "******" : value
        ));
        return Map.copyOf(redacted);
    }

    private static String redact(String argument) {
        int equals = argument.indexOf('=');
        if (equals < 0) {
            return argument;
        }
        String key = argument.substring(0, equals);
        return sensitive(key)
                ? key + "=******"
                : argument;
    }

    private static boolean sensitive(String key) {
        String normalized = key.toLowerCase(java.util.Locale.ROOT);
        return SENSITIVE_MARKERS.stream().anyMatch(normalized::contains);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    public static final class Builder {

        private final String name;

        private final String mainClass;

        private final List<String> arguments = new ArrayList<>();

        private final Map<String, String> environment =
                new LinkedHashMap<>();

        private Duration startupTimeout = Duration.ofSeconds(60);

        private Builder(String name, String mainClass) {
            this.name = name;
            this.mainClass = mainClass;
        }

        public Builder argument(String key, Object value) {
            arguments.add("--" + required(key, "argument key") + "=" + value);
            return this;
        }

        public Builder rawArgument(String argument) {
            arguments.add(required(argument, "argument"));
            return this;
        }

        public Builder environment(String key, String value) {
            environment.put(
                    required(key, "environment key"),
                    required(value, "environment value")
            );
            return this;
        }

        public Builder startupTimeout(Duration startupTimeout) {
            this.startupTimeout = startupTimeout;
            return this;
        }

        public GatewayProcessSpec build() {
            return new GatewayProcessSpec(
                    name,
                    mainClass,
                    arguments,
                    environment,
                    startupTimeout
            );
        }
    }
}
