package top.egon.cola.component.common.trace.function;

@FunctionalInterface
public interface TraceCarrierWriter {

    void set(String name, String value);
}
