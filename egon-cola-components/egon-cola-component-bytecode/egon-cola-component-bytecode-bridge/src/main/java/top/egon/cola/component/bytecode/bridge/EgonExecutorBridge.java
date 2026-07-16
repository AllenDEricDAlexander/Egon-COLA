package top.egon.cola.component.bytecode.bridge;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

public final class EgonExecutorBridge {

    private EgonExecutorBridge() {
    }

    public static void execute(Executor executor, Runnable task, Class<?> caller, long id) {
        BytecodeRuntimeDispatcher dispatcher = dispatcher(caller);
        Runnable decorated = decorateRunnable(dispatcher, caller, executor, task, id);
        try {
            executor.execute(decorated);
        } catch (RejectedExecutionException exception) {
            reportRejection(dispatcher, caller, id, executor, exception);
            throw exception;
        }
    }

    public static Future<?> submit(
            ExecutorService executor,
            Runnable task,
            Class<?> caller,
            long id
    ) {
        BytecodeRuntimeDispatcher dispatcher = dispatcher(caller);
        Runnable decorated = decorateRunnable(dispatcher, caller, executor, task, id);
        try {
            return executor.submit(decorated);
        } catch (RejectedExecutionException exception) {
            reportRejection(dispatcher, caller, id, executor, exception);
            throw exception;
        }
    }

    public static <T> Future<T> submit(
            ExecutorService executor,
            Runnable task,
            T result,
            Class<?> caller,
            long id
    ) {
        BytecodeRuntimeDispatcher dispatcher = dispatcher(caller);
        Runnable decorated = decorateRunnable(dispatcher, caller, executor, task, id);
        try {
            return executor.submit(decorated, result);
        } catch (RejectedExecutionException exception) {
            reportRejection(dispatcher, caller, id, executor, exception);
            throw exception;
        }
    }

    public static <T> Future<T> submit(
            ExecutorService executor,
            Callable<T> task,
            Class<?> caller,
            long id
    ) {
        BytecodeRuntimeDispatcher dispatcher = dispatcher(caller);
        Callable<T> decorated = decorateCallable(dispatcher, caller, executor, task, id);
        try {
            return executor.submit(decorated);
        } catch (RejectedExecutionException exception) {
            reportRejection(dispatcher, caller, id, executor, exception);
            throw exception;
        }
    }

    private static BytecodeRuntimeDispatcher dispatcher(Class<?> caller) {
        return DispatcherRegistry.dispatcher(caller, BridgeCapability.EXECUTOR).orElse(null);
    }

    private static Runnable decorateRunnable(
            BytecodeRuntimeDispatcher dispatcher,
            Class<?> caller,
            Executor executor,
            Runnable task,
            long id
    ) {
        if (dispatcher == null) {
            return task;
        }
        try {
            Runnable decorated = dispatcher.decorateRunnable(caller, executor, task, id);
            return decorated == null ? task : decorated;
        } catch (Throwable ignored) {
            return task;
        }
    }

    private static <T> Callable<T> decorateCallable(
            BytecodeRuntimeDispatcher dispatcher,
            Class<?> caller,
            Executor executor,
            Callable<T> task,
            long id
    ) {
        if (dispatcher == null) {
            return task;
        }
        try {
            Callable<T> decorated = dispatcher.decorateCallable(caller, executor, task, id);
            return decorated == null ? task : decorated;
        } catch (Throwable ignored) {
            return task;
        }
    }

    private static void reportRejection(
            BytecodeRuntimeDispatcher dispatcher,
            Class<?> caller,
            long id,
            Executor executor,
            RejectedExecutionException exception
    ) {
        if (dispatcher == null) {
            return;
        }
        try {
            dispatcher.executorRejected(caller, id, executor, exception);
        } catch (Throwable ignored) {
            // Rejection reporting must never replace the original exception.
        }
    }
}
