package top.egon.cola.component.common.trace.autoconfigure;

import org.springframework.core.task.TaskDecorator;
import top.egon.cola.component.common.trace.TraceSnapshot;

/**
 * Spring task decorator that captures the current trace context per task.
 */
public final class TraceTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        return TraceSnapshot.capture().wrap(runnable);
    }
}
