package top.egon.cola.component.gateway.engine.discovery;

import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.provider.ProviderServiceRegistry;
import top.egon.cola.component.gateway.core.provider.ProviderServiceSnapshot;
import top.egon.cola.component.gateway.core.provider.ProviderSubscription;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 中文说明：{@code ProviderDirectory} 是类型，位于当前 Gateway 模块的相关包中，负责提供方Directory相关的职责与边界。
 * English summary: {@code ProviderDirectory} is a type in the current Gateway module; it owns the provider directory-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class ProviderDirectory implements AutoCloseable {

    /**
     * 中文说明：保存 注册表 对应的状态、依赖或配置值；字段类型为 {@code ProviderServiceRegistry}，由 {@code ProviderDirectory} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by registry; its type is {@code ProviderServiceRegistry}, and {@code ProviderDirectory} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderDirectory} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderDirectory}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ProviderServiceRegistry registry;

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code ProviderDirectory} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code ProviderDirectory} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderDirectory} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderDirectory}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：保存 snapshot 对应的状态、依赖或配置值；字段类型为 {@code AtomicReference<Map<ProviderServiceKey, ProviderServiceSnapshot>>}，由 {@code ProviderDirectory} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by snapshot; its type is {@code AtomicReference<Map<ProviderServiceKey, ProviderServiceSnapshot>>}, and {@code ProviderDirectory} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderDirectory} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderDirectory}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicReference<Map<ProviderServiceKey, ProviderServiceSnapshot>>
            snapshot = new AtomicReference<>(Map.of());

    /**
     * 中文说明：保存 subscriptions 对应的状态、依赖或配置值；字段类型为 {@code Map<ProviderServiceKey, SubscriptionRef>}，由 {@code ProviderDirectory} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by subscriptions; its type is {@code Map<ProviderServiceKey, SubscriptionRef>}, and {@code ProviderDirectory} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderDirectory} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderDirectory}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Map<ProviderServiceKey, SubscriptionRef> subscriptions =
            new LinkedHashMap<>();

    /**
     * 中文说明：创建 {@code ProviderDirectory} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code ProviderDirectory} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param registry 参数 注册表；parameter registry。
     * @param clock 参数 clock；parameter clock。
     */
    public ProviderDirectory(ProviderServiceRegistry registry, Clock clock) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * 中文说明：执行 activate 操作；该方法是 {@code ProviderDirectory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the activate operation; this method is the invocation entry point on {@code ProviderDirectory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderDirectory.activate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serviceKeys 参数 服务Keys；parameter service keys。
     */
    public synchronized void activate(Set<ProviderServiceKey> serviceKeys) {
        Set<ProviderServiceKey> required = Set.copyOf(serviceKeys);
        Map<ProviderServiceKey, ProviderServiceSnapshot> prepared =
                new LinkedHashMap<>(snapshot.get());
        Map<ProviderServiceKey, ProviderSubscription> created =
                new LinkedHashMap<>();
        try {
            for (ProviderServiceKey key : required) {
                SubscriptionRef existing = subscriptions.get(key);
                if (existing != null) {
                    existing.references++;
                    continue;
                }
                ProviderServiceSnapshot initial = registry.getInstances(key);
                prepared.put(key, initial);
                ProviderSubscription subscription = registry.subscribe(
                        key,
                        this::onSnapshot
                );
                created.put(key, subscription);
            }
        } catch (RuntimeException failure) {
            created.values().forEach(ProviderSubscription::close);
            throw failure;
        }
        created.forEach((key, subscription) -> subscriptions.put(
                key,
                new SubscriptionRef(subscription, 1)
        ));
        snapshot.set(Map.copyOf(prepared));
    }

    /**
     * 中文说明：执行 发布 操作；该方法是 {@code ProviderDirectory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the release operation; this method is the invocation entry point on {@code ProviderDirectory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderDirectory.release(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serviceKeys 参数 服务Keys；parameter service keys。
     */
    public synchronized void release(Set<ProviderServiceKey> serviceKeys) {
        Map<ProviderServiceKey, ProviderServiceSnapshot> updated =
                new LinkedHashMap<>(snapshot.get());
        for (ProviderServiceKey key : Set.copyOf(serviceKeys)) {
            SubscriptionRef reference = subscriptions.get(key);
            if (reference == null) {
                continue;
            }
            reference.references--;
            if (reference.references == 0) {
                reference.subscription.close();
                subscriptions.remove(key);
                updated.remove(key);
            }
        }
        snapshot.set(Map.copyOf(updated));
    }

    /**
     * 中文说明：执行 available 操作；该方法是 {@code ProviderDirectory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the available operation; this method is the invocation entry point on {@code ProviderDirectory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderDirectory.available(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param key 参数 键；parameter key。
     * @return 返回 available 的处理结果；returns the result of the operation.
     */
    public List<ProviderInstance> available(ProviderServiceKey key) {
        Instant now = clock.instant();
        return instances(key).stream()
                .filter(instance -> instance.availableAt(now))
                .toList();
    }

    /**
     * 中文说明：执行 instances 操作；该方法是 {@code ProviderDirectory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the instances operation; this method is the invocation entry point on {@code ProviderDirectory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderDirectory.instances(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param key 参数 键；parameter key。
     * @return 返回 instances 的处理结果；returns the result of the operation.
     */
    public List<ProviderInstance> instances(ProviderServiceKey key) {
        ProviderServiceSnapshot service = snapshot.get().get(key);
        if (service == null) {
            return List.of();
        }
        return service.instances();
    }

    /**
     * 中文说明：执行 snapshot 操作；该方法是 {@code ProviderDirectory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the snapshot operation; this method is the invocation entry point on {@code ProviderDirectory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderDirectory.snapshot(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 snapshot 的处理结果；returns the result of the operation.
     */
    public Map<ProviderServiceKey, ProviderServiceSnapshot> snapshot() {
        return snapshot.get();
    }

    /**
     * 中文说明：执行 allAvailable 操作；该方法是 {@code ProviderDirectory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the all available operation; this method is the invocation entry point on {@code ProviderDirectory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderDirectory.allAvailable(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param serviceKeys 参数 服务Keys；parameter service keys。
     * @return 返回 allAvailable 的处理结果；returns the result of the operation.
     */
    public boolean allAvailable(Set<ProviderServiceKey> serviceKeys) {
        return Set.copyOf(serviceKeys).stream()
                .allMatch(key -> !available(key).isEmpty());
    }

    /**
     * 中文说明：执行 referenceCount 操作；该方法是 {@code ProviderDirectory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the reference count operation; this method is the invocation entry point on {@code ProviderDirectory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderDirectory.referenceCount(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param key 参数 键；parameter key。
     * @return 返回 referenceCount 的处理结果；returns the result of the operation.
     */
    public synchronized int referenceCount(ProviderServiceKey key) {
        SubscriptionRef reference = subscriptions.get(key);
        return reference == null ? 0 : reference.references;
    }

    /**
     * 中文说明：执行 close 操作；该方法是 {@code ProviderDirectory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the close operation; this method is the invocation entry point on {@code ProviderDirectory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderDirectory.close(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    @Override
    public synchronized void close() {
        subscriptions.values().forEach(
                reference -> reference.subscription.close()
        );
        subscriptions.clear();
        snapshot.set(Map.of());
    }

    /**
     * 中文说明：执行 onSnapshot 操作；该方法是 {@code ProviderDirectory} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the on snapshot operation; this method is the invocation entry point on {@code ProviderDirectory} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderDirectory.onSnapshot(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param updated 参数 updated；parameter updated。
     */
    private void onSnapshot(ProviderServiceSnapshot updated) {
        snapshot.updateAndGet(current -> {
            if (!subscriptions.containsKey(updated.serviceKey())) {
                return current;
            }
            ProviderServiceSnapshot existing = current.get(
                    updated.serviceKey()
            );
            if (existing != null && updated.revision() < existing.revision()) {
                return current;
            }
            Map<ProviderServiceKey, ProviderServiceSnapshot> copy =
                    new LinkedHashMap<>(current);
            copy.put(updated.serviceKey(), updated);
            return Map.copyOf(copy);
        });
    }

    /**
     * 中文说明：{@code SubscriptionRef} 是类型，位于当前 Gateway 模块的相关包中，负责订阅Ref相关的职责与边界。
     * English summary: {@code SubscriptionRef} is a type in the current Gateway module; it owns the subscription ref-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private static final class SubscriptionRef {

        /**
         * 中文说明：保存 订阅 对应的状态、依赖或配置值；字段类型为 {@code ProviderSubscription}，由 {@code ProviderDirectory.SubscriptionRef} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by subscription; its type is {@code ProviderSubscription}, and {@code ProviderDirectory.SubscriptionRef} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ProviderDirectory.SubscriptionRef} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderDirectory.SubscriptionRef}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final ProviderSubscription subscription;

        /**
         * 中文说明：保存 references 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code ProviderDirectory.SubscriptionRef} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by references; its type is {@code int}, and {@code ProviderDirectory.SubscriptionRef} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ProviderDirectory.SubscriptionRef} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderDirectory.SubscriptionRef}; do not couple callers to its representation when the owning type exposes an API.
         */
        private int references;

        /**
         * 中文说明：创建 {@code ProviderDirectory.SubscriptionRef} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code ProviderDirectory.SubscriptionRef} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param subscription 参数 订阅；parameter subscription。
         * @param references 参数 references；parameter references。
         */
        private SubscriptionRef(
                ProviderSubscription subscription,
                int references) {
            this.subscription = subscription;
            this.references = references;
        }
    }
}
