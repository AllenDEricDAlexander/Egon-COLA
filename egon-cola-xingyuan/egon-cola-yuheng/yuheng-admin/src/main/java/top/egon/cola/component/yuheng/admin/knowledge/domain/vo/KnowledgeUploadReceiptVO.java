package top.egon.cola.component.yuheng.admin.knowledge.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobStatusEnum;

/**
 * 中文说明：{@code KnowledgeUploadReceiptVO} 固定 API-015 的四字段 202 wire shape。
 * English summary: {@code KnowledgeUploadReceiptVO} fixes the four-field API-015 202 wire shape.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class KnowledgeUploadReceiptVO {

    @NotBlank
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String documentId;

    @NotBlank
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String revisionId;

    @NotBlank
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String jobId;

    @NotNull
    @Schema(implementation = KnowledgeJobStatusEnum.class)
    private KnowledgeJobStatusEnum status;
}
