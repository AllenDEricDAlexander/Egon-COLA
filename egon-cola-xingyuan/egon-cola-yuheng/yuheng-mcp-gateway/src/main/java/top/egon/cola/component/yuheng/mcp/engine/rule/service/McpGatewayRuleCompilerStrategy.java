package top.egon.cola.component.yuheng.mcp.engine.rule.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.egon.cola.component.yuheng.contract.mcp.rule.McpRuntimeTool;
import top.egon.cola.component.yuheng.contract.protocol.GatewayProtocol;
import top.egon.cola.component.yuheng.contract.rule.GatewayProviderServiceRef;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuntimeOperation;
import top.egon.cola.component.yuheng.core.provider.ProviderProtocolType;
import top.egon.cola.component.yuheng.core.provider.ProviderServiceKey;
import top.egon.cola.component.yuheng.mcp.engine.rule.domain.McpGatewayCompiledRulesDTO;
import top.egon.cola.component.yuheng.mcp.rule.service.McpRuleCompiler;
import top.egon.cola.component.yuheng.runtime.provider.service.GatewayProviderPolicyCompiler;
import top.egon.cola.component.yuheng.runtime.rule.service.GatewayRuleCompilerStrategy;
import top.egon.cola.component.yuheng.runtime.rule.service.GatewayTrafficPolicyCompiler;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 中文说明：只编译 MCP 能力及直接 Operation 调用所需的共享视图。
 * English summary: Compiles MCP capabilities and direct-provider state from the canonical release.
 * 用法 / Usage: Fixed MCP Strategy; managed provenance belongs to Admin's annotation projection.
 */
@Slf4j
@RequiredArgsConstructor
@Service("mcpGatewayRuleCompilerStrategy")
public final class McpGatewayRuleCompilerStrategy
        implements GatewayRuleCompilerStrategy<McpGatewayCompiledRulesDTO> {

    private final McpRuleCompiler mcpCompiler = new McpRuleCompiler();
    private final GatewayProviderPolicyCompiler providerCompiler = new GatewayProviderPolicyCompiler();
    private final GatewayTrafficPolicyCompiler trafficCompiler = new GatewayTrafficPolicyCompiler();

    @Override
    public McpGatewayCompiledRulesDTO compile(GatewayRuleSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        var content = snapshot.content();
        Map<String, GatewayRuntimeOperation> operations = content.operations().stream()
                .filter(operation -> !operation.deprecated())
                .collect(Collectors.toUnmodifiableMap(GatewayRuntimeOperation::operationId, Function.identity()));
        var mcpRules = mcpCompiler.compile(content.mcp(), operations.keySet());
        content.mcp().tools().forEach(tool -> validateToolSource(tool, operations));
        Set<ProviderServiceKey> services = operations.values().stream()
                .map(GatewayRuntimeOperation::providerService)
                .map(this::serviceKey)
                .collect(Collectors.toUnmodifiableSet());
        var compiled = new McpGatewayCompiledRulesDTO(snapshot, services,
                providerCompiler.compile(content.providerPolicies()),
                trafficCompiler.compile(content.trafficPolicies()), mcpRules);
        log.debug("Compiled MCP release={} servers={} tools={}", compiled.releaseId(),
                mcpRules.serversByCode().size(), mcpRules.toolsByQualifiedName().size());
        return compiled;
    }

    /**
     * 中文说明：LOCAL_OPERATION 也是注解托管 Tool 的正式 Wire 标记，不能整体拒绝。
     * English summary: Validates the published binding shape without inventing a managed-source wire flag.
     */
    private void validateToolSource(McpRuntimeTool tool, Map<String, GatewayRuntimeOperation> operations) {
        if ("LOCAL_OPERATION".equals(tool.sourceType())) {
            GatewayRuntimeOperation operation = operations.get(tool.operationId());
            if (operation == null || tool.remoteMountId() != null
                    || !operation.protocol().name().equals(tool.operationProtocol())) {
                throw new IllegalArgumentException("MCP_RULE_COMPILE_FAILED: invalid local operation binding");
            }
        } else if (!"REMOTE_MCP".equals(tool.sourceType())
                || tool.operationId() != null || tool.remoteMountId() == null) {
            throw new IllegalArgumentException("MCP_RULE_COMPILE_FAILED: unsupported tool source");
        }
    }

    /**
     * 中文说明：复用既有 Provider 标识的字段语义，保留 HTTP/RPC 直接调用目标。
     * English summary: Constructs the existing provider identity used by direct operation invocation.
     */
    private ProviderServiceKey serviceKey(GatewayProviderServiceRef service) {
        return new ProviderServiceKey(service.bizCode(), service.appCode(), service.env(), service.namespace(),
                service.protocol() == GatewayProtocol.HTTP ? ProviderProtocolType.HTTP : ProviderProtocolType.RPC,
                service.serviceName(), service.group(), service.version(), service.transport());
    }
}
