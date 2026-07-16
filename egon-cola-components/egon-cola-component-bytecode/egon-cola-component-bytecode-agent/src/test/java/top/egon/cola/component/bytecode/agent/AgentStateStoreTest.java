package top.egon.cola.component.bytecode.agent;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.bytecode.bridge.AgentBridgeStatus;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;

import java.time.Instant;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentStateStoreTest {

    @Test
    void enforcesTransitionsAndBoundsFailuresPublishedThroughBridge() {
        AgentConfiguration configuration = new AgentConfigurationLoader(new Properties(), Map.of())
                .load("enabled=true,include=application.*,failure-capacity=2");
        AgentStateStore store = new AgentStateStore("test", 2);

        store.start(configuration);
        store.recordFailure(failure("first"));
        store.recordFailure(failure("second"));
        store.recordFailure(failure("third"));
        store.degraded();

        AgentBridgeStatus status = DispatcherRegistry.agentStatus();
        assertEquals(AgentState.DEGRADED.name(), status.state());
        assertEquals(2, status.recentFailures().size());
        assertEquals(3, status.failureCount());
        assertThrows(IllegalStateException.class, store::active);
    }

    private AgentFailure failure(String message) {
        return new AgentFailure(
                Instant.now(),
                "application.OrderService",
                "loader@1",
                "EXECUTOR",
                IllegalStateException.class.getName(),
                message,
                AgentFailurePolicy.SKIP_CLASS
        );
    }
}
