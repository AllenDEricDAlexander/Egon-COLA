package top.egon.cola.component.yuheng.llm.proxy.domain.vo;

import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmModelKindEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmProtocolEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmDeploymentEnum;
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
import java.util.List;

/**
 * 中文说明：模型策略的最小安全投影；不包含供应商、渠道地址、密钥或授权主体列表。
 * English summary: Minimal model policy projection without provider endpoints, secrets or subject lists.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class LlmModelPolicyVO {
    @NotBlank
    @Pattern(regexp = "[a-z0-9][a-z0-9-]{0,63}")
    private String modelKey;
    @NotNull
    private LlmModelKindEnum kind;
    @NotNull
    @Size(min = 1, max = 4)
    private List<@NotNull LlmProtocolEnum> protocols;
    @NotNull
    @Min(1)
    private Long revision;
    @Min(1)
    @Max(16000)
    private Integer dimensions;
    @Size(max = 128)
    private String embeddingSpaceId;
    @NotNull
    private Boolean allRoutesEnabled;
    @NotNull
    @Size(max = 2)
    private List<@NotNull LlmDeploymentEnum> deployments;
}
