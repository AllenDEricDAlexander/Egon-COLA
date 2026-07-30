package top.egon.cola.component.gateway.starter.reporting;

import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReportResult;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public final class GatewayReportingState {

    private final AtomicReference<Snapshot> current =
            new AtomicReference<>(new Snapshot(
                    "PENDING",
                    null,
                    null,
                    null,
                    0
            ));

    public Snapshot snapshot() {
        return current.get();
    }

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

    void success(GatewayInterfaceDefinitionReportResult result) {
        current.set(new Snapshot(
                "SUCCESS",
                Instant.now(),
                result,
                null,
                current.get().attempt()
        ));
    }

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

    private String bounded(String value) {
        if (value == null) {
            return "gateway report failed";
        }
        return value.length() <= 512 ? value : value.substring(0, 512);
    }

    public record Snapshot(
            String status,
            Instant lastSuccessAt,
            GatewayInterfaceDefinitionReportResult result,
            String lastError,
            int attempt
    ) {
    }
}
