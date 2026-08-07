package top.egon.cola.component.bytecode.runtime.executor;

public final class RuntimeTaskDetector {

    public boolean instrumented(Object task) {
        return task instanceof EgonInstrumentedTask;
    }
}
