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
 * RPC-004 版本头：未发布时版本三字段为 null。
 * / RPC-004 version head: the three version fields are null while unpublished.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class DdcAiVersionHeadResult {

    @NotNull
    @Positive
    private Long resourceId;

    @NotBlank
    private String resourceKey;

    @Positive
    private Long versionId;

    @Positive
    private Long versionNo;

    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String contentSha256;

    @NotNull
    @Min(0)
    private Long resourceRevision;

    @NotNull
    private Boolean published;
}
