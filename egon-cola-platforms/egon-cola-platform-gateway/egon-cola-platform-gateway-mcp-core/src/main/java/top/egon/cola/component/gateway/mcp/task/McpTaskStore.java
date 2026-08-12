package top.egon.cola.component.gateway.mcp.task;

import org.reactivestreams.Publisher;

import java.time.Instant;
import java.util.Map;

/**
 * 中文说明：{@code McpTaskStore} 是接口契约，位于当前 Gateway 模块的相关包中，负责MCP任务存储相关的职责与边界。
 * English summary: {@code McpTaskStore} is an interface contract in the current Gateway module; it owns the mcp task store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public interface McpTaskStore {

    /**
     * 中文说明：执行 create 操作；该方法是 {@code McpTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create operation; this method is the invocation entry point on {@code McpTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskStore.create(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param task 参数 任务；parameter task。
     * @return 返回 create 的处理结果；returns the result of the operation.
     */
    Publisher<Void> create(McpTask task);

    /**
     * 中文说明：执行 find 操作；该方法是 {@code McpTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find operation; this method is the invocation entry point on {@code McpTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskStore.find(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param taskId 参数 任务Id；parameter task id。
     * @return 返回 find 的处理结果；returns the result of the operation.
     */
    Publisher<McpTask> find(String taskId);

    /**
     * 中文说明：执行 租约Next 操作；该方法是 {@code McpTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the lease next operation; this method is the invocation entry point on {@code McpTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskStore.leaseNext(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param workerOwner 参数 workerOwner；parameter worker owner。
     * @param now 参数 now；parameter now。
     * @param leaseUntil 参数 租约Until；parameter lease until。
     * @return 返回 租约Next 的处理结果；returns the result of the operation.
     */
    Publisher<McpTask> leaseNext(
            String workerOwner,
            Instant now,
            Instant leaseUntil
    );

    /**
     * 中文说明：执行 renew租约 操作；该方法是 {@code McpTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the renew lease operation; this method is the invocation entry point on {@code McpTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskStore.renewLease(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param taskId 参数 任务Id；parameter task id。
     * @param workerOwner 参数 workerOwner；parameter worker owner。
     * @param now 参数 now；parameter now。
     * @param leaseUntil 参数 租约Until；parameter lease until。
     * @return 返回 renew租约 的处理结果；returns the result of the operation.
     */
    Publisher<Boolean> renewLease(
            String taskId,
            String workerOwner,
            Instant now,
            Instant leaseUntil
    );

    /**
     * 中文说明：执行 transition 操作；该方法是 {@code McpTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the transition operation; this method is the invocation entry point on {@code McpTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskStore.transition(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param transition 参数 transition；parameter transition。
     * @return 返回 transition 的处理结果；returns the result of the operation.
     */
    Publisher<Boolean> transition(Transition transition);

    /**
     * 中文说明：执行 cancel 操作；该方法是 {@code McpTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the cancel operation; this method is the invocation entry point on {@code McpTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskStore.cancel(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param taskId 参数 任务Id；parameter task id。
     * @param expectedState 参数 expectedState；parameter expected state。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param now 参数 now；parameter now。
     * @return 返回 cancel 的处理结果；returns the result of the operation.
     */
    Publisher<Boolean> cancel(
            String taskId,
            McpTask.State expectedState,
            long expectedRevision,
            Instant now
    );

    /**
     * 中文说明：执行 failUnavailable 操作；该方法是 {@code McpTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the fail unavailable operation; this method is the invocation entry point on {@code McpTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskStore.failUnavailable(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param now 参数 now；parameter now。
     * @return 返回 failUnavailable 的处理结果；returns the result of the operation.
     */
    Publisher<Integer> failUnavailable(Instant now);

    /**
     * 中文说明：执行 deleteExpired 操作；该方法是 {@code McpTaskStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete expired operation; this method is the invocation entry point on {@code McpTaskStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskStore.deleteExpired(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param now 参数 now；parameter now。
     * @return 返回 deleteExpired 的处理结果；returns the result of the operation.
     */
    Publisher<Integer> deleteExpired(Instant now);

    /**
     * 中文说明：{@code Transition} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Transition相关的职责与边界。
     * English summary: {@code Transition} is an immutable data carrier in the current Gateway module; it owns the transition-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param taskId 参数 任务Id；parameter task id。
     * @param expectedState 参数 expectedState；parameter expected state。
     * @param targetState 参数 targetState；parameter target state。
     * @param expectedRevision 参数 expectedRevision；parameter expected revision。
     * @param expectedWorkerOwner 参数 expectedWorkerOwner；parameter expected worker owner。
     * @param inputPayload 参数 inputPayload；parameter input payload。
     * @param resultPayload 参数 resultPayload；parameter result payload。
     * @param errorPayload 参数 errorPayload；parameter error payload。
     * @param now 参数 now；parameter now。
     */
    record Transition(
            /**
             * 中文说明：保存 任务Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpTaskStore.Transition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by task id; its type is {@code String}, and {@code McpTaskStore.Transition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpTaskStore.Transition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskStore.Transition}; do not couple callers to its representation when the owning type exposes an API.
             */
            String taskId,
            /**
             * 中文说明：保存 expectedState 对应的状态、依赖或配置值；字段类型为 {@code McpTask.State}，由 {@code McpTaskStore.Transition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected state; its type is {@code McpTask.State}, and {@code McpTaskStore.Transition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpTaskStore.Transition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskStore.Transition}; do not couple callers to its representation when the owning type exposes an API.
             */
            McpTask.State expectedState,
            /**
             * 中文说明：保存 targetState 对应的状态、依赖或配置值；字段类型为 {@code McpTask.State}，由 {@code McpTaskStore.Transition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by target state; its type is {@code McpTask.State}, and {@code McpTaskStore.Transition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpTaskStore.Transition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskStore.Transition}; do not couple callers to its representation when the owning type exposes an API.
             */
            McpTask.State targetState,
            /**
             * 中文说明：保存 expectedRevision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpTaskStore.Transition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected revision; its type is {@code long}, and {@code McpTaskStore.Transition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpTaskStore.Transition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskStore.Transition}; do not couple callers to its representation when the owning type exposes an API.
             */
            long expectedRevision,
            /**
             * 中文说明：保存 expectedWorkerOwner 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpTaskStore.Transition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by expected worker owner; its type is {@code String}, and {@code McpTaskStore.Transition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpTaskStore.Transition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskStore.Transition}; do not couple callers to its representation when the owning type exposes an API.
             */
            String expectedWorkerOwner,
            /**
             * 中文说明：保存 inputPayload 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code McpTaskStore.Transition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by input payload; its type is {@code Map<String, Object>}, and {@code McpTaskStore.Transition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpTaskStore.Transition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskStore.Transition}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> inputPayload,
            /**
             * 中文说明：保存 resultPayload 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code McpTaskStore.Transition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by result payload; its type is {@code Map<String, Object>}, and {@code McpTaskStore.Transition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpTaskStore.Transition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskStore.Transition}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> resultPayload,
            /**
             * 中文说明：保存 errorPayload 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code McpTaskStore.Transition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by error payload; its type is {@code Map<String, Object>}, and {@code McpTaskStore.Transition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpTaskStore.Transition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskStore.Transition}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> errorPayload,
            /**
             * 中文说明：保存 now 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code McpTaskStore.Transition} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by now; its type is {@code Instant}, and {@code McpTaskStore.Transition} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpTaskStore.Transition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskStore.Transition}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant now
    ) {
    }
}
