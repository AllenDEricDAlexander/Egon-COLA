package top.egon.cola.component.gateway.engine.rpc.domain;

import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;

import java.util.Objects;

/**
 * 中文说明：{@code RpcProviderChannelKey} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Rpc提供方通道键相关的职责与边界。
 * English summary: {@code RpcProviderChannelKey} is an immutable data carrier in the current Gateway module; it owns the rpc provider channel key-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param serviceKey 参数 服务键；parameter service key。
 * @param instanceId 参数 instanceId；parameter instance id。
 * @param leaseId 参数 租约Id；parameter lease id。
 * @param host 参数 host；parameter host。
 * @param port 参数 port；parameter port。
 * @param secure 参数 secure；parameter secure。
 */
public record RpcProviderChannelKey(
        /**
         * 中文说明：保存 服务键 对应的状态、依赖或配置值；字段类型为 {@code ProviderServiceKey}，由 {@code RpcProviderChannelKey} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by service key; its type is {@code ProviderServiceKey}, and {@code RpcProviderChannelKey} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcProviderChannelKey} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcProviderChannelKey}; do not couple callers to its representation when the owning type exposes an API.
         */
        ProviderServiceKey serviceKey,
        /**
         * 中文说明：保存 instanceId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RpcProviderChannelKey} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by instance id; its type is {@code String}, and {@code RpcProviderChannelKey} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcProviderChannelKey} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcProviderChannelKey}; do not couple callers to its representation when the owning type exposes an API.
         */
        String instanceId,
        /**
         * 中文说明：保存 租约Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RpcProviderChannelKey} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by lease id; its type is {@code String}, and {@code RpcProviderChannelKey} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcProviderChannelKey} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcProviderChannelKey}; do not couple callers to its representation when the owning type exposes an API.
         */
        String leaseId,
        /**
         * 中文说明：保存 host 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RpcProviderChannelKey} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by host; its type is {@code String}, and {@code RpcProviderChannelKey} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcProviderChannelKey} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcProviderChannelKey}; do not couple callers to its representation when the owning type exposes an API.
         */
        String host,
        /**
         * 中文说明：保存 port 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code RpcProviderChannelKey} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by port; its type is {@code int}, and {@code RpcProviderChannelKey} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcProviderChannelKey} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcProviderChannelKey}; do not couple callers to its representation when the owning type exposes an API.
         */
        int port,
        /**
         * 中文说明：保存 secure 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code RpcProviderChannelKey} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by secure; its type is {@code boolean}, and {@code RpcProviderChannelKey} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcProviderChannelKey} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcProviderChannelKey}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean secure
) {

    /**
     * 中文说明：创建 {@code RpcProviderChannelKey} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code RpcProviderChannelKey} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param serviceKey 参数 服务键；parameter service key。
     * @param instanceId 参数 instanceId；parameter instance id。
     * @param leaseId 参数 租约Id；parameter lease id。
     * @param host 参数 host；parameter host。
     * @param port 参数 port；parameter port。
     * @param secure 参数 secure；parameter secure。
     */
    public RpcProviderChannelKey {
        serviceKey = Objects.requireNonNull(serviceKey, "serviceKey");
        instanceId = Objects.requireNonNull(instanceId, "instanceId");
        leaseId = Objects.requireNonNull(leaseId, "leaseId");
        host = Objects.requireNonNull(host, "host");
    }

    /**
     * 中文说明：执行 from 操作；该方法是 {@code RpcProviderChannelKey} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the from operation; this method is the invocation entry point on {@code RpcProviderChannelKey} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcProviderChannelKey.from(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param provider 参数 提供方；parameter provider。
     * @return 返回 from 的处理结果；returns the result of the operation.
     */
    public static RpcProviderChannelKey from(ProviderInstance provider) {
        return new RpcProviderChannelKey(
                provider.serviceKey(),
                provider.instanceId(),
                provider.leaseId(),
                provider.host(),
                provider.port(),
                provider.secure()
        );
    }
}
