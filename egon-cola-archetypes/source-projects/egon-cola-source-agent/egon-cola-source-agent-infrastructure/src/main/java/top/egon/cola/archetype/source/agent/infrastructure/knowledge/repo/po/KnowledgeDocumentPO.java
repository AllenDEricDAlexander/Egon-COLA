package top.egon.cola.archetype.source.agent.infrastructure.knowledge.repo.po;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.common.mybatis.model.EgonModel;

/**
 * Persistence carrier of one {@code knowledge_document} row.
 *
 * <p>Every business field is boxed on purpose: the status writes are built by setting only the
 * columns of one transition, and a primitive field would always look "present" and be written with
 * its default.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@TableName("knowledge_document")
public class KnowledgeDocumentPO extends EgonModel<KnowledgeDocumentPO> {

    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    @TableField("display_name")
    private String displayName;

    @TableField("file_name")
    private String fileName;

    @TableField("mime_type")
    private String mimeType;

    @TableField("size_bytes")
    private Long sizeBytes;

    @TableField("content_hash")
    private String contentHash;

    @TableField("storage_type")
    private String storageType;

    @TableField("storage_key")
    private String storageKey;

    @TableField("content")
    private String content;

    @TableField("status")
    private String status;

    @TableField("chunk_count")
    private Integer chunkCount;

    @TableField("attempt_count")
    private Integer attemptCount;

    // The failure details are always written: every other update leaves them out when they are
    // null, which would make clearing the previous attempt's failure impossible.
    @TableField(value = "error_code", updateStrategy = FieldStrategy.ALWAYS)
    private String errorCode;

    @TableField(value = "error_message", updateStrategy = FieldStrategy.ALWAYS)
    private String errorMessage;
}
