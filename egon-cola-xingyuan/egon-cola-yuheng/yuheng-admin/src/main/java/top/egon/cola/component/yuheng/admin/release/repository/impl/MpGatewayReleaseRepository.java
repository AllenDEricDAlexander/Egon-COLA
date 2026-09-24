package top.egon.cola.component.yuheng.admin.release.repository.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.release.converter.GatewayReleaseAttemptPersistenceConverter;
import top.egon.cola.component.yuheng.admin.release.converter.GatewayReleasePersistenceConverter;
import top.egon.cola.component.yuheng.admin.release.converter.GatewayReleaseTargetPersistenceConverter;
import top.egon.cola.component.yuheng.admin.release.domain.bo.GatewayRecoverableReleaseAttemptBO;
import top.egon.cola.component.yuheng.admin.release.domain.bo.GatewayReleaseAttemptBO;
import top.egon.cola.component.yuheng.admin.release.domain.bo.GatewayReleaseBO;
import top.egon.cola.component.yuheng.admin.release.domain.bo.GatewayReleaseTargetBO;
import top.egon.cola.component.yuheng.admin.release.domain.enums.GatewayReleaseStatus;
import top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseAttemptRecordPO;
import top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseContentPO;
import top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleasePublicationRecordPO;
import top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseRecordPO;
import top.egon.cola.component.yuheng.admin.release.domain.po.GatewayReleaseTargetRecordPO;
import top.egon.cola.component.yuheng.admin.release.repository.GatewayReleaseRepository;
import top.egon.cola.component.yuheng.admin.release.repository.mp.GatewayReleaseAttemptPersistenceRepository;
import top.egon.cola.component.yuheng.admin.release.repository.mp.GatewayReleaseContentPersistenceRepository;
import top.egon.cola.component.yuheng.admin.release.repository.mp.GatewayReleasePersistenceRepository;
import top.egon.cola.component.yuheng.admin.release.repository.mp.GatewayReleasePublicationPersistenceRepository;
import top.egon.cola.component.yuheng.admin.release.repository.mp.GatewayReleaseTargetPersistenceRepository;
import top.egon.cola.component.yuheng.admin.rule.domain.vo.CompiledGatewayRelease;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuleActivation;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuleSnapshot;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 中文说明：{@code MpGatewayReleaseRepository} 是发布聚合的 MyBatis-Plus 门面存储，逐方法替换被删除的手写 JDBC
 * 实现：发布头、内容快照、尝试日志与实例目标四张表各自经自己的受守卫
 * {@code EgonColaRepository} 边界读写（租户过滤、只读活跃行、乐观锁 CAS、版本化软删），列与 JSON 映射只经
 * {@code release/converter} 下的 MapStruct 转换器完成，公开端口不泄漏 RecordPO 或 DAO。
 * English summary: {@code MpGatewayReleaseRepository} is the MyBatis-Plus facade store of the release aggregate, replacing the
 * deleted hand-written JDBC store method by method: the release head, its content snapshot, the attempt
 * journal and the instance targets each go through their own guarded {@code EgonColaRepository} boundary (tenant filtering,
 * active-only reads, optimistic-lock CAS, versioned soft delete), column and JSON mapping happens only through the MapStruct
 * converters under {@code release/converter}, and the public port leaks neither RecordPO nor DAO.
 *
 * 用法 / Usage: 通过业务端口 {@code GatewayReleaseRepository} 由 Spring 容器注入；多表组合（登记发布、完成尝试）沿用旧实现的「按表分步写」顺序，
 * 事务归属仍由调用方（{@code gatewayTransactionManager}）承担，影响 0 行的写按冲突如实抛出，绝不伪造成功。
 * / Inject it through the business port {@code GatewayReleaseRepository}; multi-table flows keep the legacy
 * table-by-table write order, the caller still owns the transaction ({@code gatewayTransactionManager}), and a zero-row write
 * surfaces as a conflict instead of a fake success.
 */
@Slf4j
@Repository("mpGatewayReleaseRepository")
@RequiredArgsConstructor
@Validated
public class MpGatewayReleaseRepository implements GatewayReleaseRepository {

    /**
     * 中文说明：表示尝试日志的初始状态字面量，历史实现的 attempt 状态机使用 {@code PENDING}，它不在发布头状态枚举内。
     * English summary: Represents the initial attempt-journal status literal; the legacy attempt state machine uses
     * {@code PENDING}, which is not part of the release-head status enum.
     *
     * 用法 / Usage: 仅用于新建尝试行。/ Used only when inserting an attempt row.
     */
    private static final String PENDING_ATTEMPT_STATUS = "PENDING";

    /**
     * 中文说明：表示在途状态集合字面量，等价旧 {@code hasReleaseInProgress} 的 {@code status IN (CREATED, VALIDATING, READY, PUBLISHING)}。
     * English summary: Represents the in-flight status literals, equivalent to the legacy
     * {@code status IN (CREATED, VALIDATING, READY, PUBLISHING)} predicate of {@code hasReleaseInProgress}.
     *
     * 用法 / Usage: 仅用于组内并发守护计数。/ Used only by the per-group concurrency guard count.
     */
    private static final List<String> IN_PROGRESS_STATUSES = List.of(
            GatewayReleaseStatus.CREATED.name(),
            GatewayReleaseStatus.VALIDATING.name(),
            GatewayReleaseStatus.READY.name(),
            GatewayReleaseStatus.PUBLISHING.name()
    );

    @Qualifier("gatewayReleasePersistenceRepository")
    private final GatewayReleasePersistenceRepository releasePersistenceRepository;

    @Qualifier("gatewayReleaseContentPersistenceRepository")
    private final GatewayReleaseContentPersistenceRepository contentPersistenceRepository;

    @Qualifier("gatewayReleaseAttemptPersistenceRepository")
    private final GatewayReleaseAttemptPersistenceRepository attemptPersistenceRepository;

    @Qualifier("gatewayReleaseTargetPersistenceRepository")
    private final GatewayReleaseTargetPersistenceRepository targetPersistenceRepository;

    @Qualifier("gatewayReleasePublicationPersistenceRepository")
    private final GatewayReleasePublicationPersistenceRepository publicationPersistenceRepository;

    @Qualifier("gatewayReleasePersistenceConverter")
    private final GatewayReleasePersistenceConverter releasePersistenceConverter;

    @Qualifier("gatewayReleaseAttemptPersistenceConverter")
    private final GatewayReleaseAttemptPersistenceConverter attemptPersistenceConverter;

    @Qualifier("gatewayReleaseTargetPersistenceConverter")
    private final GatewayReleaseTargetPersistenceConverter targetPersistenceConverter;

    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：执行 insert 操作；等价旧实现的三条 INSERT（发布头、内容快照、首个 attempt）：先受守卫插入发布头并取回技术编号，
     * 再以该编号作为子表父键插入内容快照与 {@code PENDING} 尝试，创建/更新审计列由 {@code EgonColaMetaObjectHandler} 补齐。
     * English summary: Executes the insert operation; equivalent to the legacy three INSERT statements (release head, content
     * snapshot and the first attempt): the head is inserted through the guarded boundary first, and its technical identifier then
     * becomes the parent key of the content snapshot and the {@code PENDING} attempt row, while the audit columns are filled by
     * {@code EgonColaMetaObjectHandler}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayReleaseRepository.insert(release, compiled, attemptNo)}。调用方须在自身事务内调用。
     * / Call it inside the caller's transaction.
     * @param release 参数 发布载体；parameter release carrier。
     * @param compiled 参数 编译产物快照；parameter compiled release snapshot。
     * @param attemptNo 参数 首个尝试序号；parameter attempt no of the first attempt。
     */
    @Override
    public void insert(
            GatewayReleaseBO release,
            CompiledGatewayRelease compiled,
            int attemptNo) {
        GatewayReleaseRecordPO head = releasePersistenceConverter.newRow(release);
        requireWritten(
                releasePersistenceRepository.save(head),
                "YUHENG_ADMIN_RELEASE_INSERT_CONFLICT"
        );
        requireWritten(
                contentPersistenceRepository.save(
                        contentRow(head.getId(), compiled)
                ),
                "YUHENG_ADMIN_RELEASE_CONTENT_INSERT_CONFLICT"
        );
        insertAttempt(
                head.getId(),
                attemptNo,
                PENDING_ATTEMPT_STATUS,
                release.getCreatedAt()
        );
    }

    /**
     * 中文说明：执行 find 操作；按不透明编号做受守卫活跃读取（等价旧 {@code WHERE id = ?} 加隐式的未软删与租户过滤），
     * 编号无法命中技术列时如实返回空。
     * English summary: Executes the find operation; a guarded active read by opaque identifier (the legacy
     * {@code WHERE id = ?} plus the implicit not-soft-deleted and tenant filters) returning empty truthfully when the
     * identifier cannot match the technical column.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayReleaseRepository.find(releaseId)}。
     * @param releaseId 参数 发布Id；parameter release id。
     * @return 返回 find 的处理结果；returns the result of the operation.
     */
    @Override
    public Optional<GatewayReleaseBO> find(String releaseId) {
        Long key = technicalId(releaseId);
        if (key == null) {
            return Optional.empty();
        }
        return releasePersistenceRepository.getOptById(key)
                .map(releasePersistenceConverter::toBusiness);
    }

    /**
     * 中文说明：执行 history 操作；等价旧 {@code WHERE gateway_group_id = ? ORDER BY created_at DESC}，改用 MP 审计列
     * {@code create_time} 倒序并追加技术 id 倒序保证次序稳定。
     * English summary: Executes the history operation; equivalent to the legacy
     * {@code WHERE gateway_group_id = ? ORDER BY created_at DESC}, now ordered by the MP audit column
     * {@code create_time} descending with a stable technical-id tie-break.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayReleaseRepository.history(gatewayGroupId)}。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 history 的处理结果；returns the result of the operation.
     */
    @Override
    public List<GatewayReleaseBO> history(String gatewayGroupId) {
        Long group = technicalId(gatewayGroupId);
        if (group == null) {
            return List.of();
        }
        List<GatewayReleaseRecordPO> rows = releasePersistenceRepository.list(
                boundPredicate(Wrappers.<GatewayReleaseRecordPO>lambdaQuery()
                        .eq(GatewayReleaseRecordPO::getGatewayGroupId, group)
                        .orderByDesc(GatewayReleaseRecordPO::getCreateTime)
                        .orderByDesc(GatewayReleaseRecordPO::getId))
        );
        return releasePersistenceConverter.toBusinessList(rows);
    }

    /**
     * 中文说明：执行 recoverable 操作；等价旧 {@code status = 'PUBLISHING'} 与发布日志的联表加
     * {@code MAX(candidate.attempt_no)} 相关子查询：先受守卫列出在途发布头（按 {@code updated_at} 升序），
     * 再一次批量读取这些发布的日志行求每个发布的最大尝试序号，无日志行的发布按 INNER JOIN 语义如实排除。
     * English summary: Executes the recoverable operation; equivalent to the legacy join of {@code status = 'PUBLISHING'}
     * releases with the publication journal plus the correlated {@code MAX(candidate.attempt_no)} sub-select: the in-flight
     * release heads are listed through the guarded boundary ordered by {@code updated_at}, one batched journal read then yields
     * the greatest attempt number per release, and a release without journal rows is excluded with INNER JOIN semantics.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayReleaseRepository.recoverable()}。定时协调器须在可信租户上下文内调用。
     * / The scheduled reconciler must call it with a trusted tenant context bound.
     * @return 返回 recoverable 的处理结果；returns the result of the operation.
     */
    @Override
    public List<GatewayRecoverableReleaseAttemptBO> recoverable() {
        List<GatewayReleaseRecordPO> heads = releasePersistenceRepository.list(
                boundPredicate(Wrappers.<GatewayReleaseRecordPO>lambdaQuery()
                        .eq(
                                GatewayReleaseRecordPO::getStatus,
                                GatewayReleaseStatus.PUBLISHING.name()
                        )
                        .orderByAsc(GatewayReleaseRecordPO::getUpdateTime)
                        .orderByAsc(GatewayReleaseRecordPO::getId))
        );
        if (heads.isEmpty()) {
            return List.of();
        }
        Map<Long, Integer> greatestJournalAttempt = greatestJournalAttempt(
                heads.stream().map(GatewayReleaseRecordPO::getId).toList()
        );
        List<GatewayRecoverableReleaseAttemptBO> candidates =
                new ArrayList<>(heads.size());
        for (GatewayReleaseRecordPO head : heads) {
            Integer attemptNo = greatestJournalAttempt.get(head.getId());
            if (attemptNo == null) {
                continue;
            }
            GatewayReleaseBO carrier = releasePersistenceConverter.toBusiness(head);
            candidates.add(GatewayRecoverableReleaseAttemptBO.builder()
                    .releaseId(carrier.getId())
                    .gatewayGroupId(carrier.getGatewayGroupId())
                    .attemptNo(attemptNo)
                    .build());
        }
        return List.copyOf(candidates);
    }

    /**
     * 中文说明：执行 attempts 操作；等价旧实现的两次读取——先按 {@code attempt_no, instance_id, lease_id} 升序载入实例目标
     * 并按尝试分组，再按 {@code attempt_no DESC} 载入尝试日志，把各组目标以不可变列表聚合进载体。
     * English summary: Executes the attempts operation; equivalent to the legacy two reads: the instance targets are listed
     * ascending by {@code attempt_no, instance_id, lease_id} and grouped per attempt, then the attempt journal is listed
     * descending by {@code attempt_no} and every group is aggregated into its carrier as an immutable list.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayReleaseRepository.attempts(releaseId)}。
     * @param releaseId 参数 发布Id；parameter release id。
     * @return 返回 attempts 的处理结果；returns the result of the operation.
     */
    @Override
    public List<GatewayReleaseAttemptBO> attempts(String releaseId) {
        Long key = technicalId(releaseId);
        if (key == null) {
            return List.of();
        }
        Map<Integer, List<GatewayReleaseTargetBO>> targets =
                new LinkedHashMap<>();
        targetPersistenceRepository.list(
                boundPredicate(Wrappers.<GatewayReleaseTargetRecordPO>lambdaQuery()
                        .eq(GatewayReleaseTargetRecordPO::getReleaseId, key)
                        .orderByAsc(GatewayReleaseTargetRecordPO::getAttemptNo)
                        .orderByAsc(GatewayReleaseTargetRecordPO::getInstanceId)
                        .orderByAsc(GatewayReleaseTargetRecordPO::getLeaseId)
                        .orderByAsc(GatewayReleaseTargetRecordPO::getId))
        ).forEach(row -> targets.computeIfAbsent(
                row.getAttemptNo().intValue(),
                ignored -> new ArrayList<>()
        ).add(targetPersistenceConverter.toBusiness(row)));
        return attemptPersistenceRepository.list(
                        boundPredicate(Wrappers.<GatewayReleaseAttemptRecordPO>lambdaQuery()
                                .eq(GatewayReleaseAttemptRecordPO::getReleaseId, key)
                                .orderByDesc(GatewayReleaseAttemptRecordPO::getAttemptNo)
                                .orderByDesc(GatewayReleaseAttemptRecordPO::getId))
                ).stream()
                .map(row -> {
                    GatewayReleaseAttemptBO carrier =
                            attemptPersistenceConverter.toBusiness(row);
                    carrier.setTargets(List.copyOf(targets.getOrDefault(
                            row.getAttemptNo(),
                            List.of()
                    )));
                    return carrier;
                })
                .toList();
    }

    /**
     * 中文说明：执行 latestAttempt 操作；受守卫读取该发布的全部尝试取最大尝试序号（受守卫边界不做列投影），
     * 无任何尝试时沿用旧实现的「未找到」非法参数异常。
     * English summary: Executes the latest attempt operation; every attempt of the release is read through the guarded
     * boundary (which performs no column projection) and the greatest attempt number wins, while a release without attempts keeps
     * the legacy "not found" illegal-argument failure.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayReleaseRepository.latestAttempt(releaseId)}。
     * @param releaseId 参数 发布Id；parameter release id。
     * @return 返回 latestAttempt 的处理结果；returns the result of the operation.
     */
    @Override
    public int latestAttempt(String releaseId) {
        int greatest = greatestAttempt(technicalId(releaseId));
        if (greatest == 0) {
            throw new IllegalArgumentException(
                    "release attempt was not found"
            );
        }
        return greatest;
    }

    /**
     * 中文说明：执行 loadCompiled 操作；受守卫读取 {@code gateway_release_content} 单行，经 Jackson 还原规范快照、
     * 激活内容与分块清单，缺失内容行或列值不可解析时沿用旧实现异常类型与文案。
     * English summary: Executes the load compiled operation; the single {@code gateway_release_content} row is read through the
     * guarded boundary and Jackson restores the canonical snapshot, activation content and chunk manifest, keeping the legacy
     * exception types and messages for a missing row or an unparsable column value.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayReleaseRepository.loadCompiled(releaseId)}。
     * @param releaseId 参数 发布Id；parameter release id。
     * @return 返回 loadCompiled 的处理结果；returns the result of the operation.
     */
    @Override
    public CompiledGatewayRelease loadCompiled(String releaseId) {
        Long key = technicalId(releaseId);
        GatewayReleaseContentPO row = (key == null
                ? Optional.<GatewayReleaseContentPO>empty()
                : contentPersistenceRepository.getOneOpt(
                        boundPredicate(Wrappers.<GatewayReleaseContentPO>lambdaQuery()
                                .eq(GatewayReleaseContentPO::getReleaseId, key))
                )).orElseThrow(() -> new IllegalArgumentException(
                "release content was not found"
        ));
        String snapshotJson = jsonText(row.getCanonicalSnapshot());
        String activationJson = jsonText(row.getActivationContent());
        return new CompiledGatewayRelease(
                read(snapshotJson, GatewayRuleSnapshot.class),
                snapshotJson,
                read(activationJson, GatewayRuleActivation.class),
                activationJson,
                chunkManifest(row.getChunkManifest())
        );
    }

    /**
     * 中文说明：执行 findArtifactSha256 操作；等价旧 {@code SELECT artifact_sha256 FROM gateway_release_content WHERE
     * release_id = ?}，受守卫读取整行后只取制品摘要列，不解析快照。
     * English summary: Executes the find artifact sha256 operation; equivalent to the legacy
     * {@code SELECT artifact_sha256 FROM gateway_release_content WHERE release_id = ?}: the guarded row is read and only the
     * artifact digest column is taken, without parsing the snapshot.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayReleaseRepository.findArtifactSha256(releaseId)}。
     * @param releaseId 参数 发布Id；parameter release id。
     * @return 返回 findArtifactSha256 的处理结果；returns the result of the operation.
     */
    @Override
    public Optional<String> findArtifactSha256(String releaseId) {
        Long key = technicalId(releaseId);
        if (key == null) {
            return Optional.empty();
        }
        return contentPersistenceRepository.getOneOpt(
                        boundPredicate(Wrappers.<GatewayReleaseContentPO>lambdaQuery()
                                .eq(GatewayReleaseContentPO::getReleaseId, key))
                )
                .map(GatewayReleaseContentPO::getArtifactSha256);
    }

    /**
     * 中文说明：执行 nextAttempt 操作；先按受守卫读取全部尝试求最大序号再 +1（无尝试时为 1），随后登记
     * {@code PENDING} 尝试行，与旧实现的 {@code MAX(attempt_no)} 加 INSERT 顺序一致。
     * English summary: Executes the next attempt operation; the greatest attempt number is read through the guarded boundary and
     * incremented (one when no attempt exists), then the {@code PENDING} attempt row is registered, keeping the legacy
     * {@code MAX(attempt_no)} plus INSERT order.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayReleaseRepository.nextAttempt(releaseId, now)}。
     * @param releaseId 参数 发布Id；parameter release id。
     * @param now 参数 now；parameter now。
     * @return 返回 nextAttempt 的处理结果；returns the result of the operation.
     */
    @Override
    public int nextAttempt(String releaseId, Instant now) {
        Long key = technicalId(releaseId);
        int attempt = greatestAttempt(key) + 1;
        insertAttempt(key, attempt, PENDING_ATTEMPT_STATUS, now);
        return attempt;
    }

    /**
     * 中文说明：执行 beginAttempt 操作；等价旧的两条 UPDATE：尝试日志置 {@code PUBLISHING} 并清空
     * {@code completed_at/error_code/error_message}，发布头同步置 {@code PUBLISHING} 与 {@code updated_at}；
     * 与旧实现不同，行缺失或乐观锁 0 行不再被静默忽略，而是如实抛出冲突。
     * English summary: Executes the begin attempt operation; equivalent to the legacy two UPDATE statements: the journal row turns
     * {@code PUBLISHING} and clears {@code completed_at/error_code/error_message}, and the release head follows with
     * {@code PUBLISHING} plus {@code updated_at}. Unlike the legacy code a missing row or a zero-row optimistic lock is no longer
     * silently ignored but surfaces truthfully as a conflict.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayReleaseRepository.beginAttempt(releaseId, attemptNo, now)}。
     * @param releaseId 参数 发布Id；parameter release id。
     * @param attemptNo 参数 attemptNo；parameter attempt no。
     * @param now 参数 now；parameter now。
     */
    @Override
    public void beginAttempt(
            String releaseId,
            int attemptNo,
            Instant now) {
        GatewayReleaseAttemptRecordPO attempt = attemptRow(
                technicalId(releaseId),
                attemptNo
        );
        Long key = attempt.getReleaseId();
        requireWritten(
                attemptPersistenceRepository.update(
                        attemptToken(attempt.getId(), attempt.getVersion()),
                        boundPredicate(Wrappers.<GatewayReleaseAttemptRecordPO>lambdaUpdate()
                                .set(
                                        GatewayReleaseAttemptRecordPO::getStatus,
                                        GatewayReleaseStatus.PUBLISHING.name()
                                )
                                .set(GatewayReleaseAttemptRecordPO::getStartedAt, now)
                                .set(GatewayReleaseAttemptRecordPO::getCompletedAt, null)
                                .set(GatewayReleaseAttemptRecordPO::getErrorCode, null)
                                .set(GatewayReleaseAttemptRecordPO::getErrorMessage, null)
                                .eq(GatewayReleaseAttemptRecordPO::getId, attempt.getId())
                                .eq(GatewayReleaseAttemptRecordPO::getReleaseId, key)
                                .eq(GatewayReleaseAttemptRecordPO::getAttemptNo, attemptNo))
                ),
                "YUHENG_ADMIN_RELEASE_ATTEMPT_WRITE_CONFLICT"
        );
        updateReleaseHead(
                key,
                Wrappers.<GatewayReleaseRecordPO>lambdaUpdate()
                        .set(
                                GatewayReleaseRecordPO::getStatus,
                                GatewayReleaseStatus.PUBLISHING.name()
                        ),
                now
        );
    }

    /**
     * 中文说明：执行 completeAttempt 操作；沿用旧实现的三步顺序：尝试日志记终态（{@code change_id/error_code/error_message}
     * 允许为 NULL，经更新构造器显式下推）、发布头记状态与部分应用标记、实例目标按
     * {@code (release_id, attempt_no, instance_id, lease_id)} 业务键做「缺失插入、存在整列替换」的 CAS upsert。
     * English summary: Executes the complete attempt operation; the legacy three-step order is kept: the journal row records the
     * terminal state (with {@code change_id/error_code/error_message} allowed to be NULL and pushed explicitly by the update
     * wrapper), the release head records the status and partial-application flag, and every instance target is upserted under CAS
     * by the business key {@code (release_id, attempt_no, instance_id, lease_id)} — insert when absent, full column replacement
     * when present.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayReleaseRepository.completeAttempt(...)}。调用方须在自身事务内调用。
     * / Call it inside the caller's transaction.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param attemptNo 参数 attemptNo；parameter attempt no。
     * @param status 参数 终态状态；parameter terminal status。
     * @param partialApplied 参数 是否部分应用；parameter partial applied flag。
     * @param changeId 参数 外部变更Id；parameter external change id。
     * @param errorCode 参数 错误码；parameter error code。
     * @param errorMessage 参数 错误说明；parameter error message。
     * @param targets 参数 实例目标观测；parameter instance target observations。
     * @param now 参数 now；parameter now。
     */
    @Override
    public void completeAttempt(
            String releaseId,
            int attemptNo,
            GatewayReleaseStatus status,
            boolean partialApplied,
            String changeId,
            String errorCode,
            String errorMessage,
            List<GatewayReleaseTargetBO> targets,
            Instant now) {
        GatewayReleaseAttemptRecordPO attempt = attemptRow(
                technicalId(releaseId),
                attemptNo
        );
        Long key = attempt.getReleaseId();
        String statusName = status.name();
        requireWritten(
                attemptPersistenceRepository.update(
                        attemptToken(attempt.getId(), attempt.getVersion()),
                        boundPredicate(Wrappers.<GatewayReleaseAttemptRecordPO>lambdaUpdate()
                                .set(GatewayReleaseAttemptRecordPO::getStatus, statusName)
                                .set(GatewayReleaseAttemptRecordPO::getChangeId, changeId)
                                .set(GatewayReleaseAttemptRecordPO::getCompletedAt, now)
                                .set(GatewayReleaseAttemptRecordPO::getErrorCode, errorCode)
                                .set(GatewayReleaseAttemptRecordPO::getErrorMessage, errorMessage)
                                .eq(GatewayReleaseAttemptRecordPO::getId, attempt.getId())
                                .eq(GatewayReleaseAttemptRecordPO::getReleaseId, key)
                                .eq(GatewayReleaseAttemptRecordPO::getAttemptNo, attemptNo))
                ),
                "YUHENG_ADMIN_RELEASE_ATTEMPT_WRITE_CONFLICT"
        );
        updateReleaseHead(
                key,
                Wrappers.<GatewayReleaseRecordPO>lambdaUpdate()
                        .set(GatewayReleaseRecordPO::getStatus, statusName)
                        .set(
                                GatewayReleaseRecordPO::getPartialApplied,
                                partialApplied
                        )
                        .set(GatewayReleaseRecordPO::getChangeId, changeId),
                now
        );
        if (targets != null) {
            targets.forEach(target -> upsertTarget(key, attemptNo, target));
        }
    }

    /**
     * 中文说明：执行 hasReleaseInProgress 操作；等价旧 {@code count(*)} 组内在途状态守护，状态集合取
     * {@link #IN_PROGRESS_STATUSES}，非十进制组键如实视为无在途发布。
     * English summary: Executes the has release in progress operation; equivalent to the legacy {@code count(*)} per-group
     * concurrency guard over {@link #IN_PROGRESS_STATUSES}, and a non-decimal group key truthfully means no release is in flight.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayReleaseRepository.hasReleaseInProgress(gatewayGroupId)}。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 hasReleaseInProgress 的处理结果；returns the result of the operation.
     */
    @Override
    public boolean hasReleaseInProgress(String gatewayGroupId) {
        Long group = technicalId(gatewayGroupId);
        if (group == null) {
            return false;
        }
        return releasePersistenceRepository.count(
                boundPredicate(Wrappers.<GatewayReleaseRecordPO>lambdaQuery()
                        .eq(GatewayReleaseRecordPO::getGatewayGroupId, group)
                        .in(GatewayReleaseRecordPO::getStatus, IN_PROGRESS_STATUSES))
        ) > 0;
    }

    /**
     * 中文说明：执行 内容快照行 操作；把编译产物渲染为 {@code gateway_release_content} 行：两段 JSON 由 Jackson 解析为节点
     * 交给 jsonb 类型处理器，快照体积沿用 UTF-8 字节长度，父键取发布头刚生成的技术编号；租户、审计、软删与版本列留给受守卫边界补齐。
     * English summary: Executes the content snapshot row operation; the compiled artifact is rendered into a
     * {@code gateway_release_content} row: both JSON documents are parsed by Jackson for the jsonb type handler, the snapshot size
     * keeps the UTF-8 byte length, the parent key is the technical identifier the release head has just been inserted with, and the
     * tenant, audit, soft-delete and version columns are left to the guarded boundary.
     *
     * 用法 / Usage: 仅由 {@link #insert(GatewayReleaseBO, CompiledGatewayRelease, int)} 调用。
     * @param releaseId 参数 发布头技术编号；parameter technical release head id。
     * @param compiled 参数 编译产物；parameter compiled artifact。
     * @return 返回 内容快照行；returns the content snapshot row.
     */
    private GatewayReleaseContentPO contentRow(
            Long releaseId,
            CompiledGatewayRelease compiled) {
        String snapshotJson = compiled.snapshotJson();
        return GatewayReleaseContentPO.builder()
                .releaseId(releaseId)
                .ruleContentSha256(compiled.snapshot().ruleContentSha256())
                .artifactSha256(compiled.snapshot().artifactSha256())
                .canonicalSnapshot(parse(snapshotJson))
                .activationContent(parse(compiled.activationJson()))
                .chunkManifest(value(compiled.chunkValues()))
                .snapshotSize((long) snapshotJson.getBytes(
                        StandardCharsets.UTF_8
                ).length)
                .build();
    }

    /**
     * 中文说明：执行 insertAttempt 操作；把尝试载体经转换器渲染为行后补上父键，再受守卫插入，等价旧
     * {@code INSERT INTO gateway_release_attempt(release_id, attempt_no, status, started_at)}。
     * English summary: Executes the insert attempt operation; the attempt carrier is rendered by the converter, gets its parent key
     * and is inserted through the guarded boundary, equivalent to the legacy
     * {@code INSERT INTO gateway_release_attempt(release_id, attempt_no, status, started_at)}.
     *
     * 用法 / Usage: 由 {@link #insert(GatewayReleaseBO, CompiledGatewayRelease, int)} 与
     * {@link #nextAttempt(String, Instant)} 调用。
     * @param releaseId 参数 发布技术编号；parameter technical release id。
     * @param attemptNo 参数 尝试序号；parameter attempt no。
     * @param status 参数 尝试状态字面量；parameter attempt status literal。
     * @param startedAt 参数 开始时间；parameter started instant。
     */
    private void insertAttempt(
            Long releaseId,
            int attemptNo,
            String status,
            Instant startedAt) {
        GatewayReleaseAttemptRecordPO row = attemptPersistenceConverter.newRow(
                GatewayReleaseAttemptBO.builder()
                        .attemptNo(attemptNo)
                        .status(status)
                        .startedAt(startedAt)
                        .targets(List.of())
                        .build()
        );
        row.setReleaseId(releaseId);
        requireWritten(
                attemptPersistenceRepository.save(row),
                "YUHENG_ADMIN_RELEASE_ATTEMPT_INSERT_CONFLICT"
        );
    }

    /**
     * 中文说明：执行 尝试日志定位 操作；受守卫读取旧实现 {@code WHERE release_id = ? AND attempt_no = ?} 命中的那一行，
     * 编号不可命中或行不存在时按 0 行如实抛出冲突（旧实现忽略 0 行，这是本端口唯一新增的失败路径）。
     * English summary: Executes the attempt journal locating operation; the row the legacy
     * {@code WHERE release_id = ? AND attempt_no = ?} matched is read through the guarded boundary, and an unmatchable identifier
     * or an absent row surfaces truthfully as a zero-row conflict (the legacy code ignored it, which is the only new failure path
     * added by this port).
     *
     * 用法 / Usage: 由 {@link #beginAttempt} 与 {@link #completeAttempt} 调用。
     * @param releaseId 参数 发布技术编号，可为空；parameter technical release id, nullable。
     * @param attemptNo 参数 尝试序号；parameter attempt no。
     * @return 返回 尝试日志行；returns the journal row.
     */
    private GatewayReleaseAttemptRecordPO attemptRow(Long releaseId, int attemptNo) {
        Optional<GatewayReleaseAttemptRecordPO> current = releaseId == null
                ? Optional.empty()
                : attemptPersistenceRepository.getOneOpt(
                        boundPredicate(Wrappers.<GatewayReleaseAttemptRecordPO>lambdaQuery()
                                .eq(GatewayReleaseAttemptRecordPO::getReleaseId, releaseId)
                                .eq(GatewayReleaseAttemptRecordPO::getAttemptNo, attemptNo))
                );
        return current.orElseThrow(() -> new IllegalStateException(
                "YUHENG_ADMIN_RELEASE_ATTEMPT_WRITE_CONFLICT"
        ));
    }

    /**
     * 中文说明：执行 更新乐观锁令牌 操作；受守卫边界要求写实体携带技术主键与当前版本（{@code @NotNull(Update)} 且
     * 乐观锁拦截器据此下推 {@code version} 条件），业务列一律经更新构造器显式下推，租户与审计列由元数据填充器负责。
     * English summary: Executes the update optimistic-lock token operation; the guarded boundary requires the written entity to carry
     * its technical key and current version ({@code @NotNull(Update)}, which the optimistic-lock interceptor pushes down as the
     * {@code version} condition), business columns are always pushed explicitly by the update wrapper, and the tenant and audit
     * columns belong to the meta-object handler.
     *
     * 用法 / Usage: 每次 CAS 更新前调用。/ Called before every CAS update.
     * @param id 参数 技术主键；parameter technical identifier。
     * @param version 参数 当前版本；parameter current version。
     * @return 返回 令牌实体；returns the token entity.
     */
    private static GatewayReleaseAttemptRecordPO attemptToken(Long id, Long version) {
        GatewayReleaseAttemptRecordPO token = new GatewayReleaseAttemptRecordPO();
        token.setId(id);
        token.setVersion(version);
        return token;
    }

    /**
     * 中文说明：执行 发布头更新令牌 操作；语义同 {@link #attemptToken}，并额外携带旧 SQL 显式写入的 {@code updated_at}。
     * English summary: Executes the release head update token operation; same semantics as {@link #attemptToken} plus the
     * {@code updated_at} the legacy SQL wrote explicitly.
     *
     * 用法 / Usage: 每次发布头 CAS 前调用。
     * @param id 参数 技术主键；parameter technical identifier。
     * @param version 参数 当前版本；parameter current version。
     * @param updateTime 参数 更新时间；parameter update instant。
     * @return 返回 令牌实体；returns the token entity.
     */
    private static GatewayReleaseRecordPO releaseToken(
            Long id,
            Long version,
            Instant updateTime) {
        GatewayReleaseRecordPO token = new GatewayReleaseRecordPO();
        token.setId(id);
        token.setVersion(version);
        token.setUpdateTime(updateTime);
        return token;
    }

    /**
     * 中文说明：执行 发布头状态更新 操作；受守卫读取发布头取得 CAS 令牌，再把调用方渲染好的业务列与旧 SQL 的
     * {@code WHERE id = ?} 一并下推，并写入 {@code updated_at}；0 行按冲突如实抛出。
     * English summary: Executes the release head status update operation; the head is read through the guarded boundary to obtain the
     * CAS token, then the business columns rendered by the caller are pushed down together with the legacy {@code WHERE id = ?}
     * plus {@code updated_at}, and a zero-row write surfaces as a conflict.
     *
     * 用法 / Usage: 由 {@link #beginAttempt} 与 {@link #completeAttempt} 调用。
     * @param releaseId 参数 发布技术编号；parameter technical release id。
     * @param changes 参数 已渲染业务列的更新构造器；parameter update wrapper carrying the business columns。
     * @param now 参数 更新时间；parameter update instant。
     */
    private void updateReleaseHead(
            Long releaseId,
            LambdaUpdateWrapper<GatewayReleaseRecordPO> changes,
            Instant now) {
        GatewayReleaseRecordPO head = releasePersistenceRepository.getOptById(releaseId)
                .orElseThrow(() -> new IllegalStateException(
                        "YUHENG_ADMIN_RELEASE_WRITE_CONFLICT"
                ));
        requireWritten(
                releasePersistenceRepository.update(
                        releaseToken(head.getId(), head.getVersion(), now),
                        boundPredicate(
                                changes.eq(GatewayReleaseRecordPO::getId, head.getId()))
                ),
                "YUHENG_ADMIN_RELEASE_WRITE_CONFLICT"
        );
    }

    /**
     * 中文说明：执行 upsert目标 操作；等价旧 {@code INSERT ... ON CONFLICT (release_id, attempt_no, instance_id,
     * lease_id) DO UPDATE SET status, applied_version, applied_artifact_sha256, error_code, observed_at, engine_role}：
     * 业务键缺失时受守卫插入（父键与尝试序号由本仓储补齐，转换器不映射它们），存在时按乐观锁整列替换，
     * NULL 值经更新构造器显式下推，0 行按冲突如实抛出。
     * English summary: Executes the upsert target operation; equivalent to the legacy
     * {@code INSERT ... ON CONFLICT (release_id, attempt_no, instance_id, lease_id) DO UPDATE SET status, applied_version,
     * applied_artifact_sha256, error_code, observed_at, engine_role}: a missing business key is inserted through the guarded
     * boundary (the parent key and attempt number are supplied here because the converter does not map them), an existing row is
     * replaced column by column under the optimistic lock with NULLs pushed explicitly by the update wrapper, and a zero-row write
     * surfaces as a conflict.
     *
     * 用法 / Usage: 由 {@link #completeAttempt} 逐个目标调用。
     * @param releaseId 参数 发布技术编号；parameter technical release id。
     * @param attemptNo 参数 尝试序号；parameter attempt no。
     * @param target 参数 目标观测载体；parameter target observation carrier。
     */
    private void upsertTarget(
            Long releaseId,
            int attemptNo,
            GatewayReleaseTargetBO target) {
        GatewayReleaseTargetRecordPO candidate = targetPersistenceConverter.newRow(target);
        candidate.setReleaseId(releaseId);
        candidate.setAttemptNo((long) attemptNo);
        Optional<GatewayReleaseTargetRecordPO> current = targetPersistenceRepository.getOneOpt(
                boundPredicate(Wrappers.<GatewayReleaseTargetRecordPO>lambdaQuery()
                        .eq(GatewayReleaseTargetRecordPO::getReleaseId, releaseId)
                        .eq(GatewayReleaseTargetRecordPO::getAttemptNo, (long) attemptNo)
                        .eq(GatewayReleaseTargetRecordPO::getInstanceId, candidate.getInstanceId())
                        .eq(GatewayReleaseTargetRecordPO::getLeaseId, candidate.getLeaseId()))
        );
        if (current.isEmpty()) {
            requireWritten(
                    targetPersistenceRepository.save(candidate),
                    "YUHENG_ADMIN_RELEASE_TARGET_WRITE_CONFLICT"
            );
            return;
        }
        GatewayReleaseTargetRecordPO token = new GatewayReleaseTargetRecordPO();
        token.setId(current.get().getId());
        token.setVersion(current.get().getVersion());
        requireWritten(
                targetPersistenceRepository.update(
                        token,
                        boundPredicate(Wrappers.<GatewayReleaseTargetRecordPO>lambdaUpdate()
                                .set(GatewayReleaseTargetRecordPO::getStatus, candidate.getStatus())
                                .set(GatewayReleaseTargetRecordPO::getAppliedVersion, candidate.getAppliedVersion())
                                .set(GatewayReleaseTargetRecordPO::getAppliedArtifactSha256, candidate.getAppliedArtifactSha256())
                                .set(GatewayReleaseTargetRecordPO::getErrorCode, candidate.getErrorCode())
                                .set(GatewayReleaseTargetRecordPO::getObservedAt, candidate.getObservedAt())
                                .set(GatewayReleaseTargetRecordPO::getEngineRole, candidate.getEngineRole())
                                .eq(GatewayReleaseTargetRecordPO::getId, current.get().getId())
                                .eq(GatewayReleaseTargetRecordPO::getReleaseId, releaseId)
                                .eq(GatewayReleaseTargetRecordPO::getAttemptNo, (long) attemptNo)
                                .eq(GatewayReleaseTargetRecordPO::getInstanceId, candidate.getInstanceId())
                                .eq(GatewayReleaseTargetRecordPO::getLeaseId, candidate.getLeaseId()))
                ),
                "YUHENG_ADMIN_RELEASE_TARGET_WRITE_CONFLICT"
        );
    }

    /**
     * 中文说明：执行 最大尝试序号 操作；受守卫读取该发布的全部尝试求最大值，无尝试或编号不可命中时返回 0，
     * 与旧 SQL 的 {@code COALESCE(MAX(attempt_no), 0)} 一致。
     * English summary: Executes the greatest attempt number operation; every attempt of the release is read through the guarded
     * boundary and the maximum wins, yielding 0 for an unmatchable identifier or an empty journal exactly like the legacy
     * {@code COALESCE(MAX(attempt_no), 0)}.
     *
     * 用法 / Usage: 由 {@link #latestAttempt(String)} 与 {@link #nextAttempt(String, Instant)} 调用。
     * @param releaseId 参数 发布技术编号，可为空；parameter technical release id, nullable。
     * @return 返回 最大尝试序号；returns the greatest attempt number.
     */
    private int greatestAttempt(Long releaseId) {
        if (releaseId == null) {
            return 0;
        }
        return attemptPersistenceRepository.list(
                        boundPredicate(Wrappers.<GatewayReleaseAttemptRecordPO>lambdaQuery()
                                .eq(GatewayReleaseAttemptRecordPO::getReleaseId, releaseId))
                ).stream()
                .map(GatewayReleaseAttemptRecordPO::getAttemptNo)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
    }

    /**
     * 中文说明：执行 发布日志最大尝试 操作；一次批量读取给定发布的全部日志行，按发布归并出最大尝试序号，
     * 取代旧 SQL 的相关子查询。
     * English summary: Executes the greatest journal attempt operation; the journal rows of the given releases are read in one batch
     * and merged per release into the greatest attempt number, replacing the correlated sub-select of the legacy SQL.
     *
     * 用法 / Usage: 仅由 {@link #recoverable()} 调用。
     * @param releaseIds 参数 发布技术编号集合；parameter technical release identifiers。
     * @return 返回 发布到最大尝试序号的映射；returns the release to greatest attempt map.
     */
    private Map<Long, Integer> greatestJournalAttempt(List<Long> releaseIds) {
        Map<Long, Integer> greatest = new LinkedHashMap<>();
        publicationPersistenceRepository.list(
                boundPredicate(Wrappers.<GatewayReleasePublicationRecordPO>lambdaQuery()
                        .in(GatewayReleasePublicationRecordPO::getReleaseId, releaseIds))
        ).forEach(row -> {
            Long attemptNo = row.getAttemptNo();
            if (attemptNo != null) {
                greatest.merge(
                        row.getReleaseId(),
                        attemptNo.intValue(),
                        Integer::max
                );
            }
        });
        return greatest;
    }

    /**
     * 中文说明：把条件交付受守卫边界之前先成形一次：MyBatis-Plus 的 {@code eq/in} 只在 SQL 真正成形时
     * 才把取值写进 {@code paramNameValuePairs}，故此处先成形一次，让谓词在离开门面时参数已完全绑定；
     * 之后（包括 MyBatis 自己下发时）命中同一份片段缓存，参数与 SQL 都不再改变，列解析失败
     * （lambda 缓存缺失）也如实在门面这一层暴露，而不是留到语句下发时。
     * English summary: Forms a condition once before it is handed to the guarded boundary: MyBatis-Plus only moves the
     * values of {@code eq/in} into {@code paramNameValuePairs} while the SQL is being formed, so forming it here first
     * means the parameters are fully bound when the predicate leaves the facade; later renders (including the one MyBatis
     * performs) hit the same segment cache and change neither the parameters nor the SQL, while a column-resolution
     * failure (a missing lambda cache) surfaces truthfully at the facade instead of at statement time.
     * @param predicate 参数 已构造完成的业务条件；parameter the completed business condition.
     * @return 返回 同一份参数已绑定的条件；returns the very same condition with its parameters bound.
     */
    private static <C extends Wrapper<?>> C boundPredicate(C predicate) {
        predicate.getSqlSegment();
        return predicate;
    }

    /**
     * 中文说明：执行 要求写入生效 操作；受守卫边界以布尔值回报影响行数，为假时按冲突如实抛出，绝不把 0 行报成成功。
     * English summary: Executes the require written operation; the guarded boundary reports the affected rows as a boolean and a
     * false result surfaces truthfully as a conflict instead of reporting zero rows as success.
     *
     * 用法 / Usage: 每次受守卫写入后调用。
     * @param written 参数 写入是否生效；parameter whether the write took effect。
     * @param message 参数 冲突文案；parameter conflict message。
     */
    private static void requireWritten(boolean written, String message) {
        if (!written) {
            throw new IllegalStateException(message);
        }
    }

    /**
     * 中文说明：执行 不透明编号投影 操作；端口携带的十进制不透明编号映射为新 {@code bigint} 技术列的谓词值，
     * null 与空白在旧 VARCHAR 主键模型下永远不可能命中，故如实返回 null 让调用方映射为空读或 0 行。
     * English summary: Executes the opaque identifier projection; the decimal opaque identifier carried by the port becomes the
     * predicate value of the new {@code bigint} technical column, and null or blank input could never match under the legacy
     * VARCHAR primary-key model, so it truthfully yields null for the caller to map into an empty read or a zero-row write.
     *
     * 用法 / Usage: 仅用于为本类查询与删除准备技术键。
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
     * 中文说明：执行 json 解析 操作；把旧实现以 {@code ?::jsonb} 绑定的 JSON 文本解析成节点交给 jsonb 类型处理器，
     * 失败沿用旧 {@code release value cannot be serialized} 非法参数异常。
     * English summary: Executes the json parsing operation; the JSON text the legacy code bound as {@code ?::jsonb} is parsed into a
     * node for the jsonb type handler, keeping the legacy {@code release value cannot be serialized} illegal-argument failure.
     *
     * 用法 / Usage: 仅由 {@link #contentRow} 调用。
     * @param json 参数 JSON 文本；parameter json text。
     * @return 返回 解析后的节点；returns the parsed node.
     */
    private JsonNode parse(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException | IllegalArgumentException failure) {
            throw new IllegalArgumentException(
                    "release value cannot be serialized",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 json 渲染 操作；把结构化值序列化为节点，失败沿用旧 {@code release value cannot be serialized}。
     * English summary: Executes the json rendering operation; a structured value becomes a node, and a failure keeps the legacy
     * {@code release value cannot be serialized}.
     *
     * 用法 / Usage: 仅由 {@link #contentRow} 渲染分块清单时调用。
     * @param value 参数 结构化值；parameter structured value。
     * @return 返回 渲染后的节点；returns the rendered node.
     */
    private JsonNode value(Object value) {
        try {
            return objectMapper.readTree(objectMapper.writeValueAsString(value));
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException(
                    "release value cannot be serialized",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 列文本投影 操作；受守卫边界把 jsonb 列还原为节点，旧实现拿到的是数据库规范化后的文本，
     * 此处以紧凑序列化保持同一语义，节点缺失按旧「内容无效」异常抛出。
     * English summary: Executes the column text projection; the guarded boundary restores the jsonb column as a node while the legacy
     * code read the database-normalized text, so the compact serialization keeps the same semantics, and an absent node raises the
     * legacy "stored release content is invalid" failure.
     *
     * 用法 / Usage: 仅由 {@link #loadCompiled(String)} 调用。
     * @param node 参数 jsonb 列节点；parameter jsonb column node。
     * @return 返回 列文本；returns the column text.
     */
    private static String jsonText(JsonNode node) {
        if (node == null) {
            throw new IllegalStateException(
                    "stored release content is invalid"
            );
        }
        return node.toString();
    }

    /**
     * 中文说明：执行 read 操作；把列文本反序列化为规则契约对象，失败沿用旧
     * {@code IllegalStateException("stored release content is invalid")}。
     * English summary: Executes the read operation; the column text is deserialized into the rule contract object, keeping the legacy
     * {@code IllegalStateException("stored release content is invalid")}.
     *
     * 用法 / Usage: 仅由 {@link #loadCompiled(String)} 调用。
     * @param json 参数 列文本；parameter column text。
     * @param type 参数 目标契约类型；parameter target contract type。
     * @param <T> 参数 目标契约类型；parameter target contract type。
     * @return 返回 反序列化结果；returns the deserialized value.
     */
    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException(
                    "stored release content is invalid",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 chunk清单 操作；把分块清单节点还原为字符串映射，失败沿用旧
     * {@code IllegalStateException("stored chunk manifest is invalid")}。
     * English summary: Executes the chunk manifest operation; the manifest node becomes a string map again, keeping the legacy
     * {@code IllegalStateException("stored chunk manifest is invalid")}.
     *
     * 用法 / Usage: 仅由 {@link #loadCompiled(String)} 调用。
     * @param node 参数 分块清单节点；parameter chunk manifest node。
     * @return 返回 分块清单；returns the chunk manifest.
     */
    private Map<String, String> chunkManifest(JsonNode node) {
        try {
            return objectMapper.readValue(
                    jsonText(node),
                    new TypeReference<Map<String, String>>() {
                    }
            );
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException(
                    "stored chunk manifest is invalid",
                    failure
            );
        }
    }
}
