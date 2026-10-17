package top.egon.cola.component.tianshu.model.ai;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 发布已上传 Skill 草稿的请求；requestId 保证重试幂等。
 * / Request to publish an uploaded Skill draft; requestId keeps retries idempotent.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder(toBuilder = true)
public class DdcAiSkillPublishDTO {

    @NotBlank
    @Size(max = 128)
    private String namespaceCode;

    @NotBlank
    @Size(max = 128)
    private String resourceKey;

    @NotNull
    @Positive
    private Long resourceId;

    @NotNull
    @Min(0)
    private Long expectedRevision;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$")
    private String requestId;

    @NotNull
    @Size(max = 512)
    private String changeNote;
}
