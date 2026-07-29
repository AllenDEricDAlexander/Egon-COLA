package top.egon.cola.component.common.trace.function;

@FunctionalInterface
public interface TraceCarrierReader {

    String get(String name);
}
