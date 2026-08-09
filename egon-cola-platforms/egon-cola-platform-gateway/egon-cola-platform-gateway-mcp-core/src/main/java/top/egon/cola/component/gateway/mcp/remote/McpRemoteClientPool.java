package top.egon.cola.component.gateway.mcp.remote;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeRemoteProvider;
import top.egon.cola.component.gateway.core.mcp.remote.RemoteAuthProvider;
import top.egon.cola.component.gateway.core.mcp.remote.RemoteMcpClient;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fingerprint-scoped remote clients with timeout, bulkhead and circuit limits.
 */
public final class McpRemoteClientPool implements AutoCloseable {

    private final RemoteMcpClient.Factory factory;

    private final RemoteAuthProvider authentication;

    private final Clock clock;

    private final Duration callTimeout;

    private final int maxConcurrentCalls;

    private final int failureThreshold;

    private final Duration circuitOpenDuration;

    private final AtomicLong requestIds = new AtomicLong();

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    public McpRemoteClientPool(
            RemoteMcpClient.Factory factory,
            RemoteAuthProvider authentication) {
        this(
                factory,
                authentication,
                Clock.systemUTC(),
                Duration.ofSeconds(60),
                32,
                3,
                Duration.ofSeconds(30)
        );
    }

    public McpRemoteClientPool(
            RemoteMcpClient.Factory factory,
            RemoteAuthProvider authentication,
            Clock clock,
            Duration callTimeout,
            int maxConcurrentCalls,
            int failureThreshold,
            Duration circuitOpenDuration) {
        this.factory = Objects.requireNonNull(factory, "factory");
        this.authentication = Objects.requireNonNull(
                authentication,
                "authentication"
        );
        this.clock = Objects.requireNonNull(clock, "clock");
        this.callTimeout = positive(callTimeout, "callTimeout");
        if (maxConcurrentCalls < 1 || maxConcurrentCalls > 10_000) {
            throw new IllegalArgumentException(
                    "remote MCP maxConcurrentCalls is invalid"
            );
        }
        this.maxConcurrentCalls = maxConcurrentCalls;
        if (failureThreshold < 1 || failureThreshold > 1_000) {
            throw new IllegalArgumentException(
                    "remote MCP failureThreshold is invalid"
            );
        }
        this.failureThreshold = failureThreshold;
        this.circuitOpenDuration = positive(
                circuitOpenDuration,
                "circuitOpenDuration"
        );
    }

    public Publisher<RemoteMcpClient.ExchangeResponse> exchange(
            McpRuntimeRemoteProvider provider,
            McpDialectTranslator.OutboundCall call,
            RemoteAuthProvider.AuthContext context) {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(call, "call");
        Objects.requireNonNull(context, "context");
        if (!provider.enabled()) {
            return Mono.error(unavailable(
                    "remote MCP Provider is disabled",
                    null
            ));
        }
        return Mono.defer(() -> {
            Entry entry = entry(provider);
            if (!entry.circuit.allow(clock.instant())) {
                return Mono.error(unavailable(
                        "remote MCP circuit is open",
                        null
                ));
            }
            if (!entry.bulkhead.tryAcquire()) {
                return Mono.error(unavailable(
                        "remote MCP bulkhead is full",
                        null
                ));
            }
            return Mono.from(authentication.resolve(
                            new RemoteAuthProvider.AuthRequest(
                                    provider,
                                    context
                            )
                    ))
                    .flatMap(auth -> Mono.from(entry.client.exchange(
                            request(provider, call, auth)
                    )))
                    .timeout(callTimeout)
                    .doOnSuccess(ignored -> entry.circuit.success())
                    .doOnError(ignored -> entry.circuit.failure(
                            clock.instant(),
                            failureThreshold,
                            circuitOpenDuration
                    ))
                    .onErrorMap(failure -> failure instanceof McpProtocolException
                            ? failure
                            : unavailable(
                                    "remote MCP request failed",
                                    failure
                            ))
                    .doFinally(ignored -> entry.bulkhead.release());
        });
    }

    public Health health(McpRuntimeRemoteProvider provider) {
        Entry entry = entries.get(key(provider));
        if (entry == null) {
            return new Health("NOT_CONNECTED", 0, maxConcurrentCalls);
        }
        return new Health(
                entry.circuit.open(clock.instant()) ? "OPEN" : "AVAILABLE",
                entry.circuit.failures.get(),
                entry.bulkhead.availablePermits()
        );
    }

    @Override
    public void close() {
        entries.values().forEach(entry -> {
            try {
                entry.client.close();
            } catch (RuntimeException ignored) {
                // Best-effort shutdown; clients own no business state.
            }
        });
        entries.clear();
    }

    private RemoteMcpClient.ExchangeRequest request(
            McpRuntimeRemoteProvider provider,
            McpDialectTranslator.OutboundCall call,
            RemoteAuthProvider.OutboundAuthentication auth) {
        LinkedHashMap<String, String> headers = new LinkedHashMap<>(
                call.headers()
        );
        auth.headers().forEach((name, value) -> {
            if (headers.putIfAbsent(name, value) != null) {
                throw new IllegalArgumentException(
                        "remote authentication cannot replace protocol headers"
                );
            }
        });
        String tlsReference = auth.tlsProfileReference() == null
                ? provider.tlsProfileReference()
                : auth.tlsProfileReference();
        return new RemoteMcpClient.ExchangeRequest(
                provider,
                requestIds.incrementAndGet(),
                call.method(),
                call.params(),
                call.meta(),
                headers,
                tlsReference,
                callTimeout
        );
    }

    private Entry entry(McpRuntimeRemoteProvider provider) {
        String key = key(provider);
        return entries.computeIfAbsent(key, ignored -> new Entry(
                factory.create(provider),
                new Semaphore(maxConcurrentCalls),
                new Circuit()
        ));
    }

    private String key(McpRuntimeRemoteProvider provider) {
        return provider.providerId() + "\u0000" + provider.capabilityFingerprint();
    }

    private Duration positive(Duration value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    private McpProtocolException unavailable(
            String message,
            Throwable failure) {
        McpProtocolException result = new McpProtocolException(
                McpErrorCode.MCP_REMOTE_UNAVAILABLE,
                message
        );
        if (failure != null) {
            result.initCause(failure);
        }
        return result;
    }

    public record Health(
            String state,
            int consecutiveFailures,
            int availablePermits
    ) {
    }

    private record Entry(
            RemoteMcpClient client,
            Semaphore bulkhead,
            Circuit circuit
    ) {
    }

    private static final class Circuit {

        private final AtomicInteger failures = new AtomicInteger();

        private volatile Instant openUntil;

        private boolean allow(Instant now) {
            Instant until = openUntil;
            if (until == null) {
                return true;
            }
            if (now.isBefore(until)) {
                return false;
            }
            openUntil = null;
            failures.set(0);
            return true;
        }

        private boolean open(Instant now) {
            Instant until = openUntil;
            return until != null && now.isBefore(until);
        }

        private void success() {
            failures.set(0);
            openUntil = null;
        }

        private void failure(
                Instant now,
                int threshold,
                Duration openDuration) {
            if (failures.incrementAndGet() >= threshold) {
                openUntil = now.plus(openDuration);
            }
        }
    }
}
