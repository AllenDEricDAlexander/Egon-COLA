package top.egon.cola.component.yuheng.admin.wiki.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.exception.CommonException;
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
import top.egon.cola.component.yuheng.admin.knowledge.service.KnowledgeJobStrategy;
import top.egon.cola.component.yuheng.admin.knowledge.service.KnowledgeModelClientService;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminRevisionConflictException;
import top.egon.cola.component.yuheng.admin.wiki.domain.bo.WikiPageBO;
import top.egon.cola.component.yuheng.admin.wiki.domain.bo.WikiRevisionBO;
import top.egon.cola.component.yuheng.admin.wiki.domain.dto.WikiSourceDTO;
import top.egon.cola.component.yuheng.admin.wiki.domain.enums.WikiPublicationPolicyEnum;
import top.egon.cola.component.yuheng.admin.wiki.repository.WikiRepository;

/**
 * 中文说明：{@code WikiGenerationStrategy} 是 {@code WIKI_GENERATE} 作业的 worker 侧策略，与
 * {@code DocumentIngestionStrategy} 共用同一套租约、重试与终态语义，只把业务步骤换成「把资料整理成 Wiki 页面」。
 * 它的执行顺序是刻意固定的：先从作业 payload 取回<b>冻结</b>的来源集合与目标页面基线，再逐项用权威行复核
 * （知识库仍在、提交者仍是 EDITOR 及以上、每个来源 revision 仍 READY 且摘要相符、仍是所属文档的激活版本、
 * 目标页面 revision 未变），任何一项变化都直接 {@code STALE} 而绝不带着过时事实去写；租约续期通过后才在
 * <b>事务与锁之外</b>发起一次模型调用；模型返回的 JSON 必须先过白名单与全部边界（至多 20 页、每页正文
 * 128KiB UTF-8 字节、slug 形态、标题与标签值域、来源引用只能命中提示词里给出的引用号），
 * 因为模型可以改写文字却不能发明证据——编造的来源 id、路径、权限或指令一律按校验失败拒绝，而不是被"尽力解析"接受。
 * 内链按 slug 交给服务端解析成同库页面 id，解析不到的链接如实丢弃并在结果里计数告警，绝不伪造一个目标。
 * 最后整批页面经 {@link WikiLifecycleService#publishGenerated} 一次调用落库：那是一个事务里的「建页 + 起草 +
 * 按各自冻结策略发布」，任一页零行命中即整批回滚、旧的已发布版本原样保留，因此本策略永不会以部分发布自称成功；
 * 只有该调用成功返回后作业才是 {@code SUCCEEDED}。
 * English summary: {@code WikiGenerationStrategy} is the worker-side strategy of the {@code WIKI_GENERATE} job, sharing the
 * lease, retry and terminal semantics of {@code DocumentIngestionStrategy} and swapping the business steps for
 * 「turning material into Wiki pages」. Its order is deliberate: it first recovers the <b>frozen</b> source set and target-page
 * baseline from the job payload, then re-verifies every fact against the authoritative rows (the knowledge base still exists,
 * the submitter is still EDITOR or above, each source revision is still {@code READY}, still matches its digest and is still
 * its document's active revision, and the target page's revision has not moved), any change answering {@code STALE} rather
 * than writing on stale truth; only after the lease is renewed does it make one model call <b>outside any transaction or
 * lock</b>; the model's JSON must then clear the whitelist and every bound (at most 20 pages, 131072 UTF-8 bytes of body per
 * page, slug shape, title and tag ranges, and source references that may only name the citation labels handed to it in the
 * prompt), because a model may rewrite prose but never invent evidence — a fabricated source id, path, permission or
 * instruction is a validation failure rather than something to parse generously. Links are handed to the server as slugs and
 * resolved to same-base page ids, an unresolvable link being dropped honestly and counted in the result instead of getting a
 * fabricated target. The whole batch is then written through one {@link WikiLifecycleService#publishGenerated} call: a single
 * transaction of 「create, draft and publish under each revision's frozen policy」 where any zero-row page rolls back all of
 * it and every previous publication stands untouched, so this strategy can never call a partial release a success; the job
 * reaches {@code SUCCEEDED} only after that call returns.
 *
 * 用法 / Usage: 由 {@code knowledgeJobStrategyRegistry} 按 {@link #type()} 归表后被 {@code KnowledgeJobWorker} 调用；
 * 提示词、正文与来源文本一律不出现在日志里，只有 id、稳定码、条数与耗时。模型调用失败按可重试的依赖不可用处理，
 * 输出不合合同按不可重试的校验失败处理（重放只会再拿到另一份不合规的文本），零行写入永远不是成功。
 * Reached by the worker through the job-strategy registry; no prompt, body or source text ever reaches the log, only ids,
 * stable codes, counts and timing. A model failure is a retryable dependency problem while a non-conforming output is a
 * non-retryable validation failure (a replay would only draw another non-conforming text), and a zero-row write is never
 * success.
 */
@Slf4j
@Validated
@Service("wikiGenerationStrategy")
@RequiredArgsConstructor
public class WikiGenerationStrategy implements KnowledgeJobStrategy {

    /** 知识库配置 bean 名，与摄取策略同口径 / the knowledge properties bean name, matching the ingestion strategy. */
    private static final String KNOWLEDGE_PROPERTIES_BEAN = KnowledgeProperties.BEAN_NAME;

    /** 一次生成最多产出的页面数（Spec §7.3.6/§9.2.24）/ the page ceiling of one generation (Spec §7.3.6/§9.2.24). */
    private static final int MAX_GENERATED_PAGES = 20;

    /** 单页正文的合同字节上限，与 {@code WikiPageVO.markdown} 的 131072 一致 / the contractual byte ceiling of one body, the same 131072 as {@code WikiPageVO.markdown}. */
    private static final int MAX_BODY_BYTES = 131_072;

    /** 提示词字符上限，与模型客户端端口的 60000 约束一致 / the prompt character ceiling, matching the model port's 60000. */
    private static final int MAX_PROMPT_CHARS = 60_000;

    /** 单页至多引用的来源条数，与 revision sources 的 1–100 合同一致 / the per-page citation ceiling of the 1–100 source contract. */
    private static final int MAX_PAGE_SOURCES = 100;

    /** 一次模型调用的补全令牌上限；刻意是代码内常量而非配置键，缺失配置时绝不猜一个远端预算 / the completion-token bound of one model call, deliberately a code constant rather than a configuration key so a missing setting never guesses a remote budget. */
    private static final int MAX_COMPLETION_TOKENS = 4_096;

    /** 页面短链名的合同形态 / the contractual slug shape. */
    private static final Pattern SLUG = Pattern.compile("^[a-z0-9][a-z0-9-]{0,63}$");

    /** 十进制字符串 id 的合同形态 / the contractual decimal-string id shape. */
    private static final Pattern DECIMAL_ID = Pattern.compile("^[1-9][0-9]{0,19}$");

    /** 模型输出的顶层键白名单，其余一律拒绝 / the whitelist of top-level output keys, everything else being refused. */
    private static final Set<String> ALLOWED_ROOT_FIELDS = Set.of("pages");

    /** 模型输出的页面键白名单，其余一律拒绝 / the whitelist of page keys the model may emit. */
    private static final Set<String> ALLOWED_PAGE_FIELDS = Set.of(
            "slug", "title", "markdown", "tags", "links", "sources");

    /** 稳定错误码：来源、成员或基线已经变化 / the stable code for a changed source, membership or baseline. */
    private static final String SOURCE_STALE = "WIKI_SOURCE_STALE";

    /** 稳定错误码：模型或数据依赖不可用 / the stable code for an unavailable model or data dependency. */
    private static final String MODEL_UNAVAILABLE = "KNOWLEDGE_MODEL_UNAVAILABLE";

    /** 稳定错误码：资源不可读 / the stable code for an unreadable resource. */
    private static final String RESOURCE_NOT_FOUND = "KNOWLEDGE_RESOURCE_NOT_FOUND";

    /** 稳定错误码：版本或状态冲突 / the stable code for a version or state clash. */
    private static final String STATE_CONFLICT = "WIKI_STATE_CONFLICT";

    /** 稳定错误码：payload 不合合同 / the stable code for a payload that fails the contract. */
    private static final String VALIDATION_FAILED = "KNOWLEDGE_VALIDATION_FAILED";

    /** 稳定错误码：提交者已无足够角色 / the stable code for a submitter without a sufficient role. */
    private static final String FORBIDDEN = "KNOWLEDGE_FORBIDDEN";

    /** 稳定错误码：本部署未接入可用的发布策略 / the stable code for a deployment with no usable publication policy wired. */
    private static final String POLICY_UNAVAILABLE = "YUHENG_DEPENDENCY_UNAVAILABLE";

    /** 依赖不可用的兜底稳定码 / the fallback stable code for an unavailable dependency. */
    private static final String DEPENDENCY_UNAVAILABLE = "YUHENG_DEPENDENCY_UNAVAILABLE";

    /**
     * 中文说明：保存 知识库访问端口 对应的依赖值；字段类型为 {@code KnowledgeRepository}，由
     * {@code WikiGenerationStrategy} 在其生命周期内读取或更新。
     * English summary: Holds the dependency represented by the knowledge access port; its type is
     * {@code KnowledgeRepository}, and {@code WikiGenerationStrategy} reads or updates it during its lifecycle.
     */
    @Qualifier("knowledgeRepository")
    private final KnowledgeRepository knowledgeRepository;

    /**
     * 中文说明：保存 Wiki 访问端口 对应的依赖值。/ Wiki access port dependency; {@code WikiRepository}.
     * English summary: Holds the Wiki access port dependency; {@code WikiRepository}.
     */
    @Qualifier("wikiRepository")
    private final WikiRepository wikiRepository;

    /**
     * 中文说明：保存 Wiki 生命周期端口 对应的依赖值，整批发布只经它的一次调用。/ Wiki lifecycle port dependency;
     * {@code WikiLifecycleService}, the batch publication being its single call.
     * English summary: Holds the Wiki lifecycle port dependency; {@code WikiLifecycleService}, the batch publication being
     * its single call.
     */
    @Qualifier("wikiLifecycleServiceImpl")
    private final WikiLifecycleService wikiLifecycleService;

    /**
     * 中文说明：保存 模型调用端口 对应的依赖值；调用总在事务与锁之外发起。/ model client port dependency; the call
     * always starting outside any transaction or lock.
     * English summary: Holds the model client port dependency; the call always starting outside any transaction or lock.
     */
    @Qualifier("knowledgeModelClientServiceImpl")
    private final KnowledgeModelClientService knowledgeModelClientService;

    /**
     * 中文说明：保存 发布策略注册表 对应的依赖值；本期只登记 {@code DIRECT}，因此「用哪个策略」是查表而不是硬编码。/
     * publication-policy registry; only {@code DIRECT} is registered this release, which is why the policy is looked up
     * rather than hardcoded.
     * English summary: Holds the publication-policy registry; only {@code DIRECT} is registered this release, so the policy
     * is a lookup rather than a literal.
     */
    @Qualifier("wikiPublicationPolicyStrategyRegistry")
    private final Map<WikiPublicationPolicyEnum, WikiPublicationPolicyStrategy> wikiPublicationPolicyStrategyRegistry;

    /**
     * 中文说明：保存 作业配置 对应的依赖值；租约、重试上限与退避只读它，本类不引入新配置键。/ job settings dependency;
     * lease, attempt ceiling and backoff come from it alone, this class introducing no new key.
     * English summary: Holds the job settings dependency; lease, attempt ceiling and backoff come from it alone, this class
     * introducing no new key.
     */
    @Qualifier(KNOWLEDGE_PROPERTIES_BEAN)
    private final KnowledgeProperties knowledgeProperties;

    /**
     * 中文说明：保存 JSON 端口 对应的依赖值，用于解析模型输出与写回结果。/ JSON port dependency, used to parse the
     * model output and build the written-back result.
     * English summary: Holds the JSON port dependency, used to parse the model output and build the written-back result.
     */
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：保存受控时钟 对应的依赖值；所有时刻都取服务端时间。/ controlled clock dependency; every instant is
     * the server's.
     * English summary: Holds the controlled clock dependency; every instant is the server's.
     */
    @Qualifier("knowledgeClock")
    private final Clock clock;

    /**
     * 中文说明：执行 type 操作；本策略只认领 {@code WIKI_GENERATE}，重复认领由 {@code KnowledgeConfiguration}
     * 在启动期以 {@code IllegalStateException} 拒绝。
     * English summary: Executes the type operation; this strategy claims only {@code WIKI_GENERATE}, a duplicate claim being
     * refused at startup by {@code KnowledgeConfiguration} with an {@code IllegalStateException}.
     * @return 返回 {@link KnowledgeJobTypeEnum#WIKI_GENERATE}；returns {@link KnowledgeJobTypeEnum#WIKI_GENERATE}.
     */
    @Override
    public KnowledgeJobTypeEnum type() {
        return KnowledgeJobTypeEnum.WIKI_GENERATE;
    }

    /**
     * 中文说明：执行 run 操作；按类注释的顺序完成一次生成，并把结论封装成终态载体返回。三条不变式贯穿始终：
     * ① 发布前的每一项事实都用权威行复核，任何变化即 {@code STALE} 而不继续写入；② 模型输出必须逐字段过白名单与
     * 边界，来源引用只认提示词给出的引用号，编造即校验失败；③ 整批发布只有一次事务，任一页失败即整批回滚，
     * 作业永不会以部分发布自称成功。
     * English summary: Executes the run operation; it performs one generation in the class-documented order and returns the
     * conclusion as a terminal carrier. Three invariants hold throughout: (1) every fact is re-verified against the
     * authoritative rows before publication and any change is {@code STALE} rather than another write; (2) the model output
     * must clear the field whitelist and every bound, source citations being valid only as the labels the prompt handed out,
     * so fabrication is a validation failure; (3) the batch publishes in exactly one transaction, any page failing rolling
     * all of it back so a job can never call a partial release a success.
     * @param job 参数 已认领并持租约的作业载体；parameter the claimed job carrier holding a lease.
     * @param leaseToken 参数 本次认领获得的租约令牌；parameter the lease token acquired by this claim.
     * @return 返回 待经租约 CAS 写回的终态作业载体；returns the terminal job carrier to write back through the lease CAS.
     */
    @Override
    public KnowledgeJobBO run(KnowledgeJobBO job, long leaseToken) {
        long startedAt = System.nanoTime();
        try {
            return generate(job, leaseToken, startedAt);
        } catch (CommonException refused) {
            boolean retryable = MODEL_UNAVAILABLE.equals(refused.getStatus());
            log.warn("wiki generation refused job={} kb={} stage={} code={} retryable={} latencyMs={}",
                    job.getId(), job.getKbId(), job.getStage(), refused.getStatus(), retryable, elapsedMillis(startedAt));
            return failure(job, stage(job), refused.getStatus(), retryable);
        } catch (GatewayAdminRevisionConflictException moved) {
            log.warn("wiki generation lost a pointer compare-and-set job={} kb={} stage={} currentRevision={} "
                    + "latencyMs={}", job.getId(), job.getKbId(), stage(job), moved.currentRevision(),
                    elapsedMillis(startedAt));
            return failure(job, stage(job), STATE_CONFLICT, false);
        } catch (RuntimeException unexpected) {
            log.warn("wiki generation failed unexpectedly job={} kb={} stage={} latencyMs={} exception={}",
                    job.getId(), job.getKbId(), job.getStage(), elapsedMillis(startedAt),
                    unexpected.getClass().getSimpleName());
            return failure(job, stage(job), DEPENDENCY_UNAVAILABLE, false);
        }
    }

    /**
     * 中文说明：生成主线。先解出冻结的 payload 并逐项复核，然后在事务与锁之外做一次模型调用，再把输出校验成
     * 页面与草稿载体，最后整批发布。
     * English summary: The generation main line: it resolves the frozen payload and re-verifies it item by item, makes one
     * model call outside any transaction or lock, validates the output into page and draft carriers, then publishes the
     * batch as one.
     * @param job 参数 已认领的作业载体；parameter the claimed job carrier.
     * @param leaseToken 参数 本次认领的租约令牌；parameter this claim's lease token.
     * @param startedAt 参数 单调时钟起点；parameter the monotonic start.
     * @return 返回 终态作业载体；returns the terminal job carrier.
     */
    private KnowledgeJobBO generate(KnowledgeJobBO job, long leaseToken, long startedAt) {
        GenerationIntent intent = GenerationIntent.from(job.getPayload());
        if (intent == null) {
            log.warn("wiki generation payload is unusable job={} kb={}", job.getId(), job.getKbId());
            return failure(job, stage(job), VALIDATION_FAILED, false);
        }
        KnowledgeBaseBO base = knowledgeRepository.findBase(intent.kbId()).orElse(null);
        if (base == null) {
            return stale(job, stage(job), RESOURCE_NOT_FOUND);
        }
        if (!isEditorOrOwner(base, job.getActorId())) {
            log.warn("wiki generation submitter no longer editor job={} kb={}", job.getId(), intent.kbId());
            return stale(job, stage(job), FORBIDDEN);
        }
        List<FrozenSource> sources = verifySources(intent, job);
        WikiPageBO target = verifyTarget(intent, job);
        if (!renewLease(job, leaseToken)) {
            return stale(job, KnowledgeJobStageEnum.GENERATE, STATE_CONFLICT);
        }
        String raw = knowledgeModelClientService.generate(
                intent.kbId(), prompt(base, sources, target), MAX_COMPLETION_TOKENS);
        List<GeneratedPage> pages = readPages(raw, sources);
        Batch batch = resolveBatch(job, intent, pages);
        if (!renewLease(job, leaseToken)) {
            return stale(job, KnowledgeJobStageEnum.PUBLISH, STATE_CONFLICT);
        }
        wikiLifecycleService.publishGenerated(batch.pages(), batch.drafts(), job.getActorId());
        log.info("wiki generation published job={} kb={} pages={} citations={} latencyMs={}",
                job.getId(), intent.kbId(), batch.pages().size(), batch.unresolvedLinks(), elapsedMillis(startedAt));
        return terminal(job, KnowledgeJobStatusEnum.SUCCEEDED, KnowledgeJobStageEnum.DONE, null,
                generationResult(intent.kbId(), intent.pageId(), batch));
    }

    /**
     * 中文说明：逐个复核冻结来源：行必须还在、状态必须仍是 READY、摘要必须与冻结值相同，且它仍是所属文档的
     * 激活版本；同时把该 revision 的分块读出来作为可引用的证据单元。任何一条不成立都按 {@code STALE} 收束——
     * 把已经改写的资料当作当前事实发布成 Wiki，比拒绝这次生成危险得多。
     * English summary: Re-verifies each frozen source: the row must still exist, still be {@code READY}, still match the
     * frozen digest and still be its document's active revision, while that revision's chunks are read out as the citable
     * evidence units. Any failure closes as {@code STALE} — publishing rewritten material as current fact into a Wiki is far
     * more dangerous than refusing this generation.
     * @param intent 参数 冻结的作业意图；parameter the frozen job intent.
     * @param job 参数 已认领的作业载体；parameter the claimed job carrier.
     * @return 返回 复核后的来源集合；returns the re-verified source set.
     */
    private List<FrozenSource> verifySources(GenerationIntent intent, KnowledgeJobBO job) {
        List<FrozenSource> verified = new ArrayList<>(intent.sources().size());
        for (FrozenSource frozen : intent.sources()) {
            KnowledgeDocumentRevisionBO revision = knowledgeRepository
                    .findRevision(intent.kbId(), frozen.documentRevisionId())
                    .orElse(null);
            if (revision == null || !KnowledgeRevisionStatusEnum.READY.equals(revision.getStatus())
                    || !StringUtils.equals(revision.getContentHash(), frozen.sourceHash())) {
                log.warn("wiki generation source no longer current job={} kb={} revision={}",
                        job.getId(), intent.kbId(), frozen.documentRevisionId());
                throw new CommonException(409, SOURCE_STALE, "a wiki generation source is no longer current");
            }
            KnowledgeDocumentBO document = knowledgeRepository
                    .findDocument(intent.kbId(), revision.getDocumentId()).orElse(null);
            if (document == null
                    || !StringUtils.equals(document.getActiveRevisionId(), frozen.documentRevisionId())) {
                log.warn("wiki generation source is no longer active job={} kb={} revision={}",
                        job.getId(), intent.kbId(), frozen.documentRevisionId());
                throw new CommonException(409, SOURCE_STALE,
                        "a wiki generation source is no longer its document's active revision");
            }
            verified.add(new FrozenSource(frozen.documentRevisionId(), frozen.sourceHash(),
                    knowledgeRepository.listChunksOfRevision(frozen.documentRevisionId())));
        }
        return List.copyOf(verified);
    }

    /**
     * 中文说明：复核目标页面基线：给了 {@code pageId} 就必须仍可读、仍属于本库，且页面 revision 仍是冻结值——
     * 冻结之后有人提交过新草稿，本次生成就不去覆写它，而是让作业 {@code STALE}，由编辑者重新发起。
     * English summary: Re-checks the target-page baseline: a supplied {@code pageId} must still be readable, still belong to
     * this base, and its page revision must still equal the frozen one — if someone committed a newer draft after the freeze,
     * this generation refuses to overwrite it and goes {@code STALE} so the editor can start again.
     * @param intent 参数 冻结的作业意图；parameter the frozen job intent.
     * @param job 参数 已认领的作业载体；parameter the claimed job carrier.
     * @return 返回 目标页面载体，新建页面时为 {@code null}；returns the target page carrier, {@code null} for a new page.
     */
    private WikiPageBO verifyTarget(GenerationIntent intent, KnowledgeJobBO job) {
        if (intent.pageId() == null) {
            return null;
        }
        WikiPageBO page = wikiRepository.findPage(intent.pageId()).orElse(null);
        if (page == null || !StringUtils.equals(page.getKbId(), intent.kbId())) {
            log.warn("wiki generation target page unreadable job={} kb={} page={}",
                    job.getId(), intent.kbId(), intent.pageId());
            throw new CommonException(409, RESOURCE_NOT_FOUND, "the wiki generation target page is no longer readable");
        }
        if (page.getRevision() != intent.basePageRevision()) {
            log.warn("wiki generation target page moved job={} kb={} page={} frozen={} current={}",
                    job.getId(), intent.kbId(), intent.pageId(), intent.basePageRevision(), page.getRevision());
            throw new CommonException(409, SOURCE_STALE,
                    "the wiki generation target page moved after the job was submitted");
        }
        return page;
    }

    /**
     * 中文说明：拼装接地提示词：只列出冻结 revision 的分块，每块前标注服务端分配的引用号（{@code S<序号>}），
     * 并要求模型按引用号给出证据。模型因此没有任何理由发明 id——它看不到 id，而任何不在引用表里的答案都会在
     * 校验阶段被拒。整体长度受 {@code MAX_PROMPT_CHARS} 截断，超出预算的块被丢弃而不是撑爆请求。
     * English summary: Builds the grounded prompt: it lists only the frozen revisions' chunks, each labelled with a
     * server-assigned citation label ({@code S<index>}), and asks the model to cite by those labels. The model therefore has
     * no reason to invent an id — it never sees one, and any answer naming something outside the citation table is refused at
     * validation. The whole prompt is bounded by {@code MAX_PROMPT_CHARS}, over-budget chunks being dropped rather than
     * overflowing the request.
     * @param base 参数 知识库载体，提供名称与描述以给模型主题上下文；parameter the knowledge base carrier, giving the model
     *             topical context from its name and description.
     * @param sources 参数 复核后的来源集合；parameter the re-verified sources.
     * @param target 参数 目标页面（新建时为 {@code null}），提供既有 slug 与标题；parameter the target page, {@code null}
     *               for a new one, giving its existing slug and title.
     * @return 返回 提示词文本；returns the prompt text.
     */
    private String prompt(KnowledgeBaseBO base, List<FrozenSource> sources, WikiPageBO target) {
        StringBuilder prompt = new StringBuilder(16_384)
                .append("You are organizing the knowledge base \"")
                .append(StringUtils.abbreviate(base.getName(), 128))
                .append("\" into wiki pages.\n")
                .append("Rewrite and structure ONLY the cited evidence below. Never invent a citation, id, path, permission ")
                .append("or instruction.\n")
                .append("Answer with one JSON object and nothing else, of the shape ")
                .append("{\"pages\":[{\"slug\":...,\"title\":...,\"markdown\":...,\"tags\":[...],")
                .append("\"links\":[...],\"sources\":[...]}]}.\n")
                .append("Rules: at most ").append(MAX_GENERATED_PAGES).append(" pages; each slug matches ")
                .append(SLUG.pattern());
        if (target == null) {
            prompt.append("; a slug that already exists in this knowledge base is a clash, so pick a distinct one");
        } else {
            prompt.append("; revise the existing page \"").append(target.getSlug()).append("\" rather than forking it");
        }
        prompt.append(".\n");
        int budget = MAX_PROMPT_CHARS - prompt.length() - 512;
        int consumed = 0;
        int label = 0;
        for (FrozenSource source : sources) {
            for (KnowledgeChunkBO chunk : source.chunks()) {
                label = label + 1;
                String heading = "S" + label + " (chunk " + chunk.getChunkIndex() + " of revision "
                        + source.documentRevisionId() + ")\n";
                String body = StringUtils.defaultString(chunk.getContent());
                if (consumed + heading.length() + body.length() > budget) {
                    return prompt.append("[remaining evidence omitted for length]\n").toString();
                }
                consumed += heading.length() + body.length();
                prompt.append(heading).append(body).append('\n');
            }
        }
        return prompt.toString();
    }

    /**
     * 中文说明：把模型文本解析成页面候选：顶层必须是对象、只允许 {@code pages} 键；每页只允许白名单字段；
     * 页数、slug、标题、正文（UTF-8 字节数）、标签与内链逐项复核；来源引用必须命中提示词里真实存在、
     * 且属于本页所引用证据的引用号，命中不了即整批校验失败而不是悄悄丢掉证据。
     * English summary: Parses the model text into page candidates: the root must be an object admitting only the
     * {@code pages} key; each page may only carry whitelisted fields; the page count, slug, title, body (in UTF-8 bytes),
     * tags and links are checked one by one; and every source citation must name a label that really appeared in the prompt
     * among the evidence this page rests on — an unmatched citation failing the whole batch instead of quietly dropping
     * evidence.
     * @param raw 参数 模型原始文本；parameter the raw model text.
     * @param sources 参数 复核后的来源集合，决定合法引用号；parameter the re-verified sources deciding the legal labels.
     * @return 返回 页面候选列表；returns the page candidates.
     */
    private List<GeneratedPage> readPages(String raw, List<FrozenSource> sources) {
        JsonNode root = readJson(raw);
        requireFields(root, ALLOWED_ROOT_FIELDS);
        JsonNode pages = root.path("pages");
        if (!pages.isArray() || pages.isEmpty() || pages.size() > MAX_GENERATED_PAGES) {
            throw new CommonException(422, VALIDATION_FAILED,
                    "the generated page set is outside the 1-20 contract");
        }
        Map<String, FrozenCitation> citations = citationsOf(sources);
        List<GeneratedPage> accepted = new ArrayList<>(pages.size());
        for (JsonNode page : pages) {
            accepted.add(readPage(page, citations));
        }
        return List.copyOf(accepted);
    }

    /**
     * 中文说明：解析并校验单页。
     * English summary: Parses and validates one page.
     * @param page 参数 模型给出的页面对象；parameter the page object from the model.
     * @param citations 参数 引用号到证据三元组的映射；parameter the label-to-evidence map.
     * @return 返回 校验通过的页面候选；returns the validated page candidate.
     */
    private GeneratedPage readPage(JsonNode page, Map<String, FrozenCitation> citations) {
        if (!page.isObject()) {
            throw new CommonException(422, VALIDATION_FAILED, "a generated page is not an object");
        }
        requireFields(page, ALLOWED_PAGE_FIELDS);
        String slug = requireText(page.get("slug"), "slug");
        String title = requireText(page.get("title"), "title");
        String markdown = requireText(page.get("markdown"), "markdown");
        if (!SLUG.matcher(slug).matches()) {
            throw new CommonException(422, VALIDATION_FAILED, "a generated slug is outside the contract");
        }
        if (title.length() > 128) {
            throw new CommonException(422, VALIDATION_FAILED, "a generated title is longer than 128 characters");
        }
        if (markdown.getBytes(StandardCharsets.UTF_8).length > MAX_BODY_BYTES) {
            throw new CommonException(422, VALIDATION_FAILED, "a generated body exceeds 131072 UTF-8 bytes");
        }
        List<String> tags = readStrings(page.get("tags"), 20, "tags");
        if (new LinkedHashSet<>(tags).size() != tags.size()) {
            throw new CommonException(422, VALIDATION_FAILED, "a generated page repeats a tag");
        }
        for (String tag : tags) {
            if (tag.isBlank() || tag.length() > 32) {
                throw new CommonException(422, VALIDATION_FAILED, "a generated tag is outside the 1-32 contract");
            }
        }
        List<String> linkSlugs = readStrings(page.get("links"), 100, "links");
        JsonNode cited = page.get("sources");
        if (!cited.isArray() || cited.isEmpty() || cited.size() > MAX_PAGE_SOURCES) {
            throw new CommonException(422, VALIDATION_FAILED, "a generated page is outside the 1-100 citation contract");
        }
        List<WikiSourceDTO> sources = new ArrayList<>(cited.size());
        Set<String> seen = new LinkedHashSet<>();
        for (JsonNode reference : cited) {
            String label = requireText(reference, "sources");
            FrozenCitation citation = citations.get(label);
            if (citation == null) {
                throw new CommonException(422, VALIDATION_FAILED,
                        "a generated page cites a label that was never offered as evidence");
            }
            if (seen.add(label)) {
                sources.add(WikiSourceDTO.builder()
                        .documentRevisionId(citation.documentRevisionId())
                        .chunkId(citation.chunkId())
                        .sourceHash(citation.sourceHash())
                        .build());
            }
        }
        return new GeneratedPage(slug, title, markdown, tags, linkSlugs, List.copyOf(sources));
    }

    /**
     * 中文说明：把复核后的来源集合摊成「引用号 → 证据三元组」，引用号的分配口径必须与 {@link #prompt} 完全一致，
     * 否则提示词与校验会各自数出一套编号。这里两处共用同一遍历顺序（来源顺序、块顺序），因此编号天然对齐。
     * English summary: Flattens the re-verified sources into a label-to-evidence map; the labelling must agree exactly with
     * {@link #prompt} or the prompt and the check would each count out their own numbering. Both share this one traversal
     * order (source order, then chunk order), so the labels line up by construction.
     * @param sources 参数 复核后的来源集合；parameter the re-verified sources.
     * @return 返回 引用号映射；returns the label map.
     */
    private Map<String, FrozenCitation> citationsOf(List<FrozenSource> sources) {
        Map<String, FrozenCitation> labels = new LinkedHashMap<>();
        int label = 0;
        for (FrozenSource source : sources) {
            for (KnowledgeChunkBO chunk : source.chunks()) {
                label = label + 1;
                labels.put("S" + label, new FrozenCitation(source.documentRevisionId(), chunk.getId(),
                        source.sourceHash()));
            }
        }
        return Map.copyOf(labels);
    }

    /**
     * 中文说明：把页面候选落成待发布的「页面 + 草稿」对：slug 命中本库已有页面就复用其指针并追加草稿，
     * 未命中且本作业声明了目标页面时按 409 拒绝抢占别人的页面（候选留档，不静默改名），新建页则留空主键由
     * 持久边界落定。内链的 slug 解析不到就丢弃并计数——链接是页面 id，服务端不会为一个解析不到的名字编一个目标。
     * English summary: Turns the candidates into the page-plus-draft pairs to publish: a slug hitting an existing page of this
     * base reuses its pointers and appends a draft, a miss on a job that declared a target page is a 409 rather than a
     * preemption of someone else's page (the candidate stays on record, nothing being silently renamed), and a new page keeps
     * an empty key for the persistence boundary to settle. An unresolvable link slug is dropped and counted — a link is a page
     * id, and the server never fabricates a target for a name it cannot resolve.
     * @param job 参数 已认领的作业载体；parameter the claimed job carrier.
     * @param intent 参数 冻结的作业意图；parameter the frozen job intent.
     * @param pages 参数 校验通过的页面候选；parameter the validated page candidates.
     * @return 返回 待发布批次；returns the batch to publish.
     */
    private Batch resolveBatch(KnowledgeJobBO job,
                               GenerationIntent intent,
                               List<GeneratedPage> pages) {
        WikiPublicationPolicyEnum policy = publicationPolicy();
        Map<String, WikiPageBO> bySlug = new LinkedHashMap<>();
        for (GeneratedPage candidate : pages) {
            WikiPageBO existing = wikiRepository.findPageBySlug(intent.kbId(), candidate.slug()).orElse(null);
            if (existing == null) {
                bySlug.put(candidate.slug(), null);
                continue;
            }
            // 本期唯一允许「追加到既有页」的入口是提交时冻结的 pageId：没有它就不去动别人的页面，
            // 否则一次生成可以凭一个撞名的 slug 改写任意页面。留候选、返回 409 而不是抢占。
            if (intent.pageId() == null || !StringUtils.equals(existing.getId(), intent.pageId())) {
                log.warn("wiki generation slug clashes a page this job does not target job={} kb={} slug={} owner={}",
                        job.getId(), intent.kbId(), candidate.slug(), existing.getId());
                throw new CommonException(409, STATE_CONFLICT,
                        "the generated slug already belongs to another page");
            }
            bySlug.put(candidate.slug(), existing);
        }
        List<WikiPageBO> carriers = new ArrayList<>(pages.size());
        List<WikiRevisionBO> drafts = new ArrayList<>(pages.size());
        int unresolvedLinks = 0;
        for (GeneratedPage candidate : pages) {
            WikiPageBO existing = bySlug.get(candidate.slug());
            WikiPageBO page = existing == null
                    ? WikiPageBO.builder()
                    .kbId(intent.kbId())
                    .slug(candidate.slug())
                    .revision(0L)
                    .build()
                    : copyPage(existing);
            List<String> links = new ArrayList<>(candidate.linkSlugs().size());
            for (String linkSlug : candidate.linkSlugs()) {
                WikiPageBO resolved = bySlug.get(linkSlug);
                if (resolved != null && resolved.getId() != null) {
                    links.add(resolved.getId());
                    continue;
                }
                WikiPageBO outside = wikiRepository.findPageBySlug(intent.kbId(), linkSlug).orElse(null);
                if (outside == null) {
                    unresolvedLinks = unresolvedLinks + 1;
                    continue;
                }
                links.add(outside.getId());
            }
            drafts.add(WikiRevisionBO.newDraft(
                    intent.kbId(),
                    page.getId(),
                    candidate.title(),
                    candidate.markdown(),
                    candidate.tags(),
                    List.copyOf(links),
                    candidate.sources(),
                    job.getActorId(),
                    job.getId(),
                    policy));
            carriers.add(page);
        }
        return new Batch(List.copyOf(carriers), List.copyOf(drafts), unresolvedLinks);
    }

    /**
     * 中文说明：从注册表取本期唯一可用的发布策略：只登记了一个策略时用它，登记为空的部署按依赖未接入失败关闭，
     * 而不是猜一个策略出来。这就是「配置决定策略」而不是代码决定策略的最小实现。
     * English summary: Takes the one usable publication policy out of the registry: a deployment registering exactly one
     * strategy uses it, one registering none fails closed as an unwired dependency rather than guessing a policy. That is the
     * minimal form of 「configuration decides the policy」 instead of the code deciding it.
     * @return 返回 冻结的发布策略；returns the policy to freeze.
     */
    private WikiPublicationPolicyEnum publicationPolicy() {
        if (wikiPublicationPolicyStrategyRegistry.size() != 1) {
            log.warn("wiki generation cannot settle a publication policy from the registry registered={}",
                    wikiPublicationPolicyStrategyRegistry.size());
            throw new CommonException(503, POLICY_UNAVAILABLE,
                    "exactly one wiki publication policy must be registered for generation");
        }
        return wikiPublicationPolicyStrategyRegistry.keySet().iterator().next();
    }

    /**
     * 中文说明：解析模型文本：只接受一个 JSON 对象，容忍模型在对象前后附带解释性文字（截取首个 '{' 到末个 '}'），
     * 但结构不合即按依赖返回内容不合规处理，绝不"尽力"猜测字段。
     * English summary: Parses the model text: one JSON object is accepted, tolerating prose wrapped around it (the span from
     * the first '{' to the last '}'), but a wrong structure is treated as non-conforming upstream content rather than a field
     * to guess at.
     * @param raw 参数 模型原始文本；parameter the raw model text.
     * @return 返回 顶层节点；returns the root node.
     */
    private JsonNode readJson(String raw) {
        if (StringUtils.isBlank(raw)) {
            throw new CommonException(503, MODEL_UNAVAILABLE, "the model returned no content");
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new CommonException(503, MODEL_UNAVAILABLE, "the model returned no JSON object");
        }
        try {
            JsonNode root = objectMapper.readTree(raw.substring(start, end + 1));
            if (!root.isObject()) {
                throw new CommonException(422, VALIDATION_FAILED, "the generated root is not an object");
            }
            return root;
        } catch (CommonException refused) {
            throw refused;
        } catch (java.io.IOException malformed) {
            throw new CommonException(503, MODEL_UNAVAILABLE, "the model returned malformed JSON");
        }
    }

    /**
     * 中文说明：白名单复核：出现任何未声明的键都按校验失败拒绝。字段白名单是"模型不能扩接口"这条约束的落点，
     * 因此这里必须是严格判定而不是忽略多余键。
     * English summary: The whitelist check: any undeclared key is a validation failure. The field whitelist is where
     * 「the model may not extend the interface」 lands, so this must be strict rather than ignoring extras.
     * @param node 参数 待判定对象；parameter the object to judge.
     * @param allowed 参数 允许的键集合；parameter the admitted keys.
     */
    private static void requireFields(JsonNode node, Set<String> allowed) {
        node.fieldNames().forEachRemaining(name -> {
            if (!allowed.contains(name)) {
                throw new CommonException(422, VALIDATION_FAILED,
                        "the generated payload carries an undeclared field");
            }
        });
    }

    /**
     * 中文说明：取一个必填文本字段。
     * English summary: Reads one required textual field.
     * @param node 参数 字段节点；parameter the field node.
     * @param field 参数 字段名，只进错误消息；parameter the field name, for the error message only.
     * @return 返回 文本值；returns the text.
     */
    private static String requireText(JsonNode node, String field) {
        if (node == null || !node.isTextual() || node.textValue().isBlank()) {
            throw new CommonException(422, VALIDATION_FAILED, "the generated " + field + " is missing or blank");
        }
        return node.textValue();
    }

    /**
     * 中文说明：取一个字符串数组并复核条数上界与元素形态；缺失按空数组处理（标签与内链允许为空）。
     * English summary: Reads a string array, checking the count ceiling and element shape; absence counts as empty (tags and
     * links may both be empty).
     * @param node 参数 数组节点；parameter the array node.
     * @param max 参数 条数上界；parameter the count ceiling.
     * @param field 参数 字段名；parameter the field name.
     * @return 返回 字符串列表；returns the strings.
     */
    private static List<String> readStrings(JsonNode node, int max, String field) {
        if (node == null || node.isNull()) {
            return List.of();
        }
        if (!node.isArray() || node.size() > max) {
            throw new CommonException(422, VALIDATION_FAILED, "the generated " + field + " is outside its contract");
        }
        List<String> values = new ArrayList<>(node.size());
        for (JsonNode element : node) {
            values.add(requireText(element, field));
        }
        return List.copyOf(values);
    }

    /**
     * 中文说明：租约续期即所有权证明：返回 false 意味着令牌已推进、租约被接管或作业已终态，本次执行必须放弃
     * 而不是继续写。
     * English summary: Renewing the lease is the ownership proof: a {@code false} means the token advanced, the lease was
     * taken over or the job is terminal, so this execution must abandon rather than keep writing.
     * @param job 参数 已认领的作业载体；parameter the claimed job carrier.
     * @param leaseToken 参数 本次认领的租约令牌；parameter this claim's lease token.
     * @return 返回 是否仍持有租约；whether the lease is still held.
     */
    private boolean renewLease(KnowledgeJobBO job, long leaseToken) {
        Duration lease = knowledgeProperties.getLease();
        if (lease == null || lease.isZero() || lease.isNegative()
                || !knowledgeRepository.heartbeat(job.getId(), leaseToken, clock.instant().plus(lease))) {
            log.warn("wiki generation lease no longer held job={} kb={} leaseToken={}",
                    job.getId(), job.getKbId(), leaseToken);
            return false;
        }
        return true;
    }

    /**
     * 中文说明：提交者必须仍是 EDITOR 及以上：OWNER 天然满足，成员里角色非 READER 即满足。生成作业会以提交者
     * 身份发布页面，所以提交者被降权后绝不能继续替它发布。
     * English summary: The submitter must still be EDITOR or above: an OWNER satisfies it by definition and a member does when
     * its role is not READER. A generation job publishes pages as its submitter, so a downgraded submitter must not keep
     * publishing on its behalf.
     * @param base 参数 知识库载体；parameter the knowledge base carrier.
     * @param actorId 参数 提交者标识；parameter the submitter identity.
     * @return 返回 是否仍具备写角色；whether the write role still holds.
     */
    private static boolean isEditorOrOwner(KnowledgeBaseBO base, String actorId) {
        if (StringUtils.isBlank(actorId) || !StringUtils.equals(actorId, base.getOwnerActorId())) {
            List<KnowledgeMemberDTO> members = base.getMembers();
            if (members == null) {
                return false;
            }
            return members.stream()
                    .filter(Objects::nonNull)
                    .anyMatch(member -> StringUtils.equals(actorId, member.getActorId())
                            && member.getRole() != null
                            && member.getRole() != KnowledgeMemberRoleEnum.READER);
        }
        return true;
    }

    /**
     * 中文说明：当前阶段的权威值：认领载体已写的阶段优先，缺失按 {@code GENERATE} 处理，从不伪造百分比进度。
     * English summary: The authoritative stage: the stage already on the claimed carrier wins, a missing one counting as
     * {@code GENERATE}, and a fake percentage is never invented.
     * @param job 参数 已认领的作业载体；parameter the claimed job carrier.
     * @return 返回 阶段；returns the stage.
     */
    private static KnowledgeJobStageEnum stage(KnowledgeJobBO job) {
        KnowledgeJobStageEnum observed = job.getStage();
        return observed == null || KnowledgeJobStageEnum.QUEUED.equals(observed)
                ? KnowledgeJobStageEnum.GENERATE
                : observed;
    }

    /**
     * 中文说明：把一次失败映射为 {@code RETRY_WAIT} 或 {@code FAILED}：只有明确可重试的错误、且本次尝试之后仍有
     * {@code maxAttempts} 预算时才排程；发布冲突与合同不合都是终态失败，重放只会再拿到另一份不合规的输出。
     * English summary: Maps one failure onto {@code RETRY_WAIT} or {@code FAILED}: only an explicitly retryable error with
     * budget left under {@code maxAttempts} is scheduled; a publication clash or a non-conforming output is terminal, since a
     * replay would only draw another non-conforming result.
     * @param job 参数 已认领的作业载体；parameter the claimed job carrier.
     * @param stage 参数 失败发生的阶段；parameter the stage that failed.
     * @param code 参数 稳定错误码；parameter the stable error code.
     * @param retryable 参数 是否可重试；parameter whether a retry is possible.
     * @return 返回 终态作业载体；returns the terminal job carrier.
     */
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
        return terminal(job, KnowledgeJobStatusEnum.FAILED, stage, code, null);
    }

    /**
     * 中文说明：来源、成员、基线或租约任一发生变化即为 {@code STALE}：旧的已发布版本原样保留，后继由编辑者
     * 重新发起一次显式生成。
     * English summary: A changed source, membership, baseline or lease means {@code STALE}: every previous publication stands
     * untouched and the next attempt is an explicit re-submission by an editor.
     * @param job 参数 已认领的作业载体；parameter the claimed job carrier.
     * @param stage 参数 失效时的阶段；parameter the stage at which it went stale.
     * @param code 参数 稳定错误码；parameter the stable error code.
     * @return 返回 {@code STALE} 终态载体；returns the {@code STALE} terminal carrier.
     */
    private KnowledgeJobBO stale(KnowledgeJobBO job, KnowledgeJobStageEnum stage, String code) {
        log.warn("wiki generation went stale job={} kb={} stage={} code={}", job.getId(), job.getKbId(), stage, code);
        return terminal(job, KnowledgeJobStatusEnum.STALE, stage, code, null);
    }

    /**
     * 中文说明：构造写回载体：完整复制认领时的权威字段（id、载荷、租约三列、修订与时间戳都保留，供守卫按整行
     * CAS 使用），只改写状态、阶段、错误码与结果。
     * English summary: Builds the write-back carrier: every authoritative field of the claim (id, payload, the three lease
     * columns, revision and timestamps) is copied so the guard can compare-and-set the whole row, and only status, stage,
     * error code and result are rewritten.
     * @param job 参数 已认领的作业载体；parameter the claimed job carrier.
     * @param status 参数 终态状态；parameter the terminal status.
     * @param stage 参数 终态阶段；parameter the terminal stage.
     * @param code 参数 稳定错误码，成功为 {@code null}；parameter the stable code, {@code null} on success.
     * @param result 参数 结果节点，缺省保留原值；parameter the result node, keeping the original when absent.
     * @return 返回 写回载体；returns the write-back carrier.
     */
    private KnowledgeJobBO terminal(
            KnowledgeJobBO job, KnowledgeJobStatusEnum status, KnowledgeJobStageEnum stage,
            String code, JsonNode result) {
        return copyJob(job)
                .setStatus(status)
                .setStage(stage)
                .setErrorCode(code)
                .setResult(result == null ? job.getResult() : result);
    }

    /**
     * 中文说明：作业结果的安全形状：只有页面 id 与条数、解析不到的内链计数，绝不含正文、提示词或来源文本。
     * English summary: The safe shape of a job result: page ids and counts plus the unresolved-link tally, never the bodies,
     * the prompt or the source text.
     * @param kbId 参数 知识库 id；parameter the knowledge base id.
     * @param pageId 参数 冻结的目标页面 id；parameter the frozen target page id.
     * @param batch 参数 已发布批次；parameter the published batch.
     * @return 返回 结果节点；returns the result node.
     */
    private ObjectNode generationResult(String kbId, String pageId, Batch batch) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("kbId", kbId);
        result.putNull("pageId");
        if (pageId != null) {
            result.put("pageId", pageId);
        }
        ArrayNode published = result.putArray("pages");
        for (WikiPageBO page : batch.pages()) {
            ObjectNode entry = published.addObject();
            entry.put("pageId", page.getId());
            entry.put("slug", page.getSlug());
        }
        result.put("pageCount", batch.pages().size());
        result.put("unresolvedLinks", batch.unresolvedLinks());
        return result;
    }

    /**
     * 中文说明：作业载体的完整复制，避免就地改动调用方持有的权威实例。
     * English summary: A full copy of the job carrier so the authoritative instance held by the caller is never mutated in
     * place.
     * @param job 参数 已认领的作业载体；parameter the claimed job carrier.
     * @return 返回 等值副本；returns the equal copy.
     */
    private static KnowledgeJobBO copyJob(KnowledgeJobBO job) {
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

    /**
     * 中文说明：页面载体的完整复制，供指针切换与 CAS 使用。
     * English summary: A full copy of a page carrier for a pointer swap and its compare-and-set.
     * @param page 参数 源页面载体；parameter the source page carrier.
     * @return 返回 等值副本；returns the equal copy.
     */
    private static WikiPageBO copyPage(WikiPageBO page) {
        return WikiPageBO.builder()
                .id(page.getId())
                .kbId(page.getKbId())
                .slug(page.getSlug())
                .draftRevisionId(page.getDraftRevisionId())
                .publishedRevisionId(page.getPublishedRevisionId())
                .revision(page.getRevision())
                .createdAt(page.getCreatedAt())
                .updatedAt(page.getUpdatedAt())
                .build();
    }

    /**
     * 中文说明：单调时钟耗时（毫秒），只用于运维日志。
     * English summary: Elapsed monotonic milliseconds, for the operational log only.
     * @param startedAt 参数 起点；parameter the start.
     * @return 返回 毫秒数；returns the milliseconds.
     */
    private static long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    /**
     * 中文说明：可选的文本读取；缺失或空白返回 {@code null}，绝不把空串当成一个合法 id。
     * English summary: An optional text read; absent or blank answers {@code null} and an empty string is never a legal id.
     * @param node 参数 节点；parameter the node.
     * @return 返回 文本或 {@code null}；returns the text or {@code null}.
     */
    private static String textOrNull(JsonNode node) {
        return node == null || !node.isTextual() ? null : StringUtils.trimToNull(node.textValue());
    }

    /**
     * 中文说明：{@code GenerationIntent} 是作业 payload 的不可变读视图：只承认冻结时写入的键，
     * 任何形态不合都返回 {@code null} 由调用方按校验失败收束，而不是猜一个默认值。
     * English summary: {@code GenerationIntent} is the immutable read view of the job payload: it admits only the keys written
     * at freeze time, any malformed shape answering {@code null} for the caller to close as a validation failure rather than
     * a guessed default.
     * @param kbId 参数 知识库 id；parameter the knowledge base id.
     * @param pageId 参数 目标页面 id，新建为 {@code null}；parameter the target page id, {@code null} for a new page.
     * @param basePageRevision 参数 冻结时的页面 revision 基线，新建为 0；parameter the frozen page-revision baseline, zero for
     *                         a new page.
     * @param sources 参数 冻结的来源集合；parameter the frozen sources.
     */
    record GenerationIntent(String kbId, String pageId, long basePageRevision, List<FrozenSource> sources) {

        /**
         * 中文说明：从 payload 读出意图：{@code kbId} 必填且形态合规，来源数组非空且每条都带 revision id 与摘要，
         * 目标页面 id 可空。
         * English summary: Reads the intent out of the payload: {@code kbId} is required and well-formed, the source array is
         * non-empty with a revision id and a digest on every entry, and the target page id is optional.
         * @param payload 参数 作业载荷；parameter the job payload.
         * @return 返回 意图或 {@code null}；returns the intent or {@code null}.
         */
        static GenerationIntent from(JsonNode payload) {
            if (payload == null || !payload.isObject()) {
                return null;
            }
            String kbId = textOrNull(payload.get("kbId"));
            if (kbId == null || !DECIMAL_ID.matcher(kbId).matches()) {
                return null;
            }
            String pageId = textOrNull(payload.get("pageId"));
            if (pageId != null && !DECIMAL_ID.matcher(pageId).matches()) {
                return null;
            }
            long basePageRevision = payload.path("basePageRevision").asLong(0L);
            JsonNode frozen = payload.get("sources");
            if (frozen == null || !frozen.isArray() || frozen.isEmpty()) {
                return null;
            }
            List<FrozenSource> sources = new ArrayList<>(frozen.size());
            for (JsonNode entry : frozen) {
                String revisionId = textOrNull(entry.get("documentRevisionId"));
                String hash = textOrNull(entry.get("sourceHash"));
                if (revisionId == null || !DECIMAL_ID.matcher(revisionId).matches() || hash == null) {
                    return null;
                }
                sources.add(new FrozenSource(revisionId, hash, List.of()));
            }
            return new GenerationIntent(kbId, pageId, Math.max(basePageRevision, 0L), List.copyOf(sources));
        }
    }

    /**
     * 中文说明：{@code FrozenSource} 是一个冻结来源：revision id、冻结摘要，以及复核时取回的分块列表。
     * English summary: {@code FrozenSource} is one frozen source: the revision id, the frozen digest, and the chunks fetched
     * during re-verification.
     * @param documentRevisionId 参数 来源 revision id；parameter the source revision id.
     * @param sourceHash 参数 冻结摘要；parameter the frozen digest.
     * @param chunks 参数 分块列表；parameter the chunks.
     */
    record FrozenSource(String documentRevisionId, String sourceHash, List<KnowledgeChunkBO> chunks) {
    }

    /**
     * 中文说明：{@code FrozenCitation} 是引用号背后的证据三元组，字段形态与 {@code WikiSourceDTO} 一致。
     * English summary: {@code FrozenCitation} is the evidence triple behind a citation label, shaped like
     * {@code WikiSourceDTO}.
     * @param documentRevisionId 参数 来源 revision id；parameter the source revision id.
     * @param chunkId 参数 分块 id；parameter the chunk id.
     * @param sourceHash 参数 来源摘要；parameter the source digest.
     */
    record FrozenCitation(String documentRevisionId, String chunkId, String sourceHash) {
    }

    /**
     * 中文说明：{@code GeneratedPage} 是通过白名单与全部边界校验的一页候选，内链仍是 slug，等待服务端解析。
     * English summary: {@code GeneratedPage} is one candidate that cleared the whitelist and every bound, its links still
     * slugs awaiting server-side resolution.
     * @param slug 参数 页面短链名；parameter the slug.
     * @param title 参数 标题；parameter the title.
     * @param markdown 参数 正文；parameter the body.
     * @param tags 参数 标签；parameter the tags.
     * @param linkSlugs 参数 内链 slug；parameter the link slugs.
     * @param sources 参数 解析后的证据三元组；parameter the resolved evidence triples.
     */
    record GeneratedPage(String slug,
                         String title,
                         String markdown,
                         List<String> tags,
                         List<String> linkSlugs,
                         List<WikiSourceDTO> sources) {
    }

    /**
     * 中文说明：{@code Batch} 是一次待发布的批次：页面与草稿按索引配对，另带解析不到的内链计数。
     * English summary: {@code Batch} is one batch awaiting publication: pages paired with drafts by index plus the
     * unresolved-link tally.
     * @param pages 参数 页面载体；parameter the page carriers.
     * @param drafts 参数 草稿载体；parameter the draft carriers.
     * @param unresolvedLinks 参数 解析不到的内链数；parameter the unresolvable link count.
     */
    record Batch(List<WikiPageBO> pages,
                 List<WikiRevisionBO> drafts,
                 int unresolvedLinks) {
    }
}
