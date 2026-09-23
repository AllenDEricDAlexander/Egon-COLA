package top.egon.cola.component.yuheng.admin.mcp.domain.po;

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
 * 中文说明：{@code McpServerRecordPO} 是 MyBatis-Plus 行模型，负责 {@code gateway_mcp_server} 表业务列的持久化边界，id/tenant/审计/软删/版本列由 {@link EgonModel} 只在父类声明一次。
 * English summary: {@code McpServerRecordPO} is the MyBatis-Plus row model that owns the business columns of {@code gateway_mcp_server} while id, tenant, audit, soft-delete and version stay declared once in {@link EgonModel}.
 *
 * 用法 / Usage: 仅在持久边界使用；jsonb 列显式绑定 {@code GatewayJsonbTypeHandler}、vector 列绑定 {@code GatewayVectorTypeHandler}，并依赖 {@code autoResultMap} 读回；业务 {@code revision} 等编码列与 MP {@code version}/{@code id} 互不替代。/ Use it only at the persistence boundary; jsonb columns bind {@code GatewayJsonbTypeHandler} and vector columns {@code GatewayVectorTypeHandler} under {@code autoResultMap}; business columns such as {@code revision} stay separate from the MP {@code version} and {@code id}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "gateway_mcp_server", autoResultMap = true)
public class McpServerRecordPO extends EgonModel<McpServerRecordPO> {

    @TableField("gateway_group_id")
    private Long gatewayGroupId;

    @TableField("server_code")
    private String serverCode;

    @TableField("display_name")
    private String displayName;

    @TableField("description")
    private String description;

    @TableField("instructions")
    private String instructions;

    @TableField(value = "dialects", typeHandler = GatewayJsonbTypeHandler.class)
    private JsonNode dialects;

    @TableField("resource_uri")
    private String resourceUri;

    @TableField("list_cache_ttl_seconds")
    private Long listCacheTtlSeconds;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("revision")
    private Long revision;
}
