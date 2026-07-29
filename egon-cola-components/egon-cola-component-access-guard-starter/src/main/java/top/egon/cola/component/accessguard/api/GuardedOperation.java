package top.egon.cola.component.accessguard.api;

@FunctionalInterface
public interface GuardedOperation<T> {

    T execute() throws Throwable;
}
