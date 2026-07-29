package top.egon.cola.component.common.trace;

@FunctionalInterface
public interface TraceCarrierReader {

    String get(String name);
}
