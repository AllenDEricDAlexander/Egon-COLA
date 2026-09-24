package top.egon.cola.component.yuheng.admin.release.repository;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.release.domain.bo.GatewayRecoverableReleaseAttemptBO;
import top.egon.cola.component.yuheng.admin.release.domain.bo.GatewayReleaseAttemptBO;
import top.egon.cola.component.yuheng.admin.release.domain.bo.GatewayReleaseBO;
import top.egon.cola.component.yuheng.admin.release.domain.bo.GatewayReleaseTargetBO;
import top.egon.cola.component.yuheng.admin.release.domain.enums.GatewayReleaseStatus;
import top.egon.cola.component.yuheng.admin.rule.domain.vo.CompiledGatewayRelease;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 中文说明：{@code GatewayReleaseRepository} 是发布聚合的持久化公开端口，签名与被替换的手写 JDBC 实现一致，
 * 只收发 {@code release/domain/bo} 业务载体与规则模块的 {@code CompiledGatewayRelease} 编译产物，不泄漏 RecordPO、DAO，也不继承任何 Spring Data 仓储。
 * English summary: {@code GatewayReleaseRepository} is the public persistence port of the release aggregate; its signatures mirror
 * the replaced hand-written JDBC store and it carries only the {@code release/domain/bo} business carriers plus the rule module's
 * {@code CompiledGatewayRelease}, leaking no RecordPO or DAO and inheriting no Spring Data repository.
 *
 * 用法 / Usage: 由 {@code MpGatewayReleaseRepository} 经各表的受守卫 MyBatis-Plus 持久化边界实现本端口；方法名、返回形状与旧 JDBC 实现保持一致，
 * 事务归属仍由调用方（{@code gatewayTransactionManager}）承担。/ {@code MpGatewayReleaseRepository} implements this port over the
 * table-specific guarded MyBatis-Plus persistence boundaries; method names and return shapes stay as in the legacy JDBC store and the
 * caller still owns the transaction ({@code gatewayTransactionManager}).
 */
@Validated
public interface GatewayReleaseRepository {

    /**
     * 中文说明：执行 insert 操作；一次登记发布头、发布内容快照与首个 attempt，等价于旧实现的三条 INSERT 语句。
     * English summary: Executes the insert operation; records the release head, its content snapshot and the first attempt in one call,
     * equivalent to the three legacy INSERT statements.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseRepository.insert(release, compiled, attemptNo)}。调用方须在自身事务内调用并处理持久冲突异常；
     * / Call it inside the caller's transaction and handle persistence-conflict exceptions.
     * @param release 参数 发布载体，携带服务端可信的不透明编号与业务修订；parameter release carrier carrying the trusted opaque identifier and business revision。
     * @param compiled 参数 compiled 编译产物快照；parameter compiled release snapshot。
     * @param attemptNo 参数 attemptNo 首个尝试序号；parameter attempt no of the first attempt。
     */
    void insert(
            @Valid GatewayReleaseBO release,
            CompiledGatewayRelease compiled,
            int attemptNo);

    /**
     * 中文说明：执行 find 操作；按不透明编号读取单个活跃发布头业务载体，缺失或不可命中时如实返回空。
     * English summary: Executes the find operation; reads one active release head carrier by opaque identifier and returns empty truthfully
     * when it is absent or unmatchable.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseRepository.find(releaseId)}。
     * @param releaseId 参数 发布Id；parameter release id。
     * @return 返回 find 的处理结果；returns the result of the operation.
     */
    Optional<GatewayReleaseBO> find(@NotBlank String releaseId);

    /**
     * 中文说明：执行 history 操作；按网关组列出全部活跃发布头，保留旧 SQL 的 {@code created_at DESC} 倒序（追加技术 id 稳定次序）。
     * English summary: Executes the history operation; lists every active release head of the gateway group, preserving the legacy
     * {@code created_at DESC} order (with a stable technical-id tie-break).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseRepository.history(gatewayGroupId)}。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 history 的处理结果；returns the result of the operation.
     */
    List<GatewayReleaseBO> history(@NotBlank String gatewayGroupId);

    /**
     * 中文说明：执行 recoverable 操作；只列出仍在发布中（{@code PUBLISHING}）的发布及其发布日志中的最大 attempt 序号，
     * 终态发布仍需经管理面显式重试，等价于旧实现的 {@code status='PUBLISHING'} 联表加 {@code MAX(attempt_no)} 子查询。
     * English summary: Executes the recoverable operation; lists only releases still {@code PUBLISHING} together with the maximum attempt
     * number present in their publication journal, keeping terminal releases explicitly retryable through the management API, equivalent to
     * the legacy {@code status='PUBLISHING'} join plus the {@code MAX(attempt_no)} sub-select.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseRepository.recoverable()}。定时协调器需在可信租户上下文内调用。
     * / The scheduled reconciler must call it with a trusted tenant context bound.
     * @return 返回 recoverable 的处理结果；returns the result of the operation.
     */
    List<GatewayRecoverableReleaseAttemptBO> recoverable();

    /**
     * 中文说明：执行 attempts 操作；按 {@code attempt_no DESC} 列出发布的全部尝试，并把各 attempt 的实例目标观测聚合进载体。
     * English summary: Executes the attempts operation; lists every attempt of the release ordered by {@code attempt_no DESC} and
     * aggregates each attempt's instance targets into the carrier.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseRepository.attempts(releaseId)}。
     * @param releaseId 参数 发布Id；parameter release id。
     * @return 返回 attempts 的处理结果；returns the result of the operation.
     */
    List<GatewayReleaseAttemptBO> attempts(@NotBlank String releaseId);

    /**
     * 中文说明：执行 latestAttempt 操作；返回发布已有的最大 attempt 序号，无任何 attempt 时按旧实现抛出“未找到”非法参数异常。
     * English summary: Executes the latest attempt operation; returns the greatest attempt number of the release and, when no attempt
     * exists, raises the legacy "not found" illegal-argument failure.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseRepository.latestAttempt(releaseId)}。
     * @param releaseId 参数 发布Id；parameter release id。
     * @return 返回 latestAttempt 的处理结果；returns the result of the operation.
     */
    int latestAttempt(@NotBlank String releaseId);

    /**
     * 中文说明：执行 loadCompiled 操作；从发布内容快照还原编译产物（规范快照、激活内容与分块清单），快照缺失时按旧实现抛出“内容未找到”。
     * English summary: Executes the load compiled operation; restores the compiled artifact (canonical snapshot, activation content and
     * chunk manifest) from the release content row and raises the legacy "content not found" failure when the snapshot is missing.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseRepository.loadCompiled(releaseId)}。
     * @param releaseId 参数 发布Id；parameter release id。
     * @return 返回 loadCompiled 的处理结果；returns the result of the operation.
     */
    CompiledGatewayRelease loadCompiled(@NotBlank String releaseId);

    /**
     * 中文说明：仅查询持久化的制品 SHA，不加载完整 JSON 快照。
     * English summary: Reads only the persisted artifact SHA, not the full JSON snapshot.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseRepository.findArtifactSha256(releaseId)}。
     * @param releaseId 参数 发布Id；parameter release id。
     * @return 返回 findArtifactSha256 的处理结果；returns the result of the operation.
     */
    Optional<String> findArtifactSha256(@NotBlank String releaseId);

    /**
     * 中文说明：执行 nextAttempt 操作；在现有最大 attempt 序号之上加一并登记新的 {@code PENDING} 尝试，无尝试时从 1 开始。
     * English summary: Executes the next attempt operation; registers a new {@code PENDING} attempt one past the greatest existing attempt
     * number, starting at 1 when the release has none.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseRepository.nextAttempt(releaseId, now)}。调用方须在自身事务内调用。
     * / Call it inside the caller's transaction.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param now 参数 now 尝试起始时间；parameter attempt start instant。
     * @return 返回 nextAttempt 的处理结果；returns the result of the operation.
     */
    int nextAttempt(@NotBlank String releaseId, Instant now);

    /**
     * 中文说明：执行 beginAttempt 操作；把 attempt 与发布头同时推进到 {@code PUBLISHING}，并按旧 SQL 清空 attempt 的完成时间与错误列。
     * English summary: Executes the begin attempt operation; advances both the attempt and the release head to {@code PUBLISHING} and,
     * exactly as the legacy SQL, clears the attempt's completion and error columns.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseRepository.beginAttempt(releaseId, attemptNo, now)}。调用方须在自身事务内调用，
     * 受守卫写入 0 行按持久冲突如实抛出。/ Call it inside the caller's transaction; a zero-row guarded write surfaces truthfully as a conflict.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param attemptNo 参数 attemptNo；parameter attempt no。
     * @param now 参数 now；parameter now。
     */
    void beginAttempt(
            @NotBlank String releaseId,
            int attemptNo,
            Instant now);

    /**
     * 中文说明：执行 completeAttempt 操作；终结 attempt 的状态与错误列、回写发布头的状态/部分应用/变更号，并按旧实现的
     * {@code ON CONFLICT DO UPDATE} 逐个上补（upsert）实例目标观测。
     * English summary: Executes the complete attempt operation; finalizes the attempt's status and error columns, writes back the release
     * head's status/partial-applied/change id and upserts each instance-target observation with the legacy
     * {@code ON CONFLICT DO UPDATE} semantics.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseRepository.completeAttempt(...)}。调用方须在自身事务内调用；{@code targets} 为
     * {@code null} 元素以外的写入语义与旧实现一致。/ Call it inside the caller's transaction; per-element write semantics for {@code targets}
     * stay as in the legacy implementation.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param attemptNo 参数 attemptNo；parameter attempt no。
     * @param status 参数 status 终态；parameter terminal status。
     * @param partialApplied 参数 partialApplied；parameter partial applied。
     * @param changeId 参数 changeId；parameter change id。
     * @param errorCode 参数 errorCode；parameter error code。
     * @param errorMessage 参数 error消息；parameter error message。
     * @param targets 参数 targets；parameter targets。
     * @param now 参数 now；parameter now。
     */
    void completeAttempt(
            @NotBlank String releaseId,
            int attemptNo,
            GatewayReleaseStatus status,
            boolean partialApplied,
            String changeId,
            String errorCode,
            String errorMessage,
            List<GatewayReleaseTargetBO> targets,
            Instant now);

    /**
     * 中文说明：执行 hasReleaseInProgress 操作；判断网关组内是否仍存在未完成（{@code CREATED}/{@code VALIDATING}/{@code READY}/{@code PUBLISHING}）的发布。
     * English summary: Executes the has release in progress operation; reports whether the gateway group still holds an unfinished release
     * in {@code CREATED}/{@code VALIDATING}/{@code READY}/{@code PUBLISHING}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReleaseRepository.hasReleaseInProgress(gatewayGroupId)}。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 hasReleaseInProgress 的处理结果；returns the result of the operation.
     */
    boolean hasReleaseInProgress(@NotBlank String gatewayGroupId);
}
