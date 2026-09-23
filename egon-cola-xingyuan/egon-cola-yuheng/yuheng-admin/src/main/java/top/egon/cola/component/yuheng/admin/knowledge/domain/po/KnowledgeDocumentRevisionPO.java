package top.egon.cola.component.yuheng.admin.knowledge.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import top.egon.cola.component.common.mybatis.model.EgonModel;
import top.egon.cola.component.yuheng.admin.shared.dao.typehandler.GatewayJsonbTypeHandler;

/**
 * 中文说明：{@code KnowledgeDocumentRevisionPO} 是 MyBatis-Plus 行模型，负责 {@code gateway_knowledge_revision} 表业务列的持久化边界，id/tenant/审计/软删/版本列由 {@link EgonModel} 只在父类声明一次。
 * English summary: {@code KnowledgeDocumentRevisionPO} is the MyBatis-Plus row model that owns the business columns of {@code gateway_knowledge_revision} while id, tenant, audit, soft-delete and version stay declared once in {@link EgonModel}.
 *
 * 用法 / Usage: 仅在持久边界使用；jsonb 列显式绑定 {@code GatewayJsonbTypeHandler}、vector 列绑定 {@code GatewayVectorTypeHandler}，并依赖 {@code autoResultMap} 读回；{@code raw_bytes} 以 {@code byte[]} 承载，业务 {@code revision} 等编码列与 MP {@code version}/{@code id} 互不替代。/ Use it only at the persistence boundary; jsonb columns bind {@code GatewayJsonbTypeHandler} and vector columns {@code GatewayVectorTypeHandler} under {@code autoResultMap}; the {@code raw_bytes} column is a {@code byte[]}, and business columns such as {@code revision} stay separate from the MP {@code version} and {@code id}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gateway_knowledge_revision", autoResultMap = true)
public class KnowledgeDocumentRevisionPO extends EgonModel<KnowledgeDocumentRevisionPO> {

    @TableField("kb_id")
    private Long kbId;

    @TableField("document_id")
    private Long documentId;

    @TableField("file_name")
    private String fileName;

    @TableField("media_type")
    private String mediaType;

    @TableField("raw_bytes")
    private byte[] rawBytes;

    @TableField("byte_count")
    private Long byteCount;

    @TableField("content_hash")
    private String contentHash;

    @TableField("extracted_text")
    private String extractedText;

    @TableField("embedding_space_id")
    private String embeddingSpaceId;

    @TableField("dimensions")
    private Integer dimensions;

    @TableField(value = "chunking_config", typeHandler = GatewayJsonbTypeHandler.class)
    private JsonNode chunkingConfig;

    @TableField("status")
    private String status;

    @TableField("chunk_count")
    private Integer chunkCount;

    @TableField("revision")
    private Long revision;
}
