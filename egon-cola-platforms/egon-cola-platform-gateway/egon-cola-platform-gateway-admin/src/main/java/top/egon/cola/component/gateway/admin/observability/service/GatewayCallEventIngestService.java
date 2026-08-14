package top.egon.cola.component.gateway.admin.observability.service;


import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.gateway.admin.observability.repository.GatewayObservabilityRepository;
import top.egon.cola.component.gateway.contract.observability.GatewayCallEventV1;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 中文说明：{@code GatewayCallEventIngestService} 是服务组件，位于当前 Gateway 模块的相关包中，负责网关调用事件Ingest服务相关的职责与边界。
 * English summary: {@code GatewayCallEventIngestService} is a gateway call event ingest service service in the current Gateway module; it owns the gateway call event ingest service-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public class GatewayCallEventIngestService {

    /**
     * 中文说明：保存 存储 对应的状态、依赖或配置值；字段类型为 {@code GatewayObservabilityRepository}，由 {@code GatewayCallEventIngestService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by store; its type is {@code GatewayObservabilityRepository}, and {@code GatewayCallEventIngestService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallEventIngestService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventIngestService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayObservabilityRepository store;

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code GatewayCallEventIngestService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code GatewayCallEventIngestService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallEventIngestService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventIngestService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：保存 retention 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayCallEventIngestService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by retention; its type is {@code Duration}, and {@code GatewayCallEventIngestService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallEventIngestService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventIngestService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Duration retention;

    /**
     * 中文说明：创建 {@code GatewayCallEventIngestService} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayCallEventIngestService} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param store 参数 存储；parameter store。
     * @param clock 参数 clock；parameter clock。
     * @param retention 参数 retention；parameter retention。
     */
    public GatewayCallEventIngestService(
            GatewayObservabilityRepository store,
            Clock clock,
            Duration retention) {
        this.store = Objects.requireNonNull(store, "store");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.retention = Objects.requireNonNull(retention, "retention");
    }

    /**
     * 中文说明：执行 ingest 操作；该方法是 {@code GatewayCallEventIngestService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the ingest operation; this method is the invocation entry point on {@code GatewayCallEventIngestService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallEventIngestService.ingest(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param event 参数 事件；parameter event。
     * @return 返回 ingest 的处理结果；returns the result of the operation.
     */
    @Transactional
    public boolean ingest(GatewayCallEventV1 event) {
        return store.project(
                event,
                Instant.ofEpochMilli(event.occurredAt()).plus(retention)
        );
    }

    /**
     * 中文说明：执行 poison 操作；该方法是 {@code GatewayCallEventIngestService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the poison operation; this method is the invocation entry point on {@code GatewayCallEventIngestService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallEventIngestService.poison(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param failure 参数 failure；parameter failure。
     */
    @Transactional
    public void poison(
            top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO failure) {
        store.recordFailure(failure);
    }

    /**
     * 中文说明：执行 purgeExpired 操作；该方法是 {@code GatewayCallEventIngestService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the purge expired operation; this method is the invocation entry point on {@code GatewayCallEventIngestService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallEventIngestService.purgeExpired(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 purgeExpired 的处理结果；returns the result of the operation.
     */
    @Transactional
    public int purgeExpired() {
        return store.deleteExpired(clock.instant());
    }
}
