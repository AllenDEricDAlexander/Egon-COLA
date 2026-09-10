package top.egon.cola.archetype.source.agent.infrastructure.knowledge.repo.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.common.mybatis.model.EgonModel;

/**
 * Persistence carrier of one {@code knowledge_base} row.
 *
 * <p>The identifier, the tenant, the audit columns and the soft-delete flag come from
 * {@link EgonModel} and are never written by the business code; the declared columns are the
 * metadata a base freezes at creation time. {@code chunk_config} travels as the raw jsonb text and
 * is projected to the chunking carrier by the converter.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@TableName("knowledge_base")
public class KnowledgeBasePO extends EgonModel<KnowledgeBasePO> {

    @TableField("code")
    private String code;

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("embedding_model")
    private String embeddingModel;

    @TableField("chunk_strategy")
    private String chunkStrategy;

    @TableField(value = "chunk_config", typeHandler = JsonbStringTypeHandler.class)
    private String chunkConfig;

    @TableField("status")
    private String status;
}
