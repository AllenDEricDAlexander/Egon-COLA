package top.egon.cola.component.tianshu.admin.migration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** A normalized, tenant-bound legacy row ready for one target batch. */
@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Accessors(chain = true)
public class DdcLegacyRowDTO {

    @NotBlank
    @Size(max = 64)
    private String sourceTable;

    @NotBlank
    @Size(max = 96)
    @Pattern(regexp = "ddc_[a-z0-9_]+_t0")
    private String targetTable;

    @NotBlank
    @Size(max = 64)
    private String legacyId;

    @NotNull
    @Positive
    private Long tenantId;

    @NotBlank
    @Size(max = 64)
    private String publicId;

    @NotBlank
    @Pattern(regexp = "[0-9a-f]{64}")
    private String sourceHash;

    @NotNull
    private Map<String, Object> targetValues = new LinkedHashMap<>();

    @NotNull
    @PositiveOrZero
    private Long technicalVersion = 0L;

    public Map<String, Object> getTargetValues() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(targetValues));
    }

    public DdcLegacyRowDTO setTargetValues(Map<String, Object> targetValues) {
        this.targetValues = targetValues == null ? new LinkedHashMap<>() : new LinkedHashMap<>(targetValues);
        return this;
    }

    @Builder(access = AccessLevel.PUBLIC)
    private static DdcLegacyRowDTO create(String sourceTable, String targetTable, String legacyId, Long tenantId,
                                          String publicId, String sourceHash, Map<String, Object> targetValues,
                                          Long technicalVersion) {
        Map<String, Object> values = targetValues == null ? new LinkedHashMap<>() : new LinkedHashMap<>(targetValues);
        return new DdcLegacyRowDTO(sourceTable, targetTable, legacyId, tenantId, publicId, sourceHash, values,
                technicalVersion);
    }

    public static class DdcLegacyRowDTOBuilder {

        public DdcLegacyRowDTOBuilder targetValues(Map<String, Object> targetValues) {
            this.targetValues = targetValues == null ? new LinkedHashMap<>() : new LinkedHashMap<>(targetValues);
            return this;
        }
    }
}
