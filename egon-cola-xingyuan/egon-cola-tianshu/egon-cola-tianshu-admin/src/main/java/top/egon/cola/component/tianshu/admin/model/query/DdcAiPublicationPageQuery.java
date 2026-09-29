package top.egon.cola.component.tianshu.admin.model.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
public class DdcAiPublicationPageQuery {

    @NotNull(groups = EgonColaModelValidationGroups.Query.class)
    @Positive(groups = EgonColaModelValidationGroups.Query.class)
    private Long resourceId;

    @Size(min = 16, max = 64, groups = EgonColaModelValidationGroups.Query.class)
    @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$", groups = EgonColaModelValidationGroups.Query.class)
    private String requestId;

    @NotNull(groups = EgonColaModelValidationGroups.Query.class)
    @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class)
    private Integer pageNo;

    @NotNull(groups = EgonColaModelValidationGroups.Query.class)
    @Min(value = 1, groups = EgonColaModelValidationGroups.Query.class)
    @Max(value = 100, groups = EgonColaModelValidationGroups.Query.class)
    private Integer pageSize;
}
