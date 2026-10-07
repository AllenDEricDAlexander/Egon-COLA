package top.egon.cola.component.tianshu.admin.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import top.egon.cola.component.tianshu.admin.migration.DdcLegacyTableStrategy.DdcLegacyParentReferenceVO;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;
import jakarta.validation.Validation;

/** Offline, explicitly invoked migration operations for the frozen Tianshu V9 source. */
@Slf4j
public final class DdcLegacyImportService {

    private static final int TARGET_BATCH_SIZE = 1000;

    public DdcLegacyImportService() {
    }

    static boolean verifyMappingSignature(byte[] payload, byte[] signature, String trustedPublicKeyBase64) {
        if (payload == null || payload.length == 0 || signature == null || signature.length == 0
                || trustedPublicKeyBase64 == null || trustedPublicKeyBase64.isBlank()) {
            return false;
        }
        try {
            byte[] keyBytes = Base64.getDecoder().decode(trustedPublicKeyBase64);
            var key = KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(keyBytes));
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(key);
            verifier.update(payload);
            return verifier.verify(signature);
        } catch (GeneralSecurityException | IllegalArgumentException failure) {
            return false;
        }
    }

    public DdcMigrationReportResult validateDatabase(Connection source, Connection target, JsonNode mapping,
                                                     String targetRouteFingerprint,
                                                     DdcLegacyTableStrategy strategy) {
        return inspectDatabase("VALIDATE", source, target, mapping, targetRouteFingerprint, strategy, false);
    }

    public DdcMigrationReportResult verifyDatabase(Connection source, Connection target, JsonNode mapping,
                                                   String targetRouteFingerprint,
                                                   DdcLegacyTableStrategy strategy) {
        return inspectDatabase("VERIFY", source, target, mapping, targetRouteFingerprint, strategy, true);
    }

    public DdcMigrationReportResult importDatabase(Connection source, Connection target, JsonNode mapping,
                                                   String targetRouteFingerprint, String jobId,
                                                   DdcLegacyTableStrategy strategy, LongSupplier idSupplier,
                                                   Clock clock, Path reportDirectory) {
        DdcMigrationReportResult validation = validateDatabase(source, target, mapping,
                targetRouteFingerprint, strategy);
        if (!"SUCCESS".equals(validation.getStatus())) return withMode(validation, "IMPORT");
        if (!jobId.equals(text(mapping.get("jobId")))) {
            return failedDatabaseReport("IMPORT", jobId, validation.getSourceFingerprint(), targetRouteFingerprint,
                    "MIGRATION_JOB_ID_MISMATCH", "MAPPING", null, 0, 0, 0, null);
        }

        long rowsRead = 0;
        long rowsInserted = 0;
        long rowsSkipped = 0;
        String lastSourceId = null;
        Map<String, Long> rowsByTable = new TreeMap<>();
        Set<Long> registeredTenants = registeredTenantIds(mapping);
        try {
            Map<String, DdcMigrationCheckpointBO> checkpoints = readCheckpoints(
                    reportDirectory, jobId, validation.getSourceFingerprint());
            for (String table : strategy.orderedLegacyTableNames()) {
                DdcMigrationCheckpointBO checkpoint = checkpoints.get(table);
                long tableRowsProcessed = 0;
                String pageAfter = null;
                if (checkpoint != null) {
                    long verifiedTargetRows = verifyCheckpointPrefix(source, target, mapping, strategy, table,
                            checkpoint, validation.getSourceFingerprint(), targetRouteFingerprint,
                            registeredTenants);
                    tableRowsProcessed = checkpoint.rowCount();
                    pageAfter = checkpoint.lastOriginalId();
                    rowsRead += tableRowsProcessed;
                    rowsSkipped += verifiedTargetRows;
                    lastSourceId = pageAfter;
                }
                rowsByTable.put(table, tableRowsProcessed);
                List<DdcLegacyRowDTO> pendingRows = new ArrayList<>();
                List<String> pendingSourceIds = new ArrayList<>();
                while (true) {
                    List<Map<String, Object>> sourcePage = readSourcePage(source, table, pageAfter, TARGET_BATCH_SIZE);
                    if (sourcePage.isEmpty()) break;
                    for (Map<String, Object> sourceRow : sourcePage) {
                        String legacyId = sourceId(sourceRow);
                        List<Long> tenants = strategy.tenantIds(
                                mapping.path("tenantMappings"), table, legacyId, registeredTenants);
                        List<DdcLegacyRowDTO> group = mapRowsForTenants(
                                target, mapping, strategy, table, sourceRow, tenants, registeredTenants, true);
                        if (group.isEmpty() || group.size() > TARGET_BATCH_SIZE) {
                            throw new IllegalStateException("TIANSHU_MIGRATION_TENANT_FANOUT_LIMIT");
                        }
                        if (!pendingRows.isEmpty() && pendingRows.size() + group.size() > TARGET_BATCH_SIZE) {
                            DdcMigrationReportResult batch = importRows(
                                    target, pendingRows, jobId, strategy, idSupplier, clock);
                            rowsInserted += batch.getRowsInserted();
                            rowsSkipped += batch.getRowsSkipped();
                            if (!"SUCCESS".equals(batch.getStatus())) {
                                rowsByTable.put(table, tableRowsProcessed);
                                return withCheckpoint(failedDatabaseReport("IMPORT", jobId,
                                        validation.getSourceFingerprint(), targetRouteFingerprint,
                                        batch.getErrorCode(), "IMPORT_BATCH", table, batch.getErrorLegacyId(),
                                        batch.getErrorHash(), rowsRead, rowsInserted, rowsSkipped, lastSourceId,
                                        rowsByTable), reportDirectory, jobId);
                            }
                            lastSourceId = pendingSourceIds.getLast();
                            rowsRead += pendingSourceIds.size();
                            tableRowsProcessed += pendingSourceIds.size();
                            writeCheckpoint(reportDirectory, jobId, validation.getSourceFingerprint(), table,
                                    lastSourceId, tableRowsProcessed);
                            rowsByTable.put(table, tableRowsProcessed);
                            pendingRows.clear();
                            pendingSourceIds.clear();
                        }
                        pendingRows.addAll(group);
                        pendingSourceIds.add(legacyId);
                        pageAfter = legacyId;
                    }
                    if (!pendingRows.isEmpty()) {
                        DdcMigrationReportResult batch = importRows(
                                target, pendingRows, jobId, strategy, idSupplier, clock);
                        rowsInserted += batch.getRowsInserted();
                        rowsSkipped += batch.getRowsSkipped();
                        if (!"SUCCESS".equals(batch.getStatus())) {
                            rowsByTable.put(table, tableRowsProcessed);
                            return withCheckpoint(failedDatabaseReport("IMPORT", jobId,
                                    validation.getSourceFingerprint(), targetRouteFingerprint,
                                    batch.getErrorCode(), "IMPORT_BATCH", table, batch.getErrorLegacyId(),
                                    batch.getErrorHash(), rowsRead, rowsInserted, rowsSkipped, lastSourceId,
                                    rowsByTable), reportDirectory, jobId);
                        }
                        lastSourceId = pendingSourceIds.getLast();
                        rowsRead += pendingSourceIds.size();
                        tableRowsProcessed += pendingSourceIds.size();
                        writeCheckpoint(reportDirectory, jobId, validation.getSourceFingerprint(), table,
                                lastSourceId, tableRowsProcessed);
                        rowsByTable.put(table, tableRowsProcessed);
                        pendingRows.clear();
                        pendingSourceIds.clear();
                    }
                    pageAfter = sourcePage.getLast() == null ? pageAfter : sourceId(sourcePage.getLast());
                }
                rowsByTable.put(table, tableRowsProcessed);
            }
            return withCheckpoint(databaseReport("IMPORT", jobId, validation.getSourceFingerprint(),
                    targetRouteFingerprint, "SUCCESS", rowsRead, rowsInserted, rowsSkipped, 0, lastSourceId,
                    null, null, rowsByTable), reportDirectory, jobId);
        } catch (SQLException failure) {
            return withCheckpoint(failedDatabaseReport("IMPORT", jobId, validation.getSourceFingerprint(),
                    targetRouteFingerprint, "MIGRATION_DATABASE_FAILURE", "IMPORT", null, lastSourceId, null,
                    rowsRead, rowsInserted, rowsSkipped, lastSourceId, rowsByTable), reportDirectory, jobId);
        } catch (IOException failure) {
            return withCheckpoint(failedDatabaseReport("IMPORT", jobId, validation.getSourceFingerprint(),
                    targetRouteFingerprint, "MIGRATION_CHECKPOINT_FAILURE", "CHECKPOINT", null, lastSourceId,
                    null, rowsRead, rowsInserted, rowsSkipped, lastSourceId, rowsByTable), reportDirectory, jobId);
        } catch (IllegalArgumentException | IllegalStateException failure) {
            return withCheckpoint(failedDatabaseReport("IMPORT", jobId, validation.getSourceFingerprint(),
                    targetRouteFingerprint, safeFailureCode(failure), "IMPORT", null, lastSourceId, null,
                    rowsRead, rowsInserted, rowsSkipped, lastSourceId, rowsByTable), reportDirectory, jobId);
        }
    }

    private DdcMigrationReportResult inspectDatabase(String mode, Connection source, Connection target,
                                                    JsonNode mapping, String targetRouteFingerprint,
                                                    DdcLegacyTableStrategy strategy, boolean requireComplete) {
        String jobId = text(mapping == null ? null : mapping.get("jobId"));
        String sourceFingerprint = null;
        long rowsRead = 0;
        Map<String, Long> rowsByTable = new TreeMap<>();
        String lastSourceId = null;
        String errorStage = "MAPPING";
        String errorTable = null;
        String errorLegacyId = null;
        String errorHash = null;
        try {
            requireMigrationHeader(mapping, targetRouteFingerprint);
            errorStage = "SOURCE_HISTORY";
            if (!source.isReadOnly()) throw new IllegalStateException("TIANSHU_MIGRATION_SOURCE_NOT_READ_ONLY");
            Map<String, String> actualHistory = readFlywayHistory(source);
            Map<String, String> expectedHistory = expectedHistory(mapping);
            if (!actualHistory.equals(expectedHistory)) {
                throw new IllegalStateException("TIANSHU_MIGRATION_SOURCE_HISTORY_MISMATCH");
            }
            String sourceIdentityFingerprint = sourceFingerprint(source, actualHistory);
            MessageDigest sourceDataDigest = sha256Digest();
            Set<Long> registeredTenants = registeredTenantIds(mapping);
            validateTenantMappingShape(mapping, strategy, registeredTenants);
            Map<String, Long> expectedCounts = new HashMap<>();

            for (String table : strategy.orderedLegacyTableNames()) {
                errorStage = "SOURCE_SCHEMA";
                errorTable = table;
                validateSourceColumns(source, table, strategy.sourceColumnNames(table));
                Set<String> sourceIds = new HashSet<>();
                String pageLastId = null;
                long tableSourceRows = 0;
                while (true) {
                    List<Map<String, Object>> page = readSourcePage(source, table, pageLastId, TARGET_BATCH_SIZE);
                    if (page.isEmpty()) break;
                    for (Map<String, Object> sourceRow : page) {
                        String legacyId = sourceId(sourceRow);
                        errorStage = "TENANT_MAPPING";
                        errorLegacyId = legacyId;
                        if (!sourceIds.add(legacyId)) {
                            throw new IllegalStateException("TIANSHU_MIGRATION_SOURCE_ID_DUPLICATE: " + table);
                        }
                        List<Long> tenantIds = strategy.tenantIds(
                                mapping.path("tenantMappings"), table, legacyId, registeredTenants);
                        if (tenantIds.isEmpty()) {
                            throw new IllegalStateException("TIANSHU_LEGACY_TENANT_REQUIRED: " + table);
                        }
                        for (Long tenantId : tenantIds) {
                            List<DdcLegacyParentReferenceVO> parents = strategy.parentReferences(table, sourceRow);
                            validateParentTenantAssignments(mapping, strategy, parents, tenantId,
                                    registeredTenants);
                            errorStage = "ROW_MAPPING";
                            DdcLegacyParentResolutionBO resolution = resolveTargetParentIds(
                                    target, table, sourceRow, tenantId, strategy, false);
                            DdcLegacyRowDTO row = strategy.mapSourceRow(
                                    table, sourceRow, tenantId, resolution.targetIds());
                            updateSourceFingerprint(sourceDataDigest, row);
                            errorHash = row.getSourceHash();
                            errorStage = "TARGET_COMPARE";
                            Map<String, Object> existing = selectExisting(
                                    target, row.getTargetTable(), row, strategy);
                            if (existing != null) {
                                if (resolution.missingParent() || !sameTargetValues(existing, row, strategy)) {
                                    throw new IllegalStateException("TIANSHU_MIGRATION_TARGET_ROW_MISMATCH: " + table);
                                }
                            } else if (requireComplete) {
                                throw new IllegalStateException("TIANSHU_MIGRATION_TARGET_ROW_MISSING: " + table);
                            }
                            expectedCounts.merge(tableTenantKey(table, tenantId), 1L, Long::sum);
                        }
                        tableSourceRows++;
                        rowsRead++;
                        lastSourceId = legacyId;
                    }
                    pageLastId = sourceId(page.getLast());
                }
                verifyTenantMappingCoverage(mapping, table, sourceIds);
                rowsByTable.put(table, tableSourceRows);
            }

            errorStage = "TARGET_COUNTS";
            for (String table : strategy.orderedLegacyTableNames()) {
                errorTable = table;
                long expectedTableRows = expectedCounts.entrySet().stream()
                        .filter(entry -> entry.getKey().startsWith(table + ":"))
                        .mapToLong(Map.Entry::getValue).sum();
                long actualTableRows = countRows(target, strategy.targetTableName(table));
                if (requireComplete ? actualTableRows != expectedTableRows : actualTableRows > expectedTableRows) {
                    throw new IllegalStateException("TIANSHU_MIGRATION_TARGET_ROW_COUNT_MISMATCH: " + table);
                }
            }
            errorStage = "TARGET_MAPPING";
            validateTargetRowsHaveSourceMapping(target, mapping, strategy, registeredTenants);
            sourceFingerprint = completeSourceFingerprint(sourceIdentityFingerprint, sourceDataDigest.digest());
            if (!sourceFingerprint.equals(text(mapping.get("sourceFingerprint")))) {
                throw new IllegalStateException("TIANSHU_MIGRATION_SOURCE_FINGERPRINT_MISMATCH");
            }
            return databaseReport(mode, jobId, sourceFingerprint, targetRouteFingerprint, "SUCCESS", rowsRead,
                    0, 0, 0, lastSourceId, null, null, null, null, null, rowsByTable);
        } catch (SQLException failure) {
            return databaseReport(mode, jobId, sourceFingerprint, targetRouteFingerprint, "FAILED", rowsRead,
                    0, 0, 1, lastSourceId, "MIGRATION_DATABASE_FAILURE", errorStage,
                    errorTable, errorLegacyId, errorHash, rowsByTable);
        } catch (IllegalArgumentException | IllegalStateException failure) {
            return databaseReport(mode, jobId, sourceFingerprint, targetRouteFingerprint, "FAILED", rowsRead,
                    0, 0, 1, lastSourceId, safeFailureCode(failure), errorStage,
                    errorTable, errorLegacyId, errorHash, rowsByTable);
        }
    }

    public DdcMigrationReportResult importRows(Connection targetConnection, List<DdcLegacyRowDTO> rows,
                                               String jobId, DdcLegacyTableStrategy strategy,
                                               LongSupplier idSupplier, Clock clock) throws SQLException {
        Objects.requireNonNull(targetConnection, "targetConnection");
        Objects.requireNonNull(rows, "rows");
        Objects.requireNonNull(strategy, "strategy");
        Objects.requireNonNull(idSupplier, "idSupplier");
        Objects.requireNonNull(clock, "clock");
        if (jobId == null || !jobId.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}") || jobId.contains("..")) {
            return failedReport(jobId, "INVALID_JOB_ID", 0, 0, 0, null);
        }
        if (rows.isEmpty()) return successfulReport(jobId, 0, 0, 0, null);
        if (!targetConnection.getAutoCommit()) {
            return failedReport(jobId, "TARGET_AUTOCOMMIT_REQUIRED", rows.size(), 0, 0, null);
        }

        long inserted = 0;
        long skipped = 0;
        String lastSourceId = null;
        int start = 0;
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            var validator = validatorFactory.getValidator();
            while (start < rows.size()) {
                int end = Math.min(start + TARGET_BATCH_SIZE, rows.size());
                targetConnection.setAutoCommit(false);
                boolean transactionActive = true;
                long batchInserted = 0;
                long batchSkipped = 0;
                String batchLastSourceId = lastSourceId;
                String failureCode = null;
                DdcLegacyRowDTO failureRow = null;
                DdcLegacyRowDTO currentRow = null;
                try {
                    for (int index = start; index < end; index++) {
                        DdcLegacyRowDTO row = rows.get(index);
                        currentRow = row;
                        if (!validator.validate(row).isEmpty()) {
                            failureCode = "LEGACY_ROW_VALIDATION_FAILED";
                            failureRow = row;
                            break;
                        }
                        String table = strategy.targetTableName(row.getSourceTable());
                        if (!table.equals(row.getTargetTable())) {
                            failureCode = "LEGACY_TARGET_TABLE_INVALID";
                            failureRow = row;
                            break;
                        }
                        Map<String, Object> existing = selectExisting(targetConnection, table, row, strategy);
                        if (existing != null) {
                            if (!sameTargetValues(existing, row, strategy)) {
                                failureCode = "TARGET_ROW_MISMATCH";
                                failureRow = row;
                                break;
                            }
                            batchSkipped++;
                        } else {
                            insertRow(targetConnection, table, row, strategy, idSupplier, clock, jobId);
                            batchInserted++;
                        }
                        batchLastSourceId = row.getLegacyId();
                    }
                    if (failureCode != null) {
                        targetConnection.rollback();
                        transactionActive = false;
                        return failedReport(jobId, failureCode, rows.size(), inserted, skipped,
                                lastSourceId, failureRow);
                    }
                    targetConnection.commit();
                    transactionActive = false;
                    inserted += batchInserted;
                    skipped += batchSkipped;
                    lastSourceId = batchLastSourceId;
                    start = end;
                } catch (SQLException | RuntimeException failure) {
                    if (transactionActive) {
                        try {
                            targetConnection.rollback();
                            transactionActive = false;
                        } catch (SQLException rollbackFailure) {
                            failure.addSuppressed(rollbackFailure);
                            throw failure;
                        }
                    }
                    String code = failure instanceof RuntimeException runtimeFailure
                            ? safeFailureCode(runtimeFailure) : "TARGET_BATCH_WRITE_FAILED";
                    log.warn("Tianshu legacy import batch failed: jobId={} lastSourceId={} code={}",
                            jobId, batchLastSourceId, code);
                    return failedReport(jobId, code, rows.size(), inserted, skipped, lastSourceId, currentRow);
                } finally {
                    if (!transactionActive) targetConnection.setAutoCommit(true);
                }
            }
        }
        log.info("Tianshu legacy import batch completed: jobId={} inserted={} skipped={}", jobId, inserted, skipped);
        return successfulReport(jobId, rows.size(), inserted, skipped, lastSourceId);
    }

    private static Map<String, Object> selectExisting(Connection connection, String table,
                                                       DdcLegacyRowDTO row,
                                                       DdcLegacyTableStrategy strategy) throws SQLException {
        List<String> columns = strategy.targetColumnNames(row.getSourceTable());
        String selectedColumns = "deleted_at, version, " + columns.stream().map(DdcLegacyImportService::identifier)
                .collect(Collectors.joining(", "));
        String sql = "SELECT " + selectedColumns + " FROM " + identifier(table)
                + " WHERE tenant_id = ? AND public_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, row.getTenantId());
            statement.setString(2, row.getPublicId());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return null;
                Map<String, Object> values = new java.util.LinkedHashMap<>();
                values.put("_migration_deleted_at", result.getObject("deleted_at"));
                values.put("_migration_version", result.getObject("version"));
                for (String column : columns) values.put(column, result.getObject(column));
                if (result.next()) throw new SQLException("TIANSHU_MIGRATION_TARGET_IDENTITY_NOT_UNIQUE");
                return values;
            }
        }
    }

    private static boolean sameTargetValues(Map<String, Object> existing, DdcLegacyRowDTO row,
                                            DdcLegacyTableStrategy strategy) {
        if (existing.get("_migration_deleted_at") != null) return false;
        if (!sameValue(existing.get("_migration_version"),
                row.getTechnicalVersion() == null ? 0L : row.getTechnicalVersion())) return false;
        Map<String, Object> targetValues = row.getTargetValues();
        for (String column : strategy.targetColumnNames(row.getSourceTable())) {
            if (!sameValue(existing.get(column), targetValues.get(column))) return false;
        }
        return true;
    }

    private static boolean sameValue(Object actual, Object expected) {
        if (actual == null || expected == null) return actual == expected;
        if (actual instanceof Number actualNumber && expected instanceof Number expectedNumber) {
            return new BigDecimal(actualNumber.toString()).compareTo(new BigDecimal(expectedNumber.toString())) == 0;
        }
        if (expected instanceof Boolean expectedBoolean) {
            if (actual instanceof Number actualNumber) return (actualNumber.intValue() != 0) == expectedBoolean;
            if (actual instanceof String actualText) {
                if ("true".equalsIgnoreCase(actualText) || "1".equals(actualText)) return expectedBoolean;
                if ("false".equalsIgnoreCase(actualText) || "0".equals(actualText)) return !expectedBoolean;
                return false;
            }
        }
        if (expected instanceof LocalDateTime expectedTime) {
            LocalDateTime actualTime = asLocalDateTime(actual);
            return expectedTime.equals(actualTime);
        }
        return expected.toString().equals(actual.toString());
    }

    private static LocalDateTime asLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) return localDateTime;
        if (value instanceof Timestamp timestamp) return timestamp.toLocalDateTime();
        if (value instanceof String text) return LocalDateTime.parse(text.replace(' ', 'T'));
        return null;
    }

    private static void insertRow(Connection connection, String table, DdcLegacyRowDTO row,
                                  DdcLegacyTableStrategy strategy, LongSupplier idSupplier,
                                  Clock clock, String jobId) throws SQLException {
        List<String> businessColumns = strategy.targetColumnNames(row.getSourceTable());
        List<String> columns = new ArrayList<>(List.of("id", "tenant_id", "create_user_id", "create_time",
                "update_user_id", "update_time", "deleted_at", "version", "public_id"));
        columns.addAll(businessColumns);
        String sql = "INSERT INTO " + identifier(table) + " ("
                + columns.stream().map(DdcLegacyImportService::identifier).collect(Collectors.joining(", "))
                + ") VALUES (" + String.join(", ", java.util.Collections.nCopies(columns.size(), "?")) + ")";
        OffsetDateTime auditTime = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.MICROS);
        String actor = "system:migration:" + jobId;
        Map<String, Object> targetValues = row.getTargetValues();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            statement.setLong(index++, positiveId(idSupplier.getAsLong()));
            statement.setLong(index++, row.getTenantId());
            statement.setString(index++, actor);
            statement.setObject(index++, auditTime);
            statement.setString(index++, actor);
            statement.setObject(index++, auditTime);
            statement.setObject(index++, null);
            statement.setLong(index++, row.getTechnicalVersion() == null ? 0L : row.getTechnicalVersion());
            statement.setString(index++, row.getPublicId());
            for (String column : businessColumns) {
                setValue(statement, index++, targetValues.get(column));
            }
            if (statement.executeUpdate() != 1) throw new SQLException("TIANSHU_MIGRATION_INSERT_NOT_APPLIED");
        }
    }

    private static void setValue(PreparedStatement statement, int index, Object value) throws SQLException {
        if (value instanceof LocalDateTime localDateTime) statement.setObject(index, localDateTime);
        else statement.setObject(index, value);
    }

    private static long positiveId(long id) {
        if (id <= 0) throw new IllegalStateException("DISTRIBUTED_ID_INVALID");
        return id;
    }

    private static String identifier(String identifier) {
        if (identifier == null || !identifier.matches("[a-z][a-z0-9_]*")) {
            throw new IllegalArgumentException("TIANSHU_MIGRATION_IDENTIFIER_INVALID");
        }
        return identifier;
    }

    private static String safeFailureCode(RuntimeException failure) {
        String message = failure.getMessage();
        if (message == null) return "TARGET_BATCH_WRITE_FAILED";
        int separator = message.indexOf(':');
        String code = separator < 0 ? message : message.substring(0, separator);
        return code.matches("TIANSHU_[A-Z0-9_]+|DISTRIBUTED_ID_INVALID")
                ? code : "TARGET_BATCH_WRITE_FAILED";
    }

    private static DdcMigrationReportResult successfulReport(String jobId, long rowsRead, long inserted,
                                                             long skipped, String lastSourceId) {
        return DdcMigrationReportResult.builder().mode("IMPORT").jobId(jobId).status("SUCCESS")
                .rowsRead(rowsRead).rowsInserted(inserted).rowsSkipped(skipped)
                .lastSourceId(lastSourceId).build();
    }

    private static DdcMigrationReportResult failedReport(String jobId, String errorCode, long rowsRead,
                                                         long inserted, long skipped, String lastSourceId) {
        return failedReport(jobId, errorCode, rowsRead, inserted, skipped, lastSourceId, null);
    }

    private static DdcMigrationReportResult failedReport(String jobId, String errorCode, long rowsRead,
                                                         long inserted, long skipped, String lastSourceId,
                                                         DdcLegacyRowDTO failureRow) {
        return DdcMigrationReportResult.builder().mode("IMPORT").jobId(jobId).status("FAILED")
                .errorCode(errorCode).rowsRead(rowsRead).rowsInserted(inserted).rowsSkipped(skipped)
                .lastSourceId(lastSourceId).errorStage("IMPORT")
                .errorTable(failureRow == null ? null : failureRow.getSourceTable())
                .errorLegacyId(failureRow == null ? null : failureRow.getLegacyId())
                .errorHash(failureRow == null ? null : failureRow.getSourceHash()).build();
    }

    private static DdcMigrationReportResult successfulDatabaseReport(String mode, String jobId,
                                                                     String sourceFingerprint,
                                                                     String targetRouteFingerprint,
                                                                     long rowsRead, long rowsInserted,
                                                                     long rowsSkipped, String lastSourceId) {
        return databaseReport(mode, jobId, sourceFingerprint, targetRouteFingerprint, "SUCCESS",
                rowsRead, rowsInserted, rowsSkipped, 0, lastSourceId, null, null, Map.of());
    }

    private static void requireMigrationHeader(JsonNode mapping, String targetRouteFingerprint) {
        if (mapping == null || !mapping.isObject()) throw new IllegalArgumentException("TIANSHU_MIGRATION_MAPPING_INVALID");
        String jobId = text(mapping.get("jobId"));
        if (!jobId.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}") || jobId.contains("..")) {
            throw new IllegalArgumentException("TIANSHU_MIGRATION_JOB_ID_INVALID");
        }
        if (targetRouteFingerprint == null || !targetRouteFingerprint.equals(text(mapping.get("targetRouteFingerprint")))) {
            throw new IllegalArgumentException("TIANSHU_MIGRATION_TARGET_ROUTE_MISMATCH");
        }
        if (!text(mapping.get("sourceFingerprint")).matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("TIANSHU_MIGRATION_SOURCE_FINGERPRINT_REQUIRED");
        }
    }

    private static Map<String, String> expectedHistory(JsonNode mapping) {
        JsonNode history = mapping.get("sourceHistoryChecksums");
        if (history == null || !history.isObject() || history.size() != 9) {
            throw new IllegalArgumentException("TIANSHU_MIGRATION_SOURCE_HISTORY_REQUIRED");
        }
        Map<String, String> checksums = new TreeMap<>();
        for (int version = 1; version <= 9; version++) {
            JsonNode checksum = history.get(Integer.toString(version));
            if (checksum == null || checksum.isNull() || checksum.asText().isBlank()) {
                throw new IllegalArgumentException("TIANSHU_MIGRATION_SOURCE_HISTORY_REQUIRED");
            }
            checksums.put(Integer.toString(version), checksum.asText());
        }
        return checksums;
    }

    private static Map<String, String> readFlywayHistory(Connection source) throws SQLException {
        Map<String, String> checksums = new TreeMap<>();
        try (Statement statement = source.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT version, checksum, success FROM flyway_schema_history ORDER BY installed_rank")) {
            while (result.next()) {
                String version = result.getString("version");
                if (version == null) continue;
                if (!result.getBoolean("success")) {
                    throw new IllegalStateException("TIANSHU_MIGRATION_SOURCE_HISTORY_FAILED");
                }
                String checksum = result.getString("checksum");
                if (checksum == null || checksums.putIfAbsent(version, checksum) != null) {
                    throw new IllegalStateException("TIANSHU_MIGRATION_SOURCE_HISTORY_INVALID");
                }
            }
        }
        return checksums;
    }

    private static String sourceFingerprint(Connection source, Map<String, String> history) throws SQLException {
        DatabaseMetaData metadata = source.getMetaData();
        String catalog = source.getCatalog();
        String schema = null;
        try {
            schema = source.getSchema();
        } catch (SQLException ignored) {
            // Some JDBC drivers do not expose the current schema; the catalog and URL remain part of identity.
        }
        StringBuilder value = new StringBuilder()
                .append(metadata.getDatabaseProductName()).append('\n')
                .append(metadata.getDatabaseProductVersion()).append('\n')
                .append(Objects.toString(catalog, "")).append('\n')
                .append(Objects.toString(schema, "")).append('\n')
                .append(sanitizeJdbcUrl(metadata.getURL())).append('\n');
        history.forEach((version, checksum) -> value.append(version).append(':').append(checksum).append('\n'));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA256_UNAVAILABLE", failure);
        }
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA256_UNAVAILABLE", failure);
        }
    }

    private static void updateSourceFingerprint(MessageDigest digest, DdcLegacyRowDTO row) {
        digest.update(row.getSourceTable().getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
        digest.update(Long.toString(row.getTenantId()).getBytes(StandardCharsets.US_ASCII));
        digest.update((byte) 0);
        digest.update(row.getLegacyId().getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
        digest.update(row.getSourceHash().getBytes(StandardCharsets.US_ASCII));
        digest.update((byte) '\n');
    }

    private static String completeSourceFingerprint(String sourceIdentityFingerprint, byte[] rowDigest) {
        MessageDigest digest = sha256Digest();
        digest.update(sourceIdentityFingerprint.getBytes(StandardCharsets.US_ASCII));
        digest.update((byte) 0);
        digest.update(rowDigest);
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String sanitizeJdbcUrl(String url) {
        if (url == null) return "";
        String sanitized = url.replaceAll("(?i)(//)[^/@]+@", "$1<redacted>@");
        int query = sanitized.indexOf('?');
        if (query < 0) return sanitized;
        String base = sanitized.substring(0, query);
        String safeQuery = java.util.Arrays.stream(sanitized.substring(query + 1).split("&"))
                .filter(parameter -> {
                    String key = parameter.split("=", 2)[0].toLowerCase(Locale.ROOT);
                    return !Set.of("user", "username", "password", "passwd", "token").contains(key);
                })
                .sorted()
                .collect(Collectors.joining("&"));
        return safeQuery.isEmpty() ? base : base + "?" + safeQuery;
    }

    private static Set<Long> registeredTenantIds(JsonNode mapping) {
        JsonNode tenants = mapping.get("registeredTenants");
        if (tenants == null || !tenants.isArray() || tenants.isEmpty()) {
            throw new IllegalArgumentException("TIANSHU_MIGRATION_REGISTERED_TENANTS_REQUIRED");
        }
        Set<Long> ids = new HashSet<>();
        Set<String> identityTenantIds = new HashSet<>();
        for (JsonNode tenant : tenants) {
            String identityTenantId = text(tenant.get("identityTenantId"));
            long tenantId = longValue(tenant.get("tenantId"));
            if (identityTenantId.isBlank() || tenantId <= 0 || !identityTenantIds.add(identityTenantId)) {
                throw new IllegalArgumentException("TIANSHU_MIGRATION_REGISTERED_TENANT_INVALID");
            }
            ids.add(tenantId);
        }
        return Set.copyOf(ids);
    }

    private static void validateTenantMappingShape(JsonNode mapping, DdcLegacyTableStrategy strategy,
                                                   Set<Long> registeredTenants) {
        strategy.validateTenantMappings(mapping.get("tenantMappings"), registeredTenants);
    }

    private static void verifyTenantMappingCoverage(JsonNode mapping, String table, Set<String> sourceIds) {
        Set<String> mappedIds = new HashSet<>();
        mapping.path("tenantMappings").path(table).fieldNames().forEachRemaining(mappedIds::add);
        if (!mappedIds.equals(sourceIds)) {
            throw new IllegalStateException("TIANSHU_MIGRATION_TENANT_MAPPING_COVERAGE_MISMATCH: " + table);
        }
    }

    private static void validateParentTenantAssignments(JsonNode mapping,
                                                        DdcLegacyTableStrategy strategy,
                                                        List<DdcLegacyParentReferenceVO> parents,
                                                        Long childTenantId,
                                                        Set<Long> registeredTenants) {
        for (DdcLegacyParentReferenceVO parent : parents) {
            if (!strategy.tenantIds(mapping.path("tenantMappings"), parent.sourceTable(),
                    parent.legacyPublicId(), registeredTenants).contains(childTenantId)) {
                throw new IllegalStateException("TIANSHU_MIGRATION_CROSS_TENANT_PARENT");
            }
        }
    }

    private static List<Map<String, Object>> readSourcePage(Connection source, String table,
                                                             String afterId, int pageSize) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(identifier(table));
        if (afterId != null) sql.append(" WHERE id > ?");
        sql.append(" ORDER BY id LIMIT ?");
        try (PreparedStatement statement = source.prepareStatement(sql.toString())) {
            int parameter = 1;
            if (afterId != null) statement.setString(parameter++, afterId);
            statement.setInt(parameter, pageSize);
            statement.setFetchSize(pageSize);
            try (ResultSet result = statement.executeQuery()) {
                ResultSetMetaData metadata = result.getMetaData();
                List<Map<String, Object>> rows = new ArrayList<>();
                while (result.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int column = 1; column <= metadata.getColumnCount(); column++) {
                        String name = metadata.getColumnLabel(column).toLowerCase(Locale.ROOT);
                        row.put(name, normalizeJdbcValue(result.getObject(column)));
                    }
                    rows.add(row);
                }
                return rows;
            }
        }
    }

    private static void validateSourceColumns(Connection source, String table, List<String> expectedColumns)
            throws SQLException {
        try (Statement statement = source.createStatement();
             ResultSet result = statement.executeQuery("SELECT * FROM " + identifier(table) + " WHERE 1 = 0")) {
            ResultSetMetaData metadata = result.getMetaData();
            Set<String> actual = new HashSet<>();
            for (int column = 1; column <= metadata.getColumnCount(); column++) {
                actual.add(metadata.getColumnLabel(column).toLowerCase(Locale.ROOT));
            }
            if (!actual.equals(Set.copyOf(expectedColumns))) {
                throw new IllegalStateException("TIANSHU_MIGRATION_SOURCE_COLUMN_SET_MISMATCH: " + table);
            }
        }
    }

    private static Object normalizeJdbcValue(Object value) {
        if (value instanceof Timestamp timestamp) return timestamp.toLocalDateTime();
        return value;
    }

    private static String sourceId(Map<String, Object> row) {
        Object value = row.get("id");
        if (value == null || value.toString().isBlank()) {
            throw new IllegalStateException("TIANSHU_LEGACY_PUBLIC_ID_REQUIRED");
        }
        return value.toString();
    }

    private static long longValue(JsonNode node) {
        if (node == null || node.isNull() || !(node.isIntegralNumber() || node.isTextual())) {
            throw new IllegalArgumentException("TIANSHU_MIGRATION_TENANT_ID_INVALID");
        }
        try {
            long value = node.isIntegralNumber() ? node.longValue() : Long.parseLong(node.asText());
            if (value <= 0) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("TIANSHU_MIGRATION_TENANT_ID_INVALID");
        }
    }

    private static String text(JsonNode node) {
        return node == null || !node.isValueNode() || node.isNull() ? "" : node.asText();
    }

    private static String tableTenantKey(String table, long tenantId) {
        return table + ":" + tenantId;
    }

    private static List<DdcLegacyRowDTO> mapRowsForTenants(Connection target, JsonNode mapping,
                                                           DdcLegacyTableStrategy strategy, String table,
                                                           Map<String, Object> sourceRow, List<Long> tenantIds,
                                                           Set<Long> registeredTenants,
                                                           boolean requireParents) throws SQLException {
        List<DdcLegacyRowDTO> rows = new ArrayList<>();
        for (Long tenantId : tenantIds) {
            List<DdcLegacyParentReferenceVO> references = strategy.parentReferences(table, sourceRow);
            validateParentTenantAssignments(mapping, strategy, references, tenantId, registeredTenants);
            DdcLegacyParentResolutionBO resolution = resolveTargetParentIds(
                    target, table, sourceRow, tenantId, strategy, requireParents);
            rows.add(strategy.mapSourceRow(table, sourceRow, tenantId, resolution.targetIds()));
        }
        return rows;
    }

    private static DdcLegacyParentResolutionBO resolveTargetParentIds(Connection target, String table,
                                                                       Map<String, Object> sourceRow,
                                                                       Long tenantId,
                                                                       DdcLegacyTableStrategy strategy,
                                                                       boolean requireParents) throws SQLException {
        Map<String, Long> ids = new LinkedHashMap<>();
        boolean missingParent = false;
        for (DdcLegacyParentReferenceVO reference : strategy.parentReferences(table, sourceRow)) {
            String key = reference.sourceTable() + ":" + reference.legacyPublicId();
            Long targetId = targetIdByPublicId(target, strategy.targetTableName(reference.sourceTable()),
                    tenantId, reference.legacyPublicId());
            if (targetId == null) {
                missingParent = true;
                if (requireParents) throw new IllegalStateException("TIANSHU_LEGACY_PARENT_TARGET_MISSING");
                targetId = 1L;
            }
            ids.put(key, targetId);
        }
        return new DdcLegacyParentResolutionBO(ids, missingParent);
    }

    private static Long targetIdByPublicId(Connection target, String table, Long tenantId,
                                           String publicId) throws SQLException {
        String sql = "SELECT id FROM " + identifier(table) + " WHERE tenant_id = ? AND public_id = ?";
        try (PreparedStatement statement = target.prepareStatement(sql)) {
            statement.setLong(1, tenantId);
            statement.setString(2, publicId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return null;
                long id = result.getLong(1);
                if (result.next()) throw new SQLException("TIANSHU_MIGRATION_PARENT_ID_NOT_UNIQUE");
                return id;
            }
        }
    }

    private static long countRows(Connection target, String table) throws SQLException {
        try (Statement statement = target.createStatement();
             ResultSet result = statement.executeQuery("SELECT count(*) FROM " + identifier(table))) {
            if (!result.next()) throw new SQLException("TIANSHU_MIGRATION_ROW_COUNT_UNAVAILABLE");
            return result.getLong(1);
        }
    }

    private static void validateTargetRowsHaveSourceMapping(Connection target, JsonNode mapping,
                                                            DdcLegacyTableStrategy strategy,
                                                            Set<Long> registeredTenants) throws SQLException {
        for (String sourceTable : strategy.orderedLegacyTableNames()) {
            String targetTable = strategy.targetTableName(sourceTable);
            try (Statement statement = target.createStatement();
                 ResultSet result = statement.executeQuery(
                         "SELECT tenant_id, public_id FROM " + identifier(targetTable))) {
                while (result.next()) {
                    long tenantId = result.getLong("tenant_id");
                    String publicId = result.getString("public_id");
                    if (!registeredTenants.contains(tenantId)
                            || !strategy.tenantIds(mapping.path("tenantMappings"), sourceTable,
                            publicId, registeredTenants).contains(tenantId)) {
                        throw new IllegalStateException("TIANSHU_MIGRATION_TARGET_ROW_NOT_IN_MAPPING: "
                                + sourceTable);
                    }
                }
            }
        }
    }

    private record DdcLegacyParentResolutionBO(Map<String, Long> targetIds, boolean missingParent) {
        private DdcLegacyParentResolutionBO {
            targetIds = Map.copyOf(targetIds);
        }
    }

    private static Map<String, DdcMigrationCheckpointBO> readCheckpoints(Path reportDirectory, String jobId,
                                                                         String sourceFingerprint)
            throws IOException {
        Path checkpointFile = reportDirectory.resolve(jobId + ".checkpoint.json");
        if (!Files.exists(checkpointFile, LinkOption.NOFOLLOW_LINKS)) return Map.of();
        if (Files.isSymbolicLink(checkpointFile)
                || !Files.isRegularFile(checkpointFile, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("TIANSHU_MIGRATION_CHECKPOINT_INVALID");
        }
        List<DdcMigrationCheckpointBO> checkpoints = Jackson2ObjectMapperBuilder.json().build().readValue(
                checkpointFile.toFile(), new TypeReference<>() { });
        Map<String, DdcMigrationCheckpointBO> byTable = new LinkedHashMap<>();
        for (DdcMigrationCheckpointBO checkpoint : checkpoints) {
            if (!jobId.equals(checkpoint.jobId()) || !sourceFingerprint.equals(checkpoint.sourceHash())
                    || checkpoint.lastOriginalId() == null || checkpoint.lastOriginalId().isBlank()
                    || checkpoint.rowCount() <= 0 || byTable.putIfAbsent(checkpoint.table(), checkpoint) != null) {
                throw new IllegalStateException("TIANSHU_MIGRATION_CHECKPOINT_INVALID");
            }
        }
        return byTable;
    }

    private static void writeCheckpoint(Path reportDirectory, String jobId, String sourceFingerprint,
                                        String table, String lastOriginalId, long rowCount) throws IOException {
        Map<String, DdcMigrationCheckpointBO> checkpoints = new TreeMap<>(
                readCheckpoints(reportDirectory, jobId, sourceFingerprint));
        checkpoints.put(table, new DdcMigrationCheckpointBO(jobId, sourceFingerprint, table,
                lastOriginalId, rowCount));
        Path target = reportDirectory.resolve(jobId + ".checkpoint.json");
        Path temporary = Files.createTempFile(reportDirectory, jobId + ".checkpoint.", ".tmp");
        try {
            Jackson2ObjectMapperBuilder.json().build().writeValue(temporary.toFile(), checkpoints.values());
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException failure) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private long verifyCheckpointPrefix(Connection source, Connection target, JsonNode mapping,
                                        DdcLegacyTableStrategy strategy, String table,
                                        DdcMigrationCheckpointBO checkpoint, String sourceFingerprint,
                                        String targetRouteFingerprint,
                                        Set<Long> registeredTenants) throws SQLException {
        if (!sourceFingerprint.equals(checkpoint.sourceHash())
                || !targetRouteFingerprint.equals(text(mapping.get("targetRouteFingerprint")))) {
            throw new IllegalStateException("TIANSHU_MIGRATION_CHECKPOINT_FINGERPRINT_MISMATCH");
        }
        long sourceRows = 0;
        long targetRows = 0;
        String afterId = null;
        boolean matchedCheckpoint = false;
        while (!matchedCheckpoint) {
            List<Map<String, Object>> page = readSourcePage(source, table, afterId, TARGET_BATCH_SIZE);
            if (page.isEmpty()) break;
            for (Map<String, Object> sourceRow : page) {
                String legacyId = sourceId(sourceRow);
                List<Long> tenants = strategy.tenantIds(
                        mapping.path("tenantMappings"), table, legacyId, registeredTenants);
                List<DdcLegacyRowDTO> rows = mapRowsForTenants(target, mapping, strategy, table, sourceRow,
                        tenants, registeredTenants, true);
                for (DdcLegacyRowDTO row : rows) {
                    Map<String, Object> existing = selectExisting(target, row.getTargetTable(), row, strategy);
                    if (existing == null || !sameTargetValues(existing, row, strategy)) {
                        throw new IllegalStateException("TIANSHU_MIGRATION_CHECKPOINT_TARGET_MISMATCH");
                    }
                    targetRows++;
                }
                sourceRows++;
                afterId = legacyId;
                if (legacyId.equals(checkpoint.lastOriginalId())) {
                    matchedCheckpoint = true;
                    break;
                }
            }
        }
        if (!matchedCheckpoint || sourceRows != checkpoint.rowCount()) {
            throw new IllegalStateException("TIANSHU_MIGRATION_CHECKPOINT_SOURCE_MISMATCH");
        }
        return targetRows;
    }

    private static DdcMigrationReportResult databaseReport(String mode, String jobId, String sourceFingerprint,
                                                           String targetRouteFingerprint, String status,
                                                           long rowsRead, long rowsInserted, long rowsSkipped,
                                                           long differenceCount, String lastSourceId,
                                                           String errorCode, String errorStage,
                                                           Map<String, Long> rowsByTable) {
        return databaseReport(mode, jobId, sourceFingerprint, targetRouteFingerprint, status,
                rowsRead, rowsInserted, rowsSkipped, differenceCount, lastSourceId,
                errorCode, errorStage, null, null, null, rowsByTable);
    }

    private static DdcMigrationReportResult databaseReport(String mode, String jobId, String sourceFingerprint,
                                                           String targetRouteFingerprint, String status,
                                                           long rowsRead, long rowsInserted, long rowsSkipped,
                                                           long differenceCount, String lastSourceId,
                                                           String errorCode, String errorStage, String errorTable,
                                                           String errorLegacyId, String errorHash,
                                                           Map<String, Long> rowsByTable) {
        return DdcMigrationReportResult.builder().mode(mode).jobId(jobId)
                .sourceFingerprint(sourceFingerprint).targetRouteFingerprint(targetRouteFingerprint)
                .status(status).rowsRead(rowsRead).rowsInserted(rowsInserted).rowsSkipped(rowsSkipped)
                .differenceCount(differenceCount).lastSourceId(lastSourceId).errorCode(errorCode)
                .errorStage(errorStage).errorTable(errorTable).errorLegacyId(errorLegacyId).errorHash(errorHash)
                .rowsByTable(Map.copyOf(rowsByTable)).build();
    }

    private static DdcMigrationReportResult failedDatabaseReport(String mode, String jobId,
                                                                 String sourceFingerprint,
                                                                 String targetRouteFingerprint,
                                                                 String errorCode, String errorStage,
                                                                 String errorLegacyId, long rowsRead,
                                                                 long rowsInserted, long rowsSkipped,
                                                                 String lastSourceId) {
        return failedDatabaseReport(mode, jobId, sourceFingerprint, targetRouteFingerprint,
                errorCode, errorStage, null, errorLegacyId, null,
                rowsRead, rowsInserted, rowsSkipped, lastSourceId);
    }

    private static DdcMigrationReportResult failedDatabaseReport(String mode, String jobId,
                                                                 String sourceFingerprint,
                                                                 String targetRouteFingerprint,
                                                                 String errorCode, String errorStage,
                                                                 String errorTable, String errorLegacyId,
                                                                 String errorHash, long rowsRead,
                                                                 long rowsInserted, long rowsSkipped,
                                                                 String lastSourceId) {
        return failedDatabaseReport(mode, jobId, sourceFingerprint, targetRouteFingerprint, errorCode,
                errorStage, errorTable, errorLegacyId, errorHash, rowsRead, rowsInserted, rowsSkipped,
                lastSourceId, Map.of());
    }

    private static DdcMigrationReportResult failedDatabaseReport(String mode, String jobId,
                                                                 String sourceFingerprint,
                                                                 String targetRouteFingerprint,
                                                                 String errorCode, String errorStage,
                                                                 String errorTable, String errorLegacyId,
                                                                 String errorHash, long rowsRead,
                                                                 long rowsInserted, long rowsSkipped,
                                                                 String lastSourceId,
                                                                 Map<String, Long> rowsByTable) {
        return databaseReport(mode, jobId, sourceFingerprint, targetRouteFingerprint, "FAILED",
                rowsRead, rowsInserted, rowsSkipped, 1, lastSourceId, errorCode, errorStage,
                errorTable, errorLegacyId, errorHash, Map.copyOf(rowsByTable));
    }

    private static DdcMigrationReportResult withCheckpoint(DdcMigrationReportResult report,
                                                           Path reportDirectory, String jobId) {
        if (reportDirectory == null || jobId == null) return report;
        Path checkpointFile = reportDirectory.resolve(jobId + ".checkpoint.json");
        if (Files.isRegularFile(checkpointFile, LinkOption.NOFOLLOW_LINKS)) {
            report.setCheckpoint(checkpointFile.getFileName().toString());
        }
        return report;
    }

    private static DdcMigrationReportResult withMode(DdcMigrationReportResult report, String mode) {
        return databaseReport(mode, report.getJobId(), report.getSourceFingerprint(),
                report.getTargetRouteFingerprint(), report.getStatus(), report.getRowsRead(),
                report.getRowsInserted(), report.getRowsSkipped(), report.getDifferenceCount(),
                report.getLastSourceId(), report.getErrorCode(), report.getErrorStage(),
                report.getErrorTable(), report.getErrorLegacyId(), report.getErrorHash(), report.getRowsByTable());
    }

    private record DdcMigrationCheckpointBO(String jobId, String sourceHash, String table,
                                            String lastOriginalId, long rowCount) {
    }
}
