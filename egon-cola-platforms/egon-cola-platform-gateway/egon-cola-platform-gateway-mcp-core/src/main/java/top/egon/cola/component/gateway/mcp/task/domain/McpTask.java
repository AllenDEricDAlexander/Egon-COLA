package top.egon.cola.component.gateway.mcp.task.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Durable MCP task snapshot. Bearer credentials are intentionally excluded.
 * 补充说明 / Supplementary summary: {@code McpTask} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责MCP任务相关的职责与边界。
 * English supplement: {@code McpTask} is an immutable data carrier in the current Gateway module; it owns the mcp task-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param id 参数 id；parameter id。
 * @param principalFingerprint 参数 principalFingerprint；parameter principal fingerprint。
 * @param subjectId 参数 subjectId；parameter subject id。
 * @param tenantId 参数 tenantId；parameter tenant id。
 * @param clientId 参数 客户端Id；parameter client id。
 * @param serverCode 参数 服务器Code；parameter server code。
 * @param toolName 参数 工具Name；parameter tool name。
 * @param requestDigest 参数 请求Digest；parameter request digest。
 * @param state 参数 state；parameter state。
 * @param inputPayload 参数 inputPayload；parameter input payload。
 * @param resultPayload 参数 resultPayload；parameter result payload。
 * @param errorPayload 参数 errorPayload；parameter error payload。
 * @param workerOwner 参数 workerOwner；parameter worker owner。
 * @param leaseUntil 参数 租约Until；parameter lease until。
 * @param executionDeadline 参数 executionDeadline；parameter execution deadline。
 * @param expiresAt 参数 expiresAt；parameter expires at。
 * @param attemptCount 参数 attemptCount；parameter attempt count。
 * @param maxAttempts 参数 maxAttempts；parameter max attempts。
 * @param revision 参数 revision；parameter revision。
 * @param createdAt 参数 createdAt；parameter created at。
 * @param updatedAt 参数 updatedAt；parameter updated at。
 */
public record McpTask(
        /**
         * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpTask} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code McpTask} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpTask} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTask}; do not couple callers to its representation when the owning type exposes an API.
         */
        String id,
        /**
         * 中文说明：保存 principalFingerprint 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpTask} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by principal fingerprint; its type is {@code String}, and {@code McpTask} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpTask} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTask}; do not couple callers to its representation when the owning type exposes an API.
         */
        String principalFingerprint,
        /**
         * 中文说明：保存 subjectId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpTask} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by subject id; its type is {@code String}, and {@code McpTask} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpTask} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTask}; do not couple callers to its representation when the owning type exposes an API.
         */
        String subjectId,
        /**
         * 中文说明：保存 tenantId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpTask} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by tenant id; its type is {@code String}, and {@code McpTask} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpTask} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTask}; do not couple callers to its representation when the owning type exposes an API.
         */
        String tenantId,
        /**
         * 中文说明：保存 客户端Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpTask} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by client id; its type is {@code String}, and {@code McpTask} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpTask} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTask}; do not couple callers to its representation when the owning type exposes an API.
         */
        String clientId,
        /**
         * 中文说明：保存 服务器Code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpTask} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by server code; its type is {@code String}, and {@code McpTask} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpTask} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTask}; do not couple callers to its representation when the owning type exposes an API.
         */
        String serverCode,
        /**
         * 中文说明：保存 工具Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpTask} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by tool name; its type is {@code String}, and {@code McpTask} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpTask} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTask}; do not couple callers to its representation when the owning type exposes an API.
         */
        String toolName,
        /**
         * 中文说明：保存 请求Digest 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpTask} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by request digest; its type is {@code String}, and {@code McpTask} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpTask} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTask}; do not couple callers to its representation when the owning type exposes an API.
         */
        String requestDigest,
        /**
         * 中文说明：保存 state 对应的状态、依赖或配置值；字段类型为 {@code State}，由 {@code McpTask} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by state; its type is {@code State}, and {@code McpTask} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpTask} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTask}; do not couple callers to its representation when the owning type exposes an API.
         */
        State state,
        /**
         * 中文说明：保存 inputPayload 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code McpTask} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by input payload; its type is {@code Map<String, Object>}, and {@code McpTask} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpTask} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTask}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, Object> inputPayload,
        /**
         * 中文说明：保存 resultPayload 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code McpTask} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by result payload; its type is {@code Map<String, Object>}, and {@code McpTask} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpTask} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTask}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, Object> resultPayload,
        /**
         * 中文说明：保存 errorPayload 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code McpTask} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by error payload; its type is {@code Map<String, Object>}, and {@code McpTask} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpTask} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTask}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, Object> errorPayload,
        /**
         * 中文说明：保存 workerOwner 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpTask} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by worker owner; its type is {@code String}, and {@code McpTask} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpTask} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTask}; do not couple callers to its representation when the owning type exposes an API.
         */
        String workerOwner,
        /**
         * 中文说明：保存 租约Until 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code McpTask} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by lease until; its type is {@code Instant}, and {@code McpTask} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpTask} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTask}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant leaseUntil,
        /**
         * 中文说明：保存 executionDeadline 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code McpTask} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by execution deadline; its type is {@code Instant}, and {@code McpTask} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpTask} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTask}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant executionDeadline,
        /**
         * 中文说明：保存 expiresAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code McpTask} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by expires at; its type is {@code Instant}, and {@code McpTask} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpTask} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTask}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant expiresAt,
        /**
         * 中文说明：保存 attemptCount 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code McpTask} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by attempt count; its type is {@code int}, and {@code McpTask} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpTask} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTask}; do not couple callers to its representation when the owning type exposes an API.
         */
        int attemptCount,
        /**
         * 中文说明：保存 maxAttempts 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code McpTask} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by max attempts; its type is {@code int}, and {@code McpTask} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpTask} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTask}; do not couple callers to its representation when the owning type exposes an API.
         */
        int maxAttempts,
        /**
         * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpTask} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code McpTask} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpTask} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTask}; do not couple callers to its representation when the owning type exposes an API.
         */
        long revision,
        /**
         * 中文说明：保存 createdAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code McpTask} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by created at; its type is {@code Instant}, and {@code McpTask} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpTask} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTask}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant createdAt,
        /**
         * 中文说明：保存 updatedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code McpTask} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by updated at; its type is {@code Instant}, and {@code McpTask} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code McpTask} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTask}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant updatedAt
) {

    /**
     * 中文说明：创建 {@code McpTask} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpTask} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param id 参数 id；parameter id。
     * @param principalFingerprint 参数 principalFingerprint；parameter principal fingerprint。
     * @param subjectId 参数 subjectId；parameter subject id。
     * @param tenantId 参数 tenantId；parameter tenant id。
     * @param clientId 参数 客户端Id；parameter client id。
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param toolName 参数 工具Name；parameter tool name。
     * @param requestDigest 参数 请求Digest；parameter request digest。
     * @param state 参数 state；parameter state。
     * @param inputPayload 参数 inputPayload；parameter input payload。
     * @param resultPayload 参数 resultPayload；parameter result payload。
     * @param errorPayload 参数 errorPayload；parameter error payload。
     * @param workerOwner 参数 workerOwner；parameter worker owner。
     * @param leaseUntil 参数 租约Until；parameter lease until。
     * @param executionDeadline 参数 executionDeadline；parameter execution deadline。
     * @param expiresAt 参数 expiresAt；parameter expires at。
     * @param attemptCount 参数 attemptCount；parameter attempt count。
     * @param maxAttempts 参数 maxAttempts；parameter max attempts。
     * @param revision 参数 revision；parameter revision。
     * @param createdAt 参数 createdAt；parameter created at。
     * @param updatedAt 参数 updatedAt；parameter updated at。
     */
    public McpTask {
        id = required(id, "id");
        if (id.length() > 64) {
            throw new IllegalArgumentException("task id is too long");
        }
        principalFingerprint = required(
                principalFingerprint,
                "principalFingerprint"
        );
        subjectId = required(subjectId, "subjectId");
        tenantId = required(tenantId, "tenantId");
        clientId = required(clientId, "clientId");
        serverCode = required(serverCode, "serverCode");
        toolName = required(toolName, "toolName");
        requestDigest = digest(requestDigest);
        state = Objects.requireNonNull(state, "state");
        inputPayload = inputPayload == null ? Map.of() : Map.copyOf(
                inputPayload
        );
        resultPayload = copy(resultPayload);
        errorPayload = copy(errorPayload);
        workerOwner = optional(workerOwner);
        if ((workerOwner == null) != (leaseUntil == null)) {
            throw new IllegalArgumentException(
                    "workerOwner and leaseUntil must be set together"
            );
        }
        executionDeadline = Objects.requireNonNull(
                executionDeadline,
                "executionDeadline"
        );
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        if (!executionDeadline.isAfter(createdAt)
                || !expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException(
                    "task deadlines must be after creation"
            );
        }
        if (attemptCount < 0 || maxAttempts < 1
                || attemptCount > maxAttempts || revision < 0) {
            throw new IllegalArgumentException(
                    "task attempts or revision are invalid"
            );
        }
    }

    /**
     * 中文说明：执行 terminal 操作；该方法是 {@code McpTask} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the terminal operation; this method is the invocation entry point on {@code McpTask} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTask.terminal(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 terminal 的处理结果；returns the result of the operation.
     */
    public boolean terminal() {
        return Set.of(
                State.COMPLETED,
                State.FAILED,
                State.CANCELLED
        ).contains(state);
    }

    /**
     * 中文说明：{@code State} 是枚举类型，位于当前 Gateway 模块的相关包中，负责State相关的职责与边界。
     * English summary: {@code State} is an enumeration in the current Gateway module; it owns the state-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    public enum State {
        /**
         * 中文说明：表示 WORKING 这一固定值；它属于 {@code McpTask.State} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value working; it is a state, type, or protocol value of {@code McpTask.State} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code McpTask.State} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTask.State}; do not couple callers to its representation when the owning type exposes an API.
         */
        WORKING,
        /**
         * 中文说明：表示 INPUTREQUIRED 这一固定值；它属于 {@code McpTask.State} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value input required; it is a state, type, or protocol value of {@code McpTask.State} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code McpTask.State} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTask.State}; do not couple callers to its representation when the owning type exposes an API.
         */
        INPUT_REQUIRED,
        /**
         * 中文说明：表示 COMPLETED 这一固定值；它属于 {@code McpTask.State} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value completed; it is a state, type, or protocol value of {@code McpTask.State} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code McpTask.State} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTask.State}; do not couple callers to its representation when the owning type exposes an API.
         */
        COMPLETED,
        /**
         * 中文说明：表示 FAILED 这一固定值；它属于 {@code McpTask.State} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value failed; it is a state, type, or protocol value of {@code McpTask.State} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code McpTask.State} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTask.State}; do not couple callers to its representation when the owning type exposes an API.
         */
        FAILED,
        /**
         * 中文说明：表示 CANCELLED 这一固定值；它属于 {@code McpTask.State} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value cancelled; it is a state, type, or protocol value of {@code McpTask.State} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code McpTask.State} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTask.State}; do not couple callers to its representation when the owning type exposes an API.
         */
        CANCELLED
    }

    /**
     * 中文说明：执行 copy 操作；该方法是 {@code McpTask} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the copy operation; this method is the invocation entry point on {@code McpTask} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTask.copy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 copy 的处理结果；returns the result of the operation.
     */
    private static Map<String, Object> copy(Map<String, Object> value) {
        return value == null ? null : Map.copyOf(value);
    }

    /**
     * 中文说明：执行 digest 操作；该方法是 {@code McpTask} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the digest operation; this method is the invocation entry point on {@code McpTask} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTask.digest(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 digest 的处理结果；returns the result of the operation.
     */
    private static String digest(String value) {
        String result = required(value, "requestDigest");
        if (!result.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(
                    "requestDigest must contain a SHA-256 digest"
            );
        }
        return result;
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code McpTask} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code McpTask} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTask.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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

    /**
     * 中文说明：执行 optional 操作；该方法是 {@code McpTask} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the optional operation; this method is the invocation entry point on {@code McpTask} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTask.optional(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 optional 的处理结果；returns the result of the operation.
     */
    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
