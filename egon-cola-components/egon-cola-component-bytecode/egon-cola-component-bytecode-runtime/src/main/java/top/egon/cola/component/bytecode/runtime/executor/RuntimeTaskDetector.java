package top.egon.cola.component.bytecode.runtime.executor;

import java.util.Set;

public final class RuntimeTaskDetector {

    private static final Set<String> DTP_TASK_TYPES = Set.of(
            "top.egon.cola.component.dtp.context.DtpRunnable",
            "top.egon.cola.component.dtp.context.DtpCallable"
    );

    public boolean instrumented(Object task) {
        return task instanceof EgonInstrumentedTask
                || task != null && DTP_TASK_TYPES.contains(task.getClass().getName());
    }
}
