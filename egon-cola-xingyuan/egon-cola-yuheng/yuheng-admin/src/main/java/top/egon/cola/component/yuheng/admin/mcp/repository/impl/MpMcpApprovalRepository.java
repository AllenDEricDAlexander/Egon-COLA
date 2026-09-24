package top.egon.cola.component.yuheng.admin.mcp.repository.impl;

import com.baomidou.mybatisplus.core.conditions.AbstractLambdaWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.mcp.converter.McpApprovalPersistenceConverter;
import top.egon.cola.component.yuheng.admin.mcp.domain.bo.McpApprovalBO;
import top.egon.cola.component.yuheng.admin.mcp.domain.enums.McpPersistentApprovalStatusEnum;
import top.egon.cola.component.yuheng.admin.mcp.domain.po.McpApprovalRecordPO;
import top.egon.cola.component.yuheng.admin.mcp.repository.McpApprovalRepository;
import top.egon.cola.component.yuheng.admin.mcp.repository.mp.McpApprovalPersistenceRepository;

/**
 * 中文说明：{@code MpMcpApprovalRepository} 是 MCP 审批令牌的 MyBatis-Plus 门面存储，逐方法取代被退役的遗留持久化载体：
 * {@code gateway_mcp_approval} 的每次读写都走受守卫的 {@code EgonColaRepository} 边界（同租户过滤、仅活跃行
 * {@code deleted_at IS NULL}、技术 {@code version} 乐观锁），令牌标识落在协议列 {@code approval_key}、MCP 主体租户落在
 * {@code subject_tenant_id}（遗留 {@code tenant_id} 谓词的落位）；签发保留旧 SQL 的状态机初值字面量
 * （{@code status='PENDING'}、{@code revision=0}、{@code consumed_at=NULL}），消费、撤销与过期保留旧语句的
 * 「按业务谓词读定位 + 乐观锁 CAS 写入」两步顺序与全部谓词（摘要六元组、状态 {@code PENDING}、有效期），
 * 业务 {@code revision} 只在命中时自增，谓词不匹配或影响 0 行一律如实返回 {@code false}／少计一行，绝不伪造成功；
 * 列与类型映射只经 {@code mcpApprovalPersistenceConverter} 完成，读写两侧的载体一律重新经
 * {@code McpApprovalBO.normalized(...)} 复核旧构造器不变量，公开端口不泄漏 {@code McpApprovalRecordPO} 或 DAO。
 * English summary: {@code MpMcpApprovalRepository} is the MyBatis-Plus facade store of MCP approval tokens, replacing the retired
 * legacy persistence carrier method by method: every read and write of {@code gateway_mcp_approval} goes through the guarded
 * {@code EgonColaRepository} boundary (same-tenant filtering, active rows only under {@code deleted_at IS NULL}, the technical
 * {@code version} optimistic lock), the token identifier lands in the protocol column {@code approval_key} and the MCP subject tenant
 * in {@code subject_tenant_id} (where the legacy {@code tenant_id} predicate lands); issuing keeps the legacy state-machine literals
 * ({@code status='PENDING'}, {@code revision=0}, {@code consumed_at=NULL}), while consume, revoke and expire keep the legacy two-step
 * order of locating by the business predicates and then writing under the optimistic lock together with every predicate (the digest
 * tuple, the {@code PENDING} status and the validity window), the business {@code revision} only increments on a hit, and an unmatched
 * predicate or a zero-row effect truthfully returns {@code false} or counts one row short instead of a fake success; column and type
 * mapping happens only in {@code mcpApprovalPersistenceConverter}, both boundaries re-check carriers through
 * {@code McpApprovalBO.normalized(...)}, and the public port leaks neither {@code McpApprovalRecordPO} nor a DAO.
 *
 * 用法 / Usage: 通过业务端口 {@code McpApprovalRepository} 由 Spring 容器注入；遗留实现没有声明事务边界，令牌签发与消费由调用方
 * 的 {@code gatewayTransactionManager} 事务串联，故本门面不声明 {@code @Transactional}；{@code consume} 的一次性消费语义由
 * 「令牌摘要 + 状态 {@code PENDING} + 乐观锁 CAS」共同保证。
 * Inject it through the business port {@code McpApprovalRepository}; the legacy carrier declared no transaction boundary and token
 * issuing and consumption are chained inside the caller's {@code gatewayTransactionManager} transaction, so this facade declares no
 * {@code @Transactional}; single-use consumption is guaranteed by the token digest plus the {@code PENDING} status plus the optimistic
 * compare-and-set.
 */
@Slf4j
@Repository("mpMcpApprovalRepository")
@RequiredArgsConstructor
@Validated
public class MpMcpApprovalRepository implements McpApprovalRepository {

    /**
     * 中文说明：审批行的受守卫持久化仓储（租户过滤、活跃读取、乐观锁 CAS 的唯一入口）。
     * English summary: The guarded persistence store for approval rows, the only entry point for tenant filtering, active reads and
     * optimistic-lock CAS.
     */
    @Qualifier("mcpApprovalPersistenceRepository")
    private final McpApprovalPersistenceRepository approvalPersistenceRepository;

    /**
     * 中文说明：{@code McpApprovalBO} 与 {@code McpApprovalRecordPO} 的双向转换器。
     * English summary: The bidirectional converter between McpApprovalBO and McpApprovalRecordPO.
     */
    @Qualifier("mcpApprovalPersistenceConverter")
    private final McpApprovalPersistenceConverter approvalPersistenceConverter;

    /**
     * 中文说明：执行 issue 操作；等价遗留 {@code INSERT INTO gateway_mcp_approval(...)}：入参守护沿用
     * {@code Objects.requireNonNull(approval, "approval")}，载体先重新经 {@code McpApprovalBO.normalized(...)} 复核
     * （含摘要 64 字符与 {@code expiresAt must be after issuedAt} 守护），行模型由转换器渲染后按旧 SQL 字面量逐列补齐
     * 状态机三列，技术主键、租户与审计列由受守卫边界补齐；影响 0 行按插入冲突如实抛出。
     * English summary: Executes the issue operation; equivalent to the legacy {@code INSERT INTO gateway_mcp_approval(...)}: the legacy
     * {@code Objects.requireNonNull(approval, "approval")} guard is kept, the carrier is re-checked through
     * {@code McpApprovalBO.normalized(...)} (which carries the 64-character digest rule and {@code expiresAt must be after issuedAt}),
     * the row rendered by the converter then gets the three state-machine columns filled with the legacy SQL literals while the guarded
     * boundary supplies the technical key, tenant and audit columns, and a zero-row effect surfaces truthfully as an insert conflict.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mpMcpApprovalRepository.issue(approvalBO)}。
     * @param approval 参数 审批；parameter approval.
     */
    @Override
    public void issue(McpApprovalBO approval) {
        Objects.requireNonNull(approval, "approval");
        McpApprovalRecordPO row = approvalPersistenceConverter.newRow(renormalize(approval));
        row.setStatus(McpPersistentApprovalStatusEnum.PENDING.wireValue());
        row.setRevision(0L);
        row.setConsumedAt(null);
        if (!approvalPersistenceRepository.save(row)) {
            throw new IllegalStateException(
                    "YUHENG_ADMIN_MCP_APPROVAL_INSERT_CONFLICT"
            );
        }
    }

    /**
     * 中文说明：执行 consume 操作；等价遗留 {@code UPDATE ... SET status = 'CONSUMED', consumed_at = ?,
     * revision = revision + 1 WHERE token_digest = ? AND subject_id = ? AND tenant_id = ? AND client_id = ? AND
     * server_code = ? AND tool_name = ? AND argument_digest = ? AND status = 'PENDING' AND expires_at > ?}：
     * 守护顺序与旧参数求值顺序一致（消费时刻、令牌摘要、主体、租户、客户端、服务、工具、参数摘要、有效期），
     * 迁移后 {@code tenant_id} 谓词落在 {@code subject_tenant_id}；按同一组谓词读定位后逐行以 {@code revision} 加
     * 技术 {@code version} 双重 CAS 消费，最后仍按旧 {@code == 1} 判定成功，故多行同时命中时与旧实现一样返回 {@code false}。
     * English summary: Executes the consume operation; equivalent to the legacy
     * {@code UPDATE ... SET status = 'CONSUMED', consumed_at = ?, revision = revision + 1 WHERE token_digest = ? AND subject_id = ? AND
     * tenant_id = ? AND client_id = ? AND server_code = ? AND tool_name = ? AND argument_digest = ? AND status = 'PENDING' AND
     * expires_at > ?}: the guards run in the legacy evaluation order (the consumption instant, the token digest, the subject, the tenant,
     * the client, the server, the tool, the argument digest and the validity window), and the migrated {@code tenant_id} predicate is the
     * {@code subject_tenant_id} column; rows located by that very predicate set are consumed one by one under the double
     * {@code revision}-plus-technical-{@code version} compare-and-set, and success is still decided by the legacy {@code == 1}, so a set
     * of several matching rows returns {@code false} exactly as the legacy statement did.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mpMcpApprovalRepository.consume(tokenDigest, subjectId, tenantId, clientId, serverCode,
     * toolName, argumentDigest, now)}。
     * @param tokenDigest 参数 tokenDigest；parameter token digest.
     * @param subjectId 参数 subjectId；parameter subject id.
     * @param tenantId 参数 租户Id；parameter tenant id.
     * @param clientId 参数 客户端Id；parameter client id.
     * @param serverCode 参数 服务器Code；parameter server code.
     * @param toolName 参数 工具名称；parameter tool name.
     * @param argumentDigest 参数 argumentDigest；parameter argument digest.
     * @param now 参数 now；parameter now.
     * @return 返回 consume 的处理结果；returns {@code true} when exactly one token was consumed.
     */
    @Override
    public boolean consume(
            String tokenDigest,
            String subjectId,
            String tenantId,
            String clientId,
            String serverCode,
            String toolName,
            String argumentDigest,
            Instant now) {
        Objects.requireNonNull(now, "now");
        String digest = digest(tokenDigest, "tokenDigest");
        String subject = required(subjectId, "subjectId");
        String tenant = required(tenantId, "tenantId");
        String client = required(clientId, "clientId");
        String server = required(serverCode, "serverCode");
        String tool = required(toolName, "toolName");
        String argument = digest(argumentDigest, "argumentDigest");
        return advanceAll(
                consumeLocation(
                        Wrappers.<McpApprovalRecordPO>lambdaQuery(),
                        digest,
                        subject,
                        tenant,
                        client,
                        server,
                        tool,
                        argument,
                        now),
                McpPersistentApprovalStatusEnum.CONSUMED,
                now
        ) == 1;
    }

    /**
     * 中文说明：执行 find 操作；等价遗留 {@code SELECT ... WHERE id = ?}（旧语句对该标识无必填守护，故原样下推），
     * 迁移后令牌标识即协议列 {@code approval_key}，命中多行时与旧 {@code stream().findFirst()} 一致取首行。
     * English summary: Executes the find operation; equivalent to the legacy {@code SELECT ... WHERE id = ?} (the legacy statement guarded
     * that identifier not at all, so the value is pushed as it arrives), where the token identifier is the migrated protocol column
     * {@code approval_key}, and multiple rows resolve to the first one exactly like the legacy {@code stream().findFirst()}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mpMcpApprovalRepository.find(id)}。
     * @param id 参数 id；parameter id.
     * @return 返回 find 的处理结果；returns the approval carrier when present.
     */
    @Override
    public Optional<McpApprovalBO> find(String id) {
        return approvalPersistenceRepository.list(
                boundPredicate(Wrappers.<McpApprovalRecordPO>lambdaQuery()
                        .eq(McpApprovalRecordPO::getApprovalKey, id))
        ).stream().findFirst().map(this::carrier);
    }

    /**
     * 中文说明：执行 expire 操作；等价遗留 {@code UPDATE ... SET status = 'EXPIRED', revision = revision + 1 WHERE
     * status = 'PENDING' AND expires_at <= ?}：受守卫边界禁止无范围的批量写，故先按同一谓词读定位待过期令牌，
     * 再逐行以 {@code revision} 加技术 {@code version} 双重 CAS 推进，返回值仍是本次真正推进的行数
     * （与旧语句的影响行数一致；并发下已被他人推进的行按旧语义不再计入）。
     * English summary: Executes the expire operation; equivalent to the legacy
     * {@code UPDATE ... SET status = 'EXPIRED', revision = revision + 1 WHERE status = 'PENDING' AND expires_at <= ?}: the guarded
     * boundary forbids unscoped bulk writes, so the due tokens are located by that very predicate and advanced row by row under the
     * double {@code revision}-plus-technical-{@code version} compare-and-set, and the returned number is still how many rows this call
     * really moved (matching the legacy affected-row count; a row a concurrent worker already advanced stops counting, as the legacy
     * statement also left it untouched).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mpMcpApprovalRepository.expire(now)}。
     * @param now 参数 now；parameter now.
     * @return 返回 expire 的处理结果；returns how many tokens were expired.
     */
    @Override
    public int expire(Instant now) {
        Objects.requireNonNull(now, "now");
        return advanceAll(
                expireLocation(
                        Wrappers.<McpApprovalRecordPO>lambdaQuery(),
                        now),
                McpPersistentApprovalStatusEnum.EXPIRED,
                null
        );
    }

    /**
     * 中文说明：执行 revoke 操作；等价遗留 {@code UPDATE ... SET status = 'REVOKED', revision = revision + 1 WHERE
     * id = ? AND revision = ? AND status = 'PENDING'}（旧语句对 {@code id} 无必填守护）：按同一组谓词读定位取 CAS
     * 令牌后单行推进，行缺失、状态非 {@code PENDING}、修订不匹配或影响 0 行都返回 {@code false}。
     * English summary: Executes the revoke operation; equivalent to the legacy
     * {@code UPDATE ... SET status = 'REVOKED', revision = revision + 1 WHERE id = ? AND revision = ? AND status = 'PENDING'} (the legacy
     * statement guarded {@code id} not at all): the row is located by that very predicate set to obtain the CAS token and then advanced
     * single-row, so a missing row, a status other than {@code PENDING}, a revision mismatch or a zero-row effect returns {@code false}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mpMcpApprovalRepository.revoke(id, expectedRevision)}。
     * @param id 参数 id；parameter id.
     * @param expectedRevision 参数 expectedRevision；parameter expected revision.
     * @return 返回 revoke 的处理结果；returns {@code true} when exactly one token was revoked.
     */
    @Override
    public boolean revoke(String id, long expectedRevision) {
        Optional<McpApprovalRecordPO> current = approvalPersistenceRepository.list(
                boundPredicate(Wrappers.<McpApprovalRecordPO>lambdaQuery()
                        .eq(McpApprovalRecordPO::getApprovalKey, id)
                        .eq(McpApprovalRecordPO::getRevision, expectedRevision)
                        .eq(
                                McpApprovalRecordPO::getStatus,
                                McpPersistentApprovalStatusEnum.PENDING.wireValue()
                        ))
        ).stream().findFirst();
        if (current.isEmpty()) {
            return false;
        }
        McpApprovalRecordPO row = current.get();
        return approvalPersistenceRepository.update(
                McpApprovalRecordPO.builder()
                        .id(row.getId())
                        .version(row.getVersion())
                        .status(McpPersistentApprovalStatusEnum.REVOKED.wireValue())
                        .revision(nextRevision(row.getRevision()))
                        .build(),
                boundPredicate(Wrappers.<McpApprovalRecordPO>lambdaUpdate()
                        .eq(McpApprovalRecordPO::getId, row.getId())
                        .eq(McpApprovalRecordPO::getApprovalKey, id)
                        .eq(McpApprovalRecordPO::getRevision, expectedRevision)
                        .eq(
                                McpApprovalRecordPO::getStatus,
                                McpPersistentApprovalStatusEnum.PENDING.wireValue()
                        ))
        );
    }

    /**
     * 中文说明：执行 advanceAll 操作：把遗留单条批量 {@code UPDATE} 落实为「读定位 + 逐行乐观锁 CAS」，
     * 每次推进都随实体下发目标状态、自增后的业务修订以及（仅消费时）消费时刻，并在同一组谓词上追加技术主键；
     * 返回真正被本调用推进的行数，供 {@code consume} 复用旧的 {@code == 1} 判定、{@code expire} 复用旧的影响行数。
     * English summary: Executes the advanceAll operation, realizing the legacy bulk {@code UPDATE} as a locate-plus-per-row
     * optimistic compare-and-set: every advance carries the target status, the incremented business revision and - for consumption only -
     * the consumption instant on the entity, with the technical identifier appended to the same predicate set, and it returns how many
     * rows this call really moved so {@code consume} can reuse the legacy {@code == 1} decision and {@code expire} the legacy affected-row
     * count.
     * @param location 参数 旧 WHERE 谓词的定位查询；parameter the locating query carrying the legacy predicates.
     * @param status 参数 推进到的状态；parameter the status to advance to.
     * @param consumedAt 参数 消费时刻，仅 {@code CONSUMED} 时非空；parameter the consumption instant, non-null only for
     *                   {@code CONSUMED}.
     * @return 返回 被推进的行数；returns the number of rows advanced.
     */
    private int advanceAll(
            AbstractLambdaWrapper<McpApprovalRecordPO, ?> location,
            McpPersistentApprovalStatusEnum status,
            Instant consumedAt) {
        List<McpApprovalRecordPO> rows = approvalPersistenceRepository.list(
                boundPredicate(location)
        );
        int advanced = 0;
        for (McpApprovalRecordPO row : rows) {
            if (approvalPersistenceRepository.update(
                    McpApprovalRecordPO.builder()
                            .id(row.getId())
                            .version(row.getVersion())
                            .status(status.wireValue())
                            .revision(nextRevision(row.getRevision()))
                            .consumedAt(consumedAt)
                            .build(),
                    boundPredicate(repeatLocation(
                            Wrappers.<McpApprovalRecordPO>lambdaUpdate(),
                            row))
            )) {
                advanced++;
            }
        }
        return advanced;
    }

    /**
     * 中文说明：把消费定位谓词挂到任意 lambda 条件上：令牌摘要、主体、主体租户、客户端、服务、工具、参数摘要七项身份
     * 全部相符，状态仍为 {@code PENDING} 且尚未到期；读定位与 CAS 写入共用同一份谓词定义，避免两条路径漂移。
     * English summary: Attaches the consuming location predicates to any lambda condition: all seven identity columns (token digest,
     * subject, subject tenant, client, server, tool and argument digest) match, the status is still {@code PENDING} and the token has not
     * expired; the locating read and the compare-and-set write share one predicate definition so the two paths cannot drift apart.
     * @param predicate 参数 待挂载的条件；parameter the condition to enrich.
     * @param tokenDigest 参数 令牌摘要；parameter the token digest.
     * @param subjectId 参数 主体；parameter the subject identifier.
     * @param tenantId 参数 主体租户；parameter the subject tenant.
     * @param clientId 参数 客户端；parameter the client identifier.
     * @param serverCode 参数 服务编码；parameter the server code.
     * @param toolName 参数 工具名；parameter the Tool name.
     * @param argumentDigest 参数 参数摘要；parameter the argument digest.
     * @param now 参数 消费时刻；parameter the consumption instant.
     * @return 返回 同一份挂好业务谓词的条件；returns the very same condition carrying the business predicates.
     */
    private static <W extends AbstractLambdaWrapper<McpApprovalRecordPO, W>> W consumeLocation(
            W predicate,
            String tokenDigest,
            String subjectId,
            String tenantId,
            String clientId,
            String serverCode,
            String toolName,
            String argumentDigest,
            Instant now) {
        return pending(predicate, now)
                .eq(McpApprovalRecordPO::getTokenDigest, tokenDigest)
                .eq(McpApprovalRecordPO::getSubjectId, subjectId)
                .eq(McpApprovalRecordPO::getSubjectTenantId, tenantId)
                .eq(McpApprovalRecordPO::getClientId, clientId)
                .eq(McpApprovalRecordPO::getServerCode, serverCode)
                .eq(McpApprovalRecordPO::getToolName, toolName)
                .eq(McpApprovalRecordPO::getArgumentDigest, argumentDigest);
    }

    /**
     * 中文说明：把过期定位谓词 {@code status='PENDING' AND expires_at <= ?} 挂到任意 lambda 条件上。
     * English summary: Attaches the expiring predicate {@code status='PENDING' AND expires_at <= ?} to any lambda condition.
     * @param predicate 参数 待挂载的条件；parameter the condition to enrich.
     * @param now 参数 过期判定时刻；parameter the instant expiry is judged against.
     * @return 返回 同一份挂好业务谓词的条件；returns the very same condition carrying the business predicates.
     */
    private static <W extends AbstractLambdaWrapper<McpApprovalRecordPO, W>> W expireLocation(
            W predicate,
            Instant now) {
        return predicate
                .eq(
                        McpApprovalRecordPO::getStatus,
                        McpPersistentApprovalStatusEnum.PENDING.wireValue()
                )
                .le(McpApprovalRecordPO::getExpiresAt, now);
    }

    /**
     * 中文说明：在 CAS 条件上重放刚读到的那一行的全部业务谓词：同一技术主键、同一业务修订、同一状态（即读到的
     * {@code PENDING}），确保「0 行即失败」而不会覆盖并发写入，也不会把已推进的行二次推进。
     * English summary: Replays every business predicate of the row just read on the compare-and-set condition: the same technical
     * identifier, the same business revision and the same status (the {@code PENDING} that was read), so a zero-row effect is a failure,
     * a concurrent write is never overwritten and an already advanced row is never advanced twice.
     * @param predicate 参数 待挂载的更新条件；parameter the update condition to enrich.
     * @param row 参数 刚读到的活跃行；parameter the active row just read.
     * @return 返回 同一份挂好 CAS 谓词的条件；returns the very same condition carrying the CAS predicates.
     */
    private static <W extends AbstractLambdaWrapper<McpApprovalRecordPO, W>> W repeatLocation(
            W predicate,
            McpApprovalRecordPO row) {
        return predicate
                .eq(McpApprovalRecordPO::getId, row.getId())
                .eq(McpApprovalRecordPO::getRevision, row.getRevision())
                .eq(McpApprovalRecordPO::getStatus, row.getStatus());
    }

    /**
     * 中文说明：挂上「状态仍为 {@code PENDING} 且尚未到期」的公共谓词。
     * English summary: Attaches the shared predicate "still {@code PENDING} and not yet expired".
     * @param predicate 参数 待挂载的条件；parameter the condition to enrich.
     * @param now 参数 判定时刻；parameter the instant to judge against.
     * @return 返回 同一份条件；returns the very same condition.
     */
    private static <W extends AbstractLambdaWrapper<McpApprovalRecordPO, W>> W pending(
            W predicate,
            Instant now) {
        return predicate
                .eq(
                        McpApprovalRecordPO::getStatus,
                        McpPersistentApprovalStatusEnum.PENDING.wireValue()
                )
                .gt(McpApprovalRecordPO::getExpiresAt, now);
    }

    /**
     * 中文说明：把行模型经转换器投影为业务载体后立即复核旧构造器不变量（读边界）。
     * English summary: Projects a row onto the business carrier through the converter and immediately re-checks the legacy
     * constructor invariants, which is the read boundary.
     * @param row 参数 行模型；parameter the row model.
     * @return 返回 业务载体；returns the business carrier.
     */
    private McpApprovalBO carrier(McpApprovalRecordPO row) {
        return renormalize(approvalPersistenceConverter.toBusiness(row));
    }

    /**
     * 中文说明：执行 renormalize 操作：把已完成列映射的载体逐字段送回 {@code McpApprovalBO.normalized(...)}，
     * 因此无论签发还是载入都不存在「未经校验构造 {@code McpApprovalBO}」的路径，摘要长度与有效期守护一律按旧文案生效。
     * English summary: Executes the renormalize operation, feeding an already column-mapped carrier field by field back into
     * {@code McpApprovalBO.normalized(...)}, so neither issuing nor loading can hold an unvalidated {@code McpApprovalBO} and the digest
     * length and validity guards apply with the legacy messages.
     *
     * 用法 / Usage: 由 {@link #issue(McpApprovalBO)} 与 {@link #carrier(McpApprovalRecordPO)} 调用。
     * @param carrier 参数 已映射的载体；parameter the mapped carrier.
     * @return 返回 复核后的载体；returns the re-validated carrier.
     */
    private static McpApprovalBO renormalize(McpApprovalBO carrier) {
        return McpApprovalBO.normalized(
                carrier.getId(),
                carrier.getTokenDigest(),
                carrier.getSubjectId(),
                carrier.getTenantId(),
                carrier.getClientId(),
                carrier.getServerCode(),
                carrier.getToolName(),
                carrier.getArgumentDigest(),
                carrier.getIssuedAt(),
                carrier.getExpiresAt()
        );
    }

    /**
     * 中文说明：按旧 SQL 的 {@code revision = revision + 1} 递增读到的业务修订，{@code null} 按 0 起算。
     * English summary: Increments the business revision that was read, matching the legacy {@code revision = revision + 1} and starting
     * from {@code null} as zero.
     * @param revision 参数 读到的修订；parameter the stored revision.
     * @return 返回 自增后的修订；returns the incremented revision.
     */
    private static Long nextRevision(Long revision) {
        return (revision == null ? 0L : revision) + 1L;
    }

    /**
     * 中文说明：沿用遗留边界的摘要守护：先必填规范化，再按旧文案 {@code field + " must contain 64 characters"} 拒绝长度不符。
     * English summary: Keeps the legacy digest guard: the required normalization first, then a length mismatch rejected with the legacy
     * {@code field + " must contain 64 characters"} message.
     * @param value 参数 摘要；parameter digest.
     * @param field 参数 字段名；parameter field.
     * @return 返回 规范化后的摘要；returns the normalized digest.
     */
    private static String digest(String value, String field) {
        String digest = required(value, field);
        if (digest.length() != 64) {
            throw new IllegalArgumentException(
                    field + " must contain 64 characters"
            );
        }
        return digest;
    }

    /**
     * 中文说明：沿用遗留边界的必填规范化：{@code null} 抛 {@code NullPointerException}，空白抛
     * {@code IllegalArgumentException(field + " is required")}，否则去除首尾空白。
     * English summary: Keeps the legacy required normalization: {@code null} raises a {@code NullPointerException} and a blank value
     * raises {@code IllegalArgumentException(field + " is required")}, otherwise the value is trimmed.
     * @param value 参数 值；parameter value.
     * @param field 参数 字段名；parameter field.
     * @return 返回 规范化后的值；returns the trimmed value.
     */
    private static String required(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    /**
     * 中文说明：把条件交付受守卫边界之前先成形一次：MyBatis-Plus 的 {@code eq/gt/le} 只在 SQL 真正成形时才把取值写进
     * {@code paramNameValuePairs}，故此处先成形一次，让谓词在离开门面时参数已完全绑定；
     * 之后（包括 MyBatis 自己下发时）命中同一份片段缓存，参数与 SQL 都不再改变，列解析失败
     * （lambda 缓存缺失）也如实在门面这一层暴露，而不是留到语句下发时。
     * English summary: Forms a condition once before it is handed to the guarded boundary: MyBatis-Plus only moves the values of
     * {@code eq/gt/le} into {@code paramNameValuePairs} while the SQL is being formed, so forming it here first means the parameters are
     * fully bound when the predicate leaves the facade; later renders (including the one MyBatis performs) hit the same segment cache and
     * change neither the parameters nor the SQL, while a column-resolution failure (a missing lambda cache) surfaces truthfully at the
     * facade instead of at statement time.
     * @param predicate 参数 已构造完成的业务条件；parameter the completed business condition.
     * @return 返回 同一份参数已绑定的条件；returns the very same condition with its parameters bound.
     */
    private static <C extends Wrapper<?>> C boundPredicate(C predicate) {
        predicate.getSqlSegment();
        return predicate;
    }
}
