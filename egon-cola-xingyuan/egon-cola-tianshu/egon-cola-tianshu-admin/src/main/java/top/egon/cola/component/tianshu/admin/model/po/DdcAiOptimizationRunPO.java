package top.egon.cola.component.tianshu.admin.model.po;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.common.mybatis.model.EgonModel;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiRunErrorCodeEnum;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiRunStatePersistenceEnum;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiRunTriggerPersistenceEnum;
import java.math.BigDecimal;
import java.time.Instant;

/** 优化运行持久化对象。 / Persistence object of one optimization run. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@TableName(value = "ddc_ai_optimization_run", autoResultMap = true)
public class DdcAiOptimizationRunPO extends EgonModel<DdcAiOptimizationRunPO> {

    @TableField(value = "resource_id", updateStrategy = FieldStrategy.NEVER)
    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    @Positive(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private Long resourceId;

    @TableField(value = "policy_id", updateStrategy = FieldStrategy.NEVER)
    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    @Positive(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private Long policyId;

    @TableField(value = "run_no", updateStrategy = FieldStrategy.NEVER)
    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    @Positive(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private Long runNo;

    @TableField(value = "iteration_no", updateStrategy = FieldStrategy.NEVER)
    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    @Min(value = 1, groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private Short iterationNo;

    @TableField(value = "\"trigger\"", updateStrategy = FieldStrategy.NEVER)
    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private DdcAiRunTriggerPersistenceEnum trigger;

    @TableField(value = "request_id", updateStrategy = FieldStrategy.NEVER)
    @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$", groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private String requestId;

    @TableField(value = "request_sha256", updateStrategy = FieldStrategy.NEVER)
    @Pattern(regexp = "^[0-9a-f]{64}$", groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private String requestSha256;

    @TableField(value = "state")
    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private DdcAiRunStatePersistenceEnum state;

    @TableField(value = "base_version_id", updateStrategy = FieldStrategy.NEVER)
    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    @Positive(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private Long baseVersionId;

    @TableField(value = "sample_count", updateStrategy = FieldStrategy.NEVER)
    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    @Min(value = 0, groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private Integer sampleCount;

    @TableField(value = "average_score", updateStrategy = FieldStrategy.NEVER)
    @DecimalMin(value = "0", groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    @DecimalMax(value = "100", groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private BigDecimal averageScore;

    @TableField(value = "threshold_snapshot", updateStrategy = FieldStrategy.NEVER)
    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    @Min(value = 1, groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    @Max(value = 100, groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private Short thresholdSnapshot;

    @TableField(value = "window_start", updateStrategy = FieldStrategy.NEVER)
    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private Instant windowStart;

    @TableField(value = "window_end", updateStrategy = FieldStrategy.NEVER)
    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private Instant windowEnd;

    @TableField(value = "model_alias", updateStrategy = FieldStrategy.NEVER)
    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,63}$", groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private String modelAlias;

    @TableField(value = "prompt_sha256", updateStrategy = FieldStrategy.ALWAYS)
    @Pattern(regexp = "^[0-9a-f]{64}$", groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private String promptSha256;

    @TableField(value = "output_sha256", updateStrategy = FieldStrategy.ALWAYS)
    @Pattern(regexp = "^[0-9a-f]{64}$", groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private String outputSha256;

    @TableField(value = "prompt_tokens", updateStrategy = FieldStrategy.ALWAYS)
    @Min(value = 0, groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private Integer promptTokens;

    @TableField(value = "completion_tokens", updateStrategy = FieldStrategy.ALWAYS)
    @Min(value = 0, groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private Integer completionTokens;

    @TableField(value = "produced_version_id", updateStrategy = FieldStrategy.ALWAYS)
    @Positive(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private Long producedVersionId;

    @TableField(value = "publication_id", updateStrategy = FieldStrategy.ALWAYS)
    @Positive(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private Long publicationId;

    @TableField(value = "auto_published")
    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private Boolean autoPublished;

    @TableField(value = "error_code", updateStrategy = FieldStrategy.ALWAYS)
    private DdcAiRunErrorCodeEnum errorCode;

    @TableField(value = "error_message", updateStrategy = FieldStrategy.ALWAYS)
    @Size(max = 512, groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private String errorMessage;

    @TableField(value = "note", updateStrategy = FieldStrategy.NEVER)
    @NotNull(groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    @Size(max = 512, groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private String note;

    @TableField(value = "lease_owner", updateStrategy = FieldStrategy.ALWAYS)
    @Size(max = 128, groups = {EgonColaModelValidationGroups.Insert.class, EgonColaModelValidationGroups.Persisted.class})
    private String leaseOwner;

    @TableField(value = "lease_until", updateStrategy = FieldStrategy.ALWAYS)
    private Instant leaseUntil;

    @TableField(value = "started_at", updateStrategy = FieldStrategy.ALWAYS)
    private Instant startedAt;

    @TableField(value = "finished_at", updateStrategy = FieldStrategy.ALWAYS)
    private Instant finishedAt;

    /** 基线版本号（连接得到）。 / Base version number (joined). */
    @JsonIgnore
    @TableField(exist = false)
    private Long baseVersionNo;

    /** 产物版本号（连接得到）。 / Produced version number (joined). */
    @JsonIgnore
    @TableField(exist = false)
    private Long producedVersionNo;
}
