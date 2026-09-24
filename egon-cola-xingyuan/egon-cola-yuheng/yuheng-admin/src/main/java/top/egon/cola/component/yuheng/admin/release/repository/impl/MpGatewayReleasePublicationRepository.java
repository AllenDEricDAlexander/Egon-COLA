package top.egon.cola.component.yuheng.admin.release.repository.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.release.converter.GatewayReleasePublicationPersistenceConverter;
import top.egon.cola.component.yuheng.admin.release.domain.bo.GatewayChunkCleanupCandidateBO;
import top.egon.cola.component.yuheng.admin.release.domain.bo.GatewayReleasePublicationBO;
import top.egon.cola.component.yuheng.admin.release.domain.enums.GatewayPublicationPhaseEnum;
import top.egon.cola.component.yuheng.admin.release.domain.enums.GatewayPublicationStatusEnum;
import top.egon.cola.component.yuheng.admin.release.domain.enums.GatewayReleaseStatus;
import top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseContentPO;
import top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleasePublicationRecordPO;
import top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseRecordPO;
import top.egon.cola.component.yuheng.admin.release.repository.GatewayReleasePublicationRepository;
import top.egon.cola.component.yuheng.admin.release.repository.mp.GatewayReleaseContentPersistenceRepository;
import top.egon.cola.component.yuheng.admin.release.repository.mp.GatewayReleasePersistenceRepository;
import top.egon.cola.component.yuheng.admin.release.repository.mp.GatewayReleasePublicationPersistenceRepository;
import top.egon.cola.component.yuheng.admin.routing.domain.po.GatewayDraftRecordPO;
import top.egon.cola.component.yuheng.admin.routing.repository.mp.GatewayDraftPersistenceRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 中文说明：{@code MpGatewayReleasePublicationRepository} 是发布编排日志的 MyBatis-Plus 门面存储，逐方法替换被删除的手写
 * JDBC 实现：日志行的读写一律经 {@code gateway_release_publication} 的受守卫
 * {@code EgonColaRepository} 边界（租户过滤、只读活跃行、乐观锁 CAS、版本化软删），列与枚举映射只经
 * {@code gatewayReleasePublicationPersistenceConverter} 完成，跨表判定（发布头、内容快照、草稿）改用多次受守卫读取加内存相关，
 * 公开端口不泄漏 RecordPO 或 DAO。
 * English summary: {@code MpGatewayReleasePublicationRepository} is the MyBatis-Plus facade store of the publication journal,
 * replacing the deleted hand-written JDBC implementation method by method: every read and write of the
 * journal goes through the guarded {@code EgonColaRepository} boundary of {@code gateway_release_publication} (tenant filtering,
 * active-only reads, optimistic-lock CAS, versioned soft delete), column and enum mapping happens only through
 * {@code gatewayReleasePublicationPersistenceConverter}, the cross-table decisions (release head, content snapshot, draft) become
 * several guarded reads correlated in memory, and the public port leaks neither RecordPO nor DAO.
 *
 * 用法 / Usage: 通过业务端口 {@code GatewayReleasePublicationRepository} 由 Spring 容器注入；状态推进沿用旧实现的
 * 「先按业务谓词读定位、再乐观锁 CAS 写」顺序，谓词不匹配或影响 0 行都按旧文案如实抛出 {@code IllegalStateException}，
 * 绝不把 0 行报成成功。/ Inject it through the business port {@code GatewayReleasePublicationRepository}; state transitions keep
 * the legacy order of locating by the business predicate and then writing under the optimistic lock, and an unmatched predicate or a
 * zero-row effect raises the legacy {@code IllegalStateException} message instead of reporting success.
 */
@Slf4j
@Repository("mpGatewayReleasePublicationRepository")
@RequiredArgsConstructor
@Validated
public class MpGatewayReleasePublicationRepository
        implements GatewayReleasePublicationRepository {

    /**
     * 中文说明：表示可记录结果状态集合，等价旧 {@code markResult} 的
     * {@code ddc_status IN (RESOLVED, SUBMITTED, FAILED, PARTIAL_SUCCESS, TIMEOUT, UNKNOWN)}。
     * English summary: Represents the recordable status set, equivalent to the legacy
     * {@code ddc_status IN (RESOLVED, SUBMITTED, FAILED, PARTIAL_SUCCESS, TIMEOUT, UNKNOWN)} predicate of {@code markResult}.
     *
     * 用法 / Usage: 仅用于结果回写的定位谓词。/ Used only by the locating predicate of the result write-back.
     */
    private static final List<String> RECORDABLE_STATUSES = List.of(
            GatewayPublicationStatusEnum.RESOLVED.name(),
            GatewayPublicationStatusEnum.SUBMITTED.name(),
            GatewayPublicationStatusEnum.FAILED.name(),
            GatewayPublicationStatusEnum.PARTIAL_SUCCESS.name(),
            GatewayPublicationStatusEnum.TIMEOUT.name(),
            GatewayPublicationStatusEnum.UNKNOWN.name()
    );

    /**
     * 中文说明：表示阻挡分块回收的在途发布状态字面量，等价旧 {@code NOT EXISTS} 子查询的
     * {@code status IN (READY, PUBLISHING, UNKNOWN, TIMEOUT)}。
     * English summary: Represents the in-flight release statuses blocking chunk cleanup, equivalent to the legacy
     * {@code NOT EXISTS} sub-select {@code status IN (READY, PUBLISHING, UNKNOWN, TIMEOUT)}.
     *
     * 用法 / Usage: 仅用于分块回收候选的组内守护。/ Used only by the per-group guard of the cleanup candidates.
     */
    private static final List<String> BLOCKING_RELEASE_STATUSES = List.of(
            GatewayReleaseStatus.READY.name(),
            GatewayReleaseStatus.PUBLISHING.name(),
            GatewayReleaseStatus.UNKNOWN.name(),
            GatewayReleaseStatus.TIMEOUT.name()
    );

    /**
     * 中文说明：表示分块已被回收的错误码字面量，等价旧 {@code markChunkCleaned} 写入的
     * {@code error_code = 'CHUNK_GC_DELETED'}，同时也是分块回收候选谓词里被排除的标记。
     * English summary: Represents the error-code literal a cleaned chunk carries, equivalent to the legacy
     * {@code error_code = 'CHUNK_GC_DELETED'} written by {@code markChunkCleaned} and excluded by the cleanup-candidate predicate.
     *
     * 用法 / Usage: 仅用于分块回收的标记与守护。/ Used only by the cleanup marker and guard.
     */
    private static final String CHUNK_GC_DELETED = "CHUNK_GC_DELETED";

    @Qualifier("gatewayReleasePublicationPersistenceRepository")
    private final GatewayReleasePublicationPersistenceRepository publicationPersistenceRepository;

    @Qualifier("gatewayReleasePersistenceRepository")
    private final GatewayReleasePersistenceRepository releasePersistenceRepository;

    @Qualifier("gatewayReleaseContentPersistenceRepository")
    private final GatewayReleaseContentPersistenceRepository contentPersistenceRepository;

    @Qualifier("gatewayDraftPersistenceRepository")
    private final GatewayDraftPersistenceRepository draftPersistenceRepository;

    @Qualifier("gatewayReleasePublicationPersistenceConverter")
    private final GatewayReleasePublicationPersistenceConverter publicationPersistenceConverter;

    /**
     * 中文说明：执行 insertAll 操作；沿用旧实现的整批登记语义与事务边界：空批次与 {@code null} 元素按旧文案抛出非法参数异常，
     * 每条操作经转换器渲染为行后受守卫插入，技术主键、租户与审计列由边界补齐。
     * English summary: Executes the insert-all operation; the legacy batch registration semantics and transaction boundary are kept:
     * an empty batch and a {@code null} element raise the legacy illegal-argument failures, every operation is rendered into a row by
     * the converter and inserted through the guarded boundary, which supplies the technical key, tenant and audit columns.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayReleasePublicationRepository.insertAll(operations)}。
     * @param operations 参数 发布操作集合；parameter publication operations。
     */
    @Override
    @Transactional
    public void insertAll(List<GatewayReleasePublicationBO> operations) {
        if (operations == null || operations.isEmpty()) {
            throw new IllegalArgumentException(
                    "publication operations must not be empty"
            );
        }
        operations.forEach(this::insert);
    }

    /**
     * 中文说明：执行 findAttempt 操作；等价旧 {@code WHERE release_id = ? AND attempt_no = ? ORDER BY phase_order}，
     * 追加技术 id 升序保证同序时的稳定次序，编号不可命中时如实返回空列表。
     * English summary: Executes the find attempt operation; equivalent to the legacy
     * {@code WHERE release_id = ? AND attempt_no = ? ORDER BY phase_order} with a stable technical-id tie-break, returning an empty
     * list truthfully when the identifier cannot match the technical column.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayReleasePublicationRepository.findAttempt(releaseId, attemptNo)}。
     * @param releaseId 参数 发布Id；parameter release id。
     * @param attemptNo 参数 attemptNo；parameter attempt no。
     * @return 返回 findAttempt 的处理结果；returns the result of the operation.
     */
    @Override
    public List<GatewayReleasePublicationBO> findAttempt(
            String releaseId,
            int attemptNo) {
        Long key = technicalId(releaseId);
        if (key == null) {
            return List.of();
        }
        return publicationPersistenceConverter.toBusinessList(
                publicationPersistenceRepository.list(
                        boundPredicate(Wrappers.<GatewayReleasePublicationRecordPO>lambdaQuery()
                                .eq(GatewayReleasePublicationRecordPO::getReleaseId, key)
                                .eq(GatewayReleasePublicationRecordPO::getAttemptNo, (long) attemptNo)
                                .orderByAsc(GatewayReleasePublicationRecordPO::getPhaseOrder)
                                .orderByAsc(GatewayReleasePublicationRecordPO::getId))
                )
        );
    }

    /**
     * 中文说明：执行 nextIncomplete 操作；等价旧 {@code ddc_status <> 'SUCCESS' ORDER BY phase_order LIMIT 1}：
     * 受守卫边界不做列裁剪，故按同一谓词升序读取并取首个，语义与 LIMIT 1 一致。
     * English summary: Executes the next incomplete operation; equivalent to the legacy
     * {@code ddc_status <> 'SUCCESS' ORDER BY phase_order LIMIT 1}: the guarded boundary performs no column trimming, so the same
     * predicate is read ascending and the first element wins, which matches {@code LIMIT 1}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayReleasePublicationRepository.nextIncomplete(releaseId, attemptNo)}。
     * @param releaseId 参数 发布Id；parameter release id。
     * @param attemptNo 参数 attemptNo；parameter attempt no。
     * @return 返回 nextIncomplete 的处理结果；returns the result of the operation.
     */
    @Override
    public Optional<GatewayReleasePublicationBO> nextIncomplete(
            String releaseId,
            int attemptNo) {
        Long key = technicalId(releaseId);
        if (key == null) {
            return Optional.empty();
        }
        return publicationPersistenceConverter.toBusinessList(
                publicationPersistenceRepository.list(
                        boundPredicate(Wrappers.<GatewayReleasePublicationRecordPO>lambdaQuery()
                                .eq(GatewayReleasePublicationRecordPO::getReleaseId, key)
                                .eq(GatewayReleasePublicationRecordPO::getAttemptNo, (long) attemptNo)
                                .ne(
                                        GatewayReleasePublicationRecordPO::getDdcStatus,
                                        GatewayPublicationStatusEnum.SUCCESS.name()
                                )
                                .orderByAsc(GatewayReleasePublicationRecordPO::getPhaseOrder)
                                .orderByAsc(GatewayReleasePublicationRecordPO::getId))
                )
        ).stream().findFirst();
    }

    /**
     * 中文说明：执行 findChunkCleanupCandidates 操作；把旧的三表联查拆成受守卫的多步读取再在内存中相关，
     * 完整保留全部谓词：仅 {@code CHUNK} 阶段且已带作用域、目标版本与 {@code SUCCESS} 状态且未标记
     * {@code CHUNK_GC_DELETED} 的日志行才成为候选；同组存在 {@link #BLOCKING_RELEASE_STATUSES} 状态的发布、
     * 或仍有草稿以该发布为基线时排除；必须存在同组、创建更晚、且在截止时间前已成功激活同一作用域的 ACTIVITY 后继；
     * 结果按旧 {@code ORDER BY old_release.created_at, publication.phase_order} 排序。
     * English summary: Executes the find chunk cleanup candidates operation; the legacy three-table join becomes several guarded reads
     * correlated in memory while every predicate is preserved: only {@code CHUNK} journal rows that carry a scope, a target version
     * and {@code SUCCESS} without the {@code CHUNK_GC_DELETED} mark become candidates, a group holding a release in
     * {@link #BLOCKING_RELEASE_STATUSES} or a draft still based on the release excludes them, a later-created successor of the same
     * group whose ACTIVATION for the very same scope succeeded before the deadline is required, and the result keeps the legacy
     * {@code ORDER BY old_release.created_at, publication.phase_order}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayReleasePublicationRepository.findChunkCleanupCandidates(now)}。
     * 定时回收器须在可信租户上下文内调用。/ The scheduled collector must call it with a trusted tenant context bound.
     * @param successorActivatedBefore 参数 后继激活的截止时间；parameter latest allowed successor activation instant。
     * @return 返回 findChunkCleanupCandidates 的处理结果；returns the result of the operation.
     */
    @Override
    public List<GatewayChunkCleanupCandidateBO> findChunkCleanupCandidates(
            Instant successorActivatedBefore) {
        List<GatewayReleasePublicationRecordPO> chunks =
                publicationPersistenceRepository.list(
                        boundPredicate(Wrappers.<GatewayReleasePublicationRecordPO>lambdaQuery()
                                .eq(
                                        GatewayReleasePublicationRecordPO::getPhaseType,
                                        GatewayPublicationPhaseEnum.CHUNK.name()
                                )
                                .isNotNull(GatewayReleasePublicationRecordPO::getTargetRole)
                                .eq(
                                        GatewayReleasePublicationRecordPO::getDdcStatus,
                                        GatewayPublicationStatusEnum.SUCCESS.name()
                                )
                                .isNotNull(GatewayReleasePublicationRecordPO::getDdcTargetVersion)
                                .and(wrapper -> wrapper
                                        .ne(
                                                GatewayReleasePublicationRecordPO::getErrorCode,
                                                CHUNK_GC_DELETED
                                        )
                                        .or()
                                        .isNull(GatewayReleasePublicationRecordPO::getErrorCode)))
                );
        if (chunks.isEmpty()) {
            return List.of();
        }
        Set<Long> candidateReleaseIds = chunks.stream()
                .map(GatewayReleasePublicationRecordPO::getReleaseId)
                .filter(Objects::nonNull)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        if (candidateReleaseIds.isEmpty()) {
            return List.of();
        }
        Map<Long, GatewayReleaseRecordPO> oldReleases = new LinkedHashMap<>();
        releasePersistenceRepository.listByIds(candidateReleaseIds)
                .forEach(row -> oldReleases.put(row.getId(), row));
        Set<Long> groups = oldReleases.values().stream()
                .map(GatewayReleaseRecordPO::getGatewayGroupId)
                .filter(Objects::nonNull)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        Set<Long> blockedGroups = new HashSet<>();
        Set<Long> groupReleaseIds = new LinkedHashSet<>();
        Map<Long, List<GatewayReleaseRecordPO>> headsByGroup = new LinkedHashMap<>();
        if (!groups.isEmpty()) {
            releasePersistenceRepository.list(
                    boundPredicate(Wrappers.<GatewayReleaseRecordPO>lambdaQuery()
                            .in(GatewayReleaseRecordPO::getGatewayGroupId, groups))
            ).forEach(row -> {
                groupReleaseIds.add(row.getId());
                if (row.getGatewayGroupId() != null) {
                    headsByGroup.computeIfAbsent(
                            row.getGatewayGroupId(),
                            ignored -> new ArrayList<>()
                    ).add(row);
                }
                if (BLOCKING_RELEASE_STATUSES.contains(row.getStatus())) {
                    blockedGroups.add(row.getGatewayGroupId());
                }
            });
        }
        Set<String> draftedReleases = draftedReleases(candidateReleaseIds);
        Map<Long, String> namespaces = namespaces(candidateReleaseIds);
        Set<String> activatedScopes = activatedScopes(groupReleaseIds, successorActivatedBefore);
        List<Candidate> candidates = new ArrayList<>();
        for (GatewayReleasePublicationRecordPO chunk : chunks) {
            GatewayReleaseRecordPO oldRelease = oldReleases.get(chunk.getReleaseId());
            if (oldRelease == null
                    || blockedGroups.contains(oldRelease.getGatewayGroupId())
                    || draftedReleases.contains(opaque(chunk.getReleaseId()))
                    || chunk.getAttemptNo() == null) {
                continue;
            }
            String scope = scopeKey(
                    chunk.getReleaseId(),
                    chunk.getTargetRole(),
                    chunk.getTargetBizCode(),
                    chunk.getTargetEnv(),
                    chunk.getTargetAppCode()
            );
            if (!hasActivatedSuccessor(
                    oldRelease,
                    headsByGroup.getOrDefault(
                            oldRelease.getGatewayGroupId(),
                            List.of()
                    ),
                    chunk,
                    activatedScopes)) {
                continue;
            }
            candidates.add(new Candidate(
                    oldRelease,
                    chunk,
                    namespaces.get(chunk.getReleaseId()),
                    scope
            ));
        }
        return candidates.stream()
                .sorted(Comparator
                        .comparing((Candidate candidate) ->
                                        candidate.release().getCreateTime(),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(candidate -> candidate.journal().getPhaseOrder(),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(candidate -> candidate.journal().getId(),
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::cleanupCandidate)
                .toList();
    }

    /**
     * 中文说明：执行 resolveDocument 操作；等价旧 {@code UPDATE ... SET expected_version, content_value,
     * ddc_status='RESOLVED', updated_at WHERE change_id = ? AND ddc_status <> 'SUCCESS'}：入参守护沿用旧文案与异常类型，
     * 未命中行或 0 行按旧「成功发布不可再次解析」如实抛出。
     * English summary: Executes the resolve document operation; equivalent to the legacy
     * {@code UPDATE ... SET expected_version, content_value, ddc_status='RESOLVED', updated_at WHERE change_id = ? AND ddc_status <>
     * 'SUCCESS'}: the argument guards keep the legacy messages and exception types, and a missed row or a zero-row write raises the
     * legacy "successful publication cannot be resolved again" failure.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayReleasePublicationRepository.resolveDocument(changeId, expectedVersion,
     * documentContent, now)}。
     * @param changeId 参数 外部变更Id；parameter external change id。
     * @param expectedVersion 参数 期望配置版本；parameter expected configuration version。
     * @param documentContent 参数 文档正文；parameter document content。
     * @param now 参数 解析时间；parameter resolution instant。
     */
    @Override
    public void resolveDocument(
            String changeId,
            long expectedVersion,
            String documentContent,
            Instant now) {
        if (expectedVersion < 0) {
            throw new IllegalArgumentException(
                    "expectedVersion must not be negative"
            );
        }
        if (documentContent == null || documentContent.isBlank()) {
            throw new IllegalArgumentException(
                    "documentContent must not be blank"
            );
        }
        requireChanged(
                advance(
                        Wrappers.<GatewayReleasePublicationRecordPO>lambdaQuery()
                                .eq(GatewayReleasePublicationRecordPO::getChangeId, changeId)
                                .ne(
                                        GatewayReleasePublicationRecordPO::getDdcStatus,
                                        GatewayPublicationStatusEnum.SUCCESS.name()
                                ),
                        Wrappers.<GatewayReleasePublicationRecordPO>lambdaUpdate()
                                .set(
                                        GatewayReleasePublicationRecordPO::getExpectedVersion,
                                        expectedVersion
                                )
                                .set(
                                        GatewayReleasePublicationRecordPO::getContentValue,
                                        documentContent
                                )
                                .set(
                                        GatewayReleasePublicationRecordPO::getDdcStatus,
                                        GatewayPublicationStatusEnum.RESOLVED.name()
                                ),
                        changeId,
                        now
                ),
                "successful publication cannot be resolved again"
        );
    }

    /**
     * 中文说明：执行 markSubmitted 操作；等价旧 {@code WHERE change_id = ? AND ddc_status='RESOLVED' AND
     * expected_version IS NOT NULL}，未命中或 0 行按旧「必须先 RESOLVED」文案抛出。
     * English summary: Executes the mark submitted operation; equivalent to the legacy
     * {@code WHERE change_id = ? AND ddc_status='RESOLVED' AND expected_version IS NOT NULL}, raising the legacy "publication must
     * be RESOLVED before submission" message when nothing matched.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayReleasePublicationRepository.markSubmitted(changeId, now)}。
     * @param changeId 参数 外部变更Id；parameter external change id。
     * @param now 参数 提交时间；parameter submission instant。
     */
    @Override
    public void markSubmitted(String changeId, Instant now) {
        requireChanged(
                advance(
                        Wrappers.<GatewayReleasePublicationRecordPO>lambdaQuery()
                                .eq(GatewayReleasePublicationRecordPO::getChangeId, changeId)
                                .eq(
                                        GatewayReleasePublicationRecordPO::getDdcStatus,
                                        GatewayPublicationStatusEnum.RESOLVED.name()
                                )
                                .isNotNull(GatewayReleasePublicationRecordPO::getExpectedVersion),
                        Wrappers.<GatewayReleasePublicationRecordPO>lambdaUpdate()
                                .set(
                                        GatewayReleasePublicationRecordPO::getDdcStatus,
                                        GatewayPublicationStatusEnum.SUBMITTED.name()
                                ),
                        changeId,
                        now
                ),
                "publication must be RESOLVED before submission"
        );
    }

    /**
     * 中文说明：执行 markResult 操作；先按旧文案校验终态与成功结果必须带目标版本，再等价旧
     * {@code UPDATE ... SET ddc_target_version, ddc_status, error_code, error_message, updated_at WHERE change_id = ? AND
     * ddc_status IN (可记录集合)}；允许为 NULL 的列经更新构造器显式下推，未命中或 0 行按旧文案抛出。
     * English summary: Executes the mark result operation; the legacy messages guard the terminal status and the target version a
     * successful result requires, then the legacy {@code UPDATE ... SET ddc_target_version, ddc_status, error_code, error_message,
     * updated_at WHERE change_id = ? AND ddc_status IN (recordable set)} is applied; columns allowed to be NULL are pushed
     * explicitly by the update wrapper, and a missed row or zero-row write raises the legacy message.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayReleasePublicationRepository.markResult(...)}。
     * @param changeId 参数 外部变更Id；parameter external change id。
     * @param targetVersion 参数 引擎目标版本；parameter engine target version。
     * @param status 参数 终态状态；parameter terminal status。
     * @param errorCode 参数 错误码；parameter error code。
     * @param errorMessage 参数 错误说明；parameter error message。
     * @param now 参数 结果时间；parameter result instant。
     */
    @Override
    public void markResult(
            String changeId,
            Long targetVersion,
            GatewayPublicationStatusEnum status,
            String errorCode,
            String errorMessage,
            Instant now) {
        if (status == null || !status.terminalResult()) {
            throw new IllegalArgumentException(
                    "publication result status must be terminal"
            );
        }
        if (status == GatewayPublicationStatusEnum.SUCCESS && targetVersion == null) {
            throw new IllegalArgumentException(
                    "successful publication requires targetVersion"
            );
        }
        requireChanged(
                advance(
                        Wrappers.<GatewayReleasePublicationRecordPO>lambdaQuery()
                                .eq(GatewayReleasePublicationRecordPO::getChangeId, changeId)
                                .in(
                                        GatewayReleasePublicationRecordPO::getDdcStatus,
                                        RECORDABLE_STATUSES
                                ),
                        Wrappers.<GatewayReleasePublicationRecordPO>lambdaUpdate()
                                .set(
                                        GatewayReleasePublicationRecordPO::getDdcTargetVersion,
                                        targetVersion
                                )
                                .set(
                                        GatewayReleasePublicationRecordPO::getDdcStatus,
                                        status.name()
                                )
                                .set(
                                        GatewayReleasePublicationRecordPO::getErrorCode,
                                        errorCode
                                )
                                .set(
                                        GatewayReleasePublicationRecordPO::getErrorMessage,
                                        errorMessage
                                ),
                        changeId,
                        now
                ),
                "publication result cannot be recorded"
        );
    }

    /**
     * 中文说明：执行 markChunkCleaned 操作；等价旧 {@code SET error_code='CHUNK_GC_DELETED', error_message=NULL,
     * updated_at=? WHERE change_id = ? AND phase_type='CHUNK' AND ddc_status='SUCCESS' AND ddc_target_version IS NOT NULL}，
     * 未命中或 0 行按旧「未找到已清理分块」文案抛出。
     * English summary: Executes the mark chunk cleaned operation; equivalent to the legacy
     * {@code SET error_code='CHUNK_GC_DELETED', error_message=NULL, updated_at=? WHERE change_id = ? AND phase_type='CHUNK' AND
     * ddc_status='SUCCESS' AND ddc_target_version IS NOT NULL}, raising the legacy "cleaned chunk publication was not found" message
     * when nothing matched.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayReleasePublicationRepository.markChunkCleaned(changeId, now)}。
     * @param changeId 参数 外部变更Id；parameter external change id。
     * @param now 参数 清理时间；parameter cleanup instant。
     */
    @Override
    public void markChunkCleaned(String changeId, Instant now) {
        requireChanged(
                advance(
                        Wrappers.<GatewayReleasePublicationRecordPO>lambdaQuery()
                                .eq(GatewayReleasePublicationRecordPO::getChangeId, changeId)
                                .eq(
                                        GatewayReleasePublicationRecordPO::getPhaseType,
                                        GatewayPublicationPhaseEnum.CHUNK.name()
                                )
                                .eq(
                                        GatewayReleasePublicationRecordPO::getDdcStatus,
                                        GatewayPublicationStatusEnum.SUCCESS.name()
                                )
                                .isNotNull(GatewayReleasePublicationRecordPO::getDdcTargetVersion),
                        Wrappers.<GatewayReleasePublicationRecordPO>lambdaUpdate()
                                .set(
                                        GatewayReleasePublicationRecordPO::getErrorCode,
                                        CHUNK_GC_DELETED
                                )
                                .set(GatewayReleasePublicationRecordPO::getErrorMessage, null),
                        changeId,
                        now
                ),
                "cleaned chunk publication was not found"
        );
    }

    /**
     * 中文说明：执行 insert 操作；把单条发布操作经转换器渲染为行后受守卫插入，
     * 等价旧 {@code INSERT INTO gateway_release_publication(...)}；0 行按冲突如实抛出。
     * English summary: Executes the insert operation; a single publication operation is rendered into a row by the converter and
     * inserted through the guarded boundary, equivalent to the legacy {@code INSERT INTO gateway_release_publication(...)}; a
     * zero-row effect surfaces as a conflict.
     *
     * 用法 / Usage: 仅由 {@link #insertAll(List)} 调用。
     * @param operation 参数 发布操作；parameter publication operation。
     */
    private void insert(GatewayReleasePublicationBO operation) {
        if (operation == null) {
            throw new IllegalArgumentException(
                    "publication operation must not be null"
            );
        }
        if (!publicationPersistenceRepository.save(
                publicationPersistenceConverter.newRow(operation))) {
            throw new IllegalStateException(
                    "YUHENG_ADMIN_RELEASE_PUBLICATION_INSERT_CONFLICT"
            );
        }
    }

    /**
     * 中文说明：执行 advance 操作；把旧单条 UPDATE 拆成「受守卫定位 + 乐观锁 CAS 写入」两步：先按旧 WHERE 谓词读取活跃行
     * 取得 CAS 令牌（定位与更新两份条件都在交付边界前成形一次，使参数随谓词一并就位），再把业务列连同旧
     * {@code updated_at} 与 {@code WHERE change_id = ? AND id = ?} 一并下推；
     * 谓词不命中、行缺失或影响 0 行一律如实返回 false 由调用方按旧文案抛出。
     * English summary: Executes the advance operation; the legacy single UPDATE becomes a guarded locate plus an optimistic-lock CAS
     * write: the active row is read by the legacy WHERE predicates to obtain the CAS token (both conditions are formed once before
     * crossing the guarded boundary so their parameters arrive fully bound), then the business columns are pushed
     * together with the legacy {@code updated_at} and {@code WHERE change_id = ? AND id = ?}; an unmatched predicate, an absent row or
     * a zero-row effect truthfully returns false for the caller to translate into the legacy message.
     *
     * 用法 / Usage: 由本类四个状态推进方法调用。
     * @param location 参数 旧 WHERE 谓词的定位查询；parameter locating query carrying the legacy predicates。
     * @param changes 参数 已渲染业务列的更新构造器；parameter update wrapper carrying the business columns。
     * @param changeId 参数 外部变更Id；parameter external change id。
     * @param now 参数 旧 SQL 写入 {@code updated_at} 的可信时刻；parameter the trusted instant the legacy SQL wrote to {@code updated_at}。
     * @return 返回 是否恰好推进了一行；returns whether exactly one row advanced.
     */
    private boolean advance(
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GatewayReleasePublicationRecordPO> location,
            LambdaUpdateWrapper<GatewayReleasePublicationRecordPO> changes,
            String changeId,
            Instant now) {
        Optional<GatewayReleasePublicationRecordPO> current = publicationPersistenceRepository.getOneOpt(
                boundPredicate(location));
        if (current.isEmpty()) {
            return false;
        }
        GatewayReleasePublicationRecordPO row = current.get();
        GatewayReleasePublicationRecordPO token = new GatewayReleasePublicationRecordPO();
        token.setId(row.getId());
        token.setVersion(row.getVersion());
        token.setUpdateTime(now);
        return publicationPersistenceRepository.update(
                token,
                boundPredicate(changes
                        .eq(GatewayReleasePublicationRecordPO::getId, row.getId())
                        .eq(GatewayReleasePublicationRecordPO::getChangeId, changeId))
        );
    }

    /**
     * 中文说明：执行 draftedReleases 操作；等价旧 {@code NOT EXISTS (SELECT 1 FROM gateway_draft WHERE
     * based_on_release_id = publication.release_id)}：一次批量读取以候选发布为基线的草稿，返回被占用的不透明编号集合。
     * English summary: Executes the draftedReleases operation; equivalent to the legacy
     * {@code NOT EXISTS (SELECT 1 FROM gateway_draft WHERE based_on_release_id = publication.release_id)}: the drafts based on the
     * candidate releases are read in one batch and the occupied opaque identifiers are returned.
     *
     * 用法 / Usage: 仅由 {@link #findChunkCleanupCandidates(Instant)} 调用。
     * @param releaseIds 参数 候选发布技术编号；parameter candidate technical release identifiers。
     * @return 返回 仍有草稿基线的不透明编号集合；returns the opaque identifiers still backed by a draft.
     */
    private Set<String> draftedReleases(Set<Long> releaseIds) {
        Set<String> opaque = new HashSet<>();
        draftPersistenceRepository.list(
                boundPredicate(Wrappers.<GatewayDraftRecordPO>lambdaQuery()
                        .in(GatewayDraftRecordPO::getBasedOnReleaseId, releaseIds.stream().map(MpGatewayReleasePublicationRepository::opaque).toList()))
        ).forEach(row -> opaque.add(row.getBasedOnReleaseId()));
        return opaque;
    }

    /**
     * 中文说明：执行 namespaces 操作；等价旧 SQL 的 {@code content.canonical_snapshot -> 'content' ->> 'namespace'}
     * 投影：受守卫边界不支持 JSON 路径投影，故一次批量读取内容快照行，按发布归并出 namespace 文本（列缺失或非文本时如实为 null）。
     * English summary: Executes the namespaces operation; equivalent to the legacy SQL projection
     * {@code content.canonical_snapshot -> 'content' ->> 'namespace'}: the guarded boundary supports no JSON path projection, so the
     * content rows are read in one batch and the namespace text is merged per release (null when the member is absent or not textual).
     *
     * 用法 / Usage: 仅由 {@link #findChunkCleanupCandidates(Instant)} 调用。
     * @param releaseIds 参数 候选发布技术编号；parameter candidate technical release identifiers。
     * @return 返回 发布到命名空间的映射；returns the release to namespace map.
     */
    private Map<Long, String> namespaces(Set<Long> releaseIds) {
        Map<Long, String> namespaces = new LinkedHashMap<>();
        contentPersistenceRepository.list(
                boundPredicate(Wrappers.<GatewayReleaseContentPO>lambdaQuery()
                        .in(GatewayReleaseContentPO::getReleaseId, releaseIds))
        ).forEach(row -> {
            JsonNode snapshot = row.getCanonicalSnapshot();
            namespaces.put(
                    row.getReleaseId(),
                    snapshot == null
                            ? null
                            : snapshot.path("content").path("namespace").isTextual()
                            ? snapshot.path("content").path("namespace").asText()
                            : null
            );
        });
        return namespaces;
    }

    /**
     * 中文说明：执行 activatedScopes 操作；等价旧 EXISTS 子查询的激活侧条件：一次批量读取相关发布内
     * {@code phase_type='ACTIVATION'}、{@code ddc_status='SUCCESS'} 且 {@code updated_at <= ?} 的日志行，
     * 归并成「发布 + 作用域」键集合，供逐个候选判断是否存在更晚创建的后继。
     * English summary: Executes the activatedScopes operation; the activation side of the legacy EXISTS sub-select: the journal rows
     * of the involved releases with {@code phase_type='ACTIVATION'}, {@code ddc_status='SUCCESS'} and {@code updated_at <= ?} are read
     * in one batch and merged into a set of release-plus-scope keys, so every candidate can test whether a later-created successor
     * activated the very same scope.
     *
     * 用法 / Usage: 仅由 {@link #findChunkCleanupCandidates(Instant)} 调用。
     * @param releaseIds 参数 组内全部发布技术编号；parameter every technical release identifier of the groups。
     * @param successorActivatedBefore 参数 后继激活的截止时间；parameter latest allowed successor activation instant。
     * @return 返回 已激活作用域键集合；returns the activated scope keys.
     */
    private Set<String> activatedScopes(
            Set<Long> releaseIds,
            Instant successorActivatedBefore) {
        Set<String> scopes = new HashSet<>();
        if (releaseIds.isEmpty()) {
            return scopes;
        }
        publicationPersistenceRepository.list(
                boundPredicate(Wrappers.<GatewayReleasePublicationRecordPO>lambdaQuery()
                        .in(GatewayReleasePublicationRecordPO::getReleaseId, releaseIds)
                        .eq(
                                GatewayReleasePublicationRecordPO::getPhaseType,
                                GatewayPublicationPhaseEnum.ACTIVATION.name()
                        )
                        .eq(
                                GatewayReleasePublicationRecordPO::getDdcStatus,
                                GatewayPublicationStatusEnum.SUCCESS.name()
                        )
                        .isNotNull(GatewayReleasePublicationRecordPO::getTargetRole)
                        .le(GatewayReleasePublicationRecordPO::getUpdateTime, successorActivatedBefore))
        ).forEach(row -> scopes.add(scopeKey(
                row.getReleaseId(),
                row.getTargetRole(),
                row.getTargetBizCode(),
                row.getTargetEnv(),
                row.getTargetAppCode()
        )));
        return scopes;
    }

    /**
     * 中文说明：执行 hasActivatedSuccessor 操作；在内存中完成旧 EXISTS 的相关部分：同组、创建晚于本发布、
     * 且该作用域已在截止时间前成功激活的后继存在即为真。
     * English summary: Executes the has activated successor operation; the correlated half of the legacy EXISTS runs in memory: a
     * successor of the same group created after this release whose scope activated successfully before the deadline makes it true.
     *
     * 用法 / Usage: 仅由 {@link #findChunkCleanupCandidates(Instant)} 调用。
     * @param oldRelease 参数 候选所属发布头；parameter release head owning the candidate。
     * @param successors 参数 同组全部发布头；parameter every release head of the same group。
     * @param chunk 参数 候选日志行；parameter candidate journal row。
     * @param activatedScopes 参数 已激活作用域键集合；parameter the activated scope keys。
     * @return 返回 是否存在已激活后继；returns whether an activated successor exists.
     */
    private static boolean hasActivatedSuccessor(
            GatewayReleaseRecordPO oldRelease,
            List<GatewayReleaseRecordPO> successors,
            GatewayReleasePublicationRecordPO chunk,
            Set<String> activatedScopes) {
        Instant created = oldRelease.getCreateTime();
        if (created == null) {
            return false;
        }
        return successors.stream()
                .filter(successor -> successor.getCreateTime() != null
                        && successor.getCreateTime().isAfter(created))
                .anyMatch(successor -> activatedScopes.contains(scopeKey(
                        successor.getId(),
                        chunk.getTargetRole(),
                        chunk.getTargetBizCode(),
                        chunk.getTargetEnv(),
                        chunk.getTargetAppCode()
                )));
    }

    /**
     * 中文说明：执行 scopeKey 操作；把旧 SQL 逐列比较的四个作用域列渲染为可比较的稳定键，NULL 以空串占位，
     * 与旧 {@code activation.target_role = publication.target_role} 等等值比较一致（NULL 永不等于 NULL）。
     * English summary: Executes the scopeKey operation; the four scope columns the legacy SQL compared column by column become a
     * comparable stable key, with NULL standing for an empty string, matching the legacy equality tests where NULL never equals NULL.
     *
     * 用法 / Usage: 仅用于分块回收候选的后继判定。
     * @param releaseId 参数 发布技术编号；parameter technical release identifier。
     * @param role 参数 引擎角色；parameter engine role。
     * @param bizCode 参数 业务码；parameter business code。
     * @param env 参数 环境；parameter environment。
     * @param appCode 参数 应用码；parameter application code。
     * @return 返回 作用域键；returns the scope key.
     */
    private static String scopeKey(
            Long releaseId,
            String role,
            String bizCode,
            String env,
            String appCode) {
        return releaseId + "|" + Objects.toString(role, "") + "|"
                + Objects.toString(bizCode, "") + "|"
                + Objects.toString(env, "") + "|"
                + Objects.toString(appCode, "");
    }

    /**
     * 中文说明：执行 cleanupCandidate 操作；把候选日志行经转换器投影为业务载体后，与旧 SQL 投影一致的
     * 变更号、不透明发布号、作用域、命名空间、配置键与目标版本组装为回收候选。
     * English summary: Executes the cleanupCandidate operation; the candidate journal row is projected by the converter onto the
     * business carrier and then assembled into a cleanup candidate carrying the change id, opaque release id, scope, namespace,
     * configuration key and target version exactly as the legacy projection did.
     *
     * 用法 / Usage: 仅由 {@link #findChunkCleanupCandidates(Instant)} 调用。
     * @param candidate 参数 候选行与相关上下文；parameter candidate row with its correlated context。
     * @return 返回 回收候选；returns the cleanup candidate.
     */
    private GatewayChunkCleanupCandidateBO cleanupCandidate(Candidate candidate) {
        GatewayReleasePublicationBO carrier =
                publicationPersistenceConverter.toBusiness(candidate.journal());
        return new GatewayChunkCleanupCandidateBO(
                carrier.getChangeId(),
                carrier.getReleaseId(),
                carrier.getTargetScope() == null ? null : carrier.getTargetScope().appCode(),
                carrier.getTargetScope() == null ? null : carrier.getTargetScope().env(),
                candidate.namespace(),
                carrier.getConfigKey(),
                carrier.getDdcTargetVersion(),
                carrier.getTargetScope()
        );
    }

    /**
     * 中文说明：把条件交付受守卫边界之前先成形一次：MyBatis-Plus 的 {@code eq/in/ne/le} 只在 SQL 真正成形时
     * 才把取值写进 {@code paramNameValuePairs}，故此处先成形一次，让谓词在离开门面时参数已完全绑定；
     * 之后（包括 MyBatis 自己下发时）命中同一份片段缓存，参数与 SQL 都不再改变，列解析失败
     * （lambda 缓存缺失）也如实在门面这一层暴露，而不是留到语句下发时。
     * English summary: Forms a condition once before it is handed to the guarded boundary: MyBatis-Plus only moves the
     * values of {@code eq/in/ne/le} into {@code paramNameValuePairs} while the SQL is being formed, so forming it here
     * first means the parameters are fully bound when the predicate leaves the facade; later renders (including the one
     * MyBatis performs) hit the same segment cache and change neither the parameters nor the SQL, while a column
     * resolution failure (a missing lambda cache) surfaces truthfully at the facade instead of at statement time.
     * @param predicate 参数 已构造完成的业务条件；parameter the completed business condition.
     * @return 返回 同一份参数已绑定的条件；returns the very same condition with its parameters bound.
     */
    private static <C extends Wrapper<?>> C boundPredicate(C predicate) {
        predicate.getSqlSegment();
        return predicate;
    }

    /**
     * 中文说明：执行 requireChanged 操作；等价旧实现的 0 行/多行守护，影响行数不是恰好一行时按旧文案抛出。
     * English summary: Executes the requireChanged operation; equivalent to the legacy zero-row and multi-row guard, raising the
     * legacy message when the affected rows are not exactly one.
     *
     * 用法 / Usage: 每次状态推进后调用。
     * @param changed 参数 是否恰好推进一行；parameter whether exactly one row advanced。
     * @param message 参数 失败文案；parameter failure message。
     */
    private static void requireChanged(boolean changed, String message) {
        if (!changed) {
            throw new IllegalStateException(message);
        }
    }

    /**
     * 中文说明：执行 不透明编号投影 操作；端口携带的十进制不透明编号映射为新 {@code bigint} 技术列的谓词值，
     * null 与空白在旧 VARCHAR 主键模型下永远不可能命中，故如实返回 null 让调用方映射为空读或 0 行。
     * English summary: Executes the opaque identifier projection; the decimal opaque identifier carried by the port becomes the
     * predicate value of the new {@code bigint} technical column, and null or blank input could never match under the legacy VARCHAR
     * primary-key model, so it truthfully yields null for the caller to map into an empty read or a zero-row write.
     *
     * 用法 / Usage: 仅用于为本类查询准备技术键。
     * @param opaqueId 参数 不透明编号；parameter opaque identifier。
     * @return 返回 技术编号或 null；returns the technical identifier or null.
     */
    private static Long technicalId(String opaqueId) {
        if (opaqueId == null || opaqueId.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(opaqueId.trim());
        } catch (NumberFormatException invalidOpaqueId) {
            return null;
        }
    }

    /**
     * 中文说明：执行 编号文本投影 操作；把技术编号还原为端口使用的十进制文本，供与仍为文本的草稿基线列比较。
     * English summary: Executes the identifier text projection; a technical identifier becomes the decimal text the port carries, so
     * it can be compared with the draft baseline column that stayed textual.
     *
     * 用法 / Usage: 仅用于草稿基线守护。
     * @param id 参数 技术编号；parameter technical identifier。
     * @return 返回 编号文本；returns the identifier text.
     */
    private static String opaque(Long id) {
        return id == null ? null : Long.toString(id);
    }

    /**
     * 中文说明：{@code Candidate} 是不可变候选上下文，承载候选日志行、其发布头与命名空间投影，避免在排序阶段重复查表。
     * English summary: {@code Candidate} is the immutable candidate context carrying the journal row, its release head and the
     * namespace projection so the sorting phase never re-reads a table.
     *
     * 用法 / Usage: 仅在本类的分块回收流程内使用。/ Used only inside the cleanup flow of this class.
     * @param release 参数 候选所属发布头；parameter release head owning the candidate。
     * @param journal 参数 候选日志行；parameter candidate journal row。
     * @param namespace 参数 快照命名空间投影；parameter snapshot namespace projection。
     * @param scopeKey 参数 候选作用域键；parameter candidate scope key。
     */
    private record Candidate(
            GatewayReleaseRecordPO release,
            GatewayReleasePublicationRecordPO journal,
            String namespace,
            String scopeKey
    ) {
    }
}
