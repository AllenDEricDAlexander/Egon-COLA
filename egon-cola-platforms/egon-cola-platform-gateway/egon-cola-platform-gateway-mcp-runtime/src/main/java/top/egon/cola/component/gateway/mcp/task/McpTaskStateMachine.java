package top.egon.cola.component.gateway.mcp.task;

import java.util.Map;

public final class McpTaskStateMachine {

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

    public enum Event {
        REQUEST_INPUT,
        PROVIDE_INPUT,
        COMPLETE,
        FAIL,
        CANCEL
    }

    private record Transition(McpTask.State state, Event event) {
    }
}
