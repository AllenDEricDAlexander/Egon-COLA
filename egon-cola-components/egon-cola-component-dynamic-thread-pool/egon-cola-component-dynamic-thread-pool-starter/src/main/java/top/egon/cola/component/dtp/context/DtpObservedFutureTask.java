package top.egon.cola.component.dtp.context;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;

/**
 * @description Future 的取消和真实业务终态共享一次观察状态
 */
public final class DtpObservedFutureTask<T> extends FutureTask<T> implements DtpTaskObserver.ObservedTask {

    private final DtpTaskObserver.Observation observation;
    private Consumer<DtpObservedFutureTask<T>> completion;

    DtpObservedFutureTask(DtpTaskObserver.Observation observation, Callable<T> callable) {
        super(() -> observation.call(callable));
        this.observation = observation;
        observation.managedFuture = true;
    }

    public void onCompletion(Consumer<DtpObservedFutureTask<T>> completion) {
        this.completion = completion;
    }

    @Override
    public DtpTaskObserver.Observation observation() {
        return observation;
    }

    @Override
    protected void done() {
        if (isCancelled()) { observation.cancel(); }
        else {
            try { get(); observation.succeed(); }
            catch (java.util.concurrent.CancellationException e) { observation.cancel(); }
            catch (java.util.concurrent.ExecutionException e) { observation.fail(e.getCause()); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); observation.fail(e); }
        }
        if (completion != null) {
            completion.accept(this);
        }
    }
}
