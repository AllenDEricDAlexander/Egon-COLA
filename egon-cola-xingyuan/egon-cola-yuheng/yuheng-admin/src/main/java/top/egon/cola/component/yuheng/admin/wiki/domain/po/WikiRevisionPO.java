package top.egon.cola.component.yuheng.admin.wiki.domain.po;

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

import java.time.Instant;

/**
 * 中文说明：{@code WikiRevisionPO} 是 MyBatis-Plus 行模型，负责 {@code gateway_wiki_revision} 表业务列的持久化边界，id/tenant/审计/软删/版本列由 {@link EgonModel} 只在父类声明一次。
 * English summary: {@code WikiRevisionPO} is the MyBatis-Plus row model that owns the business columns of {@code gateway_wiki_revision} while id, tenant, audit, soft-delete and version stay declared once in {@link EgonModel}.
 *
 * 用法 / Usage: 仅在持久边界使用；jsonb 列显式绑定 {@code GatewayJsonbTypeHandler}、vector 列绑定 {@code GatewayVectorTypeHandler}，并依赖 {@code autoResultMap} 读回；业务 {@code revision} 等编码列与 MP {@code version}/{@code id} 互不替代。/ Use it only at the persistence boundary; jsonb columns bind {@code GatewayJsonbTypeHandler} and vector columns {@code GatewayVectorTypeHandler} under {@code autoResultMap}; business columns such as {@code revision} stay separate from the MP {@code version} and {@code id}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gateway_wiki_revision", autoResultMap = true)
public class WikiRevisionPO extends EgonModel<WikiRevisionPO> {

    @TableField("kb_id")
    private Long kbId;

    @TableField("page_id")
    private Long pageId;

    @TableField("title")
    private String title;

    @TableField("markdown")
    private String markdown;

    @TableField(value = "tags", typeHandler = GatewayJsonbTypeHandler.class)
    private JsonNode tags;

    @TableField(value = "links", typeHandler = GatewayJsonbTypeHandler.class)
    private JsonNode links;

    @TableField(value = "sources", typeHandler = GatewayJsonbTypeHandler.class)
    private JsonNode sources;

    @TableField("content_hash")
    private String contentHash;

    @TableField("author_actor_id")
    private String authorActorId;

    @TableField("generation_job_id")
    private Long generationJobId;

    @TableField("publication_status")
    private String publicationStatus;

    @TableField("review_status")
    private String reviewStatus;

    @TableField("publication_policy_snapshot")
    private String publicationPolicySnapshot;

    @TableField("publication_version")
    private Long publicationVersion;

    @TableField("review_instance_id")
    private String reviewInstanceId;

    @TableField("reviewer_actor_id")
    private String reviewerActorId;

    @TableField("reviewed_at")
    private Instant reviewedAt;

    @TableField("review_decision_code")
    private String reviewDecisionCode;

    @TableField("published_at")
    private Instant publishedAt;

    @TableField("published_by_actor_id")
    private String publishedByActorId;

    @TableField("archived_at")
    private Instant archivedAt;

    @TableField("archived_by_actor_id")
    private String archivedByActorId;

    @TableField("publication_error_code")
    private String publicationErrorCode;

    @TableField("ever_published")
    private Boolean everPublished;

    @TableField("revision")
    private Long revision;
}
