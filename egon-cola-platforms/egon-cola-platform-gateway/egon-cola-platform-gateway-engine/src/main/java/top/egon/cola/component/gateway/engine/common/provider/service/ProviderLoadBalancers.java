package top.egon.cola.component.gateway.engine.common.provider.service;

import top.egon.cola.component.gateway.engine.common.provider.domain.LoadBalancerType;
import top.egon.cola.component.gateway.engine.common.provider.domain.ProviderSelectionHandle;

import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * 中文说明：{@code ProviderLoadBalancers} 是类型，位于当前 Gateway 模块的相关包中，负责提供方LoadBalancers相关的职责与边界。
 * English summary: {@code ProviderLoadBalancers} is a type in the current Gateway module; it owns the provider load balancers-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class ProviderLoadBalancers {

    /**
     * 中文说明：创建 {@code ProviderLoadBalancers} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code ProviderLoadBalancers} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     */
    private ProviderLoadBalancers() {
    }

    /**
     * 中文说明：执行 create 操作；该方法是 {@code ProviderLoadBalancers} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create operation; this method is the invocation entry point on {@code ProviderLoadBalancers} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderLoadBalancers.create(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param type 参数 type；parameter type。
     * @return 返回 create 的处理结果；returns the result of the operation.
     */
    public static ProviderLoadBalancer create(LoadBalancerType type) {
        return switch (type) {
            case ROUND_ROBIN -> new RoundRobin();
            case SMOOTH_WEIGHTED_ROUND_ROBIN -> new SmoothWeightedRoundRobin();
            case RANDOM -> new Random();
            case LEAST_IN_FLIGHT -> new LeastInFlight();
        };
    }

    /**
     * 中文说明：执行 handle 操作；该方法是 {@code ProviderLoadBalancers} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the handle operation; this method is the invocation entry point on {@code ProviderLoadBalancers} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderLoadBalancers.handle(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param selected 参数 selected；parameter selected。
     * @return 返回 handle 的处理结果；returns the result of the operation.
     */
    private static ProviderSelectionHandle handle(
            ProviderInstance selected) {
        return new ProviderSelectionHandle(selected, () -> {
        });
    }

    /**
     * 中文说明：执行 checked 操作；该方法是 {@code ProviderLoadBalancers} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the checked operation; this method is the invocation entry point on {@code ProviderLoadBalancers} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderLoadBalancers.checked(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param candidates 参数 candidates；parameter candidates。
     * @return 返回 checked 的处理结果；returns the result of the operation.
     */
    private static List<ProviderInstance> checked(
            List<ProviderInstance> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalStateException("GATEWAY_PROVIDER_UNAVAILABLE");
        }
        return candidates.stream()
                .sorted(Comparator.comparing(ProviderInstance::runtimeIdentity))
                .toList();
    }

    /**
     * 中文说明：{@code RoundRobin} 是类型，位于当前 Gateway 模块的相关包中，负责RoundRobin相关的职责与边界。
     * English summary: {@code RoundRobin} is a type in the current Gateway module; it owns the round robin-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private static final class RoundRobin implements ProviderLoadBalancer {

        /**
         * 中文说明：保存 sequences 对应的状态、依赖或配置值；字段类型为 {@code Map<ProviderServiceKey, AtomicInteger>}，由 {@code ProviderLoadBalancers.RoundRobin} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by sequences; its type is {@code Map<ProviderServiceKey, AtomicInteger>}, and {@code ProviderLoadBalancers.RoundRobin} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ProviderLoadBalancers.RoundRobin} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderLoadBalancers.RoundRobin}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final Map<ProviderServiceKey, AtomicInteger> sequences =
                new ConcurrentHashMap<>();

        /**
         * 中文说明：执行 select 操作；该方法是 {@code ProviderLoadBalancers.RoundRobin} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the select operation; this method is the invocation entry point on {@code ProviderLoadBalancers.RoundRobin} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code ProviderLoadBalancers.RoundRobin.select(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param serviceKey 参数 服务键；parameter service key。
         * @param candidates 参数 candidates；parameter candidates。
         * @return 返回 select 的处理结果；returns the result of the operation.
         */
        @Override
        public ProviderSelectionHandle select(
                ProviderServiceKey serviceKey,
                List<ProviderInstance> candidates) {
            List<ProviderInstance> available = checked(candidates);
            int index = Math.floorMod(
                    sequences.computeIfAbsent(
                            serviceKey,
                            ignored -> new AtomicInteger()
                    ).getAndIncrement(),
                    available.size()
            );
            return handle(available.get(index));
        }
    }

    /**
     * 中文说明：{@code Random} 是类型，位于当前 Gateway 模块的相关包中，负责Random相关的职责与边界。
     * English summary: {@code Random} is a type in the current Gateway module; it owns the random-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private static final class Random implements ProviderLoadBalancer {

        /**
         * 中文说明：执行 select 操作；该方法是 {@code ProviderLoadBalancers.Random} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the select operation; this method is the invocation entry point on {@code ProviderLoadBalancers.Random} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code ProviderLoadBalancers.Random.select(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param serviceKey 参数 服务键；parameter service key。
         * @param candidates 参数 candidates；parameter candidates。
         * @return 返回 select 的处理结果；returns the result of the operation.
         */
        @Override
        public ProviderSelectionHandle select(
                ProviderServiceKey serviceKey,
                List<ProviderInstance> candidates) {
            List<ProviderInstance> available = checked(candidates);
            return handle(available.get(
                    ThreadLocalRandom.current().nextInt(available.size())
            ));
        }
    }

    /**
     * 中文说明：{@code SmoothWeightedRoundRobin} 是类型，位于当前 Gateway 模块的相关包中，负责SmoothWeightedRoundRobin相关的职责与边界。
     * English summary: {@code SmoothWeightedRoundRobin} is a type in the current Gateway module; it owns the smooth weighted round robin-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private static final class SmoothWeightedRoundRobin
            implements ProviderLoadBalancer {

        /**
         * 中文说明：保存 currents 对应的状态、依赖或配置值；字段类型为 {@code Map<ProviderServiceKey, Map<String, Integer>>}，由 {@code ProviderLoadBalancers.SmoothWeightedRoundRobin} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by currents; its type is {@code Map<ProviderServiceKey, Map<String, Integer>>}, and {@code ProviderLoadBalancers.SmoothWeightedRoundRobin} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ProviderLoadBalancers.SmoothWeightedRoundRobin} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderLoadBalancers.SmoothWeightedRoundRobin}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final Map<ProviderServiceKey, Map<String, Integer>> currents =
                new ConcurrentHashMap<>();

        /**
         * 中文说明：执行 select 操作；该方法是 {@code ProviderLoadBalancers.SmoothWeightedRoundRobin} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the select operation; this method is the invocation entry point on {@code ProviderLoadBalancers.SmoothWeightedRoundRobin} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code ProviderLoadBalancers.SmoothWeightedRoundRobin.select(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param serviceKey 参数 服务键；parameter service key。
         * @param candidates 参数 candidates；parameter candidates。
         * @return 返回 select 的处理结果；returns the result of the operation.
         */
        @Override
        public synchronized ProviderSelectionHandle select(
                ProviderServiceKey serviceKey,
                List<ProviderInstance> candidates) {
            List<ProviderInstance> available = checked(candidates);
            Map<String, Integer> current = currents.computeIfAbsent(
                    serviceKey,
                    ignored -> new LinkedHashMap<>()
            );
            current.keySet().retainAll(
                    available.stream()
                            .map(ProviderInstance::runtimeIdentity)
                            .toList()
            );
            int totalWeight = 0;
            ProviderInstance selected = null;
            int best = Integer.MIN_VALUE;
            for (ProviderInstance candidate : available) {
                int next = current.getOrDefault(
                        candidate.runtimeIdentity(),
                        0
                ) + candidate.weight();
                current.put(candidate.runtimeIdentity(), next);
                totalWeight += candidate.weight();
                if (selected == null || next > best) {
                    selected = candidate;
                    best = next;
                }
            }
            String selectedIdentity = selected.runtimeIdentity();
            current.put(
                    selectedIdentity,
                    current.get(selectedIdentity) - totalWeight
            );
            return handle(selected);
        }
    }

    /**
     * 中文说明：{@code LeastInFlight} 是类型，位于当前 Gateway 模块的相关包中，负责LeastInFlight相关的职责与边界。
     * English summary: {@code LeastInFlight} is a type in the current Gateway module; it owns the least in flight-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private static final class LeastInFlight implements ProviderLoadBalancer {

        /**
         * 中文说明：保存 inFlight 对应的状态、依赖或配置值；字段类型为 {@code Map<String, LongAdder>}，由 {@code ProviderLoadBalancers.LeastInFlight} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by in flight; its type is {@code Map<String, LongAdder>}, and {@code ProviderLoadBalancers.LeastInFlight} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ProviderLoadBalancers.LeastInFlight} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderLoadBalancers.LeastInFlight}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final Map<String, LongAdder> inFlight =
                new ConcurrentHashMap<>();

        /**
         * 中文说明：执行 select 操作；该方法是 {@code ProviderLoadBalancers.LeastInFlight} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the select operation; this method is the invocation entry point on {@code ProviderLoadBalancers.LeastInFlight} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code ProviderLoadBalancers.LeastInFlight.select(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param serviceKey 参数 服务键；parameter service key。
         * @param candidates 参数 candidates；parameter candidates。
         * @return 返回 select 的处理结果；returns the result of the operation.
         */
        @Override
        public ProviderSelectionHandle select(
                ProviderServiceKey serviceKey,
                List<ProviderInstance> candidates) {
            ProviderInstance selected = checked(candidates).stream()
                    .min(Comparator
                            .comparingLong((ProviderInstance candidate) ->
                                    inFlight.computeIfAbsent(
                                            candidate.runtimeIdentity(),
                                            ignored -> new LongAdder()
                                    ).sum())
                            .thenComparing(ProviderInstance::runtimeIdentity))
                    .orElseThrow();
            LongAdder counter = inFlight.computeIfAbsent(
                    selected.runtimeIdentity(),
                    ignored -> new LongAdder()
            );
            counter.increment();
            return new ProviderSelectionHandle(selected, counter::decrement);
        }
    }
}
