package top.egon.cola.component.yuheng.admin.group.repository.impl;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.group.converter.GatewayGroupPersistenceConverter;
import top.egon.cola.component.yuheng.admin.group.domain.bo.GatewayGroupBO;
import top.egon.cola.component.yuheng.admin.group.domain.po.GatewayGroupRecordPO;
import top.egon.cola.component.yuheng.admin.group.repository.GatewayGroupRepository;
import top.egon.cola.component.yuheng.admin.group.repository.mp.GatewayGroupPersistenceRepository;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminRevisionConflictException;

import java.util.List;
import java.util.Optional;

/**
 * 中文说明：{@code MpGatewayGroupRepository} 是 {@code gateway_group} 的 MyBatis-Plus 门面存储，取代旧的托管实体
 * （Spring Data/JPA）读写：列表只返回活跃行并按迁移后的 {@code create_time} 倒序，
 * {@code env + namespace} 用于划定作用域内的分组集合，保存则在同事务内读取当前行的技术 {@code id}/{@code version}
 * 后按业务 {@code revision} 做 CAS 并递增，再把权威 {@code revision} 与审计时间回写入参载体；
 * 列与类型映射只经 MapStruct 转换器完成，公开端口不泄漏 {@code GatewayGroupRecordPO}。
 * English summary: {@code MpGatewayGroupRepository} is the MyBatis-Plus facade store for {@code gateway_group} that
 * replaces the former managed-entity (Spring Data/JPA) reads and writes: listings return active rows only ordered by the
 * migrated {@code create_time} column, {@code env + namespace} bound the groups of one scope, and a save loads the current
 * row's technical {@code id}/{@code version} in the same transaction, compares-and-sets on the business
 * {@code revision}, increments it and syncs the authoritative revision plus audit timestamps back into the given carrier;
 * column and type mapping happens only in the MapStruct converter and the public port never leaks a
 * {@code GatewayGroupRecordPO}.
 *
 * 用法 / Usage: 通过业务端口 {@code GatewayGroupRepository} 由 Spring 容器注入，写入组合在调用方
 * {@code gatewayTransactionManager} 的同一事务内；影响 0 行或业务 {@code revision} 不匹配一律按
 * {@code GatewayAdminRevisionConflictException} 如实抛出，绝不伪造成功。/ Use it through the {@code GatewayGroupRepository}
 * port with writes composed inside the caller's {@code gatewayTransactionManager} transaction; a zero-row effect or a
 * business {@code revision} mismatch always surfaces as a {@code GatewayAdminRevisionConflictException} instead of a fake
 * success.
 */
@Slf4j
@Repository("mpGatewayGroupRepository")
@RequiredArgsConstructor
@Validated
public class MpGatewayGroupRepository implements GatewayGroupRepository {

    @Qualifier("gatewayGroupPersistenceRepository")
    private final GatewayGroupPersistenceRepository groupPersistenceRepository;

    @Qualifier("gatewayGroupPersistenceConverter")
    private final GatewayGroupPersistenceConverter groupPersistenceConverter;

    /**
     * 中文说明：执行 save 操作；新建写入业务 {@code revision} 0 并走受守卫插入（租户、审计与技术 {@code version} 由
     * 持久边界补齐），替换则先按 {@code id} 读取活跃行、要求载体携带的业务 {@code revision} 与库中一致，随后沿用原行的
     * 技术 {@code id}/{@code version} 以 {@code revision + 1} 做乐观锁 CAS；读取不到、CAS 失败或影响 0 行都按修订冲突抛出，
     * 并把权威 {@code revision} 与审计时间回写到入参载体后返回同一载体。
     * English summary: Executes the save operation; an insert stores business {@code revision} 0 through the guarded
     * insert (the boundary fills tenant, audit and the technical {@code version}), while a replace loads the active row by
     * {@code id}, requires the carrier's business {@code revision} to match the stored one and then performs an
     * optimistic-lock CAS with {@code revision + 1} reusing the original row's technical {@code id}/{@code version}; a
     * missing row, a failed compare-and-set or a zero-row effect raises a revision conflict, and the authoritative
     * revision plus audit timestamps are synced back into the given carrier before it is returned.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayGroupRepository.save(group)}。
     * @param group 参数 分组载体；parameter the group carrier.
     * @return 返回 权威分组载体；returns the authoritative group carrier.
     */
    @Override
    public GatewayGroupBO save(GatewayGroupBO group) {
        GatewayGroupRecordPO candidate = groupPersistenceConverter.newRow(group);
        Optional<GatewayGroupRecordPO> current = candidate.getId() == null
                ? Optional.empty()
                : groupPersistenceRepository.getOptById(candidate.getId());
        if (current.isEmpty()) {
            candidate.setRevision(0L);
            if (!groupPersistenceRepository.save(candidate)) {
                throw new GatewayAdminRevisionConflictException(group.getRevision());
            }
            return authoritative(group, candidate);
        }
        GatewayGroupRecordPO persisted = current.get();
        long storedRevision = persisted.getRevision() == null ? 0L : persisted.getRevision();
        if (group.getRevision() != storedRevision) {
            throw new GatewayAdminRevisionConflictException(storedRevision);
        }
        candidate.setId(persisted.getId());
        candidate.setVersion(persisted.getVersion());
        candidate.setRevision(storedRevision + 1);
        if (!groupPersistenceRepository.updateById(candidate)) {
            throw new GatewayAdminRevisionConflictException(storedRevision);
        }
        return authoritative(group, candidate);
    }

    /**
     * 中文说明：执行 findAllByDeletedFalseOrderByCreatedAtDesc 操作；由受守卫读取只返回活跃行并按迁移后的
     * {@code create_time} 倒序排列，追加 {@code id} 倒序以稳定并列时间戳的次序（旧派生查询未定义并列次序）。
     * English summary: Executes the find all by deleted false order by created at desc operation; the guarded read
     * returns active rows only, ordered by the migrated {@code create_time} column descending with a descending
     * {@code id} tie-break that stabilises equal timestamps (the legacy derived query left ties unspecified).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayGroupRepository.findAllByDeletedFalseOrderByCreatedAtDesc()}。
     * @return 返回 findAllByDeletedFalseOrderByCreatedAtDesc 的处理结果；returns the result of the operation.
     */
    @Override
    public List<GatewayGroupBO> findAllByDeletedFalseOrderByCreatedAtDesc() {
        return groupPersistenceConverter.toBusinessList(
                groupPersistenceRepository.list(
                        Wrappers.<GatewayGroupRecordPO>lambdaQuery()
                                .orderByDesc(GatewayGroupRecordPO::getCreateTime)
                                .orderByDesc(GatewayGroupRecordPO::getId)
                )
        );
    }

    /**
     * 中文说明：执行 findAllByEnvAndNamespaceAndDeletedFalseOrderByCreatedAtDesc 操作；等价于旧派生查询的
     * {@code env = ? AND namespace = ?} 作用域过滤，活跃与租户谓词由持久边界追加，次序同样是 {@code create_time}
     * 倒序并以 {@code id} 倒序稳定并列。
     * English summary: Executes the find all by env and namespace and deleted false order by created at desc operation;
     * the equivalent of the legacy derived query's {@code env = ? AND namespace = ?} scope filter, with the active-row and
     * tenant predicates contributed by the persistence boundary and the same {@code create_time} descending order plus a
     * descending {@code id} tie-break.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code MpGatewayGroupRepository.findAllByEnvAndNamespaceAndDeletedFalseOrderByCreatedAtDesc(env, namespace)}。
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @return 返回 findAllByEnvAnd命名空间AndDeletedFalseOrderByCreatedAtDesc 的处理结果；returns the result of the operation.
     */
    @Override
    public List<GatewayGroupBO>
    findAllByEnvAndNamespaceAndDeletedFalseOrderByCreatedAtDesc(
            String env,
            String namespace) {
        return groupPersistenceConverter.toBusinessList(
                groupPersistenceRepository.list(
                        Wrappers.<GatewayGroupRecordPO>lambdaQuery()
                                .eq(GatewayGroupRecordPO::getEnv, env)
                                .eq(GatewayGroupRecordPO::getNamespace, namespace)
                                .orderByDesc(GatewayGroupRecordPO::getCreateTime)
                                .orderByDesc(GatewayGroupRecordPO::getId)
                )
        );
    }

    /**
     * 中文说明：执行 findByIdAndDeletedFalse 操作；走受守卫的按主键活跃读取（{@code selectActiveById} 内含
     * {@code deleted_at IS NULL}，并由边界限定当前租户），因此软删行与跨租户行一律视为不存在；
     * 非十进制的不透明 id 按不存在处理。
     * English summary: Executes the find by id and deleted false operation; it uses the guarded active read by primary
     * key ({@code selectActiveById} carries {@code deleted_at IS NULL} and the boundary pins the current tenant), so a
     * soft-deleted or foreign-tenant row counts as absent, and a non-decimal opaque id is treated as absent too.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayGroupRepository.findByIdAndDeletedFalse(id)}。
     * @param id 参数 id；parameter id。
     * @return 返回 findByIdAndDeletedFalse 的处理结果；returns the result of the operation.
     */
    @Override
    public Optional<GatewayGroupBO> findByIdAndDeletedFalse(String id) {
        Long idColumn = columnValue(id);
        if (idColumn == null) {
            return Optional.empty();
        }
        return groupPersistenceRepository.getOptById(idColumn)
                .map(groupPersistenceConverter::toBusiness);
    }

    /**
     * 中文说明：把仓储侧权威的技术与业务值回写到入参载体并返回同一对象，替代旧的托管实体脏检查；
     * 只回写 {@code revision} 与审计时间/操作者，业务列以载体为准。
     * English summary: Copies the repository-authoritative technical and business values back into the given carrier and
     * returns that same object, replacing the former managed-entity dirty checking; only {@code revision} and the audit
     * timestamps and principals are written back while the business columns stay as the carrier holds them.
     * @param carrier 参数 入参载体；parameter the incoming carrier.
     * @param row 参数 仓储侧行；parameter the repository-side row.
     * @return 返回 权威载体；returns the authoritative carrier.
     */
    private GatewayGroupBO authoritative(
            GatewayGroupBO carrier,
            GatewayGroupRecordPO row) {
        carrier.setRevision(row.getRevision() == null ? 0L : row.getRevision());
        carrier.setCreatedAt(row.getCreateTime());
        carrier.setCreatedBy(row.getCreateUserId());
        carrier.setUpdatedAt(row.getUpdateTime());
        carrier.setUpdatedBy(row.getUpdateUserId());
        carrier.setDeleted(row.getDeletedAt() != null);
        log.debug("gateway_group {} resolved to authoritative revision {}", row.getId(), row.getRevision());
        return carrier;
    }

    /**
     * 中文说明：把端口上的不透明数值标识转换为受守卫谓词使用的列值；空白、非十进制或非正值返回 {@code null}，
     * 调用方据此按“无匹配行”处理。
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
}
