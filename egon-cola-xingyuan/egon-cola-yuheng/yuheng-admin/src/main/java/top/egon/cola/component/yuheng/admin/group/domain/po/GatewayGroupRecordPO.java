package top.egon.cola.component.yuheng.admin.group.domain.po;

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
 * 中文说明：{@code GatewayGroupRecordPO} 是 MyBatis-Plus 行模型，负责 {@code gateway_group} 表业务列的持久化边界，id/tenant/审计/软删/版本列由 {@link EgonModel} 只在父类声明一次。
 * English summary: {@code GatewayGroupRecordPO} is the MyBatis-Plus row model that owns the business columns of {@code gateway_group} while id, tenant, audit, soft-delete and version stay declared once in {@link EgonModel}.
 *
 * 用法 / Usage: 仅在持久边界使用；业务 {@code revision} 与 MP {@code version} 互不替代，编码键与主键分列。/ Use it only at the persistence boundary; the business {@code revision} stays separate from the MP {@code version} and the code key stays distinct from the primary key.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gateway_group", autoResultMap = true)
public class GatewayGroupRecordPO extends EgonModel<GatewayGroupRecordPO> {

    @TableField("gateway_group_code")
    private String gatewayGroupCode;

    @TableField("display_name")
    private String displayName;

    @TableField("env")
    private String env;

    @TableField("namespace")
    private String namespace;

    @TableField("description")
    private String description;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("revision")
    private Long revision;
}
