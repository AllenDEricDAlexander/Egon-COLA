package top.egon.cola.component.gateway.engine.observability;

import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingSenderTracingObservationHandler;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelPropagator;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.trace.GatewayTraceContext;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayTelemetryTest {

    @Test
    void preservesW3cTraceAndCreatesRequestAttemptDdcAndKafkaSpans() {
        CollectingSpanExporter exporter = new CollectingSpanExporter();
        SdkTracerProvider provider = SdkTracerProvider.builder()
                .setSampler(Sampler.alwaysOn())
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(provider)
                .setPropagators(ContextPropagators.create(
                        W3CTraceContextPropagator.getInstance()
                ))
                .build();
        try {
            ObservationRegistry registry = registry(openTelemetry);
            GatewayTelemetry telemetry = new GatewayTelemetry(registry);
            String traceId = "0123456789abcdef0123456789abcdef";
            GatewayTelemetry.Request request = telemetry.startRequest(
                    GatewayTraceContext.fromHeaders(
                            "00-"
                                    + traceId
                                    + "-0123456789abcdef-01",
                            "vendor=value",
                            null
                    ),
                    "HTTP",
                    "PUBLIC"
            );
            request.route("group-1", "operation-1", "route-1");
            request.transport(
                    "HTTP_STREAMING",
                    "FIRST_BODY_BUFFER_SENT",
                    "COMPLETED"
            );

            GatewayTelemetry.AttemptTrace attempt =
                    request.startAttempt(1, "provider-1", "HTTP");
            request.finishAttempt(
                    attempt.spanId(),
                    "SUCCESS",
                    null,
                    null
            );
            request.finish("COMPLETE", "SUCCESS", null);
            telemetry.startDdcApply("gateway.rules.active", 12)
                    .success();
            telemetry.startKafkaSend("event-1", traceId)
                    .failure(new IllegalStateException("broker down"));

            assertEquals(traceId, request.trace().traceId());
            assertNotEquals(
                    "0123456789abcdef",
                    request.trace().engineSpanId()
            );
            assertTrue(attempt.traceparent().startsWith(
                    "00-" + traceId + "-" + attempt.spanId()
            ));

            SpanData requestSpan = span(exporter, "gateway http");
            SpanData attemptSpan = span(
                    exporter,
                    "gateway provider http"
            );
            assertEquals(traceId, requestSpan.getTraceId());
            assertEquals(requestSpan.getSpanId(), attemptSpan.getParentSpanId());
            assertEquals(
                    "operation-1",
                    requestSpan.getAttributes().get(
                            AttributeKey.stringKey(
                                    "gateway.operation.id"
                            )
                    )
            );
            assertEquals(
                    "HTTP_STREAMING",
                    requestSpan.getAttributes().get(
                            AttributeKey.stringKey(
                                    "gateway.transport.mode"
                            )
                    )
            );
            assertEquals(
                    "FIRST_BODY_BUFFER_SENT",
                    requestSpan.getAttributes().get(
                            AttributeKey.stringKey(
                                    "gateway.commit.point"
                            )
                    )
            );
            assertEquals(
                    "HTTP",
                    attemptSpan.getAttributes().get(
                            AttributeKey.stringKey(
                                    "gateway.provider.protocol"
                            )
                    )
            );
            assertEquals(
                    StatusCode.ERROR,
                    span(exporter, "gateway.engine.kafka.send")
                            .getStatus()
                            .getStatusCode()
            );
            assertEquals(
                    "SUCCESS",
                    span(exporter, "gateway.engine.ddc.apply")
                            .getAttributes()
                            .get(AttributeKey.stringKey(
                                    "gateway.outcome"
                            ))
            );
        } finally {
            provider.close();
        }
    }

    private ObservationRegistry registry(
            OpenTelemetrySdk openTelemetry) {
        var apiTracer = openTelemetry.getTracer("gateway-test");
        OtelTracer tracer = new OtelTracer(
                apiTracer,
                new OtelCurrentTraceContext(),
                event -> {
                }
        );
        OtelPropagator propagator = new OtelPropagator(
                openTelemetry.getPropagators(),
                apiTracer
        );
        ObservationRegistry registry = ObservationRegistry.create();
        registry.observationConfig().observationHandler(
                new ObservationHandler
                        .FirstMatchingCompositeObservationHandler(
                        new PropagatingReceiverTracingObservationHandler<>(
                                tracer,
                                propagator
                        ),
                        new PropagatingSenderTracingObservationHandler<>(
                                tracer,
                                propagator
                        ),
                        new DefaultTracingObservationHandler(tracer)
                )
        );
        return registry;
    }

    private SpanData span(
            CollectingSpanExporter exporter,
            String name) {
        return exporter.spans.stream()
                .filter(candidate -> name.equals(candidate.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "missing span " + name + ": "
                                + exporter.spans.stream()
                                .map(SpanData::getName)
                                .toList()
                ));
    }

    private static final class CollectingSpanExporter
            implements SpanExporter {

        private final List<SpanData> spans =
                new CopyOnWriteArrayList<>();

        @Override
        public CompletableResultCode export(
                Collection<SpanData> exported) {
            spans.addAll(exported);
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode flush() {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode shutdown() {
            return CompletableResultCode.ofSuccess();
        }
    }
}
