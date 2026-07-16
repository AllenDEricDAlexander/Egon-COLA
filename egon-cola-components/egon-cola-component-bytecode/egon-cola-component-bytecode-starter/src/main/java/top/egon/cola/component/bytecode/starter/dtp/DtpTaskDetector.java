package top.egon.cola.component.bytecode.starter.dtp;

import java.util.Set;

public final class DtpTaskDetector {

    private static final Set<String> DTP_WRAPPERS = Set.of(
            "top.egon.cola.component.dtp.context.DtpRunnable",
            "top.egon.cola.component.dtp.context.DtpCallable"
    );

    public boolean instrumented(Object task) {
        return task != null && DTP_WRAPPERS.contains(task.getClass().getName());
    }
}
