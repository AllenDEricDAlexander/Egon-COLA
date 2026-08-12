package top.egon.cola.component.gateway.mcp.completion;

import org.reactivestreams.Publisher;
import top.egon.cola.component.gateway.mcp.prompt.McpPromptDriver;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Strategy for bounded, deterministic completion sources.
 * 补充说明 / Supplementary summary: {@code McpCompletionProvider} 是接口契约，位于当前 Gateway 模块的相关包中，负责MCP补全提供方相关的职责与边界。
 * English supplement: {@code McpCompletionProvider} is an interface contract in the current Gateway module; it owns the mcp completion provider-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public interface McpCompletionProvider {

    /**
     * 中文说明：执行 sourceType 操作；该方法是 {@code McpCompletionProvider} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the source type operation; this method is the invocation entry point on {@code McpCompletionProvider} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCompletionProvider.sourceType(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 sourceType 的处理结果；returns the result of the operation.
     */
    String sourceType();

    /**
     * 中文说明：执行 complete 操作；该方法是 {@code McpCompletionProvider} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the complete operation; this method is the invocation entry point on {@code McpCompletionProvider} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCompletionProvider.complete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 complete 的处理结果；returns the result of the operation.
     */
    Publisher<Result> complete(Request request);

    /**
     * 中文说明：执行 sensitiveArgumentName 操作；该方法是 {@code McpCompletionProvider} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the sensitive argument name operation; this method is the invocation entry point on {@code McpCompletionProvider} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCompletionProvider.sensitiveArgumentName(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 sensitiveArgumentName 的处理结果；returns the result of the operation.
     */
    static boolean sensitiveArgumentName(String value) {
        String normalized = value.toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "");
        return normalized.contains("secret")
                || normalized.contains("password")
                || normalized.contains("token")
                || normalized.contains("credential")
                || normalized.contains("privatekey")
                || normalized.contains("apikey");
    }

    /**
     * 中文说明：执行 sensitive值 操作；该方法是 {@code McpCompletionProvider} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the sensitive value operation; this method is the invocation entry point on {@code McpCompletionProvider} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCompletionProvider.sensitiveValue(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 sensitive值 的处理结果；returns the result of the operation.
     */
    static boolean sensitiveValue(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.contains("secret://")
                || normalized.contains("vault://")
                || normalized.contains("password=")
                || normalized.contains("-----begin private key");
    }

    /**
     * 中文说明：{@code Request} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责请求相关的职责与边界。
     * English summary: {@code Request} is an immutable data carrier in the current Gateway module; it owns the request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param referenceType 参数 referenceType；parameter reference type。
     * @param referenceName 参数 referenceName；parameter reference name。
     * @param argumentName 参数 argumentName；parameter argument name。
     * @param valuePrefix 参数 值Prefix；parameter value prefix。
     * @param operationId 参数 操作Id；parameter operation id。
     * @param attributes 参数 attributes；parameter attributes。
     */
    record Request(
            /**
             * 中文说明：保存 服务器Code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpCompletionProvider.Request} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server code; its type is {@code String}, and {@code McpCompletionProvider.Request} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpCompletionProvider.Request} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCompletionProvider.Request}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serverCode,
            /**
             * 中文说明：保存 referenceType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpCompletionProvider.Request} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by reference type; its type is {@code String}, and {@code McpCompletionProvider.Request} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpCompletionProvider.Request} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCompletionProvider.Request}; do not couple callers to its representation when the owning type exposes an API.
             */
            String referenceType,
            /**
             * 中文说明：保存 referenceName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpCompletionProvider.Request} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by reference name; its type is {@code String}, and {@code McpCompletionProvider.Request} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpCompletionProvider.Request} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCompletionProvider.Request}; do not couple callers to its representation when the owning type exposes an API.
             */
            String referenceName,
            /**
             * 中文说明：保存 argumentName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpCompletionProvider.Request} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by argument name; its type is {@code String}, and {@code McpCompletionProvider.Request} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpCompletionProvider.Request} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCompletionProvider.Request}; do not couple callers to its representation when the owning type exposes an API.
             */
            String argumentName,
            /**
             * 中文说明：保存 值Prefix 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpCompletionProvider.Request} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by value prefix; its type is {@code String}, and {@code McpCompletionProvider.Request} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpCompletionProvider.Request} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCompletionProvider.Request}; do not couple callers to its representation when the owning type exposes an API.
             */
            String valuePrefix,
            /**
             * 中文说明：保存 操作Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpCompletionProvider.Request} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by operation id; its type is {@code String}, and {@code McpCompletionProvider.Request} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpCompletionProvider.Request} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCompletionProvider.Request}; do not couple callers to its representation when the owning type exposes an API.
             */
            String operationId,
            /**
             * 中文说明：保存 attributes 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code McpCompletionProvider.Request} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by attributes; its type is {@code Map<String, Object>}, and {@code McpCompletionProvider.Request} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpCompletionProvider.Request} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCompletionProvider.Request}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> attributes
    ) {

        /**
         * 中文说明：创建 {@code McpCompletionProvider.Request} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code McpCompletionProvider.Request} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param serverCode 参数 服务器Code；parameter server code。
         * @param referenceType 参数 referenceType；parameter reference type。
         * @param referenceName 参数 referenceName；parameter reference name。
         * @param argumentName 参数 argumentName；parameter argument name。
         * @param valuePrefix 参数 值Prefix；parameter value prefix。
         * @param operationId 参数 操作Id；parameter operation id。
         * @param attributes 参数 attributes；parameter attributes。
         */
        public Request {
            serverCode = required(serverCode, "serverCode");
            referenceType = required(referenceType, "referenceType");
            referenceName = required(referenceName, "referenceName");
            argumentName = required(argumentName, "argumentName");
            valuePrefix = valuePrefix == null ? "" : valuePrefix;
            if (valuePrefix.length() > 256) {
                throw McpPromptDriver.invalid(
                        "MCP completion prefix is too large"
                );
            }
            operationId = operationId == null || operationId.isBlank()
                    ? null
                    : operationId.trim();
            attributes = attributes == null ? Map.of() : Map.copyOf(
                    attributes
            );
        }
    }

    /**
     * 中文说明：{@code Result} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Result相关的职责与边界。
     * English summary: {@code Result} is an immutable data carrier in the current Gateway module; it owns the result-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param values 参数 values；parameter values。
     * @param total 参数 total；parameter total。
     * @param hasMore 参数 hasMore；parameter has more。
     */
    record Result(
    /**
     * 中文说明：保存 values 对应的状态、依赖或配置值；字段类型为 {@code List<String>}，由 {@code McpCompletionProvider.Result} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by values; its type is {@code List<String>}, and {@code McpCompletionProvider.Result} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpCompletionProvider.Result} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCompletionProvider.Result}; do not couple callers to its representation when the owning type exposes an API.
     */
    List<String> values,
    /**
     * 中文说明：保存 total 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code McpCompletionProvider.Result} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by total; its type is {@code int}, and {@code McpCompletionProvider.Result} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpCompletionProvider.Result} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCompletionProvider.Result}; do not couple callers to its representation when the owning type exposes an API.
     */
    int total,
    /**
     * 中文说明：保存 hasMore 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code McpCompletionProvider.Result} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by has more; its type is {@code boolean}, and {@code McpCompletionProvider.Result} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpCompletionProvider.Result} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpCompletionProvider.Result}; do not couple callers to its representation when the owning type exposes an API.
     */
    boolean hasMore) {

        /**
         * 中文说明：创建 {@code McpCompletionProvider.Result} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code McpCompletionProvider.Result} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param values 参数 values；parameter values。
         * @param total 参数 total；parameter total。
         * @param hasMore 参数 hasMore；parameter has more。
         */
        public Result {
            values = List.copyOf(Objects.requireNonNull(values, "values"));
            if (values.size() > 100 || total < values.size()) {
                throw new IllegalArgumentException(
                        "MCP completion result is invalid"
                );
            }
        }
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code McpCompletionProvider} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code McpCompletionProvider} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpCompletionProvider.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw McpPromptDriver.invalid(
                    "MCP completion " + field + " is required"
            );
        }
        return value.trim();
    }
}
