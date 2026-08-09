package top.egon.cola.component.gateway.mcp.task;

import org.reactivestreams.Publisher;

import java.time.Instant;
import java.util.Map;

public interface McpTaskStore {

    Publisher<Void> create(McpTask task);

    Publisher<McpTask> find(String taskId);

    Publisher<McpTask> leaseNext(
            String workerOwner,
            Instant now,
            Instant leaseUntil
    );

    Publisher<Boolean> renewLease(
            String taskId,
            String workerOwner,
            Instant now,
            Instant leaseUntil
    );

    Publisher<Boolean> transition(Transition transition);

    Publisher<Boolean> cancel(
            String taskId,
            McpTask.State expectedState,
            long expectedRevision,
            Instant now
    );

    Publisher<Integer> failUnavailable(Instant now);

    Publisher<Integer> deleteExpired(Instant now);

    record Transition(
            String taskId,
            McpTask.State expectedState,
            McpTask.State targetState,
            long expectedRevision,
            String expectedWorkerOwner,
            Map<String, Object> inputPayload,
            Map<String, Object> resultPayload,
            Map<String, Object> errorPayload,
            Instant now
    ) {
    }
}
