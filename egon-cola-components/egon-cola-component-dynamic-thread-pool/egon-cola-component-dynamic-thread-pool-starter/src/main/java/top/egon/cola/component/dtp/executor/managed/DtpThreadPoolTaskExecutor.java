package top.egon.cola.component.dtp.executor.managed;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import top.egon.cola.component.dtp.context.DtpTaskObserver;
import top.egon.cola.component.dtp.executor.support.ResizableCapacityBlockingQueue;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadFactory;

/**
 * @description ThreadManage 创建的上下文感知、可调队列 Spring 线程池
 */
@Slf4j
public final class DtpThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {

    private final DtpTaskObserver observer;
    private volatile boolean initialized;
    private final java.util.concurrent.CopyOnWriteArrayList<Runnable> closeListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final java.util.concurrent.atomic.AtomicBoolean closed = new java.util.concurrent.atomic.AtomicBoolean();

    public void onShutdown(Runnable listener) { closeListeners.add(listener); }

    @Override
    public void shutdown() {
        try { super.shutdown(); }
        finally {
            if (closed.compareAndSet(false, true)) {
                for (Runnable listener : closeListeners) {
                    try { listener.run(); }
                    catch (RuntimeException e) {
                        log.warn("动态线程池关闭观察失败 executor={} cause={}", observer.getExecutorName(), e.getClass().getSimpleName());
                    }
                }
            }
        }
    }

    public DtpThreadPoolTaskExecutor(DtpTaskObserver observer) {
        this.observer = Objects.requireNonNull(observer, "observer");
        setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());
        setWaitForTasksToCompleteOnShutdown(true);
        setAwaitTerminationSeconds(30);
        setThreadFactory(this);
    }

    @Override
    public void setThreadFactory(ThreadFactory factory) {
        super.setThreadFactory(observer == null ? factory : observer.threadFactory(factory == null ? this : factory));
    }

    public DtpTaskObserver taskObserver() { return observer; }

    @Override
    public synchronized void initialize() {
        if (!initialized) {
            super.initialize();
            initialized = true;
        }
    }

    @Override
    protected BlockingQueue<Runnable> createQueue(int capacity) {
        return capacity == 0 ? new SynchronousQueue<>() : new ResizableCapacityBlockingQueue<>(capacity);
    }

    @Override
    public void setQueueCapacity(int capacity) {
        if (initialized) {
            if (getThreadPoolExecutor().getQueue() instanceof ResizableCapacityBlockingQueue<?> queue) {
                queue.setCapacity(capacity);
            } else if (capacity != 0) {
                throw new UnsupportedOperationException("running SynchronousQueue cannot change queue type");
            }
        }
        super.setQueueCapacity(capacity);
    }

    @Override
    public void setRejectedExecutionHandler(RejectedExecutionHandler handler) {
        Objects.requireNonNull(handler, "handler");
        super.setRejectedExecutionHandler((task, executor) -> {
            observer.rejectedSubmission();
            handler.rejectedExecution(task, executor);
        });
    }

    @Override
    public void execute(Runnable task) {
        observer.dispatch(observer.decorate(task, true), super::execute);
    }

    @Override
    public Future<?> submit(Runnable task) { return submit(task, null); }

    public <T> Future<T> submit(Runnable task, T result) {
        var future = observer.future(task, result, true);
        execute(future);
        return future;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        var future = observer.future(task, true);
        execute(future);
        return future;
    }

    @Override
    public CompletableFuture<Void> submitCompletable(Runnable task) {
        Objects.requireNonNull(task, "task");
        return observer.completable(this, task, () -> { task.run(); return null; }, true);
    }

    @Override
    public <T> CompletableFuture<T> submitCompletable(Callable<T> task) {
        return observer.completable(this, Objects.requireNonNull(task, "task"), task, true);
    }
}
