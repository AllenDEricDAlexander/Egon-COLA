package top.egon.cola.component.bytecode.api.executor;

public interface ContextCarrier {

    String name();

    Object capture();

    ContextScope restore(Object snapshot);
}
