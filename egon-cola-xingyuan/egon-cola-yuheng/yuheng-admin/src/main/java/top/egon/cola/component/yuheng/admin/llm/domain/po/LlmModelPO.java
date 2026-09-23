package top.egon.cola.component.yuheng.admin.llm.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import top.egon.cola.component.common.mybatis.model.EgonModel;
import top.egon.cola.component.yuheng.admin.llm.domain.enums.LlmModelKindEnum;
import top.egon.cola.component.yuheng.admin.shared.dao.typehandler.GatewayJsonbTypeHandler;

/**
 * 中文说明：{@code LlmModelPO} 是 MyBatis-Plus 行模型，负责 {@code gateway_llm_model} 表业务列的持久化边界，id/tenant/审计/软删/版本列由 {@link EgonModel} 只在父类声明一次。
 * English summary: {@code LlmModelPO} is the MyBatis-Plus row model that owns the business columns of {@code gateway_llm_model} while id, tenant, audit, soft-delete and version stay declared once in {@link EgonModel}.
 *
 * 用法 / Usage: 仅在持久边界使用；jsonb 列显式声明 {@code GatewayJsonbTypeHandler} 并以 {@code autoResultMap} 读回结构化的 Jackson node，业务 revision 与 MP version 互不替代，封闭列使用 {@code @EnumValue} 枚举。/ Use it only at the persistence boundary; jsonb columns bind through an explicit {@code GatewayJsonbTypeHandler} with {@code autoResultMap}, the business revision stays separate from the MP version, and closed columns use {@code @EnumValue} enums.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gateway_llm_model", autoResultMap = true)
public class LlmModelPO extends EgonModel<LlmModelPO> {

    @TableField("model_key")
    private String modelKey;

    @TableField("name")
    private String name;

    @TableField("kind")
    private LlmModelKindEnum kind;

    @TableField(value = "protocols", typeHandler = GatewayJsonbTypeHandler.class)
    private JsonNode protocols;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("dimensions")
    private Integer dimensions;

    @TableField("embedding_space_id")
    private String embeddingSpaceId;

    @TableField(value = "allowed_subjects", typeHandler = GatewayJsonbTypeHandler.class)
    private JsonNode allowedSubjects;

    @TableField(value = "routes", typeHandler = GatewayJsonbTypeHandler.class)
    private JsonNode routes;

    @TableField("revision")
    private Long revision;
}
