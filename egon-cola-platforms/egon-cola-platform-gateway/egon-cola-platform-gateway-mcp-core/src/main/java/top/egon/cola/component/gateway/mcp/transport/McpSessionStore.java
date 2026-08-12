package top.egon.cola.component.gateway.mcp.transport;

import org.reactivestreams.Publisher;

import java.time.Duration;
import java.time.Instant;

/**
 * Shared MCP session state. Credentials and request bodies must never be stored.
 * 补充说明 / Supplementary summary: {@code McpSessionStore} 是接口契约，位于当前 Gateway 模块的相关包中，负责MCP会话存储相关的职责与边界。
 * English supplement: {@code McpSessionStore} is an interface contract in the current Gateway module; it owns the mcp session store-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public interface McpSessionStore {

    /**
     * 中文说明：执行 create 操作；该方法是 {@code McpSessionStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create operation; this method is the invocation entry point on {@code McpSessionStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSessionStore.create(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param session 参数 会话；parameter session。
     * @param ttl 参数 ttl；parameter ttl。
     * @return 返回 create 的处理结果；returns the result of the operation.
     */
    Publisher<Void> create(Session session, Duration ttl);

    /**
     * 中文说明：执行 find 操作；该方法是 {@code McpSessionStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find operation; this method is the invocation entry point on {@code McpSessionStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSessionStore.find(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param sessionId 参数 会话Id；parameter session id。
     * @return 返回 find 的处理结果；returns the result of the operation.
     */
    Publisher<Session> find(String sessionId);

    /**
     * 中文说明：执行 touch 操作；该方法是 {@code McpSessionStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the touch operation; this method is the invocation entry point on {@code McpSessionStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSessionStore.touch(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param sessionId 参数 会话Id；parameter session id。
     * @param ttl 参数 ttl；parameter ttl。
     * @return 返回 touch 的处理结果；returns the result of the operation.
     */
    Publisher<Void> touch(String sessionId, Duration ttl);

    /**
     * 中文说明：执行 delete 操作；该方法是 {@code McpSessionStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete operation; this method is the invocation entry point on {@code McpSessionStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSessionStore.delete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param sessionId 参数 会话Id；parameter session id。
     * @return 返回 delete 的处理结果；returns the result of the operation.
     */
    Publisher<Boolean> delete(String sessionId);

    /**
     * 中文说明：{@code Session} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责会话相关的职责与边界。
     * English summary: {@code Session} is an immutable data carrier in the current Gateway module; it owns the session-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param sessionId 参数 会话Id；parameter session id。
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param subjectId 参数 subjectId；parameter subject id。
     * @param tenantId 参数 tenantId；parameter tenant id。
     * @param clientId 参数 客户端Id；parameter client id。
     * @param createdAt 参数 createdAt；parameter created at。
     */
    record Session(
            /**
             * 中文说明：保存 会话Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpSessionStore.Session} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by session id; its type is {@code String}, and {@code McpSessionStore.Session} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpSessionStore.Session} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSessionStore.Session}; do not couple callers to its representation when the owning type exposes an API.
             */
            String sessionId,
            /**
             * 中文说明：保存 服务器Code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpSessionStore.Session} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server code; its type is {@code String}, and {@code McpSessionStore.Session} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpSessionStore.Session} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSessionStore.Session}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serverCode,
            /**
             * 中文说明：保存 subjectId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpSessionStore.Session} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by subject id; its type is {@code String}, and {@code McpSessionStore.Session} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpSessionStore.Session} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSessionStore.Session}; do not couple callers to its representation when the owning type exposes an API.
             */
            String subjectId,
            /**
             * 中文说明：保存 tenantId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpSessionStore.Session} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by tenant id; its type is {@code String}, and {@code McpSessionStore.Session} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpSessionStore.Session} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSessionStore.Session}; do not couple callers to its representation when the owning type exposes an API.
             */
            String tenantId,
            /**
             * 中文说明：保存 客户端Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpSessionStore.Session} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by client id; its type is {@code String}, and {@code McpSessionStore.Session} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpSessionStore.Session} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSessionStore.Session}; do not couple callers to its representation when the owning type exposes an API.
             */
            String clientId,
            /**
             * 中文说明：保存 createdAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code McpSessionStore.Session} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by created at; its type is {@code Instant}, and {@code McpSessionStore.Session} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpSessionStore.Session} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpSessionStore.Session}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant createdAt
    ) {

        /**
         * 中文说明：创建 {@code McpSessionStore.Session} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code McpSessionStore.Session} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param sessionId 参数 会话Id；parameter session id。
         * @param serverCode 参数 服务器Code；parameter server code。
         * @param subjectId 参数 subjectId；parameter subject id。
         * @param tenantId 参数 tenantId；parameter tenant id。
         * @param clientId 参数 客户端Id；parameter client id。
         * @param createdAt 参数 createdAt；parameter created at。
         */
        public Session {
            sessionId = required(sessionId, "sessionId");
            serverCode = required(serverCode, "serverCode");
            subjectId = required(subjectId, "subjectId");
            tenantId = required(tenantId, "tenantId");
            clientId = required(clientId, "clientId");
            createdAt = java.util.Objects.requireNonNull(
                    createdAt,
                    "createdAt"
            );
        }

        /**
         * 中文说明：执行 required 操作；该方法是 {@code McpSessionStore.Session} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the required operation; this method is the invocation entry point on {@code McpSessionStore.Session} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpSessionStore.Session.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
