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

import java.time.Instant;

/**
 * 中文说明：{@code KnowledgeJobPO} 是 MyBatis-Plus 行模型，负责 {@code gateway_knowledge_job} 表业务列的持久化边界，id/tenant/审计/软删/版本列由 {@link EgonModel} 只在父类声明一次。
 * English summary: {@code KnowledgeJobPO} is the MyBatis-Plus row model that owns the business columns of {@code gateway_knowledge_job} while id, tenant, audit, soft-delete and version stay declared once in {@link EgonModel}.
 *
 * 用法 / Usage: 仅在持久边界使用；jsonb 列显式绑定 {@code GatewayJsonbTypeHandler}、vector 列绑定 {@code GatewayVectorTypeHandler}，并依赖 {@code autoResultMap} 读回；业务 {@code revision} 等编码列与 MP {@code version}/{@code id} 互不替代。/ Use it only at the persistence boundary; jsonb columns bind {@code GatewayJsonbTypeHandler} and vector columns {@code GatewayVectorTypeHandler} under {@code autoResultMap}; business columns such as {@code revision} stay separate from the MP {@code version} and {@code id}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gateway_knowledge_job", autoResultMap = true)
public class KnowledgeJobPO extends EgonModel<KnowledgeJobPO> {

    @TableField("kb_id")
    private Long kbId;

    @TableField("type")
    private String type;

    @TableField("resource_id")
    private String resourceId;

    @TableField("actor_id")
    private String actorId;

    @TableField(value = "payload", typeHandler = GatewayJsonbTypeHandler.class)
    private JsonNode payload;

    @TableField("idempotency_key")
    private String idempotencyKey;

    @TableField("request_hash")
    private String requestHash;

    @TableField("status")
    private String status;

    @TableField("stage")
    private String stage;

    @TableField("attempt")
    private Integer attempt;

    @TableField("next_attempt_at")
    private Instant nextAttemptAt;

    @TableField("lease_owner")
    private String leaseOwner;

    @TableField("lease_token")
    private Long leaseToken;

    @TableField("lease_expires_at")
    private Instant leaseExpiresAt;

    @TableField("error_code")
    private String errorCode;

    @TableField(value = "result", typeHandler = GatewayJsonbTypeHandler.class)
    private JsonNode result;

    @TableField("retry_of_job_id")
    private Long retryOfJobId;

    @TableField("revision")
    private Long revision;
}
