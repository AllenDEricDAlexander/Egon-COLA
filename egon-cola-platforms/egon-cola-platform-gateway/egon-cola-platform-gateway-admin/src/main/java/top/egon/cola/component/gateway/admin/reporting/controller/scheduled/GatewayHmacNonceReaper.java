package top.egon.cola.component.gateway.admin.reporting.controller.scheduled;


import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.egon.cola.component.gateway.admin.reporting.repository.GatewayHmacNonceRepository;

import java.time.Clock;

/**
 * 中文说明：{@code GatewayHmacNonceReaper} 是类型，位于当前 Gateway 模块的相关包中，负责网关HmacNonceReaper相关的职责与边界。
 * English summary: {@code GatewayHmacNonceReaper} is a type in the current Gateway module; it owns the gateway hmac nonce reaper-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Component
public class GatewayHmacNonceReaper {

    /**
     * 中文说明：保存 nonces 对应的状态、依赖或配置值；字段类型为 {@code GatewayHmacNonceRepository}，由 {@code GatewayHmacNonceReaper} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by nonces; its type is {@code GatewayHmacNonceRepository}, and {@code GatewayHmacNonceReaper} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayHmacNonceReaper} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHmacNonceReaper}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayHmacNonceRepository nonces;

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code GatewayHmacNonceReaper} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code GatewayHmacNonceReaper} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayHmacNonceReaper} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayHmacNonceReaper}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock = Clock.systemUTC();

    /**
     * 中文说明：创建 {@code GatewayHmacNonceReaper} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayHmacNonceReaper} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param nonces 参数 nonces；parameter nonces。
     */
    public GatewayHmacNonceReaper(GatewayHmacNonceRepository nonces) {
        this.nonces = nonces;
    }

    /**
     * 中文说明：执行 reap 操作；该方法是 {@code GatewayHmacNonceReaper} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the reap operation; this method is the invocation entry point on {@code GatewayHmacNonceReaper} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayHmacNonceReaper.reap(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    @Scheduled(fixedDelayString = "${gateway.admin.hmac.reap-delay:PT5M}")
    public void reap() {
        nonces.deleteExpired(clock.instant());
    }
}
