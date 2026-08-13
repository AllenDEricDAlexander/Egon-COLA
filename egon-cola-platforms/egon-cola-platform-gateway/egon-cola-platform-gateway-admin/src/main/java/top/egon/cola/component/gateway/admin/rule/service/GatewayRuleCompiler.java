package top.egon.cola.component.gateway.admin.rule.service;


import top.egon.cola.component.gateway.admin.application.controller.*;
import top.egon.cola.component.gateway.admin.application.domain.dto.*;
import top.egon.cola.component.gateway.admin.application.domain.exception.*;
import top.egon.cola.component.gateway.admin.application.domain.po.*;
import top.egon.cola.component.gateway.admin.application.domain.vo.*;
import top.egon.cola.component.gateway.admin.application.repository.*;
import top.egon.cola.component.gateway.admin.application.service.*;
import top.egon.cola.component.gateway.admin.auth.controller.*;
import top.egon.cola.component.gateway.admin.auth.domain.vo.*;
import top.egon.cola.component.gateway.admin.auth.service.*;
import top.egon.cola.component.gateway.admin.bootstrap.*;
import top.egon.cola.component.gateway.admin.catalog.controller.*;
import top.egon.cola.component.gateway.admin.catalog.domain.dto.*;
import top.egon.cola.component.gateway.admin.catalog.domain.enums.*;
import top.egon.cola.component.gateway.admin.catalog.domain.po.*;
import top.egon.cola.component.gateway.admin.catalog.domain.vo.*;
import top.egon.cola.component.gateway.admin.catalog.repository.*;
import top.egon.cola.component.gateway.admin.catalog.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.catalog.service.*;
import top.egon.cola.component.gateway.admin.config.*;
import top.egon.cola.component.gateway.admin.config.properties.*;
import top.egon.cola.component.gateway.admin.credential.controller.*;
import top.egon.cola.component.gateway.admin.credential.domain.dto.*;
import top.egon.cola.component.gateway.admin.credential.domain.po.*;
import top.egon.cola.component.gateway.admin.credential.domain.vo.*;
import top.egon.cola.component.gateway.admin.credential.repository.*;
import top.egon.cola.component.gateway.admin.credential.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.credential.service.*;
import top.egon.cola.component.gateway.admin.group.controller.*;
import top.egon.cola.component.gateway.admin.group.domain.dto.*;
import top.egon.cola.component.gateway.admin.group.domain.po.*;
import top.egon.cola.component.gateway.admin.group.domain.vo.*;
import top.egon.cola.component.gateway.admin.group.repository.*;
import top.egon.cola.component.gateway.admin.group.service.*;
import top.egon.cola.component.gateway.admin.mcp.controller.*;
import top.egon.cola.component.gateway.admin.mcp.domain.dto.*;
import top.egon.cola.component.gateway.admin.mcp.domain.enums.*;
import top.egon.cola.component.gateway.admin.mcp.domain.exception.*;
import top.egon.cola.component.gateway.admin.mcp.domain.po.*;
import top.egon.cola.component.gateway.admin.mcp.domain.vo.*;
import top.egon.cola.component.gateway.admin.mcp.repository.*;
import top.egon.cola.component.gateway.admin.mcp.repository.filesystem.*;
import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.mcp.service.*;
import top.egon.cola.component.gateway.admin.observability.controller.*;
import top.egon.cola.component.gateway.admin.observability.controller.message.*;
import top.egon.cola.component.gateway.admin.observability.controller.scheduled.*;
import top.egon.cola.component.gateway.admin.observability.domain.dto.*;
import top.egon.cola.component.gateway.admin.observability.domain.enums.*;
import top.egon.cola.component.gateway.admin.observability.domain.po.*;
import top.egon.cola.component.gateway.admin.observability.domain.vo.*;
import top.egon.cola.component.gateway.admin.observability.repository.*;
import top.egon.cola.component.gateway.admin.observability.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.observability.service.*;
import top.egon.cola.component.gateway.admin.release.controller.*;
import top.egon.cola.component.gateway.admin.release.controller.scheduled.*;
import top.egon.cola.component.gateway.admin.release.domain.*;
import top.egon.cola.component.gateway.admin.release.domain.dto.*;
import top.egon.cola.component.gateway.admin.release.domain.enums.*;
import top.egon.cola.component.gateway.admin.release.domain.po.*;
import top.egon.cola.component.gateway.admin.release.domain.vo.*;
import top.egon.cola.component.gateway.admin.release.repository.*;
import top.egon.cola.component.gateway.admin.release.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.release.service.*;
import top.egon.cola.component.gateway.admin.reporting.controller.openapi.*;
import top.egon.cola.component.gateway.admin.reporting.controller.scheduled.*;
import top.egon.cola.component.gateway.admin.reporting.domain.dto.*;
import top.egon.cola.component.gateway.admin.reporting.domain.po.*;
import top.egon.cola.component.gateway.admin.reporting.domain.vo.*;
import top.egon.cola.component.gateway.admin.reporting.repository.*;
import top.egon.cola.component.gateway.admin.reporting.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.reporting.service.*;
import top.egon.cola.component.gateway.admin.routing.controller.*;
import top.egon.cola.component.gateway.admin.routing.domain.*;
import top.egon.cola.component.gateway.admin.routing.domain.dto.*;
import top.egon.cola.component.gateway.admin.routing.domain.po.*;
import top.egon.cola.component.gateway.admin.routing.domain.vo.*;
import top.egon.cola.component.gateway.admin.routing.repository.*;
import top.egon.cola.component.gateway.admin.routing.repository.jdbc.*;
import top.egon.cola.component.gateway.admin.routing.service.*;
import top.egon.cola.component.gateway.admin.rule.domain.dto.*;
import top.egon.cola.component.gateway.admin.rule.domain.vo.*;
import top.egon.cola.component.gateway.admin.rule.service.*;
import top.egon.cola.component.gateway.admin.runtime.controller.*;
import top.egon.cola.component.gateway.admin.runtime.domain.dto.*;
import top.egon.cola.component.gateway.admin.runtime.domain.vo.*;
import top.egon.cola.component.gateway.admin.runtime.service.*;
import top.egon.cola.component.gateway.admin.scope.controller.*;
import top.egon.cola.component.gateway.admin.scope.domain.*;
import top.egon.cola.component.gateway.admin.scope.domain.dto.*;
import top.egon.cola.component.gateway.admin.scope.domain.vo.*;
import top.egon.cola.component.gateway.admin.scope.service.*;
import top.egon.cola.component.gateway.admin.shared.controller.*;
import top.egon.cola.component.gateway.admin.shared.domain.*;
import top.egon.cola.component.gateway.admin.shared.domain.enums.*;
import top.egon.cola.component.gateway.admin.shared.domain.exception.*;
import top.egon.cola.component.gateway.admin.shared.domain.po.*;
import top.egon.cola.component.gateway.admin.shared.domain.vo.*;
import top.egon.cola.component.gateway.admin.shared.repository.*;
import top.egon.cola.component.gateway.admin.shared.repository.jdbc.*;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayProviderServiceRef;
import top.egon.cola.component.gateway.contract.rule.GatewayRpcDescriptor;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleActivation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleActivationMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleChunkRef;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeOperation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimePolicy;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeRoute;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.route.GatewayResponseMode;
import top.egon.cola.component.gateway.core.route.HttpRouteCompiler;
import top.egon.cola.component.gateway.core.route.RuntimeHttpRoute;
import top.egon.cola.component.gateway.mcp.rule.McpRuleCompiler;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * 中文说明：{@code GatewayRuleCompiler} 是编译器，位于当前 Gateway 模块的相关包中，负责网关规则Compiler相关的职责与边界。
 * English summary: {@code GatewayRuleCompiler} is a gateway rule compiler compiler in the current Gateway module; it owns the gateway rule compiler-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayRuleCompiler {

    /**
     * 中文说明：表示 INLINELIMITBYTES 这一固定值；它属于 {@code GatewayRuleCompiler} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value inline limit bytes; it is a state, type, or protocol value of {@code GatewayRuleCompiler} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRuleCompiler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleCompiler}; do not couple callers to its representation when the owning type exposes an API.
     */
    public static final int INLINE_LIMIT_BYTES = 512 * 1024;

    /**
     * 中文说明：表示 CHUNKLIMITBYTES 这一固定值；它属于 {@code GatewayRuleCompiler} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value chunk limit bytes; it is a state, type, or protocol value of {@code GatewayRuleCompiler} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRuleCompiler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleCompiler}; do not couple callers to its representation when the owning type exposes an API.
     */
    public static final int CHUNK_LIMIT_BYTES = 256 * 1024;

    /**
     * 中文说明：表示 SUPPORTEDPOLICIES 这一固定值；它属于 {@code GatewayRuleCompiler} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value supported policies; it is a state, type, or protocol value of {@code GatewayRuleCompiler} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRuleCompiler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleCompiler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Set<String> SUPPORTED_POLICIES = Set.of(
            "LOAD_BALANCE",
            "PROVIDER_OVERRIDE",
            "RATE_LIMIT",
            "CIRCUIT_BREAKER",
            "RETRY",
            "TIMEOUT",
            "BULKHEAD",
            "REQUEST_SIZE",
            "RESPONSE_SIZE",
            "SECURITY",
            "AUTHENTICATION",
            "HEADER_TOKEN",
            "SIGNATURE",
            "IP_ACL",
            "CORS"
    );

    /**
     * 中文说明：保存 canonicalizer 对应的状态、依赖或配置值；字段类型为 {@code GatewayRuleCanonicalizer}，由 {@code GatewayRuleCompiler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by canonicalizer; its type is {@code GatewayRuleCanonicalizer}, and {@code GatewayRuleCompiler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRuleCompiler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleCompiler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayRuleCanonicalizer canonicalizer;

    /**
     * 中文说明：保存 MCPCompiler 对应的状态、依赖或配置值；字段类型为 {@code McpRuleCompiler}，由 {@code GatewayRuleCompiler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by mcp compiler; its type is {@code McpRuleCompiler}, and {@code GatewayRuleCompiler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRuleCompiler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleCompiler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpRuleCompiler mcpCompiler = new McpRuleCompiler();

    /**
     * 中文说明：保存 传输校验器 对应的状态、依赖或配置值；字段类型为 {@code GatewayRouteTransportPolicyValidator}，由 {@code GatewayRuleCompiler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by transport validator; its type is {@code GatewayRouteTransportPolicyValidator}, and {@code GatewayRuleCompiler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRuleCompiler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleCompiler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayRouteTransportPolicyValidator transportValidator =
            new GatewayRouteTransportPolicyValidator();

    /**
     * 中文说明：创建 {@code GatewayRuleCompiler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayRuleCompiler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param canonicalizer 参数 canonicalizer；parameter canonicalizer。
     */
    public GatewayRuleCompiler(GatewayRuleCanonicalizer canonicalizer) {
        this.canonicalizer = canonicalizer;
    }

    /**
     * 中文说明：执行 compile 操作；该方法是 {@code GatewayRuleCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the compile operation; this method is the invocation entry point on {@code GatewayRuleCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleCompiler.compile(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param generatedAt 参数 generatedAt；parameter generated at。
     * @param content 参数 content；parameter content。
     * @return 返回 compile 的处理结果；returns the result of the operation.
     */
    public CompiledGatewayRelease compile(
            String releaseId,
            Instant generatedAt,
            GatewayRuleContent content) {
        validate(content);
        GatewayRuleSnapshot snapshot = canonicalizer.snapshot(
                releaseId,
                generatedAt,
                content
        );
        String snapshotJson = canonicalizer.json(snapshot);
        byte[] bytes = snapshotJson.getBytes(StandardCharsets.UTF_8);
        Map<String, String> chunks = new LinkedHashMap<>();
        List<GatewayRuleChunkRef> references = new ArrayList<>();
        GatewayRuleActivationMode mode;
        String inline;
        if (bytes.length <= INLINE_LIMIT_BYTES) {
            mode = GatewayRuleActivationMode.INLINE;
            inline = snapshotJson;
        } else {
            mode = GatewayRuleActivationMode.CHUNKED;
            inline = null;
            for (int offset = 0, index = 0;
                 offset < bytes.length;
                 offset += CHUNK_LIMIT_BYTES, index++) {
                int length = Math.min(CHUNK_LIMIT_BYTES, bytes.length - offset);
                byte[] part = java.util.Arrays.copyOfRange(
                        bytes,
                        offset,
                        offset + length
                );
                String key = "gateway.rules.chunk."
                        + releaseId
                        + "."
                        + index;
                chunks.put(key, Base64.getEncoder().encodeToString(part));
                references.add(new GatewayRuleChunkRef(
                        key,
                        index,
                        length,
                        GatewayRuleCanonicalizer.sha256(part)
                ));
            }
        }
        GatewayRuleActivation activation = new GatewayRuleActivation(
                "v1",
                releaseId,
                mode,
                snapshot.ruleSchemaVersion(),
                bytes.length,
                snapshot.ruleContentSha256(),
                snapshot.artifactSha256(),
                inline,
                references
        );
        return new CompiledGatewayRelease(
                snapshot,
                snapshotJson,
                activation,
                canonicalizer.json(activation),
                chunks
        );
    }

    /**
     * 中文说明：执行 validate 操作；该方法是 {@code GatewayRuleCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate operation; this method is the invocation entry point on {@code GatewayRuleCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleCompiler.validate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     */
    private void validate(GatewayRuleContent content) {
        mcpCompiler.compile(
                content.mcp(),
                content.operations().stream()
                        .map(GatewayRuntimeOperation::operationId)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet())
        );
        unique(content.operations(), GatewayRuntimeOperation::operationId);
        unique(content.operations(), GatewayRuntimeOperation::operationKey);
        unique(content.routes(), GatewayRuntimeRoute::routeId);
        List<GatewayRuntimePolicy> policies = allPolicies(content);
        unique(policies, GatewayRuntimePolicy::policyId);
        unique(content.rpcDescriptors(), GatewayRpcDescriptor::descriptorId);
        Set<String> policyIds = policies.stream()
                .map(GatewayRuntimePolicy::policyId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        Map<String, GatewayRuntimePolicy> policiesById = policies.stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        GatewayRuntimePolicy::policyId,
                        Function.identity()
                ));
        Map<String, GatewayRuntimeOperation> operations = content.operations()
                .stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        GatewayRuntimeOperation::operationId,
                        Function.identity()
                ));
        content.routes().forEach(route -> {
            GatewayRuntimeOperation operation = operations.get(
                    route.operationId()
            );
            if (operation == null) {
                return;
            }
            List<top.egon.cola.component.gateway.admin.routing.service.GatewayTransportValidationIssue> issues =
                    transportValidator.validate(route, operation);
            if (!issues.isEmpty()) {
                top.egon.cola.component.gateway.admin.routing.service.GatewayTransportValidationIssue issue =
                        issues.getFirst();
                throw invalid(
                        issue.code()
                                + " at routes."
                                + route.routeId()
                                + "."
                                + issue.path()
                                + ": "
                                + issue.message()
                );
            }
        });
        policies.forEach(policy -> {
            if (!SUPPORTED_POLICIES.contains(policy.type())) {
                throw invalid("unsupported policy type " + policy.type());
            }
        });
        content.operations().forEach(operation -> {
            if (!policyIds.containsAll(operation.policyRefs())) {
                throw invalid(
                        "operation references an unknown policy: "
                                + operation.operationId()
                );
            }
            if (operation.protocol()
                    != operation.providerService().protocol()) {
                throw invalid(
                        "operation/provider protocol mismatch: "
                                + operation.operationId()
                );
            }
            if (operation.providerService().serviceName().contains("://")) {
                throw invalid("static provider URL is forbidden");
            }
            if (Boolean.parseBoolean(
                    operation.attributes().get("streaming"))) {
                throw invalid("streaming operation is not supported in v1");
            }
            boolean retryReferenced = operation.policyRefs().stream()
                    .map(policiesById::get)
                    .anyMatch(policy -> "RETRY".equals(policy.type()));
            if (retryReferenced && !Boolean.parseBoolean(
                    operation.attributes().get("idempotent"))) {
                throw invalid(
                        "Retry requires an explicitly idempotent operation: "
                                + operation.operationId()
                );
            }
        });
        List<RuntimeHttpRoute> runtimeRoutes = content.routes().stream()
                .filter(GatewayRuntimeRoute::enabled)
                .map(route -> runtimeRoute(content, route, operations))
                .toList();
        new HttpRouteCompiler().compile(runtimeRoutes);
        validateDescriptors(content.rpcDescriptors());
    }

    /**
     * 中文说明：执行 运行时路由 操作；该方法是 {@code GatewayRuleCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the runtime route operation; this method is the invocation entry point on {@code GatewayRuleCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleCompiler.runtimeRoute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     * @param route 参数 路由；parameter route。
     * @param operations 参数 operations；parameter operations。
     * @return 返回 运行时路由 的处理结果；returns the result of the operation.
     */
    private RuntimeHttpRoute runtimeRoute(
            GatewayRuleContent content,
            GatewayRuntimeRoute route,
            Map<String, GatewayRuntimeOperation> operations) {
        GatewayRuntimeOperation operation = operations.get(route.operationId());
        if (operation == null) {
            throw invalid("route references unknown operation");
        }
        if (route.accessZones().contains(AccessZone.PUBLIC)
                && !operation.externalAccessible()) {
            throw invalid(
                    "PUBLIC route references an internal-only operation"
            );
        }
        GatewayProviderServiceRef service = operation.providerService();
        ProviderProtocolType protocol = service.protocol()
                == GatewayProtocol.HTTP
                ? ProviderProtocolType.HTTP
                : ProviderProtocolType.RPC;
        return new RuntimeHttpRoute(
                route.routeId(),
                operation.operationId(),
                content.gatewayGroupId(),
                route.accessZones(),
                route.host(),
                Set.of(route.httpMethod()),
                route.pathPattern(),
                operation.externalAccessible(),
                new ProviderServiceKey(
                        service.bizCode(),
                        service.appCode(),
                        service.env(),
                        service.namespace(),
                        protocol,
                        service.serviceName(),
                        service.group(),
                        service.version(),
                        service.transport()
                ),
                operation.policyRefs(),
                route.priority(),
                GatewayResponseMode.valueOf(operation.responseMode()),
                Map.of()
        );
    }

    /**
     * 中文说明：执行 validateDescriptors 操作；该方法是 {@code GatewayRuleCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate descriptors operation; this method is the invocation entry point on {@code GatewayRuleCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleCompiler.validateDescriptors(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param descriptors 参数 descriptors；parameter descriptors。
     */
    private void validateDescriptors(List<GatewayRpcDescriptor> descriptors) {
        for (GatewayRpcDescriptor descriptor : descriptors) {
            byte[] value;
            try {
                value = Base64.getDecoder().decode(
                        descriptor.base64DescriptorSet()
                );
            } catch (IllegalArgumentException invalidBase64) {
                throw invalid("RPC descriptor is not valid base64");
            }
            if (!GatewayRuleCanonicalizer.sha256(value)
                    .equals(descriptor.sha256())) {
                throw invalid("RPC descriptor checksum mismatch");
            }
        }
    }

    /**
     * 中文说明：执行 allPolicies 操作；该方法是 {@code GatewayRuleCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the all policies operation; this method is the invocation entry point on {@code GatewayRuleCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleCompiler.allPolicies(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     * @return 返回 allPolicies 的处理结果；returns the result of the operation.
     */
    private List<GatewayRuntimePolicy> allPolicies(
            GatewayRuleContent content) {
        List<GatewayRuntimePolicy> result = new ArrayList<>();
        result.addAll(content.providerPolicies());
        result.addAll(content.trafficPolicies());
        result.addAll(content.securityPolicies());
        result.addAll(content.corsPolicies());
        return List.copyOf(result);
    }

    /**
     * 中文说明：执行 unique 操作；该方法是 {@code GatewayRuleCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the unique operation; this method is the invocation entry point on {@code GatewayRuleCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleCompiler.unique(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param values 参数 values；parameter values。
     * @param key 参数 键；parameter key。
     */
    private <T> void unique(List<T> values, Function<T, String> key) {
        Set<String> seen = new HashSet<>();
        values.forEach(value -> {
            if (!seen.add(key.apply(value))) {
                throw invalid("duplicate rule identifier " + key.apply(value));
            }
        });
    }

    /**
     * 中文说明：执行 invalid 操作；该方法是 {@code GatewayRuleCompiler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the invalid operation; this method is the invocation entry point on {@code GatewayRuleCompiler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleCompiler.invalid(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param detail 参数 detail；parameter detail。
     * @return 返回 invalid 的处理结果；returns the result of the operation.
     */
    private IllegalArgumentException invalid(String detail) {
        return new IllegalArgumentException(
                "GATEWAY_RELEASE_VALIDATION_FAILED: " + detail
        );
    }
}
