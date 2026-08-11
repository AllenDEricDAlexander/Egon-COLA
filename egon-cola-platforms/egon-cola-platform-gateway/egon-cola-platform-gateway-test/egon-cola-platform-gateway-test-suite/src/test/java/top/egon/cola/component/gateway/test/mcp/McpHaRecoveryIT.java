package top.egon.cola.component.gateway.test.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuleContent;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeServer;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTool;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleActivation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleActivationMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.gateway.core.provider.ProviderCatalogSnapshot;
import top.egon.cola.component.gateway.core.provider.ProviderQuery;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.provider.ProviderServiceRegistry;
import top.egon.cola.component.gateway.core.provider.ProviderServiceSnapshot;
import top.egon.cola.component.gateway.core.provider.ProviderSubscription;
import top.egon.cola.component.gateway.engine.discovery.ProviderDirectory;
import top.egon.cola.component.gateway.engine.mcp.McpEngineHttpHandler;
import top.egon.cola.component.gateway.engine.mcp.McpTaskWorker;
import top.egon.cola.component.gateway.engine.rule.CompiledGatewayRules;
import top.egon.cola.component.gateway.engine.rule.EngineGatewayRuleCompiler;
import top.egon.cola.component.gateway.engine.rule.GatewayRuleActivationApplier;
import top.egon.cola.component.gateway.engine.rule.GatewayRuleChunkStore;
import top.egon.cola.component.gateway.engine.rule.GatewayRuleJsonCodec;
import top.egon.cola.component.gateway.engine.rule.GatewayRuleLkgRepository;
import top.egon.cola.component.gateway.mcp.rule.McpRuleCompiler;
import top.egon.cola.component.gateway.mcp.server.McpMethodDispatcher;
import top.egon.cola.component.gateway.mcp.server.handler.McpDiscoverHandler;
import top.egon.cola.component.gateway.mcp.server.handler.McpInitializeHandler;
import top.egon.cola.component.gateway.mcp.server.handler.McpInitializedHandler;
import top.egon.cola.component.gateway.mcp.server.handler.McpPingHandler;
import top.egon.cola.component.gateway.mcp.subscription.McpSubscriptionService;
import top.egon.cola.component.gateway.mcp.task.McpTask;
import top.egon.cola.component.gateway.mcp.task.McpTaskExecutor;
import top.egon.cola.component.gateway.mcp.task.McpTaskService;
import top.egon.cola.component.gateway.mcp.task.McpTaskStore;
import top.egon.cola.component.gateway.mcp.transport.McpHttpRequest;
import top.egon.cola.component.gateway.mcp.transport.McpHttpResponse;
import top.egon.cola.component.gateway.mcp.transport.McpSessionStore;
import top.egon.cola.component.gateway.mcp.transport.McpSubscriptionEventStore;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Shared-state, lease takeover and last-known-good recovery gates.
 */
class McpHaRecoveryIT {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir
    Path dataDirectory;

    @Test
    void sessionCallAndSubscriptionCrossNodeBoundaries() {
        SharedTransportStore shared = new SharedTransportStore();
        McpEngineHttpHandler nodeA = handler(shared);
        McpEngineHttpHandler nodeB = handler(shared);

        McpHttpResponse initialized = nodeA.handle(request(
                "POST",
                Map.of("content-type", "application/json"),
                """
                {"jsonrpc":"2.0","id":1,"method":"initialize",\
                "params":{"protocolVersion":"2025-11-25"}}
                """
        )).block();
        String sessionId = initialized.header("Mcp-Session-Id");
        assertNotNull(sessionId);

        McpHttpResponse posted = nodeB.handle(request(
                "POST",
                Map.of(
                        "content-type", "application/json",
                        "Mcp-Protocol-Version", "2025-11-25",
                        "Mcp-Session-Id", sessionId
                ),
                """
                {"jsonrpc":"2.0","id":2,"method":"ping","params":{}}
                """
        )).block();
        assertEquals(200, posted.status());

        McpHttpResponse stream = nodeA.handle(request(
                "GET",
                Map.of(
                        "accept", "text/event-stream",
                        "Mcp-Session-Id", sessionId
                ),
                ""
        )).block();
        String event = Flux.from(stream.body())
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                .next()
                .block(Duration.ofSeconds(1));
        assertTrue(event.contains("\"id\":2"));
        assertTrue(event.contains("\"result\":{}"));

        Clock clock = Clock.fixed(
                Instant.parse("2026-08-03T01:00:00Z"),
                ZoneOffset.UTC
        );
        McpSubscriptionService stableNode = new McpSubscriptionService(
                shared,
                MAPPER,
                clock,
                Duration.ofMinutes(30),
                Duration.ofMillis(20)
        );
        McpSubscriptionService rcNode = new McpSubscriptionService(
                shared,
                MAPPER,
                clock,
                Duration.ofMinutes(30),
                Duration.ofMillis(20)
        );
        String uri = "egon://orders/report/daily";
        Mono.from(stableNode.subscribe(sessionId, uri)).block();
        Mono.from(rcNode.publishUpdated(uri)).block();
        McpSubscriptionService.ResourceEvent update = Flux.from(
                stableNode.listen(uri, null)
        ).next().block(Duration.ofSeconds(1));
        assertEquals("UPDATED", update.kind());
        assertEquals(uri, update.uri());

        McpHttpResponse tooLarge = nodeA.handle(request(
                "POST",
                Map.of("content-type", "application/json"),
                "x".repeat(1024 * 1024 + 1)
        )).block();
        assertEquals(413, tooLarge.status());
    }

    @Test
    void expiredNodeALeaseIsRecoveredAndCompletedByNodeB() {
        Instant initial = Instant.parse("2026-08-03T02:00:00Z");
        InMemoryTaskStore store = new InMemoryTaskStore();
        McpTaskService.Owner owner = new McpTaskService.Owner(
                "subject-1",
                "tenant-1",
                "client-1"
        );
        McpTaskService nodeA = taskService(store, initial);
        McpTask created = Mono.from(nodeA.create(
                new McpTaskService.CreateRequest(
                        "billing",
                        "export_invoice",
                        "a".repeat(64),
                        Map.of("invoiceId", "invoice-1"),
                        Duration.ofMinutes(10),
                        Duration.ofHours(1),
                        3
                ),
                owner
        )).block();
        Mono.from(store.leaseNext(
                "node-a",
                initial,
                initial.plusSeconds(30)
        )).block();

        McpTaskService nodeB = taskService(
                store,
                initial.plusSeconds(31)
        );
        McpTaskWorker worker = new McpTaskWorker(
                nodeB,
                task -> Mono.just(McpTaskExecutor.Outcome.completed(
                        Map.of("exportId", "export-1")
                )),
                "node-b",
                Duration.ofSeconds(30),
                Duration.ofSeconds(1)
        );
        Mono.from(worker.runOnce()).block();

        McpTask recovered = Mono.from(nodeB.get(created.id(), owner)).block();
        assertEquals(McpTask.State.COMPLETED, recovered.state());
        assertEquals("export-1", recovered.resultPayload().get("exportId"));
        assertEquals(2, recovered.attemptCount());
    }

    @Test
    void invalidDdcReleaseKeepsActiveRulesAndRestartRestoresLkg() {
        GatewayRuleActivationApplier running = applier();
        TestRelease valid = release("release-1", validMcp());
        running.apply(
                GatewayRuleActivationApplier.ACTIVE_CONFIG_KEY,
                valid.activationJson(),
                1L
        );
        CompiledGatewayRules before = running.active();

        TestRelease invalid = release("release-2", invalidMcp());
        assertThrows(IllegalArgumentException.class, () -> running.apply(
                GatewayRuleActivationApplier.ACTIVE_CONFIG_KEY,
                invalid.activationJson(),
                2L
        ));
        assertSame(before, running.active());
        assertEquals("release-1", running.active().snapshot().releaseId());

        GatewayRuleActivationApplier restarted = applier();
        assertTrue(restarted.restoreLkg());
        assertEquals("release-1", restarted.active().snapshot().releaseId());
        assertTrue(restarted.active().mcpRules()
                .server("developer")
                .isPresent());
        assertTrue(restarted.status().degraded());
    }

    private McpEngineHttpHandler handler(SharedTransportStore store) {
        McpRuntimeServer server = new McpRuntimeServer(
                "server-1",
                "orders",
                "Orders",
                "Order capabilities",
                "Use approved operations.",
                Set.of(
                        McpProtocolDialect.STABLE_2025_11_25,
                        McpProtocolDialect.RC_2026_07_28
                ),
                "https://resource.egon.top/gateway-mcp",
                30L,
                true
        );
        var rules = new McpRuleCompiler().compile(new McpRuleContent(
                List.of(server),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        ));
        return new McpEngineHttpHandler(
                () -> rules,
                new McpMethodDispatcher(List.of(
                        new McpInitializeHandler(),
                        new McpInitializedHandler(),
                        new McpPingHandler(),
                        new McpDiscoverHandler()
                )),
                store,
                store,
                (request, selected) -> Mono.just(Map.of(
                        "callerId", "user-1",
                        "tenantId", "tenant-1",
                        "idp.client-id", "client-1",
                        "identity.session-id", "login-session-1",
                        "identity.token-id", "token-1"
                )),
                MAPPER,
                Clock.fixed(
                        Instant.parse("2026-08-03T00:00:00Z"),
                        ZoneOffset.UTC
                ),
                Duration.ofMinutes(30),
                Duration.ofMillis(20),
                1024 * 1024
        );
    }

    private McpHttpRequest request(
            String method,
            Map<String, String> headers,
            String body) {
        return new McpHttpRequest(
                method,
                "/mcp/orders",
                headers,
                body,
                Map.of()
        );
    }

    private McpTaskService taskService(
            McpTaskStore store,
            Instant now) {
        return new McpTaskService(
                store,
                MAPPER,
                Clock.fixed(now, ZoneOffset.UTC),
                Duration.ofSeconds(30)
        );
    }

    private GatewayRuleActivationApplier applier() {
        Clock clock = Clock.systemUTC();
        return new GatewayRuleActivationApplier(
                new GatewayRuleJsonCodec(),
                new EngineGatewayRuleCompiler(),
                new GatewayRuleChunkStore(),
                new ProviderDirectory(new EmptyRegistry(), clock),
                new GatewayRuleLkgRepository(dataDirectory, "developer"),
                clock
        );
    }

    private TestRelease release(String releaseId, McpRuleContent mcp) {
        GatewayRuleContent content = new GatewayRuleContent(
                "group-1",
                "developer",
                "local",
                "default",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                mcp
        );
        GatewayRuleJsonCodec codec = new GatewayRuleJsonCodec();
        Instant generatedAt = Instant.parse("2026-08-03T00:00:00Z");
        String contentSha = GatewayRuleJsonCodec.sha256(codec.write(content));
        String artifactSha = GatewayRuleJsonCodec.sha256(codec.write(Map.of(
                "content", content,
                "generatedAt", generatedAt,
                "releaseId", releaseId,
                "ruleContentSha256", contentSha,
                "ruleSchemaVersion", "v1"
        )));
        GatewayRuleSnapshot snapshot = new GatewayRuleSnapshot(
                "v1",
                releaseId,
                generatedAt,
                contentSha,
                artifactSha,
                content
        );
        byte[] snapshotJson = codec.write(snapshot);
        GatewayRuleActivation activation = new GatewayRuleActivation(
                "v1",
                releaseId,
                GatewayRuleActivationMode.INLINE,
                "v1",
                snapshotJson.length,
                contentSha,
                artifactSha,
                new String(snapshotJson, StandardCharsets.UTF_8),
                List.of()
        );
        return new TestRelease(new String(
                codec.write(activation),
                StandardCharsets.UTF_8
        ));
    }

    private McpRuleContent validMcp() {
        return new McpRuleContent(
                List.of(server()),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private McpRuleContent invalidMcp() {
        return new McpRuleContent(
                List.of(server()),
                List.of(new McpRuntimeTool(
                        "tool-1",
                        "missing-server",
                        "orders.get",
                        "Get an order",
                        "LOCAL_OPERATION",
                        "orders.query",
                        "HTTP",
                        null,
                        "{}",
                        "{}",
                        Map.of(),
                        Set.of(),
                        "LOW",
                        true,
                        true
                )),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private McpRuntimeServer server() {
        return new McpRuntimeServer(
                "server-1",
                "developer",
                "Developer",
                "Developer capabilities",
                "Use reviewed tools.",
                Set.of(McpProtocolDialect.STABLE_2025_11_25),
                "https://resource.egon.top/gateway-mcp",
                30L,
                true
        );
    }

    private record TestRelease(String activationJson) {
    }

    private static final class SharedTransportStore
            implements McpSessionStore, McpSubscriptionEventStore {

        private final Map<String, Session> sessions =
                new ConcurrentHashMap<>();

        private final Map<String, List<Event>> events =
                new ConcurrentHashMap<>();

        private final AtomicLong sequence = new AtomicLong();

        @Override
        public Mono<Void> create(Session session, Duration ttl) {
            sessions.put(session.sessionId(), session);
            return Mono.empty();
        }

        @Override
        public Mono<Session> find(String sessionId) {
            return Mono.justOrEmpty(sessions.get(sessionId));
        }

        @Override
        public Mono<Void> touch(String sessionId, Duration ttl) {
            return sessions.containsKey(sessionId)
                    ? Mono.empty()
                    : Mono.error(new IllegalStateException("missing session"));
        }

        @Override
        public Mono<Boolean> delete(String sessionId) {
            events.remove(sessionId);
            return Mono.just(sessions.remove(sessionId) != null);
        }

        @Override
        public Mono<Event> append(
                String streamId,
                String type,
                String data,
                Duration ttl) {
            Event event = new Event(
                    sequence.incrementAndGet() + "-0",
                    type,
                    data,
                    Instant.parse("2026-08-03T00:00:00Z")
            );
            events.computeIfAbsent(
                    streamId,
                    ignored -> java.util.Collections.synchronizedList(
                            new ArrayList<>()
                    )
            ).add(event);
            return Mono.just(event);
        }

        @Override
        public Flux<Event> listen(
                String streamId,
                String afterEventId,
                Duration wait) {
            long after = afterEventId == null || afterEventId.isBlank()
                    ? 0L
                    : Long.parseLong(afterEventId.split("-")[0]);
            return Flux.fromIterable(List.copyOf(events.getOrDefault(
                    streamId,
                    List.of()
            ))).filter(event -> Long.parseLong(
                    event.eventId().split("-")[0]
            ) > after);
        }
    }

    private static final class InMemoryTaskStore implements McpTaskStore {

        private final Map<String, McpTask> tasks = new ConcurrentHashMap<>();

        @Override
        public Mono<Void> create(McpTask task) {
            if (tasks.putIfAbsent(task.id(), task) != null) {
                return Mono.error(new IllegalStateException("duplicate task"));
            }
            return Mono.empty();
        }

        @Override
        public Mono<McpTask> find(String taskId) {
            return Mono.justOrEmpty(tasks.get(taskId));
        }

        @Override
        public synchronized Mono<McpTask> leaseNext(
                String workerOwner,
                Instant now,
                Instant leaseUntil) {
            return tasks.values().stream()
                    .filter(task -> task.state() == McpTask.State.WORKING)
                    .filter(task -> task.workerOwner() == null
                            || !task.leaseUntil().isAfter(now))
                    .filter(task -> task.executionDeadline().isAfter(now))
                    .filter(task -> task.attemptCount() < task.maxAttempts())
                    .sorted(Comparator.comparing(McpTask::createdAt))
                    .findFirst()
                    .map(task -> {
                        McpTask leased = copy(
                                task,
                                task.state(),
                                task.inputPayload(),
                                task.resultPayload(),
                                task.errorPayload(),
                                workerOwner,
                                leaseUntil,
                                task.attemptCount() + 1,
                                now
                        );
                        tasks.put(task.id(), leased);
                        return Mono.just(leased);
                    })
                    .orElseGet(Mono::empty);
        }

        @Override
        public synchronized Mono<Boolean> renewLease(
                String taskId,
                String workerOwner,
                Instant now,
                Instant leaseUntil) {
            McpTask task = tasks.get(taskId);
            if (task == null || !workerOwner.equals(task.workerOwner())
                    || task.leaseUntil() == null
                    || !task.leaseUntil().isAfter(now)) {
                return Mono.just(false);
            }
            tasks.put(taskId, copy(
                    task,
                    task.state(),
                    task.inputPayload(),
                    task.resultPayload(),
                    task.errorPayload(),
                    workerOwner,
                    leaseUntil,
                    task.attemptCount(),
                    now
            ));
            return Mono.just(true);
        }

        @Override
        public synchronized Mono<Boolean> transition(Transition transition) {
            McpTask task = tasks.get(transition.taskId());
            if (task == null
                    || task.state() != transition.expectedState()
                    || task.revision() != transition.expectedRevision()
                    || (transition.expectedWorkerOwner() != null
                    && !transition.expectedWorkerOwner().equals(
                    task.workerOwner()))) {
                return Mono.just(false);
            }
            boolean terminal = Set.of(
                    McpTask.State.COMPLETED,
                    McpTask.State.FAILED,
                    McpTask.State.CANCELLED,
                    McpTask.State.INPUT_REQUIRED
            ).contains(transition.targetState());
            tasks.put(task.id(), copy(
                    task,
                    transition.targetState(),
                    transition.inputPayload(),
                    transition.resultPayload(),
                    transition.errorPayload(),
                    terminal ? null : task.workerOwner(),
                    terminal ? null : task.leaseUntil(),
                    task.attemptCount(),
                    transition.now()
            ));
            return Mono.just(true);
        }

        @Override
        public synchronized Mono<Boolean> cancel(
                String taskId,
                McpTask.State expectedState,
                long expectedRevision,
                Instant now) {
            McpTask task = tasks.get(taskId);
            if (task == null || task.state() != expectedState
                    || task.revision() != expectedRevision) {
                return Mono.just(false);
            }
            tasks.put(taskId, copy(
                    task,
                    McpTask.State.CANCELLED,
                    task.inputPayload(),
                    null,
                    null,
                    null,
                    null,
                    task.attemptCount(),
                    now
            ));
            return Mono.just(true);
        }

        @Override
        public Mono<Integer> failUnavailable(Instant now) {
            return Mono.just(0);
        }

        @Override
        public Mono<Integer> deleteExpired(Instant now) {
            int before = tasks.size();
            tasks.values().removeIf(task -> task.terminal()
                    && !task.expiresAt().isAfter(now));
            return Mono.just(before - tasks.size());
        }

        private static McpTask copy(
                McpTask source,
                McpTask.State state,
                Map<String, Object> input,
                Map<String, Object> result,
                Map<String, Object> error,
                String workerOwner,
                Instant leaseUntil,
                int attempts,
                Instant now) {
            return new McpTask(
                    source.id(),
                    source.principalFingerprint(),
                    source.subjectId(),
                    source.tenantId(),
                    source.clientId(),
                    source.serverCode(),
                    source.toolName(),
                    source.requestDigest(),
                    state,
                    input,
                    result,
                    error,
                    workerOwner,
                    leaseUntil,
                    source.executionDeadline(),
                    source.expiresAt(),
                    attempts,
                    source.maxAttempts(),
                    source.revision() + 1,
                    source.createdAt(),
                    now
            );
        }
    }

    private static final class EmptyRegistry
            implements ProviderServiceRegistry {

        @Override
        public ProviderCatalogSnapshot getServiceKeys(ProviderQuery query) {
            return new ProviderCatalogSnapshot(1L, Instant.now(), List.of());
        }

        @Override
        public ProviderServiceSnapshot getInstances(ProviderServiceKey key) {
            return new ProviderServiceSnapshot(
                    key,
                    1L,
                    Instant.now(),
                    List.of()
            );
        }

        @Override
        public ProviderSubscription subscribeServices(
                ProviderQuery query,
                ProviderCatalogListener listener) {
            return subscription();
        }

        @Override
        public ProviderSubscription subscribe(
                ProviderServiceKey key,
                ProviderSnapshotListener listener) {
            return subscription();
        }

        private ProviderSubscription subscription() {
            return new ProviderSubscription() {
                @Override
                public boolean active() {
                    return true;
                }

                @Override
                public void close() {
                }
            };
        }
    }
}
