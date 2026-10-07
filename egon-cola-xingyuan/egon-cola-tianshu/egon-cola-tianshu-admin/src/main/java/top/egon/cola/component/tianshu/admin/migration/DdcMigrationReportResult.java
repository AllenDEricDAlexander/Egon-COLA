package top.egon.cola.component.tianshu.admin.migration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Safe, resumable summary of one explicit offline migration mode. */
@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Accessors(chain = true)
@Builder
public class DdcMigrationReportResult {

    @NotBlank
    private String mode;

    @NotBlank
    private String jobId;

    private String sourceFingerprint;
    private String targetRouteFingerprint;

    @NotBlank
    private String status;

    @PositiveOrZero
    private long rowsRead;

    @PositiveOrZero
    private long rowsInserted;

    @PositiveOrZero
    private long rowsSkipped;

    @PositiveOrZero
    private long differenceCount;

    private String lastSourceId;
    private String checkpoint;
    private String errorCode;
    private String errorStage;
    private String errorTable;
    private String errorLegacyId;
    private String errorHash;
    @Builder.Default
    private Map<String, Long> rowsByTable = new LinkedHashMap<>();

    public Map<String, Long> getRowsByTable() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(rowsByTable));
    }

    public DdcMigrationReportResult setRowsByTable(Map<String, Long> rowsByTable) {
        this.rowsByTable = rowsByTable == null ? new LinkedHashMap<>() : new LinkedHashMap<>(rowsByTable);
        return this;
    }
}
