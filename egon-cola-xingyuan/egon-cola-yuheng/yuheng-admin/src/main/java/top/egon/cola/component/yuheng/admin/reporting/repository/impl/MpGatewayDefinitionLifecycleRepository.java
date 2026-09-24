package top.egon.cola.component.yuheng.admin.reporting.repository.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.catalog.domain.po.GatewayOperationRecordPO;
import top.egon.cola.component.yuheng.admin.catalog.repository.mp.GatewayOperationPersistenceRepository;
import top.egon.cola.component.yuheng.admin.openapi.domain.po.GatewayOpenApiSyncRecordPO;
import top.egon.cola.component.yuheng.admin.openapi.repository.mp.GatewayOpenApiSyncPersistenceRepository;
import top.egon.cola.component.yuheng.admin.reporting.domain.po.GatewayDefinitionSetOperationPO;
import top.egon.cola.component.yuheng.admin.reporting.domain.po.GatewayDefinitionSetPO;
import top.egon.cola.component.yuheng.admin.reporting.domain.vo.GatewayReconcileResultVO;
import top.egon.cola.component.yuheng.admin.reporting.repository.GatewayDefinitionLifecycleRepository;
import top.egon.cola.component.yuheng.admin.reporting.repository.mp.GatewayDefinitionSetOperationPersistenceRepository;
import top.egon.cola.component.yuheng.admin.reporting.repository.mp.GatewayDefinitionSetPersistenceRepository;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 中文说明：{@code MpGatewayDefinitionLifecycleRepository} 是网关定义生命周期端口的 MyBatis-Plus 门面实现，逐方法替换被删除的
 * {@code JdbcGatewayDefinitionLifecycleRepository}；它不写裸 SQL、不引入 JdbcTemplate/mapper XML，所有读写只经由各表的受守卫
 * {@code EgonColaRepository} 持久化边界（租户过滤、活跃读取、乐观锁 CAS、版本化软删）。原实现的多表连接（{@code gateway_definition_set_operation}
 * JOIN {@code gateway_definition_set}、{@code UPDATE gateway_operation ... FROM selected}、{@code NOT EXISTS} 子查询）被分解为
 * 同作用域、同排序的单表受守卫读取后在内存中组装，再逐行做 CAS 覆写；四类受影响行数只在 {@code updateById} 返回真值时累加，
 * 因此 0 行写入永远不会被记为成功，异常语义也与原实现一致（本类不新增任何异常类型）。
 * English summary: {@code MpGatewayDefinitionLifecycleRepository} is the MyBatis-Plus facade implementing the gateway definition-lifecycle port, replacing the deleted {@code JdbcGatewayDefinitionLifecycleRepository} method by method. It issues no raw SQL and adds no JdbcTemplate or mapper XML: every read and write goes through the table-specific guarded {@code EgonColaRepository} boundary (tenant filtering, active-only reads, optimistic-lock CAS, versioned soft delete). The legacy multi-table statements (the membership JOIN on {@code gateway_definition_set}, the {@code UPDATE gateway_operation ... FROM selected} and the {@code NOT EXISTS} sub-select) are decomposed into same-scope, same-ordering guarded single-table reads assembled in memory and applied row by row under CAS; the four row counters only advance when {@code updateById} returns true, so a zero-row write is never reported as success and no new exception type is introduced.
 *
 * 用法 / Usage: 由 Spring 经端口 {@code GatewayDefinitionLifecycleRepository} 注入到定时协调器，写入组合在调用方（{@code gatewayTransactionManager}）的同一事务内执行；
 * 返回的 {@code GatewayReconcileResultVO.changed()} 为假时不得写审计。/ Inject it through the {@code GatewayDefinitionLifecycleRepository} port into the scheduled reconciler; the writes compose inside the caller's transaction managed by {@code gatewayTransactionManager}, and auditing must stay skipped while {@code GatewayReconcileResultVO.changed()} is false.
 */
@Slf4j
@Validated
@Repository("mpGatewayDefinitionLifecycleRepository")
@RequiredArgsConstructor
public class MpGatewayDefinitionLifecycleRepository implements GatewayDefinitionLifecycleRepository {

    /** 定义集/操作激活态原 wire 字符串。/ Original wire string of the active definition-set and operation state. */
    private static final String ACTIVE_STATUS = "ACTIVE";

    /** 定义集退役态原 wire 字符串。/ Original wire string of the retired definition-set state. */
    private static final String RETIRED_STATUS = "RETIRED";

    /** 操作弃用态原 wire 字符串。/ Original wire string of the deprecated operation state. */
    private static final String DEPRECATED_STATUS = "DEPRECATED";

    /** 操作下线态原 wire 字符串。/ Original wire string of the offlined operation state. */
    private static final String OFFLINE_STATUS = "OFFLINE";

    /** OpenAPI 同步状态中代表整组有效的原 wire 字符串。/ Original sync-state wire string meaning the whole group is valid. */
    private static final String VALID_SYNC_STATUS = "VALID";

    /** 由描述符/OpenAPI 上报来源拥有、因而可被下线的一批操作来源类型。/ Descriptor-backed source types whose operations may be offlined by reconciliation. */
    private static final List<String> DESCRIPTOR_SOURCE_TYPES = List.of("RPC_DESCRIPTOR", "OPENAPI31");

    /** {@code gateway_definition_set} 的受守卫持久化边界。/ Guarded store for gateway_definition_set rows. */
    @Qualifier("gatewayDefinitionSetPersistenceRepository")
    private final GatewayDefinitionSetPersistenceRepository definitionSetRepository;

    /** {@code gateway_definition_set_operation} 成员关系的受守卫持久化边界。/ Guarded store for definition-set membership rows. */
    @Qualifier("gatewayDefinitionSetOperationPersistenceRepository")
    private final GatewayDefinitionSetOperationPersistenceRepository membershipRepository;

    /** {@code gateway_operation} 的受守卫持久化边界。/ Guarded store for gateway_operation rows. */
    @Qualifier("gatewayOperationPersistenceRepository")
    private final GatewayOperationPersistenceRepository operationRepository;

    /** {@code gateway_openapi_sync_state} 的受守卫持久化边界。/ Guarded store for gateway_openapi_sync_state rows. */
    @Qualifier("gatewayOpenApiSyncPersistenceRepository")
    private final GatewayOpenApiSyncPersistenceRepository openApiSyncRepository;

    /**
     * 中文说明：执行 reconcile 操作；先用一次扫描（{@code status = 'ACTIVE' OR id IN (:activeDefinitionSetIds)}）拿到期望态覆盖的应用集合，
     * 再按应用依次做与原实现等价的四步：激活本应用中被判定为在线且当前非 ACTIVE 的定义集（写 {@code activated_at} 的 COALESCE 语义）、
     * 退役该应用其余仍为 ACTIVE 的定义集、把成员关系中最新的定义指向刷入 {@code gateway_operation}
     * （仅当五个业务列存在 {@code IS DISTINCT FROM} 差异时才写，并自增业务 {@code revision}）、把不再被任何激活定义集引用的描述符型操作下线为
     * {@code OFFLINE}。四步的计数只统计受守卫 CAS 真正写成的行数。
     * English summary: Executes the reconcile operation; a single scan ({@code status = 'ACTIVE' OR id IN (:activeDefinitionSetIds)}) first collects the applications covered by the desired state, then each application goes through the four steps the legacy implementation had: activate the now-online definition sets that are not ACTIVE yet (keeping the {@code activated_at} COALESCE semantics), retire the application's remaining ACTIVE sets, refresh {@code gateway_operation} from the newest membership row (written only when one of the five business columns is {@code IS DISTINCT FROM} and bumping the business {@code revision}), and offline descriptor-backed operations no longer referenced by an activated set. All four counters only add rows the guarded CAS actually wrote.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayDefinitionLifecycleRepository.reconcile(activeDefinitionSetIds, now)}。
     * @param activeDefinitionSetIds 参数 当前在线定义集合 id，可为 {@code null}（按空集处理，与原实现一致）；parameter active definition set ids, {@code null} meaning empty as before.
     * @param now 参数 本次对账时间戳，绑定到业务列 {@code activated_at}/{@code retired_at}/{@code deprecated_at}；parameter reconciliation timestamp bound to the business columns {@code activated_at}/{@code retired_at}/{@code deprecated_at}.
     * @return 返回四类受影响行数；returns the four affected-row counters.
     */
    @Override
    public GatewayReconcileResultVO reconcile(
            Set<String> activeDefinitionSetIds,
            Instant now) {
        Set<Long> activeIds = toTechnicalIds(activeDefinitionSetIds);
        Map<Long, GatewayDefinitionSetPO> setsById = new LinkedHashMap<>();
        Map<Long, Set<Long>> applications = new LinkedHashMap<>();
        for (GatewayDefinitionSetPO row : definitionSetRepository.list(scanQuery(activeIds))) {
            setsById.put(row.getId(), row);
            collectApplication(applications, activeIds, row.getApplicationId(), row.getId());
        }
        int activatedSets = 0;
        int retiredSets = 0;
        int activatedOperations = 0;
        int offlinedOperations = 0;
        for (Map.Entry<Long, Set<Long>> application : applications.entrySet()) {
            Long applicationId = application.getKey();
            Set<Long> definitionSetIds = application.getValue();
            activatedSets += activateDefinitionSets(applicationId, definitionSetIds, now);
            retiredSets += retireDefinitionSets(applicationId, definitionSetIds, now);
            activatedOperations += activateOperations(
                    applicationId,
                    definitionSetIds,
                    setsById,
                    now
            );
            offlinedOperations += offlineOperations(applicationId, definitionSetIds);
        }
        return new GatewayReconcileResultVO(
                activatedSets,
                retiredSets,
                activatedOperations,
                offlinedOperations
        );
    }

    /**
     * 中文说明：执行 activeOpenApiDefinitionSetIds 操作；等价于原
     * {@code SELECT DISTINCT definition_set_id FROM gateway_openapi_sync_state WHERE status = 'VALID' AND definition_set_id IS NOT NULL}，
     * 分解为同谓词的受守卫单表读取（附加 {@code definition_set_id} 升序以获得稳定顺序），再在内存中按不透明 id 去重。
     * English summary: Executes the activeOpenApiDefinitionSetIds operation; equivalent to the original {@code SELECT DISTINCT definition_set_id FROM gateway_openapi_sync_state WHERE status = 'VALID' AND definition_set_id IS NOT NULL}, decomposed into a guarded single-table read with the same predicates (plus {@code definition_set_id} ascending for a stable order) and de-duplicated in memory by opaque id.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayDefinitionLifecycleRepository.activeOpenApiDefinitionSetIds()}。
     * @return 返回有效定义集合 id 集合，无结果时为空集；returns the valid definition set ids, empty when none qualify.
     */
    @Override
    public Set<String> activeOpenApiDefinitionSetIds() {
        Set<String> definitionSetIds = new LinkedHashSet<>();
        for (GatewayOpenApiSyncRecordPO state : openApiSyncRepository.list(
                Wrappers.<GatewayOpenApiSyncRecordPO>lambdaQuery()
                        .eq(GatewayOpenApiSyncRecordPO::getStatus, VALID_SYNC_STATUS)
                        .isNotNull(GatewayOpenApiSyncRecordPO::getDefinitionSetId)
                        .orderByAsc(GatewayOpenApiSyncRecordPO::getDefinitionSetId))) {
            String definitionSetId = text(state.getDefinitionSetId());
            if (definitionSetId != null) {
                definitionSetIds.add(definitionSetId);
            }
        }
        return Set.copyOf(definitionSetIds);
    }

    /**
     * 中文说明：构造与原扫描 SQL 同谓词的定义集查询；{@code activeIds} 为空时只按 {@code status = 'ACTIVE'} 过滤，
     * 否则用括号分组表达 {@code status = 'ACTIVE' OR id IN (:activeIds)}，并把原结果集的不确定顺序固定为
     * {@code application_id, id} 升序。
     * English summary: Builds the definition-set scan with the original predicates; when {@code activeIds} is empty it filters on {@code status = 'ACTIVE'} only, otherwise it expresses {@code status = 'ACTIVE' OR id IN (:activeIds)} as a bracketed group, and pins the previously unordered result set to {@code application_id, id} ascending.
     * @param activeIds 参数 数值化的在线定义集合 id；parameter numeric active definition set ids.
     * @return 返回扫描查询条件；returns the scan query.
     */
    private LambdaQueryWrapper<GatewayDefinitionSetPO> scanQuery(Set<Long> activeIds) {
        LambdaQueryWrapper<GatewayDefinitionSetPO> query = Wrappers.lambdaQuery();
        if (activeIds.isEmpty()) {
            query.eq(GatewayDefinitionSetPO::getStatus, ACTIVE_STATUS);
        } else {
            query.and(branch -> branch
                    .eq(GatewayDefinitionSetPO::getStatus, ACTIVE_STATUS)
                    .or()
                    .in(GatewayDefinitionSetPO::getId, activeIds));
        }
        return query
                .orderByAsc(GatewayDefinitionSetPO::getApplicationId)
                .orderByAsc(GatewayDefinitionSetPO::getId);
    }

    /**
     * 中文说明：与原 {@code collectApplication} 完全一致地把扫描行归入应用维度：应用键总是登记，只有出现在期望态里的定义集合才计入该应用的激活集合。
     * English summary: Groups scanned rows per application exactly like the original {@code collectApplication}: the application key is always registered, while only definition sets present in the desired state join that application's activation set.
     * @param applications 参数 应用→待激活定义集合累积表；parameter accumulated application to activation-set map.
     * @param activeDefinitionSetIds 参数 期望态定义集合 id；parameter desired-state definition set ids.
     * @param applicationId 参数 应用 id；parameter application id.
     * @param definitionSetId 参数 定义集合 id；parameter definition set id.
     */
    private void collectApplication(
            Map<Long, Set<Long>> applications,
            Set<Long> activeDefinitionSetIds,
            Long applicationId,
            Long definitionSetId) {
        Set<Long> applicationActiveSets = applications.computeIfAbsent(
                applicationId,
                ignored -> new LinkedHashSet<>()
        );
        if (activeDefinitionSetIds.contains(definitionSetId)) {
            applicationActiveSets.add(definitionSetId);
        }
    }

    /**
     * 中文说明：执行 activateDefinitionSets 操作；等价于原
     * {@code UPDATE gateway_definition_set SET status='ACTIVE', activated_at=COALESCE(activated_at,:now), retired_at=NULL WHERE application_id=:applicationId AND id IN (:ids) AND status<>'ACTIVE'}。
     * 期望集合为空时对应原 SQL 使用哨兵 id 命中 0 行，故直接返回 0；逐行 CAS 未写成的行不计数。
     * English summary: Executes the activateDefinitionSets operation; equivalent to the original {@code UPDATE gateway_definition_set SET status='ACTIVE', activated_at=COALESCE(activated_at,:now), retired_at=NULL WHERE application_id=:applicationId AND id IN (:ids) AND status<>'ACTIVE'}. An empty desired set reproduces the original sentinel id matching zero rows and therefore returns 0; rows whose CAS did not land are not counted.
     * @param applicationId 参数 应用 id；parameter application id.
     * @param definitionSetIds 参数 该应用待激活定义集合 id；parameter definition set ids to activate for this application.
     * @param now 参数 对账时间戳；parameter reconciliation timestamp.
     * @return 返回真正写入的行数；returns the number of rows actually written.
     */
    private int activateDefinitionSets(
            Long applicationId,
            Set<Long> definitionSetIds,
            Instant now) {
        if (definitionSetIds.isEmpty()) {
            return 0;
        }
        int changed = 0;
        for (GatewayDefinitionSetPO candidate : definitionSetRepository.list(
                Wrappers.<GatewayDefinitionSetPO>lambdaQuery()
                        .eq(GatewayDefinitionSetPO::getApplicationId, applicationId)
                        .in(GatewayDefinitionSetPO::getId, definitionSetIds)
                        .ne(GatewayDefinitionSetPO::getStatus, ACTIVE_STATUS)
                        .orderByAsc(GatewayDefinitionSetPO::getId))) {
            candidate.setStatus(ACTIVE_STATUS);
            if (candidate.getActivatedAt() == null) {
                candidate.setActivatedAt(now);
            }
            // 原 SQL 同时写 retired_at = NULL；受守卫 CAS 依 MyBatis-Plus 默认 NOT_NULL 更新策略不会下发空值列，
            // 在禁止改动 RecordPO 与 mapper XML 的前提下无法清空该列（详见迁移报告的守卫 API 缺口）。
            if (definitionSetRepository.updateById(candidate)) {
                changed++;
            }
        }
        return changed;
    }

    /**
     * 中文说明：执行 retireDefinitionSets 操作；等价于原
     * {@code UPDATE gateway_definition_set SET status='RETIRED', retired_at=:now WHERE application_id=:applicationId AND status='ACTIVE' AND id NOT IN (:ids)}；
     * {@code ids} 为空时按原哨兵语义省略 {@code NOT IN} 过滤，即退役该应用全部仍为 ACTIVE 的定义集。
     * English summary: Executes the retireDefinitionSets operation; equivalent to the original {@code UPDATE gateway_definition_set SET status='RETIRED', retired_at=:now WHERE application_id=:applicationId AND status='ACTIVE' AND id NOT IN (:ids)'}. When {@code ids} is empty the {@code NOT IN} filter is dropped, reproducing the original sentinel semantics of retiring every still-ACTIVE set of the application.
     * @param applicationId 参数 应用 id；parameter application id.
     * @param definitionSetIds 参数 本次保留的激活定义集合 id；parameter definition set ids kept active this round.
     * @param now 参数 对账时间戳；parameter reconciliation timestamp.
     * @return 返回真正写入的行数；returns the number of rows actually written.
     */
    private int retireDefinitionSets(
            Long applicationId,
            Set<Long> definitionSetIds,
            Instant now) {
        LambdaQueryWrapper<GatewayDefinitionSetPO> query = Wrappers.<GatewayDefinitionSetPO>lambdaQuery()
                .eq(GatewayDefinitionSetPO::getApplicationId, applicationId)
                .eq(GatewayDefinitionSetPO::getStatus, ACTIVE_STATUS);
        if (!definitionSetIds.isEmpty()) {
            query.notIn(GatewayDefinitionSetPO::getId, definitionSetIds);
        }
        query.orderByAsc(GatewayDefinitionSetPO::getId);
        int changed = 0;
        for (GatewayDefinitionSetPO candidate : definitionSetRepository.list(query)) {
            candidate.setStatus(RETIRED_STATUS);
            candidate.setRetiredAt(now);
            if (definitionSetRepository.updateById(candidate)) {
                changed++;
            }
        }
        return changed;
    }

    /**
     * 中文说明：执行 activateOperations 操作；把原 {@code WITH selected AS (SELECT DISTINCT ON (membership.operation_id) ... ORDER BY membership.operation_id, definition_set.received_at DESC)}
     * 分解为「按 {@code received_at} 升序逐集读取成员关系并覆盖同 operation_id 的行」，即得到每个操作最新的成员关系；随后按原
     * {@code IS DISTINCT FROM} 五列判定是否需要更新，需要时在当前定义指向、方法身份、提供方服务身份（jsonb 结构等值）、外部可达标记与生命周期状态上做受守卫 CAS 覆写，
     * 并自增业务 {@code revision}；被弃用的成员关系写 {@code deprecated_at = :now}。
     * English summary: Executes the activateOperations operation; the original {@code WITH selected AS (SELECT DISTINCT ON (membership.operation_id) ... ORDER BY membership.operation_id, definition_set.received_at DESC)} is decomposed into reading memberships set by set in ascending {@code received_at} order and overwriting the entry for the same {@code operation_id}, which yields each operation's newest membership. The five {@code IS DISTINCT FROM} columns then decide whether a write is needed, in which case the current-definition pointer, method identity, provider service identity (jsonb structural equality), external-accessible flag and lifecycle status are applied through a guarded CAS update with an incremented business {@code revision}; a deprecated membership writes {@code deprecated_at = :now}.
     * @param applicationId 参数 应用 id；parameter application id.
     * @param definitionSetIds 参数 该应用的激活定义集合 id；parameter activated definition set ids of this application.
     * @param setsById 参数 扫描得到的定义集合行缓存，用于取 {@code received_at} 与应用归属；parameter scanned definition-set rows used for {@code received_at} and ownership.
     * @param now 参数 对账时间戳；parameter reconciliation timestamp.
     * @return 返回真正写入的操作行数；returns the number of operation rows actually written.
     */
    private int activateOperations(
            Long applicationId,
            Set<Long> definitionSetIds,
            Map<Long, GatewayDefinitionSetPO> setsById,
            Instant now) {
        if (definitionSetIds.isEmpty()) {
            return 0;
        }
        int changed = 0;
        for (GatewayDefinitionSetOperationPO membership
                : latestMemberships(applicationId, definitionSetIds, setsById)) {
            Long operationId = membership.getOperationId();
            if (operationId == null) {
                continue;
            }
            GatewayOperationRecordPO operation = operationRepository.getById(operationId);
            if (operation == null) {
                continue;
            }
            String lifecycleStatus = Boolean.TRUE.equals(membership.getDeprecated())
                    ? DEPRECATED_STATUS
                    : ACTIVE_STATUS;
            if (!isDistinctFrom(operation.getCurrentDefinitionId(), membership.getDefinitionId())
                    && !isDistinctFrom(operation.getMethodIdentity(), membership.getMethodIdentity())
                    && !isDistinctFrom(
                            operation.getProviderServiceIdentity(),
                            membership.getProviderServiceIdentity()
                    )
                    && !isDistinctFrom(
                            operation.getExternalAccessible(),
                            membership.getExternalAccessible()
                    )
                    && !isDistinctFrom(operation.getLifecycleStatus(), lifecycleStatus)) {
                continue;
            }
            operation.setCurrentDefinitionId(membership.getDefinitionId());
            operation.setMethodIdentity(membership.getMethodIdentity());
            operation.setProviderServiceIdentity(membership.getProviderServiceIdentity());
            operation.setExternalAccessible(membership.getExternalAccessible());
            operation.setLifecycleStatus(lifecycleStatus);
            if (Boolean.TRUE.equals(membership.getDeprecated())) {
                operation.setDeprecatedAt(now);
            }
            // 原 SQL 的非弃用分支写 deprecated_at = NULL；同上，受守卫 CAS 的 NOT_NULL 更新策略不会下发空值列。
            operation.setRevision(nextRevision(operation.getRevision()));
            if (operationRepository.updateById(operation)) {
                changed++;
            }
        }
        return changed;
    }

    /**
     * 中文说明：等价还原原 {@code DISTINCT ON (membership.operation_id)} + {@code ORDER BY operation_id, definition_set.received_at DESC} 的选取：
     * 先把该应用的定义集合按 {@code received_at}、id 升序排列，再逐个集合读取成员关系并以 {@code operation_id} 为键覆盖，最后按
     * {@code operation_id}、成员关系 id 升序输出。定义集合的应用归属经与原 JOIN 相同的 {@code definition_set.application_id = :applicationId} 校验。
     * English summary: Reproduces the original {@code DISTINCT ON (membership.operation_id)} with {@code ORDER BY operation_id, definition_set.received_at DESC}: the application's definition sets are ordered by ascending {@code received_at} and id, memberships are read set by set and overwritten per {@code operation_id}, and the result is emitted by ascending {@code operation_id} and membership id. Definition-set ownership is checked with the same {@code definition_set.application_id = :applicationId} predicate the original JOIN carried.
     * @param applicationId 参数 应用 id；parameter application id.
     * @param definitionSetIds 参数 该应用的激活定义集合 id；parameter activated definition set ids of this application.
     * @param setsById 参数 定义集合行缓存，缺失时回退到按 id 的受守卫读取；parameter definition-set cache, falling back to a guarded read by id.
     * @return 返回每个操作最新的一条成员关系；returns the newest membership row per operation.
     */
    private List<GatewayDefinitionSetOperationPO> latestMemberships(
            Long applicationId,
            Set<Long> definitionSetIds,
            Map<Long, GatewayDefinitionSetPO> setsById) {
        List<Long> orderedSets = definitionSetIds.stream()
                .map(definitionSetId -> ownedDefinitionSet(applicationId, definitionSetId, setsById))
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .<GatewayDefinitionSetPO, Instant>comparing(
                                GatewayDefinitionSetPO::getReceivedAt)
                        .thenComparing(GatewayDefinitionSetPO::getId))
                .map(GatewayDefinitionSetPO::getId)
                .toList();
        Map<Long, GatewayDefinitionSetOperationPO> newestByOperation = new LinkedHashMap<>();
        for (Long definitionSetId : orderedSets) {
            for (GatewayDefinitionSetOperationPO membership : membershipRepository.list(
                    Wrappers.<GatewayDefinitionSetOperationPO>lambdaQuery()
                            .eq(GatewayDefinitionSetOperationPO::getDefinitionSetId, definitionSetId)
                            .orderByAsc(GatewayDefinitionSetOperationPO::getOperationId)
                            .orderByAsc(GatewayDefinitionSetOperationPO::getId))) {
                if (membership.getOperationId() == null) {
                    continue;
                }
                newestByOperation.put(membership.getOperationId(), membership);
            }
        }
        return newestByOperation.values().stream()
                .sorted(Comparator
                        .<GatewayDefinitionSetOperationPO, Long>comparing(
                                GatewayDefinitionSetOperationPO::getOperationId)
                        .thenComparing(GatewayDefinitionSetOperationPO::getId))
                .toList();
    }

    /**
     * 中文说明：执行 offlineOperations 操作；等价于原
     * {@code UPDATE gateway_operation SET lifecycle_status='OFFLINE', revision=revision+1, updated_at=:now WHERE application_id=:applicationId AND source_type IN ('RPC_DESCRIPTOR','OPENAPI31') AND lifecycle_status<>'OFFLINE' AND NOT EXISTS (SELECT 1 FROM gateway_definition_set_operation membership WHERE membership.operation_id=operation.id AND membership.definition_set_id IN (:ids))}，
     * 其中 {@code NOT EXISTS} 由「先受守卫读出被引用 operation_id 集合、再在内存中排除」等价实现；期望集合为空时引用集为空，
     * 与原哨兵 id 令 {@code NOT EXISTS} 恒真的行为一致。
     * English summary: Executes the offlineOperations operation; equivalent to the original {@code UPDATE gateway_operation SET lifecycle_status='OFFLINE', revision=revision+1, updated_at=:now WHERE application_id=:applicationId AND source_type IN ('RPC_DESCRIPTOR','OPENAPI31') AND lifecycle_status<>'OFFLINE' AND NOT EXISTS (SELECT 1 FROM gateway_definition_set_operation membership WHERE membership.operation_id=operation.id AND membership.definition_set_id IN (:ids))}, where {@code NOT EXISTS} becomes a guarded read of the referenced operation ids plus an in-memory exclusion; an empty desired set yields an empty reference set, matching the original sentinel that made {@code NOT EXISTS} always true. The legacy {@code updated_at = :now} is carried by the guarded audit fill ({@code update_time}) instead of a business timestamp parameter.
     * @param applicationId 参数 应用 id；parameter application id.
     * @param definitionSetIds 参数 该应用的激活定义集合 id；parameter activated definition set ids of this application.
     * @return 返回真正写入的操作行数；returns the number of operation rows actually written.
     */
    private int offlineOperations(
            Long applicationId,
            Set<Long> definitionSetIds) {
        Set<Long> referencedOperationIds = referencedOperationIds(definitionSetIds);
        int changed = 0;
        for (GatewayOperationRecordPO operation : operationRepository.list(
                Wrappers.<GatewayOperationRecordPO>lambdaQuery()
                        .eq(GatewayOperationRecordPO::getApplicationId, applicationId)
                        .in(GatewayOperationRecordPO::getSourceType, DESCRIPTOR_SOURCE_TYPES)
                        .ne(GatewayOperationRecordPO::getLifecycleStatus, OFFLINE_STATUS)
                        .orderByAsc(GatewayOperationRecordPO::getId))) {
            if (referencedOperationIds.contains(operation.getId())) {
                continue;
            }
            operation.setLifecycleStatus(OFFLINE_STATUS);
            operation.setRevision(nextRevision(operation.getRevision()));
            if (operationRepository.updateById(operation)) {
                changed++;
            }
        }
        return changed;
    }

    /**
     * 中文说明：读出仍被这些定义集合引用的 operation_id 集合，等价于原 {@code NOT EXISTS} 子查询的关联条件；
     * 受守卫读取额外带租户与未软删谓词。
     * English summary: Reads the operation ids still referenced by these definition sets, the correlation of the original {@code NOT EXISTS} sub-select; the guarded read additionally carries the tenant and not-soft-deleted predicates.
     * @param definitionSetIds 参数 激活定义集合 id；parameter activated definition set ids.
     * @return 返回被引用的操作 id 集合；returns the referenced operation ids.
     */
    private Set<Long> referencedOperationIds(Set<Long> definitionSetIds) {
        if (definitionSetIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> operationIds = new LinkedHashSet<>();
        for (GatewayDefinitionSetOperationPO membership : membershipRepository.list(
                Wrappers.<GatewayDefinitionSetOperationPO>lambdaQuery()
                        .in(GatewayDefinitionSetOperationPO::getDefinitionSetId, definitionSetIds)
                        .orderByAsc(GatewayDefinitionSetOperationPO::getOperationId)
                        .orderByAsc(GatewayDefinitionSetOperationPO::getId))) {
            if (membership.getOperationId() != null) {
                operationIds.add(membership.getOperationId());
            }
        }
        return operationIds;
    }

    /**
     * 中文说明：取回属于该应用的定义集合行，优先使用扫描缓存，缺失时回退为按 id 的受守卫读取；
     * 应用不匹配或行不存在（含已软删）时返回 {@code null}，等价于原 JOIN 不产生该行。
     * English summary: Resolves the definition-set row owned by this application, preferring the scan cache and falling back to a guarded read by id; a different application or a missing (or soft-deleted) row yields {@code null}, exactly as when the original JOIN produced no row.
     * @param applicationId 参数 应用 id；parameter application id.
     * @param definitionSetId 参数 定义集合 id；parameter definition set id.
     * @param setsById 参数 扫描缓存；parameter scan cache.
     * @return 返回定义集合行或 {@code null}；returns the definition-set row or {@code null}.
     */
    private GatewayDefinitionSetPO ownedDefinitionSet(
            Long applicationId,
            Long definitionSetId,
            Map<Long, GatewayDefinitionSetPO> setsById) {
        GatewayDefinitionSetPO definitionSet = setsById.get(definitionSetId);
        if (definitionSet == null) {
            definitionSet = definitionSetRepository.getById(definitionSetId);
            if (definitionSet != null) {
                setsById.put(definitionSetId, definitionSet);
            }
        }
        if (definitionSet == null
                || !Objects.equals(applicationId, definitionSet.getApplicationId())) {
            return null;
        }
        return definitionSet;
    }

    /**
     * 中文说明：以 PostgreSQL {@code IS DISTINCT FROM} 的空安全语义比较两列，JSON 列依赖 {@code JsonNode} 的结构等值。
     * English summary: Compares two columns with the null-safe PostgreSQL {@code IS DISTINCT FROM} semantics; the jsonb column relies on structural {@code JsonNode} equality.
     * @param left 参数 现值；parameter the persisted value.
     * @param right 参数 目标值；parameter the desired value.
     * @return 返回两者是否不同；returns whether the two differ.
     */
    private static boolean isDistinctFrom(Object left, Object right) {
        return !Objects.equals(left, right);
    }

    /**
     * 中文说明：在业务 revision 上自增 1，null 视为 0，与原 SQL 的 {@code revision = operation.revision + 1} 对齐。
     * English summary: Increments the business revision by one treating null as zero, matching the original SQL {@code revision = operation.revision + 1}.
     * @param revision 参数 修订；parameter revision.
     * @return 返回自增后的修订；returns the incremented revision.
     */
    private static Long nextRevision(Long revision) {
        return (revision == null ? 0L : revision) + 1L;
    }

    /**
     * 中文说明：把端口边界上的不透明定义集合 id 数值化；{@code null} 入参按空集处理（与原实现一致），
     * 空白或非数值的 id 在数值主键下本就无法匹配任何行，故如实跳过并留调试日志。
     * English summary: Parses the opaque port-boundary definition-set ids; a {@code null} input stays the empty set as before, while blank or non-numeric ids can never match a numeric primary key, so they are skipped truthfully with a debug log line.
     * @param opaqueIds 参数 端口边界 id 集合；parameter port-boundary ids.
     * @return 返回数值 id 集合（保持输入顺序）；returns the numeric ids preserving input order.
     */
    private Set<Long> toTechnicalIds(Set<String> opaqueIds) {
        if (opaqueIds == null) {
            return Set.of();
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (String opaqueId : opaqueIds) {
            Long id = toLongOrNull(opaqueId);
            if (id == null) {
                log.debug(
                        "Ignoring definition-set id {} that cannot address a numeric primary key",
                        opaqueId
                );
                continue;
            }
            ids.add(id);
        }
        return ids;
    }

    /**
     * 中文说明：把字符串 id 解析为数值主键，无法解析时返回 {@code null} 而不抛异常。
     * English summary: Parses a string id into the numeric primary key, returning {@code null} instead of throwing when it cannot be parsed.
     * @param value 参数 值；parameter value.
     * @return 返回数值 id 或 {@code null}；returns the numeric id or {@code null}.
     */
    private static Long toLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException unparsable) {
            return null;
        }
    }

    /**
     * 中文说明：把持久层数值主键投影为端口边界上的不透明字符串 id。
     * English summary: Projects a numeric persistence primary key to the opaque string id exposed on the port boundary.
     * @param value 参数 值；parameter value.
     * @return 返回字符串 id 或 {@code null}；returns the string id or {@code null}.
     */
    private static String text(Long value) {
        return value == null ? null : String.valueOf(value);
    }
}
