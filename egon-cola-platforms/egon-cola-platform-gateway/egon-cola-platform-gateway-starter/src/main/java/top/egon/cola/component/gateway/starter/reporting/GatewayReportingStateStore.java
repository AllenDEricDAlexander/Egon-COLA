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
 *
 * <p>The store uses versioned Java properties, writes through a temporary file,
 * and prefers an atomic move when replacing the destination. Invalid or
 * unreadable files are isolated so a corrupt local state cannot prevent a fresh
 * report from being submitted.
 *
 * <p>中文：存储使用带版本的 Java Properties 格式，先写入临时文件，并在替换目标
 * 文件时优先使用原子移动。无效或不可读的文件会被隔离，避免损坏的本地状态阻止
 * 重新提交报告。
 */
public final class GatewayReportingStateStore {

    /** Version of the on-disk properties format. 磁盘属性文件格式的版本。 */
    private static final String VERSION = "1";

    /** Absolute path of the reporting state file. 上报状态文件的绝对路径。 */
    private final Path path;

    /**
     * Time source for state timestamps and corrupted-file suffixes.
     * 生成状态时间戳及损坏文件后缀的时间源。
     */
    private final Clock clock;

    /**
     * Creates a state store using the UTC system clock.
     * 中文：使用 UTC 系统时钟创建状态存储。
     *
     * @param path reporting state file path
     */
    public GatewayReportingStateStore(Path path) {
        this(path, Clock.systemUTC());
    }

    /**
     * Creates a state store with an injectable time source.
     * 中文：使用可注入的时间源创建状态存储，便于测试。
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
     * 中文：加载持久化状态，并将不可读或无效文件隔离保存。
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
     * 中文：持久化报告正在等待确认这一状态。
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
     * 中文：持久化已提交报告的确认回执。
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
     * 中文：先写入临时文件，并在文件系统支持时以原子移动替换目标
     * 文件。
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
     * 中文：解析并校验持久化的属性内容。
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

    /**
     * Moves an unreadable state file aside so reporting can restart cleanly.
     * 将不可读状态文件移开，使上报可以干净地重新开始。
     */
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
     * 中文：优先使用原子移动，用源文件替换目标文件。
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
     * 中文：保存非空白的可选属性。
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
     * 中文：读取必需且非空白的属性。
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

    /**
     * Lifecycle phase persisted for a Gateway definition report.
     * 网关接口定义报告持久化的生命周期阶段。
     */
    public enum Phase {
        /**
         * The report has been persisted but is not yet acknowledged.
         * 报告已持久化但尚未确认。
         */
        PENDING,

        /**
         * Gateway Admin has acknowledged the report.
         * Gateway Admin 已确认该报告。
         */
        ACKNOWLEDGED
    }

    /**
     * Validated persistent state for a pending or acknowledged report.
     * 中文：经过校验的待处理或已确认报告持久化状态。
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

        /**
         * Validates required state fields during construction.
         * 在构造时校验状态必需字段。
         */
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
         * 中文：判断该状态是否属于指定的已构建报告及其载荷。
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
         * 中文：要求存储字符串非空且不全为空白。
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
