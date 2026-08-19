package top.egon.cola.component.gateway.mcp.common.transport;

import org.reactivestreams.Publisher;

import java.time.Duration;
import java.time.Instant;

/**
 * Cross-node append/read contract used by MCP response and subscription streams.
 * 补充说明 / Supplementary summary: {@code McpSubscriptionEventStore} 是接口契约，位于当前 Gateway 模块的相关包中，负责MCP订阅事件存储相关的职责与边界。
 * English supplement: {@code McpSubscriptionEventStore} is an interface contract in the current Gateway module; it owns the mcp subscription event store-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public interface McpSubscriptionEventStore {

    /**
     * 中文说明：执行 append 操作；该方法是 {@code McpSubscriptionEventStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the append operation; this method is the invocation entry point on {@code McpSubscriptionEventStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSubscriptionEventStore.append(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param streamId 参数 streamId；parameter stream id。
     * @param type 参数 type；parameter type。
     * @param data 参数 data；parameter data。
     * @param ttl 参数 ttl；parameter ttl。
     * @return 返回 append 的处理结果；returns the result of the operation.
     */
    Publisher<Event> append(
            String streamId,
            String type,
            String data,
            Duration ttl
    );

    /**
     * 中文说明：执行 listen 操作；该方法是 {@code McpSubscriptionEventStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the listen operation; this method is the invocation entry point on {@code McpSubscriptionEventStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSubscriptionEventStore.listen(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param streamId 参数 streamId；parameter stream id。
     * @param afterEventId 参数 after事件Id；parameter after event id。
     * @param wait 参数 wait；parameter wait。
     * @return 返回 listen 的处理结果；returns the result of the operation.
     */
    Publisher<Event> listen(
            String streamId,
            String afterEventId,
            Duration wait
    );

    /**
     * 中文说明：{@code Event} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责事件相关的职责与边界。
     * English summary: {@code Event} is an immutable data carrier in the current Gateway module; it owns the event-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param eventId 参数 事件Id；parameter event id。
     * @param type 参数 type；parameter type。
     * @param data 参数 data；parameter data。
     * @param createdAt 参数 createdAt；parameter created at。
     */
    record Event(
            /**
             * 中文说明：保存 事件Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpSubscriptionEventStore.Event} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by event id; its type is {@code String}, and {@code McpSubscriptionEventStore.Event} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpSubscriptionEventStore.Event} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSubscriptionEventStore.Event}; do not couple callers to its representation when the owning type exposes an API.
             */
            String eventId,
            /**
             * 中文说明：保存 type 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpSubscriptionEventStore.Event} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by type; its type is {@code String}, and {@code McpSubscriptionEventStore.Event} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpSubscriptionEventStore.Event} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSubscriptionEventStore.Event}; do not couple callers to its representation when the owning type exposes an API.
             */
            String type,
            /**
             * 中文说明：保存 data 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpSubscriptionEventStore.Event} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by data; its type is {@code String}, and {@code McpSubscriptionEventStore.Event} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpSubscriptionEventStore.Event} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSubscriptionEventStore.Event}; do not couple callers to its representation when the owning type exposes an API.
             */
            String data,
            /**
             * 中文说明：保存 createdAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code McpSubscriptionEventStore.Event} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by created at; its type is {@code Instant}, and {@code McpSubscriptionEventStore.Event} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpSubscriptionEventStore.Event} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSubscriptionEventStore.Event}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant createdAt
    ) {

        /**
         * 中文说明：创建 {@code McpSubscriptionEventStore.Event} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code McpSubscriptionEventStore.Event} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param eventId 参数 事件Id；parameter event id。
         * @param type 参数 type；parameter type。
         * @param data 参数 data；parameter data。
         * @param createdAt 参数 createdAt；parameter created at。
         */
        public Event {
            eventId = required(eventId, "eventId");
            type = required(type, "type");
            data = java.util.Objects.requireNonNull(data, "data");
            createdAt = java.util.Objects.requireNonNull(
                    createdAt,
                    "createdAt"
            );
        }

        /**
         * 中文说明：执行 required 操作；该方法是 {@code McpSubscriptionEventStore.Event} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the required operation; this method is the invocation entry point on {@code McpSubscriptionEventStore.Event} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpSubscriptionEventStore.Event.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param value 参数 值；parameter value。
         * @param field 参数 field；parameter field。
         * @return 返回 required 的处理结果；returns the result of the operation.
         */
        private static String required(String value, String field) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(field + " is required");
            }
            return value.trim();
        }
    }
}
