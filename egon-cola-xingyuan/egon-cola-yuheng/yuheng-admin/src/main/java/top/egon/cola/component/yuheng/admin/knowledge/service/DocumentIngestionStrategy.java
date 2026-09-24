package top.egon.cola.component.yuheng.admin.knowledge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.rag.api.RagExtractionService;
import top.egon.cola.component.rag.chunk.RagChunkingStrategy;
import top.egon.cola.component.rag.chunk.RagChunkingStrategyEnum;
import top.egon.cola.component.rag.chunk.RagChunkingStrategyFactory;
import top.egon.cola.component.rag.common.exception.RagExtractorMissingException;
import top.egon.cola.component.rag.model.ExtractedDocumentBO;
import top.egon.cola.component.rag.model.RagChunkBO;
import top.egon.cola.component.rag.model.RagChunkingConfigDTO;
import top.egon.cola.component.rag.model.RagExtractionCommand;
import top.egon.cola.component.yuheng.admin.config.properties.KnowledgeProperties;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeBaseBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeChunkBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeDocumentBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeDocumentRevisionBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeJobBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeMemberDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobStageEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobStatusEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobTypeEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeMemberRoleEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeRevisionStatusEnum;
import top.egon.cola.component.yuheng.admin.knowledge.repository.KnowledgeRepository;

/**
 * 中文说明：{@code DocumentIngestionStrategy} 是 {@code DOCUMENT_INGEST} 类型的唯一策略实现
 * （Rule 9：类型路由只经 {@code knowledgeJobStrategyRegistry}，任何地方都不得对
 * {@link KnowledgeJobTypeEnum} 写 switch/if-else），完成"解析 → 切分 → 本地嵌入 → 暂存 → CAS 发布"全链路。
 * 固定顺序是：从作业冻结的 payload 取来源 revision → 读权威 revision/document/KB 并复核
 * （revision 属于本 KB 且仍为 {@code STAGING}、文档未删且 {@code resourceId} 与 revision 的 documentId 一致、
 * 提交者仍是 KB 成员、嵌入空间与维度仍等于 KB 冻结值、作业行仍是 {@code RUNNING} 且租约令牌仍等于本次认领值）→
 * <b>复用</b>已提交的 RAG 抽取/切分 Bean（{@code ragExtractionService} 与 {@code ragChunkingStrategyFactory}，
 * 本类不实现任何分词器）→ 生成 {@code chunkIndex} 有序、每块带 SHA-256 {@code contentHash} 的确定性分块 →
 * 按 ≤{@code min(64, embedBatchSize)} 的批次经 {@code knowledgeModelClientServiceImpl} 取本地向量并逐条复验 →
 * {@code stageChunks} → <b>只有这时</b>才用观察到的文档 {@code expectedRevision} 做 {@code activateRevision} CAS。
 * English summary: {@code DocumentIngestionStrategy} is the only strategy for {@code DOCUMENT_INGEST} (Rule 9: type
 * routing happens exclusively through {@code knowledgeJobStrategyRegistry}, never a switch or if-else over
 * {@link KnowledgeJobTypeEnum}) and performs the whole "parse → chunk → local embedding → staging → CAS publish" chain.
 * The order is fixed: take the source revision from the job's frozen payload, load and re-verify the authoritative
 * revision/document/knowledge-base rows (the revision belongs to this base and is still {@code STAGING}, the document is
 * alive with a {@code resourceId} matching the revision's document id, the submitting actor is still a member, the
 * embedding space and dimensions still equal the base's frozen values, and the job row is still {@code RUNNING} under the
 * token acquired by this claim), then <b>reuse</b> the committed RAG extraction and chunking beans
 * ({@code ragExtractionService} and {@code ragChunkingStrategyFactory} — this class implements no tokenizer), derive
 * deterministic chunks with an ordered {@code chunkIndex} and a per-chunk SHA-256 {@code contentHash}, fetch local
 * vectors in batches of at most {@code min(64, embedBatchSize)} through {@code knowledgeModelClientServiceImpl} and
 * re-verify them entry by entry, call {@code stageChunks}, and only then run the {@code activateRevision} CAS with the
 * observed document {@code expectedRevision}.
 *
 * 用法 / Usage: 由 {@code KnowledgeJobWorker} 经注册表以 bean 名 {@code documentIngestionStrategy} 调用
 * {@code run(job, leaseToken)}；返回值是<b>交给调用方租约 CAS 写回</b>的终态载体，本类从不自发布、从不绕过
 * {@code publishTerminal}。失败语义固定：嵌入或暂存失败按可重试错误处理——在 {@code maxAttempts}（默认 3 次总尝试）
 * 之内以 {@code RETRY_WAIT} 排程（{@code retryDelays} 5s/30s），到顶或不可重试错误落 {@code FAILED}，
 * 来源/成员/租约/版本发生变化落 {@code STALE}；<b>任何一条失败路径都不会改动旧的 active revision</b>，
 * 因为活动指针只在向量齐备且 {@code stageChunks} 成功之后才切换。模型调用全部发生在事务与锁之外
 * （本类不标 {@code @Transactional}，每次仓储写入都是端口自己的短 CAS），嵌入永远只走 LOCAL alias、
 * 没有云端兜底；日志只记 kbId/revisionId/jobId/条数/阶段/耗时/机器码，绝不记原文、提示词、向量或字节。
 * / The worker invokes {@code run(job, leaseToken)} through the registry by bean name
 * {@code documentIngestionStrategy}; the return value is the terminal carrier for <b>the caller's lease CAS</b> — this
 * class never publishes on its own and never bypasses {@code publishTerminal}. Failure semantics are fixed: an embedding
 * or staging failure is treated as retryable and scheduled as {@code RETRY_WAIT} within {@code maxAttempts} (three total
 * attempts) using {@code retryDelays} of 5s/30s, an exhausted ceiling or a non-retryable error lands on {@code FAILED},
 * and a changed source, membership, lease or revision lands on {@code STALE}; <b>no failure path ever touches the previous
 * active revision</b>, since the pointer moves only once every vector is present and {@code stageChunks} succeeded. All
 * model calls sit outside any transaction or lock (this class carries no {@code @Transactional} and every repository write
 * is the port's own short CAS), embedding is LOCAL-only with no cloud fallback, and logs carry only
 * kbId/revisionId/jobId/counts/stage/latency/machine codes — never text, prompts, vectors or bytes.
 */
@Slf4j
@Validated
@Service("documentIngestionStrategy")
@RequiredArgsConstructor
public class DocumentIngestionStrategy implements KnowledgeJobStrategy {

    /** 中文说明：{@code yuheng.knowledge} 配置载体由 {@code @EnableConfigurationProperties} 注册，bean 名即 {@code prefix-全限定类名}。 English summary: the {@code yuheng.knowledge} holder is registered by {@code @EnableConfigurationProperties}, whose generated bean name is {@code prefix-FQCN}. */
    private static final String KNOWLEDGE_PROPERTIES_BEAN =
            "yuheng.knowledge-top.egon.cola.component.yuheng.admin.config.properties.KnowledgeProperties";

    /** 中文说明：本步使用的稳定机器码，只写进 {@code error_code} 列，不含上游正文。 English summary: the stable machine codes this step uses, written only into the {@code error_code} column and never carrying upstream text. */
    private static final String MODEL_UNAVAILABLE = "KNOWLEDGE_MODEL_UNAVAILABLE";
    private static final String RESOURCE_NOT_FOUND = "KNOWLEDGE_RESOURCE_NOT_FOUND";
    private static final String REVISION_CONFLICT = "KNOWLEDGE_REVISION_CONFLICT";
    private static final String FORBIDDEN = "KNOWLEDGE_FORBIDDEN";
    private static final String VALIDATION_FAILED = "KNOWLEDGE_VALIDATION_FAILED";
    private static final String MEDIA_UNSUPPORTED = "KNOWLEDGE_MEDIA_UNSUPPORTED";
    private static final String DEPENDENCY_UNAVAILABLE = "YUHENG_DEPENDENCY_UNAVAILABLE";

    /** 中文说明：嵌入批次的协议硬上限，与端口 {@code @Size(max = 64)} 同值；配置再大也不会越过去。 English summary: the protocol ceiling of an embedding batch, the same 64 the port declares; a larger configuration never overrides it. */
    private static final int MAX_EMBED_BATCH = 64;

    /** 中文说明：{@code chunk_index} 的库内合法上界（CHECK 0..9999），冻结配置再大也只能截到这里。 English summary: the stored {@code chunk_index} ceiling (CHECK 0..9999); an oversized frozen cap is cut back to it. */
    private static final int MAX_CHUNK_INDEX = 9_999;

    /** 中文说明：来源 revision id 在作业 payload 中的固定键名（重索引沿用 {@code sourceRevisionId}）。 English summary: the fixed payload key holding the source revision id (a reindex reuses {@code sourceRevisionId}). */
    private static final String REVISION_ID_FIELD = "revisionId";
    private static final String SOURCE_REVISION_ID_FIELD = "sourceRevisionId";

    /** 中文说明：{@code chunking_config} 冻结 jsonb 的固定键名，逐字对应 {@code {strategy,chunkSize,overlap,maxChunks}}。 English summary: the fixed keys of the frozen {@code chunking_config} jsonb, verbatim {@code {strategy,chunkSize,overlap,maxChunks}}. */
    private static final String STRATEGY_KEY = "strategy";
    private static final String CHUNK_SIZE_KEY = "chunkSize";
    private static final String OVERLAP_KEY = "overlap";
    private static final String MAX_CHUNKS_KEY = "maxChunks";

    @Qualifier("knowledgeRepository")
    private final KnowledgeRepository knowledgeRepository;

    @Qualifier("knowledgeModelClientServiceImpl")
    private final KnowledgeModelClientService knowledgeModelClientService;

    @Qualifier("ragExtractionService")
    private final RagExtractionService ragExtractionService;

    @Qualifier("ragChunkingStrategyFactory")
    private final RagChunkingStrategyFactory ragChunkingStrategyFactory;

    @Qualifier(KNOWLEDGE_PROPERTIES_BEAN)
    private final KnowledgeProperties knowledgeProperties;

    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    @Qualifier("knowledgeClock")
    private final Clock clock;

    /**
     * 中文说明：执行 type 操作；本策略只认领 {@code DOCUMENT_INGEST}，是注册表里的唯一键，重复认领由
     * {@code KnowledgeConfiguration} 在启动期以 {@code IllegalStateException} 拒绝。
     * English summary: Executes the type operation; this strategy claims only {@code DOCUMENT_INGEST}, its single registry
     * key, a duplicate claim being refused at startup by {@code KnowledgeConfiguration} with an
     * {@code IllegalStateException}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code documentIngestionStrategy.type()}；常量返回，不依赖注入状态。
     * @return 返回 {@link KnowledgeJobTypeEnum#DOCUMENT_INGEST}；returns {@link KnowledgeJobTypeEnum#DOCUMENT_INGEST}.
     */
    @Override
    public KnowledgeJobTypeEnum type() {
        return KnowledgeJobTypeEnum.DOCUMENT_INGEST;
    }

    /**
     * 中文说明：执行 run 操作；按类注释的固定顺序完成摄取，并把结论封装成终态载体返回。
     * 三条不变式贯穿始终：① 发布前的每一项事实都用权威行复核（来源 revision、KB 成员、冻结空间/维度、
     * 文档 revision、作业租约），任一变化即 {@code STALE} 而不是继续写入；② 向量不齐备或暂存不完整时
     * 绝不切换活动指针，因此旧 active revision 在部分失败下原样保留；③ 只有明确可重试的错误才在尝试上限内
     * 排 {@code RETRY_WAIT}，其余落 {@code FAILED}，影响 0 行永远按失败处理。
     * English summary: Executes the run operation; it performs ingestion in the class-documented order and returns the
     * conclusion as a terminal carrier. Three invariants hold throughout: (1) every fact is re-verified against the
     * authoritative rows before publication (source revision, knowledge-base membership, frozen space and dimensions,
     * document revision, job lease) and any change is {@code STALE} rather than another write; (2) the active pointer never
     * moves while vectors are missing or staging is incomplete, so a partial failure keeps the old active revision intact;
     * (3) only explicitly retryable errors schedule {@code RETRY_WAIT} within the attempt ceiling, everything else lands on
     * {@code FAILED}, and a zero-row effect is always treated as failure.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code documentIngestionStrategy.run(job, leaseToken)}；
     * 由 worker 在持租约的作业上调用，返回载体只经 {@code publishTerminal} 写回。
     * @param job 参数 已认领并持租约的作业载体；parameter the claimed job carrier holding a lease.
     * @param leaseToken 参数 本次认领获得的租约令牌；parameter the lease token acquired by this claim.
     * @return 返回 待经租约 CAS 写回的终态作业载体；returns the terminal job carrier to write back through the lease CAS.
     */
    @Override
    public KnowledgeJobBO run(KnowledgeJobBO job, long leaseToken) {
        long startedAt = System.nanoTime();
        try {
            return ingest(job, leaseToken, startedAt);
        } catch (CommonException refused) {
            boolean retryable = MODEL_UNAVAILABLE.equals(refused.getStatus()) || refused.isRetryable();
            log.warn("document ingestion refused job={} kb={} revision={} stage={} code={} retryable={} latencyMs={}",
                    job.getId(), job.getKbId(), job.getResourceId(), job.getStage(), refused.getStatus(), retryable,
                    elapsedMillis(startedAt));
            return failure(job, job.getStage(), refused.getStatus(), retryable);
        } catch (RuntimeException unexpected) {
            log.warn("document ingestion failed unexpectedly job={} kb={} revision={} stage={} latencyMs={}",
                    job.getId(), job.getKbId(), job.getResourceId(), job.getStage(), elapsedMillis(startedAt));
            return failure(job, job.getStage(), DEPENDENCY_UNAVAILABLE, false);
        }
    }

    /** 中文说明：摄取主线；每一步都用权威行复核，任何"事实已变"的分支都返回 {@code STALE}，任何"依赖/写入未完成"的分支都经 {@link #failure} 决定 {@code RETRY_WAIT} 或 {@code FAILED}。 English summary: the ingestion main line; every step re-verifies against the authoritative rows, a branch meaning "a fact changed" answers {@code STALE} while one meaning "a dependency or write did not complete" goes through {@link #failure} to decide {@code RETRY_WAIT} or {@code FAILED}. */
    private KnowledgeJobBO ingest(KnowledgeJobBO job, long leaseToken, long startedAt) {
        String revisionId = sourceRevisionId(job);
        if (revisionId == null) {
            log.warn("document ingestion has no frozen source revision job={} kb={}", job.getId(), job.getKbId());
            return failure(job, stage(job), VALIDATION_FAILED, false);
        }
        KnowledgeDocumentRevisionBO revision = knowledgeRepository.findRevision(job.getKbId(), revisionId).orElse(null);
        if (revision == null) {
            return stale(job, stage(job), RESOURCE_NOT_FOUND);
        }
        if (revision.getStatus() != KnowledgeRevisionStatusEnum.STAGING) {
            log.warn("document ingestion source is not staging job={} revision={} status={}",
                    job.getId(), revisionId, revision.getStatus());
            return stale(job, stage(job), REVISION_CONFLICT);
        }
        if (StringUtils.isBlank(revision.getDocumentId())
                || StringUtils.isNotBlank(job.getResourceId())
                && !StringUtils.equals(job.getResourceId(), revision.getDocumentId())) {
            return stale(job, stage(job), RESOURCE_NOT_FOUND);
        }
        Integer dimensions = revision.getDimensions();
        if (StringUtils.isBlank(revision.getEmbeddingSpaceId()) || dimensions == null || dimensions <= 0) {
            return stale(job, stage(job), REVISION_CONFLICT);
        }
        KnowledgeDocumentBO document = knowledgeRepository
                .findDocument(job.getKbId(), revision.getDocumentId()).orElse(null);
        if (document == null) {
            return stale(job, stage(job), RESOURCE_NOT_FOUND);
        }
        if (revisionId.equals(document.getActiveRevisionId())) {
            log.info("document ingestion already published job={} kb={} revision={} latencyMs={}",
                    job.getId(), job.getKbId(), revisionId, elapsedMillis(startedAt));
            return terminal(job, KnowledgeJobStatusEnum.SUCCEEDED, KnowledgeJobStageEnum.DONE, null,
                    publicationResult(revision.getDocumentId(), revisionId, revision.getChunkCount()));
        }
        KnowledgeBaseBO base = knowledgeRepository.findBase(job.getKbId()).orElse(null);
        if (base == null) {
            return stale(job, stage(job), RESOURCE_NOT_FOUND);
        }
        if (!StringUtils.equals(revision.getEmbeddingSpaceId(), base.getEmbeddingSpaceId())
                || base.getDimensions() == null || !base.getDimensions().equals(dimensions)) {
            log.warn("document ingestion frozen space diverged job={} kb={} revision={} base={}",
                    job.getId(), job.getKbId(), revision.getEmbeddingSpaceId(), base.getEmbeddingSpaceId());
            return stale(job, stage(job), REVISION_CONFLICT);
        }
        if (!isMember(base, job.getActorId())) {
            log.warn("document ingestion submitter no longer member job={} kb={} stage={}",
                    job.getId(), job.getKbId(), stage(job));
            return stale(job, stage(job), FORBIDDEN);
        }
        KnowledgeJobStageEnum parseStage = KnowledgeJobStageEnum.PARSE;
        if (!renewLease(job, leaseToken)) {
            return stale(job, parseStage, REVISION_CONFLICT);
        }
        String extracted = extractText(job, revision);
        if (StringUtils.isBlank(extracted)) {
            log.warn("document ingestion extracted nothing job={} kb={} revision={}",
                    job.getId(), job.getKbId(), revisionId);
            return failure(job, parseStage, VALIDATION_FAILED, false);
        }
        long revisionRevision = revision.getRevision();
        KnowledgeDocumentRevisionBO parsed = copyRevision(revision).setExtractedText(extracted);
        if (!knowledgeRepository.updateRevision(parsed, revisionRevision)) {
            return stale(job, parseStage, REVISION_CONFLICT);
        }
        revisionRevision = revisionRevision + 1;
        List<RagChunkBO> pieces = chunk(job, extracted, revision);
        if (pieces.isEmpty()) {
            log.warn("document ingestion produced no chunk job={} kb={} revision={}",
                    job.getId(), job.getKbId(), revisionId);
            return failure(job, parseStage, VALIDATION_FAILED, false);
        }
        KnowledgeJobStageEnum embedStage = KnowledgeJobStageEnum.EMBED;
        List<String> texts = pieces.stream().map(RagChunkBO::content).toList();
        List<float[]> vectors = embed(kbId(job), revisionId, texts, dimensions.intValue());
        List<KnowledgeChunkBO> chunks = buildChunks(job, revision, pieces, vectors, dimensions.intValue());
        KnowledgeJobStageEnum publishStage = KnowledgeJobStageEnum.PUBLISH;
        if (!renewLease(job, leaseToken)) {
            return stale(job, embedStage, REVISION_CONFLICT);
        }
        int staged = knowledgeRepository.stageChunks(kbId(job), revisionId, chunks);
        if (staged != chunks.size()) {
            log.warn("document ingestion staging incomplete job={} kb={} revision={} expected={} staged={}",
                    job.getId(), job.getKbId(), revisionId, chunks.size(), staged);
            return failure(job, embedStage, MODEL_UNAVAILABLE, true);
        }
        KnowledgeDocumentRevisionBO ready = copyRevision(revision)
                .setExtractedText(extracted)
                .setStatus(KnowledgeRevisionStatusEnum.READY)
                .setChunkCount(chunks.size());
        if (!knowledgeRepository.updateRevision(ready, revisionRevision)) {
            return stale(job, publishStage, REVISION_CONFLICT);
        }
        if (!renewLease(job, leaseToken)) {
            return stale(job, publishStage, REVISION_CONFLICT);
        }
        if (!knowledgeRepository.activateRevision(
                kbId(job), revision.getDocumentId(), revisionId, document.getRevision())) {
            log.warn("document ingestion publication lost job={} kb={} revision={} expectedRevision={}",
                    job.getId(), job.getKbId(), revisionId, document.getRevision());
            return stale(job, publishStage, REVISION_CONFLICT);
        }
        log.info("document ingestion published job={} kb={} revision={} chunks={} dimensions={} latencyMs={}",
                job.getId(), job.getKbId(), revisionId, chunks.size(), dimensions, elapsedMillis(startedAt));
        return terminal(job, KnowledgeJobStatusEnum.SUCCEEDED, KnowledgeJobStageEnum.DONE, null,
                publicationResult(revision.getDocumentId(), revisionId, chunks.size()));
    }

    /** 中文说明：从作业 payload 取来源 revision id：只认 {@code revisionId} 与 {@code sourceRevisionId} 两个文本键，且必须是十进制字符串 id；缺失或形态不合返回 {@code null} 由调用方按校验失败收束，绝不猜"最新 revision"。 English summary: reads the source revision id out of the job payload: only the textual {@code revisionId} and {@code sourceRevisionId} keys count and the value must be a decimal-string id; a missing or malformed one returns {@code null} so the caller closes it as a validation failure rather than guessing "the latest revision". */
    private String sourceRevisionId(KnowledgeJobBO job) {
        JsonNode payload = job.getPayload();
        if (payload == null || !payload.isObject()) {
            return null;
        }
        String candidate = payload.path(REVISION_ID_FIELD).isTextual()
                ? payload.path(REVISION_ID_FIELD).asText()
                : payload.path(SOURCE_REVISION_ID_FIELD).isTextual()
                        ? payload.path(SOURCE_REVISION_ID_FIELD).asText()
                        : null;
        candidate = StringUtils.trimToNull(candidate);
        return candidate != null && candidate.matches("^[1-9][0-9]{0,19}$") ? candidate : null;
    }

    /** 中文说明：租约续期即所有权证明：{@code heartbeat} 返回 false 意味着令牌已推进、租约被接管或作业已终态，本次执行必须放弃而不是继续写。 English summary: renewing the lease is the ownership proof: a {@code false} from {@code heartbeat} means the token advanced, the lease was taken over or the job is terminal, so this execution must abandon rather than keep writing. */
    private boolean renewLease(KnowledgeJobBO job, long leaseToken) {
        Duration lease = knowledgeProperties.getLease();
        if (lease == null || lease.isZero() || lease.isNegative()
                || !knowledgeRepository.heartbeat(job.getId(), leaseToken, clock.instant().plus(lease))) {
            log.warn("document ingestion lease no longer held job={} kb={} leaseToken={}",
                    job.getId(), job.getKbId(), leaseToken);
            return false;
        }
        return true;
    }

    /** 中文说明：复用已提交的 RAG 抽取 Bean 解析原始字节；抽取器缺失是"解析类型未支持"（不可重试），其余抽取失败按依赖不可用（可重试）抛出，日志与错误都不携带正文。 English summary: reuses the committed RAG extraction bean to parse the raw bytes; a missing extractor means unsupported media (non-retryable) while any other extraction failure raises an unavailable dependency (retryable), and neither the log nor the error carries content. */
    private String extractText(KnowledgeJobBO job, KnowledgeDocumentRevisionBO revision) {
        byte[] raw = revision.getRawBytes();
        if (raw == null || raw.length == 0) {
            log.warn("document ingestion source bytes absent job={} kb={} revision={} byteCount={}",
                    job.getId(), job.getKbId(), revision.getId(), revision.getByteCount());
            return null;
        }
        try (InputStream content = new ByteArrayInputStream(raw)) {
            ExtractedDocumentBO extracted = ragExtractionService.extract(new RagExtractionCommand(
                    revision.getFileName(), revision.getMediaType(), content));
            return extracted.text();
        } catch (RagExtractorMissingException unsupported) {
            log.warn("document ingestion media unsupported job={} kb={} revision={}",
                    job.getId(), job.getKbId(), revision.getId());
            throw new CommonException(415, MEDIA_UNSUPPORTED, "The stored document has no registered text extractor");
        } catch (CommonException refused) {
            throw new CommonException(503, MODEL_UNAVAILABLE, "The stored document could not be extracted");
        } catch (java.io.IOException unreadable) {
            log.warn("document ingestion source unreadable job={} kb={} revision={}",
                    job.getId(), job.getKbId(), revision.getId());
            throw new CommonException(503, MODEL_UNAVAILABLE, "The stored document bytes could not be read");
        }
    }

    /** 中文说明：复用已提交的 RAG 切分策略产确定性分块：策略与预算只取 revision 冻结的 {@code chunking_config}（缺失键回落到 {@code yuheng.knowledge.chunk-size/overlap}），本类不实现任何分词逻辑；{@code maxChunks} 与 {@code chunk_index} 的库内上界共同封顶。 English summary: reuses the committed RAG chunking strategy for deterministic chunks: the strategy and its budget come only from the revision's frozen {@code chunking_config} (missing keys falling back to {@code yuheng.knowledge.chunk-size/overlap}) and this class implements no tokenization; {@code maxChunks} and the stored {@code chunk_index} ceiling cap the result together. */
    private List<RagChunkBO> chunk(KnowledgeJobBO job, String extracted, KnowledgeDocumentRevisionBO revision) {
        JsonNode config = revision.getChunkingConfig();
        RagChunkingStrategyEnum strategy = chunkingStrategy(config);
        RagChunkingConfigDTO budget = new RagChunkingConfigDTO(
                strategy,
                config == null ? knowledgeProperties.getChunkSize() : config.path(CHUNK_SIZE_KEY).asInt(knowledgeProperties.getChunkSize()),
                config == null ? knowledgeProperties.getChunkOverlap() : config.path(OVERLAP_KEY).asInt(knowledgeProperties.getChunkOverlap()),
                1,
                null);
        ExtractedDocumentBO document = new ExtractedDocumentBO(
                extracted, revision.getFileName(), revision.getMediaType(), java.util.Map.of());
        List<RagChunkBO> pieces = ragChunkingStrategyFactory.resolve(strategy).split(document, budget);
        int maxChunks = config == null ? 0 : config.path(MAX_CHUNKS_KEY).asInt(0);
        int cap = maxChunks > 0 ? Math.min(maxChunks, MAX_CHUNK_INDEX + 1) : MAX_CHUNK_INDEX + 1;
        return pieces.size() <= cap ? pieces : new ArrayList<>(pieces.subList(0, cap));
    }

    /** 中文说明：解析冻结的策略名，未知或空白一律回落到 {@code TOKEN}（与 {@code KnowledgeProperties} 默认口径一致），绝不即兴发明新策略。 English summary: resolves the frozen strategy name, an unknown or blank value falling back to {@code TOKEN} (the {@code KnowledgeProperties} default) instead of an improvised strategy. */
    private RagChunkingStrategyEnum chunkingStrategy(JsonNode config) {
        String name = config == null ? null : StringUtils.trimToNull(config.path(STRATEGY_KEY).asText(null));
        if (name == null) {
            return RagChunkingStrategyEnum.TOKEN;
        }
        try {
            return RagChunkingStrategyEnum.valueOf(name);
        } catch (IllegalArgumentException unknown) {
            log.warn("document ingestion frozen chunking strategy unknown strategy={}", name);
            return RagChunkingStrategyEnum.TOKEN;
        }
    }

    /** 中文说明：按批次经本地嵌入 alias 取向量并逐批复验（条数等于本批文本数、每条长度等于冻结维度、分量有限、向量非全零）；任何不合都抛 {@code 503 KNOWLEDGE_MODEL_UNAVAILABLE}，由 {@link #run} 判可重试，绝不补零、截断或改道云端。 English summary: fetches vectors through the LOCAL embedding alias batch by batch and re-verifies every batch (the count equals the batch size, each vector is exactly the frozen width, its components are finite and the vector is not all zero); any breach raises {@code 503 KNOWLEDGE_MODEL_UNAVAILABLE} that {@link #run} classifies as retryable, and nothing is padded, truncated or rerouted to cloud. */
    private List<float[]> embed(String kbId, String revisionId, List<String> texts, int dimensions) {
        int batchSize = Math.min(MAX_EMBED_BATCH, Math.max(1, knowledgeProperties.getEmbedBatchSize()));
        List<float[]> vectors = new ArrayList<>(texts.size());
        for (int offset = 0; offset < texts.size(); offset += batchSize) {
            List<String> batch = texts.subList(offset, Math.min(offset + batchSize, texts.size()));
            List<float[]> served = knowledgeModelClientService.embed(kbId, batch, dimensions);
            if (served == null || served.size() != batch.size()) {
                log.warn("document embedding batch size wrong kb={} revision={} expected={} actual={} stage=EMBED",
                        kbId, revisionId, batch.size(), served == null ? -1 : served.size());
                throw new CommonException(503, MODEL_UNAVAILABLE,
                        "The embedding batch returned a different number of vectors than the submitted texts");
            }
            for (float[] vector : served) {
                requireVector(kbId, revisionId, vector, dimensions);
            }
            vectors.addAll(served);
            log.info("document embedding batch served kb={} revision={} entries={} dimensions={}",
                    kbId, revisionId, batch.size(), dimensions);
        }
        return vectors;
    }

    /** 中文说明：单条向量的形状复验：长度、有限性与非全零；日志只回维度与布尔量，绝不回任何分量。 English summary: the re-verification of one vector: length, finiteness and a non-zero norm; the log reports only the width and a boolean, never a component. */
    private void requireVector(String kbId, String revisionId, float[] vector, int dimensions) {
        if (vector == null || vector.length != dimensions) {
            log.warn("document embedding vector width rejected kb={} revision={} expected={} actual={}",
                    kbId, revisionId, dimensions, vector == null ? -1 : vector.length);
            throw new CommonException(503, MODEL_UNAVAILABLE,
                    "An embedding vector does not match the knowledge base's frozen dimensions");
        }
        boolean nonZero = false;
        for (float component : vector) {
            if (!Float.isFinite(component)) {
                log.warn("document embedding vector not finite kb={} revision={}", kbId, revisionId);
                throw new CommonException(503, MODEL_UNAVAILABLE,
                        "An embedding vector carries a non-finite component");
            }
            nonZero |= component != 0f;
        }
        if (!nonZero) {
            log.warn("document embedding vector is zero kb={} revision={}", kbId, revisionId);
            throw new CommonException(503, MODEL_UNAVAILABLE, "An embedding vector has a zero norm");
        }
    }

    /** 中文说明：把切分结果与向量装配成分块载体：{@code chunkIndex} 从 0 连续编号，{@code contentHash} 是该块 UTF-8 的 SHA-256 小写 hex，嵌入空间/维度取 revision 冻结值，metadata 只保留解析器可信定位。 English summary: assembles the chunk carriers from the pieces and the vectors: {@code chunkIndex} counts up from zero, {@code contentHash} is the lower-case SHA-256 hex of the chunk's UTF-8 bytes, the embedding space and dimensions come from the frozen revision, and metadata keeps only parser-trusted locators. */
    private List<KnowledgeChunkBO> buildChunks(
            KnowledgeJobBO job, KnowledgeDocumentRevisionBO revision, List<RagChunkBO> pieces,
            List<float[]> vectors, int dimensions) {
        List<KnowledgeChunkBO> chunks = new ArrayList<>(pieces.size());
        for (int index = 0; index < pieces.size(); index++) {
            RagChunkBO piece = pieces.get(index);
            String content = piece.content();
            if (StringUtils.isBlank(content)) {
                log.warn("document ingestion produced an empty chunk job={} kb={} index={}",
                        job.getId(), job.getKbId(), index);
                throw new CommonException(422, VALIDATION_FAILED, "Chunking produced an empty chunk");
            }
            ObjectNode metadata = objectMapper.createObjectNode();
            piece.attributes().forEach(metadata::put);
            chunks.add(KnowledgeChunkBO.builder()
                    .kbId(revision.getKbId())
                    .revisionId(revision.getId())
                    .chunkIndex(index)
                    .content(content)
                    .metadata(metadata)
                    .contentHash(DigestUtils.sha256Hex(content.getBytes(StandardCharsets.UTF_8)))
                    .embeddingSpaceId(revision.getEmbeddingSpaceId())
                    .dimensions(dimensions)
                    .embedding(vectors.get(index))
                    .revision(1L)
                    .build());
        }
        return chunks;
    }

    /** 中文说明：成员复核：提交者是 {@code ownerActorId} 或在 {@code members} 内即视为仍在授权范围内；撤销即 {@code STALE}，绝不用缓存绕过撤权。 English summary: the membership re-check: the submitter counts as still authorized when it is the {@code ownerActorId} or appears in {@code members}; a revocation is {@code STALE} and cached rights are never used to bypass one. */
    private boolean isMember(KnowledgeBaseBO base, String actorId) {
        if (StringUtils.isBlank(actorId)) {
            return false;
        }
        if (StringUtils.equals(actorId, base.getOwnerActorId())) {
            return true;
        }
        List<KnowledgeMemberDTO> members = base.getMembers();
        if (members == null) {
            return false;
        }
        return members.stream()
                .filter(java.util.Objects::nonNull)
                .anyMatch(member -> StringUtils.equals(actorId, member.getActorId())
                        && member.getRole() != null
                        && member.getRole() != KnowledgeMemberRoleEnum.READER);
    }

    /** 中文说明：当前阶段的权威值：认领载体已写的阶段优先，缺失按 {@code PARSE} 处理，从不伪造百分比进度。 English summary: the authoritative stage: the stage already on the claimed carrier wins, a missing one is treated as {@code PARSE}, and a fake percentage is never invented. */
    private KnowledgeJobStageEnum stage(KnowledgeJobBO job) {
        KnowledgeJobStageEnum observed = job.getStage();
        return observed == null ? KnowledgeJobStageEnum.PARSE : observed;
    }

    private String kbId(KnowledgeJobBO job) {
        return job.getKbId();
    }

    /** 中文说明：把一次失败映射为 {@code RETRY_WAIT} 或 {@code FAILED}：只有明确可重试的错误、且本次尝试之后仍有 {@code maxAttempts} 预算时才排程（退避取 {@code retryDelays}，按已用尝试数取值），到顶即终态失败；{@code error_code} 只存稳定机器码。 English summary: maps one failure onto {@code RETRY_WAIT} or {@code FAILED}: only an explicitly retryable error with budget left under {@code maxAttempts} is scheduled (the backoff comes from {@code retryDelays}, indexed by the attempts already used), an exhausted ceiling is a terminal failure, and {@code error_code} stores a stable machine code only. */
    private KnowledgeJobBO failure(
            KnowledgeJobBO job, KnowledgeJobStageEnum stage, String code, boolean retryable) {
        int attemptsUsed = job.getAttempt() == null ? 0 : job.getAttempt();
        int maxAttempts = knowledgeProperties.getMaxAttempts();
        List<Duration> delays = knowledgeProperties.getRetryDelays();
        if (retryable && maxAttempts > 0 && attemptsUsed + 1 < maxAttempts && delays != null && !delays.isEmpty()) {
            Duration delay = delays.get(Math.min(attemptsUsed, delays.size() - 1));
            if (delay == null || delay.isNegative()) {
                delay = Duration.ZERO;
            }
            return terminal(job, KnowledgeJobStatusEnum.RETRY_WAIT, stage, code, null)
                    .setNextAttemptAt(clock.instant().plus(delay));
        }
        markRevisionFailed(job);
        return terminal(job, KnowledgeJobStatusEnum.FAILED, stage, code, null);
    }

    /** 中文说明：不可重试失败把 {@code STAGING} 来源 revision 收束为 {@code FAILED}（重读现值再 CAS，未命中只告警），这不会触碰任何 active 指针；{@code RETRY_WAIT} 与 {@code STALE} 都保留 {@code STAGING} 让恢复或人工重试继续可用。 English summary: a non-retryable failure closes the {@code STAGING} source revision as {@code FAILED} (its current value is re-read before the CAS and a miss only warns), which touches no active pointer at all; {@code RETRY_WAIT} and {@code STALE} keep it {@code STAGING} so recovery or a manual retry stays possible. */
    private void markRevisionFailed(KnowledgeJobBO job) {
        String revisionId = sourceRevisionId(job);
        if (revisionId == null) {
            return;
        }
        Optional<KnowledgeDocumentRevisionBO> current =
                knowledgeRepository.findRevision(job.getKbId(), revisionId);
        if (current.isEmpty() || current.get().getStatus() != KnowledgeRevisionStatusEnum.STAGING) {
            return;
        }
        KnowledgeDocumentRevisionBO failed = copyRevision(current.get())
                .setStatus(KnowledgeRevisionStatusEnum.FAILED);
        if (!knowledgeRepository.updateRevision(failed, current.get().getRevision())) {
            log.warn("document ingestion could not mark the source revision failed kb={} revision={}",
                    job.getKbId(), revisionId);
        }
    }

    /** 中文说明：来源/成员/租约/版本任一发生变化即为 {@code STALE}：旧 active 原样保留，后继由 API-021 的显式重试重新冻结来源。 English summary: a changed source, membership, lease or revision means {@code STALE}: the previous active revision stays untouched and a successor re-freezes the source through the explicit retry of API-021. */
    private KnowledgeJobBO stale(KnowledgeJobBO job, KnowledgeJobStageEnum stage, String code) {
        log.warn("document ingestion went stale job={} kb={} stage={} code={}", job.getId(), job.getKbId(), stage, code);
        return terminal(job, KnowledgeJobStatusEnum.STALE, stage, code, null);
    }

    /** 中文说明：构造写回载体：完整复制认领时的权威字段（id/载荷/租约三列/修订/时间戳都保留，供守卫按整行 CAS 使用），只改写状态、阶段、错误码与结果。 English summary: builds the write-back carrier: every authoritative field of the claim (id, payload, the three lease columns, revision and timestamps) is copied so the guard can compare-and-set the whole row, and only status, stage, error code and result are rewritten. */
    private KnowledgeJobBO terminal(
            KnowledgeJobBO job, KnowledgeJobStatusEnum status, KnowledgeJobStageEnum stage,
            String code, JsonNode result) {
        return copyJob(job)
                .setStatus(status)
                .setStage(stage)
                .setErrorCode(code)
                .setResult(result == null ? job.getResult() : result);
    }

    /** 中文说明：发布结果的固定形状：只有安全 id、条数与来源 hash，不含正文、向量、租约或提示词。 English summary: the fixed shape of a publication result: safe ids, the chunk count and the source hash only, with no text, vector, lease or prompt. */
    private ObjectNode publicationResult(String documentId, String revisionId, Integer chunkCount) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("documentId", documentId);
        result.put("revisionId", revisionId);
        result.put("chunkCount", chunkCount == null ? 0 : chunkCount.intValue());
        return result;
    }

    /** 中文说明：作业载体的完整复制：与 {@code @Accessors(chain = true)} 的 setter 配合使用，避免就地改动调用方持有的权威实例。 English summary: a full copy of the job carrier, used with the chained setters so the authoritative instance held by the caller is never mutated in place. */
    private KnowledgeJobBO copyJob(KnowledgeJobBO job) {
        return KnowledgeJobBO.builder()
                .id(job.getId())
                .kbId(job.getKbId())
                .type(job.getType())
                .resourceId(job.getResourceId())
                .actorId(job.getActorId())
                .payload(job.getPayload())
                .idempotencyKey(job.getIdempotencyKey())
                .requestHash(job.getRequestHash())
                .status(job.getStatus())
                .stage(job.getStage())
                .attempt(job.getAttempt())
                .nextAttemptAt(job.getNextAttemptAt())
                .leaseOwner(job.getLeaseOwner())
                .leaseToken(job.getLeaseToken())
                .leaseExpiresAt(job.getLeaseExpiresAt())
                .errorCode(job.getErrorCode())
                .result(job.getResult())
                .retryOfJobId(job.getRetryOfJobId())
                .revision(job.getRevision())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    /** 中文说明：revision 载体的完整复制：{@code rawBytes} 由 BO 自身做防御性复制，这里只搬运业务事实。 English summary: a full copy of the revision carrier: the BO itself clone-defends {@code rawBytes}, so only the business facts are carried over here. */
    private KnowledgeDocumentRevisionBO copyRevision(KnowledgeDocumentRevisionBO revision) {
        return KnowledgeDocumentRevisionBO.builder()
                .id(revision.getId())
                .kbId(revision.getKbId())
                .documentId(revision.getDocumentId())
                .fileName(revision.getFileName())
                .mediaType(revision.getMediaType())
                .rawBytes(revision.getRawBytes())
                .byteCount(revision.getByteCount())
                .contentHash(revision.getContentHash())
                .extractedText(revision.getExtractedText())
                .embeddingSpaceId(revision.getEmbeddingSpaceId())
                .dimensions(revision.getDimensions())
                .chunkingConfig(revision.getChunkingConfig())
                .status(revision.getStatus())
                .chunkCount(revision.getChunkCount())
                .revision(revision.getRevision())
                .createdAt(revision.getCreatedAt())
                .updatedAt(revision.getUpdatedAt())
                .build();
    }

    /** 中文说明：单调时钟耗时（毫秒），只用于运维日志。 English summary: elapsed monotonic milliseconds, for the operational log only. */
    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
