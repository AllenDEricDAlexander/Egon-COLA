package top.egon.cola.component.gateway.contract.mcp.rule;

import java.util.Map;
import java.util.Set;

public record McpRuntimeRemoteMount(
        String mountId,
        String serverCode,
        String providerCode,
        String namespace,
        Set<String> primitiveTypes,
        Map<String, String> renameRules,
        String conflictPolicy,
        Set<String> requiredPermissions,
        String capabilityFingerprint,
        boolean enabled
) {

    public McpRuntimeRemoteMount {
        mountId = McpContractSupport.required(mountId, "mountId");
        serverCode = McpContractSupport.required(serverCode, "serverCode");
        providerCode = McpContractSupport.required(
                providerCode,
                "providerCode"
        );
        namespace = McpContractSupport.required(namespace, "namespace");
        primitiveTypes = McpContractSupport.sortedStrings(primitiveTypes);
        renameRules = McpContractSupport.sortedMap(renameRules);
        conflictPolicy = McpContractSupport.required(
                conflictPolicy,
                "conflictPolicy"
        );
        requiredPermissions = McpContractSupport.sortedStrings(
                requiredPermissions
        );
        capabilityFingerprint = McpContractSupport.required(
                capabilityFingerprint,
                "capabilityFingerprint"
        );
    }
}
