package top.egon.cola.component.yuheng.admin.mcp.repository.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.mcp.converter.McpManagedToolOverridePersistenceConverter;
import top.egon.cola.component.yuheng.admin.mcp.domain.bo.McpManagedToolOverrideBO;
import top.egon.cola.component.yuheng.admin.mcp.domain.dto.McpManagedToolDraftMutationDTO;
import top.egon.cola.component.yuheng.admin.mcp.domain.po.McpManagedToolOverrideRecordPO;
import top.egon.cola.component.yuheng.admin.mcp.repository.McpManagedToolOverrideRepository;
import top.egon.cola.component.yuheng.admin.mcp.repository.mp.McpManagedToolOverridePersistenceRepository;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminRevisionConflictException;

/**
 * 中文说明：{@code MpMcpManagedToolOverrideRepository} 是受管工具覆盖的 MyBatis-Plus 门面存储，逐方法取代被退役的遗留持久化载体：
 * {@code gateway_mcp_managed_tool_override} 的每次读写都走受守卫的 {@code EgonColaRepository} 边界（同租户过滤、仅活跃行
 * {@code deleted_at IS NULL}、技术 {@code version} 乐观锁），协议主键仍是 {@code tool_id}，而
 * {@code gateway_group_id}/{@code operation_id}/{@code server_id} 已是 {@code bigint} 外键；{@code save} 保留旧实现
 * 「先按 {@code (gateway_group_id, operation_id, revision)} 做 UPDATE，未命中再读当前修订决定抛冲突还是以
 * {@code revision = 0} 插入」的 upsert 语义与 {@code expectedRevision must not be negative} 守护，可空的
 * {@code server_id}/{@code minimum_risk_level}/{@code enabled} 一律按旧语句无条件覆写（空值经更新条件显式下推 SQL NULL），
 * 业务 {@code revision} 只在命中时自增；{@code delete} 保留同一组定位谓词，但受守卫边界只提供版本化软删；
 * 列与类型映射只经 {@code mcpManagedToolOverridePersistenceConverter} 完成，读写两侧的载体一律重新经
 * {@code McpManagedToolOverrideBO.normalized(...)} 复核旧构造器不变量，公开端口不泄漏行模型或 DAO。
 * English summary: {@code MpMcpManagedToolOverrideRepository} is the MyBatis-Plus facade store of managed Tool overrides, replacing the
 * retired legacy persistence carrier method by method: every read and write of {@code gateway_mcp_managed_tool_override} goes through
 * the guarded {@code EgonColaRepository} boundary (same-tenant filtering, active rows only under {@code deleted_at IS NULL}, the
 * technical {@code version} optimistic lock), {@code tool_id} stays the protocol primary key while
 * {@code gateway_group_id}/{@code operation_id}/{@code server_id} are already {@code bigint} foreign keys; {@code save} keeps the legacy
 * upsert order of first UPDATE-ing by {@code (gateway_group_id, operation_id, revision)} and then either raising a conflict or inserting
 * at {@code revision = 0} depending on the revision read back, together with the {@code expectedRevision must not be negative} guard,
 * the nullable {@code server_id}/{@code minimum_risk_level}/{@code enabled} are still overwritten unconditionally as the legacy
 * statement did (absent values pushed as explicit SQL NULL through the update condition) and the business {@code revision} only
 * increments on a hit; {@code delete} keeps the same locating predicates but the guarded boundary only offers a versioned soft delete;
 * column and type mapping happens only in {@code mcpManagedToolOverridePersistenceConverter}, both boundaries re-check carriers through
 * {@code McpManagedToolOverrideBO.normalized(...)}, and the public port leaks neither the row model nor a DAO.
 *
 * 用法 / Usage: 通过业务端口 {@code McpManagedToolOverrideRepository} 由 Spring 容器注入；遗留实现没有声明事务边界，
 * 「读当前修订—判定冲突—写入」的竞态由调用方事务与技术 {@code version} 乐观锁共同收敛，故本门面不声明
 * {@code @Transactional}；冲突一律按旧契约抛 {@code GatewayAdminRevisionConflictException}，读不到行时携带 {@code -1}。
 * Inject it through the business port {@code McpManagedToolOverrideRepository}; the legacy carrier declared no transaction boundary and
 * the read-current-revision, decide-conflict, write race is收敛 by the caller's transaction together with the technical {@code version}
 * optimistic lock, so this facade declares no {@code @Transactional}; conflicts always surface as the legacy
 * {@code GatewayAdminRevisionConflictException}, carrying {@code -1} when no row is visible.
 */
@Slf4j
@Repository("mpMcpManagedToolOverrideRepository")
@RequiredArgsConstructor
@Validated
public class MpMcpManagedToolOverrideRepository
        implements McpManagedToolOverrideRepository {

    /**
     * 中文说明：覆盖行的受守卫持久化仓储（租户过滤、活跃读取、乐观锁 CAS 与版本化软删的唯一入口）。
     * English summary: The guarded persistence store for override rows, the only entry point for tenant filtering, active reads,
     * optimistic-lock CAS and the versioned soft delete.
     */
    @Qualifier("mcpManagedToolOverridePersistenceRepository")
    private final McpManagedToolOverridePersistenceRepository overridePersistenceRepository;

    /**
     * 中文说明：{@code McpManagedToolOverrideBO} 与 {@code McpManagedToolOverrideRecordPO} 的双向转换器。
     * English summary: The bidirectional converter between McpManagedToolOverrideBO and McpManagedToolOverrideRecordPO.
     */
    @Qualifier("mcpManagedToolOverridePersistenceConverter")
    private final McpManagedToolOverridePersistenceConverter overridePersistenceConverter;

    /**
     * 中文说明：执行 load 操作；等价遗留 {@code SELECT ... WHERE gateway_group_id = ? ORDER BY operation_id}：
     * 分组标识守护沿用旧文案 {@code gatewayGroupId is required}，迁移后该列是 {@code bigint} 外键，
     * 无法表示的文本按旧「varchar 等值比较不成立」的结果如实读为空集，追加技术 id 升序保证同序时的稳定次序，
     * 每一行都经转换器投影后立即复核 {@code McpManagedToolOverrideBO.normalized(...)}。
     * English summary: Executes the load operation; equivalent to the legacy {@code SELECT ... WHERE gateway_group_id = ? ORDER BY
     * operation_id}: the group guard keeps the legacy {@code gatewayGroupId is required} message, the column became a {@code bigint}
     * foreign key so a text that cannot be represented truthfully reads as the empty set - the legacy varchar comparison never matched -
     * a technical-id ascending tie-break keeps a stable order among equal rows, and every row is projected by the converter and
     * immediately re-checked by {@code McpManagedToolOverrideBO.normalized(...)}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mpMcpManagedToolOverrideRepository.load(gatewayGroupId)}。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id.
     * @return 返回 load 的处理结果；returns the overrides of one Gateway Group in operation order.
     */
    @Override
    public List<McpManagedToolOverrideBO> load(String gatewayGroupId) {
        Long group = columnValue(required(gatewayGroupId, "gatewayGroupId"));
        if (group == null) {
            return List.of();
        }
        return overridePersistenceRepository.list(
                boundPredicate(Wrappers.<McpManagedToolOverrideRecordPO>lambdaQuery()
                        .eq(McpManagedToolOverrideRecordPO::getGatewayGroupId, group)
                        .orderByAsc(McpManagedToolOverrideRecordPO::getOperationId)
                        .orderByAsc(McpManagedToolOverrideRecordPO::getId))
        ).stream().map(this::carrier).toList();
    }

    /**
     * 中文说明：执行 save 操作；等价遗留「UPDATE ... SET tool_id, server_id, additional_permissions, minimum_risk_level,
     * enabled, revision = revision + 1, updated_at, updated_by WHERE gateway_group_id = ? AND operation_id = ? AND
     * revision = ?」，命中一行即按 {@code expectedRevision + 1} 返回；否则读回当前修订，
     * 存在修订或期望修订非 0 时按旧契约抛 {@code GatewayAdminRevisionConflictException}，
     * 只有「无行且期望修订为 0」才以 {@code revision = 0} 受守卫插入并返回 0；入参守护与顺序沿用旧实现
     * （{@code override} 必填、{@code expectedRevision} 非负、{@code now} 与 {@code actor} 必填），
     * 审计两列由 {@code EgonColaMetaObjectHandler} 以可信上下文下发。
     * English summary: Executes the save operation; equivalent to the legacy
     * {@code UPDATE ... SET tool_id, server_id, additional_permissions, minimum_risk_level, enabled, revision = revision + 1,
     * updated_at, updated_by WHERE gateway_group_id = ? AND operation_id = ? AND revision = ?}: one affected row returns
     * {@code expectedRevision + 1}; otherwise the current revision is read back, a stored revision or a non-zero expected revision raises
     * the legacy {@code GatewayAdminConflict}-shaped {@code GatewayAdminRevisionConflictException}, and only "no row plus expected
     * revision 0" inserts through the guarded boundary at {@code revision = 0} and returns 0; the argument guards and their order follow
     * the legacy implementation ({@code override} required, non-negative {@code expectedRevision}, required {@code now} and
     * {@code actor}) while the two audit columns are filled by {@code EgonColaMetaObjectHandler} from the trusted context.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mpMcpManagedToolOverrideRepository.save(overrideBO, expectedRevision, actor, now)}。
     * @param override 参数 override；parameter override.
     * @param expectedRevision 参数 expectedRevision；parameter expected revision.
     * @param actor 参数 actor；parameter actor.
     * @param now 参数 now；parameter now.
     * @return 返回 save 的处理结果；returns the mutated Tool identifier and its new revision.
     */
    @Override
    public McpManagedToolDraftMutationDTO save(
            McpManagedToolOverrideBO override,
            long expectedRevision,
            AdminActor actor,
            Instant now) {
        Objects.requireNonNull(override, "override");
        validateExpectedRevision(expectedRevision);
        McpManagedToolOverrideBO carrier = renormalize(override);
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(actor, "actor");
        Long group = numericKey(carrier.getGatewayGroupId(), "gatewayGroupId");
        Long operation = numericKey(carrier.getOperationId(), "operationId");
        Optional<McpManagedToolOverrideRecordPO> located = overridePersistenceRepository.list(
                boundPredicate(overrideKeyPredicate(
                        Wrappers.<McpManagedToolOverrideRecordPO>lambdaQuery(),
                        group,
                        operation,
                        expectedRevision))
        ).stream().findFirst();
        if (located.isPresent()) {
            McpManagedToolOverrideRecordPO row = located.get();
            McpManagedToolOverrideRecordPO changed =
                    overridePersistenceConverter.toPersistence(carrier);
            changed.setId(row.getId());
            changed.setVersion(row.getVersion());
            changed.setRevision(expectedRevision + 1L);
            if (overridePersistenceRepository.update(
                    changed,
                    boundPredicate(overrideKeyPredicate(
                            Wrappers.<McpManagedToolOverrideRecordPO>lambdaUpdate(),
                            group,
                            operation,
                            expectedRevision)
                            .eq(McpManagedToolOverrideRecordPO::getId, row.getId())
                            .set(
                                    changed.getServerId() == null,
                                    McpManagedToolOverrideRecordPO::getServerId,
                                    null
                            )
                            .set(
                                    changed.getMinimumRiskLevel() == null,
                                    McpManagedToolOverrideRecordPO::getMinimumRiskLevel,
                                    null
                            )
                            .set(
                                    changed.getEnabled() == null,
                                    McpManagedToolOverrideRecordPO::getEnabled,
                                    null
                            ))
            )) {
                return new McpManagedToolDraftMutationDTO(
                        carrier.getToolId(),
                        expectedRevision + 1
                );
            }
        }
        Long current = currentRevision(group, operation);
        if (current != null || expectedRevision != 0) {
            throw revisionConflict(current);
        }
        McpManagedToolOverrideRecordPO fresh =
                overridePersistenceConverter.newRow(carrier);
        fresh.setRevision(0L);
        if (!overridePersistenceRepository.save(fresh)) {
            throw new IllegalStateException(
                    "YUHENG_ADMIN_MCP_MANAGED_TOOL_OVERRIDE_INSERT_CONFLICT"
            );
        }
        return new McpManagedToolDraftMutationDTO(carrier.getToolId(), 0);
    }

    /**
     * 中文说明：执行 delete 操作；等价遗留「DELETE ... WHERE gateway_group_id = ? AND operation_id = ? AND revision = ?」，
     * 未命中一行时与旧实现一样读回当前修订并抛 {@code GatewayAdminRevisionConflictException}（读不到行为 {@code -1}），
     * 命中则返回 {@code expectedRevision + 1}；受守卫边界不接受带业务谓词的批量删除，只提供按实体的版本化软删，
     * 故删除落为「按同一组谓词读定位 + {@code removeById(实体)}」，行缺失或 {@code version} 竞争失败都按冲突如实抛出。
     * English summary: Executes the delete operation; equivalent to the legacy
     * {@code DELETE ... WHERE gateway_group_id = ? AND operation_id = ? AND revision = ?}: when no row is affected the current revision
     * is read back and the legacy {@code GatewayAdminRevisionConflictException} is raised ({@code -1} when nothing is visible), otherwise
     * {@code expectedRevision + 1} is returned; the guarded boundary accepts no bulk delete carrying business predicates and only offers a
     * versioned soft delete by entity, so the removal becomes "locate by the very same predicates plus {@code removeById(entity)}", and a
     * vanished row or a lost {@code version} race surfaces truthfully as the same conflict.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mpMcpManagedToolOverrideRepository.delete(toolId, gatewayGroupId, operationId,
     * expectedRevision)}。
     * @param toolId 参数 工具Id；parameter tool id.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id.
     * @param operationId 参数 操作Id；parameter operation id.
     * @param expectedRevision 参数 expectedRevision；parameter expected revision.
     * @return 返回 delete 的处理结果；returns the deleted Tool identifier and its new revision.
     */
    @Override
    public McpManagedToolDraftMutationDTO delete(
            String toolId,
            String gatewayGroupId,
            String operationId,
            long expectedRevision) {
        validateExpectedRevision(expectedRevision);
        Long group = columnValue(required(gatewayGroupId, "gatewayGroupId"));
        Long operation = columnValue(required(operationId, "operationId"));
        if (group != null && operation != null) {
            Optional<McpManagedToolOverrideRecordPO> located = overridePersistenceRepository.list(
                    boundPredicate(overrideKeyPredicate(
                            Wrappers.<McpManagedToolOverrideRecordPO>lambdaQuery(),
                            group,
                            operation,
                            expectedRevision))
            ).stream().findFirst();
            if (located.isPresent()
                    && overridePersistenceRepository.removeById(located.get())) {
                return new McpManagedToolDraftMutationDTO(
                        toolId,
                        expectedRevision + 1
                );
            }
        }
        throw revisionConflict(currentRevision(group, operation));
    }

    /**
     * 中文说明：执行 currentRevision 操作；等价旧 {@code SELECT revision ... WHERE gateway_group_id = ? AND operation_id = ?}
     * 的 {@code findFirst().orElse(null)}，行不存在（含被软删或属于其它租户）时如实返回 {@code null}。
     * English summary: Executes the currentRevision operation; equivalent to the legacy
     * {@code SELECT revision ... WHERE gateway_group_id = ? AND operation_id = ?} combined with {@code findFirst().orElse(null)}, truthfully
     * returning {@code null} when no row exists - including one soft-deleted or owned by another tenant.
     * @param group 参数 分组外键；parameter the Group foreign key.
     * @param operation 参数 操作外键；parameter the operation foreign key.
     * @return 返回 当前修订或 {@code null}；returns the stored revision or {@code null}.
     */
    private Long currentRevision(Long group, Long operation) {
        if (group == null || operation == null) {
            return null;
        }
        return overridePersistenceRepository.list(
                boundPredicate(Wrappers.<McpManagedToolOverrideRecordPO>lambdaQuery()
                        .eq(McpManagedToolOverrideRecordPO::getGatewayGroupId, group)
                        .eq(McpManagedToolOverrideRecordPO::getOperationId, operation))
        ).stream().findFirst()
                .map(McpManagedToolOverrideRecordPO::getRevision)
                .orElse(null);
    }

    /**
     * 中文说明：把旧语句的 {@code (gateway_group_id, operation_id, revision)} 自然键谓词挂到任意 lambda 条件上，
     * 定位读取与 CAS 写入共用同一份谓词定义，避免两条路径漂移。
     * English summary: Attaches the legacy {@code (gateway_group_id, operation_id, revision)} natural-key predicates to any lambda
     * condition, so the locating read and the compare-and-set write share one predicate definition and cannot drift apart.
     * @param predicate 参数 待挂载的条件；parameter the condition to enrich.
     * @param group 参数 分组外键；parameter the Group foreign key.
     * @param operation 参数 操作外键；parameter the operation foreign key.
     * @param expectedRevision 参数 期望修订；parameter the expected revision.
     * @return 返回 同一份挂好业务谓词的条件；returns the very same condition carrying the business predicates.
     */
    private static <W extends com.baomidou.mybatisplus.core.conditions.AbstractLambdaWrapper<
            McpManagedToolOverrideRecordPO, W>> W overrideKeyPredicate(
            W predicate,
            Long group,
            Long operation,
            long expectedRevision) {
        return predicate
                .eq(McpManagedToolOverrideRecordPO::getGatewayGroupId, group)
                .eq(McpManagedToolOverrideRecordPO::getOperationId, operation)
                .eq(McpManagedToolOverrideRecordPO::getRevision, expectedRevision);
    }

    /**
     * 中文说明：把行模型经转换器投影为业务载体后立即复核旧构造器不变量（读边界）。
     * English summary: Projects a row onto the business carrier through the converter and immediately re-checks the legacy
     * constructor invariants, which is the read boundary.
     * @param row 参数 行模型；parameter the row model.
     * @return 返回 业务载体；returns the business carrier.
     */
    private McpManagedToolOverrideBO carrier(McpManagedToolOverrideRecordPO row) {
        return renormalize(overridePersistenceConverter.toBusiness(row));
    }

    /**
     * 中文说明：执行 renormalize 操作：把已完成列映射的载体逐字段送回 {@code McpManagedToolOverrideBO.normalized(...)}，
     * 因此无论载入还是保存都不存在「未经校验构造 {@code McpManagedToolOverrideBO}」的路径，
     * 「不得放开工具」「必须至少收紧一项」与修订非负守护一律按旧文案生效。
     * English summary: Executes the renormalize operation, feeding an already column-mapped carrier field by field back into
     * {@code McpManagedToolOverrideBO.normalized(...)}, so neither loading nor saving can hold an unvalidated
     * {@code McpManagedToolOverrideBO} and the "an override may never enable a Tool", "an override must tighten at least one field" and
     * non-negative revision guards all apply with the legacy messages.
     *
     * 用法 / Usage: 由 {@link #save(McpManagedToolOverrideBO, long, AdminActor, Instant)} 与
     * {@link #carrier(McpManagedToolOverrideRecordPO)} 调用。
     * @param carrier 参数 已映射的载体；parameter the mapped carrier.
     * @return 返回 复核后的载体；returns the re-validated carrier.
     */
    private static McpManagedToolOverrideBO renormalize(McpManagedToolOverrideBO carrier) {
        return McpManagedToolOverrideBO.normalized(
                carrier.getToolId(),
                carrier.getGatewayGroupId(),
                carrier.getOperationId(),
                carrier.getServerId(),
                carrier.getAdditionalPermissions(),
                carrier.getMinimumRiskLevel(),
                carrier.getEnabled(),
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
     * 中文说明：把端口上的十进制外键文本换算为 {@code bigint} 列值；空白、非十进制与非正值都返回 {@code null}，
     * 由调用方按旧「等值比较不成立、读不到行」的结果处理。
     * English summary: Converts the decimal foreign-key text the port carries into the {@code bigint} column value; blank, non-decimal and
     * non-positive values all yield {@code null} so the caller falls back to the legacy outcome of a comparison that never matched.
     * @param opaqueId 参数 外键文本；parameter the foreign-key text.
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
     * 中文说明：把写入侧必需的外键文本换算为 {@code bigint} 列值；迁移后覆盖行的自然键是数值列，
     * 无法表示的标识按调用方契约错误抛出，而不是写出 {@code NULL} 冒充自然键成功。
     * English summary: Converts a write-side foreign key into the {@code bigint} column value; after migration the natural key of an
     * override row is numeric, so an unrepresentable identifier is raised as a caller contract error instead of writing {@code NULL} and
     * pretending the key held.
     * @param opaqueId 参数 外键文本；parameter the foreign-key text.
     * @param field 参数 字段名；parameter the field name.
     * @return 返回 列值；returns the column value.
     */
    private static Long numericKey(String opaqueId, String field) {
        Long column = columnValue(opaqueId);
        if (column == null) {
            throw new IllegalArgumentException(
                    field + " must be a numeric identifier"
            );
        }
        return column;
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
     * {@code paramNameValuePairs}，故此处先成形一次，让谓词在离开门面时参数已完全绑定；
     * 之后（包括 MyBatis 自己下发时）命中同一份片段缓存，参数与 SQL 都不再改变，列解析失败
     * （lambda 缓存缺失）也如实在门面这一层暴露，而不是留到语句下发时。
     * English summary: Forms a condition once before it is handed to the guarded boundary: MyBatis-Plus only moves the values of
     * {@code eq/orderByAsc} into {@code paramNameValuePairs} while the SQL is being formed, so forming it here first means the parameters
     * are fully bound when the predicate leaves the facade; later renders (including the one MyBatis performs) hit the same segment cache
     * and change neither the parameters nor the SQL, while a column-resolution failure (a missing lambda cache) surfaces truthfully at the
     * facade instead of at statement time.
     * @param predicate 参数 已构造完成的业务条件；parameter the completed business condition.
     * @return 返回 同一份参数已绑定的条件；returns the very same condition with its parameters bound.
     */
    private static <C extends Wrapper<?>> C boundPredicate(C predicate) {
        predicate.getSqlSegment();
        return predicate;
    }
}
