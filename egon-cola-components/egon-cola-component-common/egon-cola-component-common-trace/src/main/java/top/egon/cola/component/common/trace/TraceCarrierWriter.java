package top.egon.cola.component.common.trace;

@FunctionalInterface
public interface TraceCarrierWriter {

    void set(String name, String value);
}
