package top.egon.cola.component.gateway.engine.http.buffer;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.function.Supplier;

/**
 * Composable single-subscription operators for streaming DataBuffer bodies.
 */
public final class GatewayDataBufferPipeline {

    private GatewayDataBufferPipeline() {
    }

    public static Flux<DataBuffer> limitBytes(
            Flux<DataBuffer> source,
            long maxBytes,
            Supplier<? extends Throwable> failureSupplier) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(failureSupplier, "failureSupplier");
        if (maxBytes < 0) {
            throw new IllegalArgumentException(
                    "body byte limit must not be negative"
            );
        }
        return Flux.defer(() -> {
            AtomicLong receivedBytes = new AtomicLong();
            return source.handle((buffer, sink) -> {
                long received = receivedBytes.get();
                int readable = buffer.readableByteCount();
                if ((long) readable > maxBytes - received) {
                    GatewayDataBufferOwnership.release(buffer);
                    sink.error(Objects.requireNonNull(
                            failureSupplier.get(),
                            "failureSupplier result"
                    ));
                    return;
                }
                receivedBytes.addAndGet(readable);
                sink.next(buffer);
            });
        });
    }

    public static Flux<DataBuffer> observeBytes(
            Flux<DataBuffer> source,
            LongConsumer observer) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(observer, "observer");
        return source.doOnNext(buffer -> safelyObserve(
                () -> observer.accept(buffer.readableByteCount())
        ));
    }

    public static Flux<DataBuffer> sampleBodyWhenEnabled(
            Flux<DataBuffer> source,
            boolean enabled,
            int maxSampleBytes,
            Consumer<byte[]> observer) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(observer, "observer");
        if (maxSampleBytes < 0) {
            throw new IllegalArgumentException(
                    "sample byte limit must not be negative"
            );
        }
        if (!enabled || maxSampleBytes == 0) {
            return source;
        }
        return Flux.defer(() -> {
            AtomicInteger sampledBytes = new AtomicInteger();
            return source.doOnNext(buffer -> {
                int remaining = maxSampleBytes - sampledBytes.get();
                if (remaining <= 0) {
                    return;
                }
                byte[] sample = GatewayDataBufferOwnership.readOnlySample(
                        buffer,
                        remaining
                );
                sampledBytes.addAndGet(sample.length);
                if (sample.length > 0) {
                    safelyObserve(() -> observer.accept(sample));
                }
            });
        });
    }

    /**
     * Races the body with an Adapter-provided network inactivity signal. An
     * empty signal is ignored; an emitted signal fails and cancels the body.
     */
    public static Flux<DataBuffer> enforceIdleTimeout(
            Flux<DataBuffer> source,
            Publisher<?> timeoutSignal,
            Supplier<? extends Throwable> failureSupplier) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(timeoutSignal, "timeoutSignal");
        Objects.requireNonNull(failureSupplier, "failureSupplier");
        return Flux.defer(() -> {
            AtomicReference<Subscription> sourceSubscription =
                    new AtomicReference<>();
            AtomicBoolean timeoutFailed = new AtomicBoolean();
            Flux<Void> timeoutFailure = Mono.from(timeoutSignal)
                    .flatMap(ignored -> Mono.<Void>error(
                            Objects.requireNonNull(
                                failureSupplier.get(),
                                "failureSupplier result"
                            )
                    ))
                    .doOnError(ignored -> {
                        timeoutFailed.set(true);
                        cancelSource(sourceSubscription);
                    })
                    .flux()
                    .concatWith(Flux.never());
            return source
                    .doOnSubscribe(subscription -> {
                        sourceSubscription.set(subscription);
                        if (timeoutFailed.get()) {
                            cancelSource(sourceSubscription);
                        }
                    })
                    .takeUntilOther(timeoutFailure);
        });
    }

    /**
     * Installs the single release hook for upstream operators that discard
     * emitted but untransferred buffers during filtering, failure, or
     * cancellation. The producer remains responsible for pooled elements that
     * it owns but never emits; pooled bodies therefore must be lazy or expose
     * their own cancellation cleanup rather than pre-constructing elements in
     * {@code Flux.just}.
     */
    public static Flux<DataBuffer> releaseOnDiscardOrCancel(
            Flux<DataBuffer> source) {
        Objects.requireNonNull(source, "source");
        return source.doOnDiscard(
                DataBuffer.class,
                GatewayDataBufferOwnership::release
        );
    }

    private static void safelyObserve(Runnable observer) {
        try {
            observer.run();
        } catch (RuntimeException ignored) {
            // Observation cannot control or interrupt the body transfer.
        }
    }

    private static void cancelSource(
            AtomicReference<Subscription> sourceSubscription) {
        Subscription subscription = sourceSubscription.getAndSet(null);
        if (subscription != null) {
            subscription.cancel();
        }
    }
}
