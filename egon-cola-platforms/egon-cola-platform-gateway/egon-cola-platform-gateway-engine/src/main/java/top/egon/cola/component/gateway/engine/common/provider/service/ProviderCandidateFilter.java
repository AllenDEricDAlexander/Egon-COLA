package top.egon.cola.component.gateway.engine.common.provider.service;

import top.egon.cola.component.gateway.engine.common.provider.domain.ProviderCandidateFilterResult;
import top.egon.cola.component.gateway.engine.common.provider.domain.ProviderCandidateStage;
import top.egon.cola.component.gateway.engine.common.provider.domain.ProviderPolicyOverride;
import top.egon.cola.component.gateway.engine.common.provider.domain.ProviderSelectionPolicy;

import top.egon.cola.component.gateway.core.provider.ProviderHealthState;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderRegistryState;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 中文说明：{@code ProviderCandidateFilter} 是过滤器，位于当前 Gateway 模块的相关包中，负责提供方Candidate过滤器相关的职责与边界。
 * English summary: {@code ProviderCandidateFilter} is a provider candidate filter filter in the current Gateway module; it owns the provider candidate filter-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class ProviderCandidateFilter {

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code ProviderCandidateFilter} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code ProviderCandidateFilter} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderCandidateFilter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderCandidateFilter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：保存 准入Available 对应的状态、依赖或配置值；字段类型为 {@code Predicate<String>}，由 {@code ProviderCandidateFilter} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by admission available; its type is {@code Predicate<String>}, and {@code ProviderCandidateFilter} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ProviderCandidateFilter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderCandidateFilter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Predicate<String> admissionAvailable;

    /**
     * 中文说明：创建 {@code ProviderCandidateFilter} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code ProviderCandidateFilter} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param clock 参数 clock；parameter clock。
     * @param admissionAvailable 参数 准入Available；parameter admission available。
     */
    public ProviderCandidateFilter(
            Clock clock,
            Predicate<String> admissionAvailable) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.admissionAvailable = Objects.requireNonNull(
                admissionAvailable,
                "admissionAvailable"
        );
    }

    /**
     * 中文说明：执行 过滤器 操作；该方法是 {@code ProviderCandidateFilter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the filter operation; this method is the invocation entry point on {@code ProviderCandidateFilter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderCandidateFilter.filter(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param expectedService 参数 expected服务；parameter expected service。
     * @param instances 参数 instances；parameter instances。
     * @param policy 参数 策略；parameter policy。
     * @return 返回 过滤器 的处理结果；returns the result of the operation.
     */
    public ProviderCandidateFilterResult filter(
            ProviderServiceKey expectedService,
            List<ProviderInstance> instances,
            ProviderSelectionPolicy policy) {
        Objects.requireNonNull(expectedService, "expectedService");
        Objects.requireNonNull(instances, "instances");
        Objects.requireNonNull(policy, "policy");
        EnumMap<ProviderCandidateStage, Integer> counts =
                new EnumMap<>(ProviderCandidateStage.class);
        Map<String, String> rejected = new LinkedHashMap<>();
        List<Candidate> candidates = instances.stream()
                .map(Candidate::new)
                .toList();

        candidates = apply(
                candidates,
                ProviderCandidateStage.EXACT_SERVICE,
                candidate -> expectedService.equals(
                        candidate.instance.serviceKey()
                ),
                "SERVICE_KEY_MISMATCH",
                counts,
                rejected
        );
        Instant now = clock.instant();
        candidates = apply(
                candidates,
                ProviderCandidateStage.VALID_LEASE,
                candidate -> candidate.instance.registryState()
                        == ProviderRegistryState.REGISTERED
                        && candidate.instance.leaseExpireAt().isAfter(now),
                "LEASE_EXPIRED",
                counts,
                rejected
        );
        candidates = apply(
                candidates,
                ProviderCandidateStage.PROTOCOL_MATCH,
                candidate -> policy.secureRequired() == null
                        || candidate.instance.secure()
                        == policy.secureRequired(),
                "PROTOCOL_MISMATCH",
                counts,
                rejected
        );
        candidates = candidates.stream()
                .peek(candidate -> candidate.resolve(policy))
                .toList();
        candidates = apply(
                candidates,
                ProviderCandidateStage.ADMIN_ENABLED,
                candidate -> candidate.enabled,
                "ADMIN_DISABLED",
                counts,
                rejected
        );
        candidates = apply(
                candidates,
                ProviderCandidateStage.LOCATION_AND_TAGS,
                candidate -> matchesLocationAndTags(candidate, policy),
                "LOCATION_OR_TAG_MISMATCH",
                counts,
                rejected
        );
        candidates = apply(
                candidates,
                ProviderCandidateStage.HEALTHY,
                candidate -> healthy(candidate.instance)
                        && admissionAvailable.test(
                        candidate.instance.runtimeIdentity()
                ),
                "UNHEALTHY_OR_EJECTED",
                counts,
                rejected
        );
        counts.put(
                ProviderCandidateStage.ADMISSION_AVAILABLE,
                candidates.size()
        );

        List<Candidate> weighted = new ArrayList<>();
        for (Candidate candidate : candidates) {
            try {
                if (candidate.weight() > 0) {
                    weighted.add(candidate);
                } else {
                    rejected.putIfAbsent(
                            candidate.identity(),
                            "WEIGHT_DISABLED"
                    );
                }
            } catch (IllegalArgumentException invalidMetadata) {
                rejected.putIfAbsent(
                        candidate.identity(),
                        "INVALID_METADATA"
                );
            }
        }
        counts.put(ProviderCandidateStage.POSITIVE_WEIGHT, weighted.size());
        return new ProviderCandidateFilterResult(
                weighted.stream().map(Candidate::effectiveInstance).toList(),
                counts,
                rejected
        );
    }

    /**
     * 中文说明：执行 apply 操作；该方法是 {@code ProviderCandidateFilter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the apply operation; this method is the invocation entry point on {@code ProviderCandidateFilter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderCandidateFilter.apply(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param stage 参数 stage；parameter stage。
     * @param predicate 参数 predicate；parameter predicate。
     * @param rejection 参数 rejection；parameter rejection。
     * @param counts 参数 counts；parameter counts。
     * @param rejected 参数 rejected；parameter rejected。
     * @return 返回 apply 的处理结果；returns the result of the operation.
     */
    private List<Candidate> apply(
            List<Candidate> source,
            ProviderCandidateStage stage,
            Predicate<Candidate> predicate,
            String rejection,
            Map<ProviderCandidateStage, Integer> counts,
            Map<String, String> rejected) {
        List<Candidate> accepted = new ArrayList<>();
        for (Candidate candidate : source) {
            if (predicate.test(candidate)) {
                accepted.add(candidate);
            } else {
                rejected.putIfAbsent(candidate.identity(), rejection);
            }
        }
        counts.put(stage, accepted.size());
        return List.copyOf(accepted);
    }

    /**
     * 中文说明：执行 matchesLocationAndTags 操作；该方法是 {@code ProviderCandidateFilter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the matches location and tags operation; this method is the invocation entry point on {@code ProviderCandidateFilter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderCandidateFilter.matchesLocationAndTags(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param candidate 参数 candidate；parameter candidate。
     * @param policy 参数 策略；parameter policy。
     * @return 返回 matchesLocationAndTags 的处理结果；returns the result of the operation.
     */
    private boolean matchesLocationAndTags(
            Candidate candidate,
            ProviderSelectionPolicy policy) {
        return matches(policy.requiredZone(), candidate.zone)
                && matches(policy.requiredRegion(), candidate.region)
                && candidate.tags.containsAll(policy.requiredTags());
    }

    /**
     * 中文说明：执行 matches 操作；该方法是 {@code ProviderCandidateFilter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the matches operation; this method is the invocation entry point on {@code ProviderCandidateFilter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderCandidateFilter.matches(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param required 参数 required；parameter required。
     * @param actual 参数 actual；parameter actual。
     * @return 返回 matches 的处理结果；returns the result of the operation.
     */
    private boolean matches(String required, String actual) {
        return required == null || required.equals(actual);
    }

    /**
     * 中文说明：执行 healthy 操作；该方法是 {@code ProviderCandidateFilter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the healthy operation; this method is the invocation entry point on {@code ProviderCandidateFilter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderCandidateFilter.healthy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param instance 参数 instance；parameter instance。
     * @return 返回 healthy 的处理结果；returns the result of the operation.
     */
    private boolean healthy(ProviderInstance instance) {
        return allowed(instance.activeHealth())
                && allowed(instance.passiveHealth());
    }

    /**
     * 中文说明：执行 allowed 操作；该方法是 {@code ProviderCandidateFilter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the allowed operation; this method is the invocation entry point on {@code ProviderCandidateFilter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderCandidateFilter.allowed(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param state 参数 state；parameter state。
     * @return 返回 allowed 的处理结果；returns the result of the operation.
     */
    private boolean allowed(ProviderHealthState state) {
        return state != ProviderHealthState.UNHEALTHY
                && state != ProviderHealthState.EJECTED;
    }

    /**
     * 中文说明：{@code Candidate} 是类型，位于当前 Gateway 模块的相关包中，负责Candidate相关的职责与边界。
     * English summary: {@code Candidate} is a type in the current Gateway module; it owns the candidate-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private static final class Candidate {

        /**
         * 中文说明：保存 instance 对应的状态、依赖或配置值；字段类型为 {@code ProviderInstance}，由 {@code ProviderCandidateFilter.Candidate} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by instance; its type is {@code ProviderInstance}, and {@code ProviderCandidateFilter.Candidate} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ProviderCandidateFilter.Candidate} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderCandidateFilter.Candidate}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final ProviderInstance instance;

        /**
         * 中文说明：保存 元数据 对应的状态、依赖或配置值；字段类型为 {@code Map<String, String>}，由 {@code ProviderCandidateFilter.Candidate} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by metadata; its type is {@code Map<String, String>}, and {@code ProviderCandidateFilter.Candidate} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ProviderCandidateFilter.Candidate} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderCandidateFilter.Candidate}; do not couple callers to its representation when the owning type exposes an API.
         */
        private Map<String, String> metadata;

        /**
         * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code ProviderCandidateFilter.Candidate} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code ProviderCandidateFilter.Candidate} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ProviderCandidateFilter.Candidate} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderCandidateFilter.Candidate}; do not couple callers to its representation when the owning type exposes an API.
         */
        private boolean enabled;

        /**
         * 中文说明：保存 zone 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code ProviderCandidateFilter.Candidate} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by zone; its type is {@code String}, and {@code ProviderCandidateFilter.Candidate} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ProviderCandidateFilter.Candidate} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderCandidateFilter.Candidate}; do not couple callers to its representation when the owning type exposes an API.
         */
        private String zone;

        /**
         * 中文说明：保存 region 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code ProviderCandidateFilter.Candidate} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by region; its type is {@code String}, and {@code ProviderCandidateFilter.Candidate} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ProviderCandidateFilter.Candidate} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderCandidateFilter.Candidate}; do not couple callers to its representation when the owning type exposes an API.
         */
        private String region;

        /**
         * 中文说明：保存 tags 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code ProviderCandidateFilter.Candidate} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by tags; its type is {@code Set<String>}, and {@code ProviderCandidateFilter.Candidate} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ProviderCandidateFilter.Candidate} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderCandidateFilter.Candidate}; do not couple callers to its representation when the owning type exposes an API.
         */
        private Set<String> tags;

        /**
         * 中文说明：创建 {@code ProviderCandidateFilter.Candidate} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code ProviderCandidateFilter.Candidate} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param instance 参数 instance；parameter instance。
         */
        private Candidate(ProviderInstance instance) {
            this.instance = Objects.requireNonNull(instance, "instance");
        }

        /**
         * 中文说明：执行 resolve 操作；该方法是 {@code ProviderCandidateFilter.Candidate} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the resolve operation; this method is the invocation entry point on {@code ProviderCandidateFilter.Candidate} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code ProviderCandidateFilter.Candidate.resolve(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param policy 参数 策略；parameter policy。
         */
        private void resolve(ProviderSelectionPolicy policy) {
            metadata = new LinkedHashMap<>(instance.metadata());
            enabled = policy.serviceEnabled();
            apply(policy.serviceOverride());
            apply(policy.instanceOverrides().get(instance.instanceId()));
            zone = metadata.get("gateway.zone");
            region = metadata.get("gateway.region");
            tags = parseTags(metadata.get("gateway.tags"));
        }

        /**
         * 中文说明：执行 apply 操作；该方法是 {@code ProviderCandidateFilter.Candidate} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the apply operation; this method is the invocation entry point on {@code ProviderCandidateFilter.Candidate} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code ProviderCandidateFilter.Candidate.apply(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param override 参数 override；parameter override。
         */
        private void apply(ProviderPolicyOverride override) {
            if (override == null) {
                return;
            }
            if (override.enabled() != null) {
                enabled = override.enabled();
            }
            put("gateway.weight", override.weight());
            put("gateway.zone", override.zone());
            put("gateway.region", override.region());
            if (override.tags() != null) {
                metadata.put(
                        "gateway.tags",
                        String.join(",", override.tags().stream().sorted()
                                .toList())
                );
            }
        }

        /**
         * 中文说明：执行 put 操作；该方法是 {@code ProviderCandidateFilter.Candidate} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the put operation; this method is the invocation entry point on {@code ProviderCandidateFilter.Candidate} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code ProviderCandidateFilter.Candidate.put(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param key 参数 键；parameter key。
         * @param value 参数 值；parameter value。
         */
        private void put(String key, Object value) {
            if (value != null) {
                metadata.put(key, value.toString());
            }
        }

        /**
         * 中文说明：执行 weight 操作；该方法是 {@code ProviderCandidateFilter.Candidate} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the weight operation; this method is the invocation entry point on {@code ProviderCandidateFilter.Candidate} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code ProviderCandidateFilter.Candidate.weight(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 weight 的处理结果；returns the result of the operation.
         */
        private int weight() {
            String raw = metadata.getOrDefault("gateway.weight", "100");
            try {
                int result = Integer.parseInt(raw);
                if (result < 0 || result > 10000) {
                    throw new IllegalArgumentException(
                            "gateway.weight must be between 0 and 10000"
                    );
                }
                return result;
            } catch (NumberFormatException invalid) {
                throw new IllegalArgumentException(
                        "gateway.weight must be an integer",
                        invalid
                );
            }
        }

        /**
         * 中文说明：执行 effectiveInstance 操作；该方法是 {@code ProviderCandidateFilter.Candidate} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the effective instance operation; this method is the invocation entry point on {@code ProviderCandidateFilter.Candidate} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code ProviderCandidateFilter.Candidate.effectiveInstance(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 effectiveInstance 的处理结果；returns the result of the operation.
         */
        private ProviderInstance effectiveInstance() {
            return new ProviderInstance(
                    instance.serviceKey(),
                    instance.instanceId(),
                    instance.leaseId(),
                    instance.host(),
                    instance.port(),
                    instance.secure(),
                    metadata,
                    instance.leaseExpireAt(),
                    instance.registryState(),
                    instance.activeHealth(),
                    instance.passiveHealth()
            );
        }

        /**
         * 中文说明：执行 身份 操作；该方法是 {@code ProviderCandidateFilter.Candidate} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the identity operation; this method is the invocation entry point on {@code ProviderCandidateFilter.Candidate} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code ProviderCandidateFilter.Candidate.identity(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 身份 的处理结果；returns the result of the operation.
         */
        private String identity() {
            return instance.runtimeIdentity();
        }

        /**
         * 中文说明：执行 parseTags 操作；该方法是 {@code ProviderCandidateFilter.Candidate} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the parse tags operation; this method is the invocation entry point on {@code ProviderCandidateFilter.Candidate} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code ProviderCandidateFilter.Candidate.parseTags(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param raw 参数 raw；parameter raw。
         * @return 返回 parseTags 的处理结果；returns the result of the operation.
         */
        private static Set<String> parseTags(String raw) {
            if (raw == null || raw.isBlank()) {
                return Set.of();
            }
            Set<String> result = new LinkedHashSet<>();
            for (String value : raw.split(",")) {
                String tag = value.trim();
                if (tag.isEmpty()) {
                    throw new IllegalArgumentException(
                            "gateway.tags contains an empty tag"
                    );
                }
                result.add(tag);
            }
            return Set.copyOf(result);
        }
    }
}
