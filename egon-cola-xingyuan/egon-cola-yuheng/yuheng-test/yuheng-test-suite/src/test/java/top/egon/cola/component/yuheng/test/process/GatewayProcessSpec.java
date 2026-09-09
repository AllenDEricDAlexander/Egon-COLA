package top.egon.cola.component.yuheng.test.process;

import top.egon.cola.component.yuheng.contract.runtime.GatewayEngineRoleEnum;

import java.net.URI;
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
        Duration startupTimeout,
        GatewayEngineRoleEnum engineRole,
        String artifactId,
        URI dataPlaneBaseUri,
        URI managementBaseUri
) {

    private static final Map<GatewayEngineRoleEnum, EngineArtifactDTO> ENGINE_ARTIFACTS = Map.of(
            GatewayEngineRoleEnum.API_RPC, new EngineArtifactDTO(
                    "yuheng-biz-gateway",
                    "top.egon.cola.component.yuheng.engine.GatewayEngineApplication"),
            GatewayEngineRoleEnum.MCP, new EngineArtifactDTO(
                    "yuheng-mcp-gateway",
                    "top.egon.cola.component.yuheng.mcp.engine.McpGatewayEngineApplication"));

    private static final List<String> SENSITIVE_MARKERS = List.of(
            "password",
            "secret",
            "master-key",
            "access-key",
            "token"
    );

    public GatewayProcessSpec {
        name = required(name, "name");
        if (!name.matches("[A-Za-z0-9][A-Za-z0-9._-]*")) {
            throw new IllegalArgumentException(
                    "name must be a safe path segment"
            );
        }
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
        if (engineRole != null) {
            EngineArtifactDTO expected = ENGINE_ARTIFACTS.get(engineRole);
            if (!expected.artifactId().equals(artifactId) || !expected.mainClass().equals(mainClass)) {
                throw new IllegalArgumentException("Engine role, artifact and main class must agree");
            }
            validateEndpoint(dataPlaneBaseUri, "dataPlaneBaseUri");
            validateEndpoint(managementBaseUri, "managementBaseUri");
            if (dataPlaneBaseUri.getPort() == managementBaseUri.getPort()) {
                throw new IllegalArgumentException("Engine data and management ports must differ");
            }
        } else if (artifactId != null || dataPlaneBaseUri != null || managementBaseUri != null
                || ENGINE_ARTIFACTS.values().stream().map(EngineArtifactDTO::mainClass).toList().contains(mainClass)) {
            throw new IllegalArgumentException("Engine processes require an explicit role");
        }
    }

    public GatewayProcessSpec(String name, String mainClass, List<String> arguments,
                              Map<String, String> environment, Duration startupTimeout) {
        this(name, mainClass, arguments, environment, startupTimeout, null, null, null, null);
    }

    public static Builder engineBuilder(String name, GatewayEngineRoleEnum role,
                                        URI dataPlaneBaseUri, URI managementBaseUri) {
        EngineArtifactDTO artifact = ENGINE_ARTIFACTS.get(Objects.requireNonNull(role, "engineRole"));
        Builder builder = new Builder(name, artifact.mainClass());
        builder.engineRole = role;
        builder.artifactId = artifact.artifactId();
        builder.dataPlaneBaseUri = dataPlaneBaseUri;
        builder.managementBaseUri = managementBaseUri;
        return builder;
    }

    private static void validateEndpoint(URI uri, String field) {
        if (uri == null || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                || uri.getHost() == null || uri.getPort() < 1 || uri.getPort() > 65535
                || uri.getUserInfo() != null || uri.getRawQuery() != null || uri.getFragment() != null
                || !List.of("", "/").contains(uri.getPath())) {
            throw new IllegalArgumentException(field + " requires an HTTP(S) host and explicit port without credentials or path");
        }
    }

    private record EngineArtifactDTO(String artifactId, String mainClass) {
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
        private GatewayEngineRoleEnum engineRole;
        private String artifactId;
        private URI dataPlaneBaseUri;
        private URI managementBaseUri;

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
                    startupTimeout,
                    engineRole,
                    artifactId,
                    dataPlaneBaseUri,
                    managementBaseUri
            );
        }
    }
}
