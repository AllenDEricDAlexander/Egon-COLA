package top.egon.cola.component.tianshu.admin.model.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * RPC-006 回执：发布回执加上目标版本的内容摘要，调用方无需额外的读权限。
 * / RPC-006 receipt: the publication receipt plus the target version digest, so the caller needs no extra read
 * permission.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcAiPublicationReceiptResult {

    @NotNull
    @Positive
    private Long publicationId;

    @NotNull
    @Positive
    private Long resourceId;

    @NotNull
    @Positive
    private Long targetVersionId;

    @NotNull
    @Positive
    private Long targetVersionNo;

    @NotNull
    @Min(0)
    private Long resourceRevision;

    @NotBlank
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String contentSha256;
}
