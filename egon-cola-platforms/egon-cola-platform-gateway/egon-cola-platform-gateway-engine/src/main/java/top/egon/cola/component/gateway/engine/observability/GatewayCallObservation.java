package top.egon.cola.component.gateway.engine.observability;

import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.contract.observability.GatewayCallEventV1;
import top.egon.cola.component.gateway.contract.trace.GatewayTraceContext;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mutable request-local builder that can publish exactly one immutable event.
 */
public final class GatewayCallObservation {

    private final Clock clock;

    private final GatewayTraceContext trace;

    private final GatewayTelemetry.Request telemetry;

    private final String requestId;

    private final String protocol;

    private final String accessZone;

    private final String engineNodeId;

    private final long occurredAt;

    private final long startedNanos;

    private final AtomicLong requestBytes = new AtomicLong();

    private final AtomicLong responseBytes = new AtomicLong();

    private final AtomicBoolean completed = new AtomicBoolean();

    private final List<GatewayCallEventV1.Attempt> attempts =
            new ArrayList<>();

    private volatile String normalizedMethod;

    private volatile String normalizedRouteTemplate;

    private volatile String env;

    private volatile String namespace;

    private volatile String gatewayGroupId;

    private volatile String releaseId;

    private volatile String operationId;

    private volatile String routeId;

    private volatile Map<String, Object> providerIdentity = Map.of();

    private volatile String terminalStage = "RECEIVE";

    private volatile String rateLimitDecision = "NOT_APPLIED";

    private volatile String circuitDecision = "NOT_APPLIED";

    private volatile String securityDecision = "NOT_APPLIED";

    public GatewayCallObservation(
            Clock clock,
            GatewayTraceContext trace,
            String requestId,
            String protocol,
            String accessZone,
            String engineNodeId) {
        this(
                clock,
                trace,
                requestId,
                protocol,
                accessZone,
                engineNodeId,
                GatewayTelemetry.noop()
        );
    }

    public GatewayCallObservation(
            Clock clock,
            GatewayTraceContext trace,
            String requestId,
            String protocol,
            String accessZone,
            String engineNodeId,
            GatewayTelemetry gatewayTelemetry) {
        this.clock = Objects.requireNonNull(clock, "clock");
        telemetry = Objects.requireNonNull(
                gatewayTelemetry,
                "gatewayTelemetry"
        ).startRequest(
                Objects.requireNonNull(trace, "trace"),
                protocol,
                accessZone
        );
        this.trace = telemetry.trace();
        this.requestId = required(requestId, "requestId");
        this.protocol = required(protocol, "protocol");
        this.accessZone = required(accessZone, "accessZone");
        this.engineNodeId = required(engineNodeId, "engineNodeId");
        occurredAt = clock.millis();
        startedNanos = System.nanoTime();
    }

    public static GatewayCallObservation start(
            GatewayTraceContext trace,
            String protocol,
            String accessZone,
            String engineNodeId) {
        return start(
                trace,
                protocol,
                accessZone,
                engineNodeId,
                GatewayTelemetry.noop()
        );
    }

    public static GatewayCallObservation start(
            GatewayTraceContext trace,
            String protocol,
            String accessZone,
            String engineNodeId,
            GatewayTelemetry telemetry) {
        return new GatewayCallObservation(
                Clock.systemUTC(),
                trace,
                trace.requestId(),
                protocol,
                accessZone,
                engineNodeId,
                telemetry
        );
    }

    public GatewayTraceContext trace() {
        return trace;
    }

    public void route(
            String normalizedMethod,
            String normalizedRouteTemplate,
            String gatewayGroupId,
            String releaseId,
            String operationId,
            String routeId) {
        this.normalizedMethod = safe(normalizedMethod);
        this.normalizedRouteTemplate = safe(normalizedRouteTemplate);
        this.gatewayGroupId = safe(gatewayGroupId);
        this.releaseId = safe(releaseId);
        this.operationId = safe(operationId);
        this.routeId = safe(routeId);
        terminalStage = "ROUTE";
        telemetry.route(gatewayGroupId, operationId, routeId);
    }

    public void scope(String env, String namespace) {
        this.env = safe(env);
        this.namespace = safe(namespace);
    }

    /**
     * Adds passive transport facts without changing the v1 event contract.
     */
    public void transport(
            String transportMode,
            String commitPoint,
            String terminationReason) {
        telemetry.transport(
                transportMode,
                commitPoint,
                terminationReason
        );
    }

    public void provider(
            String providerInstanceId,
            Map<String, Object> providerServiceIdentity) {
        LinkedHashMap<String, Object> safeIdentity = new LinkedHashMap<>();
        if (providerServiceIdentity != null) {
            copyIdentity(providerServiceIdentity, safeIdentity, "serviceKey");
            copyIdentity(providerServiceIdentity, safeIdentity, "protocol");
            copyIdentity(providerServiceIdentity, safeIdentity, "version");
            copyIdentity(providerServiceIdentity, safeIdentity, "group");
        }
        if (providerInstanceId != null) {
            safeIdentity.put("instanceId", providerInstanceId);
        }
        providerIdentity = Map.copyOf(safeIdentity);
        terminalStage = "PROVIDER";
    }

    public void governance(
            String rateLimitDecision,
            String circuitDecision,
            String securityDecision) {
        this.rateLimitDecision = safeDecision(rateLimitDecision);
        this.circuitDecision = safeDecision(circuitDecision);
        this.securityDecision = safeDecision(securityDecision);
        terminalStage = "GOVERNANCE";
    }

    public void addRequestBytes(long bytes) {
        if (bytes > 0) {
            requestBytes.addAndGet(bytes);
        }
    }

    public void addResponseBytes(long bytes) {
        if (bytes > 0) {
            responseBytes.addAndGet(bytes);
        }
    }

    public synchronized void attempt(
            int number,
            String spanId,
            String providerInstanceId,
            long startedAt,
            long durationMs,
            String category,
            String retryReason) {
        telemetry.finishAttempt(
                spanId,
                category,
                retryReason,
                null
        );
        attempts.add(new GatewayCallEventV1.Attempt(
                number,
                spanId,
                providerInstanceId,
                startedAt,
                durationMs,
                safe(category),
                safe(retryReason)
        ));
    }

    public GatewayTelemetry.AttemptTrace beginAttempt(
            int number,
            String providerInstanceId,
            String providerProtocol) {
        return telemetry.startAttempt(
                number,
                providerInstanceId,
                providerProtocol
        );
    }

    public Optional<GatewayCallEventV1> complete(
            String terminalStage,
            String category,
            String gatewayErrorCode,
            Integer httpStatus,
            String grpcStatus) {
        if (!completed.compareAndSet(false, true)) {
            return Optional.empty();
        }
        telemetry.finish(terminalStage, category, gatewayErrorCode);
        long completedAt = Math.max(clock.millis(), occurredAt);
        long durationMs = Math.max(
                0,
                (System.nanoTime() - startedNanos) / 1_000_000
        );
        List<GatewayCallEventV1.Attempt> attemptSnapshot;
        synchronized (this) {
            attemptSnapshot = List.copyOf(attempts);
        }
        return Optional.of(new GatewayCallEventV1(
                "v1",
                UuidV7.simpleString(),
                occurredAt,
                completedAt,
                new GatewayCallEventV1.Trace(
                        trace.traceId(),
                        trace.engineSpanId(),
                        trace.sampled()
                ),
                new GatewayCallEventV1.Request(
                        requestId,
                        protocol,
                        accessZone,
                        safe(normalizedMethod),
                        safe(normalizedRouteTemplate),
                        requestBytes.get(),
                        "UNSPECIFIED"
                ),
                new GatewayCallEventV1.Routing(
                        safe(env),
                        safe(namespace),
                        safe(gatewayGroupId),
                        engineNodeId,
                        safe(releaseId),
                        safe(operationId),
                        safe(routeId),
                        providerIdentity
                ),
                new GatewayCallEventV1.Governance(
                        safe(terminalStage == null
                                ? this.terminalStage
                                : terminalStage),
                        rateLimitDecision,
                        circuitDecision,
                        securityDecision,
                        Math.max(0, attemptSnapshot.size() - 1)
                ),
                new GatewayCallEventV1.Result(
                        safe(category),
                        safe(gatewayErrorCode),
                        httpStatus,
                        safe(grpcStatus),
                        responseBytes.get(),
                        durationMs
                ),
                attemptSnapshot
        ));
    }

    private static void copyIdentity(
            Map<String, Object> source,
            Map<String, Object> target,
            String key) {
        Object value = source.get(key);
        if (value instanceof String text && !text.isBlank()) {
            target.put(key, text);
        }
    }

    private static String safeDecision(String value) {
        return value == null || value.isBlank() ? "NOT_APPLIED" : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
