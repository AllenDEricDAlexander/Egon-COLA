package top.egon.cola.component.gateway.admin.reporting.controller.scheduled;


import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.api.client.DdcManagementClient;
import top.egon.cola.component.gateway.admin.application.domain.po.GatewayApplicationPO;
import top.egon.cola.component.gateway.admin.application.repository.GatewayApplicationRepository;
import top.egon.cola.component.gateway.admin.observability.domain.po.GatewayAuditLogPO;
import top.egon.cola.component.gateway.admin.observability.repository.GatewayAuditLogRepository;
import top.egon.cola.component.gateway.admin.reporting.domain.dto.GatewayDefinitionLifecycleScopeDTO;
import top.egon.cola.component.gateway.admin.reporting.repository.GatewayDefinitionLifecycleRepository;
import top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionService;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 中文说明：{@code GatewayDefinitionLifecycleReconciler} 是类型，位于当前 Gateway 模块的相关包中，负责网关定义生命周期Reconciler相关的职责与边界。
 * English summary: {@code GatewayDefinitionLifecycleReconciler} is a type in the current Gateway module; it owns the gateway definition lifecycle reconciler-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Component
public class GatewayDefinitionLifecycleReconciler {

    /**
     * 中文说明：保存 客户端 对应的状态、依赖或配置值；字段类型为 {@code DdcManagementClient}，由 {@code GatewayDefinitionLifecycleReconciler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by client; its type is {@code DdcManagementClient}, and {@code GatewayDefinitionLifecycleReconciler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDefinitionLifecycleReconciler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDefinitionLifecycleReconciler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final DdcManagementClient client;

    /**
     * 中文说明：保存 applications 对应的状态、依赖或配置值；字段类型为 {@code GatewayApplicationRepository}，由 {@code GatewayDefinitionLifecycleReconciler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by applications; its type is {@code GatewayApplicationRepository}, and {@code GatewayDefinitionLifecycleReconciler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDefinitionLifecycleReconciler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDefinitionLifecycleReconciler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayApplicationRepository applications;

    /**
     * 中文说明：保存 projections 对应的状态、依赖或配置值；字段类型为 {@code GatewayProjectionService}，由 {@code GatewayDefinitionLifecycleReconciler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by projections; its type is {@code GatewayProjectionService}, and {@code GatewayDefinitionLifecycleReconciler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDefinitionLifecycleReconciler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDefinitionLifecycleReconciler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayProjectionService projections;

    /**
     * 中文说明：保存 生命周期 对应的状态、依赖或配置值；字段类型为 {@code GatewayDefinitionLifecycleRepository}，由 {@code GatewayDefinitionLifecycleReconciler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by lifecycle; its type is {@code GatewayDefinitionLifecycleRepository}, and {@code GatewayDefinitionLifecycleReconciler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDefinitionLifecycleReconciler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDefinitionLifecycleReconciler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayDefinitionLifecycleRepository lifecycle;

    /**
     * 中文说明：保存 audits 对应的状态、依赖或配置值；字段类型为 {@code GatewayAuditLogRepository}，由 {@code GatewayDefinitionLifecycleReconciler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by audits; its type is {@code GatewayAuditLogRepository}, and {@code GatewayDefinitionLifecycleReconciler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDefinitionLifecycleReconciler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDefinitionLifecycleReconciler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayAuditLogRepository audits;

    /**
     * 中文说明：保存 transactions 对应的状态、依赖或配置值；字段类型为 {@code TransactionTemplate}，由 {@code GatewayDefinitionLifecycleReconciler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by transactions; its type is {@code TransactionTemplate}, and {@code GatewayDefinitionLifecycleReconciler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDefinitionLifecycleReconciler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDefinitionLifecycleReconciler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final TransactionTemplate transactions;

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code GatewayDefinitionLifecycleReconciler} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code GatewayDefinitionLifecycleReconciler} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDefinitionLifecycleReconciler} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDefinitionLifecycleReconciler}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：创建 {@code GatewayDefinitionLifecycleReconciler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayDefinitionLifecycleReconciler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param client 参数 客户端；parameter client。
     * @param applications 参数 applications；parameter applications。
     * @param projections 参数 projections；parameter projections。
     * @param lifecycle 参数 生命周期；parameter lifecycle。
     * @param audits 参数 audits；parameter audits。
     * @param transactions 参数 transactions；parameter transactions。
     */
    @Autowired
    public GatewayDefinitionLifecycleReconciler(
            ObjectProvider<DdcManagementClient> client,
            GatewayApplicationRepository applications,
            GatewayProjectionService projections,
            GatewayDefinitionLifecycleRepository lifecycle,
            GatewayAuditLogRepository audits,
            TransactionTemplate transactions) {
        this(
                client.getIfAvailable(),
                applications,
                projections,
                lifecycle,
                audits,
                transactions,
                Clock.systemUTC()
        );
    }

    /**
     * 中文说明：创建 {@code GatewayDefinitionLifecycleReconciler} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayDefinitionLifecycleReconciler} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param client 参数 客户端；parameter client。
     * @param applications 参数 applications；parameter applications。
     * @param projections 参数 projections；parameter projections。
     * @param lifecycle 参数 生命周期；parameter lifecycle。
     * @param audits 参数 audits；parameter audits。
     * @param transactions 参数 transactions；parameter transactions。
     * @param clock 参数 clock；parameter clock。
     */
    GatewayDefinitionLifecycleReconciler(
            DdcManagementClient client,
            GatewayApplicationRepository applications,
            GatewayProjectionService projections,
            GatewayDefinitionLifecycleRepository lifecycle,
            GatewayAuditLogRepository audits,
            TransactionTemplate transactions,
            Clock clock) {
        this.client = client;
        this.applications = applications;
        this.projections = projections;
        this.lifecycle = lifecycle;
        this.audits = audits;
        this.transactions = transactions;
        this.clock = clock;
    }

    /**
     * 中文说明：执行 reconcile 操作；该方法是 {@code GatewayDefinitionLifecycleReconciler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the reconcile operation; this method is the invocation entry point on {@code GatewayDefinitionLifecycleReconciler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionLifecycleReconciler.reconcile(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    @Scheduled(
            fixedDelayString =
                    "${gateway.admin.definition-reconcile-delay:30000}"
    )
    public void reconcile() {
        if (client == null) {
            return;
        }
        Instant now = clock.instant();
        Set<GatewayDefinitionLifecycleScopeDTO> scopes = new LinkedHashSet<>();
        for (GatewayApplicationPO application
                : applications.findAllByDeletedFalseOrderByCreatedAtDesc()) {
            scopes.add(new GatewayDefinitionLifecycleScopeDTO(
                    application.getBizCode(),
                    application.getApplicationCode(),
                    application.getEnv(),
                    application.getNamespace()
            ));
        }
        Set<String> activeDefinitionSets = new LinkedHashSet<>();
        for (GatewayDefinitionLifecycleScopeDTO scope : scopes) {
            top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProjectionEnvelopeVO<
                    List<top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO>>
                    providers;
            try {
                providers = projections.instances(
                        scope.bizCode(),
                        scope.appCode(),
                        scope.env(),
                        scope.namespace()
                );
            } catch (RuntimeException unavailable) {
                return;
            }
            if (providers.stale()) {
                return;
            }
            providers.value().stream()
                    .filter(provider -> online(provider, now))
                    .map(top.egon.cola.component.gateway.admin.runtime.domain.vo
                            .GatewayProviderInstanceVO::definitionSetId)
                    .filter(value -> value != null && !value.isBlank())
                    .forEach(activeDefinitionSets::add);
        }
        transactions.executeWithoutResult(status -> {
            top.egon.cola.component.gateway.admin.reporting.domain.vo.GatewayReconcileResultVO result =
                    lifecycle.reconcile(activeDefinitionSets, now);
            if (result.changed()) {
                audits.save(audit(activeDefinitionSets, result, now));
            }
        });
    }

    /**
     * 中文说明：执行 online 操作；该方法是 {@code GatewayDefinitionLifecycleReconciler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the online operation; this method is the invocation entry point on {@code GatewayDefinitionLifecycleReconciler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionLifecycleReconciler.online(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param provider 参数 提供方；parameter provider。
     * @param now 参数 now；parameter now。
     * @return 返回 online 的处理结果；returns the result of the operation.
     */
    private boolean online(
            top.egon.cola.component.gateway.admin.runtime.domain.vo.GatewayProviderInstanceVO provider,
            Instant now) {
        return "ONLINE".equals(provider.status())
                && provider.expireAt() != null
                && provider.expireAt().isAfter(now);
    }

    /**
     * 中文说明：执行 审计 操作；该方法是 {@code GatewayDefinitionLifecycleReconciler} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the audit operation; this method is the invocation entry point on {@code GatewayDefinitionLifecycleReconciler} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionLifecycleReconciler.audit(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param activeDefinitionSets 参数 active定义Sets；parameter active definition sets。
     * @param result 参数 result；parameter result。
     * @param now 参数 now；parameter now。
     * @return 返回 审计 的处理结果；returns the result of the operation.
     */
    private GatewayAuditLogPO audit(
            Set<String> activeDefinitionSets,
            top.egon.cola.component.gateway.admin.reporting.domain.vo.GatewayReconcileResultVO result,
            Instant now) {
        return new GatewayAuditLogPO(
                UuidV7.simpleString(),
                "gateway-definition-reconciler",
                "SYSTEM",
                "SCHEDULED_RECONCILER",
                null,
                null,
                "DEFINITION_SET",
                "provider-active-sets",
                "RECONCILE_PROVIDER_LIFECYCLE",
                Map.of(),
                Map.of(
                        "activeDefinitionSetIds",
                        List.copyOf(activeDefinitionSets),
                        "activatedDefinitionSets",
                        result.activatedDefinitionSets(),
                        "retiredDefinitionSets",
                        result.retiredDefinitionSets(),
                        "activatedOperations",
                        result.activatedOperations(),
                        "offlinedOperations",
                        result.offlinedOperations()
                ),
                null,
                null,
                true,
                null,
                now
        );
    }


}
