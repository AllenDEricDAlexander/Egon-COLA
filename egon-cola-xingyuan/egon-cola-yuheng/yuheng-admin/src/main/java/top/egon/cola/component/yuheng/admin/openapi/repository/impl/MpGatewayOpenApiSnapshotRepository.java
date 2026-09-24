package top.egon.cola.component.yuheng.admin.openapi.repository.impl;


import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.openapi.converter.GatewayOpenApiSnapshotPersistenceConverter;
import top.egon.cola.component.yuheng.admin.openapi.domain.bo.GatewayOpenApiSnapshotBO;
import top.egon.cola.component.yuheng.admin.openapi.domain.po.GatewayOpenApiSnapshotRecordPO;
import top.egon.cola.component.yuheng.admin.openapi.repository.GatewayOpenApiSnapshotRepository;
import top.egon.cola.component.yuheng.admin.openapi.repository.mp.GatewayOpenApiSnapshotPersistenceRepository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 中文说明：{@code MpGatewayOpenApiSnapshotRepository} 是 {@code gateway_openapi_snapshot} 的 MyBatis-Plus 门面存储，
 * 逐方法取代旧手写的 JDBC 适配器：读取保留原 SQL 的四列业务契约谓词、
 * {@code fetched_at DESC, id} 与 {@code openapi_group, id} 次序，写入保留
 * {@code ON CONFLICT (application_id, build_id, openapi_group, canonical_sha256) DO NOTHING} 的“新建或复用”语义，
 * 链接 Definition Set 保留 {@code WHERE id = ? AND definition_set_id IS NULL} 的 CAS、幂等重读与冲突判定；
 * 列与类型映射只经 MapStruct 转换器完成，公开端口不泄漏 {@code GatewayOpenApiSnapshotRecordPO}。
 * English summary: {@code MpGatewayOpenApiSnapshotRepository} is the MyBatis-Plus facade store for
 * {@code gateway_openapi_snapshot} that replaces the hand-written JDBC adapter method by
 * method: the reads keep the four-column business-contract predicates plus the {@code fetched_at DESC, id} and
 * {@code openapi_group, id} orderings, the insert keeps the
 * {@code ON CONFLICT (application_id, build_id, openapi_group, canonical_sha256) DO NOTHING} insert-or-reuse semantics,
 * and the Definition Set link keeps its {@code WHERE id = ? AND definition_set_id IS NULL} compare-and-set with the
 * idempotent reload and the conflict decision; column and type mapping happens only in the MapStruct converter and the
 * public port never leaks a {@code GatewayOpenApiSnapshotRecordPO}.
 *
 * 用法 / Usage: 通过业务端口 {@code GatewayOpenApiSnapshotRepository} 由 Spring 容器注入，写入组合在调用方
 * {@code gatewayTransactionManager} 的同一事务内；每次读取与写入都经由该表的受守卫 {@code EgonColaRepository}，
 * 因而天然带同租户过滤、仅活跃行（{@code deleted_at IS NULL}）与技术 {@code version} 乐观锁，影响 0 行的写入
 * 一律走“复用既有意图”或如实抛出冲突的分支，绝不伪造成功；快照的构造不变量继续由
 * {@code GatewayOpenApiSnapshotBO.validated(...)} 在装载与写入两处把守。/ Use it through the
 * {@code GatewayOpenApiSnapshotRepository} port with writes composed inside the caller's
 * {@code gatewayTransactionManager} transaction; every read and write goes through this table's guarded
 * {@code EgonColaRepository}, so same-tenant filtering, active rows only ({@code deleted_at IS NULL}) and the technical
 * {@code version} optimistic lock are structural, and a zero-row effect always falls into the “reuse the existing
 * business intent” branch or raises the conflict instead of a fake success. The carrier invariants stay enforced by
 * {@code GatewayOpenApiSnapshotBO.validated(...)} on both the load and the write path.
 */
@Slf4j
@Repository("mpGatewayOpenApiSnapshotRepository")
@RequiredArgsConstructor
@Validated
public class MpGatewayOpenApiSnapshotRepository implements GatewayOpenApiSnapshotRepository {

    /** 中文说明：快照表的受守卫持久化仓储（租户过滤、活跃读取、乐观锁 CAS 的唯一入口）。 English summary: the guarded persistence store for the snapshot table, the only entry point for tenant filtering, active reads and optimistic-lock CAS. */
    @Qualifier("gatewayOpenApiSnapshotPersistenceRepository")
    private final GatewayOpenApiSnapshotPersistenceRepository snapshotPersistenceRepository;

    /** 中文说明：{@code GatewayOpenApiSnapshotBO} 与 {@code GatewayOpenApiSnapshotRecordPO} 的双向转换器。 English summary: the bidirectional converter between GatewayOpenApiSnapshotBO and GatewayOpenApiSnapshotRecordPO. */
    @Qualifier("gatewayOpenApiSnapshotPersistenceConverter")
    private final GatewayOpenApiSnapshotPersistenceConverter snapshotPersistenceConverter;

    /**
     * 中文说明：执行 find 操作；等价于旧 {@code SELECT ... WHERE id = ?}，改走受守卫的按主键活跃读取
     * （{@code selectActiveById} 内含 {@code deleted_at IS NULL} 并由边界限定当前租户），因此软删行与跨租户行按不存在处理；
     * 非十进制的不透明 id 同样按不存在处理，保持旧实现返回空 {@code Optional} 的形状。
     * English summary: Executes the find operation; the equivalent of the legacy {@code SELECT ... WHERE id = ?} now served
     * by the guarded active read by primary key ({@code selectActiveById} carries {@code deleted_at IS NULL} and the
     * boundary pins the current tenant), so a soft-deleted or foreign-tenant row counts as absent; a non-decimal opaque
     * identifier counts as absent too, preserving the legacy empty-{@code Optional} shape.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayOpenApiSnapshotRepository.findById(snapshotId)}。
     * @param snapshotId 参数 快照Id；parameter snapshot identifier。
     * @return 返回 find 的处理结果；returns the snapshot carrier when present.
     */
    @Override
    public Optional<GatewayOpenApiSnapshotBO> findById(String snapshotId) {
        Long idColumn = columnValue(snapshotId);
        if (idColumn == null) {
            return Optional.empty();
        }
        return snapshotPersistenceRepository.getOptById(idColumn)
                .map(this::toCarrier);
    }

    /**
     * 中文说明：执行 find 操作；保留旧 SQL 的 {@code application_id/build_id/openapi_group/canonical_sha256} 四列业务
     * 契约谓词，并按旧 {@code query(...).stream().findFirst()} 取首行（历史脏数据多行时不抛异常）。
     * English summary: Executes the find operation; keeps the legacy four-column business-contract predicates
     * {@code application_id/build_id/openapi_group/canonical_sha256} and, like the old
     * {@code query(...).stream().findFirst()}, returns the first row instead of failing on dirty history.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayOpenApiSnapshotRepository.findByContract(applicationId, buildId,
     * openapiGroup, canonicalSha256)}。
     * @param applicationId 参数 归属应用；parameter owning application identifier。
     * @param buildId 参数 不可变提供方构建；parameter immutable provider build。
     * @param openapiGroup 参数 来源Group；parameter source Group。
     * @param canonicalSha256 参数 规范文档哈希；parameter canonical document hash。
     * @return 返回 find 的处理结果；returns an existing snapshot carrier when present.
     */
    @Override
    public Optional<GatewayOpenApiSnapshotBO> findByContract(
            String applicationId,
            String buildId,
            String openapiGroup,
            String canonicalSha256) {
        Long applicationColumn = columnValue(applicationId);
        if (applicationColumn == null) {
            return Optional.empty();
        }
        return snapshotPersistenceRepository.list(
                        boundPredicate(Wrappers.<GatewayOpenApiSnapshotRecordPO>lambdaQuery()
                                .eq(GatewayOpenApiSnapshotRecordPO::getApplicationId, applicationColumn)
                                .eq(GatewayOpenApiSnapshotRecordPO::getBuildId, buildId)
                                .eq(GatewayOpenApiSnapshotRecordPO::getOpenapiGroup, openapiGroup)
                                .eq(GatewayOpenApiSnapshotRecordPO::getCanonicalSha256, canonicalSha256)))
                .stream()
                .findFirst()
                .map(this::toCarrier);
    }

    /**
     * 中文说明：执行 find 操作；保留旧 SQL 的 {@code application_id/openapi_group/canonical_sha256} 谓词与
     * {@code ORDER BY fetched_at DESC, id} 次序，取最新一份不可变快照。
     * English summary: Executes the find operation; keeps the legacy {@code application_id/openapi_group/canonical_sha256}
     * predicates with {@code ORDER BY fetched_at DESC, id} and returns the newest immutable snapshot.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayOpenApiSnapshotRepository
     * .findByApplicationGroupAndCanonicalSha256(applicationId, openapiGroup, canonicalSha256)}。
     * @param applicationId 参数 归属应用；parameter owning application identifier。
     * @param openapiGroup 参数 来源Group；parameter source Group。
     * @param canonicalSha256 参数 规范文档哈希；parameter canonical document hash。
     * @return 返回 find 的处理结果；returns the newest matching snapshot carrier when present.
     */
    @Override
    public Optional<GatewayOpenApiSnapshotBO> findByApplicationGroupAndCanonicalSha256(
            String applicationId,
            String openapiGroup,
            String canonicalSha256) {
        Long applicationColumn = columnValue(applicationId);
        if (applicationColumn == null) {
            return Optional.empty();
        }
        return snapshotPersistenceRepository.list(
                        boundPredicate(Wrappers.<GatewayOpenApiSnapshotRecordPO>lambdaQuery()
                                .eq(GatewayOpenApiSnapshotRecordPO::getApplicationId, applicationColumn)
                                .eq(GatewayOpenApiSnapshotRecordPO::getOpenapiGroup, openapiGroup)
                                .eq(GatewayOpenApiSnapshotRecordPO::getCanonicalSha256, canonicalSha256)
                                .orderByDesc(GatewayOpenApiSnapshotRecordPO::getFetchedAt)
                                .orderByAsc(GatewayOpenApiSnapshotRecordPO::getId)))
                .stream()
                .findFirst()
                .map(this::toCarrier);
    }

    /**
     * 中文说明：执行 insertOrReuse 操作；先按旧实现调用 {@code GatewayOpenApiSnapshotBO.validated(...)} 复核不可变契约，
     * 再走受守卫插入（租户、审计与技术 {@code version} 由边界补齐）。原 {@code ON CONFLICT ... DO NOTHING} 在
     * MyBatis-Plus 下表现为唯一键冲突：{@code DuplicateKeyException} 与影响 0 行都进入“复用既有行”分支，即在同一
     * 调用内按业务契约四列重读并逐字段比对；重读不到行或契约不一致都按旧实现的
     * {@code YUHENG_OPENAPI_SNAPSHOT_CONFLICT} 抛出，绝不把 0 行写入报告为新建成功。
     * English summary: Executes the insert-or-reuse operation; the immutable contract is re-checked through
     * {@code GatewayOpenApiSnapshotBO.validated(...)} exactly as the legacy implementation did, then the guarded insert
     * runs (the boundary fills tenant, audit and the technical {@code version}). Under MyBatis-Plus the legacy
     * {@code ON CONFLICT ... DO NOTHING} surfaces as a unique-key conflict: both a {@code DuplicateKeyException} and a
     * zero-row effect enter the “reuse the existing row” branch, which reloads by the four business-contract columns and
     * compares every field; a missing reload or a contract mismatch raises the legacy
     * {@code YUHENG_OPENAPI_SNAPSHOT_CONFLICT}, so a zero-row write is never reported as a fresh insert.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayOpenApiSnapshotRepository.insertOrReuse(snapshot)}。
     * @param snapshot 参数 不可变快照载体；parameter the immutable snapshot carrier.
     * @return 返回 insertOrReuse 的处理结果；returns the inserted or reused carrier.
     */
    @Override
    public GatewayOpenApiSnapshotBO insertOrReuse(
            GatewayOpenApiSnapshotBO snapshot) {
        GatewayOpenApiSnapshotBO.validated(snapshot);
        boolean inserted;
        try {
            inserted = snapshotPersistenceRepository.save(
                    snapshotPersistenceConverter.newRow(snapshot)
            );
        } catch (DuplicateKeyException conflict) {
            log.debug(
                    "gateway_openapi_snapshot business contract raced for {}, resolving the existing intent",
                    snapshot.getId(),
                    conflict
            );
            inserted = false;
        }
        if (inserted) {
            return snapshot;
        }
        return reuseExisting(snapshot);
    }

    /**
     * 中文说明：执行 find 操作；保留旧 SQL 的 {@code application_id = ? AND build_id = ? AND openapi_group IN (...)}
     * 谓词与 {@code ORDER BY openapi_group, id} 次序，并沿用旧实现对 Group 集合的判空、去空白与保序去重
     * （集合为空时不触碰任何语句）。
     * English summary: Executes the find operation; keeps the legacy {@code application_id = ? AND build_id = ? AND
     * openapi_group IN (...)} predicates with {@code ORDER BY openapi_group, id} and the legacy handling of the Group
     * collection (null check, trim, order-preserving de-duplication, and no statement at all once it is empty).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayOpenApiSnapshotRepository.findByBuildGroups(applicationId, buildId,
     * openapiGroups)}。
     * @param applicationId 参数 归属应用；parameter owning application identifier。
     * @param buildId 参数 不可变提供方构建；parameter immutable provider build。
     * @param openapiGroups 参数 通告的Group编码；parameter advertised Group codes。
     * @return 返回 find 的处理结果；returns the carriers ordered by Group and id.
     */
    @Override
    public List<GatewayOpenApiSnapshotBO> findByBuildGroups(
            String applicationId,
            String buildId,
            List<String> openapiGroups) {
        Objects.requireNonNull(openapiGroups, "openapiGroups");
        Set<String> groups = openapiGroups.stream()
                .map(value -> Objects.requireNonNull(value, "openapiGroup"))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (groups.isEmpty()) {
            return List.of();
        }
        Long applicationColumn = columnValue(applicationId);
        if (applicationColumn == null) {
            return List.of();
        }
        return toCarriers(snapshotPersistenceRepository.list(
                boundPredicate(Wrappers.<GatewayOpenApiSnapshotRecordPO>lambdaQuery()
                        .eq(GatewayOpenApiSnapshotRecordPO::getApplicationId, applicationColumn)
                        .eq(GatewayOpenApiSnapshotRecordPO::getBuildId, buildId)
                        .in(GatewayOpenApiSnapshotRecordPO::getOpenapiGroup, groups)
                        .orderByAsc(GatewayOpenApiSnapshotRecordPO::getOpenapiGroup)
                        .orderByAsc(GatewayOpenApiSnapshotRecordPO::getId))
        ));
    }

    /**
     * 中文说明：执行 linkAll 操作；逐个快照复刻旧 SQL 的 {@code UPDATE ... SET definition_set_id = ?
     * WHERE id = ? AND definition_set_id IS NULL}：受守卫读取判行不存在（含跨租户与软删）时抛
     * {@code YUHENG_OPENAPI_SNAPSHOT_NOT_FOUND}，已链接到同一 Definition Set 时按幂等计数且不写库，
     * 链接到其它 Definition Set 时抛 {@code YUHENG_OPENAPI_SNAPSHOT_CONFLICT}，只有尚未链接的行才带
     * {@code definition_set_id IS NULL} 谓词做 CAS；CAS 影响 0 行会重读一次以区分“并发方已写入同一链接”（幂等计数）
     * 与真实冲突，任何情形都不会把 0 行报告为成功。
     * English summary: Executes the link-all operation; per snapshot it reproduces the legacy
     * {@code UPDATE ... SET definition_set_id = ? WHERE id = ? AND definition_set_id IS NULL}: the guarded read treats a
     * missing, foreign-tenant or soft-deleted row as {@code YUHENG_OPENAPI_SNAPSHOT_NOT_FOUND}, an identical existing link
     * is counted idempotently without any write, a foreign link raises
     * {@code YUHENG_OPENAPI_SNAPSHOT_CONFLICT}, and only an unlinked row is written under the
     * {@code definition_set_id IS NULL} compare-and-set; a zero-row effect reloads once to tell a concurrent identical link
     * (counted idempotently) from a genuine conflict, and no case reports a zero-row effect as success.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayOpenApiSnapshotRepository.linkAllToDefinitionSet(snapshotIds,
     * definitionSetId)}。
     * @param snapshotIds 参数 不可变快照标识；parameter immutable snapshot identifiers。
     * @param definitionSetId 参数 聚合Definition Set标识；parameter aggregate Definition Set identifier。
     * @return 返回 linkAll 的处理结果；returns the number of snapshots linked or already linked.
     */
    @Override
    public int linkAllToDefinitionSet(
            List<String> snapshotIds,
            String definitionSetId) {
        Objects.requireNonNull(snapshotIds, "snapshotIds");
        String setId = required(definitionSetId, "definitionSetId");
        Long definitionSetColumn = numeric(setId, "definitionSetId");
        Set<String> ids = snapshotIds.stream()
                .map(value -> required(value, "snapshotId"))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        int linked = 0;
        for (String snapshotId : ids) {
            Long idColumn = columnValue(snapshotId);
            Optional<GatewayOpenApiSnapshotRecordPO> current = idColumn == null
                    ? Optional.empty()
                    : snapshotPersistenceRepository.getOptById(idColumn);
            if (current.isEmpty()) {
                throw notFound(snapshotId);
            }
            if (definitionSetColumn.equals(current.get().getDefinitionSetId())) {
                linked++;
                continue;
            }
            if (current.get().getDefinitionSetId() != null) {
                throw conflict(snapshotId);
            }
            if (linkOnce(current.get(), definitionSetColumn)) {
                linked++;
                continue;
            }
            GatewayOpenApiSnapshotRecordPO raced = snapshotPersistenceRepository
                    .getOptById(current.get().getId())
                    .orElseThrow(() -> notFound(snapshotId));
            if (!definitionSetColumn.equals(raced.getDefinitionSetId())) {
                throw conflict(snapshotId);
            }
            linked++;
        }
        return linked;
    }

    /**
     * 中文说明：执行 find 操作；保留旧 SQL 的 {@code WHERE definition_set_id = ? ORDER BY openapi_group, id}；
     * 非十进制聚合标识在迁移后的数值列上无对应行，按空列表返回。
     * English summary: Executes the find operation; keeps the legacy {@code WHERE definition_set_id = ? ORDER BY
     * openapi_group, id}, and an aggregate identifier that is not decimal matches no row on the migrated numeric column,
     * so an empty list is returned.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayOpenApiSnapshotRepository.findByDefinitionSetId(definitionSetId)}。
     * @param definitionSetId 参数 聚合Definition Set标识；parameter aggregate Definition Set identifier。
     * @return 返回 find 的处理结果；returns the carriers ordered by Group and id.
     */
    @Override
    public List<GatewayOpenApiSnapshotBO> findByDefinitionSetId(
            String definitionSetId) {
        Long definitionSetColumn = columnValue(definitionSetId);
        if (definitionSetColumn == null) {
            return List.of();
        }
        return toCarriers(snapshotPersistenceRepository.list(
                boundPredicate(Wrappers.<GatewayOpenApiSnapshotRecordPO>lambdaQuery()
                        .eq(GatewayOpenApiSnapshotRecordPO::getDefinitionSetId, definitionSetColumn)
                        .orderByAsc(GatewayOpenApiSnapshotRecordPO::getOpenapiGroup)
                        .orderByAsc(GatewayOpenApiSnapshotRecordPO::getId))
        ));
    }

    /**
     * 中文说明：业务契约已存在时载入既有快照并逐字段复核不可变契约，等价于旧实现
     * {@code inserted == 0} 之后的 {@code findByContract} 重读分支；重读不到行或契约漂移按旧消息抛出。
     * English summary: Reloads the existing snapshot and re-verifies the immutable contract field by field, the equivalent
     * of the legacy {@code findByContract} reload after {@code inserted == 0}; a missing reload or a drifted contract is
     * raised with the original messages.
     * @param snapshot 参数 调用方尝试写入的载体；parameter the carrier the caller tried to insert.
     * @return 返回 既有快照载体；returns the pre-existing carrier.
     */
    private GatewayOpenApiSnapshotBO reuseExisting(
            GatewayOpenApiSnapshotBO snapshot) {
        GatewayOpenApiSnapshotBO existing = findByContract(
                snapshot.getApplicationId(),
                snapshot.getBuildId(),
                snapshot.getOpenapiGroup(),
                snapshot.getCanonicalSha256()
        ).orElseThrow(() -> new IllegalStateException(
                "YUHENG_OPENAPI_SNAPSHOT_CONFLICT: snapshot identity "
                        + snapshot.getId()
                        + " could not be resolved"
        ));
        if (!sameContract(existing, snapshot)) {
            throw new IllegalStateException(
                    "YUHENG_OPENAPI_SNAPSHOT_CONFLICT: immutable contract "
                            + snapshot.getApplicationId()
                            + "/"
                            + snapshot.getBuildId()
                            + "/"
                            + snapshot.getOpenapiGroup()
            );
        }
        return existing;
    }

    /**
     * 中文说明：对尚未链接的快照执行受守卫 CAS，等价于旧
     * {@code UPDATE ... SET definition_set_id = ? WHERE id = ? AND definition_set_id IS NULL}；沿用读到的技术
     * {@code id}/{@code version}，影响 0 行返回 {@code false}。
     * English summary: Performs the guarded compare-and-set for an unlinked snapshot, the equivalent of the legacy
     * {@code UPDATE ... SET definition_set_id = ? WHERE id = ? AND definition_set_id IS NULL}; it reuses the technical
     * {@code id} and {@code version} just read and returns {@code false} for a zero-row effect.
     * @param current 参数 读到的活跃快照行；parameter the active snapshot row that was read.
     * @param definitionSetColumn 参数 数值化后的聚合标识；parameter the numeric aggregate identifier.
     * @return 返回 是否写入一行；returns whether exactly one row was written.
     */
    private boolean linkOnce(
            GatewayOpenApiSnapshotRecordPO current,
            Long definitionSetColumn) {
        GatewayOpenApiSnapshotRecordPO link = GatewayOpenApiSnapshotRecordPO.builder()
                .id(current.getId())
                .version(current.getVersion())
                .definitionSetId(definitionSetColumn)
                .build();
        return snapshotPersistenceRepository.update(
                link,
                boundPredicate(Wrappers.<GatewayOpenApiSnapshotRecordPO>lambdaUpdate()
                        .eq(GatewayOpenApiSnapshotRecordPO::getId, current.getId())
                        .isNull(GatewayOpenApiSnapshotRecordPO::getDefinitionSetId))
        );
    }

    /**
     * 中文说明：把条件交付受守卫边界之前先成形一次：MyBatis-Plus 的 {@code eq/in} 只在 SQL 真正成形时
     * 才把取值写进 {@code paramNameValuePairs}，故此处先成形一次，让谓词在离开门面时参数已完全绑定；
     * 之后（包括 MyBatis 自己下发时）命中同一份片段缓存，参数与 SQL 都不再改变，列解析失败
     * （lambda 缓存缺失）也如实在门面这一层暴露，而不是留到语句下发时。
     * English summary: Forms a condition once before it is handed to the guarded boundary: MyBatis-Plus only moves the
     * values of {@code eq/in} into {@code paramNameValuePairs} while the SQL is being formed, so forming it here
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
     * 中文说明：把行模型列表逐个投影为业务载体，并继续经过 {@code GatewayOpenApiSnapshotBO.validated(...)}
     * 复核旧紧凑构造器的不变量，避免任何一行绕过装载边界。
     * English summary: Projects every row onto the business carrier while still routing it through
     * {@code GatewayOpenApiSnapshotBO.validated(...)} so the legacy compact-constructor invariants cannot be skipped by any
     * row.
     * @param rows 参数 行模型列表；parameter the row models.
     * @return 返回 业务载体列表；returns the business carriers.
     */
    private List<GatewayOpenApiSnapshotBO> toCarriers(
            List<GatewayOpenApiSnapshotRecordPO> rows) {
        return rows.stream().map(this::toCarrier).toList();
    }

    /**
     * 中文说明：把单行模型投影为业务载体并复核旧构造器不变量（列映射只由转换器负责）。
     * English summary: Projects one row onto the business carrier and re-checks the legacy constructor invariants; column
     * mapping stays exclusively in the converter.
     * @param row 参数 行模型；parameter the row model.
     * @return 返回 业务载体；returns the business carrier.
     */
    private GatewayOpenApiSnapshotBO toCarrier(GatewayOpenApiSnapshotRecordPO row) {
        return GatewayOpenApiSnapshotBO.validated(
                snapshotPersistenceConverter.toBusiness(row)
        );
    }

    /**
     * 中文说明：逐字段比较不可变契约，与原实现的 {@code sameContract} 完全一致（仅比较文档内容与校验结论，
     * 不比较标识与抓取时间）。
     * English summary: Compares the immutable contract field by field exactly as the original {@code sameContract} did,
     * covering document content and validation outcome but not the identifiers or fetch timestamps.
     * @param left 参数 既有载体；parameter the stored carrier.
     * @param right 参数 调用方载体；parameter the submitted carrier.
     * @return 返回 契约是否一致；returns whether both describe the same contract.
     */
    private boolean sameContract(
            GatewayOpenApiSnapshotBO left,
            GatewayOpenApiSnapshotBO right) {
        return left.getArtifactVersion().equals(right.getArtifactVersion())
                && left.getOpenapiVersion().equals(right.getOpenapiVersion())
                && left.getDocumentJson().equals(right.getDocumentJson())
                && left.getValidationStatus().equals(right.getValidationStatus())
                && left.getValidationMessages().equals(
                right.getValidationMessages()
        )
                && left.getOperationCount() == right.getOperationCount()
                && left.getSchemaCount() == right.getSchemaCount();
    }

    /**
     * 中文说明：构造与原实现一致的快照未找到异常。
     * English summary: Builds the snapshot-not-found exception with exactly the original message.
     * @param snapshotId 参数 快照Id；parameter snapshot identifier.
     * @return 返回 异常；returns the exception.
     */
    private static IllegalStateException notFound(String snapshotId) {
        return new IllegalStateException(
                "YUHENG_OPENAPI_SNAPSHOT_NOT_FOUND: " + snapshotId
        );
    }

    /**
     * 中文说明：构造与原实现一致的 Definition Set 冲突异常。
     * English summary: Builds the Definition Set conflict exception with exactly the original message.
     * @param snapshotId 参数 快照Id；parameter snapshot identifier.
     * @return 返回 异常；returns the exception.
     */
    private static IllegalStateException conflict(String snapshotId) {
        return new IllegalStateException(
                "YUHENG_OPENAPI_SNAPSHOT_CONFLICT: snapshot "
                        + snapshotId
                        + " already links to another definition set"
        );
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
     * 中文说明：把必填的不透明数值标识转换为写入列值；迁移后 {@code definition_set_id} 是数值列，无法表示的标识按
     * 调用方契约错误抛出，而不是写出 {@code NULL} 冒充链接成功。
     * English summary: Converts a required opaque identifier into the written column value; {@code definition_set_id} is a
     * numeric column after migration, so an unrepresentable identifier is raised as a caller contract error instead of
     * writing {@code NULL} and pretending the link succeeded.
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
