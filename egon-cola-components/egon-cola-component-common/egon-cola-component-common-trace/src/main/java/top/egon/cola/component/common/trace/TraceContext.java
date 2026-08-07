package top.egon.cola.component.common.trace;

import org.slf4j.MDC;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Complete trace state and MDC snapshot.
 */
public final class TraceContext {

    public static final String TRACE_ID = "traceId";

    public static final String SPAN_ID = "spanId";

    public static final String PARENT_SPAN_ID = "parentSpanId";

    public static final String REQUEST_ID = "requestId";

    public static final String TRACE_FLAGS = "traceFlags";

    public static final String TRACESTATE = "tracestate";

    public static final String SOURCE_APP = "sourceApp";

    public static final String SOURCE_INSTANCE = "sourceInstance";

    public static final String TRACEPARENT_HEADER = "traceparent";

    public static final String TRACESTATE_HEADER = "tracestate";

    public static final String REQUEST_ID_HEADER = "x-egon-request-id";

    public static final String LEGACY_TRACE_ID_HEADER = "X-Trace-Id";

    private static final int TRACEPARENT_LENGTH = 55;

    private static final int MAX_HEADER_LENGTH = 512;

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final HexFormat HEX = HexFormat.of();

    private static final Set<String> OWNED_MDC_KEYS = Set.of(
            TRACE_ID,
            SPAN_ID,
            PARENT_SPAN_ID,
            REQUEST_ID,
            TRACE_FLAGS,
            TRACESTATE,
            SOURCE_APP,
            SOURCE_INSTANCE
    );

    private final String traceId;

    private final String spanId;

    private final String parentSpanId;

    private final String requestId;

    private final String traceFlags;

    private final String tracestate;

    private final String sourceApp;

    private final String sourceInstance;

    private final Map<String, String> mdcContext;

    private TraceContext(String traceId,
                         String spanId,
                         String parentSpanId,
                         String requestId,
                         String traceFlags,
                         String tracestate,
                         String sourceApp,
                         String sourceInstance,
                         Map<String, String> mdcContext) {
        this(
                traceId,
                spanId,
                parentSpanId,
                requestId,
                traceFlags,
                tracestate,
                sourceApp,
                sourceInstance,
                mdcContext,
                false
        );
    }

    private TraceContext(String traceId,
                         String spanId,
                         String parentSpanId,
                         String requestId,
                         String traceFlags,
                         String tracestate,
                         String sourceApp,
                         String sourceInstance,
                         Map<String, String> mdcContext,
                         boolean preserveMdc) {
        this.traceId = normalizeTraceId(traceId);
        this.spanId = normalizeSpanId(spanId);
        this.parentSpanId = normalizeSpanId(parentSpanId);
        this.requestId = cleanText(requestId, 128);
        this.traceFlags = this.traceId == null
                && this.spanId == null
                && traceFlags == null
                ? null
                : normalizeTraceFlags(traceFlags);
        this.tracestate = cleanText(tracestate, MAX_HEADER_LENGTH);
        this.sourceApp = cleanText(sourceApp, 128);
        this.sourceInstance = cleanText(sourceInstance, 128);
        Map<String, String> values = mdcContext == null
                ? new HashMap<>()
                : new HashMap<>(mdcContext);
        if (!preserveMdc) {
            putOrRemove(values, TRACE_ID, this.traceId);
            putOrRemove(values, SPAN_ID, this.spanId);
            putOrRemove(values, PARENT_SPAN_ID, this.parentSpanId);
            putOrRemove(values, REQUEST_ID, this.requestId);
            putOrRemove(values, TRACE_FLAGS, this.traceFlags);
            putOrRemove(values, TRACESTATE, this.tracestate);
            putOrRemove(values, SOURCE_APP, this.sourceApp);
            putOrRemove(values, SOURCE_INSTANCE, this.sourceInstance);
        }
        this.mdcContext = Map.copyOf(values);
    }

    public static TraceContext root() {
        return root(null);
    }

    public static TraceContext root(String requestId) {
        return new TraceContext(
                createTraceId(),
                createSpanId(),
                null,
                requestId,
                "00",
                null,
                null,
                null,
                copyMdc()
        );
    }

    public static TraceContext fromHeaders(Function<String, String> reader,
                                           boolean readLegacyTraceId) {
        Objects.requireNonNull(reader, "reader");
        String requestId = cleanText(
                read(reader, REQUEST_ID_HEADER),
                128
        );
        if (requestId == null) {
            requestId = createTraceId();
        }
        String traceparent = read(reader, TRACEPARENT_HEADER);
        if (isValidTraceparent(traceparent)) {
            String version = traceparent.substring(0, 2);
            String traceFlags = traceparent.substring(53, 55);
            return new TraceContext(
                    traceparent.substring(3, 35),
                    createSpanId(),
                    traceparent.substring(36, 52),
                    requestId,
                    "00".equals(version)
                            ? traceFlags
                            : sampled(traceFlags) ? "01" : "00",
                    cleanText(
                            read(reader, TRACESTATE_HEADER),
                            MAX_HEADER_LENGTH
                    ),
                    null,
                    null,
                    copyMdc()
            );
        }
        if (readLegacyTraceId) {
            String legacyTraceId = normalizeTraceId(read(
                    reader,
                    LEGACY_TRACE_ID_HEADER,
                    "x-trace-id"
            ));
            if (legacyTraceId != null) {
                return new TraceContext(
                        legacyTraceId,
                        createSpanId(),
                        null,
                        requestId,
                        "00",
                        null,
                        null,
                        null,
                        copyMdc()
                );
            }
        }
        return root(requestId);
    }

    public static TraceContext capture() {
        Map<String, String> context = copyMdc();
        return new TraceContext(
                context.get(TRACE_ID),
                context.get(SPAN_ID),
                context.get(PARENT_SPAN_ID),
                context.get(REQUEST_ID),
                context.get(TRACE_FLAGS),
                context.get(TRACESTATE),
                context.get(SOURCE_APP),
                context.get(SOURCE_INSTANCE),
                context,
                true
        );
    }

    public static Optional<TraceContext> current() {
        TraceContext context = capture();
        return context.hasTrace() ? Optional.of(context) : Optional.empty();
    }

    public static TraceContext currentOrCreate() {
        TraceContext context = capture();
        if (context.hasTrace()) {
            return context;
        }
        String traceId = normalizeTraceId(context.traceId);
        return new TraceContext(
                traceId == null ? createTraceId() : traceId,
                createSpanId(),
                null,
                context.requestId,
                "00",
                null,
                context.sourceApp,
                context.sourceInstance,
                context.mdcContext
        );
    }

    public TraceContext child() {
        TraceContext parent = hasTrace() ? this : currentOrCreate();
        return new TraceContext(
                parent.traceId,
                createSpanId(),
                parent.spanId,
                parent.requestId,
                parent.traceFlags,
                parent.tracestate,
                parent.sourceApp,
                parent.sourceInstance,
                parent.mdcContext
        );
    }

    public TraceContext withSource(String sourceApp, String sourceInstance) {
        return new TraceContext(
                traceId,
                spanId,
                parentSpanId,
                requestId,
                traceFlags,
                tracestate,
                sourceApp,
                sourceInstance,
                mdcContext
        );
    }

    public Scope open() {
        return open(this);
    }

    public static Scope open(TraceContext context) {
        Objects.requireNonNull(context, "context");
        Map<String, String> previous = MDC.getCopyOfContextMap();
        restore(context.mdcContext);
        return new Scope(previous);
    }

    public static void install(TraceContext context) {
        Objects.requireNonNull(context, "context");
        restore(context.mdcContext);
    }

    public static void clear() {
        MDC.clear();
    }

    public String traceparent() {
        if (!hasTrace()) {
            throw new IllegalStateException("trace context is incomplete");
        }
        return "00-" + traceId + "-" + spanId + "-" + traceFlags;
    }

    public void inject(BiConsumer<String, String> writer) {
        Objects.requireNonNull(writer, "writer");
        writer.accept(TRACEPARENT_HEADER, traceparent());
        if (tracestate != null) {
            writer.accept(TRACESTATE_HEADER, tracestate);
        }
        if (requestId != null) {
            writer.accept(REQUEST_ID_HEADER, requestId);
        }
    }

    public boolean hasTrace() {
        return traceId != null && spanId != null;
    }

    public boolean sampled() {
        return sampled(traceFlags);
    }

    public String traceId() {
        return traceId;
    }

    public String spanId() {
        return spanId;
    }

    public String parentSpanId() {
        return parentSpanId;
    }

    public String requestId() {
        return requestId;
    }

    public String traceFlags() {
        return traceFlags;
    }

    public String tracestate() {
        return tracestate;
    }

    public String sourceApp() {
        return sourceApp;
    }

    public String sourceInstance() {
        return sourceInstance;
    }

    public Map<String, String> mdcContext() {
        return mdcContext;
    }

    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    public static void setTraceId(String traceId) {
        String value = cleanText(traceId, 128);
        if (value == null) {
            clearTraceId();
            return;
        }
        clearOwnedKeys();
        MDC.put(TRACE_ID, value);
        String normalized = normalizeTraceId(value);
        if (normalized != null) {
            MDC.put(SPAN_ID, createSpanId());
            MDC.put(TRACE_FLAGS, "00");
        }
    }

    public static void clearTraceId() {
        clearOwnedKeys();
    }

    public static void clearOwnedKeys() {
        OWNED_MDC_KEYS.forEach(MDC::remove);
    }

    public static String createTraceId() {
        return randomHex(16);
    }

    public static boolean isValidTraceparent(String value) {
        if (value == null
                || value.length() < TRACEPARENT_LENGTH
                || value.length() > MAX_HEADER_LENGTH
                || !value.equals(value.trim())
                || value.charAt(2) != '-'
                || value.charAt(35) != '-'
                || value.charAt(52) != '-') {
            return false;
        }
        String version = value.substring(0, 2);
        if (!isLowercaseHex(version)
                || "ff".equals(version)
                || !isLowercaseHex(value.substring(3, 35))
                || !isLowercaseHex(value.substring(36, 52))
                || !isLowercaseHex(value.substring(53, 55))
                || normalizeTraceId(value.substring(3, 35)) == null
                || normalizeSpanId(value.substring(36, 52)) == null
                || normalizeTraceFlags(value.substring(53, 55)) == null) {
            return false;
        }
        return "00".equals(version)
                ? value.length() == TRACEPARENT_LENGTH
                : value.length() == TRACEPARENT_LENGTH
                || value.charAt(TRACEPARENT_LENGTH) == '-';
    }

    public static String normalizeTraceId(String value) {
        return normalizeHex(value, 32, true);
    }

    private static String normalizeSpanId(String value) {
        return normalizeHex(value, 16, true);
    }

    private static String normalizeTraceFlags(String value) {
        String normalized = normalizeHex(value, 2, false);
        return normalized == null ? "00" : normalized;
    }

    private static String normalizeHex(String value,
                                       int length,
                                       boolean rejectAllZeros) {
        if (value == null || hasLineBreak(value)) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() != length
                || rejectAllZeros && allZeros(normalized)
                || !isLowercaseHex(normalized)) {
            return null;
        }
        return normalized;
    }

    private static String cleanText(String value, int maxLength) {
        if (value == null || value.isBlank() || hasLineBreak(value)) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : null;
    }

    private static String read(Function<String, String> reader,
                               String... names) {
        for (String name : names) {
            String value = reader.apply(name);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static boolean sampled(String traceFlags) {
        return traceFlags != null
                && (Integer.parseInt(traceFlags, 16) & 1) == 1;
    }

    private static boolean hasLineBreak(String value) {
        return value != null
                && (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0);
    }

    private static boolean isLowercaseHex(String value) {
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (!((character >= '0' && character <= '9')
                    || (character >= 'a' && character <= 'f'))) {
                return false;
            }
        }
        return true;
    }

    private static boolean allZeros(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) != '0') {
                return false;
            }
        }
        return true;
    }

    private static String createSpanId() {
        return randomHex(8);
    }

    private static String randomHex(int bytes) {
        byte[] value = new byte[bytes];
        String hex;
        do {
            RANDOM.nextBytes(value);
            hex = HEX.formatHex(value);
        } while (allZeros(hex));
        return hex;
    }

    private static Map<String, String> copyMdc() {
        Map<String, String> context = MDC.getCopyOfContextMap();
        return context == null
                ? new HashMap<>()
                : new HashMap<>(context);
    }

    private static void putOrRemove(Map<String, String> target,
                                    String key,
                                    String value) {
        if (value == null) {
            target.remove(key);
        } else {
            target.put(key, value);
        }
    }

    private static void restore(Map<String, String> context) {
        if (context == null || context.isEmpty()) {
            MDC.clear();
        } else {
            MDC.setContextMap(context);
        }
    }

    /**
     * Restores the previous MDC context when closed.
     */
    public static final class Scope implements AutoCloseable {

        private final Map<String, String> previous;

        private boolean closed;

        private Scope(Map<String, String> previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            restore(previous);
        }
    }
}
