package top.egon.cola.component.gateway.core.execution;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import top.egon.cola.component.gateway.core.exchange.GatewayExchange;
import top.egon.cola.component.gateway.core.exchange.GatewayResponse;
import top.egon.cola.component.gateway.core.filter.GatewayFilterChain;
import top.egon.cola.component.gateway.core.reactive.GatewayPublishers;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

public final class DefaultGatewayExecutor implements GatewayExecutor {

    private final GatewayFilterChain chain;

    private final BiFunction<GatewayExchange, Throwable, GatewayResponse>
            errorMapper;

    private final Map<GatewayExchange, Boolean> executed =
            Collections.synchronizedMap(new WeakHashMap<>());

    public DefaultGatewayExecutor(
            GatewayFilterChain chain,
            BiFunction<GatewayExchange, Throwable, GatewayResponse> errorMapper) {
        this.chain = Objects.requireNonNull(chain, "chain");
        this.errorMapper = Objects.requireNonNull(errorMapper, "errorMapper");
    }

    @Override
    public Publisher<GatewayResponse> execute(GatewayExchange exchange) {
        Objects.requireNonNull(exchange, "exchange");
        synchronized (executed) {
            if (executed.put(exchange, Boolean.TRUE) != null) {
                return GatewayPublishers.error(new IllegalStateException(
                        "gateway exchange already executed"
                ));
            }
        }
        return subscriber -> GatewayPublishers.defer(
                () -> chain.filter(exchange)
        ).subscribe(new TerminalSubscriber(exchange, subscriber));
    }

    private void release(GatewayExchange exchange) {
        if (exchange.request().body() instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception ignored) {
                // Resource cleanup must not replace the completed gateway result.
            }
        }
    }

    private final class TerminalSubscriber implements Subscriber<GatewayResponse> {

        private final GatewayExchange exchange;

        private final Subscriber<? super GatewayResponse> downstream;

        private final AtomicBoolean terminated = new AtomicBoolean();

        private TerminalSubscriber(
                GatewayExchange exchange,
                Subscriber<? super GatewayResponse> downstream) {
            this.exchange = exchange;
            this.downstream = downstream;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            downstream.onSubscribe(new Subscription() {
                @Override
                public void request(long count) {
                    subscription.request(count);
                }

                @Override
                public void cancel() {
                    subscription.cancel();
                    finish();
                }
            });
        }

        @Override
        public void onNext(GatewayResponse response) {
            downstream.onNext(response);
        }

        @Override
        public void onError(Throwable error) {
            if (terminated.compareAndSet(false, true)) {
                try {
                    downstream.onNext(errorMapper.apply(exchange, error));
                    downstream.onComplete();
                } finally {
                    release(exchange);
                }
            }
        }

        @Override
        public void onComplete() {
            if (terminated.compareAndSet(false, true)) {
                try {
                    downstream.onComplete();
                } finally {
                    release(exchange);
                }
            }
        }

        private void finish() {
            if (terminated.compareAndSet(false, true)) {
                release(exchange);
            }
        }
    }
}
