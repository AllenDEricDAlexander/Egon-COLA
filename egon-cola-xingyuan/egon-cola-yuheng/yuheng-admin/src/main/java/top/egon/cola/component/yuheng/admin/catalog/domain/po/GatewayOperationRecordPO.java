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

import java.time.Instant;

/**
 * 中文说明：{@code GatewayOperationRecordPO} 是 MyBatis-Plus 行模型，负责 {@code gateway_operation} 表业务列的持久化边界，id/tenant/审计/软删/版本列由 {@link EgonModel} 只在父类声明一次。
 * English summary: {@code GatewayOperationRecordPO} is the MyBatis-Plus row model that owns the business columns of {@code gateway_operation} while id, tenant, audit, soft-delete and version stay declared once in {@link EgonModel}.
 *
 * 用法 / Usage: 仅在持久边界使用；jsonb 列显式声明 {@code GatewayJsonbTypeHandler} 并以 {@code autoResultMap} 读回结构化载体，业务 {@code revision} 与 MP {@code version} 互不替代，开放 {@code protocol}/{@code sourceType}/{@code lifecycleStatus} 列保持原 wire 字符串。/ Use it only at the persistence boundary; jsonb columns bind through an explicit {@code GatewayJsonbTypeHandler} with {@code autoResultMap}, the business {@code revision} stays separate from the MP {@code version}, and the open {@code protocol}/{@code sourceType}/{@code lifecycleStatus} columns keep their original wire strings.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gateway_operation", autoResultMap = true)
public class GatewayOperationRecordPO extends EgonModel<GatewayOperationRecordPO> {

    @TableField("application_id")
    private Long applicationId;

    @TableField("interface_group_id")
    private Long interfaceGroupId;

    @TableField("operation_key")
    private String operationKey;

    @TableField("protocol")
    private String protocol;

    @TableField("method_identity")
    private String methodIdentity;

    @TableField("external_accessible")
    private Boolean externalAccessible;

    @TableField(value = "provider_service_identity", typeHandler = GatewayJsonbTypeHandler.class)
    private JsonNode providerServiceIdentity;

    @TableField("source_type")
    private String sourceType;

    @TableField("lifecycle_status")
    private String lifecycleStatus;

    @TableField("current_definition_id")
    private Long currentDefinitionId;

    @TableField("deprecated_at")
    private Instant deprecatedAt;

    @TableField("revision")
    private Long revision;
}
