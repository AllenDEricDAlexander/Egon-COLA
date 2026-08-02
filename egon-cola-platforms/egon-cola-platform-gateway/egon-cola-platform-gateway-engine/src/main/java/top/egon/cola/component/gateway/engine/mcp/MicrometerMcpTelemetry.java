package top.egon.cola.component.gateway.engine.mcp;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import top.egon.cola.component.gateway.mcp.telemetry.McpTelemetry;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Micrometer Adapter with a fixed low-cardinality MCP tag vocabulary.
 */
public final class MicrometerMcpTelemetry implements McpTelemetry {

    private static final Set<String> METHODS = Set.of(
            "initialize",
            "notifications/initialized",
            "ping",
            "server/discover",
            "tools/list",
            "tools/call",
            "resources/list",
            "resources/templates/list",
            "resources/read",
            "resources/subscribe",
            "resources/unsubscribe",
            "subscriptions/listen",
            "prompts/list",
            "prompts/get",
            "completion/complete",
            "tasks/get",
            "tasks/update",
            "tasks/cancel"
    );

    private static final Set<String> PRIMITIVES = Set.of(
            "LIFECYCLE",
            "TOOL",
            "RESOURCE",
            "SUBSCRIPTION",
            "PROMPT",
            "COMPLETION",
            "TASK",
            "APP",
            "UNKNOWN"
    );

    private final MeterRegistry meters;

    private final ObservationRegistry observations;

    public MicrometerMcpTelemetry(
            MeterRegistry meters,
            ObservationRegistry observations) {
        this.meters = Objects.requireNonNull(meters, "meters");
        this.observations = Objects.requireNonNull(
                observations,
                "observations"
        );
    }

    @Override
    public Scope start(Request request) {
        Objects.requireNonNull(request, "request");
        Tags tags = tags(request);
        Observation observation = Observation.createNotStarted(
                        "mcp.server.request",
                        observations
                )
                .contextualName("mcp " + tags.method())
                .lowCardinalityKeyValue("mcp.method", tags.method())
                .lowCardinalityKeyValue("mcp.primitive", tags.primitive())
                .lowCardinalityKeyValue("mcp.server", tags.server())
                .lowCardinalityKeyValue(
                        "mcp.remote.provider",
                        tags.remoteProvider()
                )
                .start();
        Timer.Sample sample = Timer.start(meters);
        return new MeteredScope(tags, observation, sample);
    }

    private Tags tags(Request request) {
        String method = METHODS.contains(request.method())
                ? request.method()
                : "unknown";
        String primitive = request.primitive().toUpperCase(Locale.ROOT);
        if (!PRIMITIVES.contains(primitive)) {
            primitive = "UNKNOWN";
        }
        return new Tags(
                method,
                primitive,
                code(request.serverCode()),
                request.remoteProviderCode() == null
                        ? "none"
                        : code(request.remoteProviderCode())
        );
    }

    private String code(String value) {
        return value.matches("[A-Za-z0-9][A-Za-z0-9_.-]{0,63}")
                ? value
                : "other";
    }

    private String status(String value) {
        if (value == null || value.isBlank()) {
            return "ERROR";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.matches("[A-Z][A-Z0-9_]{0,63}")
                ? normalized
                : "ERROR";
    }

    private final class MeteredScope implements Scope {

        private final Tags tags;

        private final Observation observation;

        private final Timer.Sample sample;

        private final AtomicBoolean completed = new AtomicBoolean();

        private final AtomicReference<String> remoteProvider;

        private MeteredScope(
                Tags tags,
                Observation observation,
                Timer.Sample sample) {
            this.tags = tags;
            this.observation = observation;
            this.sample = sample;
            this.remoteProvider = new AtomicReference<>(
                    tags.remoteProvider()
            );
        }

        @Override
        public void remoteProvider(String providerCode) {
            String value = code(Objects.requireNonNull(
                    providerCode,
                    "providerCode"
            ));
            remoteProvider.set(value);
            observation.lowCardinalityKeyValue(
                    "mcp.remote.provider",
                    value
            );
        }

        @Override
        public Child startChild(ChildKind kind) {
            Objects.requireNonNull(kind, "kind");
            if (completed.get()) {
                return Child.noop();
            }
            Observation child = Observation.createNotStarted(
                            "mcp.server." + kind.name()
                                    .toLowerCase(Locale.ROOT),
                            observations
                    )
                    .parentObservation(observation)
                    .lowCardinalityKeyValue("mcp.method", tags.method())
                    .lowCardinalityKeyValue(
                            "mcp.child.kind",
                            kind.name()
                    )
                    .start();
            Timer.Sample childSample = Timer.start(meters);
            return new MeteredChild(
                    kind,
                    tags,
                    remoteProvider.get(),
                    child,
                    childSample
            );
        }

        @Override
        public void success() {
            complete("SUCCESS", null);
        }

        @Override
        public void failure(String errorCode) {
            String failureStatus = status(errorCode);
            complete(
                    failureStatus,
                    new TelemetryFailure(failureStatus)
            );
        }

        private void complete(String status, RuntimeException failure) {
            if (!completed.compareAndSet(false, true)) {
                return;
            }
            observation.lowCardinalityKeyValue("mcp.status", status);
            if (failure != null) {
                observation.error(failure);
            }
            observation.stop();
            sample.stop(Timer.builder("gateway.mcp.requests")
                    .tags(tags.values(status, remoteProvider.get()))
                    .register(meters));
        }
    }

    private final class MeteredChild implements Child {

        private final ChildKind kind;

        private final Tags tags;

        private final String remoteProvider;

        private final Observation observation;

        private final Timer.Sample sample;

        private final AtomicBoolean completed = new AtomicBoolean();

        private MeteredChild(
                ChildKind kind,
                Tags tags,
                String remoteProvider,
                Observation observation,
                Timer.Sample sample) {
            this.kind = kind;
            this.tags = tags;
            this.remoteProvider = remoteProvider;
            this.observation = observation;
            this.sample = sample;
        }

        @Override
        public void success() {
            complete("SUCCESS", null);
        }

        @Override
        public void failure(String errorCode) {
            String failureStatus = status(errorCode);
            complete(
                    failureStatus,
                    new TelemetryFailure(failureStatus)
            );
        }

        private void complete(String status, RuntimeException failure) {
            if (!completed.compareAndSet(false, true)) {
                return;
            }
            observation.lowCardinalityKeyValue("mcp.status", status);
            if (failure != null) {
                observation.error(failure);
            }
            observation.stop();
            sample.stop(Timer.builder("gateway.mcp.children")
                    .tags(tags.values(status, remoteProvider))
                    .tag("mcp.child.kind", kind.name())
                    .register(meters));
        }
    }

    private record Tags(
            String method,
            String primitive,
            String server,
            String remoteProvider
    ) {

        private List<Tag> values(String status, String remoteProvider) {
            return List.of(
                    Tag.of("mcp.method", method),
                    Tag.of("mcp.primitive", primitive),
                    Tag.of("mcp.server", server),
                    Tag.of("mcp.remote.provider", remoteProvider),
                    Tag.of("mcp.status", status)
            );
        }
    }

    private static final class TelemetryFailure
            extends RuntimeException {

        private TelemetryFailure(String status) {
            super(status, null, false, false);
        }
    }
}
