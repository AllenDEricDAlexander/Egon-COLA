package top.egon.cola.component.gateway.starter.reporting;

import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReportResult;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Maintains a thread-safe snapshot of the current Gateway reporting status.
 */
public final class GatewayReportingState {

    /** Latest reporting lifecycle snapshot. */
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
     *
     * @return current reporting snapshot
     */
    public Snapshot snapshot() {
        return current.get();
    }

    /**
     * Marks a reporting attempt as in progress while retaining prior success.
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
     * Records a failed attempt while retaining the last successful receipt.
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
