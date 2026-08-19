package top.egon.cola.component.gateway.engine.rpc.adapter;

import top.egon.cola.component.gateway.engine.rpc.domain.RpcProviderChannelKey;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.engine.common.security.domain.GatewayTransportSecurity;

import javax.net.ssl.SSLException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * 中文说明：{@code RpcProviderChannelCache} 是类型，位于当前 Gateway 模块的相关包中，负责Rpc提供方通道Cache相关的职责与边界。
 * English summary: {@code RpcProviderChannelCache} is a type in the current Gateway module; it owns the rpc provider channel cache-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class RpcProviderChannelCache implements AutoCloseable {

    /**
     * 中文说明：保存 entries 对应的状态、依赖或配置值；字段类型为 {@code Map<RpcProviderChannelKey, Entry>}，由 {@code RpcProviderChannelCache} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by entries; its type is {@code Map<RpcProviderChannelKey, Entry>}, and {@code RpcProviderChannelCache} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcProviderChannelCache} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcProviderChannelCache}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Map<RpcProviderChannelKey, Entry> entries =
            new ConcurrentHashMap<>();

    /**
     * 中文说明：保存 工厂 对应的状态、依赖或配置值；字段类型为 {@code Function<RpcProviderChannelKey, ManagedChannel>}，由 {@code RpcProviderChannelCache} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by factory; its type is {@code Function<RpcProviderChannelKey, ManagedChannel>}, and {@code RpcProviderChannelCache} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcProviderChannelCache} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcProviderChannelCache}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Function<RpcProviderChannelKey, ManagedChannel> factory;

    /**
     * 中文说明：保存 drain超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code RpcProviderChannelCache} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by drain timeout; its type is {@code Duration}, and {@code RpcProviderChannelCache} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcProviderChannelCache} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcProviderChannelCache}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Duration drainTimeout;

    /**
     * 中文说明：创建 {@code RpcProviderChannelCache} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code RpcProviderChannelCache} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param drainTimeout 参数 drain超时；parameter drain timeout。
     */
    public RpcProviderChannelCache(Duration drainTimeout) {
        this(
                drainTimeout,
                GatewayTransportSecurity.developmentPlaintextConfig()
        );
    }

    /**
     * 中文说明：创建 {@code RpcProviderChannelCache} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code RpcProviderChannelCache} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param drainTimeout 参数 drain超时；parameter drain timeout。
     * @param transportSecurity 参数 传输安全；parameter transport security。
     */
    public RpcProviderChannelCache(
            Duration drainTimeout,
            GatewayTransportSecurity transportSecurity) {
        this(
                drainTimeout,
                key -> createChannel(key, transportSecurity)
        );
    }

    /**
     * 中文说明：创建 {@code RpcProviderChannelCache} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code RpcProviderChannelCache} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param drainTimeout 参数 drain超时；parameter drain timeout。
     * @param factory 参数 工厂；parameter factory。
     */
    RpcProviderChannelCache(
            Duration drainTimeout,
            Function<RpcProviderChannelKey, ManagedChannel> factory) {
        this.drainTimeout = drainTimeout;
        this.factory = factory;
    }

    /**
     * 中文说明：执行 acquire 操作；该方法是 {@code RpcProviderChannelCache} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the acquire operation; this method is the invocation entry point on {@code RpcProviderChannelCache} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcProviderChannelCache.acquire(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param provider 参数 提供方；parameter provider。
     * @return 返回 acquire 的处理结果；returns the result of the operation.
     */
    public ChannelHandle acquire(ProviderInstance provider) {
        RpcProviderChannelKey key = RpcProviderChannelKey.from(provider);
        Entry entry = entries.compute(key, (ignored, current) -> {
            Entry selected = current;
            if (selected == null || selected.draining.get()) {
                selected = new Entry(factory.apply(key));
            }
            selected.inFlight.incrementAndGet();
            return selected;
        });
        return new ChannelHandle(
                key,
                entry.channel,
                () -> release(key, entry)
        );
    }

    /**
     * 中文说明：执行 retainOnly 操作；该方法是 {@code RpcProviderChannelCache} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the retain only operation; this method is the invocation entry point on {@code RpcProviderChannelCache} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcProviderChannelCache.retainOnly(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param activeKeys 参数 activeKeys；parameter active keys。
     */
    public void retainOnly(Set<RpcProviderChannelKey> activeKeys) {
        entries.forEach((key, entry) -> {
            if (!activeKeys.contains(key)
                    && entry.draining.compareAndSet(false, true)) {
                if (entry.inFlight.get() == 0) {
                    closeEntry(key, entry);
                }
            }
        });
    }

    /**
     * 中文说明：执行 size 操作；该方法是 {@code RpcProviderChannelCache} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the size operation; this method is the invocation entry point on {@code RpcProviderChannelCache} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcProviderChannelCache.size(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 size 的处理结果；returns the result of the operation.
     */
    public int size() {
        return entries.size();
    }

    /**
     * 中文说明：执行 drainingCount 操作；该方法是 {@code RpcProviderChannelCache} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the draining count operation; this method is the invocation entry point on {@code RpcProviderChannelCache} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcProviderChannelCache.drainingCount(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 drainingCount 的处理结果；returns the result of the operation.
     */
    public long drainingCount() {
        return entries.values().stream()
                .filter(entry -> entry.draining.get())
                .count();
    }

    /**
     * 中文说明：执行 close 操作；该方法是 {@code RpcProviderChannelCache} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the close operation; this method is the invocation entry point on {@code RpcProviderChannelCache} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcProviderChannelCache.close(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    @Override
    public void close() {
        entries.forEach((key, entry) -> {
            entry.draining.set(true);
            closeEntry(key, entry);
        });
        entries.clear();
    }

    /**
     * 中文说明：执行 发布 操作；该方法是 {@code RpcProviderChannelCache} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the release operation; this method is the invocation entry point on {@code RpcProviderChannelCache} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcProviderChannelCache.release(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param key 参数 键；parameter key。
     * @param entry 参数 entry；parameter entry。
     */
    private void release(RpcProviderChannelKey key, Entry entry) {
        if (entry.inFlight.decrementAndGet() == 0 && entry.draining.get()) {
            closeEntry(key, entry);
        }
    }

    /**
     * 中文说明：执行 closeEntry 操作；该方法是 {@code RpcProviderChannelCache} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the close entry operation; this method is the invocation entry point on {@code RpcProviderChannelCache} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcProviderChannelCache.closeEntry(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param key 参数 键；parameter key。
     * @param entry 参数 entry；parameter entry。
     */
    private void closeEntry(RpcProviderChannelKey key, Entry entry) {
        if (entries.remove(key, entry)) {
            entry.channel.shutdown();
            try {
                if (!entry.channel.awaitTermination(
                        drainTimeout.toMillis(),
                        TimeUnit.MILLISECONDS
                )) {
                    entry.channel.shutdownNow();
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                entry.channel.shutdownNow();
            }
        }
    }

    /**
     * 中文说明：执行 create通道 操作；该方法是 {@code RpcProviderChannelCache} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create channel operation; this method is the invocation entry point on {@code RpcProviderChannelCache} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcProviderChannelCache.createChannel(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param key 参数 键；parameter key。
     * @param transportSecurity 参数 传输安全；parameter transport security。
     * @return 返回 create通道 的处理结果；returns the result of the operation.
     */
    private static ManagedChannel createChannel(
            RpcProviderChannelKey key,
            GatewayTransportSecurity transportSecurity) {
        NettyChannelBuilder builder = NettyChannelBuilder
                .forAddress(key.host(), key.port())
                .disableRetry();
        if (key.secure()) {
            if (!transportSecurity.enabled()) {
                throw new IllegalStateException(
                        "secure RPC Provider requires configured mTLS material"
                );
            }
            try {
                builder.sslContext(GrpcSslContexts.forClient()
                        .trustManager(
                                transportSecurity
                                        .trustCertificateCollectionFile()
                                        .toFile()
                        )
                        .keyManager(
                                transportSecurity
                                        .certificateChainFile()
                                        .toFile(),
                                transportSecurity
                                        .privateKeyFile()
                                        .toFile()
                        )
                        .build());
            } catch (SSLException failure) {
                throw new IllegalStateException(
                        "failed to configure RPC Provider mTLS",
                        failure
                );
            }
        } else {
            if (transportSecurity.enabled()) {
                throw new IllegalStateException(
                        "plaintext RPC Provider rejected while mTLS is enabled"
                );
            }
            builder.usePlaintext();
        }
        return builder.build();
    }

    /**
     * 中文说明：{@code Entry} 是类型，位于当前 Gateway 模块的相关包中，负责Entry相关的职责与边界。
     * English summary: {@code Entry} is a type in the current Gateway module; it owns the entry-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private static final class Entry {

        /**
         * 中文说明：保存 通道 对应的状态、依赖或配置值；字段类型为 {@code ManagedChannel}，由 {@code RpcProviderChannelCache.Entry} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by channel; its type is {@code ManagedChannel}, and {@code RpcProviderChannelCache.Entry} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcProviderChannelCache.Entry} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcProviderChannelCache.Entry}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final ManagedChannel channel;

        /**
         * 中文说明：保存 inFlight 对应的状态、依赖或配置值；字段类型为 {@code AtomicInteger}，由 {@code RpcProviderChannelCache.Entry} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by in flight; its type is {@code AtomicInteger}, and {@code RpcProviderChannelCache.Entry} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcProviderChannelCache.Entry} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcProviderChannelCache.Entry}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final AtomicInteger inFlight = new AtomicInteger();

        /**
         * 中文说明：保存 draining 对应的状态、依赖或配置值；字段类型为 {@code AtomicBoolean}，由 {@code RpcProviderChannelCache.Entry} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by draining; its type is {@code AtomicBoolean}, and {@code RpcProviderChannelCache.Entry} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcProviderChannelCache.Entry} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcProviderChannelCache.Entry}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final AtomicBoolean draining = new AtomicBoolean();

        /**
         * 中文说明：创建 {@code RpcProviderChannelCache.Entry} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code RpcProviderChannelCache.Entry} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param channel 参数 通道；parameter channel。
         */
        private Entry(ManagedChannel channel) {
            this.channel = channel;
        }
    }

    /**
     * 中文说明：{@code ChannelHandle} 是类型，位于当前 Gateway 模块的相关包中，负责通道Handle相关的职责与边界。
     * English summary: {@code ChannelHandle} is a type in the current Gateway module; it owns the channel handle-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    public static final class ChannelHandle implements AutoCloseable {

        /**
         * 中文说明：保存 键 对应的状态、依赖或配置值；字段类型为 {@code RpcProviderChannelKey}，由 {@code RpcProviderChannelCache.ChannelHandle} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by key; its type is {@code RpcProviderChannelKey}, and {@code RpcProviderChannelCache.ChannelHandle} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcProviderChannelCache.ChannelHandle} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcProviderChannelCache.ChannelHandle}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final RpcProviderChannelKey key;

        /**
         * 中文说明：保存 通道 对应的状态、依赖或配置值；字段类型为 {@code ManagedChannel}，由 {@code RpcProviderChannelCache.ChannelHandle} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by channel; its type is {@code ManagedChannel}, and {@code RpcProviderChannelCache.ChannelHandle} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcProviderChannelCache.ChannelHandle} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcProviderChannelCache.ChannelHandle}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final ManagedChannel channel;

        /**
         * 中文说明：保存 发布 对应的状态、依赖或配置值；字段类型为 {@code Runnable}，由 {@code RpcProviderChannelCache.ChannelHandle} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by release; its type is {@code Runnable}, and {@code RpcProviderChannelCache.ChannelHandle} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcProviderChannelCache.ChannelHandle} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcProviderChannelCache.ChannelHandle}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final Runnable release;

        /**
         * 中文说明：保存 closed 对应的状态、依赖或配置值；字段类型为 {@code AtomicBoolean}，由 {@code RpcProviderChannelCache.ChannelHandle} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by closed; its type is {@code AtomicBoolean}, and {@code RpcProviderChannelCache.ChannelHandle} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcProviderChannelCache.ChannelHandle} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcProviderChannelCache.ChannelHandle}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final AtomicBoolean closed = new AtomicBoolean();

        /**
         * 中文说明：创建 {@code RpcProviderChannelCache.ChannelHandle} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code RpcProviderChannelCache.ChannelHandle} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param key 参数 键；parameter key。
         * @param channel 参数 通道；parameter channel。
         * @param release 参数 发布；parameter release。
         */
        private ChannelHandle(
                RpcProviderChannelKey key,
                ManagedChannel channel,
                Runnable release) {
            this.key = key;
            this.channel = channel;
            this.release = release;
        }

        /**
         * 中文说明：执行 键 操作；该方法是 {@code RpcProviderChannelCache.ChannelHandle} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the key operation; this method is the invocation entry point on {@code RpcProviderChannelCache.ChannelHandle} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code RpcProviderChannelCache.ChannelHandle.key(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 键 的处理结果；returns the result of the operation.
         */
        public RpcProviderChannelKey key() {
            return key;
        }

        /**
         * 中文说明：执行 通道 操作；该方法是 {@code RpcProviderChannelCache.ChannelHandle} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the channel operation; this method is the invocation entry point on {@code RpcProviderChannelCache.ChannelHandle} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code RpcProviderChannelCache.ChannelHandle.channel(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 通道 的处理结果；returns the result of the operation.
         */
        public ManagedChannel channel() {
            return channel;
        }

        /**
         * 中文说明：执行 close 操作；该方法是 {@code RpcProviderChannelCache.ChannelHandle} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the close operation; this method is the invocation entry point on {@code RpcProviderChannelCache.ChannelHandle} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code RpcProviderChannelCache.ChannelHandle.close(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         */
        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                release.run();
            }
        }
    }
}
