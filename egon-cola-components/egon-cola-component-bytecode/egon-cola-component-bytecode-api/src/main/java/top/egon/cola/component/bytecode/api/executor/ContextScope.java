package top.egon.cola.component.bytecode.api.executor;

public interface ContextScope extends AutoCloseable {

    @Override
    void close();
}
