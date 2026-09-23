package top.egon.cola.component.yuheng.admin.catalog.domain.po;

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
 * 中文说明：{@code GatewayEntityDomainPO} 是 MyBatis-Plus 行模型，负责 {@code gateway_entity_domain} 表业务列的持久化边界，id/tenant/审计/软删/版本列由 {@link EgonModel} 只在父类声明一次。
 * English summary: {@code GatewayEntityDomainPO} is the MyBatis-Plus row model that owns the business columns of {@code gateway_entity_domain} while id, tenant, audit, soft-delete and version stay declared once in {@link EgonModel}.
 *
 * 用法 / Usage: 仅在持久边界使用；外键以数值列保存，编码键与主键分列。/ Use it only at the persistence boundary; the foreign key stays a numeric column and the code key stays distinct from the primary key.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gateway_entity_domain", autoResultMap = true)
public class GatewayEntityDomainPO extends EgonModel<GatewayEntityDomainPO> {

    @TableField("business_domain_id")
    private Long businessDomainId;

    @TableField("code")
    private String code;

    @TableField("display_name")
    private String displayName;

    @TableField("description")
    private String description;
}
