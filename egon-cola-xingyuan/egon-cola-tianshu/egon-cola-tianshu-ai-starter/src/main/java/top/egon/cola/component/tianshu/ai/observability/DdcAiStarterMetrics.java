package top.egon.cola.component.tianshu.ai.observability;

/**
 * Starter 计数器端口；宿主有 Micrometer 时绑定到 MeterRegistry，否则为空实现。
 * / Counter port of the starter; bound to a MeterRegistry when the host has Micrometer, otherwise a no-op.
 */
@FunctionalInterface
public interface DdcAiStarterMetrics {

    DdcAiStarterMetrics NOOP = (name, amount, tags) -> { };

    /** 以成对的 tag 键值递增计数器。 / Increments a counter tagged with key/value pairs. */
    void increment(String name, long amount, String... tags);
}
