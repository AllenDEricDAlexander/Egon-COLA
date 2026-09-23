package top.egon.cola.component.yuheng.admin.openapi.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import java.time.Instant;

/**
 * 中文说明：{@code GatewayOpenApiSyncRecordPO} 是 MyBatis-Plus 行模型，负责 {@code gateway_openapi_sync_state} 表业务列的持久化边界，id/tenant/审计/软删/版本列由 {@link EgonModel} 只在父类声明一次。
 * English summary: {@code GatewayOpenApiSyncRecordPO} is the MyBatis-Plus row model that owns the business columns of {@code gateway_openapi_sync_state} while id, tenant, audit, soft-delete and version stay declared once in {@link EgonModel}.
 *
 * 用法 / Usage: 仅在持久边界使用；业务 {@code revision} 等编码列与 MP {@code version}/{@code id} 互不替代。/ Use it only at the persistence boundary; business columns such as {@code revision} stay separate from the MP {@code version} and {@code id}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gateway_openapi_sync_state", autoResultMap = true)
public class GatewayOpenApiSyncRecordPO extends EgonModel<GatewayOpenApiSyncRecordPO> {

    @TableField("application_id")
    private Long applicationId;

    @TableField("build_id")
    private String buildId;

    @TableField("artifact_version")
    private String artifactVersion;

    @TableField("openapi_group")
    private String openapiGroup;

    @TableField("provider_service_name")
    private String providerServiceName;

    @TableField("provider_group")
    private String providerGroup;

    @TableField("provider_version")
    private String providerVersion;

    @TableField("status")
    private String status;

    @TableField("latest_snapshot_id")
    private Long latestSnapshotId;

    @TableField("definition_set_id")
    private Long definitionSetId;

    @TableField("last_instance_id")
    private String lastInstanceId;

    @TableField("attempt_count")
    private Integer attemptCount;

    @TableField("last_error_code")
    private String lastErrorCode;

    @TableField("last_error_message")
    private String lastErrorMessage;

    @TableField("first_discovered_at")
    private Instant firstDiscoveredAt;

    @TableField("last_attempt_at")
    private Instant lastAttemptAt;

    @TableField("last_success_at")
    private Instant lastSuccessAt;

    @TableField("next_retry_at")
    private Instant nextRetryAt;

    @TableField("revision")
    private Long revision;
}
