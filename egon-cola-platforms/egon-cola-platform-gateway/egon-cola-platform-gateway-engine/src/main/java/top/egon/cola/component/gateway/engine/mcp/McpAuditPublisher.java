package top.egon.cola.component.gateway.engine.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.egon.cola.component.gateway.mcp.security.McpSecurityDigests;
import top.egon.cola.component.gateway.mcp.telemetry.McpTelemetry;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Emits sanitized runtime audit JSON without bodies, arguments or credentials.
 */
public final class McpAuditPublisher implements McpTelemetry {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            McpAuditPublisher.class
    );

    private final ObjectMapper objectMapper;

    private final Clock clock;

    private final AuditSink sink;

    public McpAuditPublisher(
            ObjectMapper objectMapper,
            Clock clock,
            AuditSink sink) {
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        ).copy();
        this.clock = Objects.requireNonNull(clock, "clock");
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    @Override
    public Scope start(Request request) {
        Objects.requireNonNull(request, "request");
        Instant startedAt = clock.instant();
        return new Scope() {
            private final AtomicBoolean completed = new AtomicBoolean();

            private final AtomicReference<String> remoteProvider =
                    new AtomicReference<>(request.remoteProviderCode());

            @Override
            public void remoteProvider(String providerCode) {
                String normalized = code(providerCode);
                if (normalized != null) {
                    remoteProvider.set(normalized);
                }
            }

            @Override
            public Child startChild(ChildKind kind) {
                return Child.noop();
            }

            @Override
            public void success() {
                publish("SUCCESS");
            }

            @Override
            public void failure(String errorCode) {
                publish(safeStatus(errorCode));
            }

            private void publish(String status) {
                if (!completed.compareAndSet(false, true)) {
                    return;
                }
                try {
                    sink.publish(objectMapper.writeValueAsString(event(
                            request,
                            startedAt,
                            status,
                            remoteProvider.get()
                    )));
                } catch (Exception failure) {
                    LOGGER.warn("MCP runtime audit publication failed");
                }
            }
        };
    }

    private Map<String, Object> event(
            Request request,
            Instant startedAt,
            String status,
            String remoteProviderCode) {
        LinkedHashMap<String, Object> event = new LinkedHashMap<>();
        event.put("eventType", "mcp.runtime.request");
        event.put("occurredAt", startedAt.toString());
        event.put("method", request.method());
        event.put("primitive", request.primitive());
        event.put("serverCode", request.serverCode());
        String remoteProvider = code(remoteProviderCode);
        if (remoteProvider != null) {
            event.put("remoteProviderCode", remoteProvider);
        }
        event.put("status", status);
        String actor = attribute(
                request.attributes(),
                "callerId",
                "identity.subject"
        );
        if (actor != null) {
            event.put(
                    "actorFingerprint",
                    McpSecurityDigests.token(actor)
            );
        }
        putIfPresent(
                event,
                "tenantId",
                codeAttribute(
                        request.attributes(),
                        "tenantId",
                        "identity.tenant-id"
                )
        );
        putIfPresent(
                event,
                "clientId",
                codeAttribute(
                        request.attributes(),
                        "idp.client-id",
                        "identity.client-id"
                )
        );
        putIfPresent(
                event,
                "traceId",
                trace(request.attributes())
        );
        return Map.copyOf(event);
    }

    private String codeAttribute(
            Map<String, Object> attributes,
            String... names) {
        return code(attribute(attributes, names));
    }

    private String code(String value) {
        if (value == null || !value.matches(
                "[A-Za-z0-9][A-Za-z0-9_.:-]{0,127}"
        )) {
            return null;
        }
        return value;
    }

    private String trace(Map<String, Object> attributes) {
        String requestId = codeAttribute(attributes, "x-egon-request-id");
        if (requestId != null) {
            return requestId;
        }
        String traceparent = attribute(attributes, "traceparent");
        return traceparent != null && traceparent.matches(
                "[0-9a-fA-F]{2}-[0-9a-fA-F]{32}-"
                        + "[0-9a-fA-F]{16}-[0-9a-fA-F]{2}"
        ) ? traceparent : null;
    }

    private void putIfPresent(
            Map<String, Object> target,
            String name,
            String value) {
        if (value != null) {
            target.put(name, value);
        }
    }

    private String attribute(
            Map<String, Object> attributes,
            String... names) {
        for (String name : names) {
            Object value = attributes.get(name);
            if (value instanceof String text && !text.isBlank()) {
                String normalized = text.trim();
                return normalized.length() <= 256
                        ? normalized
                        : normalized.substring(0, 256);
            }
        }
        return null;
    }

    private String safeStatus(String value) {
        if (value == null || !value.matches("[A-Z][A-Z0-9_]{0,63}")) {
            return "ERROR";
        }
        return value;
    }

    @FunctionalInterface
    public interface AuditSink {

        void publish(String json);
    }
}
