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

/**
 * 中文说明：{@code GatewayReleasePublicationRecordPO} 是 MyBatis-Plus 行模型，负责 {@code gateway_release_publication} 表业务列的持久化边界，id/tenant/审计/软删/版本列由 {@link EgonModel} 只在父类声明一次。
 * English summary: {@code GatewayReleasePublicationRecordPO} is the MyBatis-Plus row model that owns the business columns of {@code gateway_release_publication} while id, tenant, audit, soft-delete and version stay declared once in {@link EgonModel}.
 *
 * 用法 / Usage: 仅在持久边界使用；业务 {@code revision} 等编码列与 MP {@code version}/{@code id} 互不替代。/ Use it only at the persistence boundary; business columns such as {@code revision} stay separate from the MP {@code version} and {@code id}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gateway_release_publication", autoResultMap = true)
public class GatewayReleasePublicationRecordPO extends EgonModel<GatewayReleasePublicationRecordPO> {

    @TableField("release_id")
    private Long releaseId;

    @TableField("attempt_no")
    private Long attemptNo;

    @TableField("phase_order")
    private Integer phaseOrder;

    @TableField("phase_type")
    private String phaseType;

    @TableField("config_key")
    private String configKey;

    @TableField("content_value")
    private String contentValue;

    @TableField("content_sha256")
    private String contentSha256;

    @TableField("expected_version")
    private Long expectedVersion;

    @TableField("change_id")
    private String changeId;

    @TableField("ddc_target_version")
    private Long ddcTargetVersion;

    @TableField("ddc_status")
    private String ddcStatus;

    @TableField("error_code")
    private String errorCode;

    @TableField("error_message")
    private String errorMessage;

    @TableField("target_role")
    private String targetRole;

    @TableField("target_biz_code")
    private String targetBizCode;

    @TableField("target_env")
    private String targetEnv;

    @TableField("target_app_code")
    private String targetAppCode;
}
