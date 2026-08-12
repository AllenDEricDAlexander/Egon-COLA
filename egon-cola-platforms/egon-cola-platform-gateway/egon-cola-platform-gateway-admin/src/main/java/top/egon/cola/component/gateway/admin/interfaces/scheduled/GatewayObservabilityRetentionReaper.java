package top.egon.cola.component.gateway.admin.interfaces.scheduled;

import org.springframework.scheduling.annotation.Scheduled;
import top.egon.cola.component.gateway.admin.application.observability.GatewayCallEventIngestService;

/**
 * 中文说明：{@code GatewayObservabilityRetentionReaper} 是类型，位于当前 Gateway 模块的相关包中，负责网关可观测性RetentionReaper相关的职责与边界。
 * English summary: {@code GatewayObservabilityRetentionReaper} is a type in the current Gateway module; it owns the gateway observability retention reaper-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public class GatewayObservabilityRetentionReaper {

    /**
     * 中文说明：保存 服务 对应的状态、依赖或配置值；字段类型为 {@code GatewayCallEventIngestService}，由 {@code GatewayObservabilityRetentionReaper} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by service; its type is {@code GatewayCallEventIngestService}, and {@code GatewayObservabilityRetentionReaper} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayObservabilityRetentionReaper} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayObservabilityRetentionReaper}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayCallEventIngestService service;

    /**
     * 中文说明：创建 {@code GatewayObservabilityRetentionReaper} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayObservabilityRetentionReaper} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param service 参数 服务；parameter service。
     */
    public GatewayObservabilityRetentionReaper(
            GatewayCallEventIngestService service) {
        this.service = service;
    }

    /**
     * 中文说明：执行 reap 操作；该方法是 {@code GatewayObservabilityRetentionReaper} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the reap operation; this method is the invocation entry point on {@code GatewayObservabilityRetentionReaper} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayObservabilityRetentionReaper.reap(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    @Scheduled(
            fixedDelayString =
                    "${gateway.admin.observability.retention-reap-ms:3600000}"
    )
    public void reap() {
        service.purgeExpired();
    }
}
