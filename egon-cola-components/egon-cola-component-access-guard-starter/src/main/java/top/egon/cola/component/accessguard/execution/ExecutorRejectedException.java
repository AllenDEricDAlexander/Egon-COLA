package top.egon.cola.component.accessguard.execution;

public final class ExecutorRejectedException extends RuntimeException {

    public ExecutorRejectedException(Throwable cause) {
        super("Access Guard executor rejected the operation", cause);
    }
}
