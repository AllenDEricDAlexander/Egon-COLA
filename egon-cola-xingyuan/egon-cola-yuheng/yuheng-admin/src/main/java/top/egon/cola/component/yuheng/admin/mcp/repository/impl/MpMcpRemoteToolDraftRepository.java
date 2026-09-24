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
import top.egon.cola.component.yuheng.admin.mcp.converter.McpRemoteToolDraftPersistenceConverter;
import top.egon.cola.component.yuheng.admin.mcp.domain.bo.McpRemoteToolDraftBO;
import top.egon.cola.component.yuheng.admin.mcp.domain.dto.McpRemoteToolDraftMutationDTO;
import top.egon.cola.component.yuheng.admin.mcp.domain.po.McpRemoteToolDraftRecordPO;
import top.egon.cola.component.yuheng.admin.mcp.repository.McpRemoteToolDraftRepository;
import top.egon.cola.component.yuheng.admin.mcp.repository.mp.McpRemoteToolDraftPersistenceRepository;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminRevisionConflictException;

/**
 * 中文说明：{@code MpMcpRemoteToolDraftRepository} 是远端工具草稿的 MyBatis-Plus 门面存储，逐方法取代被退役的遗留持久化载体：
 * {@code gateway_mcp_remote_tool_draft} 的每次读写都走受守卫的 {@code EgonColaRepository} 边界（同租户过滤、仅活跃行
 * {@code deleted_at IS NULL}、技术 {@code version} 乐观锁），协议主键仍是十进制 {@code id}，而
 * {@code gateway_group_id}/{@code server_id}/{@code remote_mount_id} 已是 {@code bigint} 外键；
 * {@code load} 保留 {@code WHERE gateway_group_id = ?} 与 {@code ORDER BY server_id, tool_name}；
 * {@code save} 保留旧实现「先按 {@code (id, revision)} 做 UPDATE，未命中再读当前修订决定抛冲突还是以
 * {@code revision = 0} 插入」的 upsert 语义与 {@code expectedRevision must not be negative} 守护，
 * UPDATE 的 {@code SET} 只覆盖旧语句覆盖的 {@code tool_name/remote_mount_id/content/enabled}
 * 四列（{@code gateway_group_id}/{@code server_id} 绝不被回写，故变更实体上如实留空，缺席值经更新条件显式下推 SQL NULL），
 * 业务 {@code revision} 只在命中时自增；{@code softDelete} 保留同一组 {@code (id, revision)} 定位谓词，
 * 但受守卫边界只提供版本化软删；jsonb {@code content} 列的编解码只发生在转换器内，读写两侧的载体一律重新经
 * {@code McpRemoteToolDraftBO.normalized(...)} 复核旧构造器不变量，公开端口不泄漏行模型或 DAO。
 *
 * English summary: {@code MpMcpRemoteToolDraftRepository} is the MyBatis-Plus facade store of remote Tool drafts, replacing the retired
 * legacy persistence carrier method by method: every read and write of {@code gateway_mcp_remote_tool_draft} goes through the guarded
 * {@code EgonColaRepository} boundary (same-tenant filtering, active rows only under {@code deleted_at IS NULL}, the technical
 * {@code version} optimistic lock), the protocol key stays the decimal {@code id} while
 * {@code gateway_group_id}/{@code server_id}/{@code remote_mount_id} are already {@code bigint} foreign keys; {@code load} keeps
 * {@code WHERE gateway_group_id = ?} plus {@code ORDER BY server_id, tool_name}; {@code save} keeps the legacy upsert order of first
 * UPDATE-ing by {@code (id, revision)} and then either raising a conflict or inserting at {@code revision = 0} depending on the
 * revision read back, together with the {@code expectedRevision must not be negative} guard, and the UPDATE overwrites only the four
 * columns the legacy statement listed - {@code tool_name/remote_mount_id/content/enabled} - because {@code gateway_group_id} and
 * {@code server_id} were never written back (they stay unset on the change entity, and an absent value is pushed as an explicit SQL
 * NULL through the update condition) while the business {@code revision} only increments on a hit; {@code softDelete} keeps the same
 * {@code (id, revision)} locating predicates but the guarded boundary only offers a versioned soft delete; the jsonb {@code content}
 * column is encoded and decoded only inside the converter, both boundaries re-check carriers through
 * {@code McpRemoteToolDraftBO.normalized(...)}, and the public port leaks neither the row model nor a DAO.
 *
 * 用法 / Usage: 通过业务端口 {@code McpRemoteToolDraftRepository} 由 Spring 容器注入；遗留实现没有声明事务边界，
 * 「读当前修订—判定冲突—写入」的竞态由调用方事务与技术 {@code version} 乐观锁共同收敛，故本门面不声明
 * {@code @Transactional}；冲突一律按旧契约抛 {@code GatewayAdminRevisionConflictException}，读不到行时携带 {@code -1}。
 * Inject it through the business port {@code McpRemoteToolDraftRepository}; the legacy carrier declared no transaction boundary and the
 * read-current-revision, decide-conflict, write race is收敛 by the caller's transaction together with the technical {@code version}
 * optimistic lock, so this facade declares no {@code @Transactional}; conflicts always surface as the legacy
 * {@code GatewayAdminRevisionConflictException}, carrying {@code -1} when no row is visible.
 */
@Slf4j
@Repository("mpMcpRemoteToolDraftRepository")
@RequiredArgsConstructor
@Validated
public class MpMcpRemoteToolDraftRepository
        implements McpRemoteToolDraftRepository {

    /**
     * 中文说明：远端工具草稿行的受守卫持久化仓储（租户过滤、活跃读取、乐观锁 CAS 与版本化软删的唯一入口）。
     * English summary: The guarded persistence store for remote Tool draft rows, the only entry point for tenant filtering, active reads,
     * optimistic-lock CAS and the versioned soft delete.
     */
    @Qualifier("mcpRemoteToolDraftPersistenceRepository")
    private final McpRemoteToolDraftPersistenceRepository remoteToolDraftPersistenceRepository;

    /**
     * 中文说明：{@code McpRemoteToolDraftBO} 与 {@code McpRemoteToolDraftRecordPO} 的双向转换器。
     * English summary: The bidirectional converter between McpRemoteToolDraftBO and McpRemoteToolDraftRecordPO.
     */
    @Qualifier("mcpRemoteToolDraftPersistenceConverter")
    private final McpRemoteToolDraftPersistenceConverter remoteToolDraftPersistenceConverter;

    /**
     * 中文说明：执行 load 操作；等价遗留 {@code SELECT ... WHERE gateway_group_id = ? AND deleted = FALSE
     * ORDER BY server_id, tool_name}：分组标识守护沿用旧文案 {@code gatewayGroupId is required}，迁移后该列是
     * {@code bigint} 外键，无法表示的文本按旧「等值比较不成立」的结果如实读为空集，追加技术 id 升序保证并列行的稳定次序，
     * 每一行都经转换器投影后立即复核 {@code McpRemoteToolDraftBO.normalized(...)}。
     * English summary: Executes the load operation; equivalent to the legacy
     * {@code SELECT ... WHERE gateway_group_id = ? AND deleted = FALSE ORDER BY server_id, tool_name}: the group guard keeps the legacy
     * {@code gatewayGroupId is required} message, the column became a {@code bigint} foreign key so a text that cannot be represented
     * truthfully reads as the empty set - the legacy varchar comparison never matched - an appended ascending technical id keeps a stable
     * order among tied rows, and every row is projected by the converter and immediately re-checked by
     * {@code McpRemoteToolDraftBO.normalized(...)}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mpMcpRemoteToolDraftRepository.load(gatewayGroupId)}。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id.
     * @return 返回 load 的处理结果；returns the remote Tool drafts of one Gateway Group in server and Tool order.
     */
    @Override
    public List<McpRemoteToolDraftBO> load(String gatewayGroupId) {
        Long group = columnValue(required(gatewayGroupId, "gatewayGroupId"));
        if (group == null) {
            return List.of();
        }
        return remoteToolDraftPersistenceRepository.list(
                boundPredicate(Wrappers.<McpRemoteToolDraftRecordPO>lambdaQuery()
                        .eq(McpRemoteToolDraftRecordPO::getGatewayGroupId, group)
                        .orderByAsc(McpRemoteToolDraftRecordPO::getServerId)
                        .orderByAsc(McpRemoteToolDraftRecordPO::getToolName)
                        .orderByAsc(McpRemoteToolDraftRecordPO::getId))
        ).stream().map(this::carrier).toList();
    }

    /**
     * 中文说明：执行 save 操作；等价遗留「UPDATE ... SET tool_name, remote_mount_id, content, enabled,
     * revision = revision + 1, updated_at, updated_by WHERE id = ? AND revision = ? AND deleted = FALSE」，
     * 命中一行即按 {@code expectedRevision + 1} 返回；否则读回当前修订，存在修订或期望修订非 0 时按旧契约抛
     * {@code GatewayAdminRevisionConflictException}，只有「无行且期望修订为 0」才以 {@code revision = 0}
     * 受守卫插入并返回 0；入参守护与顺序沿用旧实现（{@code draft} 必填、{@code expectedRevision} 非负、
     * {@code now} 与 {@code actor} 必填），审计两列由 {@code EgonColaMetaObjectHandler} 以可信上下文下发。
     * English summary: Executes the save operation; equivalent to the legacy
     * {@code UPDATE ... SET tool_name, remote_mount_id, content, enabled, revision = revision + 1, updated_at, updated_by WHERE id = ?
     * AND revision = ? AND deleted = FALSE}: one affected row returns {@code expectedRevision + 1}; otherwise the current revision is
     * read back, a stored revision or a non-zero expected revision raises the legacy {@code GatewayAdminRevisionConflictException}, and
     * only "no row plus expected revision 0" inserts through the guarded boundary at {@code revision = 0} and returns 0; the argument
     * guards and their order follow the legacy implementation ({@code draft} required, non-negative {@code expectedRevision}, required
     * {@code now} and {@code actor}) while the two audit columns are filled by {@code EgonColaMetaObjectHandler} from the trusted
     * context.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mpMcpRemoteToolDraftRepository.save(draftBO, expectedRevision, actor, now)}。
     * @param draft 参数 草稿；parameter draft.
     * @param expectedRevision 参数 expectedRevision；parameter expected revision.
     * @param actor 参数 actor；parameter actor.
     * @param now 参数 now；parameter now.
     * @return 返回 save 的处理结果；returns the mutated draft identifier and its new revision.
     */
    @Override
    public McpRemoteToolDraftMutationDTO save(
            McpRemoteToolDraftBO draft,
            long expectedRevision,
            AdminActor actor,
            Instant now) {
        Objects.requireNonNull(draft, "draft");
        validateExpectedRevision(expectedRevision);
        McpRemoteToolDraftBO carrier = renormalize(draft);
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(actor, "actor");
        Long key = columnValue(carrier.getId());
        Optional<McpRemoteToolDraftRecordPO> located = key == null
                ? Optional.empty()
                : remoteToolDraftPersistenceRepository.list(
                        boundPredicate(draftKeyPredicate(
                                Wrappers.<McpRemoteToolDraftRecordPO>lambdaQuery(),
                                key,
                                expectedRevision))
                ).stream().findFirst();
        if (located.isPresent()) {
            McpRemoteToolDraftRecordPO row = located.get();
            McpRemoteToolDraftRecordPO changed =
                    remoteToolDraftPersistenceConverter.toPersistence(carrier);
            changed.setId(row.getId());
            changed.setVersion(row.getVersion());
            changed.setRevision(expectedRevision + 1L);
            /*
             * 中文说明：旧 UPDATE 从不回写 gateway_group_id 与 server_id，故这两列在变更实体上如实留空，
             * MyBatis-Plus 按非空字段生成 SET 时便会跳过它们。
             * English summary: The legacy UPDATE never rewrites gateway_group_id or server_id, so both columns stay unset on the change
             * entity and MyBatis-Plus, which builds the SET clause from non-null fields, skips them.
             */
            changed.setGatewayGroupId(null);
            changed.setServerId(null);
            if (remoteToolDraftPersistenceRepository.update(
                    changed,
                    boundPredicate(draftKeyPredicate(
                            Wrappers.<McpRemoteToolDraftRecordPO>lambdaUpdate(),
                            row.getId(),
                            expectedRevision)
                            .set(
                                    changed.getRemoteMountId() == null,
                                    McpRemoteToolDraftRecordPO::getRemoteMountId,
                                    null
                            )
                            .set(
                                    changed.getContent() == null,
                                    McpRemoteToolDraftRecordPO::getContent,
                                    null
                            ))
            )) {
                return new McpRemoteToolDraftMutationDTO(
                        carrier.getId(),
                        expectedRevision + 1
                );
            }
        }
        Long current = currentRevision(key);
        if (current != null || expectedRevision != 0) {
            throw revisionConflict(current);
        }
        McpRemoteToolDraftRecordPO fresh =
                remoteToolDraftPersistenceConverter.newRow(carrier);
        fresh.setRevision(0L);
        if (!remoteToolDraftPersistenceRepository.save(fresh)) {
            throw new IllegalStateException(
                    "YUHENG_ADMIN_MCP_REMOTE_TOOL_DRAFT_INSERT_CONFLICT"
            );
        }
        return new McpRemoteToolDraftMutationDTO(carrier.getId(), 0);
    }

    /**
     * 中文说明：执行 softDelete 操作；等价遗留「UPDATE ... SET deleted = TRUE, enabled = FALSE,
     * revision = revision + 1 WHERE id = ? AND revision = ? AND deleted = FALSE」，未命中一行时与旧实现一样
     * 读回当前修订并抛 {@code GatewayAdminRevisionConflictException}（读不到行为 {@code -1}），命中则返回
     * {@code expectedRevision + 1}；受守卫边界不接受带业务谓词的批量删除，只提供按实体的版本化软删，
     * 故删除落为「按同一组谓词读定位 + {@code removeById(实体)}」，缺席的 {@code enabled = FALSE}
     * 与自增的 {@code revision} 不再有意义——软删行对所有活跃读取都不可见，端口的返回修订仍按旧算式给出。
     * English summary: Executes the softDelete operation; equivalent to the legacy
     * {@code UPDATE ... SET deleted = TRUE, enabled = FALSE, revision = revision + 1 WHERE id = ? AND revision = ? AND deleted = FALSE}:
     * when no row is affected the current revision is read back and the legacy {@code GatewayAdminRevisionConflictException} is raised
     * ({@code -1} when nothing is visible), otherwise {@code expectedRevision + 1} is returned; the guarded boundary accepts no bulk
     * delete carrying business predicates and only offers a versioned soft delete by entity, so the removal becomes "locate by the very
     * same predicates plus {@code removeById(entity)}", and the omitted {@code enabled = FALSE} and the incremented {@code revision}
     * lose their meaning because a soft-deleted row is invisible to every active read while the port still reports the legacy revision
     * arithmetic.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mpMcpRemoteToolDraftRepository.softDelete(id, expectedRevision, actor, now)}。
     * @param id 参数 草稿Id；parameter draft id.
     * @param expectedRevision 参数 expectedRevision；parameter expected revision.
     * @param actor 参数 actor；parameter actor.
     * @param now 参数 now；parameter now.
     * @return 返回 softDelete 的处理结果；returns the soft-deleted draft identifier and its new revision.
     */
    @Override
    public McpRemoteToolDraftMutationDTO softDelete(
            String id,
            long expectedRevision,
            AdminActor actor,
            Instant now) {
        validateExpectedRevision(expectedRevision);
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(actor, "actor");
        required(id, "id");
        Long key = columnValue(id);
        Optional<McpRemoteToolDraftRecordPO> located = key == null
                ? Optional.empty()
                : remoteToolDraftPersistenceRepository.list(
                        boundPredicate(draftKeyPredicate(
                                Wrappers.<McpRemoteToolDraftRecordPO>lambdaQuery(),
                                key,
                                expectedRevision))
                ).stream().findFirst();
        if (located.isPresent()
                && remoteToolDraftPersistenceRepository.removeById(located.get())) {
            return new McpRemoteToolDraftMutationDTO(
                    id,
                    expectedRevision + 1
            );
        }
        throw revisionConflict(currentRevision(key));
    }

    /**
     * 中文说明：执行 currentRevision 操作；等价旧 {@code SELECT revision FROM gateway_mcp_remote_tool_draft
     * WHERE id = ?} 的 {@code findFirst().orElse(null)}，行不存在（含被软删或属于其它租户）时如实返回 {@code null}。
     * English summary: Executes the currentRevision operation; equivalent to the legacy
     * {@code SELECT revision FROM gateway_mcp_remote_tool_draft WHERE id = ?} combined with {@code findFirst().orElse(null)}, truthfully
     * returning {@code null} when no row exists - including one soft-deleted or owned by another tenant.
     * @param key 参数 草稿主键；parameter the draft key.
     * @return 返回 当前修订或 {@code null}；returns the stored revision or {@code null}.
     */
    private Long currentRevision(Long key) {
        if (key == null) {
            return null;
        }
        return remoteToolDraftPersistenceRepository.list(
                boundPredicate(Wrappers.<McpRemoteToolDraftRecordPO>lambdaQuery()
                        .eq(McpRemoteToolDraftRecordPO::getId, key))
        ).stream().findFirst()
                .map(McpRemoteToolDraftRecordPO::getRevision)
                .orElse(null);
    }

    /**
     * 中文说明：把旧语句的 {@code (id, revision)} 主键谓词挂到任意 lambda 条件上，定位读取与 CAS 写入
     * 共用同一份谓词定义，避免两条路径漂移；软删谓词与旧语句的 {@code deleted = FALSE} 由
     * {@code @TableLogic} 统一附加。
     * English summary: Attaches the legacy {@code (id, revision)} primary-key predicates to any lambda condition, so the locating read
     * and the compare-and-set write share one predicate definition and cannot drift apart; the soft-delete half of the legacy predicate
     * is appended uniformly by {@code @TableLogic}.
     * @param predicate 参数 待挂载的条件；parameter the condition to enrich.
     * @param key 参数 草稿主键；parameter the draft key.
     * @param expectedRevision 参数 期望修订；parameter the expected revision.
     * @return 返回 同一份挂好业务谓词的条件；returns the very same condition carrying the business predicates.
     */
    private static <W extends com.baomidou.mybatisplus.core.conditions.AbstractLambdaWrapper<
            McpRemoteToolDraftRecordPO, W>> W draftKeyPredicate(
            W predicate,
            Long key,
            long expectedRevision) {
        return predicate
                .eq(McpRemoteToolDraftRecordPO::getId, key)
                .eq(McpRemoteToolDraftRecordPO::getRevision, expectedRevision);
    }

    /**
     * 中文说明：把行模型经转换器投影为业务载体后立即复核旧构造器不变量（读边界）。
     * English summary: Projects a row onto the business carrier through the converter and immediately re-checks the legacy constructor
     * invariants, which is the read boundary.
     * @param row 参数 行模型；parameter the row model.
     * @return 返回 业务载体；returns the business carrier.
     */
    private McpRemoteToolDraftBO carrier(McpRemoteToolDraftRecordPO row) {
        return renormalize(remoteToolDraftPersistenceConverter.toBusiness(row));
    }

    /**
     * 中文说明：执行 renormalize 操作：把已完成列映射的载体逐字段送回
     * {@code McpRemoteToolDraftBO.normalized(...)}，因此无论载入还是保存都不存在「未经校验构造
     * {@code McpRemoteToolDraftBO}」的路径，必填与修订非负守护一律按旧文案生效。
     * English summary: Executes the renormalize operation, feeding an already column-mapped carrier field by field back into
     * {@code McpRemoteToolDraftBO.normalized(...)}, so neither loading nor saving can hold an unvalidated
     * {@code McpRemoteToolDraftBO} and the required fields plus the non-negative revision guard all apply with the legacy messages.
     * @param carrier 参数 已映射的载体；parameter the mapped carrier.
     * @return 返回 复核后的载体；returns the re-validated carrier.
     */
    private static McpRemoteToolDraftBO renormalize(McpRemoteToolDraftBO carrier) {
        return McpRemoteToolDraftBO.normalized(
                carrier.getId(),
                carrier.getGatewayGroupId(),
                carrier.getServerId(),
                carrier.getName(),
                carrier.getRemoteMountId(),
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
