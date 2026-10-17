package top.egon.cola.component.tianshu.admin.model.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiOutcomePersistenceEnum;

/** API-017 使用样本分页查询。 / API-017 usage-feedback page query. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder(toBuilder = true)
public class DdcAiUsageFeedbackPageQuery {

    @NotNull(groups = EgonColaModelValidationGroups.Query.class)
    @Positive(groups = EgonColaModelValidationGroups.Query.class)
    private Long resourceId;

    @Positive(groups = EgonColaModelValidationGroups.Query.class)
    private Long versionNo;

    private DdcAiOutcomePersistenceEnum outcome;

    /** 分数来源过滤。 / Score-source filter. */
    @Pattern(regexp = "^(EXPLICIT|JUDGE|NONE)$", groups = EgonColaModelValidationGroups.Query.class)
    private String scoreSource;

    @Min(value = 0, groups = EgonColaModelValidationGroups.Query.class)
    @Max(value = 100, groups = EgonColaModelValidationGroups.Query.class)
    private Integer maxScore;

    @NotNull(groups = EgonColaModelValidationGroups.Query.class)
    @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class)
    private Integer pageNo;

    @NotNull(groups = EgonColaModelValidationGroups.Query.class)
    @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class)
    @Max(value = 100, groups = EgonColaModelValidationGroups.Query.class)
    private Integer pageSize;
}
