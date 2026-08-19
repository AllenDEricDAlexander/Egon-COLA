package top.egon.cola.component.gateway.engine.common.provider.domain;

import top.egon.cola.component.gateway.core.provider.ProviderInstance;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 中文说明：{@code ProviderCandidateFilterResult} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责提供方Candidate过滤器Result相关的职责与边界。
 * English summary: {@code ProviderCandidateFilterResult} is an immutable data carrier in the current Gateway module; it owns the provider candidate filter result-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param candidates 参数 candidates；parameter candidates。
 * @param counts 参数 counts；parameter counts。
 * @param rejectedReasons 参数 rejectedReasons；parameter rejected reasons。
 */
public record ProviderCandidateFilterResult(
        /**
         * 中文说明：保存 candidates 对应的状态、依赖或配置值；字段类型为 {@code List<ProviderInstance>}，由 {@code ProviderCandidateFilterResult} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by candidates; its type is {@code List<ProviderInstance>}, and {@code ProviderCandidateFilterResult} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ProviderCandidateFilterResult} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderCandidateFilterResult}; do not couple callers to its representation when the owning type exposes an API.
         */
        List<ProviderInstance> candidates,
        /**
         * 中文说明：保存 counts 对应的状态、依赖或配置值；字段类型为 {@code Map<ProviderCandidateStage, Integer>}，由 {@code ProviderCandidateFilterResult} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by counts; its type is {@code Map<ProviderCandidateStage, Integer>}, and {@code ProviderCandidateFilterResult} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ProviderCandidateFilterResult} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderCandidateFilterResult}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<ProviderCandidateStage, Integer> counts,
        /**
         * 中文说明：保存 rejectedReasons 对应的状态、依赖或配置值；字段类型为 {@code Map<String, String>}，由 {@code ProviderCandidateFilterResult} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by rejected reasons; its type is {@code Map<String, String>}, and {@code ProviderCandidateFilterResult} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ProviderCandidateFilterResult} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderCandidateFilterResult}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, String> rejectedReasons
) {

    /**
     * 中文说明：创建 {@code ProviderCandidateFilterResult} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code ProviderCandidateFilterResult} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param candidates 参数 candidates；parameter candidates。
     * @param counts 参数 counts；parameter counts。
     * @param rejectedReasons 参数 rejectedReasons；parameter rejected reasons。
     */
    public ProviderCandidateFilterResult {
        candidates = List.copyOf(
                Objects.requireNonNull(candidates, "candidates")
        );
        counts = Map.copyOf(Objects.requireNonNull(counts, "counts"));
        rejectedReasons = Map.copyOf(
                Objects.requireNonNull(rejectedReasons, "rejectedReasons")
        );
    }
}
