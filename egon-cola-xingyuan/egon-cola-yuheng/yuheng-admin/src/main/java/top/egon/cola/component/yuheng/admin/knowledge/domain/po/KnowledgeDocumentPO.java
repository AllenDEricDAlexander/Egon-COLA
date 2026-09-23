package top.egon.cola.component.yuheng.admin.knowledge.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import top.egon.cola.component.common.mybatis.model.EgonModel;

/**
 * 中文说明：{@code KnowledgeDocumentPO} 是 MyBatis-Plus 行模型，负责 {@code gateway_knowledge_document} 表业务列的持久化边界，id/tenant/审计/软删/版本列由 {@link EgonModel} 只在父类声明一次。
 * English summary: {@code KnowledgeDocumentPO} is the MyBatis-Plus row model that owns the business columns of {@code gateway_knowledge_document} while id, tenant, audit, soft-delete and version stay declared once in {@link EgonModel}.
 *
 * 用法 / Usage: 仅在持久边界使用；业务 {@code revision} 等编码列与 MP {@code version}/{@code id} 互不替代。/ Use it only at the persistence boundary; business columns such as {@code revision} stay separate from the MP {@code version} and {@code id}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gateway_knowledge_document", autoResultMap = true)
public class KnowledgeDocumentPO extends EgonModel<KnowledgeDocumentPO> {

    @TableField("kb_id")
    private Long kbId;

    @TableField("file_name")
    private String fileName;

    @TableField("active_revision_id")
    private Long activeRevisionId;

    @TableField("latest_job_id")
    private Long latestJobId;

    @TableField("revision")
    private Long revision;
}
