package top.egon.cola.component.gateway.mcp.task;

import java.util.Map;

/**
 * 中文说明：{@code McpTaskStateMachine} 是类型，位于当前 Gateway 模块的相关包中，负责MCP任务StateMachine相关的职责与边界。
 * English summary: {@code McpTaskStateMachine} is a type in the current Gateway module; it owns the mcp task state machine-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpTaskStateMachine {

    /**
     * 中文说明：表示 TRANSITIONS 这一固定值；它属于 {@code McpTaskStateMachine} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value transitions; it is a state, type, or protocol value of {@code McpTaskStateMachine} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code McpTaskStateMachine} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskStateMachine}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Map<Transition, McpTask.State> TRANSITIONS = Map.of(
            new Transition(
                    McpTask.State.WORKING,
                    Event.REQUEST_INPUT
            ), McpTask.State.INPUT_REQUIRED,
            new Transition(
                    McpTask.State.INPUT_REQUIRED,
                    Event.PROVIDE_INPUT
            ), McpTask.State.WORKING,
            new Transition(
                    McpTask.State.WORKING,
                    Event.COMPLETE
            ), McpTask.State.COMPLETED,
            new Transition(
                    McpTask.State.WORKING,
                    Event.FAIL
            ), McpTask.State.FAILED,
            new Transition(
                    McpTask.State.WORKING,
                    Event.CANCEL
            ), McpTask.State.CANCELLED,
            new Transition(
                    McpTask.State.INPUT_REQUIRED,
                    Event.CANCEL
            ), McpTask.State.CANCELLED
    );

    /**
     * 中文说明：执行 transition 操作；该方法是 {@code McpTaskStateMachine} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the transition operation; this method is the invocation entry point on {@code McpTaskStateMachine} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpTaskStateMachine.transition(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param current 参数 current；parameter current。
     * @param event 参数 事件；parameter event。
     * @return 返回 transition 的处理结果；returns the result of the operation.
     */
    public McpTask.State transition(
            McpTask.State current,
            Event event) {
        McpTask.State target = TRANSITIONS.get(new Transition(current, event));
        if (target == null) {
            throw new IllegalStateException(
                    "MCP task transition is not allowed: "
                            + current + " + " + event
            );
        }
        return target;
    }

    /**
     * 中文说明：{@code Event} 是枚举类型，位于当前 Gateway 模块的相关包中，负责事件相关的职责与边界。
     * English summary: {@code Event} is an enumeration in the current Gateway module; it owns the event-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    public enum Event {
        /**
         * 中文说明：表示 请求INPUT 这一固定值；它属于 {@code McpTaskStateMachine.Event} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value request input; it is a state, type, or protocol value of {@code McpTaskStateMachine.Event} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code McpTaskStateMachine.Event} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskStateMachine.Event}; do not couple callers to its representation when the owning type exposes an API.
         */
        REQUEST_INPUT,
        /**
         * 中文说明：表示 PROVIDEINPUT 这一固定值；它属于 {@code McpTaskStateMachine.Event} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value provide input; it is a state, type, or protocol value of {@code McpTaskStateMachine.Event} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code McpTaskStateMachine.Event} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskStateMachine.Event}; do not couple callers to its representation when the owning type exposes an API.
         */
        PROVIDE_INPUT,
        /**
         * 中文说明：表示 COMPLETE 这一固定值；它属于 {@code McpTaskStateMachine.Event} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value complete; it is a state, type, or protocol value of {@code McpTaskStateMachine.Event} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code McpTaskStateMachine.Event} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskStateMachine.Event}; do not couple callers to its representation when the owning type exposes an API.
         */
        COMPLETE,
        /**
         * 中文说明：表示 FAIL 这一固定值；它属于 {@code McpTaskStateMachine.Event} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value fail; it is a state, type, or protocol value of {@code McpTaskStateMachine.Event} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code McpTaskStateMachine.Event} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskStateMachine.Event}; do not couple callers to its representation when the owning type exposes an API.
         */
        FAIL,
        /**
         * 中文说明：表示 CANCEL 这一固定值；它属于 {@code McpTaskStateMachine.Event} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
         * English summary: Represents the fixed value cancel; it is a state, type, or protocol value of {@code McpTaskStateMachine.Event} and keeps callers aligned with the owning type.
         *
         * 用法 / Usage: 该字段通过 {@code McpTaskStateMachine.Event} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskStateMachine.Event}; do not couple callers to its representation when the owning type exposes an API.
         */
        CANCEL
    }

    /**
     * 中文说明：{@code Transition} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Transition相关的职责与边界。
     * English summary: {@code Transition} is an immutable data carrier in the current Gateway module; it owns the transition-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param state 参数 state；parameter state。
     * @param event 参数 事件；parameter event。
     */
    private record Transition(
    /**
     * 中文说明：保存 state 对应的状态、依赖或配置值；字段类型为 {@code McpTask.State}，由 {@code McpTaskStateMachine.Transition} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by state; its type is {@code McpTask.State}, and {@code McpTaskStateMachine.Transition} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpTaskStateMachine.Transition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskStateMachine.Transition}; do not couple callers to its representation when the owning type exposes an API.
     */
    McpTask.State state,
    /**
     * 中文说明：保存 事件 对应的状态、依赖或配置值；字段类型为 {@code Event}，由 {@code McpTaskStateMachine.Transition} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by event; its type is {@code Event}, and {@code McpTaskStateMachine.Transition} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpTaskStateMachine.Transition} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpTaskStateMachine.Transition}; do not couple callers to its representation when the owning type exposes an API.
     */
    Event event) {
    }
}
