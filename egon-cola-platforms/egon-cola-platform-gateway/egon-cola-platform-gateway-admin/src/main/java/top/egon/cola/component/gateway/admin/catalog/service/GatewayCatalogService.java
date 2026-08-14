package top.egon.cola.component.gateway.admin.catalog.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualDefinitionDTO;
import top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualMetadataDTO;
import top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualOperationDTO;
import top.egon.cola.component.gateway.admin.catalog.domain.enums.GatewayCatalogProtocolEnum;
import top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayOperationDetailVO;
import top.egon.cola.component.gateway.admin.catalog.repository.GatewayCatalogRepository;
import top.egon.cola.component.gateway.admin.observability.domain.po.GatewayAuditLogPO;
import top.egon.cola.component.gateway.admin.observability.repository.GatewayAuditLogRepository;
import top.egon.cola.component.gateway.admin.rule.service.GatewayRuleCanonicalizer;
import top.egon.cola.component.gateway.admin.shared.domain.AdminActor;
import top.egon.cola.component.gateway.admin.shared.domain.RequestAuditContext;
import top.egon.cola.component.gateway.admin.shared.domain.exception.GatewayAdminNotFoundException;
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
     * 中文说明：保存 存储 对应的状态、依赖或配置值；字段类型为 {@code GatewayCatalogRepository}，由 {@code GatewayCatalogService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by store; its type is {@code GatewayCatalogRepository}, and {@code GatewayCatalogService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCatalogService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCatalogService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayCatalogRepository store;

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
            GatewayCatalogRepository store,
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
            GatewayCatalogRepository store,
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
    public top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayCatalogTreeVO catalog(String applicationId) {
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
            top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualHierarchyDTO hierarchy,
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
    public GatewayOperationDetailVO createManualOperation(
            String interfaceGroupId,
            GatewayManualOperationDTO command,
            AdminActor actor,
            RequestAuditContext request) {
        top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayInterfaceGroupScopeVO scope =
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
        top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO operation =
                new top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO(
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
        top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO definition =
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
    public GatewayOperationDetailVO detail(String operationId) {
        top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO operation =
                requiredOperation(operationId);
        return new GatewayOperationDetailVO(
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
    public GatewayOperationDetailVO updateManualDefinition(
            String operationId,
            GatewayManualDefinitionDTO definition,
            AdminActor actor,
            RequestAuditContext request) {
        top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO operation =
                requiredManualOperation(operationId);
        List<top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO> history =
                store.loadDefinitions(operationId);
        long nextVersion = history.stream()
                .mapToLong(top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO
                        ::definitionVersion)
                .max()
                .orElse(0) + 1;
        top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO appended = definition(
                operation,
                nextVersion,
                definition,
                actor.actorId(),
                clock.instant()
        );
        if (history.stream().anyMatch(existing -> existing.definitionSha256()
                .equals(appended.definitionSha256()))) {
            return new GatewayOperationDetailVO(operation, history);
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
    public GatewayOperationDetailVO updateMetadata(
            String operationId,
            GatewayManualMetadataDTO metadata,
            AdminActor actor,
            RequestAuditContext request) {
        top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO operation =
                requiredManualOperation(operationId);
        List<top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO> history =
                store.loadDefinitions(operationId);
        if (history.isEmpty()) {
            throw new IllegalStateException(
                    "manual operation has no definition"
            );
        }
        top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO current = history.getFirst();
        Map<String, Object> attributes =
                new LinkedHashMap<>(current.attributes());
        if (metadata.owner() == null || metadata.owner().isBlank()) {
            attributes.remove("owner");
        } else {
            attributes.put("owner", metadata.owner().trim());
        }
        return updateManualDefinition(
                operationId,
                new GatewayManualDefinitionDTO(
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
    public GatewayOperationDetailVO deprecate(
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
    private top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO definition(
            top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO operation,
            long version,
            GatewayManualDefinitionDTO value,
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
        return new top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO(
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
            top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayInterfaceGroupScopeVO scope,
            GatewayManualOperationDTO command) {
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
    private String methodIdentity(GatewayManualOperationDTO command) {
        return command.protocol() == GatewayCatalogProtocolEnum.HTTP
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
            top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayInterfaceGroupScopeVO scope,
            GatewayManualOperationDTO command) {
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
                        command.protocol() == GatewayCatalogProtocolEnum.HTTP ? "HTTP" : "GRPC"
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
    private top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO requiredOperation(String id) {
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
    private top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO requiredManualOperation(
            String id) {
        top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO operation =
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
            top.egon.cola.component.gateway.admin.catalog.domain.dto.GatewayManualHierarchyDTO value) {
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
            GatewayManualDefinitionDTO value) {
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
        audits.save(new GatewayAuditLogPO(
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










}
