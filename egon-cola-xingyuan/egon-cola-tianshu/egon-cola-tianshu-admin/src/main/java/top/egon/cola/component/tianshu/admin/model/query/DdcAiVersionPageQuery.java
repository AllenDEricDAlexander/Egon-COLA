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

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcAiVersionPageQuery {

    @NotNull(groups = EgonColaModelValidationGroups.Query.class)
    @Positive(groups = EgonColaModelValidationGroups.Query.class)
    private Long resourceId;

    @NotNull(groups = EgonColaModelValidationGroups.Query.class)
    @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class)
    private Integer pageNo;

    @NotNull(groups = EgonColaModelValidationGroups.Query.class)
    @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class)
    @Max(value = 100, groups = EgonColaModelValidationGroups.Query.class)
    private Integer pageSize;
}
