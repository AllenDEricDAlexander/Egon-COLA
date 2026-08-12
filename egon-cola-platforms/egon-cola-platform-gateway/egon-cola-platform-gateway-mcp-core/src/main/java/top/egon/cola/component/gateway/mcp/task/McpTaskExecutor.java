package top.egon.cola.component.gateway.mcp.task;

import org.reactivestreams.Publisher;

import java.util.Map;
import java.util.Objects;

/**
 * 中文说明：{@code McpTaskExecutor} 是接口契约，位于当前 Gateway 模块的相关包中，负责MCP任务Executor相关的职责与边界。
 * English summary: {@code McpTaskExecutor} is an interface contract in the current Gateway module; it owns the mcp task executor-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@FunctionalInterface
public interface McpTaskExecutor {

    /**
     * 中文说明：执行 execute 操作；该方法是 {@code McpTaskExecutor} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the execute operation; this method is the invocation entry point on {@code McpTaskExecutor} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskExecutor.execute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param task 参数 任务；parameter task。
     * @return 返回 execute 的处理结果；returns the result of the operation.
     */
    Publisher<Outcome> execute(McpTask task);

    /**
     * 中文说明：{@code Outcome} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Outcome相关的职责与边界。
     * English summary: {@code Outcome} is an immutable data carrier in the current Gateway module; it owns the outcome-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param type 参数 type；parameter type。
     * @param inputRequestKey 参数 input请求键；parameter input request key。
     * @param payload 参数 payload；parameter payload。
     */
    record Outcome(
            /**
             * 中文说明：保存 type 对应的状态、依赖或配置值；字段类型为 {@code Type}，由 {@code McpTaskExecutor.Outcome} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by type; its type is {@code Type}, and {@code McpTaskExecutor.Outcome} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpTaskExecutor.Outcome} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskExecutor.Outcome}; do not couple callers to its representation when the owning type exposes an API.
             */
            Type type,
            /**
             * 中文说明：保存 input请求键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpTaskExecutor.Outcome} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by input request key; its type is {@code String}, and {@code McpTaskExecutor.Outcome} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpTaskExecutor.Outcome} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskExecutor.Outcome}; do not couple callers to its representation when the owning type exposes an API.
             */
            String inputRequestKey,
            /**
             * 中文说明：保存 payload 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code McpTaskExecutor.Outcome} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by payload; its type is {@code Map<String, Object>}, and {@code McpTaskExecutor.Outcome} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpTaskExecutor.Outcome} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskExecutor.Outcome}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> payload
    ) {

        /**
         * 中文说明：创建 {@code McpTaskExecutor.Outcome} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code McpTaskExecutor.Outcome} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param type 参数 type；parameter type。
         * @param inputRequestKey 参数 input请求键；parameter input request key。
         * @param payload 参数 payload；parameter payload。
         */
        public Outcome {
            type = Objects.requireNonNull(type, "type");
            inputRequestKey = inputRequestKey == null
                    || inputRequestKey.isBlank()
                    ? null
                    : inputRequestKey.trim();
            payload = payload == null ? Map.of() : Map.copyOf(payload);
            if ((type == Type.INPUT_REQUIRED)
                    != (inputRequestKey != null)) {
                throw new IllegalArgumentException(
                        "inputRequestKey is required only for input"
                );
            }
        }

        /**
         * 中文说明：执行 completed 操作；该方法是 {@code McpTaskExecutor.Outcome} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the completed operation; this method is the invocation entry point on {@code McpTaskExecutor.Outcome} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpTaskExecutor.Outcome.completed(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param result 参数 result；parameter result。
         * @return 返回 completed 的处理结果；returns the result of the operation.
         */
        public static Outcome completed(Map<String, Object> result) {
            return new Outcome(Type.COMPLETED, null, result);
        }

        /**
         * 中文说明：执行 inputRequired 操作；该方法是 {@code McpTaskExecutor.Outcome} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the input required operation; this method is the invocation entry point on {@code McpTaskExecutor.Outcome} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpTaskExecutor.Outcome.inputRequired(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param inputRequestKey 参数 input请求键；parameter input request key。
         * @param request 参数 请求；parameter request。
         * @return 返回 inputRequired 的处理结果；returns the result of the operation.
         */
        public static Outcome inputRequired(
                String inputRequestKey,
                Map<String, Object> request) {
            return new Outcome(Type.INPUT_REQUIRED, inputRequestKey, request);
        }

        /**
         * 中文说明：执行 failed 操作；该方法是 {@code McpTaskExecutor.Outcome} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the failed operation; this method is the invocation entry point on {@code McpTaskExecutor.Outcome} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpTaskExecutor.Outcome.failed(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param error 参数 error；parameter error。
         * @return 返回 failed 的处理结果；returns the result of the operation.
         */
        public static Outcome failed(Map<String, Object> error) {
            return new Outcome(Type.FAILED, null, error);
        }
    }

    /**
     * 中文说明：{@code Type} 是枚举类型，位于当前 Gateway 模块的相关包中，负责Type相关的职责与边界。
     * English summary: {@code Type} is an enumeration in the current Gateway module; it owns the type-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    enum Type {
        /**
         * 中文说明：表示 COMPLETED 这一固定值；它属于 {@code McpTaskExecutor.Type} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value completed; it is a state, type, or protocol value of {@code McpTaskExecutor.Type} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code McpTaskExecutor.Type} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskExecutor.Type}; do not couple callers to its representation when the owning type exposes an API.
         */
        COMPLETED,
        /**
         * 中文说明：表示 INPUTREQUIRED 这一固定值；它属于 {@code McpTaskExecutor.Type} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value input required; it is a state, type, or protocol value of {@code McpTaskExecutor.Type} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code McpTaskExecutor.Type} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskExecutor.Type}; do not couple callers to its representation when the owning type exposes an API.
         */
        INPUT_REQUIRED,
        /**
         * 中文说明：表示 FAILED 这一固定值；它属于 {@code McpTaskExecutor.Type} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value failed; it is a state, type, or protocol value of {@code McpTaskExecutor.Type} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code McpTaskExecutor.Type} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskExecutor.Type}; do not couple callers to its representation when the owning type exposes an API.
         */
        FAILED
    }
}
