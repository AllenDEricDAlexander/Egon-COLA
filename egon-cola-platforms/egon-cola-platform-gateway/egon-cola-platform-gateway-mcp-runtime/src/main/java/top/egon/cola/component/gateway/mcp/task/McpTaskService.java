package top.egon.cola.component.gateway.mcp.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;
import top.egon.cola.component.gateway.mcp.security.McpSecurityDigests;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Owns task IDs, owner isolation and all state-machine transitions.
 */
public final class McpTaskService {

    private final McpTaskStore store;

    private final ObjectMapper objectMapper;

    private final Clock clock;

    private final Duration leaseDuration;

    private final SecureRandom random = new SecureRandom();

    private final McpTaskStateMachine states = new McpTaskStateMachine();

    public McpTaskService(
            McpTaskStore store,
            ObjectMapper objectMapper,
            Clock clock,
            Duration leaseDuration) {
        this.store = Objects.requireNonNull(store, "store");
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
        this.clock = Objects.requireNonNull(clock, "clock");
        this.leaseDuration = positive(leaseDuration, "leaseDuration");
    }

    public Publisher<McpTask> create(
            CreateRequest request,
            Owner owner) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(owner, "owner");
        validatePayload(request.inputPayload());
        Instant now = clock.instant();
        McpTask task = new McpTask(
                taskId(),
                owner.fingerprint(),
                owner.subjectId(),
                owner.tenantId(),
                owner.clientId(),
                request.serverCode(),
                request.toolName(),
                request.requestDigest(),
                McpTask.State.WORKING,
                request.inputPayload(),
                null,
                null,
                null,
                null,
                now.plus(request.executionTimeout()),
                now.plus(request.resultTtl()),
                0,
                request.maxAttempts(),
                0L,
                now,
                now
        );
        return Mono.from(store.create(task)).thenReturn(task);
    }

    public Publisher<McpTask> get(String taskId, Owner owner) {
        return owned(taskId, owner).flatMap(task -> {
            if (task.terminal() && !task.expiresAt().isAfter(clock.instant())) {
                return Mono.error(notFound());
            }
            return Mono.just(task);
        });
    }

    public Publisher<McpTask> provideInput(
            String taskId,
            String inputRequestKey,
            Map<String, Object> input,
            Owner owner) {
        return owned(taskId, owner).flatMap(task -> {
            McpTask.State target = states.transition(
                    task.state(),
                    McpTaskStateMachine.Event.PROVIDE_INPUT
            );
            Object expected = task.inputPayload().get("inputRequestKey");
            if (!(expected instanceof String expectedKey)
                    || !expectedKey.equals(inputRequestKey)) {
                return Mono.error(invalid(
                        "MCP task input request key does not match"
                ));
            }
            LinkedHashMap<String, Object> payload = new LinkedHashMap<>(
                    task.inputPayload()
            );
            payload.put("inputResponseKey", inputRequestKey);
            payload.put("inputResponse", Map.copyOf(input));
            payload.remove("inputRequest");
            return transition(
                    task,
                    target,
                    null,
                    Map.copyOf(payload),
                    null,
                    null
            );
        });
    }

    public Publisher<McpTask> cancel(String taskId, Owner owner) {
        return owned(taskId, owner).flatMap(task -> {
            McpTask.State target = states.transition(
                    task.state(),
                    McpTaskStateMachine.Event.CANCEL
            );
            return Mono.from(store.cancel(
                            task.id(),
                            task.state(),
                            task.revision(),
                            clock.instant()
                    ))
                    .flatMap(updated -> updated
                            ? owned(task.id(), owner)
                            : Mono.error(conflict()));
        });
    }

    public Publisher<Void> executeNext(
            String workerOwner,
            McpTaskExecutor executor) {
        Instant now = clock.instant();
        return Mono.from(store.leaseNext(
                        workerOwner,
                        now,
                        now.plus(leaseDuration)
                ))
                .flatMap(task -> executeLeased(
                        task,
                        workerOwner,
                        executor
                ))
                .then();
    }

    private Mono<Void> executeLeased(
            McpTask task,
            String workerOwner,
            McpTaskExecutor executor) {
        Mono<McpTaskExecutor.Outcome> outcome = Mono.from(
                        executor.execute(task)
                )
                .onErrorReturn(McpTaskExecutor.Outcome.failed(Map.of(
                        "code", "MCP_TASK_EXECUTION_FAILED"
                )))
                .cache();
        Duration heartbeatInterval = leaseDuration.dividedBy(3L);
        Mono<Void> heartbeat = Flux.interval(heartbeatInterval)
                .concatMap(ignored -> {
                    Instant now = clock.instant();
                    return Mono.from(store.renewLease(
                                    task.id(),
                                    workerOwner,
                                    now,
                                    now.plus(leaseDuration)
                            ))
                            .flatMap(renewed -> renewed
                                    ? Mono.<Void>empty()
                                    : Mono.error(new IllegalStateException(
                                            "MCP task lease was lost"
                                    )));
                })
                .takeUntilOther(outcome)
                .then();
        return Mono.when(
                outcome.flatMap(value -> finish(
                        task,
                        workerOwner,
                        value
                )).then(),
                heartbeat
        );
    }

    public Publisher<Integer> cleanup() {
        Instant now = clock.instant();
        return Mono.zip(
                Mono.from(store.failUnavailable(now)),
                Mono.from(store.deleteExpired(now))
        ).map(result -> result.getT1() + result.getT2());
    }

    private Mono<McpTask> finish(
            McpTask task,
            String workerOwner,
            McpTaskExecutor.Outcome outcome) {
        return switch (outcome.type()) {
            case COMPLETED -> transition(
                    task,
                    states.transition(
                            task.state(),
                            McpTaskStateMachine.Event.COMPLETE
                    ),
                    workerOwner,
                    task.inputPayload(),
                    outcome.payload(),
                    null
            );
            case FAILED -> transition(
                    task,
                    states.transition(
                            task.state(),
                            McpTaskStateMachine.Event.FAIL
                    ),
                    workerOwner,
                    task.inputPayload(),
                    null,
                    outcome.payload()
            );
            case INPUT_REQUIRED -> {
                LinkedHashMap<String, Object> input = new LinkedHashMap<>(
                        task.inputPayload()
                );
                input.put("inputRequestKey", outcome.inputRequestKey());
                input.put("inputRequest", outcome.payload());
                input.remove("inputResponseKey");
                input.remove("inputResponse");
                yield transition(
                        task,
                        states.transition(
                                task.state(),
                                McpTaskStateMachine.Event.REQUEST_INPUT
                        ),
                        workerOwner,
                        Map.copyOf(input),
                        null,
                        null
                );
            }
        };
    }

    private Mono<McpTask> transition(
            McpTask task,
            McpTask.State target,
            String workerOwner,
            Map<String, Object> input,
            Map<String, Object> result,
            Map<String, Object> error) {
        return Mono.from(store.transition(new McpTaskStore.Transition(
                        task.id(),
                        task.state(),
                        target,
                        task.revision(),
                        workerOwner,
                        input,
                        result,
                        error,
                        clock.instant()
                )))
                .flatMap(updated -> updated
                        ? Mono.from(store.find(task.id()))
                        : Mono.error(conflict()));
    }

    private Mono<McpTask> owned(String taskId, Owner owner) {
        Objects.requireNonNull(owner, "owner");
        return Mono.from(store.find(required(taskId, "taskId")))
                .filter(task -> task.principalFingerprint().equals(
                                owner.fingerprint()
                        )
                        && task.subjectId().equals(owner.subjectId())
                        && task.tenantId().equals(owner.tenantId())
                        && task.clientId().equals(owner.clientId()))
                .switchIfEmpty(Mono.error(notFound()));
    }

    private String taskId() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void validatePayload(Map<String, Object> payload) {
        try {
            if (objectMapper.writeValueAsBytes(payload).length > 1024 * 1024) {
                throw invalid("MCP task input payload is too large");
            }
        } catch (McpProtocolException failure) {
            throw failure;
        } catch (Exception failure) {
            throw invalid("MCP task input payload is invalid");
        }
    }

    private McpProtocolException notFound() {
        return new McpProtocolException(
                McpErrorCode.MCP_TASK_NOT_FOUND,
                "MCP task was not found"
        );
    }

    private McpProtocolException conflict() {
        return invalid("MCP task changed concurrently");
    }

    private McpProtocolException invalid(String message) {
        return new McpProtocolException(
                McpErrorCode.MCP_INVALID_PARAMS,
                message
        );
    }

    private Duration positive(Duration value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    public record Owner(String subjectId, String tenantId, String clientId) {

        public Owner {
            subjectId = required(subjectId, "subjectId");
            tenantId = required(tenantId, "tenantId");
            clientId = required(clientId, "clientId");
        }

        public String fingerprint() {
            return McpSecurityDigests.token(
                    subjectId + '\0' + tenantId + '\0' + clientId
            );
        }
    }

    public record CreateRequest(
            String serverCode,
            String toolName,
            String requestDigest,
            Map<String, Object> inputPayload,
            Duration executionTimeout,
            Duration resultTtl,
            int maxAttempts
    ) {

        public CreateRequest {
            serverCode = required(serverCode, "serverCode");
            toolName = required(toolName, "toolName");
            requestDigest = required(requestDigest, "requestDigest");
            if (!requestDigest.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException(
                        "requestDigest must contain a SHA-256 digest"
                );
            }
            inputPayload = Map.copyOf(Objects.requireNonNull(
                    inputPayload,
                    "inputPayload"
            ));
            executionTimeout = requirePositive(
                    executionTimeout,
                    "executionTimeout"
            );
            resultTtl = requirePositive(resultTtl, "resultTtl");
            if (maxAttempts < 1 || maxAttempts > 100) {
                throw new IllegalArgumentException(
                        "maxAttempts must be between 1 and 100"
                );
            }
        }

        private static Duration requirePositive(
                Duration value,
                String field) {
            Objects.requireNonNull(value, field);
            if (value.isZero() || value.isNegative()) {
                throw new IllegalArgumentException(
                        field + " must be positive"
                );
            }
            return value;
        }
    }
}
