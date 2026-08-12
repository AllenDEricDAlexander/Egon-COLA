package top.egon.cola.component.gateway.engine.discovery;

import top.egon.cola.component.ddc.model.registry.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceInstance;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.ddc.api.registry.DdcRegistrySubscription;
import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.gateway.core.provider.ProviderCatalogSnapshot;
import top.egon.cola.component.gateway.core.provider.ProviderHealthState;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.provider.ProviderQuery;
import top.egon.cola.component.gateway.core.provider.ProviderRegistryState;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.provider.ProviderServiceRegistry;
import top.egon.cola.component.gateway.core.provider.ProviderServiceSnapshot;
import top.egon.cola.component.gateway.core.provider.ProviderSubscription;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 中文说明：{@code DdcProviderServiceRegistryAdapter} 是适配器，位于当前 Gateway 模块的相关包中，负责Ddc提供方服务注册表Adapter相关的职责与边界。
 * English summary: {@code DdcProviderServiceRegistryAdapter} is a ddc provider service registry adapter adapter in the current Gateway module; it owns the ddc provider service registry adapter-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class DdcProviderServiceRegistryAdapter
        implements ProviderServiceRegistry {

    /**
     * 中文说明：保存 delegate 对应的状态、依赖或配置值；字段类型为 {@code DdcServiceRegistryClient}，由 {@code DdcProviderServiceRegistryAdapter} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by delegate; its type is {@code DdcServiceRegistryClient}, and {@code DdcProviderServiceRegistryAdapter} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DdcProviderServiceRegistryAdapter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DdcProviderServiceRegistryAdapter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final DdcServiceRegistryClient delegate;

    /**
     * 中文说明：创建 {@code DdcProviderServiceRegistryAdapter} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code DdcProviderServiceRegistryAdapter} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param delegate 参数 delegate；parameter delegate。
     */
    public DdcProviderServiceRegistryAdapter(
            DdcServiceRegistryClient delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * 中文说明：执行 get服务Keys 操作；该方法是 {@code DdcProviderServiceRegistryAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get service keys operation; this method is the invocation entry point on {@code DdcProviderServiceRegistryAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DdcProviderServiceRegistryAdapter.getServiceKeys(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param query 参数 query；parameter query。
     * @return 返回 get服务Keys 的处理结果；returns the result of the operation.
     */
    @Override
    public ProviderCatalogSnapshot getServiceKeys(ProviderQuery query) {
        List<ProviderServiceKey> keys = new ArrayList<>();
        long revision = 0;
        java.time.Instant observedAt = java.time.Instant.EPOCH;
        for (DdcServiceQuery ddcQuery : queries(query)) {
            DdcServiceCatalogSnapshot snapshot =
                    delegate.getServiceKeys(ddcQuery);
            keys.addAll(snapshot.serviceKeys().stream()
                    .map(key -> serviceKey(key, query.namespace()))
                    .toList());
            revision = Math.max(revision, snapshot.revision());
            if (snapshot.observedAt().isAfter(observedAt)) {
                observedAt = snapshot.observedAt();
            }
        }
        return new ProviderCatalogSnapshot(revision, observedAt, keys);
    }

    /**
     * 中文说明：执行 getInstances 操作；该方法是 {@code DdcProviderServiceRegistryAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get instances operation; this method is the invocation entry point on {@code DdcProviderServiceRegistryAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DdcProviderServiceRegistryAdapter.getInstances(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param key 参数 键；parameter key。
     * @return 返回 getInstances 的处理结果；returns the result of the operation.
     */
    @Override
    public ProviderServiceSnapshot getInstances(ProviderServiceKey key) {
        return snapshot(delegate.getInstances(ddcKey(key)), key);
    }

    /**
     * 中文说明：执行 subscribeServices 操作；该方法是 {@code DdcProviderServiceRegistryAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the subscribe services operation; this method is the invocation entry point on {@code DdcProviderServiceRegistryAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DdcProviderServiceRegistryAdapter.subscribeServices(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param query 参数 query；parameter query。
     * @param listener 参数 监听器；parameter listener。
     * @return 返回 subscribeServices 的处理结果；returns the result of the operation.
     */
    @Override
    public ProviderSubscription subscribeServices(
            ProviderQuery query,
            ProviderCatalogListener listener) {
        List<DdcRegistrySubscription> subscriptions = queries(query)
                .stream()
                .map(ddcQuery -> delegate.subscribeServices(
                        ddcQuery,
                        ignored -> listener.onSnapshot(getServiceKeys(query))
                ))
                .toList();
        return composite(subscriptions);
    }

    /**
     * 中文说明：执行 subscribe 操作；该方法是 {@code DdcProviderServiceRegistryAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the subscribe operation; this method is the invocation entry point on {@code DdcProviderServiceRegistryAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DdcProviderServiceRegistryAdapter.subscribe(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param key 参数 键；parameter key。
     * @param listener 参数 监听器；parameter listener。
     * @return 返回 subscribe 的处理结果；returns the result of the operation.
     */
    @Override
    public ProviderSubscription subscribe(
            ProviderServiceKey key,
            ProviderSnapshotListener listener) {
        DdcRegistrySubscription subscription = delegate.subscribe(
                ddcKey(key),
                value -> listener.onSnapshot(snapshot(value, key))
        );
        return composite(List.of(subscription));
    }

    /**
     * 中文说明：执行 queries 操作；该方法是 {@code DdcProviderServiceRegistryAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the queries operation; this method is the invocation entry point on {@code DdcProviderServiceRegistryAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DdcProviderServiceRegistryAdapter.queries(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param query 参数 query；parameter query。
     * @return 返回 queries 的处理结果；returns the result of the operation.
     */
    private List<DdcServiceQuery> queries(ProviderQuery query) {
        if (query.protocolType() == ProviderProtocolType.RPC) {
            return List.of(query(query, DdcServiceKind.RPC_PROVIDER, "grpc"));
        }
        if (query.protocolType() == ProviderProtocolType.HTTP) {
            return List.of(
                    query(query, DdcServiceKind.HTTP_PROVIDER, "http"),
                    query(query, DdcServiceKind.HTTP_PROVIDER, "https")
            );
        }
        return List.of(
                query(query, DdcServiceKind.HTTP_PROVIDER, "http"),
                query(query, DdcServiceKind.HTTP_PROVIDER, "https"),
                query(query, DdcServiceKind.RPC_PROVIDER, "grpc")
        );
    }

    /**
     * 中文说明：执行 query 操作；该方法是 {@code DdcProviderServiceRegistryAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the query operation; this method is the invocation entry point on {@code DdcProviderServiceRegistryAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DdcProviderServiceRegistryAdapter.query(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param query 参数 query；parameter query。
     * @param kind 参数 kind；parameter kind。
     * @param protocol 参数 protocol；parameter protocol。
     * @return 返回 query 的处理结果；returns the result of the operation.
     */
    private DdcServiceQuery query(
            ProviderQuery query,
            DdcServiceKind kind,
            String protocol) {
        return new DdcServiceQuery(
                query.bizCode(),
                query.env(),
                query.appCode(),
                kind,
                protocol,
                null,
                null,
                null
        );
    }

    /**
     * 中文说明：执行 snapshot 操作；该方法是 {@code DdcProviderServiceRegistryAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the snapshot operation; this method is the invocation entry point on {@code DdcProviderServiceRegistryAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DdcProviderServiceRegistryAdapter.snapshot(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param key 参数 键；parameter key。
     * @return 返回 snapshot 的处理结果；returns the result of the operation.
     */
    private ProviderServiceSnapshot snapshot(
            DdcServiceSnapshot value,
            ProviderServiceKey key) {
        Instant now = Instant.now();
        return new ProviderServiceSnapshot(
                key,
                value.revision(),
                value.observedAt(),
                value.instances().stream()
                        .map(instance -> instance(key, instance, now))
                        .toList()
        );
    }

    /**
     * 中文说明：执行 instance 操作；该方法是 {@code DdcProviderServiceRegistryAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the instance operation; this method is the invocation entry point on {@code DdcProviderServiceRegistryAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DdcProviderServiceRegistryAdapter.instance(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param key 参数 键；parameter key。
     * @param value 参数 值；parameter value。
     * @param now 参数 now；parameter now。
     * @return 返回 instance 的处理结果；returns the result of the operation.
     */
    private ProviderInstance instance(
            ProviderServiceKey key,
            DdcServiceInstance value,
            Instant now) {
        return new ProviderInstance(
                key,
                value.instanceId(),
                value.leaseId(),
                value.host(),
                value.port(),
                value.secure(),
                value.metadata(),
                value.leaseExpireAt(),
                value.normalizedStatus().isAvailable(
                        now,
                        value.leaseExpireAt()
                )
                        ? ProviderRegistryState.REGISTERED
                        : ProviderRegistryState.EXPIRED,
                ProviderHealthState.UNKNOWN,
                ProviderHealthState.UNKNOWN
        );
    }

    /**
     * 中文说明：执行 服务键 操作；该方法是 {@code DdcProviderServiceRegistryAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the service key operation; this method is the invocation entry point on {@code DdcProviderServiceRegistryAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DdcProviderServiceRegistryAdapter.serviceKey(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param key 参数 键；parameter key。
     * @param namespace 参数 命名空间；parameter namespace。
     * @return 返回 服务键 的处理结果；returns the result of the operation.
     */
    private ProviderServiceKey serviceKey(
            DdcServiceKey key,
            String namespace) {
        return new ProviderServiceKey(
                key.bizCode(),
                key.appCode(),
                key.env(),
                namespace,
                key.serviceKind() == DdcServiceKind.HTTP_PROVIDER
                        ? ProviderProtocolType.HTTP
                        : ProviderProtocolType.RPC,
                key.serviceName(),
                key.group(),
                key.version(),
                key.protocol()
        );
    }

    /**
     * 中文说明：执行 ddc键 操作；该方法是 {@code DdcProviderServiceRegistryAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the ddc key operation; this method is the invocation entry point on {@code DdcProviderServiceRegistryAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DdcProviderServiceRegistryAdapter.ddcKey(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param key 参数 键；parameter key。
     * @return 返回 ddc键 的处理结果；returns the result of the operation.
     */
    private DdcServiceKey ddcKey(ProviderServiceKey key) {
        return new DdcServiceKey(
                key.bizCode(),
                key.env(),
                key.appCode(),
                key.protocolType() == ProviderProtocolType.HTTP
                        ? DdcServiceKind.HTTP_PROVIDER
                        : DdcServiceKind.RPC_PROVIDER,
                key.serviceName(),
                key.group(),
                key.version(),
                key.transport()
        );
    }

    /**
     * 中文说明：执行 composite 操作；该方法是 {@code DdcProviderServiceRegistryAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the composite operation; this method is the invocation entry point on {@code DdcProviderServiceRegistryAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DdcProviderServiceRegistryAdapter.composite(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param subscriptions 参数 subscriptions；parameter subscriptions。
     * @return 返回 composite 的处理结果；returns the result of the operation.
     */
    private ProviderSubscription composite(
            List<DdcRegistrySubscription> subscriptions) {
        return new ProviderSubscription() {
            /**
             * 中文说明：保存 active 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code DdcProviderServiceRegistryAdapter} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by active; its type is {@code boolean}, and {@code DdcProviderServiceRegistryAdapter} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code DdcProviderServiceRegistryAdapter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DdcProviderServiceRegistryAdapter}; do not couple callers to its representation when the owning type exposes an API.
             */
            private volatile boolean active = true;

            /**
             * 中文说明：执行 active 操作；该方法是 {@code DdcProviderServiceRegistryAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the active operation; this method is the invocation entry point on {@code DdcProviderServiceRegistryAdapter} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code DdcProviderServiceRegistryAdapter.active(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             * @return 返回 active 的处理结果；returns the result of the operation.
             */
            @Override
            public boolean active() {
                return active;
            }

            /**
             * 中文说明：执行 close 操作；该方法是 {@code DdcProviderServiceRegistryAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the close operation; this method is the invocation entry point on {@code DdcProviderServiceRegistryAdapter} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code DdcProviderServiceRegistryAdapter.close(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             */
            @Override
            public void close() {
                if (active) {
                    active = false;
                    subscriptions.forEach(DdcRegistrySubscription::close);
                }
            }
        };
    }
}
