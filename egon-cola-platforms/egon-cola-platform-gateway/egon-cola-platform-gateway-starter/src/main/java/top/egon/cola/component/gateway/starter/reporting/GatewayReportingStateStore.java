package top.egon.cola.component.gateway.starter.reporting;

import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReportResult;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/**
 * Atomically persists the last pending or acknowledged report receipt.
 */
public final class GatewayReportingStateStore {

    private static final String VERSION = "1";

    private final Path path;

    private final Clock clock;

    public GatewayReportingStateStore(Path path) {
        this(path, Clock.systemUTC());
    }

    GatewayReportingStateStore(Path path, Clock clock) {
        this.path = Objects.requireNonNull(path, "path").toAbsolutePath();
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public synchronized Optional<StoredState> load() {
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        Properties values = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            values.load(input);
            if (!VERSION.equals(values.getProperty("version"))) {
                throw new IllegalArgumentException(
                        "unsupported reporting state version"
                );
            }
            return Optional.of(read(values));
        } catch (IOException | RuntimeException corrupted) {
            isolateCorrupted();
            return Optional.empty();
        }
    }

    public synchronized void pending(
            GatewayDefinitionReportFactory.BuiltReport report,
            String payloadHash) {
        save(new StoredState(
                Phase.PENDING,
                payloadHash,
                report.report().definitionSetId(),
                report.report().reportId(),
                null,
                null,
                clock.instant()
        ));
    }

    public synchronized void acknowledged(
            String payloadHash,
            GatewayInterfaceDefinitionReportResult result) {
        save(new StoredState(
                Phase.ACKNOWLEDGED,
                payloadHash,
                result.definitionSetId(),
                result.reportId(),
                result.status().name(),
                result.applicationId(),
                clock.instant()
        ));
    }

    private void save(StoredState state) {
        Path parent = path.getParent();
        Path temporary = parent.resolve(
                path.getFileName() + ".tmp-" + UuidV7.simpleString()
        );
        try {
            Files.createDirectories(parent);
            Properties values = new Properties();
            values.setProperty("version", VERSION);
            values.setProperty("phase", state.phase().name());
            values.setProperty("payloadHash", state.payloadHash());
            values.setProperty(
                    "definitionSetId",
                    state.definitionSetId()
            );
            values.setProperty("reportId", state.reportId());
            optional(values, "receiptStatus", state.receiptStatus());
            optional(values, "applicationId", state.applicationId());
            values.setProperty("updatedAt", state.updatedAt().toString());
            try (OutputStream output = Files.newOutputStream(temporary)) {
                values.store(output, "Egon COLA Gateway report state");
            }
            move(temporary, path);
        } catch (IOException failure) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException suppressed) {
                failure.addSuppressed(suppressed);
            }
            throw new IllegalStateException(
                    "failed to persist gateway reporting state",
                    failure
            );
        }
    }

    private StoredState read(Properties values) {
        return new StoredState(
                Phase.valueOf(required(values, "phase")),
                required(values, "payloadHash"),
                required(values, "definitionSetId"),
                required(values, "reportId"),
                values.getProperty("receiptStatus"),
                values.getProperty("applicationId"),
                Instant.parse(required(values, "updatedAt"))
        );
    }

    private void isolateCorrupted() {
        Path corrupted = path.resolveSibling(
                path.getFileName()
                        + ".corrupt-"
                        + clock.millis()
        );
        try {
            Files.move(
                    path,
                    corrupted,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException ignored) {
            // A read-only state directory still falls back to re-reporting.
        }
    }

    private void move(Path source, Path target) throws IOException {
        try {
            Files.move(
                    source,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(
                    source,
                    target,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    private void optional(
            Properties values,
            String name,
            String value) {
        if (value != null && !value.isBlank()) {
            values.setProperty(name, value);
        }
    }

    private String required(Properties values, String name) {
        String value = values.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "missing reporting state " + name
            );
        }
        return value;
    }

    public enum Phase {
        PENDING,
        ACKNOWLEDGED
    }

    public record StoredState(
            Phase phase,
            String payloadHash,
            String definitionSetId,
            String reportId,
            String receiptStatus,
            String applicationId,
            Instant updatedAt
    ) {

        public StoredState {
            Objects.requireNonNull(phase, "phase");
            payloadHash = required(payloadHash, "payloadHash");
            definitionSetId = required(
                    definitionSetId,
                    "definitionSetId"
            );
            reportId = required(reportId, "reportId");
            Objects.requireNonNull(updatedAt, "updatedAt");
        }

        public boolean matches(
                GatewayDefinitionReportFactory.BuiltReport report,
                String expectedPayloadHash) {
            return payloadHash.equals(expectedPayloadHash)
                    && definitionSetId.equals(
                    report.report().definitionSetId()
            );
        }

        private static String required(String value, String name) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(name + " is required");
            }
            return value;
        }
    }
}
