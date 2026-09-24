package top.egon.cola.component.yuheng.admin.shared.repository.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.shared.converter.IdempotencyPersistenceConverter;
import top.egon.cola.component.yuheng.admin.shared.domain.bo.IdempotencyBO;
import top.egon.cola.component.yuheng.admin.shared.domain.po.IdempotencyRecordPO;
import top.egon.cola.component.yuheng.admin.shared.repository.IdempotencyRepository;
import top.egon.cola.component.yuheng.admin.shared.repository.mp.IdempotencyPersistenceRepository;

/**
 * 中文说明：{@code MpIdempotencyRepository} 是幂等记录存储的 MyBatis-Plus 门面，逐方法取代被退役的手写 JDBC
 * {@code JdbcIdempotencyRepository}（{@code gateway_idempotency_record} 表）：{@code find} 保留旧
 * {@code WHERE scope_type = ? AND scope_id = ? AND idempotency_key = ?} 的三列组合键谓词与
 * {@code stream().findFirst()} 取首行语义，读取改走受守卫边界，因此天然带同租户过滤与仅活跃行
 * （{@code deleted_at IS NULL}）；{@code save} 保留旧 {@code INSERT} 的「无 ON CONFLICT」口径，组合业务键重复仍然
 * 如实抛出 {@code DuplicateKeyException} 让调用方可观测，绝不静默覆盖或吞掉冲突，影响 0 行的写入按冲突抛出而非
 * 伪造成功；列与类型映射（含 {@code response_content} jsonb 编解码、{@code idempotency_key} 与 {@code create_time}
 * 的列名桥接）只经 {@code idempotencyPersistenceConverter} 完成，公开端口不泄漏 {@code IdempotencyRecordPO} 或 DAO。
 * English summary: {@code MpIdempotencyRepository} is the MyBatis-Plus facade store of idempotency records, replacing the retired
 * hand-written {@code JdbcIdempotencyRepository} for {@code gateway_idempotency_record} method by method: {@code find} keeps the
 * legacy three-column composite-key predicate {@code WHERE scope_type = ? AND scope_id = ? AND idempotency_key = ?} and its
 * {@code stream().findFirst()} semantics while the read now runs on the guarded boundary, so same-tenant filtering and active rows
 * only ({@code deleted_at IS NULL}) are structural; {@code save} keeps the legacy {@code INSERT} without any {@code ON CONFLICT}, so a
 * duplicated composite business key still surfaces truthfully as a {@code DuplicateKeyException} for the caller to observe instead of
 * being silently overwritten or swallowed, and a zero-row write raises a conflict rather than faking success; every column and type
 * mapping (the {@code response_content} jsonb codec and the {@code idempotency_key}/{@code create_time} column bridges included) happens
 * only in {@code idempotencyPersistenceConverter}, and the public port leaks neither {@code IdempotencyRecordPO} nor a DAO.
 *
 * 用法 / Usage: 通过业务端口 {@code IdempotencyRepository} 由 Spring 容器注入；与遗留实现一致，本门面不声明
 * {@code @Transactional}，因此「先 {@code find} 复查再 {@code save} 落账」必须在调用方（幂等 replay 与受理）的同一事务内
 * 组合，且租户上下文由调用方携带。/ Inject it through the {@code IdempotencyRepository} port; exactly like the legacy store this
 * facade declares no {@code @Transactional}, so the lookup-then-persist pair has to be composed inside the caller's transaction (the
 * idempotent replay and acceptance paths) with a trusted tenant context.
 */
@Slf4j
@Validated
@Repository("idempotencyRepository")
@RequiredArgsConstructor
public class MpIdempotencyRepository implements IdempotencyRepository {

    /**
     * 中文说明：幂等记录行的受守卫持久化仓储（租户过滤、活跃读取与插入盖章的唯一入口）。
     * English summary: The guarded persistence store for idempotency rows, the only entry point for tenant filtering, active reads and
     * insert stamping.
     */
    @Qualifier("idempotencyPersistenceRepository")
    private final IdempotencyPersistenceRepository idempotencyPersistenceRepository;

    /**
     * 中文说明：{@code IdempotencyBO} 与 {@code IdempotencyRecordPO} 的双向转换器。
     * English summary: The bidirectional converter between IdempotencyBO and IdempotencyRecordPO.
     */
    @Qualifier("idempotencyPersistenceConverter")
    private final IdempotencyPersistenceConverter idempotencyPersistenceConverter;

    /**
     * 中文说明：执行 find 操作；等价遗留 {@code SELECT ... WHERE scope_type = ? AND scope_id = ? AND idempotency_key = ?}，
     * 组合键至多一行，故沿用旧 {@code stream().findFirst()} 取首行而不抛多行异常；未命中如实返回空 {@link Optional}，
     * 命中行的 {@code response_content} 由转换器解码为结构化响应、{@code create_time} 投影为 {@code createdAt}。
     * English summary: Executes the find operation; equivalent to the legacy
     * {@code SELECT ... WHERE scope_type = ? AND scope_id = ? AND idempotency_key = ?}. The composite key matches at most one row, so
     * the legacy {@code stream().findFirst()} keeps returning the first row instead of failing on multiples; a miss truthfully returns an
     * empty {@link Optional}, while a hit decodes {@code response_content} into the structured response and projects {@code create_time}
     * onto {@code createdAt} through the converter.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mpIdempotencyRepository.find(scopeType, scopeId, key)}。
     * @param scopeType 参数 scopeType；parameter scope type。
     * @param scopeId 参数 scopeId；parameter scope id。
     * @param key 参数 键；parameter key。
     * @return 返回 find 的处理结果；returns the stored idempotency record when present.
     */
    @Override
    public Optional<IdempotencyBO> find(
            String scopeType,
            String scopeId,
            String key) {
        return idempotencyPersistenceRepository.list(
                Wrappers.<IdempotencyRecordPO>lambdaQuery()
                        .eq(IdempotencyRecordPO::getScopeType, scopeType)
                        .eq(IdempotencyRecordPO::getScopeId, scopeId)
                        .eq(IdempotencyRecordPO::getIdempotencyKey, key)
        ).stream().findFirst().map(idempotencyPersistenceConverter::toBusiness);
    }

    /**
     * 中文说明：执行 save 操作；等价遗留 {@code INSERT INTO gateway_idempotency_record(...)}，无 {@code ON CONFLICT}，
     * 因此同一组合键重复写入的 {@code DuplicateKeyException} 原样上浮供调用方观测（旧实现即如此）；
     * 技术主键、租户、审计列与 {@code create_time} 由受守卫边界补齐，影响 0 行按写入冲突如实抛出，不返回假成功。
     * English summary: Executes the save operation; equivalent to the legacy {@code INSERT INTO gateway_idempotency_record(...)} with no
     * {@code ON CONFLICT}, so a repeated composite key still surfaces the {@code DuplicateKeyException} the legacy store let escape and
     * callers can still observe it; the technical key, tenant, audit columns and {@code create_time} are filled by the guarded boundary,
     * while a zero-row insert surfaces as a write conflict instead of a fake success.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mpIdempotencyRepository.save(idempotencyBO)}；端口返回 {@code void}，
     * 调用方随后以 {@link #find(String, String, String)} 复读。/ The port returns {@code void} and the caller re-reads through
     * {@link #find(String, String, String)}.
     * @param record 参数 record；parameter record。
     */
    @Override
    public void save(IdempotencyBO record) {
        if (!idempotencyPersistenceRepository.save(
                idempotencyPersistenceConverter.newRow(record))) {
            throw new IllegalStateException(
                    "YUHENG_ADMIN_IDEMPOTENCY_INSERT_CONFLICT"
            );
        }
    }
}
