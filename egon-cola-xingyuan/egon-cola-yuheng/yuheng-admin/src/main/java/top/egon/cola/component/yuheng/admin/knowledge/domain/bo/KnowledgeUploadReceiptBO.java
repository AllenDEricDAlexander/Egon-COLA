package top.egon.cola.component.yuheng.admin.knowledge.domain.bo;

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
 * 中文说明：{@code KnowledgeUploadReceiptBO} 是 API-015 在提交文档、修订和摄取作业后返回的稳定受理结果。
 * English summary: {@code KnowledgeUploadReceiptBO} is the stable API-015 acceptance result after the document, revision
 * and ingestion job have committed together.
 *
 * 用法 / Usage: 仅携带上传回执字段；不暴露持久化行或文档原文。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class KnowledgeUploadReceiptBO {

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
    private KnowledgeJobStatusEnum status;
}
