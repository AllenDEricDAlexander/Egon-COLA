package top.egon.cola.component.yuheng.admin.observability.repository.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.observability.converter.GatewayAuditLogPersistenceConverter;
import top.egon.cola.component.yuheng.admin.observability.domain.bo.GatewayAuditLogBO;
import top.egon.cola.component.yuheng.admin.observability.domain.po.GatewayAuditLogRecordPO;
import top.egon.cola.component.yuheng.admin.observability.repository.GatewayAuditLogRepository;
import top.egon.cola.component.yuheng.admin.observability.repository.mp.GatewayAuditLogPersistenceRepository;

/**
 * 中文说明：{@code MpGatewayAuditLogRepository} 是管理面审计写入的 MyBatis-Plus 门面，取代原先直接继承 Spring Data
 * {@code JpaRepository} 的 {@code GatewayAuditLogRepository}（业务载体早已不是托管实体，那条继承链在启动期即不可用），
 * 并按端口保留调用方实际使用的唯一方法 {@code save}：写入按端口主键是否存在决定新增还是更新——
 * 无标识时走受守卫插入（{@code ASSIGN_ID} 补技术主键、租户与审计列由边界盖章），
 * 已带雪花号时先按同一活跃行谓词读到 {@code version} 令牌再以乐观锁 CAS 回写，行缺失即退回插入；
 * 影响 0 行按写入冲突如实抛出，绝不伪造成功；返回值是守卫补齐技术字段后的持久化业务对象
 * （标识按十进制文本还原，与旧层 {@code getString("id")} 完全一致）；两个 jsonb 摘要列的编解码、
 * {@code successful} 的空列口径与 {@code occurred_at} 直传只经 {@code gatewayAuditLogPersistenceConverter} 完成，
 * 公开端口不泄漏 {@code GatewayAuditLogRecordPO} 或 DAO。
 * English summary: {@code MpGatewayAuditLogRepository} is the MyBatis-Plus facade for management audit writes, replacing the port that
 * used to extend Spring Data {@code JpaRepository} directly (the business carrier stopped being a managed entity long ago, so that
 * inheritance chain was unusable at bootstrap), and it keeps on the port only the single method its nine callers use, {@code save}:
 * the write is insert or update decided by whether the port identifier is present — a missing one takes the guarded insert where
 * {@code ASSIGN_ID} supplies the technical key and the boundary stamps tenant and audit columns, while a supplied snowflake value first
 * reads the {@code version} token through the same active-row predicate and then writes back under the optimistic lock, falling back to
 * an insert when no such row exists; a zero-row effect surfaces truthfully as a write conflict instead of a fake success, the returned
 * value is the persisted business object after the guard filled its technical fields (the identifier rendered as the very same decimal
 * text the legacy {@code getString("id")} produced), and the jsonb summary codecs, the absent-flag reading of {@code successful} and the
 * direct {@code occurred_at} transfer happen only in {@code gatewayAuditLogPersistenceConverter}, so the public port leaks neither a
 * {@code GatewayAuditLogRecordPO} nor a DAO.
 *
 * 用法 / Usage: 通过业务端口 {@code GatewayAuditLogRepository} 由 Spring 容器注入；与遗留实现一致，本门面不声明
 * {@code @Transactional}，审计写入必须留在触发它的业务用例事务内，租户与操作者上下文由调用方经 MDC 携带。
 * Inject it through the {@code GatewayAuditLogRepository} port; exactly like the legacy repository this facade declares no
 * {@code @Transactional}, so an audit write stays inside the transaction of the use case that emits it, with the tenant and actor
 * context carried by the caller through MDC.
 */
@Slf4j
@Validated
@Repository("gatewayAuditLogRepository")
@RequiredArgsConstructor
public class MpGatewayAuditLogRepository implements GatewayAuditLogRepository {

    /**
     * 中文说明：审计行的受守卫持久化仓储（租户过滤、活跃读取、插入盖章与乐观锁 CAS 的唯一入口）。
     * English summary: The guarded persistence store for audit rows, the only entry point for tenant filtering, active reads, insert
     * stamping and optimistic-lock CAS.
     */
    @Qualifier("gatewayAuditLogPersistenceRepository")
    private final GatewayAuditLogPersistenceRepository gatewayAuditLogPersistenceRepository;

    /**
     * 中文说明：{@code GatewayAuditLogBO} 与 {@code GatewayAuditLogRecordPO} 的双向转换器。
     * English summary: The bidirectional converter between GatewayAuditLogBO and GatewayAuditLogRecordPO.
     */
    @Qualifier("gatewayAuditLogPersistenceConverter")
    private final GatewayAuditLogPersistenceConverter gatewayAuditLogPersistenceConverter;

    /**
     * 中文说明：执行 save 操作；等价被取代的 {@code JpaRepository.save(...)} 语义——按主键是否存在决定插入或合并更新：
     * 九个调用点都自带雪花号，因此常规路径是带标识插入；标识已存在于本租户的活跃行时改走乐观锁 CAS 回写，
     * 并先读回守卫令牌；{@code null} 载体由端口 {@code @NotNull} 契约拒绝（受守卫边界亦按
     * 「entity must not be null」拒绝），影响 0 行按写入冲突抛出。
     * English summary: Executes the save operation; the equivalent of the replaced {@code JpaRepository.save(...)} semantics, choosing
     * insert or merged update from the presence of the identifier: all nine call sites bring their own snowflake value, so the normal
     * path is an identified insert; when that identifier already exists among this tenant's active rows the write becomes an
     * optimistic-lock CAS after the guard token has been read back. A {@code null} carrier is rejected by the port's {@code @NotNull}
     * contract (the guarded boundary rejects it as {@code entity must not be null} too), and a zero-row effect raises a write conflict.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mpGatewayAuditLogRepository.save(auditLog)}。调用方需在同一事务内、
     * 携带可信租户上下文时使用；/ Call it inside one transaction with a trusted tenant context and handle the thrown conflict.
     * @param auditLog 待写入的审计业务对象；the audit business object to persist。
     * @return 返回 已持久化的审计业务对象；returns the persisted audit business object.
     */
    @Override
    public GatewayAuditLogBO save(GatewayAuditLogBO auditLog) {
        GatewayAuditLogRecordPO candidate =
                gatewayAuditLogPersistenceConverter.newRow(auditLog);
        Optional<GatewayAuditLogRecordPO> current = candidate.getId() == null
                ? Optional.empty()
                : gatewayAuditLogPersistenceRepository.list(
                        Wrappers.<GatewayAuditLogRecordPO>lambdaQuery()
                                .eq(GatewayAuditLogRecordPO::getId, candidate.getId())
                ).stream().findFirst();
        boolean written;
        if (current.isEmpty()) {
            written = gatewayAuditLogPersistenceRepository.save(candidate);
        } else {
            candidate.setVersion(current.get().getVersion());
            written = gatewayAuditLogPersistenceRepository.updateById(candidate);
        }
        if (!written) {
            throw new IllegalStateException("YUHENG_ADMIN_AUDIT_LOG_WRITE_CONFLICT");
        }
        return gatewayAuditLogPersistenceConverter.toBusiness(candidate);
    }
}
