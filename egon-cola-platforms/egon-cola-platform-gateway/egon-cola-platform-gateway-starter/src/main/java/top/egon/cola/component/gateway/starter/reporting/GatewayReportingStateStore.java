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

    /** Version of the on-disk properties format. */
    private static final String VERSION = "1";

    /** Absolute path of the reporting state file. */
    private final Path path;

    /** Time source for state timestamps and corrupted-file suffixes. */
    private final Clock clock;

    /**
     * Creates a state store using the UTC system clock.
     *
     * @param path reporting state file path
     */
    public GatewayReportingStateStore(Path path) {
        this(path, Clock.systemUTC());
    }

    /**
     * Creates a state store with an injectable time source.
     *
     * @param path reporting state file path
     * @param clock timestamp source
     */
    GatewayReportingStateStore(Path path, Clock clock) {
        this.path = Objects.requireNonNull(path, "path").toAbsolutePath();
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Loads the persisted state, isolating an unreadable or invalid file.
     *
     * @return stored state, or empty when no usable state exists
     */
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

    /**
     * Persists that a report is awaiting acknowledgement.
     *
     * @param report pending report
     * @param payloadHash fingerprint of the serialized report definition
     */
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

    /**
     * Persists the acknowledgement receipt for a submitted report.
     *
     * @param payloadHash fingerprint of the acknowledged report definition
     * @param result acknowledgement receipt
     */
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

    /**
     * Writes state to a temporary file and replaces the destination atomically
     * when the file system supports it.
     *
     * @param state state to persist
     * @throws IllegalStateException if the state cannot be persisted
     */
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

    /**
     * Parses and validates stored properties.
     *
     * @param values persisted properties
     * @return parsed reporting state
     * @throws IllegalArgumentException if a required value is absent or invalid
     */
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

    /** Moves an unreadable state file aside so reporting can restart cleanly. */
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

    /**
     * Replaces the target with the source, preferring an atomic move.
     *
     * @param source temporary state file
     * @param target final state file
     * @throws IOException if both the atomic and fallback move fail
     */
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

    /**
     * Stores a non-blank optional property.
     *
     * @param values destination properties
     * @param name property name
     * @param value optional property value
     */
    private void optional(
            Properties values,
            String name,
            String value) {
        if (value != null && !value.isBlank()) {
            values.setProperty(name, value);
        }
    }

    /**
     * Reads a required non-blank property.
     *
     * @param values source properties
     * @param name property name
     * @return non-blank property value
     * @throws IllegalArgumentException if the property is absent or blank
     */
    private String required(Properties values, String name) {
        String value = values.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "missing reporting state " + name
            );
        }
        return value;
    }

    /** Lifecycle phase persisted for a Gateway definition report. */
    public enum Phase {
        /** The report has been persisted but is not yet acknowledged. */
        PENDING,

        /** Gateway Admin has acknowledged the report. */
        ACKNOWLEDGED
    }

    /**
     * Validated persistent state for a pending or acknowledged report.
     *
     * @param phase persistence lifecycle phase
     * @param payloadHash fingerprint of the report definition payload
     * @param definitionSetId stable definition set identifier
     * @param reportId concrete report identifier
     * @param receiptStatus optional acknowledgement status
     * @param applicationId optional Admin application identifier
     * @param updatedAt time the state was persisted
     */
    public record StoredState(
            Phase phase,
            String payloadHash,
            String definitionSetId,
            String reportId,
            String receiptStatus,
            String applicationId,
            Instant updatedAt
    ) {

        /** Validates required state fields during construction. */
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

        /**
         * Tests whether this state belongs to a built report and payload.
         *
         * @param report report to compare
         * @param expectedPayloadHash expected report payload fingerprint
         * @return {@code true} when the definition and payload match
         */
        public boolean matches(
                GatewayDefinitionReportFactory.BuiltReport report,
                String expectedPayloadHash) {
            return payloadHash.equals(expectedPayloadHash)
                    && definitionSetId.equals(
                    report.report().definitionSetId()
            );
        }

        /**
         * Requires a non-blank stored string.
         *
         * @param value stored value
         * @param name field name used in validation errors
         * @return validated value
         * @throws IllegalArgumentException if the value is absent or blank
         */
        private static String required(String value, String name) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(name + " is required");
            }
            return value;
        }
    }
}
