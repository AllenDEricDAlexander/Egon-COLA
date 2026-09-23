package top.egon.cola.component.yuheng.admin.openapi.domain.po;

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
 * 中文说明：{@code GatewayOpenApiSnapshotRecordPO} 是 MyBatis-Plus 行模型，负责 {@code gateway_openapi_snapshot} 表业务列的持久化边界，id/tenant/审计/软删/版本列由 {@link EgonModel} 只在父类声明一次。
 * English summary: {@code GatewayOpenApiSnapshotRecordPO} is the MyBatis-Plus row model that owns the business columns of {@code gateway_openapi_snapshot} while id, tenant, audit, soft-delete and version stay declared once in {@link EgonModel}.
 *
 * 用法 / Usage: 仅在持久边界使用；jsonb 列显式绑定 {@code GatewayJsonbTypeHandler}、vector 列绑定 {@code GatewayVectorTypeHandler}，并依赖 {@code autoResultMap} 读回；业务 {@code revision} 等编码列与 MP {@code version}/{@code id} 互不替代。/ Use it only at the persistence boundary; jsonb columns bind {@code GatewayJsonbTypeHandler} and vector columns {@code GatewayVectorTypeHandler} under {@code autoResultMap}; business columns such as {@code revision} stay separate from the MP {@code version} and {@code id}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gateway_openapi_snapshot", autoResultMap = true)
public class GatewayOpenApiSnapshotRecordPO extends EgonModel<GatewayOpenApiSnapshotRecordPO> {

    @TableField("application_id")
    private Long applicationId;

    @TableField("definition_set_id")
    private Long definitionSetId;

    @TableField("build_id")
    private String buildId;

    @TableField("artifact_version")
    private String artifactVersion;

    @TableField("openapi_group")
    private String openapiGroup;

    @TableField("openapi_version")
    private String openapiVersion;

    @TableField("document_sha256")
    private String documentSha256;

    @TableField("canonical_sha256")
    private String canonicalSha256;

    @TableField(value = "document_json", typeHandler = GatewayJsonbTypeHandler.class)
    private JsonNode documentJson;

    @TableField("validation_status")
    private String validationStatus;

    @TableField(value = "validation_messages", typeHandler = GatewayJsonbTypeHandler.class)
    private JsonNode validationMessages;

    @TableField("operation_count")
    private Integer operationCount;

    @TableField("schema_count")
    private Integer schemaCount;

    @TableField("fetched_from_instance_id")
    private String fetchedFromInstanceId;

    @TableField("fetched_at")
    private Instant fetchedAt;

    @TableField("validated_at")
    private Instant validatedAt;
}
