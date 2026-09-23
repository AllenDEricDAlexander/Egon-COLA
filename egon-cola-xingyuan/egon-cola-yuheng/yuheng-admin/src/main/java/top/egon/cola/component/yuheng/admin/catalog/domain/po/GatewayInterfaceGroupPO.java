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
 * 中文说明：{@code GatewayInterfaceGroupPO} 是 MyBatis-Plus 行模型，负责 {@code gateway_interface_group} 表业务列的持久化边界，id/tenant/审计/软删/版本列由 {@link EgonModel} 只在父类声明一次。
 * English summary: {@code GatewayInterfaceGroupPO} is the MyBatis-Plus row model that owns the business columns of {@code gateway_interface_group} while id, tenant, audit, soft-delete and version stay declared once in {@link EgonModel}.
 *
 * 用法 / Usage: 仅在持久边界使用；{@code sourceType} 是开放 varchar 列，按原 wire 字符串存取，不与 API 层协议枚举混用。/ Use it only at the persistence boundary; the open {@code sourceType} column keeps its original wire string and is not merged with the API protocol enum.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gateway_interface_group", autoResultMap = true)
public class GatewayInterfaceGroupPO extends EgonModel<GatewayInterfaceGroupPO> {

    @TableField("entity_domain_id")
    private Long entityDomainId;

    @TableField("code")
    private String code;

    @TableField("display_name")
    private String displayName;

    @TableField("source_type")
    private String sourceType;

    @TableField("class_name")
    private String className;

    @TableField("description")
    private String description;
}
