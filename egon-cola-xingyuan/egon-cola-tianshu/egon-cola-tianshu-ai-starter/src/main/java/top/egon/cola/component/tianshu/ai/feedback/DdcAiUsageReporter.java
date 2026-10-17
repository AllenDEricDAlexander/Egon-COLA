package top.egon.cola.component.tianshu.ai.feedback;

import top.egon.cola.component.tianshu.model.ai.DdcAiUsageReportDTO;

/**
 * 下游使用上报 SPI：非阻塞入队，队列满、已关闭或校验失败时返回 false 并计数，从不抛出。
 * / Downstream usage-report SPI: a non-blocking enqueue that returns false and counts when the queue is full, the
 * reporter is closed or validation fails; it never throws.
 */
public interface DdcAiUsageReporter {

    boolean report(DdcAiUsageReportDTO report);
}
