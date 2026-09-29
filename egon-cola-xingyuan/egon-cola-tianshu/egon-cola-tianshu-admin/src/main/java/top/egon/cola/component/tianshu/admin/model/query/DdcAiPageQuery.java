package top.egon.cola.component.tianshu.admin.model.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiScopeDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcAiPageQuery {

    @NotBlank
    @Size(max = 128)
    @Pattern(regexp = "^[^\\p{Cntrl}]+$")
    private String bizCode;

    @NotBlank
    @Size(max = 128)
    @Pattern(regexp = "^[^\\p{Cntrl}]+$")
    private String namespaceCode;

    @NotBlank
    @Size(max = 32)
    @Pattern(regexp = "^[^\\p{Cntrl}]+$")
    private String env;

    @NotBlank
    @Size(max = 128)
    @Pattern(regexp = "^[^\\p{Cntrl}]+$")
    private String appCode;

    @NotNull
    private DdcAiKindEnum kind;

    @Size(max = 128)
    private String resourceKeyPrefix;

    @Min(1)
    @Builder.Default
    private int pageNo = 1;

    @Min(1)
    @Max(100)
    @Builder.Default
    private int pageSize = 20;

    public DdcAiScopeDTO toScope() {
        return new DdcAiScopeDTO(bizCode, namespaceCode, env, appCode);
    }
}
