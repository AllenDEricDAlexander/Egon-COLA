package top.egon.cola.component.gateway.admin.application.release;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.model.management.DdcManagementPublishTarget;
import top.egon.cola.component.gateway.admin.application.GatewayAdminNotFoundException;
import top.egon.cola.component.gateway.admin.application.RequestAuditContext;
import top.egon.cola.component.gateway.admin.application.catalog.GatewayCatalogStore;
import top.egon.cola.component.gateway.admin.application.routing.GatewayDraftService;
import top.egon.cola.component.gateway.admin.application.routing.GatewayDraftStore;
import top.egon.cola.component.gateway.admin.application.reporting.GatewayOperationSchemaValidator;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.domain.GatewayReleaseStatus;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayAuditLogRepository;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayDraftEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayDraftRepository;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayGroupEntity;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayGroupRepository;
import top.egon.cola.component.gateway.admin.mcp.application.McpReleaseContentFactory;
import top.egon.cola.component.gateway.admin.rule.CompiledGatewayRelease;
import top.egon.cola.component.gateway.admin.rule.GatewayRouteDraftMapper;
import top.egon.cola.component.gateway.admin.rule.GatewayRouteTransportPolicyValidator;
import top.egon.cola.component.gateway.admin.rule.GatewayRuleCanonicalizer;
import top.egon.cola.component.gateway.admin.rule.GatewayRuleCompiler;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuleContent;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayProviderServiceRef;
import top.egon.cola.component.gateway.contract.rule.GatewayRpcDescriptor;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeOperation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimePolicy;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeRoute;
import top.egon.cola.component.gateway.core.route.GatewayResponseMode;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 中文说明：{@code GatewayReleaseService} 是服务组件，位于当前 Gateway 模块的相关包中，负责网关发布服务相关的职责与边界。
 * English summary: {@code GatewayReleaseService} is a gateway release service service in the current Gateway module; it owns the gateway release service-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Service
public class GatewayReleaseService {

    /**
     * 中文说明：保存 groups 对应的状态、依赖或配置值；字段类型为 {@code GatewayGroupRepository}，由 {@code GatewayReleaseService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by groups; its type is {@code GatewayGroupRepository}, and {@code GatewayReleaseService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleaseService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayGroupRepository groups;

    /**
     * 中文说明：保存 drafts 对应的状态、依赖或配置值；字段类型为 {@code GatewayDraftRepository}，由 {@code GatewayReleaseService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by drafts; its type is {@code GatewayDraftRepository}, and {@code GatewayReleaseService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleaseService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayDraftRepository drafts;

    /**
     * 中文说明：保存 草稿服务 对应的状态、依赖或配置值；字段类型为 {@code GatewayDraftService}，由 {@code GatewayReleaseService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by draft service; its type is {@code GatewayDraftService}, and {@code GatewayReleaseService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleaseService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayDraftService draftService;

    /**
     * 中文说明：保存 目录 对应的状态、依赖或配置值；字段类型为 {@code GatewayCatalogStore}，由 {@code GatewayReleaseService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by catalog; its type is {@code GatewayCatalogStore}, and {@code GatewayReleaseService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleaseService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayCatalogStore catalog;

    /**
     * 中文说明：保存 releases 对应的状态、依赖或配置值；字段类型为 {@code GatewayReleaseStore}，由 {@code GatewayReleaseService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by releases; its type is {@code GatewayReleaseStore}, and {@code GatewayReleaseService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleaseService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayReleaseStore releases;

    /**
     * 中文说明：保存 audits 对应的状态、依赖或配置值；字段类型为 {@code GatewayAuditLogRepository}，由 {@code GatewayReleaseService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by audits; its type is {@code GatewayAuditLogRepository}, and {@code GatewayReleaseService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleaseService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayAuditLogRepository audits;

    /**
     * 中文说明：保存 transactions 对应的状态、依赖或配置值；字段类型为 {@code TransactionTemplate}，由 {@code GatewayReleaseService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by transactions; its type is {@code TransactionTemplate}, and {@code GatewayReleaseService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleaseService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final TransactionTemplate transactions;

    /**
     * 中文说明：保存 publications 对应的状态、依赖或配置值；字段类型为 {@code GatewayReleasePublicationCoordinator}，由 {@code GatewayReleaseService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by publications; its type is {@code GatewayReleasePublicationCoordinator}, and {@code GatewayReleaseService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleaseService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayReleasePublicationCoordinator publications;

    /**
     * 中文说明：保存 MCPContent工厂 对应的状态、依赖或配置值；字段类型为 {@code McpReleaseContentFactory}，由 {@code GatewayReleaseService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by mcp content factory; its type is {@code McpReleaseContentFactory}, and {@code GatewayReleaseService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleaseService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final McpReleaseContentFactory mcpContentFactory;

    /**
     * 中文说明：保存 canonicalizer 对应的状态、依赖或配置值；字段类型为 {@code GatewayRuleCanonicalizer}，由 {@code GatewayReleaseService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by canonicalizer; its type is {@code GatewayRuleCanonicalizer}, and {@code GatewayReleaseService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleaseService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayRuleCanonicalizer canonicalizer =
            new GatewayRuleCanonicalizer();

    /**
     * 中文说明：保存 compiler 对应的状态、依赖或配置值；字段类型为 {@code GatewayRuleCompiler}，由 {@code GatewayReleaseService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by compiler; its type is {@code GatewayRuleCompiler}, and {@code GatewayReleaseService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleaseService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayRuleCompiler compiler =
            new GatewayRuleCompiler(canonicalizer);

    /**
     * 中文说明：保存 路由映射器 对应的状态、依赖或配置值；字段类型为 {@code GatewayRouteDraftMapper}，由 {@code GatewayReleaseService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by route mapper; its type is {@code GatewayRouteDraftMapper}, and {@code GatewayReleaseService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleaseService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayRouteDraftMapper routeMapper =
            new GatewayRouteDraftMapper();

    /**
     * 中文说明：保存 传输校验器 对应的状态、依赖或配置值；字段类型为 {@code GatewayRouteTransportPolicyValidator}，由 {@code GatewayReleaseService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by transport validator; its type is {@code GatewayRouteTransportPolicyValidator}, and {@code GatewayReleaseService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleaseService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayRouteTransportPolicyValidator transportValidator =
            new GatewayRouteTransportPolicyValidator();

    /**
     * 中文说明：保存 模式校验器 对应的状态、依赖或配置值；字段类型为 {@code GatewayOperationSchemaValidator}，由 {@code GatewayReleaseService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by schema validator; its type is {@code GatewayOperationSchemaValidator}, and {@code GatewayReleaseService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleaseService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayOperationSchemaValidator schemaValidator =
            new GatewayOperationSchemaValidator(new ObjectMapper());

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code GatewayReleaseService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code GatewayReleaseService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReleaseService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：创建 {@code GatewayReleaseService} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayReleaseService} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param groups 参数 groups；parameter groups。
     * @param drafts 参数 drafts；parameter drafts。
     * @param draftService 参数 草稿服务；parameter draft service。
     * @param catalog 参数 目录；parameter catalog。
     * @param releases 参数 releases；parameter releases。
     * @param audits 参数 audits；parameter audits。
     * @param transactions 参数 transactions；parameter transactions。
     * @param publications 参数 publications；parameter publications。
     * @param mcpContentFactory 参数 MCPContent工厂；parameter mcp content factory。
     */
    @Autowired
    public GatewayReleaseService(
            GatewayGroupRepository groups,
            GatewayDraftRepository drafts,
            GatewayDraftService draftService,
            GatewayCatalogStore catalog,
            GatewayReleaseStore releases,
            GatewayAuditLogRepository audits,
            TransactionTemplate transactions,
            ObjectProvider<GatewayReleasePublicationCoordinator>
                    publications,
            ObjectProvider<McpReleaseContentFactory> mcpContentFactory) {
        this(
                groups,
                drafts,
                draftService,
                catalog,
                releases,
                audits,
                transactions,
                publications.getIfAvailable(),
                mcpContentFactory.getIfAvailable(),
                Clock.systemUTC()
        );
    }

    /**
     * 中文说明：创建 {@code GatewayReleaseService} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayReleaseService} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param groups 参数 groups；parameter groups。
     * @param drafts 参数 drafts；parameter drafts。
     * @param draftService 参数 草稿服务；parameter draft service。
     * @param catalog 参数 目录；parameter catalog。
     * @param releases 参数 releases；parameter releases。
     * @param audits 参数 audits；parameter audits。
     * @param transactions 参数 transactions；parameter transactions。
     * @param publications 参数 publications；parameter publications。
     * @param clock 参数 clock；parameter clock。
     */
    GatewayReleaseService(
            GatewayGroupRepository groups,
            GatewayDraftRepository drafts,
            GatewayDraftService draftService,
            GatewayCatalogStore catalog,
            GatewayReleaseStore releases,
            GatewayAuditLogRepository audits,
            TransactionTemplate transactions,
            GatewayReleasePublicationCoordinator publications,
            Clock clock) {
        this(
                groups,
                drafts,
                draftService,
                catalog,
                releases,
                audits,
                transactions,
                publications,
                null,
                clock
        );
    }

    /**
     * 中文说明：创建 {@code GatewayReleaseService} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayReleaseService} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param groups 参数 groups；parameter groups。
     * @param drafts 参数 drafts；parameter drafts。
     * @param draftService 参数 草稿服务；parameter draft service。
     * @param catalog 参数 目录；parameter catalog。
     * @param releases 参数 releases；parameter releases。
     * @param audits 参数 audits；parameter audits。
     * @param transactions 参数 transactions；parameter transactions。
     * @param publications 参数 publications；parameter publications。
     * @param mcpContentFactory 参数 MCPContent工厂；parameter mcp content factory。
     * @param clock 参数 clock；parameter clock。
     */
    GatewayReleaseService(
            GatewayGroupRepository groups,
            GatewayDraftRepository drafts,
            GatewayDraftService draftService,
            GatewayCatalogStore catalog,
            GatewayReleaseStore releases,
            GatewayAuditLogRepository audits,
            TransactionTemplate transactions,
            GatewayReleasePublicationCoordinator publications,
            McpReleaseContentFactory mcpContentFactory,
            Clock clock) {
        this.groups = groups;
        this.drafts = drafts;
        this.draftService = draftService;
        this.catalog = catalog;
        this.releases = releases;
        this.audits = audits;
        this.transactions = transactions;
        this.publications = publications;
        this.mcpContentFactory = mcpContentFactory;
        this.clock = clock;
    }

    /**
     * 中文说明：执行 create 操作；该方法是 {@code GatewayReleaseService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create operation; this method is the invocation entry point on {@code GatewayReleaseService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseService.create(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param command 参数 command；parameter command。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 create 的处理结果；returns the result of the operation.
     */
    public ReleaseView create(
            String gatewayGroupId,
            CreateRelease command,
            AdminActor actor,
            RequestAuditContext request) {
        PreparedRelease prepared = transactions.execute(status -> prepare(
                gatewayGroupId,
                command.expectedDraftRevision(),
                command.changeReason(),
                null,
                null,
                actor,
                request
        ));
        return publish(prepared, actor);
    }

    /**
     * 中文说明：执行 重试 操作；该方法是 {@code GatewayReleaseService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the retry operation; this method is the invocation entry point on {@code GatewayReleaseService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseService.retry(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 重试 的处理结果；returns the result of the operation.
     */
    public ReleaseView retry(
            String releaseId,
            AdminActor actor,
            RequestAuditContext request) {
        PreparedRelease prepared = transactions.execute(status -> {
            GatewayReleaseStore.ReleaseRecord release = required(releaseId);
            if (!Set.of(
                    GatewayReleaseStatus.FAILED,
                    GatewayReleaseStatus.TIMEOUT,
                    GatewayReleaseStatus.UNKNOWN
            ).contains(release.status())) {
                throw new IllegalStateException(
                        "GATEWAY_ADMIN_RELEASE_NOT_RETRYABLE"
                );
            }
            int attempt = releases.nextAttempt(
                    releaseId,
                    clock.instant()
            );
            audit(
                    actor,
                    request,
                    release.gatewayGroupId(),
                    releaseId,
                    "RETRY",
                    release.draftRevision()
            );
            return new PreparedRelease(
                    release,
                    releases.loadCompiled(releaseId),
                    attempt
            );
        });
        return publish(prepared, actor);
    }

    /**
     * 中文说明：执行 rollback 操作；该方法是 {@code GatewayReleaseService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the rollback operation; this method is the invocation entry point on {@code GatewayReleaseService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseService.rollback(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param command 参数 command；parameter command。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 rollback 的处理结果；returns the result of the operation.
     */
    public ReleaseView rollback(
            String gatewayGroupId,
            RollbackRelease command,
            AdminActor actor,
            RequestAuditContext request) {
        GatewayReleaseStore.ReleaseRecord source =
                required(command.sourceReleaseId());
        if (!source.gatewayGroupId().equals(gatewayGroupId)
                || source.status() != GatewayReleaseStatus.SUCCESS) {
            throw new IllegalArgumentException(
                    "rollback source must be a successful release "
                            + "from the same gateway group"
            );
        }
        GatewayRuleContent content = releases.loadCompiled(source.id())
                .snapshot()
                .content();
        PreparedRelease prepared = transactions.execute(status -> prepare(
                gatewayGroupId,
                command.expectedDraftRevision(),
                command.changeReason(),
                source.id(),
                content,
                actor,
                request
        ));
        return publish(prepared, actor);
    }

    /**
     * 中文说明：执行 get 操作；该方法是 {@code GatewayReleaseService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get operation; this method is the invocation entry point on {@code GatewayReleaseService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseService.get(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @return 返回 get 的处理结果；returns the result of the operation.
     */
    public ReleaseView get(String releaseId) {
        return view(required(releaseId));
    }

    /**
     * 中文说明：执行 history 操作；该方法是 {@code GatewayReleaseService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the history operation; this method is the invocation entry point on {@code GatewayReleaseService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseService.history(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 history 的处理结果；returns the result of the operation.
     */
    public List<ReleaseView> history(String gatewayGroupId) {
        return releases.history(gatewayGroupId)
                .stream()
                .map(this::view)
                .toList();
    }

    /**
     * 中文说明：执行 diff 操作；该方法是 {@code GatewayReleaseService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the diff operation; this method is the invocation entry point on {@code GatewayReleaseService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseService.diff(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @return 返回 diff 的处理结果；returns the result of the operation.
     */
    public Map<String, Object> diff(String releaseId) {
        return required(releaseId).structuredDiff();
    }

    /**
     * 中文说明：执行 prepare 操作；该方法是 {@code GatewayReleaseService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the prepare operation; this method is the invocation entry point on {@code GatewayReleaseService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseService.prepare(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param changeReason 参数 changeReason；parameter change reason。
     * @param rollbackOfReleaseId 参数 rollbackOf发布Id；parameter rollback of release id。
     * @param rollbackContent 参数 rollbackContent；parameter rollback content。
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @return 返回 prepare 的处理结果；returns the result of the operation.
     */
    private PreparedRelease prepare(
            String gatewayGroupId,
            long expectedRevision,
            String changeReason,
            String rollbackOfReleaseId,
            GatewayRuleContent rollbackContent,
            AdminActor actor,
            RequestAuditContext request) {
        GatewayGroupEntity group = groups.findByIdAndDeletedFalse(
                gatewayGroupId
        ).orElseThrow(() -> new GatewayAdminNotFoundException(
                "gateway group " + gatewayGroupId + " was not found"
        ));
        if (!group.isEnabled()) {
            throw new IllegalStateException(
                    "GATEWAY_ADMIN_GATEWAY_GROUP_DISABLED"
            );
        }
        if (releases.hasReleaseInProgress(gatewayGroupId)) {
            throw new IllegalStateException(
                    "GATEWAY_ADMIN_RELEASE_IN_PROGRESS"
            );
        }
        GatewayDraftEntity draft = drafts.findById(gatewayGroupId)
                .orElseThrow(() -> new GatewayAdminNotFoundException(
                        "gateway draft " + gatewayGroupId + " was not found"
                ));
        draft.assertEditable(expectedRevision);
        GatewayDraftService.ValidationReport validation =
                draftService.validate(gatewayGroupId);
        if (!validation.valid()) {
            throw new IllegalArgumentException(
                    "draft validation failed: " + validation.errors()
            );
        }
        String releaseId = UuidV7.simpleString();
        GatewayRuleContent content = rollbackContent == null
                ? content(group, draftService.get(gatewayGroupId))
                : rollbackContent;
        CompiledGatewayRelease compiled = compiler.compile(
                releaseId,
                clock.instant(),
                content
        );
        Instant now = clock.instant();
        Map<String, Object> structuredDiff = Map.of(
                "basedOnReleaseId",
                value(draft.getBasedOnReleaseId()),
                "rollbackOfReleaseId",
                value(rollbackOfReleaseId),
                "routeCount", content.routes().size(),
                "operationCount", content.operations().size(),
                "policyCount", policyCount(content),
                "mcpServerCount", content.mcp().servers().size(),
                "mcpToolCount", content.mcp().tools().size(),
                "ruleContentSha256",
                compiled.snapshot().ruleContentSha256()
        );
        GatewayReleaseStore.ReleaseRecord release =
                new GatewayReleaseStore.ReleaseRecord(
                        releaseId,
                        gatewayGroupId,
                        draft.getRevision(),
                        draft.getBasedOnReleaseId(),
                        rollbackOfReleaseId,
                        GatewayReleaseStatus.READY,
                        false,
                        null,
                        Map.of(
                                "valid", true,
                                "warnings", validation.warnings(),
                                "draftSha256", validation.draftSha256()
                        ),
                        structuredDiff,
                        required(changeReason, "changeReason"),
                        now,
                        actor.actorId(),
                        now
                );
        releases.insert(release, compiled, 1);
        audit(
                actor,
                request,
                gatewayGroupId,
                releaseId,
                rollbackOfReleaseId == null ? "CREATE_RELEASE" : "ROLLBACK",
                draft.getRevision()
        );
        return new PreparedRelease(release, compiled, 1);
    }

    /**
     * 中文说明：执行 publish 操作；该方法是 {@code GatewayReleaseService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the publish operation; this method is the invocation entry point on {@code GatewayReleaseService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseService.publish(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param prepared 参数 prepared；parameter prepared。
     * @param actor 参数 actor；parameter actor。
     * @return 返回 publish 的处理结果；returns the result of the operation.
     */
    private ReleaseView publish(
            PreparedRelease prepared,
            AdminActor actor) {
        transactions.executeWithoutResult(status -> releases.beginAttempt(
                prepared.release().id(),
                prepared.attemptNo(),
                clock.instant()
        ));
        if (publications == null) {
            transactions.executeWithoutResult(status -> releases
                    .completeAttempt(
                            prepared.release().id(),
                            prepared.attemptNo(),
                            GatewayReleaseStatus.FAILED,
                            false,
                            null,
                            "GATEWAY_ADMIN_DDC_UNAVAILABLE",
                            "DDC management client is not configured",
                            List.of(),
                            clock.instant()
                    ));
            return get(prepared.release().id());
        }
        try {
            GatewayReleasePublicationCoordinator.PublicationOutcome outcome =
                    publications.execute(
                            prepared.release().id(),
                            prepared.attemptNo(),
                            prepared.compiled(),
                            actor.actorId()
                    );
            List<GatewayReleaseStore.TargetRecord> targets = outcome.result()
                    .targets()
                    .stream()
                    .map(target -> target(
                            target,
                            prepared.compiled()
                                    .activation()
                                    .artifactSha256()
                    ))
                    .toList();
            transactions.executeWithoutResult(status -> {
                releases.completeAttempt(
                        prepared.release().id(),
                        prepared.attemptNo(),
                        releaseStatus(outcome.status()),
                        outcome.partialApplied(),
                        outcome.changeId(),
                        outcome.successful()
                                ? null
                                : "DDC_PUBLISH_" + outcome.status(),
                        outcome.result().errorMessage(),
                        targets,
                        clock.instant()
                );
                if (outcome.successful()) {
                    GatewayDraftEntity draft = drafts.findById(
                            prepared.release().gatewayGroupId()
                    ).orElseThrow();
                    draft.baseOn(
                            prepared.release().id(),
                            actor.actorId(),
                            clock.instant()
                    );
                    drafts.flush();
                }
            });
        } catch (RuntimeException failure) {
            transactions.executeWithoutResult(status -> releases
                    .completeAttempt(
                            prepared.release().id(),
                            prepared.attemptNo(),
                            GatewayReleaseStatus.UNKNOWN,
                            false,
                            null,
                            "GATEWAY_ADMIN_DDC_UNAVAILABLE",
                            bounded(failure.getMessage()),
                            List.of(),
                            clock.instant()
                    ));
        }
        return get(prepared.release().id());
    }

    /**
     * 中文说明：执行 发布Status 操作；该方法是 {@code GatewayReleaseService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the release status operation; this method is the invocation entry point on {@code GatewayReleaseService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseService.releaseStatus(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param status 参数 status；parameter status。
     * @return 返回 发布Status 的处理结果；returns the result of the operation.
     */
    private GatewayReleaseStatus releaseStatus(
            GatewayReleasePublicationStore.PublicationStatus status) {
        return switch (status) {
            case SUCCESS -> GatewayReleaseStatus.SUCCESS;
            case FAILED, PARTIAL_SUCCESS -> GatewayReleaseStatus.FAILED;
            case TIMEOUT -> GatewayReleaseStatus.TIMEOUT;
            case PLANNED, RESOLVED, SUBMITTED, UNKNOWN ->
                    GatewayReleaseStatus.UNKNOWN;
        };
    }

    /**
     * 中文说明：执行 content 操作；该方法是 {@code GatewayReleaseService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the content operation; this method is the invocation entry point on {@code GatewayReleaseService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseService.content(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param group 参数 group；parameter group。
     * @param draft 参数 草稿；parameter draft。
     * @return 返回 content 的处理结果；returns the result of the operation.
     */
    private GatewayRuleContent content(
            GatewayGroupEntity group,
            GatewayDraftService.DraftView draft) {
        McpRuleContent mcp = mcpContentFactory == null
                ? McpRuleContent.empty()
                : mcpContentFactory.compileForRelease(
                        group.getId(),
                        draft.revision()
                );
        Set<String> referencedOperationIds = new LinkedHashSet<>();
        draft.routes().stream()
                .filter(GatewayDraftStore.RouteDraft::enabled)
                .map(GatewayDraftStore.RouteDraft::operationId)
                .forEach(referencedOperationIds::add);
        mcp.tools().stream()
                .filter(tool -> tool.enabled()
                        && "LOCAL_OPERATION".equals(tool.sourceType()))
                .map(tool -> tool.operationId())
                .forEach(referencedOperationIds::add);
        mcp.resources().stream()
                .filter(resource -> resource.enabled()
                        && "LOCAL_OPERATION".equals(resource.driverType()))
                .map(resource -> resource.operationId())
                .forEach(referencedOperationIds::add);
        mcp.resourceTemplates().stream()
                .filter(template -> template.enabled()
                        && "LOCAL_OPERATION".equals(template.driverType()))
                .map(template -> template.operationId())
                .forEach(referencedOperationIds::add);
        mcp.prompts().stream()
                .filter(prompt -> prompt.enabled()
                        && "LOCAL_OPERATION".equals(prompt.sourceType()))
                .map(prompt -> prompt.operationId())
                .forEach(referencedOperationIds::add);
        Map<String, GatewayCatalogStore.OperationRecord> operations =
                new LinkedHashMap<>();
        referencedOperationIds.forEach(operationId -> operations.put(
                operationId,
                catalog.findOperation(operationId)
                        .orElseThrow(() -> new GatewayAdminNotFoundException(
                                "gateway operation "
                                        + operationId
                                        + " was not found"
                        ))
        ));
        List<GatewayRuntimePolicy> policies = draft.policies()
                .stream()
                .filter(GatewayDraftStore.PolicyDraft::enabled)
                .map(policy -> new GatewayRuntimePolicy(
                        policy.policyId(),
                        policy.policyType(),
                        policy.policyScope(),
                        policy.content()
                ))
                .toList();
        List<GatewayRuntimeOperation> runtimeOperations = operations.values()
                .stream()
                .map(operation -> operation(
                        operation,
                        policyRefs(operation.id(), policies)
                ))
                .toList();
        Map<String, GatewayRuntimeOperation> runtimeOperationsById =
                runtimeOperations.stream().collect(
                        java.util.stream.Collectors.toUnmodifiableMap(
                                GatewayRuntimeOperation::operationId,
                                java.util.function.Function.identity()
                        )
                );
        List<GatewayRuntimeRoute> runtimeRoutes = draft.routes()
                .stream()
                .filter(GatewayDraftStore.RouteDraft::enabled)
                .map(route -> route(
                        route,
                        runtimeOperationsById.get(route.operationId())
                ))
                .toList();
        List<GatewayRuntimePolicy> provider = policies.stream()
                .filter(policy -> Set.of(
                        "LOAD_BALANCE",
                        "PROVIDER_OVERRIDE"
                ).contains(policy.type()))
                .toList();
        List<GatewayRuntimePolicy> security = policies.stream()
                .filter(policy -> "SECURITY".equals(policy.type()))
                .toList();
        List<GatewayRuntimePolicy> cors = policies.stream()
                .filter(policy -> "CORS".equals(policy.type()))
                .toList();
        Set<String> separated = new LinkedHashSet<>();
        provider.forEach(policy -> separated.add(policy.policyId()));
        security.forEach(policy -> separated.add(policy.policyId()));
        cors.forEach(policy -> separated.add(policy.policyId()));
        List<GatewayRuntimePolicy> traffic = policies.stream()
                .filter(policy -> !separated.contains(policy.policyId()))
                .toList();
        List<GatewayRpcDescriptor> descriptors = runtimeOperations.stream()
                .filter(operation -> operation.protocol()
                        == GatewayProtocol.RPC)
                .map(operation -> descriptor(operations.get(
                        operation.operationId()
                )))
                .distinct()
                .toList();
        return new GatewayRuleContent(
                group.getId(),
                group.getGatewayGroupCode(),
                group.getEnv(),
                group.getNamespace(),
                runtimeOperations,
                runtimeRoutes,
                provider,
                traffic,
                security,
                cors,
                descriptors,
                mcp
        );
    }

    /**
     * 中文说明：执行 操作 操作；该方法是 {@code GatewayReleaseService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the operation operation; this method is the invocation entry point on {@code GatewayReleaseService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseService.operation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operation 参数 操作；parameter operation。
     * @param policyRefs 参数 策略Refs；parameter policy refs。
     * @return 返回 操作 的处理结果；returns the result of the operation.
     */
    private GatewayRuntimeOperation operation(
            GatewayCatalogStore.OperationRecord operation,
            Set<String> policyRefs) {
        GatewayCatalogStore.OperationDefinition definition =
                catalog.loadDefinitions(operation.id())
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "operation has no definition: "
                                        + operation.id()
                        ));
        Map<String, Object> reported = definition.attributes();
        schemaValidator.validate(
                operation.operationKey(),
                operation.protocol(),
                definition.requestSchema(),
                definition.responseSchema(),
                reported
        );
        Map<String, String> attributes = new LinkedHashMap<>();
        reported.forEach((key, value) -> {
            if (value != null) {
                attributes.put(key, value.toString());
            }
        });
        if (operation.protocol().equals("RPC")
                && definition.descriptorSnapshot() != null) {
            attributes.put(
                    "descriptorSha256",
                    definition.descriptorSnapshot()
                            .get("sha256")
                            .toString()
            );
        }
        Map<String, Object> provider = operation.providerServiceIdentity();
        GatewayProtocol protocol = GatewayProtocol.valueOf(
                operation.protocol()
        );
        String requestSchema = protocol == GatewayProtocol.RPC
                ? text(definition.requestSchema(), "messageType")
                : canonicalizer.json(definition.requestSchema());
        String responseSchema = protocol == GatewayProtocol.RPC
                ? text(definition.responseSchema(), "messageType")
                : canonicalizer.json(definition.responseSchema());
        return new GatewayRuntimeOperation(
                operation.id(),
                operation.operationKey(),
                protocol,
                operation.methodIdentity(),
                requestSchema,
                responseSchema,
                operation.externalAccessible(),
                new GatewayProviderServiceRef(
                        text(provider, "bizCode"),
                        text(provider, "appCode"),
                        text(provider, "env"),
                        text(provider, "namespace"),
                        protocol,
                        text(provider, "serviceName"),
                        text(provider, "group"),
                        text(provider, "version"),
                        text(provider, "transport")
                ),
                attributes.getOrDefault(
                        "responseMode",
                        "TRANSPARENT"
                ),
                policyRefs,
                attributes,
                "DEPRECATED".equals(operation.lifecycleStatus())
        );
    }

    /**
     * 中文说明：执行 路由 操作；该方法是 {@code GatewayReleaseService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the route operation; this method is the invocation entry point on {@code GatewayReleaseService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseService.route(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param route 参数 路由；parameter route。
     * @param operation 参数 操作；parameter operation。
     * @return 返回 路由 的处理结果；returns the result of the operation.
     */
    private GatewayRuntimeRoute route(
            GatewayDraftStore.RouteDraft route,
            GatewayRuntimeOperation operation) {
        Map<String, Object> content = routeMapper.canonicalize(
                route.content()
        );
        List<GatewayRouteTransportPolicyValidator.ValidationIssue> issues =
                transportValidator.validate(
                        content,
                        operation.protocol(),
                        GatewayResponseMode.valueOf(operation.responseMode())
                );
        if (!issues.isEmpty()) {
            GatewayRouteTransportPolicyValidator.ValidationIssue issue =
                    issues.getFirst();
            throw new IllegalArgumentException(
                    "GATEWAY_RELEASE_VALIDATION_FAILED: "
                            + issue.code()
                            + " at "
                            + issue.path()
                            + ": "
                            + issue.message()
            );
        }
        return new GatewayRuntimeRoute(
                route.routeId(),
                route.operationId(),
                text(content, "host"),
                text(content, "httpMethod"),
                text(content, "pathPattern"),
                values(content.get("accessZones")).stream()
                        .map(value -> AccessZone.valueOf(
                                value.toUpperCase()
                        ))
                        .collect(java.util.stream.Collectors.toUnmodifiableSet()),
                number(content.get("priority"), 0),
                route.enabled(),
                routeMapper.transportPolicy(content)
        );
    }

    /**
     * 中文说明：执行 descriptor 操作；该方法是 {@code GatewayReleaseService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the descriptor operation; this method is the invocation entry point on {@code GatewayReleaseService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseService.descriptor(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operation 参数 操作；parameter operation。
     * @return 返回 descriptor 的处理结果；returns the result of the operation.
     */
    private GatewayRpcDescriptor descriptor(
            GatewayCatalogStore.OperationRecord operation) {
        Map<String, Object> descriptor = catalog.loadDefinitions(
                operation.id()
        ).getFirst().descriptorSnapshot();
        if (descriptor == null) {
            throw new IllegalArgumentException(
                    "RPC operation has no descriptor snapshot"
            );
        }
        return new GatewayRpcDescriptor(
                text(descriptor, "descriptorId"),
                text(descriptor, "sha256"),
                text(descriptor, "base64DescriptorSet")
        );
    }

    /**
     * 中文说明：执行 策略Refs 操作；该方法是 {@code GatewayReleaseService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the policy refs operation; this method is the invocation entry point on {@code GatewayReleaseService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseService.policyRefs(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operationId 参数 操作Id；parameter operation id。
     * @param policies 参数 policies；parameter policies。
     * @return 返回 策略Refs 的处理结果；returns the result of the operation.
     */
    private Set<String> policyRefs(
            String operationId,
            List<GatewayRuntimePolicy> policies) {
        return policies.stream()
                .filter(policy -> applies(policy, operationId))
                .map(GatewayRuntimePolicy::policyId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /**
     * 中文说明：执行 applies 操作；该方法是 {@code GatewayReleaseService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the applies operation; this method is the invocation entry point on {@code GatewayReleaseService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseService.applies(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policy 参数 策略；parameter policy。
     * @param operationId 参数 操作Id；parameter operation id。
     * @return 返回 applies 的处理结果；returns the result of the operation.
     */
    private boolean applies(
            GatewayRuntimePolicy policy,
            String operationId) {
        if ("GLOBAL".equals(policy.scope())) {
            return true;
        }
        Object operationIds = policy.configuration().get("operationIds");
        return values(operationIds).contains(operationId);
    }

    /**
     * 中文说明：执行 target 操作；该方法是 {@code GatewayReleaseService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the target operation; this method is the invocation entry point on {@code GatewayReleaseService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseService.target(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param target 参数 target；parameter target。
     * @param artifactSha256 参数 制品Sha256；parameter artifact sha256。
     * @return 返回 target 的处理结果；returns the result of the operation.
     */
    private GatewayReleaseStore.TargetRecord target(
            DdcManagementPublishTarget target,
            String artifactSha256) {
        return new GatewayReleaseStore.TargetRecord(
                target.instanceId(),
                target.leaseId(),
                target.status(),
                target.currentVersion(),
                artifactSha256,
                target.errorMessage() == null ? null : "DDC_TARGET_ERROR",
                target.ackAt() == null ? clock.instant() : target.ackAt()
        );
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code GatewayReleaseService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code GatewayReleaseService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseService.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private GatewayReleaseStore.ReleaseRecord required(String releaseId) {
        return releases.find(releaseId)
                .orElseThrow(() -> new GatewayAdminNotFoundException(
                        "gateway release " + releaseId + " was not found"
                ));
    }

    /**
     * 中文说明：执行 审计 操作；该方法是 {@code GatewayReleaseService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the audit operation; this method is the invocation entry point on {@code GatewayReleaseService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseService.audit(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param actor 参数 actor；parameter actor。
     * @param request 参数 请求；parameter request。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param releaseId 参数 发布Id；parameter release id。
     * @param action 参数 action；parameter action。
     * @param revision 参数 revision；parameter revision。
     */
    private void audit(
            AdminActor actor,
            RequestAuditContext request,
            String gatewayGroupId,
            String releaseId,
            String action,
            long revision) {
        audits.save(new GatewayAuditLogEntity(
                UuidV7.simpleString(),
                actor.actorId(),
                actor.actorType().name(),
                "MANAGEMENT_API",
                request.requestId(),
                request.traceId(),
                "GATEWAY_RELEASE",
                releaseId,
                action,
                null,
                Map.of("gatewayGroupId", gatewayGroupId),
                revision,
                releaseId,
                true,
                null,
                clock.instant()
        ));
    }

    /**
     * 中文说明：执行 view 操作；该方法是 {@code GatewayReleaseService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the view operation; this method is the invocation entry point on {@code GatewayReleaseService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseService.view(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param release 参数 发布；parameter release。
     * @return 返回 view 的处理结果；returns the result of the operation.
     */
    private ReleaseView view(GatewayReleaseStore.ReleaseRecord release) {
        return new ReleaseView(
                release.id(),
                release.gatewayGroupId(),
                release.draftRevision(),
                release.basedOnReleaseId(),
                release.rollbackOfReleaseId(),
                release.status(),
                release.partialApplied(),
                release.changeId(),
                release.validationReport(),
                release.structuredDiff(),
                release.changeReason(),
                release.createdAt(),
                release.updatedAt(),
                releases.attempts(release.id())
        );
    }

    /**
     * 中文说明：执行 策略Count 操作；该方法是 {@code GatewayReleaseService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the policy count operation; this method is the invocation entry point on {@code GatewayReleaseService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseService.policyCount(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     * @return 返回 策略Count 的处理结果；returns the result of the operation.
     */
    private int policyCount(GatewayRuleContent content) {
        return content.providerPolicies().size()
                + content.trafficPolicies().size()
                + content.securityPolicies().size()
                + content.corsPolicies().size();
    }

    /**
     * 中文说明：执行 values 操作；该方法是 {@code GatewayReleaseService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the values operation; this method is the invocation entry point on {@code GatewayReleaseService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseService.values(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 values 的处理结果；returns the result of the operation.
     */
    private List<String> values(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(Object::toString).toList();
        }
        return List.of(value.toString());
    }

    /**
     * 中文说明：执行 number 操作；该方法是 {@code GatewayReleaseService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the number operation; this method is the invocation entry point on {@code GatewayReleaseService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseService.number(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param fallback 参数 fallback；parameter fallback。
     * @return 返回 number 的处理结果；returns the result of the operation.
     */
    private int number(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        return value instanceof Number number
                ? number.intValue()
                : Integer.parseInt(value.toString());
    }

    /**
     * 中文说明：执行 text 操作；该方法是 {@code GatewayReleaseService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the text operation; this method is the invocation entry point on {@code GatewayReleaseService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseService.text(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param values 参数 values；parameter values。
     * @param key 参数 键；parameter key。
     * @return 返回 text 的处理结果；returns the result of the operation.
     */
    private String text(Map<String, ?> values, String key) {
        Object value = values.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value.toString().trim();
    }

    /**
     * 中文说明：执行 值 操作；该方法是 {@code GatewayReleaseService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the value operation; this method is the invocation entry point on {@code GatewayReleaseService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseService.value(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 值 的处理结果；returns the result of the operation.
     */
    private String value(String value) {
        return value == null ? "" : value;
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code GatewayReleaseService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code GatewayReleaseService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseService.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 bounded 操作；该方法是 {@code GatewayReleaseService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the bounded operation; this method is the invocation entry point on {@code GatewayReleaseService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseService.bounded(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 bounded 的处理结果；returns the result of the operation.
     */
    private String bounded(String value) {
        if (value == null) {
            return "DDC publish failed";
        }
        return value.length() <= 1024 ? value : value.substring(0, 1024);
    }

    /**
     * 中文说明：{@code PreparedRelease} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Prepared发布相关的职责与边界。
     * English summary: {@code PreparedRelease} is an immutable data carrier in the current Gateway module; it owns the prepared release-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param release 参数 发布；parameter release。
     * @param compiled 参数 compiled；parameter compiled。
     * @param attemptNo 参数 attemptNo；parameter attempt no。
     */
    private record PreparedRelease(
            /**
             * 中文说明：保存 发布 对应的状态、依赖或配置值；字段类型为 {@code GatewayReleaseStore.ReleaseRecord}，由 {@code GatewayReleaseService.PreparedRelease} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by release; its type is {@code GatewayReleaseStore.ReleaseRecord}, and {@code GatewayReleaseService.PreparedRelease} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseService.PreparedRelease} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService.PreparedRelease}; do not couple callers to its representation when the owning type exposes an API.
             */
            GatewayReleaseStore.ReleaseRecord release,
            /**
             * 中文说明：保存 compiled 对应的状态、依赖或配置值；字段类型为 {@code CompiledGatewayRelease}，由 {@code GatewayReleaseService.PreparedRelease} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by compiled; its type is {@code CompiledGatewayRelease}, and {@code GatewayReleaseService.PreparedRelease} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseService.PreparedRelease} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService.PreparedRelease}; do not couple callers to its representation when the owning type exposes an API.
             */
            CompiledGatewayRelease compiled,
            /**
             * 中文说明：保存 attemptNo 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayReleaseService.PreparedRelease} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by attempt no; its type is {@code int}, and {@code GatewayReleaseService.PreparedRelease} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseService.PreparedRelease} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService.PreparedRelease}; do not couple callers to its representation when the owning type exposes an API.
             */
            int attemptNo
    ) {
    }

    /**
     * 中文说明：{@code CreateRelease} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Create发布相关的职责与边界。
     * English summary: {@code CreateRelease} is an immutable data carrier in the current Gateway module; it owns the create release-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param expectedDraftRevision 参数 expected草稿Revision；parameter expected draft revision。
     * @param changeReason 参数 changeReason；parameter change reason。
     */
    public record CreateRelease(
            /**
             * 中文说明：保存 expected草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayReleaseService.CreateRelease} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected draft revision; its type is {@code long}, and {@code GatewayReleaseService.CreateRelease} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseService.CreateRelease} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService.CreateRelease}; do not couple callers to its representation when the owning type exposes an API.
             */
            long expectedDraftRevision,
            /**
             * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleaseService.CreateRelease} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code GatewayReleaseService.CreateRelease} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseService.CreateRelease} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService.CreateRelease}; do not couple callers to its representation when the owning type exposes an API.
             */
            String changeReason
    ) {
    }

    /**
     * 中文说明：{@code RollbackRelease} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Rollback发布相关的职责与边界。
     * English summary: {@code RollbackRelease} is an immutable data carrier in the current Gateway module; it owns the rollback release-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param sourceReleaseId 参数 source发布Id；parameter source release id。
     * @param expectedDraftRevision 参数 expected草稿Revision；parameter expected draft revision。
     * @param changeReason 参数 changeReason；parameter change reason。
     */
    public record RollbackRelease(
            /**
             * 中文说明：保存 source发布Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleaseService.RollbackRelease} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by source release id; its type is {@code String}, and {@code GatewayReleaseService.RollbackRelease} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseService.RollbackRelease} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService.RollbackRelease}; do not couple callers to its representation when the owning type exposes an API.
             */
            String sourceReleaseId,
            /**
             * 中文说明：保存 expected草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayReleaseService.RollbackRelease} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected draft revision; its type is {@code long}, and {@code GatewayReleaseService.RollbackRelease} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseService.RollbackRelease} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService.RollbackRelease}; do not couple callers to its representation when the owning type exposes an API.
             */
            long expectedDraftRevision,
            /**
             * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleaseService.RollbackRelease} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code GatewayReleaseService.RollbackRelease} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseService.RollbackRelease} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService.RollbackRelease}; do not couple callers to its representation when the owning type exposes an API.
             */
            String changeReason
    ) {
    }

    /**
     * 中文说明：{@code ReleaseView} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责发布View相关的职责与边界。
     * English summary: {@code ReleaseView} is an immutable data carrier in the current Gateway module; it owns the release view-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param draftRevision 参数 草稿Revision；parameter draft revision。
     * @param basedOnReleaseId 参数 basedOn发布Id；parameter based on release id。
     * @param rollbackOfReleaseId 参数 rollbackOf发布Id；parameter rollback of release id。
     * @param status 参数 status；parameter status。
     * @param partialApplied 参数 partialApplied；parameter partial applied。
     * @param changeId 参数 changeId；parameter change id。
     * @param validationReport 参数 validation报告；parameter validation report。
     * @param structuredDiff 参数 structuredDiff；parameter structured diff。
     * @param changeReason 参数 changeReason；parameter change reason。
     * @param createdAt 参数 createdAt；parameter created at。
     * @param updatedAt 参数 updatedAt；parameter updated at。
     * @param attempts 参数 attempts；parameter attempts。
     */
    public record ReleaseView(
            /**
             * 中文说明：保存 发布Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleaseService.ReleaseView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by release id; its type is {@code String}, and {@code GatewayReleaseService.ReleaseView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseService.ReleaseView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService.ReleaseView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String releaseId,
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleaseService.ReleaseView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code GatewayReleaseService.ReleaseView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseService.ReleaseView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService.ReleaseView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String gatewayGroupId,
            /**
             * 中文说明：保存 草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayReleaseService.ReleaseView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by draft revision; its type is {@code long}, and {@code GatewayReleaseService.ReleaseView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseService.ReleaseView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService.ReleaseView}; do not couple callers to its representation when the owning type exposes an API.
             */
            long draftRevision,
            /**
             * 中文说明：保存 basedOn发布Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleaseService.ReleaseView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by based on release id; its type is {@code String}, and {@code GatewayReleaseService.ReleaseView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseService.ReleaseView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService.ReleaseView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String basedOnReleaseId,
            /**
             * 中文说明：保存 rollbackOf发布Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleaseService.ReleaseView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by rollback of release id; its type is {@code String}, and {@code GatewayReleaseService.ReleaseView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseService.ReleaseView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService.ReleaseView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String rollbackOfReleaseId,
            /**
             * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code GatewayReleaseStatus}，由 {@code GatewayReleaseService.ReleaseView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code GatewayReleaseStatus}, and {@code GatewayReleaseService.ReleaseView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseService.ReleaseView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService.ReleaseView}; do not couple callers to its representation when the owning type exposes an API.
             */
            GatewayReleaseStatus status,
            /**
             * 中文说明：保存 partialApplied 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayReleaseService.ReleaseView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by partial applied; its type is {@code boolean}, and {@code GatewayReleaseService.ReleaseView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseService.ReleaseView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService.ReleaseView}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean partialApplied,
            /**
             * 中文说明：保存 changeId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleaseService.ReleaseView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change id; its type is {@code String}, and {@code GatewayReleaseService.ReleaseView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseService.ReleaseView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService.ReleaseView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String changeId,
            /**
             * 中文说明：保存 validation报告 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code GatewayReleaseService.ReleaseView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by validation report; its type is {@code Map<String, Object>}, and {@code GatewayReleaseService.ReleaseView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseService.ReleaseView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService.ReleaseView}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> validationReport,
            /**
             * 中文说明：保存 structuredDiff 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code GatewayReleaseService.ReleaseView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by structured diff; its type is {@code Map<String, Object>}, and {@code GatewayReleaseService.ReleaseView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseService.ReleaseView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService.ReleaseView}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> structuredDiff,
            /**
             * 中文说明：保存 changeReason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayReleaseService.ReleaseView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by change reason; its type is {@code String}, and {@code GatewayReleaseService.ReleaseView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseService.ReleaseView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService.ReleaseView}; do not couple callers to its representation when the owning type exposes an API.
             */
            String changeReason,
            /**
             * 中文说明：保存 createdAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayReleaseService.ReleaseView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by created at; its type is {@code Instant}, and {@code GatewayReleaseService.ReleaseView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseService.ReleaseView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService.ReleaseView}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant createdAt,
            /**
             * 中文说明：保存 updatedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayReleaseService.ReleaseView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by updated at; its type is {@code Instant}, and {@code GatewayReleaseService.ReleaseView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseService.ReleaseView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService.ReleaseView}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant updatedAt,
            /**
             * 中文说明：保存 attempts 对应的状态、依赖或配置值；字段类型为 {@code List<GatewayReleaseStore.AttemptRecord>}，由 {@code GatewayReleaseService.ReleaseView} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by attempts; its type is {@code List<GatewayReleaseStore.AttemptRecord>}, and {@code GatewayReleaseService.ReleaseView} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayReleaseService.ReleaseView} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReleaseService.ReleaseView}; do not couple callers to its representation when the owning type exposes an API.
             */
            List<GatewayReleaseStore.AttemptRecord> attempts
    ) {
    }
}
