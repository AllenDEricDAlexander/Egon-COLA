package top.egon.cola.component.yuheng.admin.mcp.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.component.yuheng.admin.mcp.domain.bo.McpCapabilityRecordBO;
import top.egon.cola.component.yuheng.admin.mcp.domain.enums.McpCapabilityKindEnum;
import top.egon.cola.component.yuheng.admin.mcp.domain.po.McpAppBindingDraftPO;
import top.egon.cola.component.yuheng.admin.mcp.domain.po.McpPromptDraftPO;
import top.egon.cola.component.yuheng.admin.mcp.domain.po.McpResourceDraftPO;
import top.egon.cola.component.yuheng.admin.mcp.domain.po.McpResourceTemplateDraftPO;
import top.egon.cola.component.yuheng.admin.mcp.domain.po.McpTaskPolicyDraftPO;

/**
 * 中文说明：{@code McpCapabilityDraftPersistenceConverter} 是五张 MCP 能力草稿表
 * （{@code gateway_mcp_resource_draft}、{@code gateway_mcp_resource_template_draft}、
 * {@code gateway_mcp_prompt_draft}、{@code gateway_mcp_task_policy_draft}、
 * {@code gateway_mcp_app_binding_draft}）在持久边界唯一的转换器，负责同一份 {@link McpCapabilityRecordBO}
 * 业务载体与五种行模型之间的双向映射：一条端口 {@code SELECT ... WHERE gateway_group_id = ? AND deleted = FALSE
 * ORDER BY server_id, <capability_name>} 模板按能力种类实例化成五份，因此本转换器按种类提供五个读取方法与
 * 五个插入渲染器、五个更新渲染器，而不是 {@code BaseConverter} 的一对一形态。
 * 端口上的十进制标识（{@code id}/{@code gateway_group_id}/{@code server_id}）与
 * {@code bigint} 列按十进制文本往返，空白文本视为尚未生成、交由 {@code ASSIGN_ID} 补位，
 * 非法文本如实抛「{@code <table> identifier must be decimal}」而不是写出 {@code NULL} 冒充主键；
 * 每种能力的专属列（{@code resource_uri}/{@code driver_type}/{@code uri_template}/{@code source_type}
 * 是文本，{@code operation_id}/{@code remote_mount_id}/{@code app_artifact_id} 是外键）按
 * {@code McpCapabilityBinding} 给出的顺序逐位取用，即被替换语句里占位符的原始顺序，
 * 必填缺失与空白可选分别按旧文案 {@code capability content <key> is required} 与 SQL NULL 落地，
 * 外键位上的非十进制文本同样如实拒绝；jsonb {@code content} 列经 Spring 托管的 Jackson 在
 * {@code JsonNode} 与 {@code Map<String, Object>} 之间往返，并逐字保留被替换边界的规范形态——
 * 编码失败抛 {@code MCP persistence value cannot be serialized}、解码失败抛
 * {@code stored MCP persistence value is invalid}，读回的映射一律做不可变复制；
 * 读取一律经 {@code McpCapabilityRecordBO.normalized(...)} 建立载体，写入方向绝不触碰租户、审计、软删、
 * 版本与技术主键，业务 {@code revision} 与 {@code enabled} 由门面按旧算式覆写。
 *
 * English summary: {@code McpCapabilityDraftPersistenceConverter} is the only converter at the persistence boundary of the five MCP
 * capability draft tables ({@code gateway_mcp_resource_draft}, {@code gateway_mcp_resource_template_draft},
 * {@code gateway_mcp_prompt_draft}, {@code gateway_mcp_task_policy_draft} and {@code gateway_mcp_app_binding_draft}), mapping one
 * {@link McpCapabilityRecordBO} carrier against five row models: the replaced
 * {@code SELECT ... WHERE gateway_group_id = ? AND deleted = FALSE ORDER BY server_id, <capability_name>} statement was one template
 * instantiated per capability kind, so this converter offers five readers, five insert renderers and five change renderers instead of the
 * one-to-one shape of {@code BaseConverter}. The decimal identifiers of the port ({@code id}/{@code gateway_group_id}/
 * {@code server_id}) round-trip as decimal text against their {@code bigint} columns, a blank text means "not generated yet" so
 * {@code ASSIGN_ID} fills it, and a malformed text is rejected truthfully as {@code <table> identifier must be decimal} instead of
 * writing {@code NULL} in place of a key; the dedicated column of each kind (the textual {@code resource_uri}/{@code driver_type}/
 * {@code uri_template}/{@code source_type} and the foreign keys {@code operation_id}/{@code remote_mount_id}/{@code app_artifact_id})
 * is taken positionally from the list {@code McpCapabilityBinding} produces, which is exactly the placeholder order of the replaced
 * statement, so a missing required value still raises {@code capability content <key> is required}, an absent optional one still becomes
 * SQL NULL, and a non-decimal value on a foreign-key position is refused just as truthfully; the jsonb {@code content} column
 * round-trips through the Spring-managed Jackson mapper between a {@code JsonNode} and a {@code Map<String, Object>} and keeps the
 * canonical shape of the replaced boundary verbatim - an encoding failure raises {@code MCP persistence value cannot be serialized}, a
 * decoding failure {@code stored MCP persistence value is invalid} - and every read map is copied immutably; reads always build the
 * carrier through {@code McpCapabilityRecordBO.normalized(...)}, the write direction never touches the tenant, audit, soft-delete,
 * version or technical identifier columns, and the business {@code revision} and {@code enabled} are overwritten by the facade with the
 * legacy arithmetic.
 *
 * 用法 / Usage: 由 {@code gateway_mcp_*_draft} 五张表的受守卫 MP 门面注入（bean 名
 * {@code mcpCapabilityDraftPersistenceConverter}）；调用方必须先按 {@code McpCapabilityBinding.of(kind)}
 * 取得专属列值再交给对应渲染器，读取侧由门面再送回 {@code McpCapabilityRecordBO.normalized(...)} 复核。
 * Injected by the guarded facades of the five {@code gateway_mcp_*_draft} tables under the bean name
 * {@code mcpCapabilityDraftPersistenceConverter}; a caller first takes the dedicated column values through
 * {@code McpCapabilityBinding.of(kind)} and hands them to the matching renderer, while the read side is re-checked by the facade
 * through {@code McpCapabilityRecordBO.normalized(...)}.
 */
@Slf4j
@Component("mcpCapabilityDraftPersistenceConverter")
@RequiredArgsConstructor
public class McpCapabilityDraftPersistenceConverter {

    /**
     * 中文说明：表示 CONTENT_MAP 这一固定值，声明 jsonb {@code content} 列的读取形态，与被替换边界
     * {@code new TypeReference<Map<String, Object>>()} 完全一致。
     * English summary: Represents the fixed content-map value, the read shape of the jsonb {@code content} column, identical to the
     * {@code new TypeReference<Map<String, Object>>()} of the replaced persistence boundary.
     *
     * 用法 / Usage: 仅由本类的映射解码使用。/ Used only by the map decoding of this class.
     */
    private static final TypeReference<Map<String, Object>> CONTENT_MAP =
            new TypeReference<>() {
            };

    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：执行 toResource 操作，把资源草稿行投影为 {@code RESOURCE} 能力载体，
     * 等价旧 {@code SELECT id, gateway_group_id, server_id, resource_name AS capability_name,
     * content::text, enabled, revision FROM gateway_mcp_resource_draft} 的行映射器。
     * English summary: Executes the toResource operation, projecting a resource draft row onto the {@code RESOURCE} capability carrier,
     * equivalent to the row mapper of the legacy
     * {@code SELECT id, gateway_group_id, server_id, resource_name AS capability_name, content::text, enabled, revision FROM
     * gateway_mcp_resource_draft}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpCapabilityDraftPersistenceConverter.toResource(row)}。传入 {@code null}
     * 返回 {@code null}。/ Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model.
     * @return 返回 业务载体；returns the business carrier.
     */
    public McpCapabilityRecordBO toResource(McpResourceDraftPO row) {
        if (row == null) {
            log.debug("gateway_mcp_resource_draft row is absent; no business carrier projected");
            return null;
        }
        return record(
                McpCapabilityKindEnum.RESOURCE,
                row.getId(),
                row.getGatewayGroupId(),
                row.getServerId(),
                row.getResourceName(),
                row.getContent(),
                row.getEnabled(),
                row.getRevision()
        );
    }

    /**
     * 中文说明：执行 toResourceTemplate 操作，把资源模板草稿行投影为 {@code RESOURCE_TEMPLATE} 能力载体，
     * 名称列按旧语句取 {@code template_name}。
     * English summary: Executes the toResourceTemplate operation, projecting a resource template draft row onto the
     * {@code RESOURCE_TEMPLATE} capability carrier with the legacy {@code template_name} name column.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpCapabilityDraftPersistenceConverter.toResourceTemplate(row)}。传入
     * {@code null} 返回 {@code null}。/ Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model.
     * @return 返回 业务载体；returns the business carrier.
     */
    public McpCapabilityRecordBO toResourceTemplate(McpResourceTemplateDraftPO row) {
        if (row == null) {
            log.debug("gateway_mcp_resource_template_draft row is absent; no business carrier projected");
            return null;
        }
        return record(
                McpCapabilityKindEnum.RESOURCE_TEMPLATE,
                row.getId(),
                row.getGatewayGroupId(),
                row.getServerId(),
                row.getTemplateName(),
                row.getContent(),
                row.getEnabled(),
                row.getRevision()
        );
    }

    /**
     * 中文说明：执行 toPrompt 操作，把提示词草稿行投影为 {@code PROMPT} 能力载体，名称列按旧语句取
     * {@code prompt_name}。
     * English summary: Executes the toPrompt operation, projecting a prompt draft row onto the {@code PROMPT} capability carrier with the
     * legacy {@code prompt_name} name column.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpCapabilityDraftPersistenceConverter.toPrompt(row)}。传入 {@code null}
     * 返回 {@code null}。/ Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model.
     * @return 返回 业务载体；returns the business carrier.
     */
    public McpCapabilityRecordBO toPrompt(McpPromptDraftPO row) {
        if (row == null) {
            log.debug("gateway_mcp_prompt_draft row is absent; no business carrier projected");
            return null;
        }
        return record(
                McpCapabilityKindEnum.PROMPT,
                row.getId(),
                row.getGatewayGroupId(),
                row.getServerId(),
                row.getPromptName(),
                row.getContent(),
                row.getEnabled(),
                row.getRevision()
        );
    }

    /**
     * 中文说明：执行 toTaskPolicy 操作，把任务策略草稿行投影为 {@code TASK_POLICY} 能力载体，
     * 名称列按旧语句取 {@code tool_name}，该种类没有任何专属列。
     * English summary: Executes the toTaskPolicy operation, projecting a task policy draft row onto the {@code TASK_POLICY} capability
     * carrier with the legacy {@code tool_name} name column and no dedicated column at all.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpCapabilityDraftPersistenceConverter.toTaskPolicy(row)}。传入
     * {@code null} 返回 {@code null}。/ Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model.
     * @return 返回 业务载体；returns the business carrier.
     */
    public McpCapabilityRecordBO toTaskPolicy(McpTaskPolicyDraftPO row) {
        if (row == null) {
            log.debug("gateway_mcp_task_policy_draft row is absent; no business carrier projected");
            return null;
        }
        return record(
                McpCapabilityKindEnum.TASK_POLICY,
                row.getId(),
                row.getGatewayGroupId(),
                row.getServerId(),
                row.getToolName(),
                row.getContent(),
                row.getEnabled(),
                row.getRevision()
        );
    }

    /**
     * 中文说明：执行 toAppBinding 操作，把应用绑定草稿行投影为 {@code APP_BINDING} 能力载体，
     * 名称列按旧语句取 {@code tool_name}，专属列 {@code app_artifact_id} 只写不读（旧投影语句从未选择它）。
     * English summary: Executes the toAppBinding operation, projecting an app binding draft row onto the {@code APP_BINDING} capability
     * carrier with the legacy {@code tool_name} name column; the dedicated {@code app_artifact_id} is write-only because the replaced
     * projection never selected it.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpCapabilityDraftPersistenceConverter.toAppBinding(row)}。传入
     * {@code null} 返回 {@code null}。/ Passing {@code null} returns {@code null}.
     * @param row 参数 行模型；parameter row model.
     * @return 返回 业务载体；returns the business carrier.
     */
    public McpCapabilityRecordBO toAppBinding(McpAppBindingDraftPO row) {
        if (row == null) {
            log.debug("gateway_mcp_app_binding_draft row is absent; no business carrier projected");
            return null;
        }
        return record(
                McpCapabilityKindEnum.APP_BINDING,
                row.getId(),
                row.getGatewayGroupId(),
                row.getServerId(),
                row.getToolName(),
                row.getContent(),
                row.getEnabled(),
                row.getRevision()
        );
    }

    /**
     * 中文说明：执行 resourceDraft 操作，渲染 {@code gateway_mcp_resource_draft} 的插入行，逐列复刻旧
     * {@code INSERT ... (id, gateway_group_id, server_id, resource_name, resource_uri, driver_type,
     * operation_id, remote_mount_id, content, enabled, revision, deleted, created_at, created_by,
     * updated_at, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb ?, 0, FALSE, ...)}：专属位依次是
     * {@code resource_uri}、{@code driver_type}、{@code operation_id}、{@code remote_mount_id}；
     * 业务修订与审计列留空，交由受守卫边界按旧字面量落 {@code revision = 0} 与审计上下文。
     * English summary: Executes the resourceDraft operation, rendering the insert row of {@code gateway_mcp_resource_draft} column by
     * column like the legacy {@code INSERT ... (id, gateway_group_id, server_id, resource_name, resource_uri, driver_type, operation_id,
     * remote_mount_id, content, enabled, revision, deleted, created_at, created_by, updated_at, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?,
     * ?, ?::jsonb ?, 0, FALSE, ...)}: the dedicated positions are {@code resource_uri}, {@code driver_type}, {@code operation_id} and
     * {@code remote_mount_id} in that order; the business revision and the audit columns stay empty because the guarded boundary applies
     * the legacy {@code revision = 0} literal and the audit context.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code mcpCapabilityDraftPersistenceConverter.resourceDraft(carrier, bindingValues)}。
     * @param carrier 参数 业务载体；parameter business carrier.
     * @param binding 参数 按旧占位符顺序给出的专属列值；parameter the dedicated values in the legacy placeholder order.
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public McpResourceDraftPO resourceDraft(
            McpCapabilityRecordBO carrier,
            List<String> binding) {
        McpResourceDraftPO row = resourceChange(carrier, binding);
        row.setId(identifier(McpCapabilityKindEnum.RESOURCE, carrier.getId()));
        row.setGatewayGroupId(identifier(
                McpCapabilityKindEnum.RESOURCE,
                carrier.getGatewayGroupId()
        ));
        row.setServerId(identifier(
                McpCapabilityKindEnum.RESOURCE,
                carrier.getServerId()
        ));
        return row;
    }

    /**
     * 中文说明：执行 resourceChange 操作，渲染 {@code gateway_mcp_resource_draft} 的更新实体：
     * 旧 {@code UPDATE ... SET resource_name = ?, resource_uri = ?, driver_type = ?, operation_id = ?,
     * remote_mount_id = ?, content = ?::jsonb, enabled = ?, revision = revision + 1, updated_at = ?,
     * updated_by = ? WHERE id = ? AND revision = ? AND deleted = FALSE} 从不回写
     * {@code gateway_group_id}/{@code server_id}，故这两列与技术主键在此一律留空，
     * 可空专属列的 SQL NULL 由门面经更新条件显式下推。
     * English summary: Executes the resourceChange operation, rendering the update entity of {@code gateway_mcp_resource_draft}: the legacy
     * {@code UPDATE ... SET resource_name = ?, resource_uri = ?, driver_type = ?, operation_id = ?, remote_mount_id = ?,
     * content = ?::jsonb, enabled = ?, revision = revision + 1, updated_at = ?, updated_by = ? WHERE id = ? AND revision = ? AND
     * deleted = FALSE} never rewrites {@code gateway_group_id} or {@code server_id}, so both columns and the technical key stay unset
     * here and the SQL NULL of a nullable dedicated column is pushed explicitly by the facade through the update condition.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code mcpCapabilityDraftPersistenceConverter.resourceChange(carrier, bindingValues)}。
     * @param carrier 参数 业务载体；parameter business carrier.
     * @param binding 参数 按旧占位符顺序给出的专属列值；parameter the dedicated values in the legacy placeholder order.
     * @return 返回 变更行模型；returns the change row model.
     */
    public McpResourceDraftPO resourceChange(
            McpCapabilityRecordBO carrier,
            List<String> binding) {
        McpResourceDraftPO row = new McpResourceDraftPO();
        row.setResourceName(carrier.getName());
        row.setResourceUri(at(binding, 0));
        row.setDriverType(at(binding, 1));
        row.setOperationId(foreignKey(
                McpCapabilityKindEnum.RESOURCE,
                "operation_id",
                at(binding, 2)
        ));
        row.setRemoteMountId(foreignKey(
                McpCapabilityKindEnum.RESOURCE,
                "remote_mount_id",
                at(binding, 3)
        ));
        row.setContent(contentNode(carrier.getContent()));
        row.setEnabled(carrier.isEnabled());
        return row;
    }

    /**
     * 中文说明：执行 resourceTemplateDraft 操作，渲染 {@code gateway_mcp_resource_template_draft}
     * 的插入行，专属位依次是 {@code uri_template}、{@code driver_type}、{@code operation_id}、
     * {@code remote_mount_id}。
     * English summary: Executes the resourceTemplateDraft operation, rendering the insert row of
     * {@code gateway_mcp_resource_template_draft} whose dedicated positions are {@code uri_template}, {@code driver_type},
     * {@code operation_id} and {@code remote_mount_id} in that order.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code mcpCapabilityDraftPersistenceConverter.resourceTemplateDraft(carrier, bindingValues)}。
     * @param carrier 参数 业务载体；parameter business carrier.
     * @param binding 参数 按旧占位符顺序给出的专属列值；parameter the dedicated values in the legacy placeholder order.
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public McpResourceTemplateDraftPO resourceTemplateDraft(
            McpCapabilityRecordBO carrier,
            List<String> binding) {
        McpResourceTemplateDraftPO row = resourceTemplateChange(carrier, binding);
        row.setId(identifier(
                McpCapabilityKindEnum.RESOURCE_TEMPLATE,
                carrier.getId()
        ));
        row.setGatewayGroupId(identifier(
                McpCapabilityKindEnum.RESOURCE_TEMPLATE,
                carrier.getGatewayGroupId()
        ));
        row.setServerId(identifier(
                McpCapabilityKindEnum.RESOURCE_TEMPLATE,
                carrier.getServerId()
        ));
        return row;
    }

    /**
     * 中文说明：执行 resourceTemplateChange 操作，渲染 {@code gateway_mcp_resource_template_draft}
     * 的更新实体（名称列 {@code template_name}），语义同 {@link #resourceChange}。
     * English summary: Executes the resourceTemplateChange operation, rendering the update entity of
     * {@code gateway_mcp_resource_template_draft} whose name column is {@code template_name}, with the semantics of
     * {@link #resourceChange}.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code mcpCapabilityDraftPersistenceConverter.resourceTemplateChange(carrier, bindingValues)}。
     * @param carrier 参数 业务载体；parameter business carrier.
     * @param binding 参数 按旧占位符顺序给出的专属列值；parameter the dedicated values in the legacy placeholder order.
     * @return 返回 变更行模型；returns the change row model.
     */
    public McpResourceTemplateDraftPO resourceTemplateChange(
            McpCapabilityRecordBO carrier,
            List<String> binding) {
        McpResourceTemplateDraftPO row = new McpResourceTemplateDraftPO();
        row.setTemplateName(carrier.getName());
        row.setUriTemplate(at(binding, 0));
        row.setDriverType(at(binding, 1));
        row.setOperationId(foreignKey(
                McpCapabilityKindEnum.RESOURCE_TEMPLATE,
                "operation_id",
                at(binding, 2)
        ));
        row.setRemoteMountId(foreignKey(
                McpCapabilityKindEnum.RESOURCE_TEMPLATE,
                "remote_mount_id",
                at(binding, 3)
        ));
        row.setContent(contentNode(carrier.getContent()));
        row.setEnabled(carrier.isEnabled());
        return row;
    }

    /**
     * 中文说明：执行 promptDraft 操作，渲染 {@code gateway_mcp_prompt_draft} 的插入行，
     * 专属位依次是 {@code source_type}、{@code operation_id}、{@code remote_mount_id}。
     * English summary: Executes the promptDraft operation, rendering the insert row of {@code gateway_mcp_prompt_draft} whose dedicated
     * positions are {@code source_type}, {@code operation_id} and {@code remote_mount_id}.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code mcpCapabilityDraftPersistenceConverter.promptDraft(carrier, bindingValues)}。
     * @param carrier 参数 业务载体；parameter business carrier.
     * @param binding 参数 按旧占位符顺序给出的专属列值；parameter the dedicated values in the legacy placeholder order.
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public McpPromptDraftPO promptDraft(
            McpCapabilityRecordBO carrier,
            List<String> binding) {
        McpPromptDraftPO row = promptChange(carrier, binding);
        row.setId(identifier(McpCapabilityKindEnum.PROMPT, carrier.getId()));
        row.setGatewayGroupId(identifier(
                McpCapabilityKindEnum.PROMPT,
                carrier.getGatewayGroupId()
        ));
        row.setServerId(identifier(
                McpCapabilityKindEnum.PROMPT,
                carrier.getServerId()
        ));
        return row;
    }

    /**
     * 中文说明：执行 promptChange 操作，渲染 {@code gateway_mcp_prompt_draft} 的更新实体
     * （名称列 {@code prompt_name}），语义同 {@link #resourceChange}。
     * English summary: Executes the promptChange operation, rendering the update entity of {@code gateway_mcp_prompt_draft} whose name
     * column is {@code prompt_name}, with the semantics of {@link #resourceChange}.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code mcpCapabilityDraftPersistenceConverter.promptChange(carrier, bindingValues)}。
     * @param carrier 参数 业务载体；parameter business carrier.
     * @param binding 参数 按旧占位符顺序给出的专属列值；parameter the dedicated values in the legacy placeholder order.
     * @return 返回 变更行模型；returns the change row model.
     */
    public McpPromptDraftPO promptChange(
            McpCapabilityRecordBO carrier,
            List<String> binding) {
        McpPromptDraftPO row = new McpPromptDraftPO();
        row.setPromptName(carrier.getName());
        row.setSourceType(at(binding, 0));
        row.setOperationId(foreignKey(
                McpCapabilityKindEnum.PROMPT,
                "operation_id",
                at(binding, 1)
        ));
        row.setRemoteMountId(foreignKey(
                McpCapabilityKindEnum.PROMPT,
                "remote_mount_id",
                at(binding, 2)
        ));
        row.setContent(contentNode(carrier.getContent()));
        row.setEnabled(carrier.isEnabled());
        return row;
    }

    /**
     * 中文说明：执行 taskPolicyDraft 操作，渲染 {@code gateway_mcp_task_policy_draft} 的插入行；
     * 该种类没有任何专属列（旧实现的 {@code McpCapabilityBinding.none()}）。
     * English summary: Executes the taskPolicyDraft operation, rendering the insert row of
     * {@code gateway_mcp_task_policy_draft}; this kind carries no dedicated column at all, the legacy
     * {@code McpCapabilityBinding.none()}.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code mcpCapabilityDraftPersistenceConverter.taskPolicyDraft(carrier, bindingValues)}。
     * @param carrier 参数 业务载体；parameter business carrier.
     * @param binding 参数 该种类为空集；parameter the empty value list of this kind.
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public McpTaskPolicyDraftPO taskPolicyDraft(
            McpCapabilityRecordBO carrier,
            List<String> binding) {
        McpTaskPolicyDraftPO row = taskPolicyChange(carrier, binding);
        row.setId(identifier(
                McpCapabilityKindEnum.TASK_POLICY,
                carrier.getId()
        ));
        row.setGatewayGroupId(identifier(
                McpCapabilityKindEnum.TASK_POLICY,
                carrier.getGatewayGroupId()
        ));
        row.setServerId(identifier(
                McpCapabilityKindEnum.TASK_POLICY,
                carrier.getServerId()
        ));
        return row;
    }

    /**
     * 中文说明：执行 taskPolicyChange 操作，渲染 {@code gateway_mcp_task_policy_draft} 的更新实体，
     * 旧语句只覆写 {@code tool_name}、{@code content} 与 {@code enabled}。
     * English summary: Executes the taskPolicyChange operation, rendering the update entity of
     * {@code gateway_mcp_task_policy_draft}, which the legacy statement limited to {@code tool_name}, {@code content} and
     * {@code enabled}.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code mcpCapabilityDraftPersistenceConverter.taskPolicyChange(carrier, bindingValues)}。
     * @param carrier 参数 业务载体；parameter business carrier.
     * @param binding 参数 该种类为空集；parameter the empty value list of this kind.
     * @return 返回 变更行模型；returns the change row model.
     */
    public McpTaskPolicyDraftPO taskPolicyChange(
            McpCapabilityRecordBO carrier,
            List<String> binding) {
        McpTaskPolicyDraftPO row = new McpTaskPolicyDraftPO();
        row.setToolName(carrier.getName());
        row.setContent(contentNode(carrier.getContent()));
        row.setEnabled(carrier.isEnabled());
        return row;
    }

    /**
     * 中文说明：执行 appBindingDraft 操作，渲染 {@code gateway_mcp_app_binding_draft} 的插入行，
     * 唯一专属位是外键 {@code app_artifact_id}（旧语句按必填文本给出，迁移后是 {@code bigint} 外键）。
     * English summary: Executes the appBindingDraft operation, rendering the insert row of
     * {@code gateway_mcp_app_binding_draft} whose only dedicated position is the foreign key {@code app_artifact_id}, a required text in
     * the replaced statement and a {@code bigint} foreign key after migration.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code mcpCapabilityDraftPersistenceConverter.appBindingDraft(carrier, bindingValues)}。
     * @param carrier 参数 业务载体；parameter business carrier.
     * @param binding 参数 按旧占位符顺序给出的专属列值；parameter the dedicated values in the legacy placeholder order.
     * @return 返回 待插入行模型；returns the row model to insert.
     */
    public McpAppBindingDraftPO appBindingDraft(
            McpCapabilityRecordBO carrier,
            List<String> binding) {
        McpAppBindingDraftPO row = appBindingChange(carrier, binding);
        row.setId(identifier(
                McpCapabilityKindEnum.APP_BINDING,
                carrier.getId()
        ));
        row.setGatewayGroupId(identifier(
                McpCapabilityKindEnum.APP_BINDING,
                carrier.getGatewayGroupId()
        ));
        row.setServerId(identifier(
                McpCapabilityKindEnum.APP_BINDING,
                carrier.getServerId()
        ));
        return row;
    }

    /**
     * 中文说明：执行 appBindingChange 操作，渲染 {@code gateway_mcp_app_binding_draft} 的更新实体，
     * 旧语句覆写 {@code tool_name}、{@code app_artifact_id}、{@code content} 与 {@code enabled}。
     * English summary: Executes the appBindingChange operation, rendering the update entity of
     * {@code gateway_mcp_app_binding_draft}, whose legacy SET clause covered {@code tool_name}, {@code app_artifact_id},
     * {@code content} and {@code enabled}.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code mcpCapabilityDraftPersistenceConverter.appBindingChange(carrier, bindingValues)}。
     * @param carrier 参数 业务载体；parameter business carrier.
     * @param binding 参数 按旧占位符顺序给出的专属列值；parameter the dedicated values in the legacy placeholder order.
     * @return 返回 变更行模型；returns the change row model.
     */
    public McpAppBindingDraftPO appBindingChange(
            McpCapabilityRecordBO carrier,
            List<String> binding) {
        McpAppBindingDraftPO row = new McpAppBindingDraftPO();
        row.setToolName(carrier.getName());
        row.setAppArtifactId(foreignKey(
                McpCapabilityKindEnum.APP_BINDING,
                "app_artifact_id",
                at(binding, 0)
        ));
        row.setContent(contentNode(carrier.getContent()));
        row.setEnabled(carrier.isEnabled());
        return row;
    }

    /**
     * 中文说明：执行 contentNode 操作，把内容映射渲染为 jsonb 列值，与被替换边界
     * {@code writeValueAsString(...)} 的规范形态一致；缺失时按旧 NOT NULL 列语义写入 JSON null
     * 而非 SQL NULL，编码失败沿用「MCP persistence value cannot be serialized」文案。
     * English summary: Executes the contentNode operation, rendering the content map as the jsonb column value in the same canonical shape
     * as the replaced {@code writeValueAsString(...)}; an absent value writes JSON null into the NOT NULL column instead of SQL NULL, and
     * an encoding failure keeps the {@code MCP persistence value cannot be serialized} message.
     *
     * 用法 / Usage: 由各渲染器调用。/ Invoked by the renderers.
     * @param value 参数 内容映射；parameter the content map.
     * @return 返回 jsonb 列值；returns the jsonb column value.
     */
    public JsonNode contentNode(Map<String, Object> value) {
        if (value == null) {
            return objectMapper.nullNode();
        }
        try {
            return objectMapper.valueToTree(value);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException(
                    "MCP persistence value cannot be serialized",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 content 操作，把 jsonb 列值解码为不可变映射，逐字复刻旧
     * {@code Map.copyOf(readValue(content::text, MAP))} 语义；列值为 SQL NULL 时如实返回 {@code null}，
     * 交由 {@code McpCapabilityRecordBO.normalized(...)} 按旧文案拒绝，解码失败沿用
     * 「stored MCP persistence value is invalid」文案。
     * English summary: Executes the content operation, decoding a jsonb column value into an immutable map exactly like the replaced
     * {@code Map.copyOf(readValue(content::text, MAP))}; an SQL NULL column value truthfully yields {@code null} so
     * {@code McpCapabilityRecordBO.normalized(...)} rejects it with the legacy wording, and a decoding failure keeps the
     * {@code stored MCP persistence value is invalid} message.
     *
     * 用法 / Usage: 由各读取投影调用。/ Invoked by the read projections.
     * @param value 参数 jsonb 列值；parameter the jsonb column value.
     * @return 返回 不可变映射或 {@code null}；returns the immutable map or {@code null}.
     */
    public Map<String, Object> content(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return Map.copyOf(objectMapper.convertValue(value, CONTENT_MAP));
        } catch (IllegalArgumentException failure) {
            throw new IllegalStateException(
                    "stored MCP persistence value is invalid",
                    failure
            );
        }
    }

    /**
     * 中文说明：按旧投影语句的字段顺序建立能力载体，并立即经
     * {@code McpCapabilityRecordBO.normalized(...)} 复核必填与修订非负守护。
     * English summary: Builds a capability carrier in the field order of the replaced projection and re-checks it immediately through
     * {@code McpCapabilityRecordBO.normalized(...)}.
     * @param kind 参数 能力种类；parameter the capability kind.
     * @param id 参数 主键列值；parameter the key column value.
     * @param gatewayGroupId 参数 分组外键列值；parameter the Group foreign key.
     * @param serverId 参数 服务外键列值；parameter the Server foreign key.
     * @param name 参数 名称列值；parameter the name column value.
     * @param content 参数 jsonb 内容列值；parameter the jsonb content column value.
     * @param enabled 参数 启用列值；parameter the enabled column value.
     * @param revision 参数 修订列值；parameter the revision column value.
     * @return 返回 复核后的能力载体；returns the re-validated capability carrier.
     */
    private McpCapabilityRecordBO record(
            McpCapabilityKindEnum kind,
            Long id,
            Long gatewayGroupId,
            Long serverId,
            String name,
            JsonNode content,
            Boolean enabled,
            Long revision) {
        return McpCapabilityRecordBO.normalized(
                kind,
                text(id),
                text(gatewayGroupId),
                text(serverId),
                name,
                content(content),
                enabled != null && enabled,
                revision == null ? 0L : revision
        );
    }

    /**
     * 中文说明：执行 at 操作，按旧占位符顺序取用 {@code McpCapabilityBinding} 给出的专属列值；
     * 必填缺失已由绑定侧按「capability content <key> is required」抛出，越界只可能是没有专属列的种类。
     * English summary: Executes the at operation, taking one dedicated value from {@code McpCapabilityBinding} in the legacy placeholder
     * order; a missing required value was already refused by the binding side with "capability content <key> is required", and an
     * out-of-range position can only belong to a kind without dedicated columns.
     * @param binding 参数 专属列值；parameter the dedicated values.
     * @param index 参数 位置；parameter the position.
     * @return 返回 列值或 {@code null}；returns the column value or {@code null}.
     */
    private static String at(List<String> binding, int index) {
        return binding == null || index >= binding.size()
                ? null
                : binding.get(index);
    }

    /**
     * 中文说明：执行 text 操作，把 MP 的 {@code bigint} 列按十进制文本投影，{@code null} 保持 {@code null}。
     * English summary: Executes the text operation, projecting an MP {@code bigint} column as decimal text; {@code null} stays
     * {@code null}.
     * @param value 参数 列值；parameter the column value.
     * @return 返回 十进制文本；returns the decimal text.
     */
    private static String text(Long value) {
        return value == null ? null : Long.toString(value);
    }

    /**
     * 中文说明：执行 identifier 操作，把业务十进制标识解析为所属表 {@code bigint} 主键或外键；
     * 空白视为未生成、交由 {@code ASSIGN_ID} 补位，非法文本如实抛
     * {@code <table> identifier must be decimal: <value>}。
     * English summary: Executes the identifier operation, parsing a decimal business identifier into the {@code bigint} key or foreign key
     * of its own table; a blank value means "not generated yet" so {@code ASSIGN_ID} fills it, while a malformed value is refused
     * truthfully as {@code <table> identifier must be decimal: <value>}.
     * @param kind 参数 能力种类，决定所属表；parameter the capability kind selecting the table.
     * @param value 参数 业务标识；parameter the business identifier.
     * @return 返回 列值或 {@code null}；returns the column value or {@code null}.
     */
    private static Long identifier(McpCapabilityKindEnum kind, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(
                    kind.table() + " identifier must be decimal: " + value,
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 foreignKey 操作，把 {@code McpCapabilityBinding} 给出的可选外键文本换算为
     * {@code bigint} 列值；空白如实返回 {@code null}（旧语句写下 SQL NULL），非十进制文本按
     * {@code <table>.<column> must be decimal: <value>} 如实拒绝，绝不写出 {@code NULL} 冒充外键。
     * English summary: Executes the foreignKey operation, converting the optional foreign-key text of {@code McpCapabilityBinding} into
     * the {@code bigint} column value; a blank value truthfully yields {@code null}, which is the SQL NULL the legacy statement wrote,
     * while a non-decimal text is refused as {@code <table>.<column> must be decimal: <value>} instead of disguising {@code NULL} as a
     * foreign key.
     * @param kind 参数 能力种类，决定所属表；parameter the capability kind selecting the table.
     * @param column 参数 列名；parameter the column name.
     * @param value 参数 外键文本；parameter the foreign-key text.
     * @return 返回 列值或 {@code null}；returns the column value or {@code null}.
     */
    private static Long foreignKey(
            McpCapabilityKindEnum kind,
            String column,
            String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(
                    kind.table() + "." + column + " must be decimal: " + value,
                    failure
            );
        }
    }
}
