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
 * RPC-005 回执：新草稿修订、草稿摘要以及它是否等于当前发布版本。
 * / RPC-005 receipt: the new draft revision, its digest and whether it equals the current published version.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcAiRevisionResult {

    @NotNull
    @Positive
    private Long resourceId;

    @NotNull
    @Min(0)
    private Long resourceRevision;

    @NotBlank
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String draftSha256;

    @NotNull
    private Boolean draftMatchesCurrent;
}
