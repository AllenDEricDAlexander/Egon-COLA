package top.egon.cola.component.gateway.admin.application.catalog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.admin.application.GatewayAdminNotFoundException;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogRepository;
import top.egon.cola.component.gateway.admin.rule.GatewayRuleCanonicalizer;
import top.egon.cola.component.gateway.contract.identity.GatewayOperationKey;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 中文说明：{@code GatewayCatalogService} 是服务组件，位于当前 Gateway 模块的相关包中，负责网关目录服务相关的职责与边界。
 * English summary: {@code GatewayCatalogService} is a gateway catalog service service in the current Gateway module; it owns the gateway catalog service-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Service
public class GatewayCatalogService {

    /**
     * 中文说明：保存 存储 对应的状态、依赖或配置值；字段类型为 {@code GatewayCatalogStore}，由 {@code GatewayCatalogService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by store; its type is {@code GatewayCatalogStore}, and {@code GatewayCatalogService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCatalogService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayCatalogStore store;

    /**
     * 中文说明：保存 audits 对应的状态、依赖或配置值；字段类型为 {@code GatewayAuditLogRepository}，由 {@code GatewayCatalogService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by audits; its type is {@code GatewayAuditLogRepository}, and {@code GatewayCatalogService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCatalogService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayAuditLogRepository audits;

    /**
     * 中文说明：保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code GatewayCatalogService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code GatewayCatalogService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCatalogService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code GatewayCatalogService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code GatewayCatalogService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCatalogService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：保存 idGenerator 对应的状态、依赖或配置值；字段类型为 {@code LongIdGenerator}，由 {@code GatewayCatalogService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by id generator; its type is {@code LongIdGenerator}, and {@code GatewayCatalogService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCatalogService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final LongIdGenerator idGenerator;

    /**
     * 中文说明：创建 {@code GatewayCatalogService} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayCatalogService} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param store 参数 存储；parameter store。
     * @param audits 参数 audits；parameter audits。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param idGenerator 参数 idGenerator；parameter id generator。
     */
    @Autowired
    public GatewayCatalogService(
            GatewayCatalogStore store,
            GatewayAuditLogRepository audits,
            ObjectMapper objectMapper,
            LongIdGenerator idGenerator) {
        this(
                store,
                audits,
                objectMapper,
                Clock.systemUTC(),
                idGenerator
        );
    }

    /**
     * 中文说明：创建 {@code GatewayCatalogService} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayCatalogService} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param store 参数 存储；parameter store。
     * @param audits 参数 audits；parameter audits。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param clock 参数 clock；parameter clock。
     * @param idGenerator 参数 idGenerator；parameter id generator。
     */
    GatewayCatalogService(
            GatewayCatalogStore store,
            GatewayAuditLogRepository audits,
            ObjectMapper objectMapper,
            Clock clock,
            LongIdGenerator idGenerator) {
        this.store = store;
        this.audits = audits;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.idGenerator = idGenerator;
    }

    /**
     * 中文说明：执行 目录 操作；该方法是 {@code GatewayCatalogService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the catalog operation; this method is the invocation entry point on {@code GatewayCatalogService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogService.catalog(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @return 返回 目录 的处理结果；returns the result of the operation.
     */
    @Transactional(readOnly = true)
    public GatewayCatalogStore.CatalogTree catalog(String applicationId) {
        return store.loadCatalog(applicationId);
    }

    /**
     * 中文说明：执行 createManual接口Group 操作；该方法是 {@code GatewayCatalogService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create manual interface group operation; this method is the invocation entry point on {@code GatewayCatalogService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogService.createManualInterfaceGroup(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @param hierarchy 参数 hierarchy；parameter hierarchy。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 createManual接口Group 的处理结果；returns the result of the operation.
     */
    @Transactional
    public String createManualInterfaceGroup(
            String applicationId,
            GatewayCatalogStore.ManualHierarchy hierarchy,
            AdminActor actor,
            RequestAuditContext request) {
        validateHierarchy(hierarchy);
        String id = store.createManualHierarchy(
                applicationId,
                hierarchy,
                clock.instant()
        );
        audit(actor, request, "INTERFACE_GROUP", id, "CREATE_MANUAL", Map.of(
                "applicationId", applicationId,
                "businessCode", hierarchy.businessCode(),
                "entityCode", hierarchy.entityCode(),
                "interfaceGroupCode", hierarchy.interfaceGroupCode()
        ));
        return id;
    }

    /**
     * 中文说明：执行 createManual操作 操作；该方法是 {@code GatewayCatalogService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create manual operation operation; this method is the invocation entry point on {@code GatewayCatalogService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogService.createManualOperation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param interfaceGroupId 参数 接口GroupId；parameter interface group id。
     * @param command 参数 command；parameter command。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 createManual操作 的处理结果；returns the result of the operation.
     */
    @Transactional
    public OperationDetail createManualOperation(
            String interfaceGroupId,
            ManualOperation command,
            AdminActor actor,
            RequestAuditContext request) {
        GatewayCatalogStore.InterfaceGroupScope scope =
                store.findInterfaceGroup(interfaceGroupId)
                        .orElseThrow(() -> new GatewayAdminNotFoundException(
                                "interface group "
                                        + interfaceGroupId
                                        + " was not found"
                        ));
        String operationKey = operationKey(scope, command);
        store.findOperation(scope.applicationId(), operationKey)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "operation key already exists with source "
                                    + existing.sourceType()
                    );
                });
        Instant now = clock.instant();
        String operationId = idGenerator.nextId();
        GatewayCatalogStore.OperationRecord operation =
                new GatewayCatalogStore.OperationRecord(
                        operationId,
                        scope.applicationId(),
                        interfaceGroupId,
                        operationKey,
                        command.protocol().name(),
                        methodIdentity(command),
                        command.externalAccessible(),
                        providerIdentity(scope, command),
                        "MANUAL",
                        "DISCOVERED",
                        null,
                        0,
                        now,
                        now
                );
        store.insertOperation(operation);
        GatewayCatalogStore.OperationDefinition definition =
                definition(
                        operation,
                        1,
                        command.definition(),
                        actor.actorId(),
                        now
                );
        store.appendDefinition(definition);
        store.pointToDefinition(
                operationId,
                definition.id(),
                definition.externalAccessible(),
                now
        );
        audit(actor, request, "OPERATION", operationId, "CREATE_MANUAL",
                Map.of(
                        "operationKey", operationKey,
                        "externalAccessible",
                        command.externalAccessible(),
                        "definitionSha256",
                        definition.definitionSha256()
                ));
        return detail(operationId);
    }

    /**
     * 中文说明：执行 detail 操作；该方法是 {@code GatewayCatalogService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the detail operation; this method is the invocation entry point on {@code GatewayCatalogService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogService.detail(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operationId 参数 操作Id；parameter operation id。
     * @return 返回 detail 的处理结果；returns the result of the operation.
     */
    @Transactional(readOnly = true)
    public OperationDetail detail(String operationId) {
        GatewayCatalogStore.OperationRecord operation =
                requiredOperation(operationId);
        return new OperationDetail(
                operation,
                store.loadDefinitions(operationId)
        );
    }

    /**
     * 中文说明：执行 updateManual定义 操作；该方法是 {@code GatewayCatalogService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the update manual definition operation; this method is the invocation entry point on {@code GatewayCatalogService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogService.updateManualDefinition(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operationId 参数 操作Id；parameter operation id。
     * @param definition 参数 定义；parameter definition。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 updateManual定义 的处理结果；returns the result of the operation.
     */
    @Transactional
    public OperationDetail updateManualDefinition(
            String operationId,
            ManualDefinition definition,
            AdminActor actor,
            RequestAuditContext request) {
        GatewayCatalogStore.OperationRecord operation =
                requiredManualOperation(operationId);
        List<GatewayCatalogStore.OperationDefinition> history =
                store.loadDefinitions(operationId);
        long nextVersion = history.stream()
                .mapToLong(GatewayCatalogStore.OperationDefinition
                        ::definitionVersion)
                .max()
                .orElse(0) + 1;
        GatewayCatalogStore.OperationDefinition appended = definition(
                operation,
                nextVersion,
                definition,
                actor.actorId(),
                clock.instant()
        );
        if (history.stream().anyMatch(existing -> existing.definitionSha256()
                .equals(appended.definitionSha256()))) {
            return new OperationDetail(operation, history);
        }
        store.appendDefinition(appended);
        store.pointToDefinition(
                operationId,
                appended.id(),
                appended.externalAccessible(),
                appended.createdAt()
        );
        audit(actor, request, "OPERATION", operationId,
                "UPDATE_MANUAL_DEFINITION", Map.of(
                        "definitionVersion", nextVersion,
                        "definitionSha256", appended.definitionSha256(),
                        "externalAccessible",
                        appended.externalAccessible()
                ));
        return detail(operationId);
    }

    /**
     * 中文说明：执行 update元数据 操作；该方法是 {@code GatewayCatalogService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the update metadata operation; this method is the invocation entry point on {@code GatewayCatalogService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogService.updateMetadata(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operationId 参数 操作Id；parameter operation id。
     * @param metadata 参数 元数据；parameter metadata。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 update元数据 的处理结果；returns the result of the operation.
     */
    @Transactional
    public OperationDetail updateMetadata(
            String operationId,
            ManualMetadata metadata,
            AdminActor actor,
            RequestAuditContext request) {
        GatewayCatalogStore.OperationRecord operation =
                requiredManualOperation(operationId);
        List<GatewayCatalogStore.OperationDefinition> history =
                store.loadDefinitions(operationId);
        if (history.isEmpty()) {
            throw new IllegalStateException(
                    "manual operation has no definition"
            );
        }
        GatewayCatalogStore.OperationDefinition current = history.getFirst();
        Map<String, Object> attributes =
                new LinkedHashMap<>(current.attributes());
        if (metadata.owner() == null || metadata.owner().isBlank()) {
            attributes.remove("owner");
        } else {
            attributes.put("owner", metadata.owner().trim());
        }
        return updateManualDefinition(
                operationId,
                new ManualDefinition(
                        metadata.summary(),
                        metadata.tags(),
                        current.requestSchema(),
                        current.responseSchema(),
                        current.errorSchema(),
                        current.descriptorSnapshot(),
                        attributes,
                        current.externalAccessible()
                ),
                actor,
                request
        );
    }

    /**
     * 中文说明：执行 deprecate 操作；该方法是 {@code GatewayCatalogService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the deprecate operation; this method is the invocation entry point on {@code GatewayCatalogService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogService.deprecate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operationId 参数 操作Id；parameter operation id。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 deprecate 的处理结果；returns the result of the operation.
     */
    @Transactional
    public OperationDetail deprecate(
            String operationId,
            AdminActor actor,
            RequestAuditContext request) {
        requiredOperation(operationId);
        store.deprecate(operationId, clock.instant());
        audit(actor, request, "OPERATION", operationId, "DEPRECATE", Map.of(
                "lifecycleStatus", "DEPRECATED"
        ));
        return detail(operationId);
    }

    /**
     * 中文说明：执行 定义 操作；该方法是 {@code GatewayCatalogService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the definition operation; this method is the invocation entry point on {@code GatewayCatalogService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogService.definition(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operation 参数 操作；parameter operation。
     * @param version 参数 version；parameter version。
     * @param value 参数 值；parameter value。
     * @param actorId 参数 actorId；parameter actor id。
     * @param now 参数 now；parameter now。
     * @return 返回 定义 的处理结果；returns the result of the operation.
     */
    private GatewayCatalogStore.OperationDefinition definition(
            GatewayCatalogStore.OperationRecord operation,
            long version,
            ManualDefinition value,
            String actorId,
            Instant now) {
        validateDefinition(operation.protocol(), value);
        Map<String, Object> digestMaterial = new LinkedHashMap<>();
        digestMaterial.put("summary", value.summary());
        digestMaterial.put("tags", value.tags());
        digestMaterial.put("requestSchema", value.requestSchema());
        digestMaterial.put("responseSchema", value.responseSchema());
        digestMaterial.put("errorSchema", value.errorSchema());
        digestMaterial.put("descriptorSnapshot", value.descriptorSnapshot());
        digestMaterial.put("attributes", value.attributes());
        digestMaterial.put(
                "externalAccessible",
                value.externalAccessible()
        );
        return new GatewayCatalogStore.OperationDefinition(
                UuidV7.simpleString(),
                operation.id(),
                version,
                GatewayRuleCanonicalizer.sha256(bytes(digestMaterial)),
                value.summary(),
                List.copyOf(value.tags()),
                Map.copyOf(value.requestSchema()),
                Map.copyOf(value.responseSchema()),
                List.copyOf(value.errorSchema()),
                value.descriptorSnapshot() == null
                        ? null
                        : Map.copyOf(value.descriptorSnapshot()),
                Map.copyOf(value.attributes()),
                value.externalAccessible(),
                now,
                actorId
        );
    }

    /**
     * 中文说明：执行 操作键 操作；该方法是 {@code GatewayCatalogService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the operation key operation; this method is the invocation entry point on {@code GatewayCatalogService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogService.operationKey(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param scope 参数 scope；parameter scope。
     * @param command 参数 command；parameter command。
     * @return 返回 操作键 的处理结果；returns the result of the operation.
     */
    private String operationKey(
            GatewayCatalogStore.InterfaceGroupScope scope,
            ManualOperation command) {
        return switch (command.protocol()) {
            case HTTP -> GatewayOperationKey.http(
                    scope.applicationCode(),
                    required(command.httpMethod(), "httpMethod"),
                    required(command.path(), "path")
            ).value();
            case RPC -> GatewayOperationKey.rpc(
                    scope.applicationCode(),
                    required(command.serviceName(), "serviceName"),
                    defaultValue(command.group(), "default"),
                    defaultValue(command.version(), "1.0.0"),
                    required(command.fullMethodName(), "fullMethodName")
            ).value();
        };
    }

    /**
     * 中文说明：执行 方法身份 操作；该方法是 {@code GatewayCatalogService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the method identity operation; this method is the invocation entry point on {@code GatewayCatalogService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogService.methodIdentity(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param command 参数 command；parameter command。
     * @return 返回 方法身份 的处理结果；returns the result of the operation.
     */
    private String methodIdentity(ManualOperation command) {
        return command.protocol() == Protocol.HTTP
                ? required(command.httpMethod(), "httpMethod").toUpperCase()
                + " " + required(command.path(), "path")
                : required(command.fullMethodName(), "fullMethodName");
    }

    /**
     * 中文说明：执行 提供方身份 操作；该方法是 {@code GatewayCatalogService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the provider identity operation; this method is the invocation entry point on {@code GatewayCatalogService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogService.providerIdentity(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param scope 参数 scope；parameter scope。
     * @param command 参数 command；parameter command。
     * @return 返回 提供方身份 的处理结果；returns the result of the operation.
     */
    private Map<String, Object> providerIdentity(
            GatewayCatalogStore.InterfaceGroupScope scope,
            ManualOperation command) {
        return Map.of(
                "bizCode", scope.bizCode(),
                "appCode", scope.applicationCode(),
                "env", scope.env(),
                "namespace", scope.namespace(),
                "protocol", command.protocol().name(),
                "serviceName",
                required(command.providerServiceName(), "providerServiceName"),
                "group", defaultValue(command.group(), "default"),
                "version", defaultValue(command.version(), "1.0.0"),
                "transport",
                defaultValue(
                        command.transport(),
                        command.protocol() == Protocol.HTTP ? "HTTP" : "GRPC"
                )
        );
    }

    /**
     * 中文说明：执行 required操作 操作；该方法是 {@code GatewayCatalogService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation operation; this method is the invocation entry point on {@code GatewayCatalogService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogService.requiredOperation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 required操作 的处理结果；returns the result of the operation.
     */
    private GatewayCatalogStore.OperationRecord requiredOperation(String id) {
        return store.findOperation(id)
                .orElseThrow(() -> new GatewayAdminNotFoundException(
                        "gateway operation " + id + " was not found"
                ));
    }

    /**
     * 中文说明：执行 requiredManual操作 操作；该方法是 {@code GatewayCatalogService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required manual operation operation; this method is the invocation entry point on {@code GatewayCatalogService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogService.requiredManualOperation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param id 参数 id；parameter id。
     * @return 返回 requiredManual操作 的处理结果；returns the result of the operation.
     */
    private GatewayCatalogStore.OperationRecord requiredManualOperation(
            String id) {
        GatewayCatalogStore.OperationRecord operation =
                requiredOperation(id);
        if (!"MANUAL".equals(operation.sourceType())) {
            throw new IllegalArgumentException(
                    "STARTER operation cannot be modified by manual API"
            );
        }
        return operation;
    }

    /**
     * 中文说明：执行 validateHierarchy 操作；该方法是 {@code GatewayCatalogService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate hierarchy operation; this method is the invocation entry point on {@code GatewayCatalogService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogService.validateHierarchy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     */
    private void validateHierarchy(
            GatewayCatalogStore.ManualHierarchy value) {
        required(value.businessCode(), "businessCode");
        required(value.businessName(), "businessName");
        required(value.entityCode(), "entityCode");
        required(value.entityName(), "entityName");
        required(value.interfaceGroupCode(), "interfaceGroupCode");
        required(value.interfaceGroupName(), "interfaceGroupName");
    }

    /**
     * 中文说明：执行 validate定义 操作；该方法是 {@code GatewayCatalogService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate definition operation; this method is the invocation entry point on {@code GatewayCatalogService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogService.validateDefinition(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param protocol 参数 protocol；parameter protocol。
     * @param value 参数 值；parameter value。
     */
    private void validateDefinition(
            String protocol,
            ManualDefinition value) {
        if (value.requestSchema() == null
                || value.responseSchema() == null
                || value.errorSchema() == null
                || value.tags() == null
                || value.attributes() == null) {
            throw new IllegalArgumentException(
                    "definition schema collections are required"
            );
        }
        if ("RPC".equals(protocol) && value.descriptorSnapshot() == null) {
            throw new IllegalArgumentException(
                    "RPC descriptorSnapshot is required"
            );
        }
    }

    /**
     * 中文说明：执行 bytes 操作；该方法是 {@code GatewayCatalogService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the bytes operation; this method is the invocation entry point on {@code GatewayCatalogService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogService.bytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 bytes 的处理结果；returns the result of the operation.
     */
    private byte[] bytes(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException(
                    "operation definition cannot be serialized",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 审计 操作；该方法是 {@code GatewayCatalogService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the audit operation; this method is the invocation entry point on {@code GatewayCatalogService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogService.audit(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @param resourceType 参数 资源Type；parameter resource type。
     * @param resourceId 参数 资源Id；parameter resource id。
     * @param action 参数 action；parameter action。
     * @param after 参数 after；parameter after。
     */
    private void audit(
            AdminActor actor,
            RequestAuditContext request,
            String resourceType,
            String resourceId,
            String action,
            Map<String, Object> after) {
        audits.save(new GatewayAuditLogEntity(
                UuidV7.simpleString(),
                actor.actorId(),
                actor.actorType().name(),
                "MANAGEMENT_API",
                request.requestId(),
                request.traceId(),
                resourceType,
                resourceId,
                action,
                null,
                after,
                null,
                null,
                true,
                null,
                clock.instant()
        ));
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code GatewayCatalogService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code GatewayCatalogService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogService.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 中文说明：执行 default值 操作；该方法是 {@code GatewayCatalogService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the default value operation; this method is the invocation entry point on {@code GatewayCatalogService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCatalogService.defaultValue(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param fallback 参数 fallback；parameter fallback。
     * @return 返回 default值 的处理结果；returns the result of the operation.
     */
    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    /**
     * 中文说明：{@code Protocol} 是枚举类型，位于当前 Gateway 模块的相关包中，负责Protocol相关的职责与边界。
     * English summary: {@code Protocol} is an enumeration in the current Gateway module; it owns the protocol-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    public enum Protocol {
        /**
         * 中文说明：表示 HTTP 这一固定值；它属于 {@code GatewayCatalogService.Protocol} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value http; it is a state, type, or protocol value of {@code GatewayCatalogService.Protocol} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayCatalogService.Protocol} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService.Protocol}; do not couple callers to its representation when the owning type exposes an API.
         */
        HTTP,
        /**
         * 中文说明：表示 RPC 这一固定值；它属于 {@code GatewayCatalogService.Protocol} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value rpc; it is a state, type, or protocol value of {@code GatewayCatalogService.Protocol} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayCatalogService.Protocol} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService.Protocol}; do not couple callers to its representation when the owning type exposes an API.
         */
        RPC
    }

    /**
     * 中文说明：{@code ManualOperation} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Manual操作相关的职责与边界。
     * English summary: {@code ManualOperation} is an immutable data carrier in the current Gateway module; it owns the manual operation-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param protocol 参数 protocol；parameter protocol。
     * @param httpMethod 参数 http方法；parameter http method。
     * @param path 参数 path；parameter path。
     * @param serviceName 参数 服务Name；parameter service name。
     * @param fullMethodName 参数 full方法Name；parameter full method name。
     * @param providerServiceName 参数 提供方服务Name；parameter provider service name。
     * @param group 参数 group；parameter group。
     * @param version 参数 version；parameter version。
     * @param transport 参数 传输；parameter transport。
     * @param externalAccessible 参数 externalAccessible；parameter external accessible。
     * @param definition 参数 定义；parameter definition。
     */
    public record ManualOperation(
            /**
             * 中文说明：保存 protocol 对应的状态、依赖或配置值；字段类型为 {@code Protocol}，由 {@code GatewayCatalogService.ManualOperation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by protocol; its type is {@code Protocol}, and {@code GatewayCatalogService.ManualOperation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogService.ManualOperation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService.ManualOperation}; do not couple callers to its representation when the owning type exposes an API.
             */
            Protocol protocol,
            /**
             * 中文说明：保存 http方法 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogService.ManualOperation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by http method; its type is {@code String}, and {@code GatewayCatalogService.ManualOperation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogService.ManualOperation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService.ManualOperation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String httpMethod,
            /**
             * 中文说明：保存 path 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogService.ManualOperation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by path; its type is {@code String}, and {@code GatewayCatalogService.ManualOperation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogService.ManualOperation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService.ManualOperation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String path,
            /**
             * 中文说明：保存 服务Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogService.ManualOperation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by service name; its type is {@code String}, and {@code GatewayCatalogService.ManualOperation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogService.ManualOperation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService.ManualOperation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serviceName,
            /**
             * 中文说明：保存 full方法Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogService.ManualOperation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by full method name; its type is {@code String}, and {@code GatewayCatalogService.ManualOperation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogService.ManualOperation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService.ManualOperation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String fullMethodName,
            /**
             * 中文说明：保存 提供方服务Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogService.ManualOperation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by provider service name; its type is {@code String}, and {@code GatewayCatalogService.ManualOperation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogService.ManualOperation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService.ManualOperation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String providerServiceName,
            /**
             * 中文说明：保存 group 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogService.ManualOperation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by group; its type is {@code String}, and {@code GatewayCatalogService.ManualOperation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogService.ManualOperation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService.ManualOperation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String group,
            /**
             * 中文说明：保存 version 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogService.ManualOperation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by version; its type is {@code String}, and {@code GatewayCatalogService.ManualOperation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogService.ManualOperation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService.ManualOperation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String version,
            /**
             * 中文说明：保存 传输 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogService.ManualOperation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by transport; its type is {@code String}, and {@code GatewayCatalogService.ManualOperation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogService.ManualOperation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService.ManualOperation}; do not couple callers to its representation when the owning type exposes an API.
             */
            String transport,
            /**
             * 中文说明：保存 externalAccessible 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayCatalogService.ManualOperation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by external accessible; its type is {@code boolean}, and {@code GatewayCatalogService.ManualOperation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogService.ManualOperation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService.ManualOperation}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean externalAccessible,
            /**
             * 中文说明：保存 定义 对应的状态、依赖或配置值；字段类型为 {@code ManualDefinition}，由 {@code GatewayCatalogService.ManualOperation} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by definition; its type is {@code ManualDefinition}, and {@code GatewayCatalogService.ManualOperation} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogService.ManualOperation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService.ManualOperation}; do not couple callers to its representation when the owning type exposes an API.
             */
            ManualDefinition definition
    ) {
    }

    /**
     * 中文说明：{@code ManualDefinition} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Manual定义相关的职责与边界。
     * English summary: {@code ManualDefinition} is an immutable data carrier in the current Gateway module; it owns the manual definition-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param summary 参数 summary；parameter summary。
     * @param tags 参数 tags；parameter tags。
     * @param requestSchema 参数 请求模式；parameter request schema。
     * @param responseSchema 参数 响应模式；parameter response schema。
     * @param errorSchema 参数 error模式；parameter error schema。
     * @param descriptorSnapshot 参数 descriptorSnapshot；parameter descriptor snapshot。
     * @param attributes 参数 attributes；parameter attributes。
     * @param externalAccessible 参数 externalAccessible；parameter external accessible。
     */
    public record ManualDefinition(
            /**
             * 中文说明：保存 summary 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogService.ManualDefinition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by summary; its type is {@code String}, and {@code GatewayCatalogService.ManualDefinition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogService.ManualDefinition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService.ManualDefinition}; do not couple callers to its representation when the owning type exposes an API.
             */
            String summary,
            /**
             * 中文说明：保存 tags 对应的状态、依赖或配置值；字段类型为 {@code List<String>}，由 {@code GatewayCatalogService.ManualDefinition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by tags; its type is {@code List<String>}, and {@code GatewayCatalogService.ManualDefinition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogService.ManualDefinition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService.ManualDefinition}; do not couple callers to its representation when the owning type exposes an API.
             */
            List<String> tags,
            /**
             * 中文说明：保存 请求模式 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code GatewayCatalogService.ManualDefinition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by request schema; its type is {@code Map<String, Object>}, and {@code GatewayCatalogService.ManualDefinition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogService.ManualDefinition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService.ManualDefinition}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> requestSchema,
            /**
             * 中文说明：保存 响应模式 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code GatewayCatalogService.ManualDefinition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by response schema; its type is {@code Map<String, Object>}, and {@code GatewayCatalogService.ManualDefinition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogService.ManualDefinition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService.ManualDefinition}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> responseSchema,
            /**
             * 中文说明：保存 error模式 对应的状态、依赖或配置值；字段类型为 {@code List<Map<String, Object>>}，由 {@code GatewayCatalogService.ManualDefinition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by error schema; its type is {@code List<Map<String, Object>>}, and {@code GatewayCatalogService.ManualDefinition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogService.ManualDefinition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService.ManualDefinition}; do not couple callers to its representation when the owning type exposes an API.
             */
            List<Map<String, Object>> errorSchema,
            /**
             * 中文说明：保存 descriptorSnapshot 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code GatewayCatalogService.ManualDefinition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by descriptor snapshot; its type is {@code Map<String, Object>}, and {@code GatewayCatalogService.ManualDefinition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogService.ManualDefinition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService.ManualDefinition}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> descriptorSnapshot,
            /**
             * 中文说明：保存 attributes 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code GatewayCatalogService.ManualDefinition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by attributes; its type is {@code Map<String, Object>}, and {@code GatewayCatalogService.ManualDefinition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogService.ManualDefinition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService.ManualDefinition}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> attributes,
            /**
             * 中文说明：保存 externalAccessible 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayCatalogService.ManualDefinition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by external accessible; its type is {@code boolean}, and {@code GatewayCatalogService.ManualDefinition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogService.ManualDefinition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService.ManualDefinition}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean externalAccessible
    ) {
    }

    /**
     * 中文说明：{@code ManualMetadata} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Manual元数据相关的职责与边界。
     * English summary: {@code ManualMetadata} is an immutable data carrier in the current Gateway module; it owns the manual metadata-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param summary 参数 summary；parameter summary。
     * @param tags 参数 tags；parameter tags。
     * @param owner 参数 owner；parameter owner。
     */
    public record ManualMetadata(
            /**
             * 中文说明：保存 summary 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogService.ManualMetadata} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by summary; its type is {@code String}, and {@code GatewayCatalogService.ManualMetadata} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogService.ManualMetadata} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService.ManualMetadata}; do not couple callers to its representation when the owning type exposes an API.
             */
            String summary,
            /**
             * 中文说明：保存 tags 对应的状态、依赖或配置值；字段类型为 {@code List<String>}，由 {@code GatewayCatalogService.ManualMetadata} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by tags; its type is {@code List<String>}, and {@code GatewayCatalogService.ManualMetadata} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogService.ManualMetadata} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService.ManualMetadata}; do not couple callers to its representation when the owning type exposes an API.
             */
            List<String> tags,
            /**
             * 中文说明：保存 owner 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCatalogService.ManualMetadata} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by owner; its type is {@code String}, and {@code GatewayCatalogService.ManualMetadata} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogService.ManualMetadata} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService.ManualMetadata}; do not couple callers to its representation when the owning type exposes an API.
             */
            String owner
    ) {
    }

    /**
     * 中文说明：{@code OperationDetail} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责操作Detail相关的职责与边界。
     * English summary: {@code OperationDetail} is an immutable data carrier in the current Gateway module; it owns the operation detail-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param operation 参数 操作；parameter operation。
     * @param definitions 参数 definitions；parameter definitions。
     */
    public record OperationDetail(
            /**
             * 中文说明：保存 操作 对应的状态、依赖或配置值；字段类型为 {@code GatewayCatalogStore.OperationRecord}，由 {@code GatewayCatalogService.OperationDetail} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by operation; its type is {@code GatewayCatalogStore.OperationRecord}, and {@code GatewayCatalogService.OperationDetail} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogService.OperationDetail} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService.OperationDetail}; do not couple callers to its representation when the owning type exposes an API.
             */
            GatewayCatalogStore.OperationRecord operation,
            /**
             * 中文说明：保存 definitions 对应的状态、依赖或配置值；字段类型为 {@code List<GatewayCatalogStore.OperationDefinition>}，由 {@code GatewayCatalogService.OperationDetail} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by definitions; its type is {@code List<GatewayCatalogStore.OperationDefinition>}, and {@code GatewayCatalogService.OperationDetail} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCatalogService.OperationDetail} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService.OperationDetail}; do not couple callers to its representation when the owning type exposes an API.
             */
            List<GatewayCatalogStore.OperationDefinition> definitions
    ) {
    }
}
