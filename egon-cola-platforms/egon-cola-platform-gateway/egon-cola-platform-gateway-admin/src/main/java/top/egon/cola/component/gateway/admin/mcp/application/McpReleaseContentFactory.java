package top.egon.cola.component.gateway.admin.mcp.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.gateway.admin.application.GatewayAdminNotFoundException;
import top.egon.cola.component.gateway.admin.application.catalog.GatewayCatalogStore;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayDraftRepository;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpArtifactMetadataStore;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpCapabilityDraftStore;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpManagedToolOverrideStore;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpRemoteProviderStore;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpRemoteToolDraftStore;
import top.egon.cola.component.gateway.admin.mcp.persistence.McpServerEntity;
import top.egon.cola.component.gateway.admin.mcp.persistence.McpServerRepository;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuleContent;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeApp;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimePrompt;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeRemoteMount;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeRemoteProvider;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeResource;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeResourceTemplate;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeServer;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTaskPolicy;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTool;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 中文说明：{@code McpReleaseContentFactory} 是工厂，位于当前 Gateway 模块的相关包中，负责MCP发布Content工厂相关的职责与边界。
 * English summary: {@code McpReleaseContentFactory} is a mcp release content factory factory in the current Gateway module; it owns the mcp release content factory-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Service
public class McpReleaseContentFactory {

    /**
     * 中文说明：表示 RISKLEVELS 这一固定值；它属于 {@code McpReleaseContentFactory} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value risk levels; it is a state, type, or protocol value of {@code McpReleaseContentFactory} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code McpReleaseContentFactory} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpReleaseContentFactory}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final List<String> RISK_LEVELS = List.of(
            "LOW", "MEDIUM", "HIGH", "CRITICAL"
    );

    /**
     * 中文说明：保存 servers 对应的状态、依赖或配置值；字段类型为 {@code McpServerRepository}，由 {@code McpReleaseContentFactory} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by servers; its type is {@code McpServerRepository}, and {@code McpReleaseContentFactory} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpReleaseContentFactory} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpReleaseContentFactory}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpServerRepository servers;

    /**
     * 中文说明：保存 capabilities 对应的状态、依赖或配置值；字段类型为 {@code JdbcMcpCapabilityDraftStore}，由 {@code McpReleaseContentFactory} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by capabilities; its type is {@code JdbcMcpCapabilityDraftStore}, and {@code McpReleaseContentFactory} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpReleaseContentFactory} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpReleaseContentFactory}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcMcpCapabilityDraftStore capabilities;

    /**
     * 中文说明：保存 managedOverrides 对应的状态、依赖或配置值；字段类型为 {@code JdbcMcpManagedToolOverrideStore}，由 {@code McpReleaseContentFactory} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by managed overrides; its type is {@code JdbcMcpManagedToolOverrideStore}, and {@code McpReleaseContentFactory} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpReleaseContentFactory} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpReleaseContentFactory}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcMcpManagedToolOverrideStore managedOverrides;

    /**
     * 中文说明：保存 远程Tools 对应的状态、依赖或配置值；字段类型为 {@code JdbcMcpRemoteToolDraftStore}，由 {@code McpReleaseContentFactory} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by remote tools; its type is {@code JdbcMcpRemoteToolDraftStore}, and {@code McpReleaseContentFactory} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpReleaseContentFactory} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpReleaseContentFactory}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcMcpRemoteToolDraftStore remoteTools;

    /**
     * 中文说明：保存 远程 对应的状态、依赖或配置值；字段类型为 {@code JdbcMcpRemoteProviderStore}，由 {@code McpReleaseContentFactory} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by remote; its type is {@code JdbcMcpRemoteProviderStore}, and {@code McpReleaseContentFactory} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpReleaseContentFactory} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpReleaseContentFactory}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcMcpRemoteProviderStore remote;

    /**
     * 中文说明：保存 artifacts 对应的状态、依赖或配置值；字段类型为 {@code JdbcMcpArtifactMetadataStore}，由 {@code McpReleaseContentFactory} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by artifacts; its type is {@code JdbcMcpArtifactMetadataStore}, and {@code McpReleaseContentFactory} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpReleaseContentFactory} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpReleaseContentFactory}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final JdbcMcpArtifactMetadataStore artifacts;

    /**
     * 中文说明：保存 drafts 对应的状态、依赖或配置值；字段类型为 {@code GatewayDraftRepository}，由 {@code McpReleaseContentFactory} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by drafts; its type is {@code GatewayDraftRepository}, and {@code McpReleaseContentFactory} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpReleaseContentFactory} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpReleaseContentFactory}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayDraftRepository drafts;

    /**
     * 中文说明：保存 目录 对应的状态、依赖或配置值；字段类型为 {@code GatewayCatalogStore}，由 {@code McpReleaseContentFactory} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by catalog; its type is {@code GatewayCatalogStore}, and {@code McpReleaseContentFactory} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpReleaseContentFactory} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpReleaseContentFactory}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayCatalogStore catalog;

    /**
     * 中文说明：保存 validation 对应的状态、依赖或配置值；字段类型为 {@code McpValidationService}，由 {@code McpReleaseContentFactory} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by validation; its type is {@code McpValidationService}, and {@code McpReleaseContentFactory} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpReleaseContentFactory} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpReleaseContentFactory}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpValidationService validation;

    /**
     * 中文说明：保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code McpReleaseContentFactory} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code McpReleaseContentFactory} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpReleaseContentFactory} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpReleaseContentFactory}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：创建 {@code McpReleaseContentFactory} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpReleaseContentFactory} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param servers 参数 servers；parameter servers。
     * @param capabilities 参数 capabilities；parameter capabilities。
     * @param managedOverrides 参数 managedOverrides；parameter managed overrides。
     * @param remoteTools 参数 远程Tools；parameter remote tools。
     * @param remote 参数 远程；parameter remote。
     * @param artifacts 参数 artifacts；parameter artifacts。
     * @param drafts 参数 drafts；parameter drafts。
     * @param catalog 参数 目录；parameter catalog。
     * @param validation 参数 validation；parameter validation。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     */
    public McpReleaseContentFactory(
            McpServerRepository servers,
            JdbcMcpCapabilityDraftStore capabilities,
            JdbcMcpManagedToolOverrideStore managedOverrides,
            JdbcMcpRemoteToolDraftStore remoteTools,
            JdbcMcpRemoteProviderStore remote,
            JdbcMcpArtifactMetadataStore artifacts,
            GatewayDraftRepository drafts,
            GatewayCatalogStore catalog,
            McpValidationService validation,
            ObjectMapper objectMapper) {
        this.servers = servers;
        this.capabilities = capabilities;
        this.managedOverrides = managedOverrides;
        this.remoteTools = remoteTools;
        this.remote = remote;
        this.artifacts = artifacts;
        this.drafts = drafts;
        this.catalog = catalog;
        this.validation = validation;
        this.objectMapper = objectMapper.copy();
    }

    /**
     * 中文说明：执行 compileFor发布 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the compile for release operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.compileForRelease(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param expectedDraftRevision 参数 expected草稿Revision；parameter expected draft revision。
     * @return 返回 compileFor发布 的处理结果；returns the result of the operation.
     */
    @Transactional(readOnly = true)
    public McpRuleContent compileForRelease(
            String gatewayGroupId,
            long expectedDraftRevision) {
        drafts.findById(gatewayGroupId)
                .orElseThrow(() -> new GatewayAdminNotFoundException(
                        "gateway draft " + gatewayGroupId + " was not found"
                ))
                .assertEditable(expectedDraftRevision);
        McpRuleContent content = create(gatewayGroupId);
        validation.requireValid(content);
        return content;
    }

    /**
     * 中文说明：执行 preview 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the preview operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.preview(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 preview 的处理结果；returns the result of the operation.
     */
    @Transactional(readOnly = true)
    public McpRuleContent preview(String gatewayGroupId) {
        return create(gatewayGroupId);
    }

    /**
     * 中文说明：执行 managedTools 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the managed tools operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.managedTools(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 managedTools 的处理结果；returns the result of the operation.
     */
    @Transactional(readOnly = true)
    public List<ManagedToolProjection> managedTools(String gatewayGroupId) {
        List<McpServerEntity> serverEntities = servers
                .findAllByGatewayGroupIdAndDeletedFalseOrderByServerCode(
                        gatewayGroupId
                );
        return managedTools(gatewayGroupId, serverEntities);
    }

    /**
     * 中文说明：执行 create 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.create(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 create 的处理结果；returns the result of the operation.
     */
    private McpRuleContent create(String gatewayGroupId) {
        List<McpServerEntity> serverEntities = servers
                .findAllByGatewayGroupIdAndDeletedFalseOrderByServerCode(
                        gatewayGroupId
                );
        Map<String, McpServerEntity> serverById = serverEntities.stream()
                .collect(Collectors.toUnmodifiableMap(
                        McpServerEntity::getId,
                        Function.identity()
                ));
        var draft = capabilities.load(gatewayGroupId);
        List<JdbcMcpRemoteProviderStore.RemoteProviderDraft> providers =
                remote.providers(gatewayGroupId);
        Map<String, JdbcMcpRemoteProviderStore.RemoteProviderDraft>
                providerById = providers.stream().collect(
                java.util.stream.Collectors.toUnmodifiableMap(
                        JdbcMcpRemoteProviderStore.RemoteProviderDraft::id,
                        java.util.function.Function.identity()
                )
        );

        return new McpRuleContent(
                serverEntities.stream().map(this::server).toList(),
                tools(gatewayGroupId, serverEntities, serverById),
                draft.capabilities(
                        JdbcMcpCapabilityDraftStore.CapabilityKind.RESOURCE
                ).stream().map(item -> resource(item, serverById)).toList(),
                draft.capabilities(
                        JdbcMcpCapabilityDraftStore.CapabilityKind
                                .RESOURCE_TEMPLATE
                ).stream().map(item -> template(item, serverById)).toList(),
                draft.capabilities(
                        JdbcMcpCapabilityDraftStore.CapabilityKind.PROMPT
                ).stream().map(item -> prompt(item, serverById)).toList(),
                draft.capabilities(
                        JdbcMcpCapabilityDraftStore.CapabilityKind.TASK_POLICY
                ).stream().map(item -> taskPolicy(item, serverById)).toList(),
                draft.capabilities(
                        JdbcMcpCapabilityDraftStore.CapabilityKind.APP_BINDING
                ).stream().map(item -> app(item, serverById)).toList(),
                providers.stream().map(this::provider).toList(),
                remote.mounts(gatewayGroupId).stream()
                        .map(item -> mount(
                                item,
                                serverById,
                                providerById
                        ))
                        .toList()
        );
    }

    /**
     * 中文说明：执行 tools 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the tools operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.tools(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param serverEntities 参数 服务器Entities；parameter server entities。
     * @param serverById 参数 服务器ById；parameter server by id。
     * @return 返回 tools 的处理结果；returns the result of the operation.
     */
    private List<McpRuntimeTool> tools(
            String gatewayGroupId,
            List<McpServerEntity> serverEntities,
            Map<String, McpServerEntity> serverById) {
        List<McpRuntimeTool> result = new ArrayList<>();
        managedTools(gatewayGroupId, serverEntities).stream()
                .map(ManagedToolProjection::tool)
                .forEach(result::add);
        remoteTools.load(gatewayGroupId).stream()
                .map(item -> remoteTool(item, serverById))
                .forEach(result::add);
        return List.copyOf(result);
    }

    /**
     * 中文说明：执行 managedTools 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the managed tools operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.managedTools(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param serverEntities 参数 服务器Entities；parameter server entities。
     * @return 返回 managedTools 的处理结果；returns the result of the operation.
     */
    private List<ManagedToolProjection> managedTools(
            String gatewayGroupId,
            List<McpServerEntity> serverEntities) {
        Map<String, McpServerEntity> serverById = serverEntities.stream()
                .collect(Collectors.toUnmodifiableMap(
                        McpServerEntity::getId,
                        Function.identity()
                ));
        Map<String, McpServerEntity> serverByCode = serverEntities.stream()
                .collect(Collectors.toUnmodifiableMap(
                        McpServerEntity::getServerCode,
                        Function.identity()
                ));
        Map<String, JdbcMcpManagedToolOverrideStore.ManagedToolOverride>
                overrideByOperationId = managedOverrides.load(gatewayGroupId)
                .stream()
                .collect(Collectors.toUnmodifiableMap(
                        JdbcMcpManagedToolOverrideStore.ManagedToolOverride
                                ::operationId,
                        Function.identity()
                ));
        List<ManagedToolProjection> result = new ArrayList<>();
        for (GatewayCatalogStore.CurrentOperationDefinition current
                : catalog.loadCurrentOperationDefinitions(gatewayGroupId)) {
            if (!"STARTER".equals(current.operation().sourceType())) {
                continue;
            }
            Map<String, Object> exposure = objectMap(
                    current.definition().attributes().get("mcpExposure")
            );
            if (!bool(exposure, "registerMcp", false)) {
                continue;
            }
            result.add(managedTool(
                    gatewayGroupId,
                    current,
                    exposure,
                    serverById,
                    serverByCode,
                    overrideByOperationId
            ));
        }
        return List.copyOf(result);
    }

    /**
     * 中文说明：执行 managed工具 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the managed tool operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.managedTool(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param current 参数 current；parameter current。
     * @param exposure 参数 exposure；parameter exposure。
     * @param serverById 参数 服务器ById；parameter server by id。
     * @param serverByCode 参数 服务器ByCode；parameter server by code。
     * @param overrideByOperationId 参数 overrideBy操作Id；parameter override by operation id。
     * @return 返回 managed工具 的处理结果；returns the result of the operation.
     */
    private ManagedToolProjection managedTool(
            String gatewayGroupId,
            GatewayCatalogStore.CurrentOperationDefinition current,
            Map<String, Object> exposure,
            Map<String, McpServerEntity> serverById,
            Map<String, McpServerEntity> serverByCode,
            Map<String, JdbcMcpManagedToolOverrideStore.ManagedToolOverride>
                    overrideByOperationId) {
        GatewayCatalogStore.OperationRecord operation = current.operation();
        GatewayCatalogStore.OperationDefinition definition =
                current.definition();
        String codeServerCode = required(exposure, "mcpServerCode");
        McpServerEntity codeServer = serverByCode.get(codeServerCode);
        if (codeServer == null) {
            throw new McpValidationException(
                    "GATEWAY_MCP_SERVER_NOT_FOUND",
                    "operations." + operation.operationKey()
                            + ".mcpExposure.mcpServerCode",
                    "MCP Server " + codeServerCode + " was not found"
            );
        }
        String toolId = managedToolId(codeServerCode, operation.operationKey());
        JdbcMcpManagedToolOverrideStore.ManagedToolOverride override =
                overrideByOperationId.get(operation.id());
        McpServerEntity effectiveServer = override == null
                || override.serverId() == null
                ? codeServer
                : serverById.get(override.serverId());
        if (effectiveServer == null) {
            throw new McpValidationException(
                    "GATEWAY_MCP_SERVER_NOT_FOUND",
                    "managedTools." + toolId + ".serverId",
                    "override MCP Server was not found"
            );
        }
        Set<String> codePermissions = strings(
                exposure.get("requiredPermissions")
        );
        Set<String> additionalPermissions = override == null
                ? Set.of()
                : override.additionalPermissions();
        LinkedHashSet<String> effectivePermissions = new LinkedHashSet<>(
                codePermissions
        );
        effectivePermissions.addAll(additionalPermissions);
        String codeRisk = text(exposure, "riskLevel", "LOW");
        String minimumRisk = override == null
                ? null
                : override.minimumRiskLevel();
        String effectiveRisk = maximumRisk(codeRisk, minimumRisk);
        if (!Set.of("HTTP", "RPC").contains(operation.protocol())) {
            throw new McpValidationException(
                    "GATEWAY_MCP_OPERATION_PROTOCOL_UNSUPPORTED",
                    "operations." + operation.operationKey() + ".protocol",
                    "managed MCP Tool requires HTTP or RPC Operation"
            );
        }
        if ("HTTP".equals(operation.protocol())
                && bool(definition.attributes(), "streaming", false)) {
            throw new McpValidationException(
                    "GATEWAY_MCP_STREAMING_UNSUPPORTED",
                    "operations." + operation.operationKey() + ".streaming",
                    "streaming Operation cannot be projected as an MCP Tool"
            );
        }
        boolean enabled = override == null || override.enabled() == null;
        McpRuntimeTool tool = new McpRuntimeTool(
                toolId,
                effectiveServer.getServerCode(),
                required(exposure, "mcpName"),
                description(definition),
                "LOCAL_OPERATION",
                operation.id(),
                operation.protocol(),
                null,
                inputSchema(operation.protocol(), definition.requestSchema()),
                schema(definition.responseSchema()),
                Map.of(),
                Set.copyOf(effectivePermissions),
                effectiveRisk,
                bool(exposure, "idempotent", false),
                enabled
        );
        return new ManagedToolProjection(
                gatewayGroupId,
                operation.operationKey(),
                codeServer.getId(),
                codeServer.getServerCode(),
                effectiveServer.getId(),
                codePermissions,
                additionalPermissions,
                codeRisk,
                minimumRisk,
                override == null ? 0 : override.revision(),
                tool
        );
    }

    /**
     * 中文说明：执行 服务器 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the server operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.server(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param server 参数 服务器；parameter server。
     * @return 返回 服务器 的处理结果；returns the result of the operation.
     */
    private McpRuntimeServer server(McpServerEntity server) {
        return new McpRuntimeServer(
                server.getId(),
                server.getServerCode(),
                server.getDisplayName(),
                server.getDescription(),
                server.getInstructions(),
                server.getDialects().stream()
                        .map(McpProtocolDialect::valueOf)
                        .collect(java.util.stream.Collectors.toSet()),
                server.getResourceUri(),
                server.getListCacheTtlSeconds(),
                server.isEnabled()
        );
    }

    /**
     * 中文说明：执行 远程工具 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the remote tool operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.remoteTool(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param draft 参数 草稿；parameter draft。
     * @param serverById 参数 服务器ById；parameter server by id。
     * @return 返回 远程工具 的处理结果；returns the result of the operation.
     */
    private McpRuntimeTool remoteTool(
            JdbcMcpRemoteToolDraftStore.RemoteToolDraft draft,
            Map<String, McpServerEntity> serverById) {
        Map<String, Object> value = draft.content();
        return new McpRuntimeTool(
                draft.id(),
                serverCode(draft.serverId(), serverById),
                draft.name(),
                optional(value, "description"),
                "REMOTE_MCP",
                null,
                null,
                draft.remoteMountId(),
                schema(value.get("inputSchema")),
                schema(value.get("outputSchema")),
                stringMap(value.get("annotations")),
                strings(value.get("requiredPermissions")),
                text(value, "riskLevel", "LOW"),
                bool(value, "idempotent", false),
                draft.enabled()
        );
    }

    /**
     * 中文说明：执行 资源 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the resource operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.resource(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param draft 参数 草稿；parameter draft。
     * @param serverById 参数 服务器ById；parameter server by id。
     * @return 返回 资源 的处理结果；returns the result of the operation.
     */
    private McpRuntimeResource resource(
            JdbcMcpCapabilityDraftStore.CapabilityDraft draft,
            Map<String, McpServerEntity> serverById) {
        Map<String, Object> value = draft.content();
        return new McpRuntimeResource(
                draft.id(),
                serverCode(draft.serverId(), serverById),
                draft.name(),
                required(value, "uri"),
                optional(value, "description"),
                text(value, "mimeType", "application/json"),
                required(value, "driverType"),
                optional(value, "operationId"),
                optional(value, "remoteMountId"),
                stringMap(value.get("configuration")),
                strings(value.get("requiredPermissions")),
                number(value, "maxBytes", 67_108_864L),
                draft.enabled()
        );
    }

    /**
     * 中文说明：执行 模板 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the template operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.template(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param draft 参数 草稿；parameter draft。
     * @param serverById 参数 服务器ById；parameter server by id。
     * @return 返回 模板 的处理结果；returns the result of the operation.
     */
    private McpRuntimeResourceTemplate template(
            JdbcMcpCapabilityDraftStore.CapabilityDraft draft,
            Map<String, McpServerEntity> serverById) {
        Map<String, Object> value = draft.content();
        return new McpRuntimeResourceTemplate(
                draft.id(),
                serverCode(draft.serverId(), serverById),
                draft.name(),
                required(value, "uriTemplate"),
                optional(value, "description"),
                text(value, "mimeType", "application/json"),
                required(value, "driverType"),
                optional(value, "operationId"),
                optional(value, "remoteMountId"),
                stringMap(value.get("configuration")),
                strings(value.get("requiredPermissions")),
                number(value, "maxBytes", 67_108_864L),
                draft.enabled()
        );
    }

    /**
     * 中文说明：执行 提示词 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the prompt operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.prompt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param draft 参数 草稿；parameter draft。
     * @param serverById 参数 服务器ById；parameter server by id。
     * @return 返回 提示词 的处理结果；returns the result of the operation.
     */
    private McpRuntimePrompt prompt(
            JdbcMcpCapabilityDraftStore.CapabilityDraft draft,
            Map<String, McpServerEntity> serverById) {
        Map<String, Object> value = draft.content();
        return new McpRuntimePrompt(
                draft.id(),
                serverCode(draft.serverId(), serverById),
                draft.name(),
                optional(value, "description"),
                required(value, "sourceType"),
                optional(value, "template"),
                optional(value, "operationId"),
                optional(value, "remoteMountId"),
                List.copyOf(strings(value.get("arguments"))),
                strings(value.get("requiredPermissions")),
                draft.enabled()
        );
    }

    /**
     * 中文说明：执行 任务策略 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the task policy operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.taskPolicy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param draft 参数 草稿；parameter draft。
     * @param serverById 参数 服务器ById；parameter server by id。
     * @return 返回 任务策略 的处理结果；returns the result of the operation.
     */
    private McpRuntimeTaskPolicy taskPolicy(
            JdbcMcpCapabilityDraftStore.CapabilityDraft draft,
            Map<String, McpServerEntity> serverById) {
        Map<String, Object> value = draft.content();
        return new McpRuntimeTaskPolicy(
                draft.id(),
                serverCode(draft.serverId(), serverById),
                draft.name(),
                bool(value, "durable", true),
                bool(value, "inputAllowed", false),
                number(value, "executionTimeoutSeconds", 60),
                number(value, "resultTtlSeconds", 86_400),
                Math.toIntExact(number(value, "maxAttempts", 3)),
                draft.enabled()
        );
    }

    /**
     * 中文说明：执行 app 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the app operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.app(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param draft 参数 草稿；parameter draft。
     * @param serverById 参数 服务器ById；parameter server by id。
     * @return 返回 app 的处理结果；returns the result of the operation.
     */
    private McpRuntimeApp app(
            JdbcMcpCapabilityDraftStore.CapabilityDraft draft,
            Map<String, McpServerEntity> serverById) {
        Map<String, Object> value = draft.content();
        String artifactId = required(value, "appArtifactId");
        var artifact = artifacts.find(artifactId)
                .orElseThrow(() -> new McpValidationException(
                        "GATEWAY_MCP_ARTIFACT_NOT_FOUND",
                        "apps." + draft.name() + ".appArtifactId",
                        "MCP App artifact was not found"
                ));
        return new McpRuntimeApp(
                draft.id(),
                serverCode(draft.serverId(), serverById),
                draft.name(),
                artifact.appCode(),
                artifact.version(),
                artifact.resourceUri(),
                artifact.id(),
                artifact.artifactReference(),
                artifact.sha256(),
                artifact.sizeBytes(),
                artifact.mimeType(),
                artifact.contentSecurityPolicy(),
                artifact.permissions(),
                artifact.allowedOrigins(),
                strings(value.get("allowedTools")),
                draft.enabled()
        );
    }

    /**
     * 中文说明：执行 提供方 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the provider operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.provider(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param draft 参数 草稿；parameter draft。
     * @return 返回 提供方 的处理结果；returns the result of the operation.
     */
    private McpRuntimeRemoteProvider provider(
            JdbcMcpRemoteProviderStore.RemoteProviderDraft draft) {
        Map<String, Object> value = draft.content();
        return new McpRuntimeRemoteProvider(
                draft.id(),
                draft.providerCode(),
                required(value, "displayName"),
                McpProtocolDialect.valueOf(required(value, "dialect")),
                required(value, "transportType"),
                required(value, "endpointReference"),
                optional(value, "authProfileReference"),
                optional(value, "tlsProfileReference"),
                required(value, "capabilityFingerprint"),
                draft.enabled()
        );
    }

    /**
     * 中文说明：执行 mount 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the mount operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.mount(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param draft 参数 草稿；parameter draft。
     * @param serverById 参数 服务器ById；parameter server by id。
     * @param providerById 参数 提供方ById；parameter provider by id。
     * @return 返回 mount 的处理结果；returns the result of the operation.
     */
    private McpRuntimeRemoteMount mount(
            JdbcMcpRemoteProviderStore.RemoteMountDraft draft,
            Map<String, McpServerEntity> serverById,
            Map<String, JdbcMcpRemoteProviderStore.RemoteProviderDraft>
                    providerById) {
        var provider = providerById.get(draft.providerId());
        if (provider == null) {
            throw new McpValidationException(
                    "GATEWAY_MCP_REMOTE_PROVIDER_NOT_FOUND",
                    "remoteMounts." + draft.id() + ".providerId",
                    "remote MCP Provider was not found"
            );
        }
        Map<String, Object> value = draft.content();
        return new McpRuntimeRemoteMount(
                draft.id(),
                serverCode(draft.serverId(), serverById),
                provider.providerCode(),
                draft.namespace(),
                strings(value.getOrDefault(
                        "primitiveTypes",
                        List.of(
                                "TOOL",
                                "RESOURCE",
                                "RESOURCE_TEMPLATE",
                                "PROMPT",
                                "APP"
                        )
                )),
                stringMap(value.get("renameRules")),
                text(value, "conflictPolicy", "REJECT"),
                strings(value.get("requiredPermissions")),
                draft.capabilityFingerprint(),
                draft.enabled()
        );
    }

    /**
     * 中文说明：执行 服务器Code 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the server code operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.serverCode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serverId 参数 服务器Id；parameter server id。
     * @param serverById 参数 服务器ById；parameter server by id。
     * @return 返回 服务器Code 的处理结果；returns the result of the operation.
     */
    private String serverCode(
            String serverId,
            Map<String, McpServerEntity> serverById) {
        McpServerEntity server = serverById.get(serverId);
        if (server == null) {
            throw new McpValidationException(
                    "GATEWAY_MCP_SERVER_NOT_FOUND",
                    "serverId",
                    "MCP Server " + serverId + " was not found"
            );
        }
        return server.getServerCode();
    }

    /**
     * 中文说明：执行 description 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the description operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.description(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param definition 参数 定义；parameter definition。
     * @return 返回 description 的处理结果；returns the result of the operation.
     */
    private String description(
            GatewayCatalogStore.OperationDefinition definition) {
        String description = optional(definition.attributes(), "description");
        return description == null ? definition.summary() : description;
    }

    /**
     * 中文说明：执行 input模式 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the input schema operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.inputSchema(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param protocol 参数 protocol；parameter protocol。
     * @param requestSchema 参数 请求模式；parameter request schema。
     * @return 返回 input模式 的处理结果；returns the result of the operation.
     */
    private String inputSchema(
            String protocol,
            Map<String, Object> requestSchema) {
        if ("HTTP".equals(protocol)) {
            return schema(httpInputSchema(requestSchema));
        }
        return schema(requestSchema);
    }

    /**
     * 中文说明：执行 httpInput模式 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the http input schema operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.httpInputSchema(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param requestSchema 参数 请求模式；parameter request schema。
     * @return 返回 httpInput模式 的处理结果；returns the result of the operation.
     */
    private Map<String, Object> httpInputSchema(
            Map<String, Object> requestSchema) {
        if (requestSchema == null) {
            return null;
        }
        Map<String, Object> projected = new LinkedHashMap<>();
        requestSchema.forEach((key, value) -> {
            if (!"properties".equals(key) && !"required".equals(key)) {
                projected.put(key, value);
            }
        });
        Map<String, Object> properties = objectMap(
                requestSchema.get("properties")
        );
        Map<String, Object> exposedProperties = new LinkedHashMap<>();
        for (String location : List.of("path", "query", "body")) {
            if (properties.containsKey(location)) {
                exposedProperties.put(location, properties.get(location));
            }
        }
        projected.put("properties", exposedProperties);
        Object requiredValue = requestSchema.get("required");
        if (requiredValue instanceof Collection<?> required) {
            List<String> exposedRequired = required.stream()
                    .map(String::valueOf)
                    .filter(List.of("path", "query", "body")::contains)
                    .toList();
            if (!exposedRequired.isEmpty()) {
                projected.put("required", exposedRequired);
            }
        }
        return Map.copyOf(projected);
    }

    /**
     * 中文说明：执行 managed工具Id 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the managed tool id operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.managedToolId(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param operationKey 参数 操作键；parameter operation key。
     * @return 返回 managed工具Id 的处理结果；returns the result of the operation.
     */
    static String managedToolId(String serverCode, String operationKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(serverCode.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(operationKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    /**
     * 中文说明：执行 maximumRisk 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the maximum risk operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.maximumRisk(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param codeRisk 参数 codeRisk；parameter code risk。
     * @param minimumRisk 参数 minimumRisk；parameter minimum risk。
     * @return 返回 maximumRisk 的处理结果；returns the result of the operation.
     */
    private String maximumRisk(String codeRisk, String minimumRisk) {
        int code = risk(codeRisk);
        if (minimumRisk == null) {
            return codeRisk;
        }
        int minimum = risk(minimumRisk);
        return code >= minimum ? codeRisk : minimumRisk;
    }

    /**
     * 中文说明：执行 risk 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the risk operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.risk(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 risk 的处理结果；returns the result of the operation.
     */
    private int risk(String value) {
        int level = RISK_LEVELS.indexOf(value);
        if (level < 0) {
            throw new McpValidationException(
                    "GATEWAY_MCP_RISK_INVALID",
                    "riskLevel",
                    "unsupported MCP Tool risk level " + value
            );
        }
        return level;
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param key 参数 键；parameter key。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private String required(Map<String, Object> value, String key) {
        String result = optional(value, key);
        if (result == null) {
            throw new IllegalArgumentException(key + " is required");
        }
        return result;
    }

    /**
     * 中文说明：执行 optional 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the optional operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.optional(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param key 参数 键；parameter key。
     * @return 返回 optional 的处理结果；returns the result of the operation.
     */
    private String optional(Map<String, Object> value, String key) {
        Object result = value.get(key);
        return result == null || result.toString().isBlank()
                ? null
                : result.toString().trim();
    }

    /**
     * 中文说明：执行 text 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the text operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.text(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param key 参数 键；parameter key。
     * @param defaultValue 参数 default值；parameter default value。
     * @return 返回 text 的处理结果；returns the result of the operation.
     */
    private String text(
            Map<String, Object> value,
            String key,
            String defaultValue) {
        String result = optional(value, key);
        return result == null ? defaultValue : result;
    }

    /**
     * 中文说明：执行 bool 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the bool operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.bool(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param key 参数 键；parameter key。
     * @param defaultValue 参数 default值；parameter default value。
     * @return 返回 bool 的处理结果；returns the result of the operation.
     */
    private boolean bool(
            Map<String, Object> value,
            String key,
            boolean defaultValue) {
        Object result = value.get(key);
        return result == null
                ? defaultValue
                : result instanceof Boolean bool
                ? bool
                : Boolean.parseBoolean(result.toString());
    }

    /**
     * 中文说明：执行 number 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the number operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.number(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param key 参数 键；parameter key。
     * @param defaultValue 参数 default值；parameter default value。
     * @return 返回 number 的处理结果；returns the result of the operation.
     */
    private long number(
            Map<String, Object> value,
            String key,
            long defaultValue) {
        Object result = value.get(key);
        if (result == null) {
            return defaultValue;
        }
        return result instanceof Number number
                ? number.longValue()
                : Long.parseLong(result.toString());
    }

    /**
     * 中文说明：执行 strings 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the strings operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.strings(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 strings 的处理结果；returns the result of the operation.
     */
    private Set<String> strings(Object value) {
        if (value == null) {
            return Set.of();
        }
        Collection<?> source = value instanceof Collection<?> collection
                ? collection
                : List.of(value);
        LinkedHashSet<String> result = new LinkedHashSet<>();
        source.forEach(item -> result.add(item.toString().trim()));
        return Set.copyOf(result);
    }

    /**
     * 中文说明：执行 stringMap 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the string map operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.stringMap(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 stringMap 的处理结果；returns the result of the operation.
     */
    private Map<String, String> stringMap(Object value) {
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> source)) {
            throw new IllegalArgumentException("MCP mapping must be an object");
        }
        Map<String, String> result = new LinkedHashMap<>();
        source.forEach((key, item) -> result.put(
                key.toString(),
                Objects.toString(item, "")
        ));
        return Map.copyOf(result);
    }

    /**
     * 中文说明：执行 objectMap 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the object map operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.objectMap(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 objectMap 的处理结果；returns the result of the operation.
     */
    private Map<String, Object> objectMap(Object value) {
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> source)) {
            throw new IllegalArgumentException("MCP value must be an object");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, item) -> result.put(key.toString(), item));
        return Map.copyOf(result);
    }

    /**
     * 中文说明：执行 模式 操作；该方法是 {@code McpReleaseContentFactory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the schema operation; this method is the invocation entry point on {@code McpReleaseContentFactory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpReleaseContentFactory.schema(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 模式 的处理结果；returns the result of the operation.
     */
    private String schema(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String string) {
            return string;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException(
                    "MCP JSON Schema cannot be serialized",
                    failure
            );
        }
    }

    /**
     * 中文说明：{@code ManagedToolProjection} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Managed工具投影相关的职责与边界。
     * English summary: {@code ManagedToolProjection} is an immutable data carrier in the current Gateway module; it owns the managed tool projection-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param operationKey 参数 操作键；parameter operation key。
     * @param codeServerId 参数 code服务器Id；parameter code server id。
     * @param codeServerCode 参数 code服务器Code；parameter code server code。
     * @param serverId 参数 服务器Id；parameter server id。
     * @param codePermissions 参数 codePermissions；parameter code permissions。
     * @param additionalPermissions 参数 additionalPermissions；parameter additional permissions。
     * @param codeRiskLevel 参数 codeRiskLevel；parameter code risk level。
     * @param minimumRiskLevel 参数 minimumRiskLevel；parameter minimum risk level。
     * @param overrideRevision 参数 overrideRevision；parameter override revision。
     * @param tool 参数 工具；parameter tool。
     */
    public record ManagedToolProjection(
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpReleaseContentFactory.ManagedToolProjection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code McpReleaseContentFactory.ManagedToolProjection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpReleaseContentFactory.ManagedToolProjection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpReleaseContentFactory.ManagedToolProjection}; do not couple callers to its representation when the owning type exposes an API.
             */
            String gatewayGroupId,
            /**
             * 中文说明：保存 操作键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpReleaseContentFactory.ManagedToolProjection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by operation key; its type is {@code String}, and {@code McpReleaseContentFactory.ManagedToolProjection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpReleaseContentFactory.ManagedToolProjection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpReleaseContentFactory.ManagedToolProjection}; do not couple callers to its representation when the owning type exposes an API.
             */
            String operationKey,
            /**
             * 中文说明：保存 code服务器Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpReleaseContentFactory.ManagedToolProjection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by code server id; its type is {@code String}, and {@code McpReleaseContentFactory.ManagedToolProjection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpReleaseContentFactory.ManagedToolProjection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpReleaseContentFactory.ManagedToolProjection}; do not couple callers to its representation when the owning type exposes an API.
             */
            String codeServerId,
            /**
             * 中文说明：保存 code服务器Code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpReleaseContentFactory.ManagedToolProjection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by code server code; its type is {@code String}, and {@code McpReleaseContentFactory.ManagedToolProjection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpReleaseContentFactory.ManagedToolProjection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpReleaseContentFactory.ManagedToolProjection}; do not couple callers to its representation when the owning type exposes an API.
             */
            String codeServerCode,
            /**
             * 中文说明：保存 服务器Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpReleaseContentFactory.ManagedToolProjection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server id; its type is {@code String}, and {@code McpReleaseContentFactory.ManagedToolProjection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpReleaseContentFactory.ManagedToolProjection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpReleaseContentFactory.ManagedToolProjection}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serverId,
            /**
             * 中文说明：保存 codePermissions 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code McpReleaseContentFactory.ManagedToolProjection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by code permissions; its type is {@code Set<String>}, and {@code McpReleaseContentFactory.ManagedToolProjection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpReleaseContentFactory.ManagedToolProjection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpReleaseContentFactory.ManagedToolProjection}; do not couple callers to its representation when the owning type exposes an API.
             */
            Set<String> codePermissions,
            /**
             * 中文说明：保存 additionalPermissions 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code McpReleaseContentFactory.ManagedToolProjection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by additional permissions; its type is {@code Set<String>}, and {@code McpReleaseContentFactory.ManagedToolProjection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpReleaseContentFactory.ManagedToolProjection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpReleaseContentFactory.ManagedToolProjection}; do not couple callers to its representation when the owning type exposes an API.
             */
            Set<String> additionalPermissions,
            /**
             * 中文说明：保存 codeRiskLevel 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpReleaseContentFactory.ManagedToolProjection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by code risk level; its type is {@code String}, and {@code McpReleaseContentFactory.ManagedToolProjection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpReleaseContentFactory.ManagedToolProjection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpReleaseContentFactory.ManagedToolProjection}; do not couple callers to its representation when the owning type exposes an API.
             */
            String codeRiskLevel,
            /**
             * 中文说明：保存 minimumRiskLevel 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpReleaseContentFactory.ManagedToolProjection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by minimum risk level; its type is {@code String}, and {@code McpReleaseContentFactory.ManagedToolProjection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpReleaseContentFactory.ManagedToolProjection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpReleaseContentFactory.ManagedToolProjection}; do not couple callers to its representation when the owning type exposes an API.
             */
            String minimumRiskLevel,
            /**
             * 中文说明：保存 overrideRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpReleaseContentFactory.ManagedToolProjection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by override revision; its type is {@code long}, and {@code McpReleaseContentFactory.ManagedToolProjection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpReleaseContentFactory.ManagedToolProjection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpReleaseContentFactory.ManagedToolProjection}; do not couple callers to its representation when the owning type exposes an API.
             */
            long overrideRevision,
            /**
             * 中文说明：保存 工具 对应的状态、依赖或配置值；字段类型为 {@code McpRuntimeTool}，由 {@code McpReleaseContentFactory.ManagedToolProjection} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by tool; its type is {@code McpRuntimeTool}, and {@code McpReleaseContentFactory.ManagedToolProjection} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpReleaseContentFactory.ManagedToolProjection} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpReleaseContentFactory.ManagedToolProjection}; do not couple callers to its representation when the owning type exposes an API.
             */
            McpRuntimeTool tool
    ) {
    }

}
