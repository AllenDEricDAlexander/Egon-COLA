package top.egon.cola.component.yuheng.admin.observability.domain.po;

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
 * 中文说明：{@code GatewayCallEventSummaryPO} 是 MyBatis-Plus 行模型，负责 {@code gateway_call_event_summary} 表业务列的持久化边界，id/tenant/审计/软删/版本列由 {@link EgonModel} 只在父类声明一次。
 * English summary: {@code GatewayCallEventSummaryPO} is the MyBatis-Plus row model that owns the business columns of {@code gateway_call_event_summary} while id, tenant, audit, soft-delete and version stay declared once in {@link EgonModel}.
 *
 * 用法 / Usage: 仅在持久边界使用；业务 {@code revision} 等编码列与 MP {@code version}/{@code id} 互不替代。/ Use it only at the persistence boundary; business columns such as {@code revision} stay separate from the MP {@code version} and {@code id}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gateway_call_event_summary", autoResultMap = true)
public class GatewayCallEventSummaryPO extends EgonModel<GatewayCallEventSummaryPO> {

    @TableField("event_id")
    private String eventId;

    @TableField("trace_id")
    private String traceId;

    @TableField("occurred_at")
    private Instant occurredAt;

    @TableField("completed_at")
    private Instant completedAt;

    @TableField("duration_ms")
    private Long durationMs;

    @TableField("protocol")
    private String protocol;

    @TableField("access_zone")
    private String accessZone;

    @TableField("env")
    private String env;

    @TableField("namespace")
    private String namespace;

    @TableField("gateway_group_id")
    private String gatewayGroupId;

    @TableField("operation_id")
    private String operationId;

    @TableField("route_id")
    private String routeId;

    @TableField("result_category")
    private String resultCategory;

    @TableField("gateway_error_code")
    private String gatewayErrorCode;

    @TableField("http_status")
    private Integer httpStatus;

    @TableField("grpc_status")
    private String grpcStatus;

    @TableField("engine_node_id")
    private String engineNodeId;

    @TableField("provider_service")
    private String providerService;

    @TableField("attempt_count")
    private Integer attemptCount;

    @TableField("expires_at")
    private Instant expiresAt;
}
