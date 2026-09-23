package top.egon.cola.component.yuheng.admin.release.domain.po;

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
 * 中文说明：{@code GatewayReleaseTargetRecordPO} 是 MyBatis-Plus 行模型，负责 {@code gateway_release_target} 表业务列的持久化边界，id/tenant/审计/软删/版本列由 {@link EgonModel} 只在父类声明一次。
 * English summary: {@code GatewayReleaseTargetRecordPO} is the MyBatis-Plus row model that owns the business columns of {@code gateway_release_target} while id, tenant, audit, soft-delete and version stay declared once in {@link EgonModel}.
 *
 * 用法 / Usage: 仅在持久边界使用；业务 {@code revision} 等编码列与 MP {@code version}/{@code id} 互不替代。/ Use it only at the persistence boundary; business columns such as {@code revision} stay separate from the MP {@code version} and {@code id}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gateway_release_target", autoResultMap = true)
public class GatewayReleaseTargetRecordPO extends EgonModel<GatewayReleaseTargetRecordPO> {

    @TableField("release_id")
    private Long releaseId;

    @TableField("attempt_no")
    private Long attemptNo;

    @TableField("instance_id")
    private String instanceId;

    @TableField("lease_id")
    private String leaseId;

    @TableField("status")
    private String status;

    @TableField("applied_version")
    private Long appliedVersion;

    @TableField("applied_artifact_sha256")
    private String appliedArtifactSha256;

    @TableField("error_code")
    private String errorCode;

    @TableField("observed_at")
    private Instant observedAt;

    @TableField("engine_role")
    private String engineRole;
}
