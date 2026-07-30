package top.egon.cola.component.gateway.engine.transport;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Reactive timeout operators with stable failure categories.
 */
public final class GatewayTransportTimeouts {

    private GatewayTransportTimeouts() {
    }

    public static <T> Mono<T> connect(
            Mono<T> source,
            Duration timeout) {
        return monoTimeout(
                source,
                timeout,
                GatewayConnectTimeoutException::new
        );
    }

    public static <T> Mono<T> responseHeaders(
            Mono<T> source,
            Duration timeout) {
        return monoTimeout(
                source,
                timeout,
                GatewayResponseHeaderTimeoutException::new
        );
    }

    public static <T> Flux<T> requestIdle(
            Flux<T> source,
            Duration timeout) {
        return fluxIdleTimeout(
                source,
                timeout,
                () -> new GatewayStreamIdleTimeoutException(
                        GatewayStreamDirection.REQUEST
                )
        );
    }

    public static <T> Flux<T> responseIdle(
            Flux<T> source,
            Duration timeout) {
        return fluxIdleTimeout(
                source,
                timeout,
                () -> new GatewayStreamIdleTimeoutException(
                        GatewayStreamDirection.RESPONSE
                )
        );
    }

    public static <T> Mono<T> total(
            Mono<T> source,
            Optional<Duration> timeout) {
        Objects.requireNonNull(timeout, "timeout");
        return timeout.map(value -> monoTimeout(
                        source,
                        value,
                        GatewayTotalTimeoutException::new
                ))
                .orElse(source);
    }

    public static <T> Flux<T> total(
            Flux<T> source,
            Optional<Duration> timeout) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(timeout, "timeout");
        return timeout.map(value -> source.takeUntilOther(
                        Mono.delay(requirePositive(value))
                                .flatMap(ignored -> Mono.error(
                                        new GatewayTotalTimeoutException()
                                ))
                ))
                .orElse(source);
    }

    public static <T> Flux<T> websocketIdle(
            Flux<T> source,
            Duration timeout) {
        return fluxIdleTimeout(
                source,
                timeout,
                GatewayWebSocketIdleTimeoutException::new
        );
    }

    private static <T> Mono<T> monoTimeout(
            Mono<T> source,
            Duration timeout,
            Supplier<? extends RuntimeException> failure) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(failure, "failure");
        return source.timeout(
                requirePositive(timeout),
                Mono.defer(() -> Mono.error(failure.get()))
        );
    }

    private static <T> Flux<T> fluxIdleTimeout(
            Flux<T> source,
            Duration timeout,
            Supplier<? extends RuntimeException> failure) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(failure, "failure");
        Publisher<T> fallback = Flux.defer(() ->
                Flux.error(failure.get())
        );
        return source.timeout(requirePositive(timeout), fallback);
    }

    private static Duration requirePositive(Duration timeout) {
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException(
                    "timeout must be positive"
            );
        }
        return timeout;
    }
}
