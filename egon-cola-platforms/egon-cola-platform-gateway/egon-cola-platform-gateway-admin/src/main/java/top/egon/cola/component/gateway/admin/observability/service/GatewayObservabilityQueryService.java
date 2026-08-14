package top.egon.cola.component.gateway.admin.observability.service;


import top.egon.cola.component.gateway.admin.observability.repository.GatewayObservabilityRepository;
import top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionService;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

/**
 * 中文说明：{@code GatewayObservabilityQueryService} 是服务组件，位于当前 Gateway 模块的相关包中，负责网关可观测性Query服务相关的职责与边界。
 * English summary: {@code GatewayObservabilityQueryService} is a gateway observability query service service in the current Gateway module; it owns the gateway observability query service-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public class GatewayObservabilityQueryService {

    /**
     * 中文说明：保存 存储 对应的状态、依赖或配置值；字段类型为 {@code GatewayObservabilityRepository}，由 {@code GatewayObservabilityQueryService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by store; its type is {@code GatewayObservabilityRepository}, and {@code GatewayObservabilityQueryService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayObservabilityQueryService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityQueryService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayObservabilityRepository store;

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code GatewayObservabilityQueryService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code GatewayObservabilityQueryService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayObservabilityQueryService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityQueryService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：保存 projections 对应的状态、依赖或配置值；字段类型为 {@code GatewayProjectionService}，由 {@code GatewayObservabilityQueryService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by projections; its type is {@code GatewayProjectionService}, and {@code GatewayObservabilityQueryService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayObservabilityQueryService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityQueryService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayProjectionService projections;

    /**
     * 中文说明：创建 {@code GatewayObservabilityQueryService} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayObservabilityQueryService} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param store 参数 存储；parameter store。
     * @param clock 参数 clock；parameter clock。
     */
    public GatewayObservabilityQueryService(
            GatewayObservabilityRepository store,
            Clock clock) {
        this(store, clock, null);
    }

    /**
     * 中文说明：创建 {@code GatewayObservabilityQueryService} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayObservabilityQueryService} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param store 参数 存储；parameter store。
     * @param clock 参数 clock；parameter clock。
     * @param projections 参数 projections；parameter projections。
     */
    public GatewayObservabilityQueryService(
            GatewayObservabilityRepository store,
            Clock clock,
            GatewayProjectionService projections) {
        this.store = Objects.requireNonNull(store, "store");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.projections = projections;
    }

    /**
     * 中文说明：执行 traces 操作；该方法是 {@code GatewayObservabilityQueryService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the traces operation; this method is the invocation entry point on {@code GatewayObservabilityQueryService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayObservabilityQueryService.traces(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param query 参数 query；parameter query。
     * @return 返回 traces 的处理结果；returns the result of the operation.
     */
    public top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayPageVO<
            top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO> traces(
            top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayTraceQueryDTO query) {
        return store.traces(query);
    }

    /**
     * 中文说明：执行 dashboard 操作；该方法是 {@code GatewayObservabilityQueryService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the dashboard operation; this method is the invocation entry point on {@code GatewayObservabilityQueryService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayObservabilityQueryService.dashboard(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param bizCode 参数 bizCode；parameter biz code。
     * @param appCode 参数 appCode；parameter app code。
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @return 返回 dashboard 的处理结果；returns the result of the operation.
     */
    public top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO dashboard(
            String bizCode,
            String appCode,
            String env,
            String namespace) {
        top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO summary = store.dashboard(
                env,
                namespace,
                clock.instant().minus(Duration.ofHours(1))
        );
        if (projections == null) {
            return summary;
        }
        try {
            top.egon.cola.component.gateway.admin.runtime.service.GatewayProjectionCounts counts =
                    projections.scopeCounts(
                            bizCode,
                            appCode,
                            env,
                            namespace
                    );
            return new top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO(
                    summary.gatewayGroups(),
                    counts.readyEngines(),
                    counts.totalEngines(),
                    counts.inconsistentGroups(),
                    counts.activeProviders(),
                    counts.abnormalProviders(),
                    summary.releaseSuccessRate(),
                    summary.requestSeries(),
                    summary.protocolCalls(),
                    counts.stale()
                            ? "PROJECTION_STALE"
                            : summary.observabilityState()
            );
        } catch (RuntimeException ignored) {
            return new top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO(
                    summary.gatewayGroups(),
                    summary.readyEngines(),
                    summary.totalEngines(),
                    summary.inconsistentGroups(),
                    summary.activeProviders(),
                    summary.abnormalProviders(),
                    summary.releaseSuccessRate(),
                    summary.requestSeries(),
                    summary.protocolCalls(),
                    "PROJECTION_UNAVAILABLE"
            );
        }
    }

    /**
     * 中文说明：执行 audits 操作；该方法是 {@code GatewayObservabilityQueryService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the audits operation; this method is the invocation entry point on {@code GatewayObservabilityQueryService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayObservabilityQueryService.audits(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param query 参数 query；parameter query。
     * @return 返回 audits 的处理结果；returns the result of the operation.
     */
    public top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayPageVO<
            top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO> audits(
            top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO query) {
        return store.audits(query);
    }
}
