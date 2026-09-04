package top.egon.cola.component.agentflow.execution;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.agentflow.exception.AgentFlowException;
import top.egon.cola.component.agentflow.exception.AgentFlowSessionBusyException;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentFlowSessionExecutionGuardTest {

    @Test
    void permits_one_lease_per_session_tuple_and_releases_atomically() {
        AgentFlowSessionExecutionGuard guard = new AgentFlowSessionExecutionGuard();
        AgentFlowSessionExecutionGuard.Lease first = guard.acquire("flow", "user", "session");

        assertThatThrownBy(() -> guard.acquire("flow", "user", "session"))
                .isInstanceOf(AgentFlowSessionBusyException.class);
        assertThat(guard.activeCount()).isEqualTo(1);

        first.close();
        first.close();
        assertThat(guard.activeCount()).isZero();
        AgentFlowSessionExecutionGuard.Lease second = guard.acquire("flow", "user", "session");
        assertThat(second).isNotNull();
        second.close();
    }

    @Test
    void permits_different_tuples_in_parallel_and_rejects_new_work_when_closing() {
        AgentFlowSessionExecutionGuard guard = new AgentFlowSessionExecutionGuard();
        AgentFlowSessionExecutionGuard.Lease first = guard.acquire("flow", "user-a", "session");
        AgentFlowSessionExecutionGuard.Lease second = guard.acquire("flow", "user-b", "session");

        assertThat(guard.activeCount()).isEqualTo(2);
        assertThat(guard.beginClosing()).isTrue();
        assertThatThrownBy(() -> guard.acquire("flow", "user-c", "session"))
                .isInstanceOf(AgentFlowException.class);

        first.close();
        second.close();
        assertThat(guard.awaitQuiescence(Duration.ofMillis(50))).isTrue();
        guard.markClosed();
        assertThat(guard.isClosed()).isTrue();
    }
}
