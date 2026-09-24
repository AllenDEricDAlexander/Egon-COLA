package top.egon.cola.component.yuheng.admin.application.repository.impl;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.application.converter.GatewayApplicationPersistenceConverter;
import top.egon.cola.component.yuheng.admin.application.domain.bo.GatewayApplicationBO;
import top.egon.cola.component.yuheng.admin.application.domain.po.GatewayApplicationRecordPO;
import top.egon.cola.component.yuheng.admin.application.repository.GatewayApplicationRepository;
import top.egon.cola.component.yuheng.admin.application.repository.mp.GatewayApplicationPersistenceRepository;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminRevisionConflictException;

import java.util.List;
import java.util.Optional;

/**
 * 中文说明：{@code MpGatewayApplicationRepository} 是 {@code gateway_application} 的 MyBatis-Plus 门面存储，
 * 取代旧的托管实体（Spring Data/JPA）读写：查询按 {@code create_time} 倒序只返回活跃行，业务唯一键
 * {@code biz_code + application_code + env} 在活跃集合内定位至多一行，保存则在同事务内读取当前行的技术
 * {@code id}/{@code version} 后按业务 {@code revision} 做 CAS 并递增，再把权威 {@code revision} 与审计时间回写入参载体；
 * 列与类型映射只经 MapStruct 转换器完成，公开端口不泄漏 {@code GatewayApplicationRecordPO}。
 * English summary: {@code MpGatewayApplicationRepository} is the MyBatis-Plus facade store for
 * {@code gateway_application} that replaces the former managed-entity (Spring Data/JPA) reads and writes: queries return
 * active rows only, ordered by {@code create_time} descending, the business unique key
 * {@code biz_code + application_code + env} locates at most one active row, and a save loads the current row's technical
 * {@code id}/{@code version} in the same transaction, compares-and-sets on the business {@code revision}, increments it
 * and syncs the authoritative revision plus audit timestamps back into the given carrier; column and type mapping happens
 * only in the MapStruct converter and the public port never leaks a {@code GatewayApplicationRecordPO}.
 *
 * 用法 / Usage: 通过业务端口 {@code GatewayApplicationRepository} 由 Spring 容器注入，写入组合在调用方
 * {@code gatewayTransactionManager} 的同一事务内；影响 0 行或业务 {@code revision} 不匹配一律按
 * {@code GatewayAdminRevisionConflictException} 如实抛出，绝不伪造成功。/ Use it through the
 * {@code GatewayApplicationRepository} port with writes composed inside the caller's {@code gatewayTransactionManager}
 * transaction; a zero-row effect or a business {@code revision} mismatch always surfaces as a
 * {@code GatewayAdminRevisionConflictException} instead of a fake success.
 */
@Slf4j
@Repository("mpGatewayApplicationRepository")
@RequiredArgsConstructor
@Validated
public class MpGatewayApplicationRepository implements GatewayApplicationRepository {

    @Qualifier("gatewayApplicationPersistenceRepository")
    private final GatewayApplicationPersistenceRepository applicationPersistenceRepository;

    @Qualifier("gatewayApplicationPersistenceConverter")
    private final GatewayApplicationPersistenceConverter applicationPersistenceConverter;

    /**
     * 中文说明：执行 save 操作；新建写入业务 {@code revision} 0 并走受守卫插入（租户、审计与技术 {@code version} 由
     * 持久边界补齐），替换则先按 {@code id} 读取活跃行、要求载体携带的业务 {@code revision} 与库中一致，随后沿用原行的
     * 技术 {@code id}/{@code version} 以 {@code revision + 1} 做乐观锁 CAS；读取不到、CAS 失败或影响 0 行都按修订冲突抛出，
     * 并把权威 {@code revision} 与审计时间回写到入参载体后返回同一载体。
     * English summary: Executes the save operation; an insert stores business {@code revision} 0 through the guarded
     * insert (the boundary fills tenant, audit and the technical {@code version}), while a replace loads the active row
     * by {@code id}, requires the carrier's business {@code revision} to match the stored one and then performs an
     * optimistic-lock CAS with {@code revision + 1} reusing the original row's technical {@code id}/{@code version}; a
     * missing row, a failed compare-and-set or a zero-row effect raises a revision conflict, and the authoritative
     * revision plus audit timestamps are synced back into the given carrier before it is returned.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayApplicationRepository.save(application)}。
     * @param application 参数 应用载体；parameter the application carrier.
     * @return 返回 权威应用载体；returns the authoritative application carrier.
     */
    @Override
    public GatewayApplicationBO save(GatewayApplicationBO application) {
        GatewayApplicationRecordPO candidate = applicationPersistenceConverter.newRow(application);
        Optional<GatewayApplicationRecordPO> current = candidate.getId() == null
                ? Optional.empty()
                : applicationPersistenceRepository.getOptById(candidate.getId());
        if (current.isEmpty()) {
            candidate.setRevision(0L);
            try {
                if (!applicationPersistenceRepository.save(candidate)) {
                    throw new GatewayAdminRevisionConflictException(application.getRevision());
                }
            } catch (DuplicateKeyException conflict) {
                return businessIntent(candidate, conflict);
            }
            return authoritative(application, candidate);
        }
        GatewayApplicationRecordPO persisted = current.get();
        long storedRevision = persisted.getRevision() == null ? 0L : persisted.getRevision();
        if (application.getRevision() != storedRevision) {
            throw new GatewayAdminRevisionConflictException(storedRevision);
        }
        candidate.setId(persisted.getId());
        candidate.setVersion(persisted.getVersion());
        candidate.setRevision(storedRevision + 1);
        if (!applicationPersistenceRepository.updateById(candidate)) {
            throw new GatewayAdminRevisionConflictException(storedRevision);
        }
        return authoritative(application, candidate);
    }

    /**
     * 中文说明：执行 findAllByDeletedFalseOrderByCreatedAtDesc 操作；由受守卫读取只返回活跃行并按迁移后的
     * {@code create_time} 倒序排列，追加 {@code id} 倒序以稳定并列时间戳的次序（旧派生查询未定义并列次序）。
     * English summary: Executes the find all by deleted false order by created at desc operation; the guarded read
     * returns active rows only, ordered by the migrated {@code create_time} column descending with a descending
     * {@code id} tie-break that stabilises equal timestamps (the legacy derived query left ties unspecified).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayApplicationRepository.findAllByDeletedFalseOrderByCreatedAtDesc()}。
     * @return 返回 findAllByDeletedFalseOrderByCreatedAtDesc 的处理结果；returns the result of the operation.
     */
    @Override
    public List<GatewayApplicationBO> findAllByDeletedFalseOrderByCreatedAtDesc() {
        return applicationPersistenceConverter.toBusinessList(
                applicationPersistenceRepository.list(
                        Wrappers.<GatewayApplicationRecordPO>lambdaQuery()
                                .orderByDesc(GatewayApplicationRecordPO::getCreateTime)
                                .orderByDesc(GatewayApplicationRecordPO::getId)
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
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayApplicationRepository.findByIdAndDeletedFalse(id)}。
     * @param id 参数 id；parameter id。
     * @return 返回 findByIdAndDeletedFalse 的处理结果；returns the result of the operation.
     */
    @Override
    public Optional<GatewayApplicationBO> findByIdAndDeletedFalse(String id) {
        Long idColumn = columnValue(id);
        if (idColumn == null) {
            return Optional.empty();
        }
        return applicationPersistenceRepository.getOptById(idColumn)
                .map(applicationPersistenceConverter::toBusiness);
    }

    /**
     * 中文说明：执行 findByBizCodeAndApplicationCodeAndEnvAndDeletedFalse 操作；{@code biz_code}、{@code application_code}
     * 与 {@code env} 三列构成业务唯一键，配合边界追加的活跃与租户谓词，在活跃集合内至多命中一行。
     * English summary: Executes the find by biz code and application code and env and deleted false operation; the
     * {@code biz_code}, {@code application_code} and {@code env} columns form the business unique key which, together
     * with the boundary's active-row and tenant predicates, matches at most one row.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code MpGatewayApplicationRepository.findByBizCodeAndApplicationCodeAndEnvAndDeletedFalse(bizCode, applicationCode, env)}。
     * @param bizCode 参数 bizCode；parameter biz code。
     * @param applicationCode 参数 applicationCode；parameter application code。
     * @param env 参数 env；parameter env。
     * @return 返回 findByBizCodeAndApplicationCodeAndEnvAndDeletedFalse 的处理结果；returns the result of the operation.
     */
    @Override
    public Optional<GatewayApplicationBO>
    findByBizCodeAndApplicationCodeAndEnvAndDeletedFalse(
            String bizCode,
            String applicationCode,
            String env) {
        return applicationPersistenceRepository.getOneOpt(
                Wrappers.<GatewayApplicationRecordPO>lambdaQuery()
                        .eq(GatewayApplicationRecordPO::getBizCode, bizCode)
                        .eq(GatewayApplicationRecordPO::getApplicationCode, applicationCode)
                        .eq(GatewayApplicationRecordPO::getEnv, env)
        ).map(applicationPersistenceConverter::toBusiness);
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
    private GatewayApplicationBO authoritative(
            GatewayApplicationBO carrier,
            GatewayApplicationRecordPO row) {
        carrier.setRevision(row.getRevision() == null ? 0L : row.getRevision());
        carrier.setCreatedAt(row.getCreateTime());
        carrier.setCreatedBy(row.getCreateUserId());
        carrier.setUpdatedAt(row.getUpdateTime());
        carrier.setUpdatedBy(row.getUpdateUserId());
        carrier.setDeleted(row.getDeletedAt() != null);
        log.debug("gateway_application {} resolved to authoritative revision {}", row.getId(), row.getRevision());
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

    /**
     * 中文说明：业务唯一键（biz_code + application_code + env）冲突时重新载入既有的活跃行，使调用方沿用已成立的业务意图；若并发方已移除该行则抛出原始冲突。
     * English summary: Reloads the pre-existing active row when the business unique key (biz_code + application_code + env) conflicts, so the caller continues with the business intent that already holds; if a concurrent writer removed it, the original conflict is rethrown.
     *
     * 用法 / Usage: 仅在 {@code MpGatewayApplicationRepository.save(...)} 的插入分支捕获 {@code DuplicateKeyException} 时调用。/ Called only from the insert branch of {@code save} after a {@code DuplicateKeyException} is caught.
     * @param candidate 参数 准备插入的行模型；parameter the row candidate that conflicted.
     * @param conflict 参数 原始唯一键冲突；parameter the original unique-key conflict.
     * @return 返回既有行的业务载体；returns the business carrier of the pre-existing row.
     */
    private GatewayApplicationBO businessIntent(
            GatewayApplicationRecordPO candidate,
            DuplicateKeyException conflict
    ) {
        return applicationPersistenceRepository.getOneOpt(
                        Wrappers.<GatewayApplicationRecordPO>lambdaQuery()
                                .eq(GatewayApplicationRecordPO::getBizCode, candidate.getBizCode())
                                .eq(GatewayApplicationRecordPO::getApplicationCode, candidate.getApplicationCode())
                                .eq(GatewayApplicationRecordPO::getEnv, candidate.getEnv())
                )
                .map(applicationPersistenceConverter::toBusiness)
                .orElseThrow(() -> conflict);
    }
}
