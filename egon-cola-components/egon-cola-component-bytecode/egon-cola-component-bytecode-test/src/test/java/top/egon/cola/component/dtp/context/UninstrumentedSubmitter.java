package top.egon.cola.component.dtp.context;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public final class UninstrumentedSubmitter {

    private UninstrumentedSubmitter() {
    }

    public static <V> Future<V> submit(ExecutorService executor, Callable<V> task) {
        return executor.submit(task);
    }
}
