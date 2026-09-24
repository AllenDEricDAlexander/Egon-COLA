package top.egon.cola.component.yuheng.admin.openapi.repository.impl;


import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.openapi.converter.GatewayOpenApiSyncPersistenceConverter;
import top.egon.cola.component.yuheng.admin.openapi.domain.bo.GatewayOpenApiSyncBO;
import top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiSyncKeyDTO;
import top.egon.cola.component.yuheng.admin.openapi.domain.enums.GatewayOpenApiSyncStateEnum;
import top.egon.cola.component.yuheng.admin.openapi.domain.po.GatewayOpenApiSyncRecordPO;
import top.egon.cola.component.yuheng.admin.openapi.repository.GatewayOpenApiSyncRepository;
import top.egon.cola.component.yuheng.admin.openapi.repository.mp.GatewayOpenApiSyncPersistenceRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 中文说明：{@code MpGatewayOpenApiSyncRepository} 是 {@code gateway_openapi_sync_state} 恢复 journal 的 MyBatis-Plus
 * 门面存储，逐方法取代旧手写的 JDBC 适配器：读取保留原 SQL 的业务键三元组、状态与到期
 * 谓词以及 {@code build_id, openapi_group, id}、{@code updated_at, id}、{@code next_retry_at NULLS FIRST, id} 次序；
 * {@code upsertDiscovered} 保留 {@code ON CONFLICT ... DO UPDATE} 只刷新提供方观察列、原样保留生命周期状态、
 * 尝试计数、不可变链接与业务 {@code revision} 的语义；{@code claim}/{@code transition}/{@code setValid}/
 * {@code markFailure} 保留原语句的 {@code WHERE id = ? AND revision = ? AND status IN (...)} 业务修订 CAS 与把
 * {@code next_retry_at}、{@code last_error_*} 置空的可空列写入；列与类型映射只经 MapStruct 转换器完成，
 * 公开端口不泄漏 {@code GatewayOpenApiSyncRecordPO}。
 * English summary: {@code MpGatewayOpenApiSyncRepository} is the MyBatis-Plus facade store for the
 * {@code gateway_openapi_sync_state} recovery journal, replacing the hand-written JDBC
 * adapter method by method: the reads keep the legacy business-key triple, state and
 * due-window predicates together with the {@code build_id, openapi_group, id}, {@code updated_at, id} and
 * {@code next_retry_at NULLS FIRST, id} orderings; {@code upsertDiscovered} keeps the {@code ON CONFLICT ... DO UPDATE}
 * behaviour of refreshing only the provider observation columns while the lifecycle state, attempt counter, immutable
 * links and business {@code revision} stay untouched; {@code claim}, {@code transition}, {@code setValid} and
 * {@code markFailure} keep the legacy {@code WHERE id = ? AND revision = ? AND status IN (...)} business-revision
 * compare-and-set together with the nullable writes that clear {@code next_retry_at} and {@code last_error_*}; column and
 * type mapping happens only in the MapStruct converter and the public port never leaks a
 * {@code GatewayOpenApiSyncRecordPO}.
 *
 * 用法 / Usage: 通过业务端口 {@code GatewayOpenApiSyncRepository} 由 Spring 容器注入，工作者更新组合在调用方
 * {@code gatewayTransactionManager} 的同一事务内；每次读写都走该表的受守卫 {@code EgonColaRepository}，因此同租户过滤、
 * 仅活跃行（{@code deleted_at IS NULL}）与技术 {@code version} 乐观锁是结构性的，业务 {@code revision} 不匹配或影响
 * 0 行一律返回 {@code false}（journal 的 {@code revision} 只在 CAS 命中时自增），绝不伪造成功。装载出的每个载体仍经
 * {@code GatewayOpenApiSyncBO.validated(...)} 复核旧构造器不变量。/ Use it through the
 * {@code GatewayOpenApiSyncRepository} port with worker updates composed inside the caller's
 * {@code gatewayTransactionManager} transaction; every read and write goes through this table's guarded
 * {@code EgonColaRepository}, so same-tenant filtering, active rows only ({@code deleted_at IS NULL}) and the technical
 * {@code version} optimistic lock are structural, and a business-revision mismatch or a zero-row effect returns
 * {@code false} (the journal revision increments only on a CAS hit) instead of a fake success. Every loaded carrier is
 * still re-checked by {@code GatewayOpenApiSyncBO.validated(...)}.
 */
@Slf4j
@Repository("mpGatewayOpenApiSyncRepository")
@RequiredArgsConstructor
@Validated
public class MpGatewayOpenApiSyncRepository implements GatewayOpenApiSyncRepository {

    /** 中文说明：可认领状态集合，逐字对应旧 SQL 的 {@code ('DISCOVERED', 'FETCH_FAILED', 'INGEST_FAILED', 'STALE')}。 English summary: the claimable state set, matching the legacy {@code ('DISCOVERED', 'FETCH_FAILED', 'INGEST_FAILED', 'STALE')} verbatim. */
    private static final List<String> CLAIMABLE_STATES = List.of(
            GatewayOpenApiSyncStateEnum.DISCOVERED.wireValue(),
            GatewayOpenApiSyncStateEnum.FETCH_FAILED.wireValue(),
            GatewayOpenApiSyncStateEnum.INGEST_FAILED.wireValue(),
            GatewayOpenApiSyncStateEnum.STALE.wireValue());

    /** 中文说明：{@code markFailure} 允许的失败/终态集合，与原实现的 {@code Set.of(...)} 守卫一致。 English summary: the failure or terminal states {@code markFailure} accepts, identical to the original {@code Set.of(...)} guard. */
    private static final Set<GatewayOpenApiSyncStateEnum> SUPPORTED_FAILURE_STATES = Set.of(
            GatewayOpenApiSyncStateEnum.INVALID,
            GatewayOpenApiSyncStateEnum.INCONSISTENT_BUILD,
            GatewayOpenApiSyncStateEnum.FETCH_FAILED,
            GatewayOpenApiSyncStateEnum.INGEST_FAILED,
            GatewayOpenApiSyncStateEnum.STALE);

    /** 中文说明：错误码与错误消息的数据库列上限，沿用原实现的入参守卫长度。 English summary: the database column limits for the error code and message, kept from the original argument guard. */
    private static final int ERROR_CODE_LIMIT = 128;

    /** 中文说明：错误消息列上限。 English summary: the error message column limit. */
    private static final int ERROR_MESSAGE_LIMIT = 1024;

    /** 中文说明：同步行表的受守卫持久化仓储（租户过滤、活跃读取、乐观锁 CAS 的唯一入口）。 English summary: the guarded persistence store for the synchronization rows, the only entry point for tenant filtering, active reads and optimistic-lock CAS. */
    @Qualifier("gatewayOpenApiSyncPersistenceRepository")
    private final GatewayOpenApiSyncPersistenceRepository syncPersistenceRepository;

    /** 中文说明：{@code GatewayOpenApiSyncBO} 与 {@code GatewayOpenApiSyncRecordPO} 的双向转换器。 English summary: the bidirectional converter between GatewayOpenApiSyncBO and GatewayOpenApiSyncRecordPO. */
    @Qualifier("gatewayOpenApiSyncPersistenceConverter")
    private final GatewayOpenApiSyncPersistenceConverter syncPersistenceConverter;

    /**
     * 中文说明：执行 find 操作；保留旧 SQL 的 {@code application_id/build_id/openapi_group} 业务键谓词与
     * {@code stream().findFirst()} 取首行语义。
     * English summary: Executes the find operation; keeps the legacy {@code application_id/build_id/openapi_group}
     * business-key predicate and the {@code stream().findFirst()} first-row semantics.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayOpenApiSyncRepository.findByKey(key)}。
     * @param key 参数 应用/构建/Group键；parameter application/build/Group key。
     * @return 返回 find 的处理结果；returns an existing row carrier when present.
     */
    @Override
    public Optional<GatewayOpenApiSyncBO> findByKey(
            GatewayOpenApiSyncKeyDTO key) {
        Objects.requireNonNull(key, "key");
        return firstRow(
                key.applicationId(),
                key.buildId(),
                key.openapiGroup()
        ).map(this::toCarrier);
    }

    /**
     * 中文说明：执行 find 操作；走受守卫的按主键活跃读取，非十进制或不存在的标识按无行处理。
     * English summary: Executes the find operation; the guarded active read by primary key is used, so a non-decimal or
     * absent identifier counts as no row.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayOpenApiSyncRepository.findById(syncId)}。
     * @param syncId 参数 状态行标识；parameter state row identifier。
     * @return 返回 find 的处理结果；returns an existing row carrier when present.
     */
    @Override
    public Optional<GatewayOpenApiSyncBO> findById(String syncId) {
        Long idColumn = columnValue(syncId);
        if (idColumn == null) {
            return Optional.empty();
        }
        return syncPersistenceRepository.getOptById(idColumn).map(this::toCarrier);
    }

    /**
     * 中文说明：执行 find 操作；保留旧 SQL 的必填 {@code application_id} 校验与
     * {@code ORDER BY build_id, openapi_group, id} 次序。
     * English summary: Executes the find operation; keeps the legacy required {@code application_id} guard and the
     * {@code ORDER BY build_id, openapi_group, id} ordering.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayOpenApiSyncRepository.findByApplicationId(applicationId)}。
     * @param applicationId 参数 物理网关应用标识；parameter physical Gateway application identifier。
     * @return 返回 find 的处理结果；returns the carriers in stable build/Group order.
     */
    @Override
    public List<GatewayOpenApiSyncBO> findByApplicationId(
            String applicationId) {
        Long applicationColumn = columnValue(required(applicationId, "applicationId"));
        if (applicationColumn == null) {
            return List.of();
        }
        return toCarriers(syncPersistenceRepository.list(
                boundPredicate(Wrappers.<GatewayOpenApiSyncRecordPO>lambdaQuery()
                        .eq(GatewayOpenApiSyncRecordPO::getApplicationId, applicationColumn)
                        .orderByAsc(GatewayOpenApiSyncRecordPO::getBuildId)
                        .orderByAsc(GatewayOpenApiSyncRecordPO::getOpenapiGroup)
                        .orderByAsc(GatewayOpenApiSyncRecordPO::getId))
        ));
    }

    /**
     * 中文说明：执行 find 操作；保留旧 SQL 的 {@code WHERE status = ? ORDER BY updated_at, id}，迁移后的
     * {@code updated_at} 即持久边界的 {@code update_time} 列。
     * English summary: Executes the find operation; keeps the legacy {@code WHERE status = ? ORDER BY updated_at, id},
     * where the migrated {@code updated_at} is the boundary's {@code update_time} column.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayOpenApiSyncRepository.findByStatus(state)}。
     * @param state 参数 待查询状态；parameter state to query。
     * @return 返回 find 的处理结果；returns the carriers in stable update order.
     */
    @Override
    public List<GatewayOpenApiSyncBO> findByStatus(
            GatewayOpenApiSyncStateEnum state) {
        Objects.requireNonNull(state, "state");
        return toCarriers(syncPersistenceRepository.list(
                boundPredicate(Wrappers.<GatewayOpenApiSyncRecordPO>lambdaQuery()
                        .eq(GatewayOpenApiSyncRecordPO::getStatus, state.wireValue())
                        .orderByAsc(GatewayOpenApiSyncRecordPO::getUpdateTime)
                        .orderByAsc(GatewayOpenApiSyncRecordPO::getId))
        ));
    }

    /**
     * 中文说明：执行 upsertDiscovered 操作；等价于旧 {@code INSERT ... ON CONFLICT (application_id, build_id,
     * openapi_group) DO UPDATE SET artifact_version, provider_service_name, provider_group, provider_version,
     * updated_at}：状态非 {@code DISCOVERED} 时先按旧实现抛出，随后行不存在走受守卫插入（唯一键冲突按
     * {@code DuplicateKeyException} 落到既有行的分支），行存在只回写四列提供方观察值与时间戳，
     * 生命周期状态、尝试计数、链接与业务 {@code revision} 原样保留；最后一律按业务键重读库中当前行返回，
     * 重读不到即旧的 {@code OpenAPI sync row disappeared after upsert} 异常，绝不返回入参冒充落库结果。
     * English summary: Executes the upsert-discovered operation; the equivalent of the legacy
     * {@code INSERT ... ON CONFLICT (application_id, build_id, openapi_group) DO UPDATE SET artifact_version,
     * provider_service_name, provider_group, provider_version, updated_at}: a non-{@code DISCOVERED} carrier is rejected
     * with the original exception first, an absent row goes through the guarded insert (a {@code DuplicateKeyException}
     * falls into the branch that keeps the pre-existing row, exactly what ON CONFLICT resolved in place), an existing row
     * only gets the four provider observation columns and the timestamp rewritten while the lifecycle state, attempt
     * counter, links and business {@code revision} stay untouched; the persisted row is always re-read by business key and
     * returned, a missing reload raising the legacy {@code OpenAPI sync row disappeared after upsert} instead of
     * echoing the argument back as a fake success.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayOpenApiSyncRepository.upsertDiscovered(state)}。
     * @param state 参数 已发现的观察结果；parameter discovered observation。
     * @return 返回 upsertDiscovered 的处理结果；returns the current persisted carrier.
     */
    @Override
    public GatewayOpenApiSyncBO upsertDiscovered(
            GatewayOpenApiSyncBO state) {
        Objects.requireNonNull(state, "state");
        if (state.getStatus() != GatewayOpenApiSyncStateEnum.DISCOVERED) {
            throw new IllegalArgumentException(
                    "upsertDiscovered requires DISCOVERED state"
            );
        }
        Optional<GatewayOpenApiSyncRecordPO> current = firstRow(
                state.getApplicationId(),
                state.getBuildId(),
                state.getOpenapiGroup()
        );
        if (current.isEmpty()) {
            discover(state);
        } else {
            refreshObservation(current.get(), state);
        }
        return findByKey(new GatewayOpenApiSyncKeyDTO(
                state.getApplicationId(),
                state.getBuildId(),
                state.getOpenapiGroup()
        )).orElseThrow(() -> new IllegalStateException(
                "OpenAPI sync row disappeared after upsert"
        ));
    }

    /**
     * 中文说明：执行 find 操作；保留旧 SQL 的 {@code status IN claimable AND (next_retry_at IS NULL OR
     * next_retry_at <= ?)} 谓词与 {@code ORDER BY next_retry_at NULLS FIRST, id} 次序及 {@code LIMIT ?}：
     * 受守卫 API 不表达 NULLS FIRST，故按“先无重试时间的到期行（按 id 升序）、再按重试时间升序（并列时按 id）”
     * 两次读取拼接，最后以 {@code limit} 截断，结果集与次序与原语句一致。
     * English summary: Executes the find operation; keeps the legacy
     * {@code status IN claimable AND (next_retry_at IS NULL OR next_retry_at <= ?)} predicate, the
     * {@code ORDER BY next_retry_at NULLS FIRST, id} ordering and the {@code LIMIT ?}: the guarded API cannot express
     * NULLS FIRST, so two reads are concatenated — rows without a retry time ordered by id, then rows due by retry time
     * and id — and the combined result is truncated to {@code limit}, yielding the same rows in the same order.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayOpenApiSyncRepository.findDue(now, limit)}。
     * @param now 参数 当前UTC时刻；parameter current UTC instant。
     * @param limit 参数 最大行数；parameter maximum number of rows。
     * @return 返回 find 的处理结果；returns the due carriers.
     */
    @Override
    public List<GatewayOpenApiSyncBO> findDue(Instant now, int limit) {
        Objects.requireNonNull(now, "now");
        if (limit <= 0) {
            return List.of();
        }
        List<GatewayOpenApiSyncRecordPO> rows = new ArrayList<>(
                syncPersistenceRepository.list(
                        boundPredicate(Wrappers.<GatewayOpenApiSyncRecordPO>lambdaQuery()
                                .in(GatewayOpenApiSyncRecordPO::getStatus, CLAIMABLE_STATES)
                                .isNull(GatewayOpenApiSyncRecordPO::getNextRetryAt)
                                .orderByAsc(GatewayOpenApiSyncRecordPO::getId))
                )
        );
        if (rows.size() < limit) {
            rows.addAll(syncPersistenceRepository.list(
                    boundPredicate(Wrappers.<GatewayOpenApiSyncRecordPO>lambdaQuery()
                            .in(GatewayOpenApiSyncRecordPO::getStatus, CLAIMABLE_STATES)
                            .le(GatewayOpenApiSyncRecordPO::getNextRetryAt, now)
                            .orderByAsc(GatewayOpenApiSyncRecordPO::getNextRetryAt)
                            .orderByAsc(GatewayOpenApiSyncRecordPO::getId))
            ));
        }
        return rows.stream()
                .limit(limit)
                .map(this::toCarrier)
                .toList();
    }

    /**
     * 中文说明：执行 claim 操作；等价于旧 {@code UPDATE ... SET status = 'FETCHING', attempt_count = attempt_count + 1,
     * last_attempt_at = ?, next_retry_at = NULL, last_error_code = NULL, last_error_message = NULL,
     * revision = revision + 1, updated_at = ? WHERE id = ? AND revision = ? AND status IN claimable
     * AND (next_retry_at IS NULL OR next_retry_at <= ?)}：先按主键做受守卫活跃读取以取得技术 {@code id}/{@code version}
     * 作为 CAS 令牌，再把原 WHERE 谓词连同三列置空一起下推；行不存在、谓词不匹配或影响 0 行都返回
     * {@code false}，业务 {@code revision} 也只在命中时自增。
     * English summary: Executes the claim operation; the equivalent of the legacy
     * {@code UPDATE ... SET status = 'FETCHING', attempt_count = attempt_count + 1, last_attempt_at = ?, next_retry_at =
     * NULL, last_error_code = NULL, last_error_message = NULL, revision = revision + 1, updated_at = ? WHERE id = ? AND
     * revision = ? AND status IN claimable AND (next_retry_at IS NULL OR next_retry_at <= ?)}: the active row is first
     * read through the guarded boundary to obtain the technical {@code id} and {@code version} as the CAS token, then the
     * original predicates are pushed down together with the three columns cleared to NULL; a missing row, an unmatched
     * predicate or a zero-row effect returns {@code false}, and the business {@code revision} only increments on a hit.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayOpenApiSyncRepository.claim(syncId, expectedRevision, now)}。
     * @param syncId 参数 行标识；parameter row identifier。
     * @param expectedRevision 参数 调用方观察到的修订；parameter revision observed by the caller。
     * @param now 参数 认领时刻；parameter claim timestamp。
     * @return 返回 claim 的处理结果；returns {@code true} when ownership was acquired.
     */
    @Override
    public boolean claim(String syncId, long expectedRevision, Instant now) {
        requireRevision(expectedRevision);
        Objects.requireNonNull(now, "now");
        Optional<GatewayOpenApiSyncRecordPO> current = activeRow(syncId);
        if (current.isEmpty()) {
            return false;
        }
        GatewayOpenApiSyncRecordPO row = current.get();
        GatewayOpenApiSyncRecordPO claimed = GatewayOpenApiSyncRecordPO.builder()
                .id(row.getId())
                .version(row.getVersion())
                .status(GatewayOpenApiSyncStateEnum.FETCHING.wireValue())
                .attemptCount(nextAttempt(row.getAttemptCount()))
                .lastAttemptAt(now)
                .revision(expectedRevision + 1L)
                .updateTime(now)
                .build();
        return syncPersistenceRepository.update(
                claimed,
                boundPredicate(claimPredicate(row.getId(), expectedRevision, now)
                        .set(GatewayOpenApiSyncRecordPO::getNextRetryAt, null)
                        .set(GatewayOpenApiSyncRecordPO::getLastErrorCode, null)
                        .set(GatewayOpenApiSyncRecordPO::getLastErrorMessage, null))
        );
    }

    /**
     * 中文说明：执行 transition 操作；等价于旧 {@code UPDATE ... SET status = ?, revision = revision + 1,
     * updated_at = ? WHERE id = ? AND revision = ? AND status = ?}，非法状态迁移在触达持久边界前按旧实现的
     * {@code IllegalArgumentException} 抛出；影响 0 行返回 {@code false}。
     * English summary: Executes the transition operation; the equivalent of the legacy
     * {@code UPDATE ... SET status = ?, revision = revision + 1, updated_at = ? WHERE id = ? AND revision = ? AND status
     * = ?}, with an illegal state-machine move raised before persistence is touched; a zero-row effect returns
     * {@code false}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayOpenApiSyncRepository.transition(syncId, expectedRevision,
     * expectedState, nextState, now)}。
     * @param syncId 参数 行标识；parameter row identifier。
     * @param expectedRevision 参数 调用方观察到的修订；parameter revision observed by the caller。
     * @param expectedState 参数 调用方期望的当前状态；parameter current state expected by the caller。
     * @param nextState 参数 请求的下一状态；parameter requested next state。
     * @param now 参数 迁移时刻；parameter transition timestamp。
     * @return 返回 transition 的处理结果；returns {@code true} when one row was updated.
     */
    @Override
    public boolean transition(
            String syncId,
            long expectedRevision,
            GatewayOpenApiSyncStateEnum expectedState,
            GatewayOpenApiSyncStateEnum nextState,
            Instant now) {
        requireRevision(expectedRevision);
        Objects.requireNonNull(expectedState, "expectedState");
        Objects.requireNonNull(nextState, "nextState");
        Objects.requireNonNull(now, "now");
        if (!expectedState.canTransitionTo(nextState)) {
            throw new IllegalArgumentException(
                    "illegal OpenAPI sync transition: "
                            + expectedState
                            + " -> "
                            + nextState
            );
        }
        Optional<GatewayOpenApiSyncRecordPO> current = activeRow(syncId);
        if (current.isEmpty()) {
            return false;
        }
        GatewayOpenApiSyncRecordPO row = current.get();
        GatewayOpenApiSyncRecordPO moved = GatewayOpenApiSyncRecordPO.builder()
                .id(row.getId())
                .version(row.getVersion())
                .status(nextState.wireValue())
                .revision(expectedRevision + 1L)
                .updateTime(now)
                .build();
        return syncPersistenceRepository.update(
                moved,
                boundPredicate(Wrappers.<GatewayOpenApiSyncRecordPO>lambdaUpdate()
                        .eq(GatewayOpenApiSyncRecordPO::getId, row.getId())
                        .eq(GatewayOpenApiSyncRecordPO::getRevision, expectedRevision)
                        .eq(GatewayOpenApiSyncRecordPO::getStatus, expectedState.wireValue()))
        );
    }

    /**
     * 中文说明：执行 setValid 操作；等价于旧 {@code UPDATE ... SET status = 'VALID', latest_snapshot_id = ?,
     * definition_set_id = ?, last_success_at = ?, next_retry_at = NULL, last_error_code = NULL,
     * last_error_message = NULL, revision = revision + 1, updated_at = ? WHERE id = ? AND revision = ? AND
     * status = 'INGESTING'}；标识必填校验与非十进制守护先于业务，行缺失、状态或修订不匹配、影响 0 行都返回
     * {@code false}。
     * English summary: Executes the set-valid operation; the equivalent of the legacy
     * {@code UPDATE ... SET status = 'VALID', latest_snapshot_id = ?, definition_set_id = ?, last_success_at = ?,
     * next_retry_at = NULL, last_error_code = NULL, last_error_message = NULL, revision = revision + 1, updated_at = ?
     * WHERE id = ? AND revision = ? AND status = 'INGESTING'}: the required and numeric identifier guards run before the
     * business check, and a missing row, an unmatched state or revision, or a zero-row effect returns {@code false}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayOpenApiSyncRepository.setValid(syncId, expectedRevision, snapshotId,
     * definitionSetId, now)}。
     * @param syncId 参数 行标识；parameter row identifier。
     * @param expectedRevision 参数 调用方观察到的修订；parameter revision observed by the caller。
     * @param snapshotId 参数 最新Group快照；parameter latest Group snapshot。
     * @param definitionSetId 参数 聚合Definition Set；parameter aggregate Definition Set。
     * @param now 参数 成功时刻；parameter success timestamp。
     * @return 返回 setValid 的处理结果；returns {@code true} when one row was updated.
     */
    @Override
    public boolean setValid(
            String syncId,
            long expectedRevision,
            String snapshotId,
            String definitionSetId,
            Instant now) {
        requireRevision(expectedRevision);
        Long snapshot = numeric(required(snapshotId, "snapshotId"), "snapshotId");
        String definitionSet = required(definitionSetId, "definitionSetId");
        Objects.requireNonNull(now, "now");
        Long definitionSetColumn = numeric(definitionSet, "definitionSetId");
        Optional<GatewayOpenApiSyncRecordPO> current = activeRow(syncId);
        if (current.isEmpty()) {
            return false;
        }
        GatewayOpenApiSyncRecordPO row = current.get();
        GatewayOpenApiSyncRecordPO valid = GatewayOpenApiSyncRecordPO.builder()
                .id(row.getId())
                .version(row.getVersion())
                .status(GatewayOpenApiSyncStateEnum.VALID.wireValue())
                .latestSnapshotId(snapshot)
                .definitionSetId(definitionSetColumn)
                .lastSuccessAt(now)
                .revision(expectedRevision + 1L)
                .updateTime(now)
                .build();
        return syncPersistenceRepository.update(
                valid,
                boundPredicate(Wrappers.<GatewayOpenApiSyncRecordPO>lambdaUpdate()
                        .eq(GatewayOpenApiSyncRecordPO::getId, row.getId())
                        .eq(GatewayOpenApiSyncRecordPO::getRevision, expectedRevision)
                        .eq(
                                GatewayOpenApiSyncRecordPO::getStatus,
                                GatewayOpenApiSyncStateEnum.INGESTING.wireValue()
                        )
                        .set(GatewayOpenApiSyncRecordPO::getNextRetryAt, null)
                        .set(GatewayOpenApiSyncRecordPO::getLastErrorCode, null)
                        .set(GatewayOpenApiSyncRecordPO::getLastErrorMessage, null))
        );
    }

    /**
     * 中文说明：执行 markFailure 操作；等价于旧 {@code UPDATE ... SET status = ?, last_error_code = ?,
     * last_error_message = ?, next_retry_at = ?, revision = revision + 1, updated_at = ? WHERE id = ? AND revision = ?
     * AND status IN (allowedFailureStates)}：不受支持的失败状态、超长错误详情与非正修订都按旧实现的异常先抛出，
     * 允许的源状态集合逐一对应旧 SQL；{@code next_retry_at} 与原实现一样总是被写入（可为 {@code NULL}），
     * 行缺失、状态或修订不匹配、影响 0 行都返回 {@code false}。
     * English summary: Executes the mark-failure operation; the equivalent of the legacy
     * {@code UPDATE ... SET status = ?, last_error_code = ?, last_error_message = ?, next_retry_at = ?,
     * revision = revision + 1, updated_at = ? WHERE id = ? AND revision = ? AND status IN (allowedFailureStates)}: an
     * unsupported failure state, oversized error details or a negative revision are raised first exactly as before and the
     * allowed source-state sets mirror the legacy SQL one by one; {@code next_retry_at} is always written (possibly
     * {@code NULL}) like the original, while a missing row, an unmatched state or revision, or a zero-row effect returns
     * {@code false}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayOpenApiSyncRepository.markFailure(syncId, expectedRevision,
     * nextState, errorCode, errorMessage, nextRetryAt, now)}。
     * @param syncId 参数 行标识；parameter row identifier。
     * @param expectedRevision 参数 调用方观察到的修订；parameter revision observed by the caller。
     * @param nextState 参数 失败或终态；parameter failure or terminal state。
     * @param errorCode 参数 有界稳定错误码；parameter bounded stable error code。
     * @param errorMessage 参数 有界安全运维信息；parameter bounded safe operator message。
     * @param nextRetryAt 参数 重试时刻或 null；parameter retry timestamp, or {@code null}.
     * @param now 参数 迁移时刻；parameter transition timestamp。
     * @return 返回 markFailure 的处理结果；returns {@code true} when one row was updated.
     */
    @Override
    public boolean markFailure(
            String syncId,
            long expectedRevision,
            GatewayOpenApiSyncStateEnum nextState,
            String errorCode,
            String errorMessage,
            Instant nextRetryAt,
            Instant now) {
        requireRevision(expectedRevision);
        Objects.requireNonNull(nextState, "nextState");
        if (!SUPPORTED_FAILURE_STATES.contains(nextState)) {
            throw new IllegalArgumentException(
                    "unsupported OpenAPI sync failure state: " + nextState
            );
        }
        String code = required(errorCode, "errorCode");
        String message = required(errorMessage, "errorMessage");
        if (code.length() > ERROR_CODE_LIMIT || message.length() > ERROR_MESSAGE_LIMIT) {
            throw new IllegalArgumentException(
                    "OpenAPI sync failure details exceed database limits"
            );
        }
        Objects.requireNonNull(now, "now");
        List<String> allowedStates = allowedFailureStates(nextState);
        Optional<GatewayOpenApiSyncRecordPO> current = activeRow(syncId);
        if (current.isEmpty()) {
            return false;
        }
        GatewayOpenApiSyncRecordPO row = current.get();
        GatewayOpenApiSyncRecordPO failed = GatewayOpenApiSyncRecordPO.builder()
                .id(row.getId())
                .version(row.getVersion())
                .status(nextState.wireValue())
                .lastErrorCode(code)
                .lastErrorMessage(message)
                .revision(expectedRevision + 1L)
                .updateTime(now)
                .build();
        return syncPersistenceRepository.update(
                failed,
                boundPredicate(Wrappers.<GatewayOpenApiSyncRecordPO>lambdaUpdate()
                        .eq(GatewayOpenApiSyncRecordPO::getId, row.getId())
                        .eq(GatewayOpenApiSyncRecordPO::getRevision, expectedRevision)
                        .in(GatewayOpenApiSyncRecordPO::getStatus, allowedStates)
                        .set(GatewayOpenApiSyncRecordPO::getNextRetryAt, nextRetryAt))
        );
    }

    /**
     * 中文说明：执行 discover 操作；受守卫插入新的发现行（租户、审计与技术 {@code version} 由边界补齐），
     * 唯一键上的并发冲突按旧 {@code ON CONFLICT DO UPDATE} 的意图交给后续重读，因此在此如实记录并继续。
     * English summary: Executes the discover operation; inserts the discovered row through the guarded store (the boundary
     * fills tenant, audit and the technical {@code version}) and, because a concurrent conflict on the unique key is
     * resolved by the follow-up reload exactly as the legacy {@code ON CONFLICT DO UPDATE} did, records it and continues.
     * @param state 参数 已发现的观察结果；parameter the discovered observation.
     */
    private void discover(GatewayOpenApiSyncBO state) {
        try {
            if (!syncPersistenceRepository.save(
                    syncPersistenceConverter.newRow(state))) {
                log.debug(
                        "gateway_openapi_sync_state discovery affected no row for {}/{}/{}; "
                                + "resolving the authoritative row",
                        state.getApplicationId(),
                        state.getBuildId(),
                        state.getOpenapiGroup()
                );
            }
        } catch (DuplicateKeyException conflict) {
            log.debug(
                    "gateway_openapi_sync_state discovery raced for {}/{}/{}",
                    state.getApplicationId(),
                    state.getBuildId(),
                    state.getOpenapiGroup(),
                    conflict
            );
        }
    }

    /**
     * 中文说明：执行 refreshObservation 操作；只把旧 {@code DO UPDATE SET} 列出的四列提供方观察值与
     * {@code updated_at}（迁移后为 {@code update_time}）写回既有行，其余生命周期列与业务 {@code revision}
     * 沿用读到的原值，等价于旧语句对它们的沉默；CAS 失败时如实记录并交由权威重读返回。
     * English summary: Executes the refresh-observation operation; only the four provider observation columns named by the
     * legacy {@code DO UPDATE SET} plus {@code updated_at} (the migrated {@code update_time}) are written back, every
     * lifecycle column and the business {@code revision} keeping the values just read, which is what the legacy statement
     * left untouched; a lost compare-and-set is recorded honestly and the authoritative reload decides the result.
     * @param row 参数 既有活跃行；parameter the pre-existing active row.
     * @param state 参数 已发现的观察结果；parameter the discovered observation.
     */
    private void refreshObservation(
            GatewayOpenApiSyncRecordPO row,
            GatewayOpenApiSyncBO state) {
        row.setArtifactVersion(state.getArtifactVersion());
        row.setProviderServiceName(state.getProviderServiceName());
        row.setProviderGroup(state.getProviderGroup());
        row.setProviderVersion(state.getProviderVersion());
        row.setUpdateTime(state.getUpdatedAt());
        if (!syncPersistenceRepository.updateById(row)) {
            log.debug(
                    "gateway_openapi_sync_state {} lost the observation refresh to a concurrent worker; "
                            + "returning the authoritative row",
                    row.getId()
            );
        }
    }

    /**
     * 中文说明：按业务键三元组做受守卫读取并取首行，等价于旧 {@code ON CONFLICT} 目标键的定位方式。
     * English summary: Performs the guarded read by the three-part business key and takes the first row, which is how the
     * legacy {@code ON CONFLICT} target key was located.
     * @param applicationId 参数 归属应用；parameter owning application identifier.
     * @param buildId 参数 不可变提供方构建；parameter immutable provider build.
     * @param openapiGroup 参数 来源Group；parameter source Group.
     * @return 返回 首行行模型；returns the first row model when present.
     */
    private Optional<GatewayOpenApiSyncRecordPO> firstRow(
            String applicationId,
            String buildId,
            String openapiGroup) {
        Long applicationColumn = columnValue(applicationId);
        if (applicationColumn == null) {
            return Optional.empty();
        }
        return syncPersistenceRepository.list(
                boundPredicate(Wrappers.<GatewayOpenApiSyncRecordPO>lambdaQuery()
                        .eq(GatewayOpenApiSyncRecordPO::getApplicationId, applicationColumn)
                        .eq(GatewayOpenApiSyncRecordPO::getBuildId, buildId)
                        .eq(GatewayOpenApiSyncRecordPO::getOpenapiGroup, openapiGroup))
        ).stream().findFirst();
    }

    /**
     * 中文说明：按主键读取活跃同步行，作为业务修订 CAS 的技术令牌来源；非十进制标识、行缺失、
     * 跨租户与软删一律按“无可更新行”处理。
     * English summary: Reads the active synchronization row by primary key as the source of the technical token for the
     * business-revision compare-and-set; a non-decimal identifier, a missing row, another tenant or a soft-deleted row all
     * mean “nothing to update”.
     * @param syncId 参数 行标识；parameter row identifier.
     * @return 返回 活跃行；returns the active row when present.
     */
    private Optional<GatewayOpenApiSyncRecordPO> activeRow(String syncId) {
        Long idColumn = columnValue(syncId);
        if (idColumn == null) {
            return Optional.empty();
        }
        return syncPersistenceRepository.getOptById(idColumn);
    }

    /**
     * 中文说明：构造旧 {@code claim} 的 WHERE 谓词：同一行、同一业务 {@code revision}、状态属于可认领集合，
     * 且尚未安排重试或重试时间已到期。
     * English summary: Builds the WHERE clause of the legacy {@code claim}: the same row, the same business
     * {@code revision}, a claimable state, and either no scheduled retry or a retry time that has come due.
     * @param idColumn 参数 数值主键；parameter the numeric primary key.
     * @param expectedRevision 参数 期望修订；parameter the expected revision.
     * @param now 参数 认领时刻；parameter the claim timestamp.
     * @return 返回 带业务谓词的受守卫更新条件；returns the guarded update predicate carrying the business predicates.
     */
    private LambdaUpdateWrapper<GatewayOpenApiSyncRecordPO> claimPredicate(
            Long idColumn,
            long expectedRevision,
            Instant now) {
        return casPredicate(idColumn, expectedRevision)
                .in(GatewayOpenApiSyncRecordPO::getStatus, CLAIMABLE_STATES)
                .and(due -> due
                        .isNull(GatewayOpenApiSyncRecordPO::getNextRetryAt)
                        .or()
                        .le(GatewayOpenApiSyncRecordPO::getNextRetryAt, now));
    }

    /**
     * 中文说明：构造所有 CAS 方法共用的 {@code WHERE id = ? AND revision = ?} 前缀；受守卫
     * {@code EgonColaRepository#update} 要求非空条件（{@code BUSINESS_PREDICATE_REQUIRED}），因此主键与业务修订
     * 留在这一层，状态谓词由各自的方法继续追加。
     * English summary: Builds the shared {@code WHERE id = ? AND revision = ?} prefix of every compare-and-set method; the
     * guarded {@code EgonColaRepository#update} demands a non-empty condition ({@code BUSINESS_PREDICATE_REQUIRED}), so the
     * primary key and the business revision live here while each method appends its own status predicates.
     * @param idColumn 参数 数值主键；parameter the numeric primary key.
     * @param expectedRevision 参数 期望修订；parameter the expected revision.
     * @return 返回 带业务谓词的受守卫更新条件；returns the guarded update predicate carrying the business predicates.
     */
    private LambdaUpdateWrapper<GatewayOpenApiSyncRecordPO> casPredicate(
            Long idColumn,
            long expectedRevision) {
        return Wrappers.<GatewayOpenApiSyncRecordPO>lambdaUpdate()
                .eq(GatewayOpenApiSyncRecordPO::getId, idColumn)
                .eq(GatewayOpenApiSyncRecordPO::getRevision, expectedRevision);
    }

    /**
     * 中文说明：把条件交付受守卫边界之前先成形一次：MyBatis-Plus 的 {@code eq/in/le} 只在 SQL 真正成形时
     * 才把取值写进 {@code paramNameValuePairs}，故此处先成形一次，让谓词在离开门面时参数已完全绑定；
     * 之后（包括 MyBatis 自己下发时）命中同一份片段缓存，参数与 SQL 都不再改变，列解析失败
     * （lambda 缓存缺失）也如实在门面这一层暴露，而不是留到语句下发时。
     * English summary: Forms a condition once before it is handed to the guarded boundary: MyBatis-Plus only moves the
     * values of {@code eq/in/le} into {@code paramNameValuePairs} while the SQL is being formed, so forming it here first
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
     * 中文说明：把持久层行模型投影为业务载体并复核旧构造器不变量（列映射只由转换器负责）。
     * English summary: Projects a persistence row onto the business carrier and re-checks the legacy constructor
     * invariants; column mapping stays exclusively in the converter.
     * @param row 参数 行模型；parameter the row model.
     * @return 返回 业务载体；returns the business carrier.
     */
    private GatewayOpenApiSyncBO toCarrier(GatewayOpenApiSyncRecordPO row) {
        return GatewayOpenApiSyncBO.validated(
                syncPersistenceConverter.toBusiness(row)
        );
    }

    /**
     * 中文说明：逐行投影为业务载体，保证任何一行都不绕过装载边界。
     * English summary: Projects every row onto a business carrier so no row can bypass the load boundary.
     * @param rows 参数 行模型列表；parameter the row models.
     * @return 返回 业务载体列表；returns the business carriers.
     */
    private List<GatewayOpenApiSyncBO> toCarriers(
            List<GatewayOpenApiSyncRecordPO> rows) {
        return rows.stream().map(this::toCarrier).toList();
    }

    /**
     * 中文说明：把 {@code markFailure} 的目标状态映射为旧 SQL 允许的源状态集合，逐字对应原
     * {@code allowedFailureStates} 的字面量集合；未知目标状态按旧实现抛出。
     * English summary: Maps the {@code markFailure} target state onto the source states the legacy SQL accepted, mirroring
     * the original {@code allowedFailureStates} literals one by one, and raises the legacy error for an unknown target.
     * @param nextState 参数 失败或终态；parameter failure or terminal state.
     * @return 返回 允许的源状态；returns the allowed source states.
     */
    private static List<String> allowedFailureStates(
            GatewayOpenApiSyncStateEnum nextState) {
        return switch (nextState) {
            case FETCH_FAILED -> List.of(
                    GatewayOpenApiSyncStateEnum.FETCHING.wireValue());
            case INGEST_FAILED -> List.of(
                    GatewayOpenApiSyncStateEnum.INGESTING.wireValue());
            case INVALID, INCONSISTENT_BUILD -> List.of(
                    GatewayOpenApiSyncStateEnum.VALIDATING.wireValue());
            case STALE -> List.of(
                    GatewayOpenApiSyncStateEnum.DISCOVERED.wireValue(),
                    GatewayOpenApiSyncStateEnum.FETCHING.wireValue(),
                    GatewayOpenApiSyncStateEnum.VALIDATING.wireValue(),
                    GatewayOpenApiSyncStateEnum.INVALID.wireValue(),
                    GatewayOpenApiSyncStateEnum.INCONSISTENT_BUILD.wireValue(),
                    GatewayOpenApiSyncStateEnum.INGESTING.wireValue(),
                    GatewayOpenApiSyncStateEnum.VALID.wireValue(),
                    GatewayOpenApiSyncStateEnum.FETCH_FAILED.wireValue(),
                    GatewayOpenApiSyncStateEnum.INGEST_FAILED.wireValue());
            default -> throw new IllegalArgumentException(
                    "unsupported OpenAPI sync failure state: " + nextState
            );
        };
    }

    /**
     * 中文说明：按原 SQL 的 {@code attempt_count = attempt_count + 1} 递增读到的尝试计数，null 视为 0。
     * English summary: Increments the attempt counter that was read, matching the legacy
     * {@code attempt_count = attempt_count + 1} and treating null as zero.
     * @param attemptCount 参数 读到的尝试计数；parameter the stored attempt counter.
     * @return 返回 自增后的尝试计数；returns the incremented attempt counter.
     */
    private static Integer nextAttempt(Integer attemptCount) {
        return (attemptCount == null ? 0 : attemptCount) + 1;
    }

    /**
     * 中文说明：沿用原实现的修订守卫：负数业务 {@code revision} 抛
     * {@code IllegalArgumentException("revision must not be negative")}。
     * English summary: Keeps the original revision guard: a negative business {@code revision} raises
     * {@code IllegalArgumentException("revision must not be negative")}.
     * @param revision 参数 修订；parameter revision.
     */
    private static void requireRevision(long revision) {
        if (revision < 0) {
            throw new IllegalArgumentException(
                    "revision must not be negative"
            );
        }
    }

    /**
     * 中文说明：沿用旧实现的必填规范化：{@code null} 抛 {@code NullPointerException}，空白抛
     * {@code IllegalArgumentException(field + " must not be blank")}。
     * English summary: Keeps the legacy required normalization: {@code null} raises a {@code NullPointerException} and a
     * blank value raises {@code IllegalArgumentException(field + " must not be blank")}.
     * @param value 参数 值；parameter value.
     * @param field 参数 字段名；parameter field.
     * @return 返回 规范化后的值；returns the trimmed value.
     */
    private static String required(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return normalized;
    }

    /**
     * 中文说明：把端口上的不透明数值标识转换为受守卫谓词使用的列值；空白、非十进制或非正值返回
     * {@code null}，调用方据此按“无匹配行”处理。
     * English summary: Converts an opaque numeric identifier from the port into the column value the guarded predicate
     * needs; blank, non-decimal or non-positive input yields {@code null} so the caller treats it as “no matching row”.
     * @param opaqueId 参数 不透明标识；parameter opaque identifier。
     * @return 返回 列值或 null；returns the column value or null.
     */
    private static Long columnValue(String opaqueId) {
        if (opaqueId == null || opaqueId.isBlank()) {
            return null;
        }
        try {
            long parsed = Long.parseLong(opaqueId.trim());
            return parsed > 0L ? parsed : null;
        } catch (NumberFormatException invalidOpaqueId) {
            return null;
        }
    }

    /**
     * 中文说明：把必填的不透明标识转换为数值列值；迁移后 {@code latest_snapshot_id}/{@code definition_set_id}
     * 是数值列，无法表示的标识按调用方契约错误抛出，而不是写出 {@code NULL} 冒充链接成功。
     * English summary: Converts a required opaque identifier into the numeric column value; after migration
     * {@code latest_snapshot_id} and {@code definition_set_id} are numeric columns, so an unrepresentable identifier is
     * raised as a caller contract error instead of writing {@code NULL} and pretending the link succeeded.
     * @param opaqueId 参数 不透明标识；parameter opaque identifier。
     * @param field 参数 字段名；parameter field.
     * @return 返回 列值；returns the column value.
     */
    private static Long numeric(String opaqueId, String field) {
        Long column = columnValue(opaqueId);
        if (column == null) {
            throw new IllegalArgumentException(
                    field + " must be a numeric identifier"
            );
        }
        return column;
    }
}
