package top.egon.cola.component.yuheng.admin.mcp.repository.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.common.mybatis.model.EgonModel;
import top.egon.cola.component.yuheng.admin.mcp.converter.McpCapabilityDraftPersistenceConverter;
import top.egon.cola.component.yuheng.admin.mcp.domain.bo.McpCapabilityDraftBO;
import top.egon.cola.component.yuheng.admin.mcp.domain.bo.McpCapabilityRecordBO;
import top.egon.cola.component.yuheng.admin.mcp.domain.dto.McpCapabilityDraftMutationDTO;
import top.egon.cola.component.yuheng.admin.mcp.domain.enums.McpCapabilityKindEnum;
import top.egon.cola.component.yuheng.admin.mcp.domain.po.McpAppBindingDraftPO;
import top.egon.cola.component.yuheng.admin.mcp.domain.po.McpPromptDraftPO;
import top.egon.cola.component.yuheng.admin.mcp.domain.po.McpResourceDraftPO;
import top.egon.cola.component.yuheng.admin.mcp.domain.po.McpResourceTemplateDraftPO;
import top.egon.cola.component.yuheng.admin.mcp.domain.po.McpTaskPolicyDraftPO;
import top.egon.cola.component.yuheng.admin.mcp.repository.McpCapabilityDraftRepository;
import top.egon.cola.component.yuheng.admin.mcp.repository.mp.McpAppBindingDraftPersistenceRepository;
import top.egon.cola.component.yuheng.admin.mcp.repository.mp.McpPromptDraftPersistenceRepository;
import top.egon.cola.component.yuheng.admin.mcp.repository.mp.McpResourceDraftPersistenceRepository;
import top.egon.cola.component.yuheng.admin.mcp.repository.mp.McpResourceTemplateDraftPersistenceRepository;
import top.egon.cola.component.yuheng.admin.mcp.repository.mp.McpTaskPolicyDraftPersistenceRepository;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminRevisionConflictException;

/**
 * 中文说明：{@code MpMcpCapabilityDraftRepository} 是 MCP 控制面能力草稿的 MyBatis-Plus 门面存储，逐方法取代被退役的
 * 遗留持久化载体：旧实现按 {@code McpCapabilityKindEnum} 把同一条模板语句实例化到
 * {@code gateway_mcp_resource_draft}、{@code gateway_mcp_resource_template_draft}、{@code gateway_mcp_prompt_draft}、
 * {@code gateway_mcp_task_policy_draft}、{@code gateway_mcp_app_binding_draft} 五张表，本门面同样按种类分派，
 * 但每一次读写都走各自的受守卫 {@code EgonColaRepository} 边界（同租户过滤、仅活跃行 {@code deleted_at IS NULL}、
 * 技术 {@code version} 乐观锁）；{@code load} 保留旧实现「五种能力逐个 {@code SELECT ... WHERE gateway_group_id = ?
 * AND deleted = FALSE ORDER BY server_id, <capability_name>} 后装入 {@code EnumMap}」的形态，缺项也是空列表而非缺席；
 * {@code save} 保留「先按 {@code (id, revision)} 做 UPDATE，未命中再读当前修订决定抛冲突还是以 {@code revision = 0}
 * 插入」的 upsert 语义、{@code expectedRevision must not be negative} 守护，以及专属列取值先于任何写入发生的顺序——
 * 专属列值一律由 {@code McpCapabilityBinding.of(kind).values(content)} 按旧占位符顺序给出，
 * 因此 {@code capability content <key> is required} 与旧实现一字不差；UPDATE 的 {@code SET} 只覆盖旧语句覆盖的
 * 名称列、专属列、{@code content} 与 {@code enabled}（{@code gateway_group_id}/{@code server_id} 绝不被回写，
 * 变更实体上如实留空，缺席的可空专属列经更新条件显式下推 SQL NULL），业务 {@code revision} 只在命中时自增；
 * {@code softDelete} 保留同一组 {@code (id, revision)} 定位谓词，但受守卫边界只提供版本化软删；
 * 列与类型映射只经 {@code mcpCapabilityDraftPersistenceConverter} 完成，读写两侧的载体一律重新经
 * {@code McpCapabilityRecordBO.normalized(...)} 复核旧构造器不变量，公开端口不泄漏行模型、能力草稿行或 DAO。
 *
 * English summary: {@code MpMcpCapabilityDraftRepository} is the MyBatis-Plus facade store of MCP control-plane capability drafts,
 * replacing the retired legacy persistence carrier method by method: the legacy implementation instantiated one statement template over
 * the five tables {@code gateway_mcp_resource_draft}, {@code gateway_mcp_resource_template_draft}, {@code gateway_mcp_prompt_draft},
 * {@code gateway_mcp_task_policy_draft} and {@code gateway_mcp_app_binding_draft} per {@code McpCapabilityKindEnum}, and this facade
 * dispatches the same way while every read and write goes through the matching guarded {@code EgonColaRepository} boundary (same-tenant
 * filtering, active rows only under {@code deleted_at IS NULL}, the technical {@code version} optimistic lock); {@code load} keeps the
 * legacy shape of running one {@code SELECT ... WHERE gateway_group_id = ? AND deleted = FALSE ORDER BY server_id, <capability_name>}
 * per kind into an {@code EnumMap}, where a kind without rows is an empty list rather than an absent entry; {@code save} keeps the
 * legacy upsert order of first UPDATE-ing by {@code (id, revision)} and then either raising a conflict or inserting at
 * {@code revision = 0} depending on the revision read back, the {@code expectedRevision must not be negative} guard, and the order in
 * which the dedicated column values are taken before any write happens - those values always come from
 * {@code McpCapabilityBinding.of(kind).values(content)} in the legacy placeholder order, so {@code capability content <key> is required}
 * reads exactly as before; the UPDATE overwrites only the columns the legacy statement listed, namely the name column, the dedicated
 * columns, {@code content} and {@code enabled}, because {@code gateway_group_id} and {@code server_id} were never written back (they stay
 * unset on the change entity and an absent nullable dedicated value is pushed as an explicit SQL NULL through the update condition),
 * while the business {@code revision} only increments on a hit; {@code softDelete} keeps the same {@code (id, revision)} locating
 * predicates but the guarded boundary only offers a versioned soft delete; column and type mapping happens only in
 * {@code mcpCapabilityDraftPersistenceConverter}, both boundaries re-check carriers through
 * {@code McpCapabilityRecordBO.normalized(...)}, and the public port leaks neither a row model, a capability draft row nor a DAO.
 *
 * 用法 / Usage: 通过业务端口 {@code McpCapabilityDraftRepository} 由 Spring 容器注入；遗留实现没有声明事务边界，
 * 「读当前修订—判定冲突—写入」的竞态由调用方事务与技术 {@code version} 乐观锁共同收敛，故本门面不声明
 * {@code @Transactional}；冲突一律按旧契约抛 {@code GatewayAdminRevisionConflictException}，读不到行时携带 {@code -1}。
 * Inject it through the business port {@code McpCapabilityDraftRepository}; the legacy carrier declared no transaction boundary and the
 * read-current-revision, decide-conflict, write race is收敛 by the caller's transaction together with the technical {@code version}
 * optimistic lock, so this facade declares no {@code @Transactional}; conflicts always surface as the legacy
 * {@code GatewayAdminRevisionConflictException}, carrying {@code -1} when no row is visible.
 */
@Slf4j
@Repository("mpMcpCapabilityDraftRepository")
@RequiredArgsConstructor
@Validated
public class MpMcpCapabilityDraftRepository
        implements McpCapabilityDraftRepository {

    /**
     * 中文说明：资源草稿行的受守卫持久化仓储。
     * English summary: The guarded persistence store for resource draft rows.
     */
    @Qualifier("mcpResourceDraftPersistenceRepository")
    private final McpResourceDraftPersistenceRepository resourceDraftPersistenceRepository;

    /**
     * 中文说明：资源模板草稿行的受守卫持久化仓储。
     * English summary: The guarded persistence store for resource template draft rows.
     */
    @Qualifier("mcpResourceTemplateDraftPersistenceRepository")
    private final McpResourceTemplateDraftPersistenceRepository resourceTemplateDraftPersistenceRepository;

    /**
     * 中文说明：提示词草稿行的受守卫持久化仓储。
     * English summary: The guarded persistence store for prompt draft rows.
     */
    @Qualifier("mcpPromptDraftPersistenceRepository")
    private final McpPromptDraftPersistenceRepository promptDraftPersistenceRepository;

    /**
     * 中文说明：任务策略草稿行的受守卫持久化仓储。
     * English summary: The guarded persistence store for task policy draft rows.
     */
    @Qualifier("mcpTaskPolicyDraftPersistenceRepository")
    private final McpTaskPolicyDraftPersistenceRepository taskPolicyDraftPersistenceRepository;

    /**
     * 中文说明：应用绑定草稿行的受守卫持久化仓储。
     * English summary: The guarded persistence store for app binding draft rows.
     */
    @Qualifier("mcpAppBindingDraftPersistenceRepository")
    private final McpAppBindingDraftPersistenceRepository appBindingDraftPersistenceRepository;

    /**
     * 中文说明：{@code McpCapabilityRecordBO} 与五种能力草稿行模型之间的双向转换器。
     * English summary: The bidirectional converter between McpCapabilityRecordBO and the five capability draft row models.
     */
    @Qualifier("mcpCapabilityDraftPersistenceConverter")
    private final McpCapabilityDraftPersistenceConverter capabilityDraftPersistenceConverter;

    /**
     * 中文说明：执行 load 操作；等价遗留实现：分组标识先按旧文案 {@code gatewayGroupId is required} 规范化，
     * 再按 {@code McpCapabilityKindEnum.values()} 的声明顺序逐种读取，装入 {@code EnumMap} 后交给
     * {@code McpCapabilityDraftBO.normalized(...)}；迁移后 {@code gateway_group_id} 是 {@code bigint} 外键，
     * 无法表示的文本按旧「等值比较不成立」的结果如实读为五种空列表。
     * English summary: Executes the load operation; equivalent to the legacy implementation: the group identifier is normalized first with
     * the legacy {@code gatewayGroupId is required} wording, every kind of {@code McpCapabilityKindEnum.values()} is read in declaration
     * order, and the {@code EnumMap} is handed to {@code McpCapabilityDraftBO.normalized(...)}; the group column is a {@code bigint}
     * foreign key after migration, so a text that cannot be represented truthfully reads as five empty lists - the legacy comparison
     * never matched.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mpMcpCapabilityDraftRepository.load(gatewayGroupId)}。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id.
     * @return 返回 load 的处理结果；returns every capability draft of one Gateway Group.
     */
    @Override
    public McpCapabilityDraftBO load(String gatewayGroupId) {
        String group = required(gatewayGroupId, "gatewayGroupId");
        Long key = columnValue(group);
        EnumMap<McpCapabilityKindEnum, List<McpCapabilityRecordBO>> values =
                new EnumMap<>(McpCapabilityKindEnum.class);
        for (McpCapabilityKindEnum kind : McpCapabilityKindEnum.values()) {
            values.put(kind, key == null ? List.of() : loadRows(kind, key));
        }
        return McpCapabilityDraftBO.normalized(group, values);
    }

    /**
     * 中文说明：执行 save 操作；入参守护与顺序沿用旧实现（{@code draft} 必填、{@code expectedRevision} 非负、
     * 载体复核、专属列取值、{@code now} 与 {@code actor} 必填），随后按能力种类分派到各自的受守卫边界，
     * 语义与旧模板语句一致：命中 {@code (id, revision)} 即按 {@code expectedRevision + 1} 返回，
     * 否则读回当前修订，存在修订或期望修订非 0 时抛 {@code GatewayAdminRevisionConflictException}，
     * 只有「无行且期望修订为 0」才以 {@code revision = 0} 受守卫插入并返回 0。
     * English summary: Executes the save operation; the argument guards and their order follow the legacy implementation ({@code draft}
     * required, non-negative {@code expectedRevision}, the carrier re-check, the dedicated column values, required {@code now} and
     * {@code actor}) and the work is then dispatched per capability kind onto its guarded boundary, with the semantics of the legacy
     * statement template: a hit on {@code (id, revision)} returns {@code expectedRevision + 1}, otherwise the current revision is read
     * back, a stored revision or a non-zero expected revision raises the legacy {@code GatewayAdminRevisionConflictException}, and only
     * "no row plus expected revision 0" inserts through the guarded boundary at {@code revision = 0} and returns 0.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code mpMcpCapabilityDraftRepository.save(recordBO, expectedRevision, actor, now)}。
     * @param draft 参数 草稿；parameter draft.
     * @param expectedRevision 参数 expectedRevision；parameter expected revision.
     * @param actor 参数 actor；parameter actor.
     * @param now 参数 now；parameter now.
     * @return 返回 save 的处理结果；returns the mutated capability identifier and its new revision.
     */
    @Override
    public McpCapabilityDraftMutationDTO save(
            McpCapabilityRecordBO draft,
            long expectedRevision,
            AdminActor actor,
            Instant now) {
        Objects.requireNonNull(draft, "draft");
        validateExpectedRevision(expectedRevision);
        McpCapabilityRecordBO carrier = renormalize(draft);
        McpCapabilityKindEnum kind = carrier.getKind();
        List<String> dedicated = McpCapabilityBinding.of(kind)
                .values(carrier.getContent());
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(actor, "actor");
        Long key = columnValue(carrier.getId());
        return switch (kind) {
            case RESOURCE -> saveResource(carrier, key, expectedRevision, dedicated);
            case RESOURCE_TEMPLATE ->
                    saveResourceTemplate(carrier, key, expectedRevision, dedicated);
            case PROMPT -> savePrompt(carrier, key, expectedRevision, dedicated);
            case TASK_POLICY ->
                    saveTaskPolicy(carrier, key, expectedRevision, dedicated);
            case APP_BINDING ->
                    saveAppBinding(carrier, key, expectedRevision, dedicated);
        };
    }

    /**
     * 中文说明：执行 softDelete 操作；等价遗留「UPDATE %s SET deleted = TRUE, enabled = FALSE,
     * revision = revision + 1 WHERE id = ? AND revision = ? AND deleted = FALSE」，未命中一行时与旧实现一样
     * 读回当前修订并抛 {@code GatewayAdminRevisionConflictException}（读不到行为 {@code -1}），命中则返回
     * {@code expectedRevision + 1}；受守卫边界不接受带业务谓词的批量删除，只提供按实体的版本化软删，
     * 故删除落为「按同一组谓词读定位 + {@code removeById(实体)}」，缺席的 {@code enabled = FALSE}
     * 与自增的 {@code revision} 不再有意义——软删行对所有活跃读取都不可见。
     * English summary: Executes the softDelete operation; equivalent to the legacy
     * {@code UPDATE %s SET deleted = TRUE, enabled = FALSE, revision = revision + 1 WHERE id = ? AND revision = ? AND deleted = FALSE}:
     * when no row is affected the current revision is read back and the legacy {@code GatewayAdminRevisionConflictException} is raised
     * ({@code -1} when nothing is visible), otherwise {@code expectedRevision + 1} is returned; the guarded boundary accepts no bulk
     * delete carrying business predicates and only offers a versioned soft delete by entity, so the removal becomes "locate by the very
     * same predicates plus {@code removeById(entity)}", and the omitted {@code enabled = FALSE} and the incremented {@code revision}
     * lose their meaning because a soft-deleted row is invisible to every active read.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code mpMcpCapabilityDraftRepository.softDelete(kind, id, expectedRevision, actor, now)}。
     * @param kind 参数 能力种类；parameter capability kind.
     * @param id 参数 草稿Id；parameter draft id.
     * @param expectedRevision 参数 expectedRevision；parameter expected revision.
     * @param actor 参数 actor；parameter actor.
     * @param now 参数 now；parameter now.
     * @return 返回 softDelete 的处理结果；returns the soft-deleted capability identifier and its new revision.
     */
    @Override
    public McpCapabilityDraftMutationDTO softDelete(
            McpCapabilityKindEnum kind,
            String id,
            long expectedRevision,
            AdminActor actor,
            Instant now) {
        validateExpectedRevision(expectedRevision);
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(actor, "actor");
        required(id, "id");
        Long key = columnValue(id);
        return switch (kind) {
            case RESOURCE -> removeResource(key, id, expectedRevision);
            case RESOURCE_TEMPLATE ->
                    removeResourceTemplate(key, id, expectedRevision);
            case PROMPT -> removePrompt(key, id, expectedRevision);
            case TASK_POLICY -> removeTaskPolicy(key, id, expectedRevision);
            case APP_BINDING -> removeAppBinding(key, id, expectedRevision);
        };
    }

    /**
     * 中文说明：按能力种类分派旧模板 {@code SELECT ... WHERE gateway_group_id = ? AND deleted = FALSE
     * ORDER BY server_id, <capability_name>}，追加技术 id 升序只为并列行给出稳定次序；
     * 每行由转换器按旧投影列表建立已复核的载体。
     * English summary: Dispatches the legacy statement template {@code SELECT ... WHERE gateway_group_id = ? AND deleted = FALSE
     * ORDER BY server_id, <capability_name>} per capability kind, the appended ascending technical id only stabilizing tied rows; every
     * row becomes a re-validated carrier built by the converter from the legacy projection list.
     * @param kind 参数 能力种类；parameter the capability kind.
     * @param group 参数 分组外键；parameter the Group foreign key.
     * @return 返回 该种类的能力载体；returns the capability carriers of one kind.
     */
    private List<McpCapabilityRecordBO> loadRows(
            McpCapabilityKindEnum kind,
            Long group) {
        return switch (kind) {
            case RESOURCE ->
                    resourceDraftPersistenceRepository.list(boundPredicate(
                            Wrappers.<McpResourceDraftPO>lambdaQuery()
                                    .eq(
                                            McpResourceDraftPO::getGatewayGroupId,
                                            group
                                    )
                                    .orderByAsc(McpResourceDraftPO::getServerId)
                                    .orderByAsc(McpResourceDraftPO::getResourceName)
                                    .orderByAsc(McpResourceDraftPO::getId))
                    ).stream()
                            .map(capabilityDraftPersistenceConverter::toResource)
                            .toList();
            case RESOURCE_TEMPLATE ->
                    resourceTemplateDraftPersistenceRepository.list(boundPredicate(
                            Wrappers.<McpResourceTemplateDraftPO>lambdaQuery()
                                    .eq(
                                            McpResourceTemplateDraftPO::getGatewayGroupId,
                                            group
                                    )
                                    .orderByAsc(McpResourceTemplateDraftPO::getServerId)
                                    .orderByAsc(McpResourceTemplateDraftPO::getTemplateName)
                                    .orderByAsc(McpResourceTemplateDraftPO::getId))
                    ).stream()
                            .map(capabilityDraftPersistenceConverter::toResourceTemplate)
                            .toList();
            case PROMPT -> promptDraftPersistenceRepository.list(boundPredicate(
                    Wrappers.<McpPromptDraftPO>lambdaQuery()
                            .eq(
                                    McpPromptDraftPO::getGatewayGroupId,
                                    group
                            )
                            .orderByAsc(McpPromptDraftPO::getServerId)
                            .orderByAsc(McpPromptDraftPO::getPromptName)
                            .orderByAsc(McpPromptDraftPO::getId))
            ).stream()
                    .map(capabilityDraftPersistenceConverter::toPrompt)
                    .toList();
            case TASK_POLICY -> taskPolicyDraftPersistenceRepository.list(
                    boundPredicate(
                            Wrappers.<McpTaskPolicyDraftPO>lambdaQuery()
                                    .eq(
                                            McpTaskPolicyDraftPO::getGatewayGroupId,
                                            group
                                    )
                                    .orderByAsc(McpTaskPolicyDraftPO::getServerId)
                                    .orderByAsc(McpTaskPolicyDraftPO::getToolName)
                                    .orderByAsc(McpTaskPolicyDraftPO::getId))
            ).stream()
                    .map(capabilityDraftPersistenceConverter::toTaskPolicy)
                    .toList();
            case APP_BINDING -> appBindingDraftPersistenceRepository.list(
                    boundPredicate(
                            Wrappers.<McpAppBindingDraftPO>lambdaQuery()
                                    .eq(
                                            McpAppBindingDraftPO::getGatewayGroupId,
                                            group
                                    )
                                    .orderByAsc(McpAppBindingDraftPO::getServerId)
                                    .orderByAsc(McpAppBindingDraftPO::getToolName)
                                    .orderByAsc(McpAppBindingDraftPO::getId))
            ).stream()
                    .map(capabilityDraftPersistenceConverter::toAppBinding)
                    .toList();
        };
    }

    /**
     * 中文说明：保存 {@code RESOURCE} 能力草稿，专属列为 {@code resource_uri}、{@code driver_type}
     * 与两个可空外键 {@code operation_id}、{@code remote_mount_id}。
     * English summary: Saves the {@code RESOURCE} capability draft whose dedicated columns are {@code resource_uri},
     * {@code driver_type} and the nullable foreign keys {@code operation_id} and {@code remote_mount_id}.
     * @param carrier 参数 能力载体；parameter the capability carrier.
     * @param key 参数 草稿主键或 {@code null}；parameter the draft key or {@code null}.
     * @param expectedRevision 参数 期望修订；parameter the expected revision.
     * @param dedicated 参数 专属列值；parameter the dedicated values.
     * @return 返回 变更结果；returns the mutation outcome.
     */
    private McpCapabilityDraftMutationDTO saveResource(
            McpCapabilityRecordBO carrier,
            Long key,
            long expectedRevision,
            List<String> dedicated) {
        return saveRow(
                resourceDraftPersistenceRepository,
                McpResourceDraftPO::getId,
                McpResourceDraftPO::getRevision,
                McpResourceDraftPO::setRevision,
                (predicate, row) -> predicate
                        .set(
                                row.getOperationId() == null,
                                McpResourceDraftPO::getOperationId,
                                null
                        )
                        .set(
                                row.getRemoteMountId() == null,
                                McpResourceDraftPO::getRemoteMountId,
                                null
                        ),
                capabilityDraftPersistenceConverter.resourceChange(carrier, dedicated),
                capabilityDraftPersistenceConverter.resourceDraft(carrier, dedicated),
                carrier,
                key,
                expectedRevision
        );
    }

    /**
     * 中文说明：保存 {@code RESOURCE_TEMPLATE} 能力草稿，专属列为 {@code uri_template}、
     * {@code driver_type} 与两个可空外键。
     * English summary: Saves the {@code RESOURCE_TEMPLATE} capability draft whose dedicated columns are {@code uri_template},
     * {@code driver_type} and the two nullable foreign keys.
     * @param carrier 参数 能力载体；parameter the capability carrier.
     * @param key 参数 草稿主键或 {@code null}；parameter the draft key or {@code null}.
     * @param expectedRevision 参数 期望修订；parameter the expected revision.
     * @param dedicated 参数 专属列值；parameter the dedicated values.
     * @return 返回 变更结果；returns the mutation outcome.
     */
    private McpCapabilityDraftMutationDTO saveResourceTemplate(
            McpCapabilityRecordBO carrier,
            Long key,
            long expectedRevision,
            List<String> dedicated) {
        return saveRow(
                resourceTemplateDraftPersistenceRepository,
                McpResourceTemplateDraftPO::getId,
                McpResourceTemplateDraftPO::getRevision,
                McpResourceTemplateDraftPO::setRevision,
                (predicate, row) -> predicate
                        .set(
                                row.getOperationId() == null,
                                McpResourceTemplateDraftPO::getOperationId,
                                null
                        )
                        .set(
                                row.getRemoteMountId() == null,
                                McpResourceTemplateDraftPO::getRemoteMountId,
                                null
                        ),
                capabilityDraftPersistenceConverter.resourceTemplateChange(carrier, dedicated),
                capabilityDraftPersistenceConverter.resourceTemplateDraft(carrier, dedicated),
                carrier,
                key,
                expectedRevision
        );
    }

    /**
     * 中文说明：保存 {@code PROMPT} 能力草稿，专属列为 {@code source_type} 与两个可空外键。
     * English summary: Saves the {@code PROMPT} capability draft whose dedicated columns are {@code source_type} and the two nullable
     * foreign keys.
     * @param carrier 参数 能力载体；parameter the capability carrier.
     * @param key 参数 草稿主键或 {@code null}；parameter the draft key or {@code null}.
     * @param expectedRevision 参数 期望修订；parameter the expected revision.
     * @param dedicated 参数 专属列值；parameter the dedicated values.
     * @return 返回 变更结果；returns the mutation outcome.
     */
    private McpCapabilityDraftMutationDTO savePrompt(
            McpCapabilityRecordBO carrier,
            Long key,
            long expectedRevision,
            List<String> dedicated) {
        return saveRow(
                promptDraftPersistenceRepository,
                McpPromptDraftPO::getId,
                McpPromptDraftPO::getRevision,
                McpPromptDraftPO::setRevision,
                (predicate, row) -> predicate
                        .set(
                                row.getOperationId() == null,
                                McpPromptDraftPO::getOperationId,
                                null
                        )
                        .set(
                                row.getRemoteMountId() == null,
                                McpPromptDraftPO::getRemoteMountId,
                                null
                        ),
                capabilityDraftPersistenceConverter.promptChange(carrier, dedicated),
                capabilityDraftPersistenceConverter.promptDraft(carrier, dedicated),
                carrier,
                key,
                expectedRevision
        );
    }

    /**
     * 中文说明：保存 {@code TASK_POLICY} 能力草稿；该种类没有专属列，旧语句只覆写
     * {@code tool_name}、{@code content} 与 {@code enabled}，故无需下推任何 SQL NULL。
     * English summary: Saves the {@code TASK_POLICY} capability draft; this kind has no dedicated column, the legacy statement only
     * overwrote {@code tool_name}, {@code content} and {@code enabled}, so no SQL NULL has to be pushed.
     * @param carrier 参数 能力载体；parameter the capability carrier.
     * @param key 参数 草稿主键或 {@code null}；parameter the draft key or {@code null}.
     * @param expectedRevision 参数 期望修订；parameter the expected revision.
     * @param dedicated 参数 该种类的空集专属列值；parameter the empty dedicated value list of this kind.
     * @return 返回 变更结果；returns the mutation outcome.
     */
    private McpCapabilityDraftMutationDTO saveTaskPolicy(
            McpCapabilityRecordBO carrier,
            Long key,
            long expectedRevision,
            List<String> dedicated) {
        return saveRow(
                taskPolicyDraftPersistenceRepository,
                McpTaskPolicyDraftPO::getId,
                McpTaskPolicyDraftPO::getRevision,
                McpTaskPolicyDraftPO::setRevision,
                (predicate, row) -> predicate,
                capabilityDraftPersistenceConverter.taskPolicyChange(carrier, dedicated),
                capabilityDraftPersistenceConverter.taskPolicyDraft(carrier, dedicated),
                carrier,
                key,
                expectedRevision
        );
    }

    /**
     * 中文说明：保存 {@code APP_BINDING} 能力草稿；唯一专属列 {@code app_artifact_id} 在旧语句里是必填文本参数，
     * 迁移后是 {@code bigint} 外键且永不下推 SQL NULL。
     * English summary: Saves the {@code APP_BINDING} capability draft; its only dedicated column {@code app_artifact_id} was a required
     * text parameter in the legacy statement and a {@code bigint} foreign key after migration, so no SQL NULL is ever pushed for it.
     * @param carrier 参数 能力载体；parameter the capability carrier.
     * @param key 参数 草稿主键或 {@code null}；parameter the draft key or {@code null}.
     * @param expectedRevision 参数 期望修订；parameter the expected revision.
     * @param dedicated 参数 专属列值；parameter the dedicated values.
     * @return 返回 变更结果；returns the mutation outcome.
     */
    private McpCapabilityDraftMutationDTO saveAppBinding(
            McpCapabilityRecordBO carrier,
            Long key,
            long expectedRevision,
            List<String> dedicated) {
        return saveRow(
                appBindingDraftPersistenceRepository,
                McpAppBindingDraftPO::getId,
                McpAppBindingDraftPO::getRevision,
                McpAppBindingDraftPO::setRevision,
                (predicate, row) -> predicate,
                capabilityDraftPersistenceConverter.appBindingChange(carrier, dedicated),
                capabilityDraftPersistenceConverter.appBindingDraft(carrier, dedicated),
                carrier,
                key,
                expectedRevision
        );
    }

    /**
     * 中文说明：五张能力草稿表共用的 upsert 骨架，逐字复刻旧模板语句：先按 {@code (id, revision)} 读定位，
     * 命中即把技术主键与 {@code version} 交给变更实体、把业务修订按 {@code expectedRevision + 1} 覆写，
     * 并以同一条谓词做 CAS（可空专属列的 SQL NULL 由种类自己的更新条件下推），返回 {@code expectedRevision + 1}；
     * 未命中则读回当前修订，存在修订或期望修订非 0 时抛冲突，只有「无行且期望修订为 0」才以
     * {@code revision = 0} 受守卫插入，插入未成功时按所属表上报
     * {@code <TABLE>_INSERT_CONFLICT}。
     * English summary: The upsert skeleton shared by the five capability draft tables, reproducing the legacy statement template word for
     * word: a row is located by {@code (id, revision)} and, on a hit, the technical key and {@code version} are handed to the change
     * entity, the business revision is overwritten with {@code expectedRevision + 1} and the compare-and-set runs on the very same
     * predicate - the SQL NULL of a nullable dedicated column is pushed by the kind's own update condition - returning
     * {@code expectedRevision + 1}; on a miss the current revision is read back, a stored revision or a non-zero expected revision raises
     * the conflict, and only "no row plus expected revision 0" inserts through the guarded boundary at {@code revision = 0}, reporting
     * {@code <TABLE>_INSERT_CONFLICT} for its own table when the insert did not land.
     * @param store 参数 该种类的受守卫仓储；parameter the guarded store of this kind.
     * @param idColumn 参数 技术主键列；parameter the technical key column.
     * @param revisionColumn 参数 业务修订列；parameter the business revision column.
     * @param revisionSetter 参数 业务修订列写入器；parameter the business revision writer.
     * @param nullPushes 参数 该种类的可空专属列下推；parameter the nullable dedicated column pushes of this kind.
     * @param change 参数 变更实体；parameter the change entity.
     * @param draft 参数 待插入行；parameter the row to insert.
     * @param carrier 参数 能力载体；parameter the capability carrier.
     * @param key 参数 草稿主键或 {@code null}；parameter the draft key or {@code null}.
     * @param expectedRevision 参数 期望修订；parameter the expected revision.
     * @return 返回 变更结果；returns the mutation outcome.
     */
    private <P extends EgonModel<P>> McpCapabilityDraftMutationDTO saveRow(
            EgonColaRepository<?, P> store,
            SFunction<P, ?> idColumn,
            SFunction<P, ?> revisionColumn,
            BiConsumer<P, Long> revisionSetter,
            BiFunction<LambdaUpdateWrapper<P>, P, LambdaUpdateWrapper<P>> nullPushes,
            P change,
            P draft,
            McpCapabilityRecordBO carrier,
            Long key,
            long expectedRevision) {
        if (key != null) {
            Optional<P> located = store.list(boundPredicate(
                    Wrappers.<P>lambdaQuery()
                            .eq(idColumn, key)
                            .eq(revisionColumn, expectedRevision))
            ).stream().findFirst();
            if (located.isPresent()) {
                P row = located.get();
                change.setId(row.getId());
                change.setVersion(row.getVersion());
                revisionSetter.accept(change, expectedRevision + 1L);
                if (store.update(
                        change,
                        boundPredicate(nullPushes.apply(
                                Wrappers.<P>lambdaUpdate()
                                        .eq(idColumn, row.getId())
                                        .eq(
                                                revisionColumn,
                                                expectedRevision
                                        ),
                                change)))) {
                    return new McpCapabilityDraftMutationDTO(
                            carrier.getId(),
                            expectedRevision + 1
                    );
                }
            }
        }
        Long current = currentRevision(store, idColumn, revisionColumn, key);
        if (current != null || expectedRevision != 0) {
            throw revisionConflict(current);
        }
        revisionSetter.accept(draft, 0L);
        if (!store.save(draft)) {
            throw new IllegalStateException(
                    carrier.getKind().table().toUpperCase(Locale.ROOT)
                            + "_INSERT_CONFLICT"
            );
        }
        return new McpCapabilityDraftMutationDTO(carrier.getId(), 0);
    }

    /**
     * 中文说明：软删 {@code RESOURCE} 能力草稿。
     * English summary: Soft-deletes a {@code RESOURCE} capability draft.
     * @param key 参数 草稿主键或 {@code null}；parameter the draft key or {@code null}.
     * @param id 参数 端口标识；parameter the port identifier.
     * @param expectedRevision 参数 期望修订；parameter the expected revision.
     * @return 返回 变更结果；returns the mutation outcome.
     */
    private McpCapabilityDraftMutationDTO removeResource(
            Long key,
            String id,
            long expectedRevision) {
        return removeRow(
                resourceDraftPersistenceRepository,
                McpResourceDraftPO::getId,
                McpResourceDraftPO::getRevision,
                key,
                id,
                expectedRevision
        );
    }

    /**
     * 中文说明：软删 {@code RESOURCE_TEMPLATE} 能力草稿。
     * English summary: Soft-deletes a {@code RESOURCE_TEMPLATE} capability draft.
     * @param key 参数 草稿主键或 {@code null}；parameter the draft key or {@code null}.
     * @param id 参数 端口标识；parameter the port identifier.
     * @param expectedRevision 参数 期望修订；parameter the expected revision.
     * @return 返回 变更结果；returns the mutation outcome.
     */
    private McpCapabilityDraftMutationDTO removeResourceTemplate(
            Long key,
            String id,
            long expectedRevision) {
        return removeRow(
                resourceTemplateDraftPersistenceRepository,
                McpResourceTemplateDraftPO::getId,
                McpResourceTemplateDraftPO::getRevision,
                key,
                id,
                expectedRevision
        );
    }

    /**
     * 中文说明：软删 {@code PROMPT} 能力草稿。
     * English summary: Soft-deletes a {@code PROMPT} capability draft.
     * @param key 参数 草稿主键或 {@code null}；parameter the draft key or {@code null}.
     * @param id 参数 端口标识；parameter the port identifier.
     * @param expectedRevision 参数 期望修订；parameter the expected revision.
     * @return 返回 变更结果；returns the mutation outcome.
     */
    private McpCapabilityDraftMutationDTO removePrompt(
            Long key,
            String id,
            long expectedRevision) {
        return removeRow(
                promptDraftPersistenceRepository,
                McpPromptDraftPO::getId,
                McpPromptDraftPO::getRevision,
                key,
                id,
                expectedRevision
        );
    }

    /**
     * 中文说明：软删 {@code TASK_POLICY} 能力草稿。
     * English summary: Soft-deletes a {@code TASK_POLICY} capability draft.
     * @param key 参数 草稿主键或 {@code null}；parameter the draft key or {@code null}.
     * @param id 参数 端口标识；parameter the port identifier.
     * @param expectedRevision 参数 期望修订；parameter the expected revision.
     * @return 返回 变更结果；returns the mutation outcome.
     */
    private McpCapabilityDraftMutationDTO removeTaskPolicy(
            Long key,
            String id,
            long expectedRevision) {
        return removeRow(
                taskPolicyDraftPersistenceRepository,
                McpTaskPolicyDraftPO::getId,
                McpTaskPolicyDraftPO::getRevision,
                key,
                id,
                expectedRevision
        );
    }

    /**
     * 中文说明：软删 {@code APP_BINDING} 能力草稿。
     * English summary: Soft-deletes an {@code APP_BINDING} capability draft.
     * @param key 参数 草稿主键或 {@code null}；parameter the draft key or {@code null}.
     * @param id 参数 端口标识；parameter the port identifier.
     * @param expectedRevision 参数 期望修订；parameter the expected revision.
     * @return 返回 变更结果；returns the mutation outcome.
     */
    private McpCapabilityDraftMutationDTO removeAppBinding(
            Long key,
            String id,
            long expectedRevision) {
        return removeRow(
                appBindingDraftPersistenceRepository,
                McpAppBindingDraftPO::getId,
                McpAppBindingDraftPO::getRevision,
                key,
                id,
                expectedRevision
        );
    }

    /**
     * 中文说明：五张能力草稿表共用的软删骨架：按 {@code (id, revision)} 读定位后交给受守卫边界的
     * 版本化软删，行缺失、已被软删或 {@code version} 竞争失败都按旧契约回读当前修订并抛冲突。
     * English summary: The soft-delete skeleton shared by the five capability draft tables: a row located by
     * {@code (id, revision)} is handed to the versioned soft delete of the guarded boundary, and a missing row, an already soft-deleted
     * row or a lost {@code version} race all read the current revision back and raise the legacy conflict.
     * @param store 参数 该种类的受守卫仓储；parameter the guarded store of this kind.
     * @param idColumn 参数 技术主键列；parameter the technical key column.
     * @param revisionColumn 参数 业务修订列；parameter the business revision column.
     * @param key 参数 草稿主键或 {@code null}；parameter the draft key or {@code null}.
     * @param id 参数 端口标识；parameter the port identifier.
     * @param expectedRevision 参数 期望修订；parameter the expected revision.
     * @return 返回 变更结果；returns the mutation outcome.
     */
    private <P extends EgonModel<P>> McpCapabilityDraftMutationDTO removeRow(
            EgonColaRepository<?, P> store,
            SFunction<P, ?> idColumn,
            SFunction<P, ?> revisionColumn,
            Long key,
            String id,
            long expectedRevision) {
        Optional<P> located = key == null
                ? Optional.empty()
                : store.list(boundPredicate(
                        Wrappers.<P>lambdaQuery()
                                .eq(idColumn, key)
                                .eq(revisionColumn, expectedRevision))
                ).stream().findFirst();
        if (located.isPresent() && store.removeById(located.get())) {
            return new McpCapabilityDraftMutationDTO(
                    id,
                    expectedRevision + 1
            );
        }
        throw revisionConflict(
                currentRevision(store, idColumn, revisionColumn, key)
        );
    }

    /**
     * 中文说明：执行 currentRevision 操作；等价旧 {@code SELECT revision FROM <table> WHERE id = ?} 的
     * {@code findFirst().orElse(null)}，行不存在（含被软删或属于其它租户）时如实返回 {@code null}。
     * English summary: Executes the currentRevision operation; equivalent to the legacy {@code SELECT revision FROM <table> WHERE id = ?}
     * combined with {@code findFirst().orElse(null)}, truthfully returning {@code null} when no row exists - including one soft-deleted
     * or owned by another tenant.
     * @param store 参数 该种类的受守卫仓储；parameter the guarded store of this kind.
     * @param idColumn 参数 技术主键列；parameter the technical key column.
     * @param revisionColumn 参数 业务修订列；parameter the business revision column.
     * @param key 参数 草稿主键或 {@code null}；parameter the draft key or {@code null}.
     * @return 返回 当前修订或 {@code null}；returns the stored revision or {@code null}.
     */
    private <P extends EgonModel<P>> Long currentRevision(
            EgonColaRepository<?, P> store,
            SFunction<P, ?> idColumn,
            SFunction<P, ?> revisionColumn,
            Long key) {
        if (key == null) {
            return null;
        }
        return store.list(boundPredicate(Wrappers.<P>lambdaQuery()
                .eq(idColumn, key))
        ).stream().findFirst()
                .map(row -> (Long) revisionColumn.apply(row))
                .orElse(null);
    }

    /**
     * 中文说明：执行 renormalize 操作：把端口载体逐字段送回 {@code McpCapabilityRecordBO.normalized(...)}，
     * 因此不存在「未经校验构造 {@code McpCapabilityRecordBO}」的路径，必填与修订非负守护一律按旧文案生效。
     * English summary: Executes the renormalize operation, feeding the port carrier field by field back into
     * {@code McpCapabilityRecordBO.normalized(...)}, so no path can hold an unvalidated {@code McpCapabilityRecordBO} and the required
     * fields plus the non-negative revision guard all apply with the legacy messages.
     * @param carrier 参数 能力载体；parameter the capability carrier.
     * @return 返回 复核后的载体；returns the re-validated carrier.
     */
    private static McpCapabilityRecordBO renormalize(McpCapabilityRecordBO carrier) {
        return McpCapabilityRecordBO.normalized(
                carrier.getKind(),
                carrier.getId(),
                carrier.getGatewayGroupId(),
                carrier.getServerId(),
                carrier.getName(),
                carrier.getContent(),
                carrier.isEnabled(),
                carrier.getRevision()
        );
    }

    /**
     * 中文说明：沿用旧实现的期望修订守护：负数 {@code expectedRevision} 抛
     * {@code IllegalArgumentException("expectedRevision must not be negative")}。
     * English summary: Keeps the legacy expected-revision guard: a negative {@code expectedRevision} raises
     * {@code IllegalArgumentException("expectedRevision must not be negative")}.
     * @param expectedRevision 参数 期望修订；parameter the expected revision.
     */
    private static void validateExpectedRevision(long expectedRevision) {
        if (expectedRevision < 0) {
            throw new IllegalArgumentException(
                    "expectedRevision must not be negative"
            );
        }
    }

    /**
     * 中文说明：执行 revisionConflict 操作，逐字沿用旧实现：读不到当前修订时以 {@code -1} 上报冲突。
     * English summary: Executes the revisionConflict operation, keeping the legacy shape word for word: an unreadable current revision is
     * reported as {@code -1}.
     * @param currentRevision 参数 当前修订；parameter the current revision.
     * @return 返回 冲突异常；returns the conflict failure.
     */
    private static GatewayAdminRevisionConflictException revisionConflict(Long currentRevision) {
        return new GatewayAdminRevisionConflictException(
                currentRevision == null ? -1 : currentRevision
        );
    }

    /**
     * 中文说明：把端口上的十进制标识换算为 {@code bigint} 列值；空白、非十进制与非正值都返回 {@code null}，
     * 由调用方按旧「等值比较不成立、读不到行」的结果处理。
     * English summary: Converts the decimal identifier the port carries into the {@code bigint} column value; blank, non-decimal and
     * non-positive values all yield {@code null} so the caller falls back to the legacy outcome of a comparison that never matched.
     * @param opaqueId 参数 标识文本；parameter the identifier text.
     * @return 返回 列值或 {@code null}；returns the column value or {@code null}.
     */
    private static Long columnValue(String opaqueId) {
        if (opaqueId == null || opaqueId.isBlank()) {
            return null;
        }
        try {
            long parsed = Long.parseLong(opaqueId.trim());
            return parsed > 0L ? parsed : null;
        } catch (NumberFormatException invalidIdentifier) {
            return null;
        }
    }

    /**
     * 中文说明：沿用遗留边界的必填规范化：{@code null} 抛 {@code NullPointerException}，空白抛
     * {@code IllegalArgumentException(field + " is required")}，否则去除首尾空白。
     * English summary: Keeps the legacy required normalization: {@code null} raises a {@code NullPointerException} and a blank value
     * raises {@code IllegalArgumentException(field + " is required")}, otherwise the value is trimmed.
     * @param value 参数 值；parameter value.
     * @param field 参数 字段名；parameter field.
     * @return 返回 规范化后的值；returns the trimmed value.
     */
    private static String required(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    /**
     * 中文说明：把条件交付受守卫边界之前先成形一次：MyBatis-Plus 的 {@code eq/orderByAsc} 只在 SQL 真正成形时才把取值写进
     * {@code paramNameValuePairs}，故此处先成形一次，让谓词在离开门面时参数已完全绑定；之后（包括 MyBatis 自己下发时）
     * 命中同一份片段缓存，参数与 SQL 都不再改变，列解析失败（lambda 缓存缺失）也如实在门面这一层暴露，
     * 而不是留到语句下发时。
     * English summary: Forms a condition once before it is handed to the guarded boundary: MyBatis-Plus only moves the values of
     * {@code eq/orderByAsc} into {@code paramNameValuePairs} while the SQL is being formed, so forming it here first means the
     * parameters are fully bound when the predicate leaves the facade; later renders (including the one MyBatis performs) hit the same
     * segment cache and change neither the parameters nor the SQL, while a column-resolution failure (a missing lambda cache) surfaces
     * truthfully at the facade instead of at statement time.
     * @param predicate 参数 已构造完成的业务条件；parameter the completed business condition.
     * @return 返回 同一份参数已绑定的条件；returns the very same condition with its parameters bound.
     */
    private static <C extends Wrapper<?>> C boundPredicate(C predicate) {
        predicate.getSqlSegment();
        return predicate;
    }
}
