package top.egon.cola.component.gateway.engine.observability;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.transport.Kind;
import io.micrometer.observation.transport.ReceiverContext;
import io.micrometer.observation.transport.SenderContext;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.handler.TracingObservationHandler;
import top.egon.cola.component.gateway.contract.trace.GatewayTraceContext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.DoubleSupplier;

/**
 * Observation facade for the data-plane telemetry boundaries.
 */
public final class GatewayTelemetry {

    private static final String REQUEST = "gateway.engine.request";

    private static final String ATTEMPT =
            "gateway.engine.provider.attempt";

    private static final String DDC_APPLY = "gateway.engine.ddc.apply";

    private static final String KAFKA_SEND = "gateway.engine.kafka.send";

    private final ObservationRegistry registry;

    private final double samplingProbability;

    private final DoubleSupplier random;

    public GatewayTelemetry(ObservationRegistry registry) {
        this(registry, 0.1);
    }

    public GatewayTelemetry(
            ObservationRegistry registry,
            double samplingProbability) {
        this(
                registry,
                samplingProbability,
                () -> ThreadLocalRandom.current().nextDouble()
        );
    }

    GatewayTelemetry(
            ObservationRegistry registry,
            double samplingProbability,
            DoubleSupplier random) {
        this.registry = Objects.requireNonNull(registry, "registry");
        if (samplingProbability < 0 || samplingProbability > 1) {
            throw new IllegalArgumentException(
                    "samplingProbability must be between 0 and 1"
            );
        }
        this.samplingProbability = samplingProbability;
        this.random = Objects.requireNonNull(random, "random");
    }

    public static GatewayTelemetry noop() {
        return new GatewayTelemetry(
                ObservationRegistry.NOOP,
                0,
                () -> 1
        );
    }

    public Request startRequest(
            GatewayTraceContext selectedTrace,
            String protocol,
            String accessZone) {
        Map<String, String> carrier = inboundCarrier(selectedTrace);
        ReceiverContext<Map<String, String>> context =
                new ReceiverContext<>(
                        (source, name) -> source.get(name),
                        Kind.SERVER
                );
        context.setCarrier(carrier);
        Observation observation = Observation.createNotStarted(
                        REQUEST,
                        () -> context,
                        registry
                )
                .contextualName("gateway " + protocol.toLowerCase())
                .lowCardinalityKeyValue("gateway.protocol", protocol)
                .lowCardinalityKeyValue(
                        "gateway.access.zone",
                        accessZone
                )
                .highCardinalityKeyValue(
                        "gateway.trace.id",
                        selectedTrace.traceId()
                )
                .start();
        return new Request(
                observation,
                effectiveTrace(context, selectedTrace),
                protocol
        );
    }

    public Operation startDdcApply(String key, long version) {
        return operation(DDC_APPLY)
                .low("gateway.operation", "ddc.apply")
                .high("ddc.config.key", key)
                .high("ddc.config.version", Long.toString(version))
                .start();
    }

    public Operation startKafkaSend(
            String eventId,
            String traceId) {
        return operation(KAFKA_SEND)
                .low("gateway.operation", "kafka.send")
                .low("messaging.system", "kafka")
                .high("messaging.message.id", eventId)
                .high("gateway.trace.id", traceId)
                .start();
    }

    private Operation operation(String name) {
        return new Operation(
                Observation.createNotStarted(name, registry)
        );
    }

    private Map<String, String> inboundCarrier(
            GatewayTraceContext trace) {
        String parentId = trace.parentSpanId() == null
                ? trace.engineSpanId()
                : trace.parentSpanId();
        String flags = trace.source()
                == GatewayTraceContext.Source.TRACEPARENT
                ? trace.traceFlags()
                : random.getAsDouble() < samplingProbability
                ? "01"
                : "00";
        Map<String, String> carrier = new LinkedHashMap<>();
        carrier.put(
                "traceparent",
                "00-"
                        + trace.traceId()
                        + "-"
                        + parentId
                        + "-"
                        + flags
        );
        if (trace.tracestate() != null) {
            carrier.put("tracestate", trace.tracestate());
        }
        return carrier;
    }

    private static GatewayTraceContext effectiveTrace(
            Observation.Context context,
            GatewayTraceContext fallback) {
        TraceContext tracing = tracingContext(context);
        if (tracing == null
                || tracing.traceId() == null
                || tracing.spanId() == null) {
            return fallback;
        }
        return new GatewayTraceContext(
                tracing.traceId(),
                fallback.requestId(),
                tracing.parentId(),
                tracing.spanId(),
                Boolean.TRUE.equals(tracing.sampled()) ? "01" : "00",
                fallback.tracestate(),
                fallback.source(),
                fallback.headerConflict()
        );
    }

    private static TraceContext tracingContext(
            Observation.Context context) {
        TracingObservationHandler.TracingContext tracing = context.get(
                TracingObservationHandler.TracingContext.class
        );
        Span span = tracing == null ? null : tracing.getSpan();
        return span == null || span.isNoop() ? null : span.context();
    }

    public final class Request {

        private final Observation observation;

        private final GatewayTraceContext trace;

        private final String protocol;

        private final Map<String, Operation> attempts =
                new ConcurrentHashMap<>();

        private final AtomicBoolean stopped = new AtomicBoolean();

        private Request(
                Observation observation,
                GatewayTraceContext trace,
                String protocol) {
            this.observation = observation;
            this.trace = trace;
            this.protocol = protocol;
        }

        public GatewayTraceContext trace() {
            return trace;
        }

        public void route(
                String gatewayGroupId,
                String operationId,
                String routeId) {
            observation.highCardinalityKeyValue(
                    "gateway.group.id",
                    safe(gatewayGroupId)
            );
            observation.highCardinalityKeyValue(
                    "gateway.operation.id",
                    safe(operationId)
            );
            observation.highCardinalityKeyValue(
                    "gateway.route.id",
                    safe(routeId)
            );
        }

        public AttemptTrace startAttempt(
                int number,
                String providerInstanceId,
                String providerProtocol) {
            Map<String, String> carrier = new LinkedHashMap<>();
            SenderContext<Map<String, String>> context =
                    new SenderContext<>(
                            Map::put,
                            Kind.CLIENT
                    );
            context.setCarrier(carrier);
            context.setRemoteServiceName(providerInstanceId);
            Observation child = Observation.createNotStarted(
                            ATTEMPT,
                            () -> context,
                            registry
                    )
                    .parentObservation(observation)
                    .contextualName("gateway provider "
                            + providerProtocol.toLowerCase())
                    .lowCardinalityKeyValue(
                            "gateway.protocol",
                            protocol
                    )
                    .lowCardinalityKeyValue(
                            "gateway.provider.protocol",
                            providerProtocol
                    )
                    .highCardinalityKeyValue(
                            "gateway.provider.instance.id",
                            safe(providerInstanceId)
                    )
                    .highCardinalityKeyValue(
                            "gateway.attempt.number",
                            Integer.toString(number)
                    )
                    .start();
            TraceContext tracing = tracingContext(context);
            String spanId = tracing == null
                    ? trace.newChildSpanId()
                    : tracing.spanId();
            String traceparent = carrier.get("traceparent");
            if (traceparent == null) {
                traceparent = trace.childTraceparent(spanId);
            }
            String tracestate = carrier.getOrDefault(
                    "tracestate",
                    trace.tracestate()
            );
            attempts.put(spanId, new Operation(child));
            return new AttemptTrace(spanId, traceparent, tracestate);
        }

        public void finishAttempt(
                String spanId,
                String outcome,
                String retryReason,
                Throwable failure) {
            Operation attempt = attempts.remove(spanId);
            if (attempt == null) {
                return;
            }
            if (retryReason != null && !retryReason.isBlank()) {
                attempt.high("gateway.retry.reason", retryReason);
            }
            if (failure == null && "ERROR".equals(outcome)) {
                failure = new TelemetryFailure("provider attempt failed");
            }
            attempt.finish(outcome, failure);
        }

        public void finish(
                String terminalStage,
                String outcome,
                String errorCode) {
            if (!stopped.compareAndSet(false, true)) {
                return;
            }
            attempts.values().forEach(attempt ->
                    attempt.finish("CANCELLED", null)
            );
            attempts.clear();
            observation.lowCardinalityKeyValue(
                    "gateway.outcome",
                    safe(outcome)
            );
            observation.lowCardinalityKeyValue(
                    "gateway.terminal.stage",
                    safe(terminalStage)
            );
            if (errorCode != null && !errorCode.isBlank()) {
                observation.highCardinalityKeyValue(
                        "gateway.error.code",
                        errorCode
                );
                observation.error(new TelemetryFailure(errorCode));
            }
            observation.stop();
        }
    }

    public static final class Operation {

        private final Observation observation;

        private final AtomicBoolean stopped = new AtomicBoolean();

        private Operation(Observation observation) {
            this.observation = observation;
        }

        private Operation low(String name, String value) {
            observation.lowCardinalityKeyValue(name, safe(value));
            return this;
        }

        private Operation high(String name, String value) {
            observation.highCardinalityKeyValue(name, safe(value));
            return this;
        }

        private Operation start() {
            observation.start();
            return this;
        }

        public void success() {
            finish("SUCCESS", null);
        }

        public void ignored() {
            finish("IGNORED", null);
        }

        public void failure(Throwable failure) {
            finish("ERROR", failure);
        }

        private void finish(String outcome, Throwable failure) {
            if (!stopped.compareAndSet(false, true)) {
                return;
            }
            observation.lowCardinalityKeyValue(
                    "gateway.outcome",
                    outcome
            );
            if (failure != null) {
                observation.error(failure);
            }
            observation.stop();
        }
    }

    public record AttemptTrace(
            String spanId,
            String traceparent,
            String tracestate) {
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    private static final class TelemetryFailure
            extends RuntimeException {

        private TelemetryFailure(String code) {
            super(code, null, false, false);
        }
    }
}
