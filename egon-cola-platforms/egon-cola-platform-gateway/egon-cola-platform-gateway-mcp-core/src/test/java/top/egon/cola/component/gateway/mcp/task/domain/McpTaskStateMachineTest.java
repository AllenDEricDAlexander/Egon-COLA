package top.egon.cola.component.gateway.mcp.task.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static top.egon.cola.component.gateway.mcp.task.domain.McpTask.State.COMPLETED;
import static top.egon.cola.component.gateway.mcp.task.domain.McpTask.State.INPUT_REQUIRED;
import static top.egon.cola.component.gateway.mcp.task.domain.McpTask.State.WORKING;

class McpTaskStateMachineTest {

    private final McpTaskStateMachine state = new McpTaskStateMachine();

    @Test
    void onlyDeclaredTransitionsAreAccepted() {
        assertEquals(INPUT_REQUIRED, state.transition(
                WORKING,
                McpTaskStateMachine.Event.REQUEST_INPUT
        ));
        assertEquals(WORKING, state.transition(
                INPUT_REQUIRED,
                McpTaskStateMachine.Event.PROVIDE_INPUT
        ));
        assertThrows(IllegalStateException.class, () -> state.transition(
                COMPLETED,
                McpTaskStateMachine.Event.CANCEL
        ));
    }
}
