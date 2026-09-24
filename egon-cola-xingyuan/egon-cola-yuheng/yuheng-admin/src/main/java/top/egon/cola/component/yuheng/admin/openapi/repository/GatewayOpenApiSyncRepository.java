package top.egon.cola.component.yuheng.admin.openapi.repository;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.openapi.domain.bo.GatewayOpenApiSyncBO;
import top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiSyncKeyDTO;
import top.egon.cola.component.yuheng.admin.openapi.domain.enums.GatewayOpenApiSyncStateEnum;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 中文说明：{@code GatewayOpenApiSyncRepository} 是 {@code gateway_openapi_sync_state} 的业务端口，只声明调用方实际
 * 使用的命名方法；每个 {@code application/build/group} 一行同步 journal，工作者更新一律经过业务 {@code revision} CAS，
 * 端口不继承 Spring Data/JPA 泛型 CRUD，也不外泄 MyBatis-Plus 行模型。
 * English summary: {@code GatewayOpenApiSyncRepository} is the business port for {@code gateway_openapi_sync_state};
 * it declares only the named methods current callers actually invoke, keeps one synchronization journal row per
 * {@code application/build/group} whose worker updates all travel through the business {@code revision} CAS, inherits no
 * Spring Data/JPA generic CRUD and leaks no MyBatis-Plus row model.
 *
 * 用法 / Usage: 由 {@code MpGatewayOpenApiSyncRepository} 在调用方 {@code gatewayTransactionManager} 事务内以受守卫的
 * MP 读写实现；journal 的业务 {@code revision} 只在 CAS 命中时自增，0 行影响一律不得被报告为成功。/
 * Implement it through {@code MpGatewayOpenApiSyncRepository} inside the caller's {@code gatewayTransactionManager}
 * transaction; the journal's business {@code revision} increments only when the compare-and-set hits and a zero-row
 * effect is never reported as success.
 */
@Validated
public interface GatewayOpenApiSyncRepository {

    /**
     * 中文说明：执行 find 操作；按稳定业务键（应用/构建/Group）读取同步行，与旧 SQL 的三列谓词一致并保持“取首行”。
     * English summary: Executes the find operation; loads the synchronization row by its stable business key
     * (application/build/Group) with the same three legacy predicates and the legacy first-row selection.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOpenApiSyncRepository.findByKey(key)}。
     * @param key 参数 应用/构建/Group键；parameter application/build/Group key。
     * @return 返回 find 的处理结果；returns an existing row carrier when present.
     */
    Optional<GatewayOpenApiSyncBO> findByKey(@Valid GatewayOpenApiSyncKeyDTO key);

    /**
     * 中文说明：执行 find 操作；按同步行标识读取单行，不存在的 id 返回空 Optional。
     * English summary: Executes the find operation; loads one row by its identifier and returns an empty Optional when
     * the identifier is absent.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOpenApiSyncRepository.findById(syncId)}。
     * @param syncId 参数 状态行标识；parameter state row identifier。
     * @return 返回 find 的处理结果；returns an existing row carrier when present.
     */
    Optional<GatewayOpenApiSyncBO> findById(@NotBlank String syncId);

    /**
     * 中文说明：执行 find 操作；列出某网关应用拥有的全部同步 journal，沿用旧 SQL 的
     * {@code ORDER BY build_id, openapi_group, id} 稳定次序。
     * English summary: Executes the find operation; lists every synchronization journal owned by one Gateway application
     * in the legacy {@code ORDER BY build_id, openapi_group, id} stable order.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOpenApiSyncRepository.findByApplicationId(applicationId)}。
     * @param applicationId 参数 物理网关应用标识；parameter physical Gateway application identifier。
     * @return 返回 find 的处理结果；returns the carriers in stable build/Group order.
     */
    List<GatewayOpenApiSyncBO> findByApplicationId(@NotBlank String applicationId);

    /**
     * 中文说明：执行 find 操作；按生命周期状态列出 journal 供重启修复与诊断使用，沿用旧 SQL 的
     * {@code ORDER BY updated_at, id}（迁移后为持久边界的 {@code update_time}）。
     * English summary: Executes the find operation; lists journals by lifecycle state for restart repair and diagnostics
     * with the legacy {@code ORDER BY updated_at, id} (the migrated {@code update_time} column).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOpenApiSyncRepository.findByStatus(state)}。
     * @param state 参数 待查询状态；parameter state to query。
     * @return 返回 find 的处理结果；returns the carriers in stable update order.
     */
    List<GatewayOpenApiSyncBO> findByStatus(GatewayOpenApiSyncStateEnum state);

    /**
     * 中文说明：执行 upsertDiscovered 操作；等价于旧 {@code ON CONFLICT (application_id, build_id, openapi_group)
     * DO UPDATE}：只刷新提供方观察列，既有生命周期状态、尝试计数、不可变链接与业务 {@code revision} 必须原样保留，
     * 返回库中当前行。
     * English summary: Executes the upsert-discovered operation; the equivalent of the legacy
     * {@code ON CONFLICT (application_id, build_id, openapi_group) DO UPDATE}: only the provider observation columns are
     * refreshed while the existing lifecycle state, attempt count, immutable links and business {@code revision} are kept
     * as they are, and the current stored row is returned.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOpenApiSyncRepository.upsertDiscovered(state)}。
     * @param state 参数 已发现的观察结果；parameter discovered observation。
     * @return 返回 upsertDiscovered 的处理结果；returns the current persisted carrier.
     */
    GatewayOpenApiSyncBO upsertDiscovered(@Valid GatewayOpenApiSyncBO state);

    /**
     * 中文说明：执行 find 操作；有界地列出到期可认领的 journal（旧 SQL 的
     * {@code status IN claimable AND (next_retry_at IS NULL OR next_retry_at <= ?)} 与
     * {@code ORDER BY next_retry_at NULLS FIRST, id LIMIT ?}）。
     * English summary: Executes the find operation; loads due claimable journals in a bounded stable order (the legacy
     * {@code status IN claimable AND (next_retry_at IS NULL OR next_retry_at <= ?)} plus
     * {@code ORDER BY next_retry_at NULLS FIRST, id LIMIT ?}).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOpenApiSyncRepository.findDue(now, limit)}。
     * @param now 参数 当前UTC时刻；parameter current UTC instant。
     * @param limit 参数 最大行数；parameter maximum number of rows。
     * @return 返回 find 的处理结果；returns the due carriers.
     */
    List<GatewayOpenApiSyncBO> findDue(Instant now, int limit);

    /**
     * 中文说明：执行 claim 操作；以业务 {@code revision} 与可认领状态集合做 CAS 认领到期行，命中才递增尝试计数与
     * {@code revision}；影响 0 行返回 {@code false}，绝不伪造认领成功。
     * English summary: Executes the claim operation; claims a due row under the business {@code revision} plus the
     * claimable-state set compare-and-set, incrementing the attempt counter and the revision only on a hit; a zero-row
     * effect returns {@code false} and never fakes ownership.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOpenApiSyncRepository.claim(syncId, expectedRevision, now)}。
     * @param syncId 参数 行标识；parameter row identifier。
     * @param expectedRevision 参数 调用方观察到的修订；parameter revision observed by the caller。
     * @param now 参数 认领时刻；parameter claim timestamp。
     * @return 返回 claim 的处理结果；returns {@code true} when ownership was acquired.
     */
    boolean claim(@NotBlank String syncId, long expectedRevision, Instant now);

    /**
     * 中文说明：执行 transition 操作；在业务 {@code revision} 与期望状态谓词下移动状态机，非法迁移在触达持久边界前按
     * 旧实现的 {@code IllegalArgumentException} 抛出；影响 0 行返回 {@code false}。
     * English summary: Executes the transition operation; moves the state machine under the business {@code revision}
     * and the expected-state predicate, raising the legacy {@code IllegalArgumentException} for an illegal transition
     * before any persistence is touched, and returning {@code false} for a zero-row effect.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOpenApiSyncRepository.transition(syncId, expectedRevision,
     * expectedState, nextState, now)}。
     * @param syncId 参数 行标识；parameter row identifier。
     * @param expectedRevision 参数 调用方观察到的修订；parameter revision observed by the caller。
     * @param expectedState 参数 调用方期望的当前状态；parameter current state expected by the caller。
     * @param nextState 参数 请求的下一状态；parameter requested next state。
     * @param now 参数 迁移时刻；parameter transition timestamp。
     * @return 返回 transition 的处理结果；returns {@code true} when one row was updated.
     */
    boolean transition(
            @NotBlank String syncId,
            long expectedRevision,
            GatewayOpenApiSyncStateEnum expectedState,
            GatewayOpenApiSyncStateEnum nextState,
            Instant now);

    /**
     * 中文说明：执行 setValid 操作；完成摄取并写入快照与聚合 Definition Set 链接，仅在
     * {@code status = 'INGESTING'} 且业务 {@code revision} 命中时生效；影响 0 行返回 {@code false}。
     * English summary: Executes the set-valid operation; completes ingestion and stores the snapshot plus aggregate
     * Definition Set link, effective only while {@code status = 'INGESTING'} and the business revision matches; a
     * zero-row effect returns {@code false}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOpenApiSyncRepository.setValid(syncId, expectedRevision, snapshotId,
     * definitionSetId, now)}。
     * @param syncId 参数 行标识；parameter row identifier。
     * @param expectedRevision 参数 调用方观察到的修订；parameter revision observed by the caller。
     * @param snapshotId 参数 最新Group快照；parameter latest Group snapshot。
     * @param definitionSetId 参数 聚合Definition Set；parameter aggregate Definition Set。
     * @param now 参数 成功时刻；parameter success timestamp。
     * @return 返回 setValid 的处理结果；returns {@code true} when one row was updated.
     */
    boolean setValid(
            @NotBlank String syncId,
            long expectedRevision,
            @NotBlank String snapshotId,
            @NotBlank String definitionSetId,
            Instant now);

    /**
     * 中文说明：执行 markFailure 操作；写入分类后的失败状态、错误码/错误信息与可选重试时间，源状态集合按失败类型
     * 与旧 SQL 完全一致；不受支持的失败状态或超长错误详情在触达持久边界前按旧实现的异常抛出，影响 0 行返回
     * {@code false}。
     * English summary: Executes the mark-failure operation; stores the classified failure state, error code and message
     * plus an optional retry time, with the allowed source-state set exactly as the legacy SQL per failure type. An
     * unsupported failure state or an oversized error detail is raised before persistence is touched, and a zero-row
     * effect returns {@code false}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayOpenApiSyncRepository.markFailure(syncId, expectedRevision,
     * nextState, errorCode, errorMessage, nextRetryAt, now)}。
     * @param syncId 参数 行标识；parameter row identifier。
     * @param expectedRevision 参数 调用方观察到的修订；parameter revision observed by the caller。
     * @param nextState 参数 失败或终态；parameter failure or terminal state。
     * @param errorCode 参数 有界稳定错误码；parameter bounded stable error code。
     * @param errorMessage 参数 有界安全运维信息；parameter bounded safe operator message。
     * @param nextRetryAt 参数 重试时刻或 null；parameter retry timestamp, or {@code null}。
     * @param now 参数 迁移时刻；parameter transition timestamp。
     * @return 返回 markFailure 的处理结果；returns {@code true} when one row was updated.
     */
    boolean markFailure(
            @NotBlank String syncId,
            long expectedRevision,
            GatewayOpenApiSyncStateEnum nextState,
            @NotBlank String errorCode,
            @NotBlank String errorMessage,
            Instant nextRetryAt,
            Instant now);
}
