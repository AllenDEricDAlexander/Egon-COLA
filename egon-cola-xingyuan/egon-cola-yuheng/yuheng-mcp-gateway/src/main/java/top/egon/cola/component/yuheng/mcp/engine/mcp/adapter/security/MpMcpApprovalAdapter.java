package top.egon.cola.component.yuheng.mcp.engine.mcp.adapter.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import top.egon.cola.component.yuheng.core.mcp.security.McpApprovalPort;
import top.egon.cola.component.yuheng.mcp.engine.mcp.adapter.support.McpGatewayPersistenceContext;
import top.egon.cola.component.yuheng.mcp.engine.mcp.converter.McpApprovalPersistenceConverter;
import top.egon.cola.component.yuheng.mcp.engine.mcp.domain.enums.McpPersistentApprovalStatusEnum;
import top.egon.cola.component.yuheng.mcp.engine.mcp.domain.po.McpApprovalRecordPO;
import top.egon.cola.component.yuheng.mcp.engine.mcp.repository.McpApprovalPersistenceRepository;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * 中文说明：{@code MpMcpApprovalAdapter} 以 MyBatis-Plus 实现 {@link McpApprovalPort}，逐语义取代旧手写 JDBC 的
 * legacy persistence carrier：每次读写都先进入受信任身份上下文（{@link McpGatewayPersistenceContext}）再走
 * {@code gateway_mcp_approval} 的受守卫边界，因此同租户过滤、仅活跃行（{@code deleted_at IS NULL}）与技术
 * {@code version} 乐观锁是结构性的。旧 {@code SELECT ... WHERE token_digest = ?} 改为具名活跃行查询，旧的六个
 * 身份谓词与 {@code status='PENDING'}、{@code expires_at > ?} 先在新分配的行上逐字复核，随后只走继承的
 * {@code updateById}：以 {@code id}+读取到的 {@code version} 做单行 CAS 写入
 * {@code status='CONSUMED'}、{@code consumed_at}、业务 {@code revision + 1} 与时间，因此两个并发调用者只有一个能命中，
 * 影响 0 行绝不伪装成功；CAS 落败后按旧实现重读该行，仍为 CONSUMED 则返回 CONSUMED，否则 MISMATCH。
 * English summary: {@code MpMcpApprovalAdapter} implements {@link McpApprovalPort} on MyBatis-Plus and replaces the
 * hand-written JDBC legacy persistence carrier semantics by semantics: every read and write first enters the trusted
 * identity context ({@link McpGatewayPersistenceContext}) and then the guarded boundary of
 * {@code gateway_mcp_approval}, so same-tenant filtering, active rows only ({@code deleted_at IS NULL}) and the
 * technical {@code version} optimistic lock are structural. The legacy {@code SELECT ... WHERE token_digest = ?} became
 * the named active-row query, the six legacy identity predicates plus {@code status='PENDING'} and
 * {@code expires_at > ?} are re-checked literally on the freshly loaded row, and only the inherited
 * {@code updateById} writes: a single-row compare-and-set on {@code id} plus the {@code version} that was read storing
 * {@code status='CONSUMED'}, {@code consumed_at}, the business {@code revision + 1} and the timestamp, so of two racing
 * callers only one can hit and a zero-row effect is never presented as success; after a lost compare-and-set the row is
 * re-read like the legacy code did, staying CONSUMED when it was already consumed and MISMATCH otherwise.
 *
 * 用法 / Usage: 通过 {@link McpApprovalPort} 端口由 Spring 注入，返回 {@code Publisher}、阻塞工作固定在
 * boundedElastic 上；持久层不可用（{@link SQLException} 或 {@link DataAccessException}）按旧契约映射为
 * {@link Result#UNAVAILABLE}，其余异常如实向下游传播；列与类型映射只由 {@link McpApprovalPersistenceConverter}
 * 完成，本类不出现手写 {@code UPDATE} SQL，也不泄漏 {@code McpApprovalRecordPO}。/ Inject it through the
 * {@link McpApprovalPort} port; it returns a {@code Publisher} whose blocking work stays on boundedElastic, an
 * unavailable persistence layer ({@link SQLException} or {@link DataAccessException}) maps to
 * {@link Result#UNAVAILABLE} exactly as the legacy contract did while every other failure propagates unchanged, column
 * and type mapping belongs to {@link McpApprovalPersistenceConverter} alone, this class contains no hand-written
 * {@code UPDATE} SQL and never leaks a {@code McpApprovalRecordPO}.
 */
@Slf4j
@Component("mcpApprovalAdapter")
@RequiredArgsConstructor
@Validated
public final class MpMcpApprovalAdapter implements McpApprovalPort {

    /** 中文说明：可被消费的持久化状态，逐字对应旧 SQL 的 {@code status = 'PENDING'}。 English summary: the persisted status that may be consumed, matching the legacy {@code status = 'PENDING'} verbatim. */
    private static final McpPersistentApprovalStatusEnum PENDING = McpPersistentApprovalStatusEnum.PENDING;

    /** 中文说明：已消费的持久化状态，逐字对应旧实现的 {@code "CONSUMED".equals(status)} 分支。 English summary: the persisted consumed status, matching the legacy {@code "CONSUMED".equals(status)} branch verbatim. */
    private static final McpPersistentApprovalStatusEnum CONSUMED = McpPersistentApprovalStatusEnum.CONSUMED;

    /** 中文说明：{@code gateway_mcp_approval} 的受守卫持久化边界。 English summary: the guarded persistence boundary for {@code gateway_mcp_approval}. */
    @Qualifier("mcpApprovalPersistenceRepository")
    private final McpApprovalPersistenceRepository approvalPersistenceRepository;

    /** 中文说明：审批行模型与消费写入之间的唯一列映射器。 English summary: the only column mapper between the approval row model and the consumption write. */
    @Qualifier("mcpApprovalPersistenceConverter")
    private final McpApprovalPersistenceConverter approvalPersistenceConverter;

    /** 中文说明：受守卫读写所需的部署身份上下文包装器。 English summary: the identity-context wrapper the guarded reads and writes need. */
    @Qualifier("mcpGatewayPersistenceContext")
    private final McpGatewayPersistenceContext persistenceContext;

    /** 中文说明：网关统一时钟，等价于旧适配器构造入参的时钟。 English summary: the shared gateway clock, the equivalent of the clock the legacy adapter was constructed with. */
    @Qualifier("gatewayClock")
    private final Clock clock;

    /**
     * 中文说明：执行 consume 操作；先做端口自身的必填校验，再把阻塞工作放到 boundedElastic 上，
     * 并按旧契约只把持久层不可用映射为 {@link Result#UNAVAILABLE}。
     * English summary: Executes the consume operation; the port's own required validation runs first, the blocking work
     * then goes to boundedElastic, and only an unavailable persistence layer maps to
     * {@link Result#UNAVAILABLE} as the legacy contract did.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpApprovalAdapter.consume(request)}。
     * @param request 参数 消费请求；parameter the consumption request.
     * @return 返回 consume 的处理结果；returns the approval decision of the consumption attempt.
     */
    @Override
    public Publisher<Result> consume(ConsumptionRequest request) {
        Objects.requireNonNull(request, "request");
        return Mono.fromCallable(() -> consumeBlocking(request))
                .onErrorReturn(MpMcpApprovalAdapter::isPersistenceUnavailable, Result.UNAVAILABLE)
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 中文说明：在受守卫身份上下文内复刻旧的一次性消费流程：读取活跃行 → 身份与有效期复核 →
     * CONSUMED 直接返回 → 非 PENDING 视为 MISMATCH → 单行 CAS → CAS 落败后重读复核。
     * English summary: Reproduces the legacy one-shot consumption flow inside the guarded identity context: load the
     * active row, re-check identity and validity, return CONSUMED directly, treat any non-PENDING status as MISMATCH,
     * run the single-row compare-and-set and re-read after a lost compare-and-set.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpApprovalAdapter.consumeBlocking(request)}。
     * @param request 参数 消费请求；parameter the consumption request.
     * @return 返回 consumeBlocking 的处理结果；returns the approval decision of the consumption attempt.
     * @throws Exception 受守卫读写抛出的异常，按旧实现原样向下游传播；exceptions the guarded work raises, propagated unchanged like the legacy implementation did.
     */
    private Result consumeBlocking(ConsumptionRequest request) throws Exception {
        Instant now = clock.instant();
        return call(() -> {
            Optional<McpApprovalRecordPO> loaded =
                    approvalPersistenceRepository.findActiveByTokenDigest(request.tokenDigest());
            if (loaded.isEmpty()
                    || !approvalPersistenceConverter.matchesIdentity(loaded.get(), request)
                    || !validAt(loaded.get(), now)) {
                return Result.MISMATCH;
            }
            McpApprovalRecordPO row = loaded.get();
            if (row.getStatus() == CONSUMED) {
                return Result.CONSUMED;
            }
            if (row.getStatus() != PENDING) {
                return Result.MISMATCH;
            }
            if (approvalPersistenceRepository.updateById(
                    approvalPersistenceConverter.consumedRow(row, now))) {
                return Result.APPROVED;
            }
            return approvalPersistenceRepository
                    .findActiveByTokenDigest(request.tokenDigest())
                    .filter(raced -> raced.getStatus() == CONSUMED)
                    .map(raced -> Result.CONSUMED)
                    .orElse(Result.MISMATCH);
        });
    }

    /**
     * 中文说明：复核旧谓词 {@code expires_at > ?}；有效期列为空时按旧实现的空值解引用抛出，不降级为不匹配。
     * English summary: Re-checks the legacy {@code expires_at > ?} predicate; a missing validity column raises like the
     * legacy dereference did instead of degrading into a mismatch.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpMcpApprovalAdapter.validAt(row, now)}。
     * @param row 参数 新分配行；parameter the freshly loaded row.
     * @param now 参数 当前UTC时刻；parameter the current UTC instant.
     * @return 返回 是否仍在有效期内；returns whether the approval is still valid.
     */
    private static boolean validAt(McpApprovalRecordPO row, Instant now) {
        return row.getExpiresAt().isAfter(now);
    }

    /**
     * 中文说明：沿用旧的异常契约：只有持久层不可用（SQL 或 MyBatis/Spring 数据访问失败）才降级为
     * {@link Result#UNAVAILABLE}，业务与数据损坏异常继续向上游传播。
     * English summary: Keeps the legacy exception contract: only an unavailable persistence layer (SQL or the
     * MyBatis/Spring data-access wrappers) degrades into {@link Result#UNAVAILABLE} while business and corrupt-data
     * exceptions keep propagating to the caller.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpMcpApprovalAdapter.isPersistenceUnavailable(failure)}。
     * @param failure 参数 失败原因；parameter the raised failure.
     * @return 返回 是否为持久层不可用；returns whether persistence is unavailable.
     */
    private static boolean isPersistenceUnavailable(Throwable failure) {
        return failure instanceof SQLException || failure instanceof DataAccessException;
    }

    /**
     * 中文说明：把阻塞的受守卫工作放到身份上下文内执行，租户与审计身份只在语句期间存在；
     * 工作抛出的异常如实向上传播。
     * English summary: Runs the blocking guarded work inside the identity context so the tenant and audit identities exist
     * only while the statement runs; any exception the work raises propagates unchanged.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code mcpApprovalAdapter.call(work)}。
     * @param work 参数 受守卫工作；parameter the guarded work.
     * @param <T> 返回类型 / the return type.
     * @return 返回 工作结果；returns the work result.
     * @throws Exception 工作自身抛出的异常；an exception raised by the work itself.
     */
    private <T> T call(Callable<T> work) throws Exception {
        return persistenceContext.call(work);
    }
}
