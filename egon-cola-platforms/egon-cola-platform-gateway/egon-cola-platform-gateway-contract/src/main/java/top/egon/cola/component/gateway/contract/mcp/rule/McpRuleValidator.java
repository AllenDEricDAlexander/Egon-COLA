package top.egon.cola.component.gateway.contract.mcp.rule;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * MCP 规则内容的跨对象一致性校验器。
 *
 * <p>负责检查 Server、能力名称、远程 provider 和 namespace 的唯一性及引用完整性。
 */
public final class McpRuleValidator {

    private McpRuleValidator() {
    }

    public static void validateServers(List<McpRuntimeServer> servers) {
        unique(servers, McpRuntimeServer::serverId, "server id");
        unique(servers, McpRuntimeServer::serverCode, "server code");
    }

    public static void validateCapabilityNames(
            List<McpRuntimeTool> tools,
            List<McpRuntimeResource> resources,
            List<McpRuntimeResourceTemplate> resourceTemplates,
            List<McpRuntimePrompt> prompts,
            List<McpRuntimeApp> apps) {
        HashSet<String> names = new HashSet<>();
        tools.forEach(tool -> addCapability(
                names,
                tool.serverCode(),
                tool.name()
        ));
        resources.forEach(resource -> addCapability(
                names,
                resource.serverCode(),
                resource.name()
        ));
        resourceTemplates.forEach(template -> addCapability(
                names,
                template.serverCode(),
                template.name()
        ));
        prompts.forEach(prompt -> addCapability(
                names,
                prompt.serverCode(),
                prompt.name()
        ));
        apps.forEach(app -> addCapability(
                names,
                app.serverCode(),
                app.name()
        ));
    }

    public static void validateRemoteNamespaces(
            List<McpRuntimeRemoteProvider> providers,
            List<McpRuntimeRemoteMount> mounts) {
        unique(
                providers,
                McpRuntimeRemoteProvider::providerId,
                "remote provider id"
        );
        Set<String> providerCodes = unique(
                providers,
                McpRuntimeRemoteProvider::providerCode,
                "remote provider code"
        );
        unique(mounts, McpRuntimeRemoteMount::mountId, "remote mount id");
        unique(
                mounts,
                mount -> mount.serverCode() + "\u0000" + mount.namespace(),
                "remote namespace"
        );
        mounts.forEach(mount -> {
            if (!providerCodes.contains(mount.providerCode())) {
                throw new IllegalArgumentException(
                        "remote mount references unknown provider: "
                                + mount.providerCode()
                );
            }
        });
    }

    private static void addCapability(
            Set<String> names,
            String serverCode,
            String name) {
        String key = serverCode + "\u0000" + name;
        if (!names.add(key)) {
            throw new IllegalArgumentException(
                    "duplicate MCP capability name: " + name
            );
        }
    }

    private static <T> Set<String> unique(
            List<T> values,
            Function<T, String> key,
            String label) {
        HashSet<String> seen = new HashSet<>();
        values.forEach(value -> {
            String candidate = key.apply(value);
            if (!seen.add(candidate)) {
                throw new IllegalArgumentException(
                        "duplicate MCP " + label + ": " + candidate
                );
            }
        });
        return Set.copyOf(seen);
    }
}
