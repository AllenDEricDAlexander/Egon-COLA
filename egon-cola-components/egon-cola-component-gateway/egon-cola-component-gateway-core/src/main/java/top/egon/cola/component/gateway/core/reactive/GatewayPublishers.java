package top.egon.cola.component.gateway.core.reactive;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public final class GatewayPublishers {

    private GatewayPublishers() {
    }

    public static <T> Publisher<T> just(T value) {
        Objects.requireNonNull(value, "value");
        return subscriber -> subscriber.onSubscribe(
                new ScalarSubscription<>(subscriber, value)
        );
    }

    public static <T> Publisher<T> error(Throwable error) {
        Objects.requireNonNull(error, "error");
        return subscriber -> {
            subscriber.onSubscribe(EmptySubscription.INSTANCE);
            subscriber.onError(error);
        };
    }

    public static <T> Publisher<T> defer(
            Supplier<? extends Publisher<T>> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return subscriber -> {
            try {
                Publisher<T> publisher = Objects.requireNonNull(
                        supplier.get(),
                        "deferred publisher"
                );
                publisher.subscribe(subscriber);
            } catch (Throwable error) {
                GatewayPublishers.<T>error(error).subscribe(subscriber);
            }
        };
    }

    private static final class ScalarSubscription<T> implements Subscription {

        private final Subscriber<? super T> subscriber;

        private final T value;

        private final AtomicBoolean completed = new AtomicBoolean();

        private ScalarSubscription(
                Subscriber<? super T> subscriber,
                T value) {
            this.subscriber = subscriber;
            this.value = value;
        }

        @Override
        public void request(long count) {
            if (count <= 0 && completed.compareAndSet(false, true)) {
                subscriber.onError(new IllegalArgumentException(
                        "request count must be positive"
                ));
                return;
            }
            if (completed.compareAndSet(false, true)) {
                subscriber.onNext(value);
                subscriber.onComplete();
            }
        }

        @Override
        public void cancel() {
            completed.set(true);
        }
    }

    private enum EmptySubscription implements Subscription {

        INSTANCE;

        @Override
        public void request(long count) {
        }

        @Override
        public void cancel() {
        }
    }
}
