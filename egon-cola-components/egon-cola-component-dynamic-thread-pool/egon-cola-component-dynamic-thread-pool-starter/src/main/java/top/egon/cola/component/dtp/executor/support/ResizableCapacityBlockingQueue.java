package top.egon.cola.component.dtp.executor.support;

import java.util.AbstractQueue;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @description 可调容量 FIFO 队列；缩容不丢任务，扩容唤醒等待生产者
 */
public final class ResizableCapacityBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();
    private final ArrayDeque<Node<E>> elements = new ArrayDeque<>();
    private int capacity;

    public ResizableCapacityBlockingQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("queueCapacity must be positive");
        }
        this.capacity = capacity;
    }

    public void setCapacity(int capacity) {
        resize(capacity, () -> { });
    }

    /**
     * 校验和参数更新共享队列锁，避免积压校验失败前已部分修改线程数。
     */
    public void resize(int capacity, Runnable updateConfiguration) {
        Objects.requireNonNull(updateConfiguration, "updateConfiguration");
        lock.lock();
        try {
            if (capacity <= 0 || capacity < elements.size()) {
                throw new IllegalArgumentException("queueCapacity must be positive and >= current queue size");
            }
            updateConfiguration.run();
            this.capacity = capacity;
            notFull.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public CapacitySnapshot snapshot() {
        lock.lock();
        try {
            return new CapacitySnapshot(capacity, elements.size());
        } finally {
            lock.unlock();
        }
    }

    public record CapacitySnapshot(int capacity, int size) { }

    @Override
    public boolean offer(E element) {
        Objects.requireNonNull(element, "element");
        lock.lock();
        try {
            if (elements.size() >= capacity) {
                return false;
            }
            enqueue(element);
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void put(E element) throws InterruptedException {
        Objects.requireNonNull(element, "element");
        lock.lockInterruptibly();
        try {
            while (elements.size() >= capacity) {
                notFull.await();
            }
            enqueue(element);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean offer(E element, long timeout, TimeUnit unit) throws InterruptedException {
        Objects.requireNonNull(element, "element");
        long nanos = unit.toNanos(timeout);
        lock.lockInterruptibly();
        try {
            while (elements.size() >= capacity) {
                if (nanos <= 0) {
                    return false;
                }
                nanos = notFull.awaitNanos(nanos);
            }
            enqueue(element);
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E take() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (elements.isEmpty()) {
                notEmpty.await();
            }
            return dequeue();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        lock.lockInterruptibly();
        try {
            while (elements.isEmpty()) {
                if (nanos <= 0) {
                    return null;
                }
                nanos = notEmpty.awaitNanos(nanos);
            }
            return dequeue();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E poll() {
        lock.lock();
        try {
            return elements.isEmpty() ? null : dequeue();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E peek() {
        lock.lock();
        try {
            Node<E> head = elements.peek();
            return head == null ? null : head.value;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int size() {
        return snapshot().size();
    }

    @Override
    public int remainingCapacity() {
        CapacitySnapshot snapshot = snapshot();
        return snapshot.capacity() - snapshot.size();
    }

    @Override
    public int drainTo(Collection<? super E> target) {
        return drainTo(target, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(Collection<? super E> target, int maxElements) {
        Objects.requireNonNull(target, "target");
        if (target == this) {
            throw new IllegalArgumentException("cannot drain to this queue");
        }
        lock.lock();
        try {
            int transferred = 0;
            while (transferred < maxElements && !elements.isEmpty()) {
                target.add(elements.getFirst().value);
                dequeue();
                transferred++;
            }
            return transferred;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clear() {
        lock.lock();
        try {
            elements.clear();
            notFull.signalAll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Iterator<E> iterator() {
        ArrayList<Node<E>> snapshot;
        lock.lock();
        try {
            snapshot = new ArrayList<>(elements);
        } finally {
            lock.unlock();
        }
        return new Iterator<>() {
            private int index;
            private Node<E> last;

            @Override
            public boolean hasNext() {
                return index < snapshot.size();
            }

            @Override
            public E next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                last = snapshot.get(index++);
                return last.value;
            }

            @Override
            public void remove() {
                if (last == null) {
                    throw new IllegalStateException();
                }
                lock.lock();
                try {
                    elements.remove(last);
                    last = null;
                    notFull.signalAll();
                } finally {
                    lock.unlock();
                }
            }
        };
    }

    private void enqueue(E value) {
        elements.addLast(new Node<>(value));
        notEmpty.signal();
    }

    private E dequeue() {
        E value = elements.removeFirst().value;
        notFull.signalAll();
        return value;
    }

    private static final class Node<E> {
        private final E value;

        private Node(E value) {
            this.value = value;
        }
    }
}
