package top.egon.cola.component.yuheng.admin.reporting.domain.po;

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
 * 中文说明：{@code GatewayDefinitionSetPO} 是 MyBatis-Plus 行模型，负责 {@code gateway_definition_set} 表业务列的持久化边界，id/tenant/审计/软删/版本列由 {@link EgonModel} 只在父类声明一次。
 * English summary: {@code GatewayDefinitionSetPO} is the MyBatis-Plus row model that owns the business columns of {@code gateway_definition_set} while id, tenant, audit, soft-delete and version stay declared once in {@link EgonModel}.
 *
 * 用法 / Usage: 仅在持久边界使用；上报时间戳按 UTC {@code Instant} 保存，开放 {@code protocol}/{@code status} 列保持原 wire 字符串。/ Use it only at the persistence boundary; report timestamps are UTC {@code Instant} and the open {@code protocol}/{@code status} columns keep their original wire strings.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gateway_definition_set", autoResultMap = true)
public class GatewayDefinitionSetPO extends EgonModel<GatewayDefinitionSetPO> {

    @TableField("application_id")
    private Long applicationId;

    @TableField("report_id")
    private String reportId;

    @TableField("build_id")
    private String buildId;

    @TableField("protocol")
    private String protocol;

    @TableField("fingerprint")
    private String fingerprint;

    @TableField("complete_set")
    private Boolean completeSet;

    @TableField("status")
    private String status;

    @TableField("operation_count")
    private Integer operationCount;

    @TableField("accepted_count")
    private Integer acceptedCount;

    @TableField("conflict_count")
    private Integer conflictCount;

    @TableField("received_at")
    private Instant receivedAt;

    @TableField("completed_at")
    private Instant completedAt;

    @TableField("activated_at")
    private Instant activatedAt;

    @TableField("retired_at")
    private Instant retiredAt;
}
