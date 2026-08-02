package top.egon.cola.component.gateway.mcp.remote;

import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeRemoteMount;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeRemoteProvider;
import top.egon.cola.component.gateway.mcp.rule.CompiledMcpRules;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Applies the immutable namespace and rename rules from an Active Release.
 */
public final class McpNamespaceRouter {

    public Binding binding(
            CompiledMcpRules rules,
            String mountId,
            String primitiveType,
            String exposedName) {
        Objects.requireNonNull(rules, "rules");
        McpRuntimeRemoteMount mount = rules.remoteMountsById().get(mountId);
        if (mount == null || !mount.enabled()) {
            throw new IllegalArgumentException("remote MCP mount is unavailable");
        }
        McpRuntimeRemoteProvider provider = rules.remoteProvidersByCode().get(
                mount.providerCode()
        );
        if (provider == null || !provider.enabled()) {
            throw new IllegalArgumentException(
                    "remote MCP Provider is unavailable"
            );
        }
        if (!provider.capabilityFingerprint().equals(
                mount.capabilityFingerprint()
        )) {
            throw new IllegalArgumentException(
                    "remote MCP capability fingerprint has drifted"
            );
        }
        String type = required(primitiveType, "primitiveType")
                .toUpperCase(Locale.ROOT);
        if (!mount.primitiveTypes().contains(type)) {
            throw new IllegalArgumentException(
                    "remote MCP primitive is not mounted"
            );
        }
        return new Binding(
                mount,
                provider,
                remoteName(mount, exposedName)
        );
    }

    /**
     * Rename rules are stored as {@code remoteName -> localName}.
     */
    public String exposedName(
            McpRuntimeRemoteMount mount,
            String remoteName) {
        String remote = required(remoteName, "remoteName");
        String local = mount.renameRules().getOrDefault(remote, remote);
        return mount.namespace() + "." + local;
    }

    public String remoteName(
            McpRuntimeRemoteMount mount,
            String exposedName) {
        String exposed = required(exposedName, "exposedName");
        String prefix = mount.namespace() + ".";
        if (!exposed.startsWith(prefix) || exposed.length() == prefix.length()) {
            throw new IllegalArgumentException(
                    "remote MCP capability is outside its namespace"
            );
        }
        String local = exposed.substring(prefix.length());
        return mount.renameRules().entrySet().stream()
                .filter(entry -> entry.getValue().equals(local))
                .map(java.util.Map.Entry::getKey)
                .findFirst()
                .orElse(local);
    }

    public Set<String> merge(
            Set<String> existing,
            McpRuntimeRemoteMount mount,
            String remoteName) {
        LinkedHashSet<String> result = new LinkedHashSet<>(
                Objects.requireNonNull(existing, "existing")
        );
        String exposed = exposedName(mount, remoteName);
        if (!result.contains(exposed)) {
            result.add(exposed);
            return Set.copyOf(result);
        }
        switch (mount.conflictPolicy().toUpperCase(Locale.ROOT)) {
            case "KEEP_LOCAL" -> {
                return Set.copyOf(result);
            }
            case "REPLACE" -> {
                result.remove(exposed);
                result.add(exposed);
                return Set.copyOf(result);
            }
            case "REJECT" -> throw new IllegalArgumentException(
                    "remote MCP capability conflicts with an existing name"
            );
            default -> throw new IllegalArgumentException(
                    "remote MCP conflict policy is unsupported"
            );
        }
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "remote MCP " + field + " is required"
            );
        }
        return value.trim();
    }

    public record Binding(
            McpRuntimeRemoteMount mount,
            McpRuntimeRemoteProvider provider,
            String remoteName
    ) {

        public Binding {
            mount = Objects.requireNonNull(mount, "mount");
            provider = Objects.requireNonNull(provider, "provider");
            remoteName = Objects.requireNonNull(remoteName, "remoteName");
        }
    }
}
