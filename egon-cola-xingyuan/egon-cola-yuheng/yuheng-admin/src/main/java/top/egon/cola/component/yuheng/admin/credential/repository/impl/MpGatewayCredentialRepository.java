package top.egon.cola.component.yuheng.admin.credential.repository.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.credential.converter.GatewayCredentialPersistenceConverter;
import top.egon.cola.component.yuheng.admin.credential.domain.bo.GatewayCredentialBO;
import top.egon.cola.component.yuheng.admin.credential.domain.po.GatewayCredentialRecordPO;
import top.egon.cola.component.yuheng.admin.credential.repository.GatewayCredentialRepository;
import top.egon.cola.component.yuheng.admin.credential.repository.mp.GatewayCredentialPersistenceRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 中文说明：{@code MpGatewayCredentialRepository} 是 MyBatis-Plus 门面存储，逐方法替换旧手写 JDBC 的
 * {@code JdbcGatewayCredentialRepository}（{@code gateway_application_credential} 表）：读取保留原 SQL 的
 * {@code application_id}/{@code id OR access_key} 谓词与 {@code created_at DESC, id DESC} 次序，
 * 写入保留原 {@code overlap}/{@code revoke} 的状态迁移语义；列与类型映射只经 MapStruct 转换器完成，
 * 公开端口不泄漏 {@code GatewayCredentialRecordPO}。
 * English summary: {@code MpGatewayCredentialRepository} is the MyBatis-Plus facade store that replaces the legacy
 * hand-written {@code JdbcGatewayCredentialRepository} for {@code gateway_application_credential} method by method: the
 * reads keep the original {@code application_id} and {@code id OR access_key} predicates plus the
 * {@code created_at DESC, id DESC} ordering, while the writes keep the original {@code overlap}/{@code revoke} status
 * transitions; column and type mapping happens only in the MapStruct converter and the public port never leaks a
 * {@code GatewayCredentialRecordPO}.
 *
 * 用法 / Usage: 通过业务端口 {@code GatewayCredentialRepository} 由 Spring 容器注入；所有读写走该表的受守卫
 * {@code EgonColaRepository} 边界，因此天然带同租户过滤、仅活跃行（{@code deleted_at IS NULL}）与乐观锁 CAS，
 * 旧实现里被忽略的 0 行写入现在按冲突如实抛出，绝不伪造成功。/ Use it through the {@code GatewayCredentialRepository}
 * port; every statement goes through this table's guarded {@code EgonColaRepository} boundary, so tenant filtering,
 * active-only rows ({@code deleted_at IS NULL}) and optimistic-lock CAS are structural, and a zero-row write that the
 * legacy implementation silently ignored now surfaces as a conflict instead of a fake success.
 */
@Slf4j
@Repository("mpGatewayCredentialRepository")
@RequiredArgsConstructor
@Validated
public class MpGatewayCredentialRepository implements GatewayCredentialRepository {

    /** 中文说明：凭证可继续参与签发的活跃状态；对应旧 SQL 的 {@code status = 'ACTIVE'}。 English summary: the active credential status of the legacy {@code status = 'ACTIVE'} predicate. */
    private static final String STATUS_ACTIVE = "ACTIVE";

    /** 中文说明：轮换中的过渡状态；对应旧 SQL 的 {@code status = 'ROTATING'} 与 {@code overlap} 写入值。 English summary: the rotating transition status written by {@code overlap} and matched by the legacy status guard. */
    private static final String STATUS_ROTATING = "ROTATING";

    /** 中文说明：吊销后的终态；对应旧 SQL 的 {@code status = 'REVOKED'}。 English summary: the revoked terminal status written by {@code revoke}. */
    private static final String STATUS_REVOKED = "REVOKED";

    @Qualifier("gatewayCredentialPersistenceRepository")
    private final GatewayCredentialPersistenceRepository credentialPersistenceRepository;

    @Qualifier("gatewayCredentialPersistenceConverter")
    private final GatewayCredentialPersistenceConverter credentialPersistenceConverter;

    /**
     * 中文说明：执行 insert 操作；等价于旧 {@code INSERT INTO gateway_application_credential(...)}，先由转换器把业务载体
     * 渲染为待插入行，再走受守卫插入（{@code secret_reference} 保持 {@code NULL}，租户、审计列与技术 version 由持久边界补齐）；
     * 影响 0 行时按写入冲突如实抛出，不返回假成功。
     * English summary: Executes the insert operation; the equivalent of the legacy
     * {@code INSERT INTO gateway_application_credential(...)} — the converter renders the business carrier into the row
     * to insert and the guarded insert then keeps {@code secret_reference} {@code NULL} while the persistence boundary
     * fills tenant, audit columns and the technical version; a zero-row insert surfaces as a write conflict instead of a
     * fake success.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayCredentialRepository.insert(credential)}。调用方应在
     * {@code gatewayTransactionManager} 事务内携带可信租户上下文调用；/ Call it inside the caller's
     * {@code gatewayTransactionManager} transaction with a trusted tenant context, and handle the thrown conflict.
     * @param credential 参数 凭证；parameter credential。
     */
    @Override
    public void insert(GatewayCredentialBO credential) {
        GatewayCredentialRecordPO candidate = credentialPersistenceConverter.newRow(credential);
        if (!credentialPersistenceRepository.save(candidate)) {
            throw new IllegalStateException("YUHENG_ADMIN_CREDENTIAL_INSERT_CONFLICT");
        }
    }

    /**
     * 中文说明：执行 find 操作；保留旧 SQL 的 {@code WHERE application_id = ? AND (id = ? OR access_key = ?)} 语义，
     * 并按旧 {@code queryAll(...).stream().findFirst()} 取首行（重复键不抛异常）；不可解析为数值列的 opaque 标识按无行处理。
     * English summary: Executes the find operation; keeps the legacy {@code WHERE application_id = ? AND
     * (id = ? OR access_key = ?)} shape and, like the old {@code queryAll(...).stream().findFirst()}, returns the first
     * row instead of failing on duplicates; an opaque identifier that cannot be read as a numeric column matches nothing.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayCredentialRepository.find(applicationId, keyId)}。
     * @param applicationId 参数 applicationId；parameter application id。
     * @param keyId 参数 键Id；parameter key id。
     * @return 返回 find 的处理结果；returns the result of the operation.
     */
    @Override
    public Optional<GatewayCredentialBO> find(
            String applicationId,
            String keyId) {
        Long applicationColumn = columnValue(applicationId);
        if (applicationColumn == null) {
            return Optional.empty();
        }
        Long keyColumn = columnValue(keyId);
        LambdaQueryWrapper<GatewayCredentialRecordPO> query =
                Wrappers.<GatewayCredentialRecordPO>lambdaQuery()
                        .eq(GatewayCredentialRecordPO::getApplicationId, applicationColumn);
        if (keyColumn == null) {
            query.eq(GatewayCredentialRecordPO::getAccessKey, keyId);
        } else {
            query.and(matched -> matched
                    .eq(GatewayCredentialRecordPO::getId, keyColumn)
                    .or()
                    .eq(GatewayCredentialRecordPO::getAccessKey, keyId));
        }
        return credentialPersistenceRepository.list(query).stream()
                .findFirst()
                .map(credentialPersistenceConverter::toBusiness);
    }

    /**
     * 中文说明：执行 findByAccess键 操作；等价于旧 {@code WHERE access_key = ?}，取首行语义与原实现一致；
     * 受守卫读取额外限定当前租户与活跃行，旧实现跨租户口径由持久边界的租户过滤取代。
     * English summary: Executes the find by access key operation; the equivalent of the legacy
     * {@code WHERE access_key = ?} with the same first-row semantics, while the guarded read additionally pins the
     * current tenant and active rows, replacing the cross-tenant reach of the legacy statement.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayCredentialRepository.findByAccessKey(accessKey)}。
     * @param accessKey 参数 access键；parameter access key。
     * @return 返回 findByAccess键 的处理结果；returns the result of the operation.
     */
    @Override
    public Optional<GatewayCredentialBO> findByAccessKey(String accessKey) {
        return credentialPersistenceRepository.list(
                Wrappers.<GatewayCredentialRecordPO>lambdaQuery()
                        .eq(GatewayCredentialRecordPO::getAccessKey, accessKey)
        ).stream().findFirst().map(credentialPersistenceConverter::toBusiness);
    }

    /**
     * 中文说明：执行 list 操作；等价于旧 {@code WHERE application_id = ? ORDER BY created_at DESC, id DESC}，
     * 迁移后按 {@code create_time} 倒序并以 {@code id} 倒序稳定次序；不可解析的 applicationId 返回空列表。
     * English summary: Executes the list operation; the equivalent of the legacy
     * {@code WHERE application_id = ? ORDER BY created_at DESC, id DESC}, reading the migrated {@code create_time}
     * column descending with a stable descending {@code id} tie-break, and an unparsable application id yields an empty
     * list.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayCredentialRepository.list(applicationId)}。
     * @param applicationId 参数 applicationId；parameter application id。
     * @return 返回 list 的处理结果；returns the result of the operation.
     */
    @Override
    public List<GatewayCredentialBO> list(String applicationId) {
        Long applicationColumn = columnValue(applicationId);
        if (applicationColumn == null) {
            return List.of();
        }
        return credentialPersistenceRepository.list(
                Wrappers.<GatewayCredentialRecordPO>lambdaQuery()
                        .eq(GatewayCredentialRecordPO::getApplicationId, applicationColumn)
                        .orderByDesc(GatewayCredentialRecordPO::getCreateTime)
                        .orderByDesc(GatewayCredentialRecordPO::getId)
        ).stream().map(credentialPersistenceConverter::toBusiness).toList();
    }

    /**
     * 中文说明：执行 overlap 操作；等价于旧 {@code UPDATE ... SET status = 'ROTATING', valid_until = ?, updated_at = ?
     * WHERE id = ? AND status IN ('ACTIVE', 'ROTATING')}。旧实现忽略影响行数，因此对已吊销或不属于当前租户/已软删的凭证会
     * 静默假成功；门面先按 id 与状态集合读取活跃行、再以乐观锁 CAS 写回，读取为空或写入 0 行都按状态冲突抛出。
     * {@code updated_at} 由持久边界的时钟盖章，入参 {@code now} 仍作为业务列 {@code valid_until} 落库。
     * English summary: Executes the overlap operation; the equivalent of the legacy
     * {@code UPDATE ... SET status = 'ROTATING', valid_until = ?, updated_at = ? WHERE id = ? AND
     * status IN ('ACTIVE', 'ROTATING')}. The legacy statement ignored the affected row count, so a revoked, foreign
     * tenant or soft-deleted credential produced a silent fake success; the facade loads the active row by id and the
     * status set first, then writes under optimistic-lock CAS, and both an empty read and a zero-row update raise a
     * state conflict. {@code updated_at} is stamped by the persistence boundary's clock while the {@code now} argument
     * still lands in the business column {@code valid_until}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayCredentialRepository.overlap(id, validUntil, now)}。
     * @param id 参数 id；parameter id。
     * @param validUntil 参数 validUntil；parameter valid until。
     * @param now 参数 now；parameter now。
     */
    @Override
    public void overlap(String id, Instant validUntil, Instant now) {
        Long idColumn = columnValue(id);
        Optional<GatewayCredentialRecordPO> current = idColumn == null
                ? Optional.empty()
                : credentialPersistenceRepository.getOneOpt(
                        Wrappers.<GatewayCredentialRecordPO>lambdaQuery()
                                .eq(GatewayCredentialRecordPO::getId, idColumn)
                                .in(GatewayCredentialRecordPO::getStatus,
                                        STATUS_ACTIVE, STATUS_ROTATING)
                );
        if (current.isEmpty()) {
            throw new IllegalStateException("YUHENG_ADMIN_CREDENTIAL_OVERLAP_CONFLICT");
        }
        GatewayCredentialRecordPO rotating = current.get();
        rotating.setStatus(STATUS_ROTATING);
        rotating.setValidUntil(validUntil);
        rotating.setUpdateTime(now);
        if (!credentialPersistenceRepository.updateById(rotating)) {
            throw new IllegalStateException("YUHENG_ADMIN_CREDENTIAL_OVERLAP_CONFLICT");
        }
    }

    /**
     * 中文说明：执行 revoke 操作；等价于旧 {@code UPDATE ... SET status = 'REVOKED', valid_until = ?, updated_at = ?
     * WHERE id = ?}（旧实现无状态谓词，因此任意状态的活跃凭证都可被吊销）；门面同样不加状态谓词，但行缺失、跨租户、
     * 已软删或写入 0 行都按状态冲突抛出而不再假成功。
     * English summary: Executes the revoke operation; the equivalent of the legacy
     * {@code UPDATE ... SET status = 'REVOKED', valid_until = ?, updated_at = ? WHERE id = ?} — the legacy statement
     * carried no status predicate, so any active credential in any state could be revoked. The facade keeps that
     * predicate-free state transition, but a missing row, another tenant, a soft-deleted row or a zero-row update now
     * raises a state conflict instead of reporting success.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayCredentialRepository.revoke(id, now)}。
     * @param id 参数 id；parameter id。
     * @param now 参数 now；parameter now。
     */
    @Override
    public void revoke(String id, Instant now) {
        Long idColumn = columnValue(id);
        Optional<GatewayCredentialRecordPO> current = idColumn == null
                ? Optional.empty()
                : credentialPersistenceRepository.getOneOpt(
                        Wrappers.<GatewayCredentialRecordPO>lambdaQuery()
                                .eq(GatewayCredentialRecordPO::getId, idColumn)
                );
        if (current.isEmpty()) {
            throw new IllegalStateException("YUHENG_ADMIN_CREDENTIAL_REVOKE_CONFLICT");
        }
        GatewayCredentialRecordPO revoked = current.get();
        revoked.setStatus(STATUS_REVOKED);
        revoked.setValidUntil(now);
        revoked.setUpdateTime(now);
        if (!credentialPersistenceRepository.updateById(revoked)) {
            throw new IllegalStateException("YUHENG_ADMIN_CREDENTIAL_REVOKE_CONFLICT");
        }
    }

    /**
     * 中文说明：把端口上的不透明数值标识转换为受守卫谓词使用的列值；空白或非十进制输入返回 {@code null}，
     * 调用方据此按“无匹配行”处理，避免把不可比较的字面量下推到数值列。
     * English summary: Converts an opaque numeric identifier from the port into the column value the guarded predicate
     * needs; blank or non-decimal input yields {@code null} so the caller treats it as “no matching row” instead of
     * pushing an incomparable literal onto a numeric column.
     * @param opaqueId 参数 不透明标识；parameter opaque identifier。
     * @return 返回 列值或 null；returns the column value or null.
     */
    private static Long columnValue(String opaqueId) {
        if (opaqueId == null || opaqueId.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(opaqueId.trim());
        } catch (NumberFormatException invalidOpaqueId) {
            return null;
        }
    }
}
