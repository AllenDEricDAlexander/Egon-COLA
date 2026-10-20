package top.egon.cola.component.dtp.executor.support;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

class ResizableCapacityBlockingQueueTest {
    @Test
    void expansionWakesBlockedProducerAndShrinkDoesNotDropTasks() throws Exception {
        var queue = new ResizableCapacityBlockingQueue<Integer>(1);
        queue.put(1);
        Thread producer = Thread.ofPlatform().start(() -> {
            try { queue.put(2); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        try {
            await().atMost(Duration.ofSeconds(2)).until(() -> producer.getState() == Thread.State.WAITING);
            queue.setCapacity(2);
            producer.join(2000);
            assertThat(producer.isAlive()).isFalse();
            AtomicBoolean mutated = new AtomicBoolean();
            assertThatThrownBy(() -> queue.resize(1, () -> mutated.set(true))).isInstanceOf(IllegalArgumentException.class);
            assertThat(mutated).isFalse();
            assertThat(queue.remainingCapacity()).isZero();
            assertThat(queue.take()).isEqualTo(1);
            assertThat(queue.take()).isEqualTo(2);
        } finally { producer.interrupt(); producer.join(2000); }
    }

    @Test
    void iteratorRemovesTheExactOccurrenceAndDrainFailureKeepsTheUntransferredElement() {
        var queue = new ResizableCapacityBlockingQueue<Object>(4);
        Object repeated = new Object();
        queue.add(repeated); queue.add(repeated);
        var iterator = queue.iterator(); iterator.next(); iterator.next(); iterator.remove();
        assertThat(queue).hasSize(1);
        var target = new ArrayList<Object>() {
            @Override public boolean add(Object value) { throw new IllegalStateException("expected"); }
        };
        assertThatThrownBy(() -> queue.drainTo(target)).isInstanceOf(IllegalStateException.class);
        assertThat(queue).containsExactly(repeated);
        queue.clear();
        assertThat(queue.remainingCapacity()).isEqualTo(4);
    }

    @Test
    void timedOperationsAndInterruptsRespectBlockingQueueContract() throws Exception {
        var queue = new ResizableCapacityBlockingQueue<Integer>(1);
        assertThat(queue.poll(1, TimeUnit.MILLISECONDS)).isNull();
        queue.put(1);
        assertThat(queue.offer(2, 1, TimeUnit.MILLISECONDS)).isFalse();
        try (var workers = Executors.newSingleThreadExecutor()) {
            var result = workers.submit(() -> {
                Thread.currentThread().interrupt();
                try { queue.put(2); return false; } catch (InterruptedException expected) { return true; }
            });
            assertThat(result.get(2, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(queue).containsExactly(1);
    }
}
