package top.egon.cola.component.tianshu.admin.model.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiRunStatePersistenceEnum;

/** API-015 优化运行分页查询。 / API-015 optimization-run page query. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder(toBuilder = true)
public class DdcAiOptimizationRunPageQuery {

    @NotNull(groups = EgonColaModelValidationGroups.Query.class)
    @Positive(groups = EgonColaModelValidationGroups.Query.class)
    private Long resourceId;

    private DdcAiRunStatePersistenceEnum state;

    @NotNull(groups = EgonColaModelValidationGroups.Query.class)
    @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class)
    private Integer pageNo;

    @NotNull(groups = EgonColaModelValidationGroups.Query.class)
    @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class)
    @Max(value = 100, groups = EgonColaModelValidationGroups.Query.class)
    private Integer pageSize;
}
