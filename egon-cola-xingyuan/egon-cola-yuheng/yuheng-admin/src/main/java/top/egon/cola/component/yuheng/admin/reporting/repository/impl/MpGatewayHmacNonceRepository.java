package top.egon.cola.component.yuheng.admin.reporting.repository.impl;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.component.yuheng.admin.reporting.domain.po.GatewayHmacNoncePO;
import top.egon.cola.component.yuheng.admin.reporting.repository.GatewayHmacNonceRepository;
import top.egon.cola.component.yuheng.admin.reporting.repository.mp.GatewayHmacNoncePersistenceRepository;

import java.time.Instant;

/**
 * 中文说明：{@code MpGatewayHmacNonceRepository} 是 HMAC 防重放 nonce 端口的 MyBatis-Plus 门面实现，逐方法替换被删除的手写 JDBC 仓储：
 * {@code claim} 由「单行 {@code INSERT} + 捕获 {@code DataIntegrityViolationException}」保持首登记为真、重放为假的原有判定，
 * {@code deleteExpired} 由「按 {@code expires_at < now} 的受守卫读取 + 逐行版本化软删」替换原裸 {@code DELETE}，
 * 且只有真正写成软删的行才计数，因此 0 行永远不会被报告为成功。本类不写裸 SQL、不引入 JdbcTemplate，也不新增异常类型。
 * English summary: {@code MpGatewayHmacNonceRepository} is the MyBatis-Plus facade implementing the HMAC replay-protection nonce port,
 * replacing the deleted hand-written JDBC repository method by method: {@code claim} keeps the original verdict — a first
 * registration claims {@code true} and a replay claims {@code false} — through a single-row insert plus the same
 * {@code DataIntegrityViolationException} capture, while {@code deleteExpired} turns the bare {@code DELETE} into a guarded
 * {@code expires_at < now} read followed by a versioned soft delete per row, counting only rows the guarded write actually landed so a
 * zero-row effect is never reported as success. It issues no raw SQL, adds no JdbcTemplate and introduces no new exception type.
 *
 * 用法 / Usage: 经端口 {@code GatewayHmacNonceRepository} 注入到 {@code GatewayReportHmacFilter} 与 {@code GatewayHmacNonceReaper}；
 * 主键、租户、审计与版本列由 {@code gateway_hmac_nonce} 的受守卫边界负责，重放唯一性仍由数据库上
 * {@code (access_key, nonce)} 的唯一约束强制。/ Inject it through the {@code GatewayHmacNonceRepository} port into
 * {@code GatewayReportHmacFilter} and {@code GatewayHmacNonceReaper}; the primary key, tenant, audit and version columns belong to the
 * guarded {@code gateway_hmac_nonce} boundary, while replay uniqueness stays enforced by the database
 * {@code (access_key, nonce)} constraint.
 */
@Slf4j
@Validated
@Repository("mpGatewayHmacNonceRepository")
@RequiredArgsConstructor
public class MpGatewayHmacNonceRepository implements GatewayHmacNonceRepository {

    /** {@code gateway_hmac_nonce} 的受守卫持久化边界。/ Guarded store for gateway_hmac_nonce rows. */
    @Qualifier("gatewayHmacNoncePersistenceRepository")
    private final GatewayHmacNoncePersistenceRepository nonceRepository;

    /**
     * 中文说明：执行 claim 操作；以雪花主键登记 (accessKey, nonce) 与业务列 {@code expires_at}，
     * 唯一键冲突按原实现捕获 {@code DataIntegrityViolationException} 并返回假，写入影响 0 行时同样返回假。
     * 原 SQL 同时把入参 {@code now} 写入 {@code created_at}；迁移后技术创建时间由审计边界统一填充，故 {@code now}
     * 仅作为调用方参考时间保留在契约中。
     * English summary: Executes the claim operation; registers {@code (accessKey, nonce)} under a snowflake primary key with the business
     * column {@code expires_at}. A unique-key conflict is captured as {@code DataIntegrityViolationException} and claims {@code false}
     * exactly as before, and an insert affecting zero rows likewise claims {@code false}. The legacy statement also wrote the
     * {@code now} argument into {@code created_at}; after the migration the technical creation timestamp is filled by the audit
     * boundary, so {@code now} stays on the contract only as the caller's reference instant.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayHmacNonceRepository.claim(accessKey, nonce, expiresAt, now)}。
     * @param accessKey 参数 访问键；parameter access key。
     * @param nonce 参数 nonce；parameter nonce。
     * @param expiresAt 参数 过期时间；parameter expiry instant。
     * @param now 参数 调用方参考时间；parameter caller's reference instant。
     * @return 返回是否首次登记；returns whether the nonce was claimed freshly.
     */
    @Override
    public boolean claim(
            String accessKey,
            String nonce,
            Instant expiresAt,
            Instant now) {
        GatewayHmacNoncePO row = new GatewayHmacNoncePO();
        row.setId(SnowflakeIdGenerator.nextLongId());
        row.setAccessKey(accessKey);
        row.setNonce(nonce);
        row.setExpiresAt(expiresAt);
        try {
            return nonceRepository.save(row);
        } catch (DataIntegrityViolationException replay) {
            log.debug(
                    "Nonce replay rejected for access key {} by the unique constraint",
                    accessKey
            );
            return false;
        }
    }

    /**
     * 中文说明：执行 deleteExpired 操作；先按 {@code expires_at < now} 读出待清理行（受守卫读取额外带租户与未软删谓词，
     * 并以主键升序固定顺序），再逐行走版本化软删，只有写成的行才计数，等价于原 {@code DELETE FROM gateway_hmac_nonce WHERE
     * expires_at < ?} 的受影响行数。
     * English summary: Executes the deleteExpired operation; reads the rows to sweep under {@code expires_at < now} first — the guarded
     * read additionally carries the tenant and not-soft-deleted predicates and pins the previously unordered set by ascending primary
     * key — then soft-deletes each row under its version, counting only the rows that landed, which equals the affected-row count of the
     * original {@code DELETE FROM gateway_hmac_nonce WHERE expires_at < ?}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpGatewayHmacNonceRepository.deleteExpired(now)}。
     * @param now 参数 清理基准时间；parameter sweep cut-off instant。
     * @return 返回被清理行数；returns the number of swept rows.
     */
    @Override
    public int deleteExpired(Instant now) {
        int swept = 0;
        for (GatewayHmacNoncePO row : nonceRepository.list(
                Wrappers.<GatewayHmacNoncePO>lambdaQuery()
                        .lt(GatewayHmacNoncePO::getExpiresAt, now)
                        .orderByAsc(GatewayHmacNoncePO::getId))) {
            if (nonceRepository.removeById(row)) {
                swept++;
            }
        }
        return swept;
    }
}
