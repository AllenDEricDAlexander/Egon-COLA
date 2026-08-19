package top.egon.cola.component.gateway.engine.http.common.buffer;

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
 * 补充说明 / Supplementary summary: {@code GatewayDataBufferPipeline} 是类型，位于当前 Gateway 模块的相关包中，负责网关Data缓冲区Pipeline相关的职责与边界。
 * English supplement: {@code GatewayDataBufferPipeline} is a type in the current Gateway module; it owns the gateway data buffer pipeline-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayDataBufferPipeline {

    /**
     * 中文说明：创建 {@code GatewayDataBufferPipeline} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayDataBufferPipeline} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     */
    private GatewayDataBufferPipeline() {
    }

    /**
     * 中文说明：执行 limitBytes 操作；该方法是 {@code GatewayDataBufferPipeline} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the limit bytes operation; this method is the invocation entry point on {@code GatewayDataBufferPipeline} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDataBufferPipeline.limitBytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param maxBytes 参数 maxBytes；parameter max bytes。
     * @param failureSupplier 参数 failureSupplier；parameter failure supplier。
     * @return 返回 limitBytes 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 observeBytes 操作；该方法是 {@code GatewayDataBufferPipeline} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the observe bytes operation; this method is the invocation entry point on {@code GatewayDataBufferPipeline} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDataBufferPipeline.observeBytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param observer 参数 observer；parameter observer。
     * @return 返回 observeBytes 的处理结果；returns the result of the operation.
     */
    public static Flux<DataBuffer> observeBytes(
            Flux<DataBuffer> source,
            LongConsumer observer) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(observer, "observer");
        return source.doOnNext(buffer -> safelyObserve(
                () -> observer.accept(buffer.readableByteCount())
        ));
    }

    /**
     * 中文说明：执行 sampleBodyWhenEnabled 操作；该方法是 {@code GatewayDataBufferPipeline} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the sample body when enabled operation; this method is the invocation entry point on {@code GatewayDataBufferPipeline} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDataBufferPipeline.sampleBodyWhenEnabled(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param enabled 参数 enabled；parameter enabled。
     * @param maxSampleBytes 参数 maxSampleBytes；parameter max sample bytes。
     * @param observer 参数 observer；parameter observer。
     * @return 返回 sampleBodyWhenEnabled 的处理结果；returns the result of the operation.
     */
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
     * 补充说明 / Supplementary summary: 执行 enforceIdle超时 操作；该方法是 {@code GatewayDataBufferPipeline} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English supplement: Executes the enforce idle timeout operation; this method is the invocation entry point on {@code GatewayDataBufferPipeline} and performs the corresponding runtime, management, or protocol work.
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDataBufferPipeline.enforceIdleTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 补充说明 / Supplementary summary: 执行 发布OnDiscardOrCancel 操作；该方法是 {@code GatewayDataBufferPipeline} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English supplement: Executes the release on discard or cancel operation; this method is the invocation entry point on {@code GatewayDataBufferPipeline} and performs the corresponding runtime, management, or protocol work.
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDataBufferPipeline.releaseOnDiscardOrCancel(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    public static Flux<DataBuffer> releaseOnDiscardOrCancel(
            Flux<DataBuffer> source) {
        Objects.requireNonNull(source, "source");
        return source.doOnDiscard(
                DataBuffer.class,
                GatewayDataBufferOwnership::release
        );
    }

    /**
     * 中文说明：执行 safelyObserve 操作；该方法是 {@code GatewayDataBufferPipeline} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the safely observe operation; this method is the invocation entry point on {@code GatewayDataBufferPipeline} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDataBufferPipeline.safelyObserve(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param observer 参数 observer；parameter observer。
     */
    private static void safelyObserve(Runnable observer) {
        try {
            observer.run();
        } catch (RuntimeException ignored) {
            // Observation cannot control or interrupt the body transfer.
        }
    }

    /**
     * 中文说明：执行 cancelSource 操作；该方法是 {@code GatewayDataBufferPipeline} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the cancel source operation; this method is the invocation entry point on {@code GatewayDataBufferPipeline} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDataBufferPipeline.cancelSource(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param sourceSubscription 参数 source订阅；parameter source subscription。
     */
    private static void cancelSource(
            AtomicReference<Subscription> sourceSubscription) {
        Subscription subscription = sourceSubscription.getAndSet(null);
        if (subscription != null) {
            subscription.cancel();
        }
    }
}
