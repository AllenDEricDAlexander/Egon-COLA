package top.egon.cola.component.gateway.provider;

import top.egon.cola.component.gateway.contract.definition.GatewayDefinitionIdentity;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public record HttpProviderRuntimeProperties(
        boolean enabled,
        String env,
        String namespace,
        String instanceId,
        String serviceName,
        String group,
        String version,
        String protocol,
        String host,
        int port,
        int leaseSeconds,
        int heartbeatIntervalSeconds,
        boolean failFast,
        Map<String, String> metadata,
        GatewayDefinitionIdentity definitionIdentity
) {

    public HttpProviderRuntimeProperties {
        env = required(env, "env");
        namespace = required(namespace, "namespace");
        instanceId = required(instanceId, "instanceId");
        serviceName = required(serviceName, "serviceName");
        group = required(group, "group");
        version = required(version, "version");
        protocol = required(protocol, "protocol").toLowerCase(Locale.ROOT);
        if (!protocol.equals("http") && !protocol.equals("https")) {
            throw new IllegalArgumentException(
                    "protocol must be http or https"
            );
        }
        host = required(host, "host");
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException(
                    "port must be between 0 and 65535"
            );
        }
        if (leaseSeconds <= 0
                || heartbeatIntervalSeconds <= 0
                || heartbeatIntervalSeconds >= leaseSeconds) {
            throw new IllegalArgumentException(
                    "heartbeat interval must be positive and less than lease"
            );
        }
        if (!local(env) && port == 0) {
            throw new IllegalArgumentException(
                    "port=0 is only allowed in local/test"
            );
        }
        if (!local(env) && unsafeHost(host)) {
            throw new IllegalArgumentException(
                    "wildcard or loopback provider host is not allowed"
            );
        }
        metadata = mergedMetadata(metadata, definitionIdentity);
    }

    public int resolvedPort(int actualServerPort) {
        int resolved = port == 0 ? actualServerPort : port;
        if (resolved < 1 || resolved > 65535) {
            throw new IllegalArgumentException(
                    "resolved HTTP server port is invalid"
            );
        }
        return resolved;
    }

    private static Map<String, String> mergedMetadata(
            Map<String, String> source,
            GatewayDefinitionIdentity identity) {
        Map<String, String> result = new LinkedHashMap<>(
                source == null ? Map.of() : source
        );
        if (identity != null) {
            putStable(
                    result,
                    "gateway.definition-set-id",
                    identity.definitionSetId()
            );
            putStable(
                    result,
                    "gateway.artifact-version",
                    identity.artifactVersion()
            );
            putStable(result, "gateway.build-id", identity.buildId());
        }
        return Map.copyOf(result);
    }

    private static void putStable(
            Map<String, String> target,
            String key,
            String value) {
        String previous = target.putIfAbsent(key, value);
        if (previous != null && !previous.equals(value)) {
            throw new IllegalArgumentException(
                    "provider metadata conflicts with definition identity: "
                            + key
            );
        }
    }

    private static boolean unsafeHost(String host) {
        if (host.equals("0.0.0.0") || host.equals("::")
                || host.equals("[::]") || host.equalsIgnoreCase("localhost")) {
            return true;
        }
        try {
            return InetAddress.getByName(host).isLoopbackAddress();
        } catch (UnknownHostException ignored) {
            return false;
        }
    }

    private static boolean local(String env) {
        return env.equalsIgnoreCase("local")
                || env.equalsIgnoreCase("test");
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
