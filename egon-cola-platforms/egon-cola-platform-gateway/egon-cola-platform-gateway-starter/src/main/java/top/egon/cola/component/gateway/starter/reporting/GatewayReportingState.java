package top.egon.cola.component.gateway.starter.reporting;

import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReportResult;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Maintains a thread-safe snapshot of the current Gateway reporting status.
 *
 * <p>This object is an in-memory observability view only. It is reset on every
 * application restart, which intentionally causes the current report to be
 * submitted again.
 *
 * <p>中文：该对象仅提供内存中的可观测状态视图。应用重启后状态会重置，因此每次
 * 启动都会重新提交当前报告。
 */
public final class GatewayReportingState {

    /** Latest reporting lifecycle snapshot. 最新的上报生命周期快照。 */
    private final AtomicReference<Snapshot> current =
            new AtomicReference<>(new Snapshot(
                    "PENDING",
                    null,
                    null,
                    null,
                    0
            ));

    /**
     * Returns the latest immutable reporting snapshot.
     * 中文：返回最新的不可变上报状态快照。
     *
     * @return current reporting snapshot
     */
    public Snapshot snapshot() {
        return current.get();
    }

    /**
     * Marks a reporting attempt as in progress while retaining prior success.
     * 中文：将上报标记为进行中，同时保留此前成功记录。
     *
     * @param attempt current attempt number
     */
    void attempting(int attempt) {
        Snapshot previous = current.get();
        current.set(new Snapshot(
                "REPORTING",
                previous.lastSuccessAt(),
                previous.result(),
                null,
                attempt
        ));
    }

    /**
     * Records a successful acknowledgement and its receipt.
     * 中文：记录成功确认及其回执。
     *
     * @param result acknowledged report result
     */
    void success(GatewayInterfaceDefinitionReportResult result) {
        current.set(new Snapshot(
                "SUCCESS",
                Instant.now(),
                result,
                null,
                current.get().attempt()
        ));
    }

    /**
     * Records a failed startup attempt.
     * 中文：记录启动阶段的一次失败尝试。
     *
     * @param error failure message to expose in bounded form
     */
    void failure(String error) {
        Snapshot previous = current.get();
        current.set(new Snapshot(
                "FAILED",
                previous.lastSuccessAt(),
                previous.result(),
                bounded(error),
                previous.attempt()
        ));
    }

    /**
     * Normalizes and limits an exposed error message to 512 characters.
     * 中文：规范化错误信息，并将对外暴露的长度限制为 512 个字符。
     *
     * @param value original failure message
     * @return non-null bounded failure message
     */
    private String bounded(String value) {
        if (value == null) {
            return "gateway report failed";
        }
        return value.length() <= 512 ? value : value.substring(0, 512);
    }

    /**
     * Immutable view of one reporting lifecycle state.
     * 中文：一次上报生命周期状态的不可变视图。
     *
     * @param status lifecycle status such as {@code PENDING} or {@code SUCCESS}
     * @param lastSuccessAt time of the latest successful acknowledgement
     * @param result latest successfully acknowledged result
     * @param lastError bounded message from the latest failure
     * @param attempt current or most recently completed attempt number
     */
    public record Snapshot(
            String status,
            Instant lastSuccessAt,
            GatewayInterfaceDefinitionReportResult result,
            String lastError,
            int attempt
    ) {
    }
}
