package top.egon.cola.component.yuheng.admin.release.repository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.release.domain.bo.GatewayChunkCleanupCandidateBO;
import top.egon.cola.component.yuheng.admin.release.domain.bo.GatewayReleasePublicationBO;
import top.egon.cola.component.yuheng.admin.release.domain.enums.GatewayPublicationStatusEnum;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 发布编排持久化端口：领域服务依赖的面向业务对象的发布操作记录读写契约。
 * English summary: Persistence port for release orchestration used by domain services in terms of business objects.
 * <p>
 * 端口方法集合按调用方（发布协调器、分块回收器与草稿运行时投影）实际调用的集合梳理，只暴露业务语义，
 * 不暴露技术主键、行模型、DAO 或查询链。
 * English summary: The method set mirrors exactly what callers (the publication coordinator, the chunk garbage
 * collector and the runtime projection) invoke; no technical id, row model, DAO or query chain is exposed.</p>
 * <p>
 * 端口以 {@code @Validated} 声明载体与不透明标识的 Bean Validation；实现侧通过表级受守卫仓储与 MapStruct 转换器完成读写。
 * English summary: The port declares Bean Validation on carriers and opaque identifiers; implementations use the
 * guarded table repositories and MapStruct converters.</p>
 */
@Validated
public interface GatewayReleasePublicationRepository {

    /**
     * 登记一次发布尝试的全部操作。
     * English summary: Registers all operations for one release attempt.
     * <p>
     * 用法 / Usage: 同一尝试的操作集整体写入，实现侧在同一事务内提交，避免半个操作集可见。
     * English summary: The operation set for one attempt is written as a whole inside a single transaction.</p>
     *
     * @param operations 发布操作集合 / publication operations
     */
    void insertAll(@Valid List<GatewayReleasePublicationBO> operations);

    /**
     * 按尝试编号读取该尝试的全部操作，含完整文档正文。
     * English summary: Reads every operation of an attempt, including full document payloads.
     *
     * @param releaseId 发布 ID / release id
     * @param attemptNo 尝试序号 / attempt number
     * @return 按阶段顺序排列的操作列表 / operations ordered by phase order
     */
    List<GatewayReleasePublicationBO> findAttempt(@NotBlank String releaseId, int attemptNo);

    /**
     * 读取发布阶段元数据而不加载历史完整文档。
     * English summary: Reads publication metadata without loading historical full documents.
     * <p>
     * 用法 / Usage: 默认委托 {@link #findAttempt}；列投影语义在受守卫边界内由全行读取近似（守卫边界会校验每一列），
     * 因此实现不再单独投影，只保持调用方可见的返回值一致。
     * English summary: Delegates to {@link #findAttempt} by default; the column projection is approximated by a
     * full-row read because the guarded boundary validates every column, so only the caller-visible result stays the
     * same.</p>
     *
     * @param releaseId 发布 ID / release id
     * @param attemptNo 尝试序号 / attempt number
     * @return 按阶段顺序排列的元数据列表 / metadata ordered by phase order
     */
    default List<GatewayReleasePublicationBO> findAttemptMetadata(
            String releaseId,
            int attemptNo) {
        return findAttempt(releaseId, attemptNo);
    }

    /**
     * 读取一个发布阶段的完整内容。
     * English summary: Reads the full content for one publication phase.
     * <p>
     * 用法 / Usage: 默认从 {@link #findAttempt} 结果中按阶段顺序过滤取首个，与被替换的历史实现语义一致。
     * English summary: By default filters the {@link #findAttempt} result by phase order and takes the first, matching
     * the implementation it replaces.</p>
     *
     * @param releaseId  发布 ID / release id
     * @param attemptNo  尝试序号 / attempt number
     * @param phaseOrder 阶段序号 / phase order
     * @return 匹配的发布阶段 / matching publication phase
     */
    default Optional<GatewayReleasePublicationBO> findOperation(
            String releaseId,
            int attemptNo,
            int phaseOrder) {
        return findAttempt(releaseId, attemptNo).stream()
                .filter(operation -> operation.getPhaseOrder() == phaseOrder)
                .findFirst();
    }

    /**
     * 查询该尝试内第一个尚未成功的操作。
     * English summary: Queries the first not-yet-successful operation of an attempt.
     *
     * @param releaseId 发布 ID / release id
     * @param attemptNo 尝试序号 / attempt number
     * @return 待执行操作 / operation to run
     */
    Optional<GatewayReleasePublicationBO> nextIncomplete(
            @NotBlank String releaseId,
            int attemptNo);

    /**
     * 查询到期且可安全清理的不可变分块。
     * English summary: Queries due immutable chunks that can be cleaned safely.
     * <p>
     * 用法 / Usage: 只有当前无待发布版本、无活跃草稿依赖且已有同作用域后继激活成功的旧分块才会返回。
     * English summary: Only legacy chunks with no pending release in the group, no dependent active draft and a
     * successfully activated successor in the same scope are returned.</p>
     *
     * @param successorActivatedBefore 后继激活的截止时间 / latest allowed successor activation instant
     * @return 待清理分块 / chunks to clean
     */
    List<GatewayChunkCleanupCandidateBO> findChunkCleanupCandidates(
            Instant successorActivatedBefore);

    /**
     * 记录已解析的文档正文、期望版本并把操作推进到 RESOLVED。
     * English summary: Records the resolved document content and expected version and advances the operation to RESOLVED.
     * <p>
     * 用法 / Usage: 仅推进尚未成功的操作；已成功的操作不允许再次解析，未命中任何行时抛出异常。
     * English summary: Only not-yet-successful operations advance; a successful operation cannot be resolved again and a
     * write matching no row raises.</p>
     *
     * @param changeId        外部变更 ID / external change id
     * @param expectedVersion 期望配置版本 / expected configuration version
     * @param documentContent 文档正文 / document content
     * @param now             解析时间 / resolution time
     */
    void resolveDocument(
            @NotBlank String changeId,
            long expectedVersion,
            String documentContent,
            Instant now);

    /**
     * 把操作标记为已提交到外部引擎。
     * English summary: Marks an operation as submitted to the external engine.
     * <p>
     * 用法 / Usage: 只有 RESOLVED 且已带期望版本的操作可提交，未命中时抛出异常。
     * English summary: Only a RESOLVED operation carrying an expected version can be submitted; a miss raises.</p>
     *
     * @param changeId 外部变更 ID / external change id
     * @param now      提交时间 / submission time
     */
    void markSubmitted(@NotBlank String changeId, Instant now);

    /**
     * 记录外部引擎操作结果并推进发布游标。
     * English summary: Records an external engine result and advances the release cursor.
     * <p>
     * 用法 / Usage: 只接受终态结果，成功结果必须带目标版本；非终态或越界的当前状态抛出异常，未命中行同样抛出。
     * English summary: Only terminal results are accepted and a successful result requires a target version; a
     * non-terminal status or a state outside the accepted set raises, as does a write matching no row.</p>
     *
     * @param changeId      外部变更 ID / external change id
     * @param targetVersion 引擎目标版本 / engine target version
     * @param status        外部结果状态 / external result status
     * @param errorCode     错误码 / error code
     * @param errorMessage  错误说明 / error message
     * @param now           结果时间 / result time
     */
    void markResult(
            @NotBlank String changeId,
            Long targetVersion,
            GatewayPublicationStatusEnum status,
            String errorCode,
            String errorMessage,
            Instant now);

    /**
     * 标记分块已成功清理。
     * English summary: Marks a chunk as cleaned successfully.
     * <p>
     * 用法 / Usage: 仅命中已是 SUCCESS 且带目标版本的 CHUNK 阶段，未命中时抛出异常。
     * English summary: Only a CHUNK phase already SUCCESS with a target version is matched; a miss raises.</p>
     *
     * @param changeId 外部变更 ID / external change id
     * @param now      清理时间 / cleanup time
     */
    void markChunkCleaned(@NotBlank String changeId, Instant now);
}
