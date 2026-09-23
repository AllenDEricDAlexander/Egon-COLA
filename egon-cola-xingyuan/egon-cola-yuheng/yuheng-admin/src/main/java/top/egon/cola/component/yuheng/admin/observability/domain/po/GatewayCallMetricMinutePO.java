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
 * 中文说明：{@code GatewayCallMetricMinutePO} 是 MyBatis-Plus 行模型，负责 {@code gateway_call_metric_minute} 表业务列的持久化边界，id/tenant/审计/软删/版本列由 {@link EgonModel} 只在父类声明一次。
 * English summary: {@code GatewayCallMetricMinutePO} is the MyBatis-Plus row model that owns the business columns of {@code gateway_call_metric_minute} while id, tenant, audit, soft-delete and version stay declared once in {@link EgonModel}.
 *
 * 用法 / Usage: 仅在持久边界使用；业务 {@code revision} 等编码列与 MP {@code version}/{@code id} 互不替代。/ Use it only at the persistence boundary; business columns such as {@code revision} stay separate from the MP {@code version} and {@code id}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gateway_call_metric_minute", autoResultMap = true)
public class GatewayCallMetricMinutePO extends EgonModel<GatewayCallMetricMinutePO> {

    @TableField("bucket_at")
    private Instant bucketAt;

    @TableField("env")
    private String env;

    @TableField("namespace")
    private String namespace;

    @TableField("protocol")
    private String protocol;

    @TableField("gateway_group_id")
    private String gatewayGroupId;

    @TableField("request_count")
    private Long requestCount;

    @TableField("error_count")
    private Long errorCount;

    @TableField("duration_total_ms")
    private Long durationTotalMs;

    @TableField("duration_max_ms")
    private Long durationMaxMs;
}
