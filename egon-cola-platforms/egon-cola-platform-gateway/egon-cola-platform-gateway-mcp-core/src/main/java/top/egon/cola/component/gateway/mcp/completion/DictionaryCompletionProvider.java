package top.egon.cola.component.gateway.mcp.completion;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 中文说明：{@code DictionaryCompletionProvider} 是提供方组件，位于当前 Gateway 模块的相关包中，负责Dictionary补全提供方相关的职责与边界。
 * English summary: {@code DictionaryCompletionProvider} is a dictionary completion provider provider in the current Gateway module; it owns the dictionary completion provider-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class DictionaryCompletionProvider
        implements McpCompletionProvider {

    /**
     * 中文说明：保存 dictionaries 对应的状态、依赖或配置值；字段类型为 {@code Map<Key, List<String>>}，由 {@code DictionaryCompletionProvider} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by dictionaries; its type is {@code Map<Key, List<String>>}, and {@code DictionaryCompletionProvider} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DictionaryCompletionProvider} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DictionaryCompletionProvider}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Map<Key, List<String>> dictionaries;

    /**
     * 中文说明：创建 {@code DictionaryCompletionProvider} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code DictionaryCompletionProvider} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param dictionaries 参数 dictionaries；parameter dictionaries。
     */
    public DictionaryCompletionProvider(Map<Key, List<String>> dictionaries) {
        java.util.LinkedHashMap<Key, List<String>> copy =
                new java.util.LinkedHashMap<>();
        java.util.Objects.requireNonNull(
                dictionaries,
                "dictionaries"
        ).forEach((key, value) -> copy.put(key, List.copyOf(value)));
        this.dictionaries = Map.copyOf(copy);
    }

    /**
     * 中文说明：执行 sourceType 操作；该方法是 {@code DictionaryCompletionProvider} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the source type operation; this method is the invocation entry point on {@code DictionaryCompletionProvider} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DictionaryCompletionProvider.sourceType(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 sourceType 的处理结果；returns the result of the operation.
     */
    @Override
    public String sourceType() {
        return "LOCAL_DICTIONARY";
    }

    /**
     * 中文说明：执行 complete 操作；该方法是 {@code DictionaryCompletionProvider} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the complete operation; this method is the invocation entry point on {@code DictionaryCompletionProvider} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DictionaryCompletionProvider.complete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 complete 的处理结果；returns the result of the operation.
     */
    @Override
    public Mono<Result> complete(Request request) {
        if (McpCompletionProvider.sensitiveArgumentName(
                request.argumentName()
        )) {
            return Mono.just(new Result(List.of(), 0, false));
        }
        List<String> values = dictionaries.getOrDefault(
                        new Key(
                                request.referenceType(),
                                request.referenceName(),
                                request.argumentName()
                        ),
                        List.of()
                ).stream()
                .filter(value -> value != null && value.length() <= 256)
                .filter(value -> value.startsWith(request.valuePrefix()))
                .filter(value -> !McpCompletionProvider.sensitiveValue(value))
                .distinct()
                .sorted()
                .toList();
        return Mono.just(new Result(
                values.stream().limit(100).toList(),
                values.size(),
                values.size() > 100
        ));
    }

    /**
     * 中文说明：{@code Key} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责键相关的职责与边界。
     * English summary: {@code Key} is an immutable data carrier in the current Gateway module; it owns the key-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param referenceType 参数 referenceType；parameter reference type。
     * @param referenceName 参数 referenceName；parameter reference name。
     * @param argumentName 参数 argumentName；parameter argument name。
     */
    public record Key(
            /**
             * 中文说明：保存 referenceType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code DictionaryCompletionProvider.Key} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by reference type; its type is {@code String}, and {@code DictionaryCompletionProvider.Key} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code DictionaryCompletionProvider.Key} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DictionaryCompletionProvider.Key}; do not couple callers to its representation when the owning type exposes an API.
             */
            String referenceType,
            /**
             * 中文说明：保存 referenceName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code DictionaryCompletionProvider.Key} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by reference name; its type is {@code String}, and {@code DictionaryCompletionProvider.Key} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code DictionaryCompletionProvider.Key} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DictionaryCompletionProvider.Key}; do not couple callers to its representation when the owning type exposes an API.
             */
            String referenceName,
            /**
             * 中文说明：保存 argumentName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code DictionaryCompletionProvider.Key} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by argument name; its type is {@code String}, and {@code DictionaryCompletionProvider.Key} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code DictionaryCompletionProvider.Key} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DictionaryCompletionProvider.Key}; do not couple callers to its representation when the owning type exposes an API.
             */
            String argumentName
    ) {
    }
}
