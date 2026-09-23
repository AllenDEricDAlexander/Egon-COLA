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
 * 中文说明：{@code GatewayConsumeFailureRecordPO} 是 MyBatis-Plus 行模型，负责 {@code gateway_call_event_consume_failure} 表业务列的持久化边界，id/tenant/审计/软删/版本列由 {@link EgonModel} 只在父类声明一次。
 * English summary: {@code GatewayConsumeFailureRecordPO} is the MyBatis-Plus row model that owns the business columns of {@code gateway_call_event_consume_failure} while id, tenant, audit, soft-delete and version stay declared once in {@link EgonModel}.
 *
 * 用法 / Usage: 仅在持久边界使用；业务 {@code revision} 等编码列与 MP {@code version}/{@code id} 互不替代。/ Use it only at the persistence boundary; business columns such as {@code revision} stay separate from the MP {@code version} and {@code id}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gateway_call_event_consume_failure", autoResultMap = true)
public class GatewayConsumeFailureRecordPO extends EgonModel<GatewayConsumeFailureRecordPO> {

    @TableField("topic")
    private String topic;

    @TableField("partition_no")
    private Integer partitionNo;

    @TableField("offset_no")
    private Long offsetNo;

    @TableField("event_id")
    private String eventId;

    @TableField("failure_code")
    private String failureCode;

    @TableField("failure_message")
    private String failureMessage;

    @TableField("payload_sha256")
    private String payloadSha256;

    @TableField("payload_size")
    private Integer payloadSize;

    @TableField("occurred_at")
    private Instant occurredAt;
}
