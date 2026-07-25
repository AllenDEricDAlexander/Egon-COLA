package top.egon.cola.component.gateway.engine.observability;

import top.egon.cola.component.gateway.contract.observability.GatewayCallEventV1;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A best-effort dispatcher with drop-new backpressure semantics.
 */
public final class GatewayCallEventDispatcher
        implements GatewayCallCompletionListener, AutoCloseable {

    private final ArrayBlockingQueue<QueuedEvent> queue;

    private final long maxQueuedBytes;

    private final GatewayCallEventSerializer serializer;

    private final GatewayCallEventSink sink;

    private final Duration shutdownDrain;

    private final AtomicLong queuedBytes = new AtomicLong();

    private final AtomicLong accepted = new AtomicLong();

    private final AtomicLong sent = new AtomicLong();

    private final AtomicLong droppedQueueFull = new AtomicLong();

    private final AtomicLong droppedPayloadTooLarge = new AtomicLong();

    private final AtomicLong failed = new AtomicLong();

    private final Thread worker;

    private volatile boolean accepting = true;

    public GatewayCallEventDispatcher(
            int maxQueuedEvents,
            long maxQueuedBytes,
            Duration shutdownDrain,
            GatewayCallEventSerializer serializer,
            GatewayCallEventSink sink) {
        if (maxQueuedEvents < 1 || maxQueuedBytes < 1) {
            throw new IllegalArgumentException(
                    "queue bounds must be positive"
            );
        }
        this.queue = new ArrayBlockingQueue<>(maxQueuedEvents);
        this.maxQueuedBytes = maxQueuedBytes;
        this.shutdownDrain = Objects.requireNonNull(
                shutdownDrain,
                "shutdownDrain"
        );
        this.serializer = Objects.requireNonNull(serializer, "serializer");
        this.sink = Objects.requireNonNull(sink, "sink");
        worker = Thread.ofPlatform()
                .daemon(true)
                .name("gateway-call-event-dispatcher")
                .unstarted(this::run);
        worker.start();
    }

    @Override
    public void onComplete(GatewayCallEventV1 event) {
        if (!accepting) {
            droppedQueueFull.incrementAndGet();
            return;
        }
        long estimatedBytes = estimateBytes(event);
        synchronized (queue) {
            if (!accepting
                    || queue.remainingCapacity() == 0
                    || queuedBytes.get() + estimatedBytes
                    > maxQueuedBytes
                    || !queue.offer(new QueuedEvent(
                    event,
                    estimatedBytes
            ))) {
                droppedQueueFull.incrementAndGet();
                return;
            }
            queuedBytes.addAndGet(estimatedBytes);
            accepted.incrementAndGet();
        }
    }

    public Health health() {
        return new Health(
                accepting,
                queue.size(),
                queuedBytes.get(),
                accepted.get(),
                sent.get(),
                droppedQueueFull.get(),
                droppedPayloadTooLarge.get(),
                failed.get()
        );
    }

    @Override
    public void close() {
        accepting = false;
        long deadline = System.nanoTime() + shutdownDrain.toNanos();
        while (!queue.isEmpty() && System.nanoTime() < deadline) {
            try {
                worker.join(Math.min(
                        100,
                        Math.max(
                                1,
                                TimeUnit.NANOSECONDS.toMillis(
                                        deadline - System.nanoTime()
                                )
                        )
                ));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        worker.interrupt();
        try {
            worker.join(500);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } finally {
            sink.close();
        }
    }

    private void run() {
        while (accepting || !queue.isEmpty()) {
            try {
                QueuedEvent queued = queue.poll(200, TimeUnit.MILLISECONDS);
                if (queued != null) {
                    queuedBytes.addAndGet(-queued.estimatedBytes);
                    dispatch(queued.event);
                }
            } catch (InterruptedException interrupted) {
                if (accepting) {
                    Thread.currentThread().interrupt();
                }
                return;
            }
        }
    }

    private void dispatch(GatewayCallEventV1 event) {
        try {
            byte[] payload = serializer.serialize(event);
            sink.send(event, payload);
            sent.incrementAndGet();
        } catch (GatewayCallEventSerializer.PayloadTooLargeException tooLarge) {
            droppedPayloadTooLarge.incrementAndGet();
        } catch (RuntimeException failure) {
            failed.incrementAndGet();
        }
    }

    private static long estimateBytes(GatewayCallEventV1 event) {
        return Math.min(
                GatewayCallEventSerializer.MAX_PAYLOAD_BYTES,
                2048L + event.attempts().size() * 256L
        );
    }

    private record QueuedEvent(
            GatewayCallEventV1 event,
            long estimatedBytes
    ) {
    }

    public record Health(
            boolean accepting,
            int queuedEvents,
            long queuedBytes,
            long accepted,
            long sent,
            long droppedQueueFull,
            long droppedPayloadTooLarge,
            long failed
    ) {
    }
}
