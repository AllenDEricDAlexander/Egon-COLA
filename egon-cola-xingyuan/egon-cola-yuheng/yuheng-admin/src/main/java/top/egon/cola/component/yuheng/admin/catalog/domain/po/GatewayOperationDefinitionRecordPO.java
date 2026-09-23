package top.egon.cola.component.yuheng.admin.catalog.domain.po;

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
import top.egon.cola.component.yuheng.admin.shared.dao.typehandler.GatewayJsonbTypeHandler;

/**
 * 中文说明：{@code GatewayOperationDefinitionRecordPO} 是 MyBatis-Plus 行模型，负责 {@code gateway_operation_definition} 表业务列的持久化边界，id/tenant/审计/软删/版本列由 {@link EgonModel} 只在父类声明一次。
 * English summary: {@code GatewayOperationDefinitionRecordPO} is the MyBatis-Plus row model that owns the business columns of {@code gateway_operation_definition} while id, tenant, audit, soft-delete and version stay declared once in {@link EgonModel}.
 *
 * 用法 / Usage: 仅在持久边界使用；schema/描述符/属性等 jsonb 列显式声明 {@code GatewayJsonbTypeHandler} 并以 {@code autoResultMap} 读回结构化载体，业务 {@code definitionVersion} 与 MP {@code version} 互不替代。/ Use it only at the persistence boundary; the schema/descriptor/attributes jsonb columns bind through an explicit {@code GatewayJsonbTypeHandler} with {@code autoResultMap}, and the business {@code definitionVersion} stays separate from the MP {@code version}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gateway_operation_definition", autoResultMap = true)
public class GatewayOperationDefinitionRecordPO extends EgonModel<GatewayOperationDefinitionRecordPO> {

    @TableField("operation_id")
    private Long operationId;

    @TableField("definition_set_id")
    private Long definitionSetId;

    @TableField("definition_version")
    private Long definitionVersion;

    @TableField("definition_sha256")
    private String definitionSha256;

    @TableField("summary")
    private String summary;

    @TableField(value = "tags", typeHandler = GatewayJsonbTypeHandler.class)
    private JsonNode tags;

    @TableField(value = "request_schema", typeHandler = GatewayJsonbTypeHandler.class)
    private JsonNode requestSchema;

    @TableField(value = "response_schema", typeHandler = GatewayJsonbTypeHandler.class)
    private JsonNode responseSchema;

    @TableField(value = "error_schema", typeHandler = GatewayJsonbTypeHandler.class)
    private JsonNode errorSchema;

    @TableField(value = "descriptor_snapshot", typeHandler = GatewayJsonbTypeHandler.class)
    private JsonNode descriptorSnapshot;

    @TableField(value = "attributes", typeHandler = GatewayJsonbTypeHandler.class)
    private JsonNode attributes;

    @TableField("external_accessible")
    private Boolean externalAccessible;
}
