package top.egon.cola.component.yuheng.admin.wiki.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeBaseBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeDocumentBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeDocumentRevisionBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeJobBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeMemberDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobStageEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobStatusEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobTypeEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeMemberRoleEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeRevisionStatusEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeJobVO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgePageVO;
import top.egon.cola.component.yuheng.admin.knowledge.repository.KnowledgeRepository;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminNotFoundException;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminRevisionConflictException;
import top.egon.cola.component.yuheng.admin.wiki.converter.WikiRevisionConverter;
import top.egon.cola.component.yuheng.admin.wiki.domain.bo.WikiPageBO;
import top.egon.cola.component.yuheng.admin.wiki.domain.bo.WikiRevisionBO;
import top.egon.cola.component.yuheng.admin.wiki.domain.dto.WikiDraftCommandDTO;
import top.egon.cola.component.yuheng.admin.wiki.domain.dto.WikiGenerationCommandDTO;
import top.egon.cola.component.yuheng.admin.wiki.domain.dto.WikiPageQueryDTO;
import top.egon.cola.component.yuheng.admin.wiki.domain.dto.WikiPublicationCommandDTO;
import top.egon.cola.component.yuheng.admin.wiki.domain.dto.WikiSourceDTO;
import top.egon.cola.component.yuheng.admin.wiki.domain.dto.WikiTransitionCommandDTO;
import top.egon.cola.component.yuheng.admin.wiki.domain.enums.WikiPublicationPolicyEnum;
import top.egon.cola.component.yuheng.admin.wiki.domain.enums.WikiPublicationStatusEnum;
import top.egon.cola.component.yuheng.admin.wiki.domain.enums.WikiTransitionEventEnum;
import top.egon.cola.component.yuheng.admin.wiki.domain.vo.WikiGraphVO;
import top.egon.cola.component.yuheng.admin.wiki.domain.vo.WikiPageVO;
import top.egon.cola.component.yuheng.admin.wiki.repository.WikiRepository;
import top.egon.cola.component.yuheng.admin.wiki.service.WikiLifecycleService;
import top.egon.cola.component.yuheng.admin.wiki.service.WikiPublicationPolicyStrategy;
import top.egon.cola.component.yuheng.admin.wiki.service.WikiService;

/**
 * 中文说明：{@code WikiServiceImpl} 是 Wiki 管理面（API-023 至 API-028、API-031）的业务编排层，
 * 它只承担四类职责：<b>授权判定</b>（知识库可见性与角色集合，先 404 后 403，绝不泄漏存在性）、
 * <b>归属复核</b>（路径上的 {@code kbId} 与页面/修订实际所属库必须一致，跨库读一律 404）、
 * <b>期望版本预检</b>（调用方观察到的 {@code expectedRevision} 与库中现值不等即 409 并回报现值）以及
 * <b>证据新鲜度判定</b>（§7.3.4 的两类 stale：来源仍可读只是有了新版则 {@code stale=true} 且历史可看，
 * 来源已被删除或不再是 READY 则连正文都不返回）。它<b>不</b>实现状态机：DRAFT/PUBLISHING/PUBLISHED/
 * SUPERSEDED/ARCHIVED 之间的迁移、指针切换与审计行一律经 {@link WikiLifecycleService}，
 * 因为「谁能从哪个状态走到哪个状态」只有一个权威出处，管理面与 worker 面必须走同一条通路。
 * 本类也<b>不</b>开事务：读侧是短事务外的只读查询（§9.2 明令不承诺跨页快照），写侧的事务边界在
 * {@code WikiLifecycleServiceImpl} 的 {@code @Transactional} 方法上，在这里再包一层只会把一批页面拆成
 * 两个提交点。生成作业只落 {@code QUEUED} 行，模型调用与批次发布都在 worker 侧。
 * English summary: {@code WikiServiceImpl} is the business orchestration layer of the Wiki management face
 * (API-023 to API-028 plus API-031) and takes on exactly four duties: <b>authorization</b> (base visibility and the role
 * set, 404 before 403 so existence never leaks), <b>ownership re-check</b> (the path's {@code kbId} must match the page or
 * revision's real base, a cross-base read being a 404), <b>expected-revision pre-check</b> (a caller-observed
 * {@code expectedRevision} that differs from the stored one answers 409 carrying the current value), and <b>evidence
 * currency</b> (the two §7.3.4 kinds of staleness: a readable source that merely has a newer version yields
 * {@code stale = true} and history stays viewable, while a deleted or non-READY source withholds the body entirely).
 * It implements <b>no</b> state machine: migrations between DRAFT/PUBLISHING/PUBLISHED/SUPERSEDED/ARCHIVED, pointer swaps
 * and audit rows all go through {@link WikiLifecycleService}, because "which state may reach which state, and who" must
 * have one authoritative owner shared by the management face and the worker. This class also opens <b>no</b> transaction:
 * reads are queries outside a transaction (§9.2 promises no cross-page snapshot) and the write transaction boundary sits
 * on {@code WikiLifecycleServiceImpl}'s {@code @Transactional} methods, so wrapping another one here would only split one
 * batch across two commit points. A generation job only lands as {@code QUEUED}; the model call and the batch publication
 * belong to the worker.
 *
 * 用法 / Usage: 由 {@code WikiController} 以 {@code @Qualifier("wikiServiceImpl")} 注入，参数级校验先由本类上的
 * {@code @Validated} 与端口注解完成，再进入下面的编排；每个写方法返回的都是重新读取后的投影，
 * 因此调用方永远看到服务端权威值而不是自己提交的内容。日志只记 id、状态与计数，正文、提示词与向量一律不落日志。
 * Injected by {@code WikiController} under {@code @Qualifier("wikiServiceImpl")}: parameter validation runs first via
 * {@code @Validated} and the port annotations, then the orchestration below takes over, and every write returns a
 * re-read projection so the caller always sees the server's authoritative row rather than what it submitted. Logs carry
 * ids, statuses and counts only — never a body, a prompt or a vector.
 */
@Slf4j
@Validated
@Service("wikiServiceImpl")
@RequiredArgsConstructor
public class WikiServiceImpl implements WikiService {

    /** 中文说明：无权限与不存在共用知识面既有稳定码，避免同一事实出现第二个码。 English summary: the knowledge face's existing stable codes are reused so one fact never gains a second code. */
    private static final String NOT_FOUND = "KNOWLEDGE_RESOURCE_NOT_FOUND";

    private static final String FORBIDDEN = "KNOWLEDGE_FORBIDDEN";

    private static final String VALIDATION_FAILED = "KNOWLEDGE_VALIDATION_FAILED";

    private static final String DEPENDENCY_UNAVAILABLE = "YUHENG_DEPENDENCY_UNAVAILABLE";

    /** 中文说明：API-031 额外要求的功能能力：下线是公开面收缩，必须同时持有知识写能力与库内 EDITOR。 English summary: API-031's extra capability: withdrawing a publication needs the knowledge write authority as well as EDITOR on the base. */
    private static final Set<String> KNOWLEDGE_WRITE_AUTHORITIES =
            Set.of("CAP_yuheng:knowledge:write", "CAP_*");

    /** 中文说明：可读角色集合。/ the roles allowed to read. */
    private static final Set<KnowledgeMemberRoleEnum> READER_OR_ABOVE =
            Set.of(KnowledgeMemberRoleEnum.READER, KnowledgeMemberRoleEnum.EDITOR, KnowledgeMemberRoleEnum.OWNER);

    /** 中文说明：可写角色集合：草稿、发布与下线都只认 EDITOR 及以上。/ the roles allowed to write. */
    private static final Set<KnowledgeMemberRoleEnum> EDITOR_OR_ABOVE =
            Set.of(KnowledgeMemberRoleEnum.EDITOR, KnowledgeMemberRoleEnum.OWNER);

    /** 中文说明：新建作业行的业务 revision 哨兵，与知识面同值；{@code *_revision} 列的创建态。 English summary: the create-intent revision sentinel of a job row, the same value the knowledge face uses. */
    private static final long CREATE_REVISION = 0L;

    /** 中文说明：意图键最短长度，短于它的调用方键必须换成派生键，因为载体列要求 16–64。 English summary: the shortest usable intent key; a shorter supplied key is replaced because the column demands 16-64. */
    private static final int INTENT_KEY_MIN_LENGTH = 16;

    /** 中文说明：生成意图的判别键，使同一 {@code Idempotency-Key} 不会在上传、重索引与生成之间互相冒充。 English summary: the discriminator of a generation intent, so one Idempotency-Key cannot impersonate itself across upload, reindex and generation. */
    private static final String GENERATION_OPERATION = "WIKI_GENERATE";

    /** 中文说明：图输出合同上限：节点至多 100。/ the graph contract's node ceiling. */
    private static final int GRAPH_NODE_LIMIT = 100;

    /** 中文说明：图输出合同上限：边至多 200。/ the graph contract's edge ceiling. */
    private static final int GRAPH_EDGE_LIMIT = 200;

    /** 中文说明：API-026 的稳定原因码，写入状态机审计行。/ the stable reason code API-026 hands the state machine. */
    private static final String DRAFT_REASON = "ADMIN_REPLACE_DRAFT";

    /** 中文说明：API-027 的稳定原因码。/ the stable reason code of API-027. */
    private static final String PUBLISH_REASON = "ADMIN_DIRECT_PUBLISH";

    /** 中文说明：API-031 的稳定原因码。/ the stable reason code of API-031. */
    private static final String UNPUBLISH_REASON = "ADMIN_UNPUBLISH";

    /** 中文说明：十进制字符串 id 的数值序：先比长度再比字面，因此与数据库的 bigint 排序一致。 English summary: numeric order over decimal-string ids, length first then literal, matching the database's bigint ordering. */
    private static final Comparator<String> DECIMAL_ID_ORDER =
            Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder());

    @Qualifier("wikiRepository")
    private final WikiRepository wikiRepository;

    @Qualifier("knowledgeRepository")
    private final KnowledgeRepository knowledgeRepository;

    @Qualifier("wikiLifecycleServiceImpl")
    private final WikiLifecycleService wikiLifecycleService;

    /** 中文说明：草稿命令到修订载体的唯一出口，内容列的冻结口径（列表复制、摘要与状态初值）都在这一个转换器里。 English summary: the only exit turning a draft command into a revision carrier, since one converter owns the freezing of the content columns, the digest and the initial status. */
    @Qualifier("wikiRevisionConverter")
    private final WikiRevisionConverter wikiRevisionConverter;

    /** 中文说明：发布策略注册表，是「本期到底接入哪些发布策略」的唯一事实来源：本类不写死 {@code DIRECT}， 草稿冻结的策略一律从这张表里查出来。 English summary: the publication policy registry, the single fact of which policies are wired this release: DIRECT is never hardcoded here, a draft's frozen policy is looked up in this map. */
    @Qualifier("wikiPublicationPolicyStrategyRegistry")
    private final Map<WikiPublicationPolicyEnum, WikiPublicationPolicyStrategy> wikiPublicationPolicyStrategyRegistry;

    @Qualifier("knowledgeClock")
    private final Clock knowledgeClock;

    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper jacksonObjectMapper;

    /**
     * 中文说明：API-023 目录分页。先定角色（{@code includeDraft=true} 必须有 EDITOR，否则 403 而不是悄悄降级），
     * 再取当页与同谓词总数；每条页面按其「当前可见版本」批量读回修订，一次判定来源可读性与新鲜度。
     * 某个来源已被删除或不再 READY 时，§7.3.4 要求连正文都不给，因此该行不进入 {@code items}
     * （Spec 明言 {@code total} 是本次匹配总数而非跨页快照，items 少于 total 是允许且如实的结果）。
     * English summary: API-023 pages the catalog. The role settles first ({@code includeDraft = true} needs EDITOR, else 403
     * rather than a quiet downgrade), then the page and its same-predicate total are read, and every row's currently visible
     * revision comes back in one batch for a single evidence pass. When a source was deleted or stopped being READY,
     * §7.3.4 withholds even the body, so that row leaves {@code items} — the Spec states {@code total} counts matches rather
     * than snapshotting the page, so fewer items than total is the permitted, honest outcome.
     */
    @Override
    public KnowledgePageVO<WikiPageVO> listPages(AdminActor actor, String kbId, WikiPageQueryDTO query) {
        requireVisibleBase(actor, kbId, query.isIncludeDraft() ? EDITOR_OR_ABOVE : READER_OR_ABOVE);
        long total = wikiRepository.countPages(kbId, query);
        List<WikiPageBO> pages = total == 0L
                ? List.of()
                : wikiRepository.listPages(kbId, query);
        Evidence evidence = new Evidence(kbId);
        List<WikiPageVO> items = views(kbId, pages, query.isIncludeDraft(), evidence);
        log.info("wiki catalog served kb={} actor={} page={} size={} items={} total={} includeDraft={}",
                kbId, actor.actorId(), query.getPage(), query.getSize(), items.size(), total, query.isIncludeDraft());
        return KnowledgePageVO.<WikiPageVO>builder()
                .items(items)
                .page(query.getPage())
                .size(query.getSize())
                .total(total)
                .build();
    }

    /**
     * 中文说明：API-024 排队一次生成作业。顺序固定为「角色 → 来源复核 → 目标基线 → 幂等意图 → 落行」：
     * 来源必须是本库、READY 且正是所属文档的当前活动版本，否则按 422 拒绝而不是排队一个注定 STALE 的作业；
     * 新页要求 {@code expectedRevision=0}，追加页则必须匹配页面现值（409 回报现值），因为 worker 会以这个
     * 基线判断「用户是否在我之后又存过草稿」。意图键与请求摘要都来自规范化后的载荷（键按字典序写入），
     * 同一意图重放返回既有作业投影，摘要不同即 409，绝不覆盖既有作业。
     * English summary: API-024 queues one generation job in the fixed order "role, then sources, then target baseline, then
     * the idempotent intent, then the row". Sources must be this base's READY revisions that are still their document's
     * active version, else a 422 refuses the call instead of queueing a job doomed to go STALE; a new page requires
     * {@code expectedRevision = 0} while an append must match the page's current value (409 reporting it), because the worker
     * uses that baseline to decide whether the user drafted again after it. Both the intent key and the request digest come
     * from the canonicalized payload (alphabetical keys): a replay of the same intent returns the existing job projection
     * and a different digest is a 409 that never overwrites the stored job.
     */
    @Override
    public KnowledgeJobVO createGenerationJob(AdminActor actor,
                                              String kbId,
                                              WikiGenerationCommandDTO command,
                                              String idempotencyKey) {
        requireVisibleBase(actor, kbId, EDITOR_OR_ABOVE);
        List<WikiSourceDTO> frozen = verifyGenerationSources(kbId, command.getSourceRevisionIds());
        String pageId = StringUtils.trimToNull(command.getPageId());
        long basePageRevision = pageId == null
                ? requireCreateRevision(command.getExpectedRevision())
                : requireAppendBaseline(kbId, pageId, command.getExpectedRevision());
        JsonNode payload = canonicalGenerationIntent(kbId, pageId, basePageRevision, frozen);
        String requestHash = sha256Hex(payload.toString());
        String intentKey = intentKey(idempotencyKey, actor.actorId(), kbId, payload.toString());
        KnowledgeJobTypeEnum type = KnowledgeJobTypeEnum.WIKI_GENERATE;
        Optional<KnowledgeJobBO> existing =
                knowledgeRepository.findJobByIntent(kbId, actor.actorId(), type, intentKey);
        if (existing.isPresent()) {
            assertSameIntent(existing.get(), requestHash);
            log.info("wiki generation intent replayed kb={} actor={} job={} status={}",
                    kbId, actor.actorId(), existing.get().getId(), existing.get().getStatus());
            return jobView(existing.get());
        }
        KnowledgeJobBO queued = knowledgeRepository.insertJob(queuedGenerationJob(
                kbId, actor, pageId, intentKey, requestHash, payload));
        log.info("wiki generation queued kb={} actor={} job={} sources={} pageId={}",
                kbId, actor.actorId(), queued.getId(), frozen.size(), pageId);
        return jobView(queued);
    }

    /**
     * 中文说明：API-025 读取单页。缺省读当前发布版；带 {@code revisionId} 时它必须属于该页面（跨页/跨库 404）。
     * 读者只允许当前或历史已发布版本，草稿与从未发布的修订一律要求 EDITOR；来源被删除或不再 READY 时按
     * §7.3.4 直接 404 拒绝正文，只有「来源仍可读但有新版」才以 {@code stale=true} 的形式放行历史内容。
     * English summary: API-025 reads one page, the current publication by default; a supplied {@code revisionId} must belong
     * to that page (cross-page or cross-base is a 404). A reader reaches the current or a historical publication only, while
     * drafts and never-published revisions require EDITOR; a deleted or non-READY source refuses the body outright as 404
     * per §7.3.4, and only a readable source that merely has a newer version passes history through as {@code stale = true}.
     */
    @Override
    public WikiPageVO getPage(AdminActor actor, String kbId, String pageId, String revisionId) {
        KnowledgeBaseBO base = requireVisibleBase(actor, kbId, READER_OR_ABOVE);
        WikiPageBO page = requirePageInBase(kbId, pageId);
        WikiRevisionBO revision = StringUtils.isBlank(revisionId)
                ? requireVisibleRevision(kbId, page, false)
                : requireRevisionOf(kbId, page, revisionId);
        if (!isPublishedHistory(revision)) {
            requireRole(base, actor, EDITOR_OR_ABOVE);
        }
        Evidence evidence = new Evidence(kbId);
        if (!evidence.readable(revision)) {
            throw bodyRefused();
        }
        return view(page, revision, evidence.stale(revision));
    }

    /**
     * 中文说明：API-026 保存人工修订。slug 是页面身份的一部分，本方法从不改写它（内链因此不会漂移）；
     * 命令经转换器变成 DRAFT 载体并冻结当次在登记的发布策略，随后交 {@link WikiLifecycleService#saveDraft}
     * 原子落位（新页建行、旧草稿按 REPLACE_DRAFT 归档、草稿指针切换、审计同事务）。返回重新读取的投影，
     * 使调用方看到服务端的 revision 与状态而不是自己提交的副本。
     * English summary: API-026 stores a human revision. The slug belongs to the page's identity and is never rewritten here,
     * so links cannot drift; the command becomes a DRAFT carrier through the converter with the registered publication policy
     * frozen onto it, and {@link WikiLifecycleService#saveDraft} then places it atomically (a new page row, the previous draft
     * archived under REPLACE_DRAFT, the draft pointer moved, the audit in the same transaction). The projection returned is
     * re-read so the caller sees the server's revision and status rather than its own copy.
     */
    @Override
    public WikiPageVO replaceDraft(AdminActor actor, String kbId, String pageId, WikiDraftCommandDTO command) {
        requireVisibleBase(actor, kbId, EDITOR_OR_ABOVE);
        WikiPageBO page = requirePageInBase(kbId, pageId);
        assertRevision(page.getRevision(), requireExpectedRevision(command.getExpectedRevision(), pageId));
        WikiRevisionBO draft = wikiRevisionConverter.newDraft(
                command, kbId, page.getId(), actor.actorId(), null, inForcePublicationPolicy());
        wikiLifecycleService.saveDraft(page, draft, actor);
        WikiPageBO placed = requirePageInBase(kbId, pageId);
        WikiRevisionBO revision = requireRevisionOf(kbId, placed, draft.getId());
        Evidence evidence = new Evidence(kbId);
        if (!evidence.readable(revision)) {
            throw bodyRefused();
        }
        log.info("wiki draft stored kb={} page={} revision={} actor={} sources={}",
                kbId, placed.getId(), revision.getId(), actor.actorId(), revision.getSources().size());
        return view(placed, revision, evidence.stale(revision));
    }

    /**
     * 中文说明：API-027 直接发布一个草稿。先复核归属与期望版本，再判定幂等：页面当前发布指针已指向该修订
     * 且该修订确为 PUBLISHED 时，直接回报现状而不重复推进版本（§9.2.27 要求重复发布保持请求效果）。
     * 否则把 {@code [DIRECT_PUBLISH, COMMIT_PUBLICATION]} 作为一对命令交状态机在同一事务内推进——第二条命令
     * 的期望状态版本必然比第一条高一，因为每一次状态迁移都会推进 {@code publicationVersion}；
     * 来源失鲜、CAS 零行与策略未接入都由状态机如实抛出，本方法不改写、不重试、更不伪造评审通过。
     * English summary: API-027 publishes one draft directly. Ownership and the expected revision settle first, then the
     * idempotent case: when the page's published pointer already is this revision and it really is PUBLISHED, the current
     * projection answers straight away without advancing a version again (§9.2.27 keeps a repeated publication at its
     * request effect). Otherwise {@code [DIRECT_PUBLISH, COMMIT_PUBLICATION]} goes to the state machine as a pair inside one
     * transaction — the second command's expected status version is exactly one higher, since every migration advances
     * {@code publicationVersion}. A stale source, a zero-row compare-and-set and an unwired policy surface from the machine
     * untouched: this method neither rewrites, retries nor fabricates an approval.
     */
    @Override
    public WikiPageVO publishRevision(AdminActor actor,
                                      String kbId,
                                      String pageId,
                                      WikiPublicationCommandDTO command) {
        requireVisibleBase(actor, kbId, EDITOR_OR_ABOVE);
        WikiPageBO page = requirePageInBase(kbId, pageId);
        long expectedRevision = requireExpectedRevision(command.getExpectedRevision(), pageId);
        assertRevision(page.getRevision(), expectedRevision);
        WikiRevisionBO draft = requireRevisionOf(kbId, page, command.getDraftRevisionId());
        if (StringUtils.equals(page.getPublishedRevisionId(), draft.getId()) && draft.isPublished()) {
            log.info("wiki publication already effective kb={} page={} revision={} actor={}",
                    kbId, page.getId(), draft.getId(), actor.actorId());
            Evidence effective = new Evidence(kbId);
            if (!effective.readable(draft)) {
                throw bodyRefused();
            }
            return view(page, draft, effective.stale(draft));
        }
        wikiLifecycleService.transitionAll(List.of(
                migration(page.getId(), draft.getId(), WikiTransitionEventEnum.DIRECT_PUBLISH,
                        expectedRevision, draft.getPublicationVersion(), PUBLISH_REASON),
                migration(page.getId(), draft.getId(), WikiTransitionEventEnum.COMMIT_PUBLICATION,
                        expectedRevision, draft.getPublicationVersion() + 1L, PUBLISH_REASON)), actor);
        WikiPageBO published = requirePageInBase(kbId, pageId);
        WikiRevisionBO revision = requireRevisionOf(kbId, published, draft.getId());
        Evidence evidence = new Evidence(kbId);
        if (!evidence.readable(revision)) {
            throw bodyRefused();
        }
        log.info("wiki publication committed kb={} page={} revision={} pageRevision={} actor={}",
                kbId, published.getId(), revision.getId(), published.getRevision(), actor.actorId());
        return view(published, revision, evidence.stale(revision));
    }

    /**
     * 中文说明：API-028 关联图。节点只来自当前已发布修订（未发布与已下线一律不出现，连标题都不给），
     * 边只在两端都可见时成立，因此永不会有悬空边。带 {@code pageId} 时收缩为该中心的一跳邻域
     * （出链与入链都算关联）；节点按 id 数值序稳定裁剪到 {@code limit}，边裁剪到 200，
     * 任何一种裁剪都会把 {@code truncated} 置真而不是假装图是完整的。
     * English summary: API-028 answers the graph. Nodes come from currently published revisions only, so an unpublished or
     * withdrawn page contributes not even its title, and an edge exists only between two visible nodes, which rules out a
     * dangling edge by construction. With a {@code pageId} the answer shrinks to that centre's one-hop neighbourhood (both
     * outgoing and incoming links count as relations); nodes trim to {@code limit} in stable numeric id order and edges to 200,
     * and any such trim sets {@code truncated} rather than pretending the graph arrived whole.
     */
    @Override
    public WikiGraphVO graph(AdminActor actor, String kbId, String pageId, int limit) {
        requireVisibleBase(actor, kbId, READER_OR_ABOVE);
        int nodeLimit = Math.min(limit, GRAPH_NODE_LIMIT);
        Map<String, WikiRevisionBO> published = publishedByPage(kbId, nodeLimit);
        boolean truncated = published.size() >= nodeLimit;
        WikiPageBO centre = StringUtils.isBlank(pageId) ? null : requirePageInBase(kbId, pageId);
        if (centre != null) {
            if (StringUtils.isNotBlank(centre.getPublishedRevisionId())) {
                wikiRepository.findRevision(kbId, centre.getPublishedRevisionId())
                        .filter(WikiRevisionBO::isPublished)
                        .ifPresent(revision -> published.putIfAbsent(revision.getPageId(), revision));
            }
            Set<String> neighbourhood = new LinkedHashSet<>();
            neighbourhood.add(centre.getId());
            published.values().forEach(revision -> {
                if (StringUtils.equals(revision.getPageId(), centre.getId())) {
                    neighbourhood.addAll(revision.getLinks());
                } else if (revision.getLinks().contains(centre.getId())) {
                    neighbourhood.add(revision.getPageId());
                }
            });
            truncated |= retainOnlyVisibleLinks(published, centre, neighbourhood);
            published.keySet().retainAll(neighbourhood);
        }
        List<String> nodeIds = published.keySet().stream().sorted(DECIMAL_ID_ORDER).limit(nodeLimit).toList();
        truncated |= published.size() > nodeIds.size();
        Set<String> visible = new LinkedHashSet<>(nodeIds);
        List<WikiGraphVO.WikiGraphNodeVO> nodes = new ArrayList<>(nodeIds.size());
        for (String nodeId : nodeIds) {
            nodes.add(WikiGraphVO.WikiGraphNodeVO.builder()
                    .id(nodeId)
                    .title(published.get(nodeId).getTitle())
                    .build());
        }
        List<WikiGraphVO.WikiGraphEdgeVO> edges = new ArrayList<>();
        for (String nodeId : nodeIds) {
            for (String target : published.get(nodeId).getLinks()) {
                if (StringUtils.equals(nodeId, target) || !visible.contains(target)) {
                    continue;
                }
                if (edges.size() >= GRAPH_EDGE_LIMIT) {
                    truncated = true;
                    break;
                }
                edges.add(WikiGraphVO.WikiGraphEdgeVO.builder().source(nodeId).target(target).build());
            }
        }
        log.info("wiki graph served kb={} actor={} centre={} nodes={} edges={} truncated={}",
                kbId, actor.actorId(), pageId, nodes.size(), edges.size(), truncated);
        return WikiGraphVO.builder()
                .nodes(nodes)
                .edges(edges)
                .truncated(truncated)
                .build();
    }

    /**
     * 中文说明：API-031 下线当前发布版：除了库内 EDITOR，还额外要求知识写能力（§9.2.31 的授权行）。
     * 已经没有发布版时幂等地什么都不做（仍然先做权限复核，所以这不是绕过授权的 204）；否则把 UNPUBLISH
     * 事件交状态机——清发布指针与把修订置 ARCHIVED 在同一事务内原子完成，且 {@code publishedAt}、
     * {@code everPublished} 等历史事实全部保留，下线永不抹掉「曾经发布过」这条记录。
     * English summary: API-031 withdraws the current publication, requiring the knowledge write capability on top of EDITOR
     * membership (§9.2.31's authorization row). With no publication left it does nothing idempotently — the permission check
     * still ran first, so that 204 is never an authorization bypass — and otherwise hands the UNPUBLISH event to the state
     * machine, which clears the published pointer and archives the revision atomically in one transaction while keeping
     * {@code publishedAt} and {@code everPublished}: withdrawing never erases that something was published.
     */
    @Override
    public void unpublish(AdminActor actor, String kbId, String pageId, long expectedRevision) {
        KnowledgeBaseBO base = requireVisibleBase(actor, kbId, EDITOR_OR_ABOVE);
        assertWriteAuthority(base, actor);
        WikiPageBO page = requirePageInBase(kbId, pageId);
        assertRevision(page.getRevision(), expectedRevision);
        if (StringUtils.isBlank(page.getPublishedRevisionId())) {
            log.info("wiki unpublish already effective kb={} page={} actor={}", kbId, pageId, actor.actorId());
            return;
        }
        WikiRevisionBO revision = requireRevisionOf(kbId, page, page.getPublishedRevisionId());
        wikiLifecycleService.transition(
                migration(page.getId(), revision.getId(), WikiTransitionEventEnum.UNPUBLISH,
                        page.getRevision(), revision.getPublicationVersion(), UNPUBLISH_REASON),
                actor);
        log.info("wiki publication withdrawn kb={} page={} revision={} actor={}",
                kbId, page.getId(), revision.getId(), actor.actorId());
    }

    /**
     * 中文说明：按「当前可见版本」批量读回修订并逐条判定证据：先一次取齐当页所需的修订，再逐页判定。
     * 目录与单页共用这条通路，区别只在于目录丢弃不可读的行而单页抛出 404。
     * English summary: Reads back the visible revisions in one batch and judges the evidence per row: the revisions a page
     * needs come first, then each row is judged. The catalog and the single read share this route, differing only in that
     * the catalog drops an unreadable row while the single read raises a 404.
     */
    private List<WikiPageVO> views(String kbId, List<WikiPageBO> pages, boolean includeDraft, Evidence evidence) {
        if (pages.isEmpty()) {
            return List.of();
        }
        Map<String, WikiRevisionBO> byId = new HashMap<>();
        List<String> pointerIds = new ArrayList<>(pages.size() * 2);
        for (WikiPageBO page : pages) {
            String contentRevisionId = page.contentRevisionId(includeDraft);
            if (contentRevisionId != null) {
                pointerIds.add(contentRevisionId);
            }
        }
        if (pointerIds.isEmpty()) {
            return List.of();
        }
        for (WikiRevisionBO revision : wikiRepository.listRevisions(kbId, pointerIds)) {
            byId.put(revision.getId(), revision);
        }
        List<WikiPageVO> views = new ArrayList<>(pages.size());
        for (WikiPageBO page : pages) {
            String contentRevisionId = page.contentRevisionId(includeDraft);
            WikiRevisionBO revision = contentRevisionId == null ? null : byId.get(contentRevisionId);
            if (revision == null) {
                throw new GatewayAdminNotFoundException(NOT_FOUND + " wiki content revision was not found");
            }
            if (!evidence.readable(revision)) {
                log.info("wiki catalog row withheld kb={} page={} actor-independent reason=source unreadable",
                        kbId, page.getId());
                continue;
            }
            views.add(view(page, revision, evidence.stale(revision)));
        }
        return views;
    }

    /**
     * 中文说明：生成来源复核：每条都必须是本库、READY 且正是所属文档的当前活动 revision；
     * 重复的 id 只保留一次并按原序冻结。任何一条不合格即 422 拒绝整个请求，因为排队一个注定
     * {@code STALE} 的作业只会让用户看到「提交成功却永远不出页面」。
     * English summary: Re-checks the generation sources: each must be this base's READY revision and still its document's
     * active one, a repeated id collapsing to its first occurrence. One failure refuses the whole request as 422, because
     * queueing a job doomed to {@code STALE} would only show the user a submitted request that never produces a page.
     */
    private List<WikiSourceDTO> verifyGenerationSources(String kbId, List<String> sourceRevisionIds) {
        List<WikiSourceDTO> verified = new ArrayList<>(sourceRevisionIds.size());
        Set<String> seen = new LinkedHashSet<>();
        for (String revisionId : sourceRevisionIds) {
            if (!seen.add(revisionId)) {
                continue;
            }
            KnowledgeDocumentRevisionBO revision = knowledgeRepository.findRevision(kbId, revisionId)
                    .orElseThrow(() -> new GatewayAdminNotFoundException(
                            NOT_FOUND + " wiki generation source revision was not found"));
            KnowledgeDocumentBO document = knowledgeRepository.findDocument(kbId, revision.getDocumentId())
                    .orElseThrow(() -> new GatewayAdminNotFoundException(
                            NOT_FOUND + " wiki generation source document was not found"));
            if (revision.getStatus() != KnowledgeRevisionStatusEnum.READY
                    || !StringUtils.equals(document.getActiveRevisionId(), revision.getId())) {
                throw new CommonException(422, VALIDATION_FAILED,
                        "wiki generation sources must be this knowledge base's current active revisions");
            }
            verified.add(WikiSourceDTO.builder()
                    .documentRevisionId(revision.getId())
                    .sourceHash(revision.getContentHash())
                    .build());
        }
        if (verified.isEmpty()) {
            throw new CommonException(422, VALIDATION_FAILED, "wiki generation requires at least one usable source revision");
        }
        return List.copyOf(verified);
    }

    /**
     * 中文说明：构造生成作业的规范化意图：键按字典序写入，因此同一请求重放必然得到同一摘要；
     * {@code operation} 判别键保证同一 {@code Idempotency-Key} 不会在上传、重索引与生成之间互相冒充，
     * 而 {@code basePageRevision} 是 worker 判断「用户是否又存了草稿」的唯一基线。
     * 载荷形态与 {@code WikiGenerationStrategy.GenerationIntent} 的读侧一一对应。
     * English summary: Builds the canonical generation intent with alphabetical keys so a replay of the same request must
     * produce the same digest; the {@code operation} discriminator keeps one Idempotency-Key from impersonating itself across
     * upload, reindex and generation, while {@code basePageRevision} is the worker's only baseline for "did the user draft
     * again". The shape pairs one-to-one with the read side in {@code WikiGenerationStrategy.GenerationIntent}.
     */
    private JsonNode canonicalGenerationIntent(String kbId,
                                               String pageId,
                                               long basePageRevision,
                                               List<WikiSourceDTO> sources) {
        ObjectNode canonical = jacksonObjectMapper.createObjectNode();
        canonical.put("basePageRevision", basePageRevision);
        canonical.put("kbId", kbId);
        canonical.put("operation", GENERATION_OPERATION);
        if (pageId == null) {
            canonical.putNull("pageId");
        } else {
            canonical.put("pageId", pageId);
        }
        ArrayNode frozen = canonical.putArray("sources");
        for (WikiSourceDTO source : sources) {
            ObjectNode entry = frozen.addObject();
            entry.put("documentRevisionId", source.getDocumentRevisionId());
            entry.put("sourceHash", source.getSourceHash());
        }
        return canonical;
    }

    /** 中文说明：新页基线：{@code expectedRevision} 必须为 0，非零说明调用方把追加当成了新建。 English summary: The new-page baseline: expectedRevision must be zero, anything else means the caller read an append as a create. */
    private static long requireCreateRevision(Long expectedRevision) {
        if (expectedRevision == null || expectedRevision != CREATE_REVISION) {
            throw new CommonException(422, VALIDATION_FAILED,
                    "expectedRevision must be 0 when no wiki page exists yet");
        }
        return CREATE_REVISION;
    }

    /** 中文说明：追加基线：页面必须可读、属于本库，且 {@code expectedRevision} 等于其现值，否则 409 回报现值。 English summary: The append baseline: the page must be readable in this base and expectedRevision must equal its current value, else a 409 reports it. */
    private long requireAppendBaseline(String kbId, String pageId, Long expectedRevision) {
        WikiPageBO page = requirePageInBase(kbId, pageId);
        assertRevision(page.getRevision(), requireExpectedRevision(expectedRevision, pageId));
        return page.getRevision();
    }

    /** 中文说明：构造 {@code QUEUED} 的生成作业载体：{@code resourceId} 在追加时是页面 id、新建时退化为知识库 id， 因为该列 NOT NULL 而「尚无页面」是本合同的合法状态。 English summary: Builds the QUEUED generation carrier: resourceId is the page id for an append and falls back to the knowledge base id for a new page, since the column is NOT NULL while "no page yet" is a legal state of this contract. */
    private KnowledgeJobBO queuedGenerationJob(String kbId,
                                               AdminActor actor,
                                               String pageId,
                                               String intentKey,
                                               String requestHash,
                                               JsonNode payload) {
        return KnowledgeJobBO.builder()
                .kbId(kbId)
                .type(KnowledgeJobTypeEnum.WIKI_GENERATE)
                .resourceId(pageId == null ? kbId : pageId)
                .actorId(actor.actorId())
                .payload(payload)
                .idempotencyKey(intentKey)
                .requestHash(requestHash)
                .status(KnowledgeJobStatusEnum.QUEUED)
                .stage(KnowledgeJobStageEnum.QUEUED)
                .attempt(0)
                .nextAttemptAt(knowledgeClock.instant())
                .leaseOwner(null)
                .leaseToken(0L)
                .leaseExpiresAt(null)
                .errorCode(null)
                .result(null)
                .retryOfJobId(null)
                .revision(CREATE_REVISION)
                .build();
    }

    /** 中文说明：作业投影：只输出安全状态字段与时刻，不含载荷、租约或主体内部字段。 English summary: Projects a job, emitting the safe status fields and instants only, without the payload, the lease or internal actor fields. */
    private static KnowledgeJobVO jobView(KnowledgeJobBO job) {
        return KnowledgeJobVO.builder()
                .id(job.getId())
                .kbId(job.getKbId())
                .type(job.getType())
                .resourceId(job.getResourceId())
                .status(job.getStatus())
                .stage(job.getStage())
                .revision(job.getRevision())
                .attempt(job.getAttempt())
                .errorCode(job.getErrorCode())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    /** 中文说明：构造一条状态迁移命令；版本与原因码都在这里成形，之后只有状态机能裁决。 English summary: Builds one migration command; the versions and the reason code take shape here, and only the state machine adjudicates afterwards. */
    private static WikiTransitionCommandDTO migration(String pageId,
                                                      String revisionId,
                                                      WikiTransitionEventEnum event,
                                                      long expectedPageRevision,
                                                      long expectedPublicationVersion,
                                                      String reasonCode) {
        return WikiTransitionCommandDTO.builder()
                .pageId(pageId)
                .revisionId(revisionId)
                .event(event)
                .expectedPageRevision(expectedPageRevision)
                .expectedPublicationVersion(expectedPublicationVersion)
                .reasonCode(reasonCode)
                .build();
    }

    /** 中文说明：读取页面并复核它确实属于路径上的知识库，跨库按 404 处理而不是 403。 English summary: Reads a page and re-checks it belongs to the base on the path, a cross-base read being a 404 rather than a 403. */
    private WikiPageBO requirePageInBase(String kbId, String pageId) {
        WikiPageBO page = wikiRepository.findPage(pageId)
                .orElseThrow(() -> new GatewayAdminNotFoundException(NOT_FOUND + " wiki page was not found"));
        if (!StringUtils.equals(page.getKbId(), kbId)) {
            throw new GatewayAdminNotFoundException(NOT_FOUND + " wiki page was not found");
        }
        return page;
    }

    /** 中文说明：读取修订并复核它确实属于给定页面：跨页或跨库一律 404，免得用「存在但无权限」泄漏结构。 English summary: Reads a revision and re-checks it belongs to the given page, a cross-page or cross-base read being a 404 so existence never leaks a page structure. */
    private WikiRevisionBO requireRevisionOf(String kbId, WikiPageBO page, String revisionId) {
        WikiRevisionBO revision = wikiRepository.findRevision(kbId, revisionId)
                .orElseThrow(() -> new GatewayAdminNotFoundException(NOT_FOUND + " wiki revision was not found"));
        if (!StringUtils.equals(revision.getPageId(), page.getId())) {
            throw new GatewayAdminNotFoundException(NOT_FOUND + " wiki revision was not found");
        }
        return revision;
    }

    /** 中文说明：取「当前可见版本」：缺省是已发布版，已发布为空时（已下线或从未发布）退到草稿版， 两者都为空即数据不完整，按 404 如实收束而不是返回空壳。 English summary: Takes the currently visible revision: the publication by default, the draft when nothing is published (withdrawn or never published), and an honest 404 when neither pointer exists rather than an empty shell. */
    private WikiRevisionBO requireVisibleRevision(String kbId, WikiPageBO page, boolean preferDraft) {
        String revisionId = preferDraft && StringUtils.isNotBlank(page.getDraftRevisionId())
                ? page.getDraftRevisionId()
                : StringUtils.defaultIfBlank(page.getPublishedRevisionId(), page.getDraftRevisionId());
        if (StringUtils.isBlank(revisionId)) {
            throw new GatewayAdminNotFoundException(NOT_FOUND + " wiki page has no readable content revision");
        }
        return requireRevisionOf(kbId, page, revisionId);
    }

    /** 中文说明：修订是否属于「读者可见的已发布历史」：当前发布、已被接替、或曾发布后归档都算， 从未发布过的修订（含 DRAFT 与 PUBLISHING 中间态）一律不算。 English summary: Whether a revision counts as published history a reader may see: the current publication, a superseded one or an archived one that was published before, while a revision never published — a DRAFT or the PUBLISHING intermediate — never does. */
    private static boolean isPublishedHistory(WikiRevisionBO revision) {
        if (Boolean.TRUE.equals(revision.getEverPublished())) {
            return true;
        }
        return revision.getPublicationStatus() == WikiPublicationStatusEnum.PUBLISHED
                || revision.getPublicationStatus() == WikiPublicationStatusEnum.SUPERSEDED;
    }

    /** 中文说明：读取知识库并复核角色：不可见即 404，角色不足即 403，顺序固定以不泄漏存在性。 English summary: Reads the knowledge base and re-checks the role: invisible is a 404 and an insufficient role a 403, in that fixed order so existence never leaks. */
    private KnowledgeBaseBO requireVisibleBase(AdminActor actor, String kbId, Set<KnowledgeMemberRoleEnum> allowed) {
        KnowledgeBaseBO base = knowledgeRepository.findBase(kbId)
                .orElseThrow(() -> new GatewayAdminNotFoundException(NOT_FOUND + " knowledge base was not found"));
        requireRole(base, actor, allowed);
        return base;
    }

    /** 中文说明：角色判定：owner 即 OWNER，否则取该主体成员角色中最高的一条；无匹配或不足即 403。 English summary: The role decision: the owner is OWNER, otherwise the actor's highest membership wins, and no match or an insufficient one is a 403. */
    private static void requireRole(KnowledgeBaseBO base, AdminActor actor, Set<KnowledgeMemberRoleEnum> allowed) {
        KnowledgeMemberRoleEnum derived = null;
        if (StringUtils.equals(base.getOwnerActorId(), actor.actorId())) {
            derived = KnowledgeMemberRoleEnum.OWNER;
        } else {
            List<KnowledgeMemberDTO> members = base.getMembers();
            for (KnowledgeMemberDTO member : members == null ? List.<KnowledgeMemberDTO>of() : members) {
                if (member == null || !StringUtils.equals(member.getActorId(), actor.actorId())
                        || member.getRole() == null) {
                    continue;
                }
                if (derived == null || member.getRole().getCode() > derived.getCode()) {
                    derived = member.getRole();
                }
            }
        }
        if (derived == null || !allowed.contains(derived)) {
            throw new CommonException(403, FORBIDDEN,
                    "the presenting identity holds no sufficient role on this knowledge base");
        }
    }

    /** 中文说明：下线额外要求的功能能力；成员角色不足以收缩公开面时按 403 拒绝。 English summary: The extra capability an unpublish needs: membership alone cannot shrink the public face, so lacking it answers 403. */
    private static void assertWriteAuthority(KnowledgeBaseBO base, AdminActor actor) {
        Set<String> granted = new LinkedHashSet<>(actor.scopes());
        granted.addAll(actor.roles());
        for (String authority : KNOWLEDGE_WRITE_AUTHORITIES) {
            if (granted.contains(authority)) {
                return;
            }
        }
        throw new CommonException(403, FORBIDDEN,
                "withdrawing a wiki publication requires the knowledge write capability");
    }

    /** 中文说明：本期在登记的发布策略：注册表里恰好一个键时就是它，多策略并存或一个都没有都失败关闭， 因为「按配置决定策略」绝不能退化成「按 bean 顺序决定」。 English summary: The registered publication policy in force: exactly one registry key names it, while several policies or none fail closed, because "the configuration decides" must never degrade into "bean order decides". */
    private WikiPublicationPolicyEnum inForcePublicationPolicy() {
        Set<WikiPublicationPolicyEnum> registered = wikiPublicationPolicyStrategyRegistry.keySet();
        if (registered.size() != 1) {
            throw new CommonException(503, DEPENDENCY_UNAVAILABLE,
                    "exactly one wiki publication policy must be registered for drafting to be possible");
        }
        return registered.iterator().next();
    }

    /** 中文说明：把来源不可读收束为 404：§7.3.4 要求被撤回的来源既不返回正文也不返回标题， 且不区分「没有」与「有但被撤回」，以免存在性本身成为泄漏面。 English summary: Collapses an unreadable source into a 404: §7.3.4 withholds both body and title of a withdrawn source and keeps "absent" indistinguishable from "present but withdrawn", so existence itself never becomes the leak. */
    private static GatewayAdminNotFoundException bodyRefused() {
        return new GatewayAdminNotFoundException(NOT_FOUND + " wiki page content is unavailable for its sources");
    }

    /** 中文说明：期望 revision 规范化：null 或非正按 422 拒绝，避免退化成无条件写。 English summary: Normalizes the expected revision: null or non-positive is a 422 so a write may never degrade into an unconditional one. */
    private static long requireExpectedRevision(Long expectedRevision, String resourceId) {
        if (expectedRevision == null || expectedRevision < 1L) {
            throw new CommonException(422, VALIDATION_FAILED,
                    "expectedRevision is required and must be positive for " + resourceId);
        }
        return expectedRevision;
    }

    /** 中文说明：乐观版本预检：现值不等即 409 并携带库中权威现值。 English summary: The optimistic pre-check: a mismatch is a 409 carrying the stored authoritative value. */
    private static void assertRevision(long storedRevision, long expectedRevision) {
        if (storedRevision != expectedRevision) {
            throw new GatewayAdminRevisionConflictException(storedRevision);
        }
    }

    /** 中文说明：意图键规范化：足够长的可打印 ASCII 键原样使用，否则以载荷派生，重放必然命中同一意图。 English summary: Normalizes the intent key: a long enough printable ASCII key passes through, otherwise the payload derives one, so a replay must hit the same intent. */
    private static String intentKey(String supplied, String... seedParts) {
        String trimmed = StringUtils.trimToEmpty(supplied);
        if (trimmed.length() >= INTENT_KEY_MIN_LENGTH && isPrintableAscii(trimmed)) {
            return trimmed;
        }
        StringBuilder seed = new StringBuilder();
        for (String part : seedParts) {
            seed.append('|').append(StringUtils.trimToEmpty(part));
        }
        return sha256Hex(seed.toString());
    }

    /** 中文说明：意图键只接受可打印 ASCII，控制字符不得进入唯一键列。 English summary: Only printable ASCII is accepted for an intent key so control characters never reach a unique column. */
    private static boolean isPrintableAscii(String value) {
        for (int index = 0; index < value.length(); index = index + 1) {
            char current = value.charAt(index);
            if (current < 0x21 || current > 0x7E) {
                return false;
            }
        }
        return true;
    }

    /** 中文说明：同一意图键携带不同载荷即 409，绝不覆盖既有作业。 English summary: The same intent key with a different payload is a 409 and never an overwrite of the stored job. */
    private static void assertSameIntent(KnowledgeJobBO existing, String requestHash) {
        if (!Objects.equals(existing.getRequestHash(), requestHash)) {
            throw new CommonException(409, "KNOWLEDGE_IDEMPOTENCY_CONFLICT",
                    "the idempotency key was reused with a different payload");
        }
    }

    /** 中文说明：SHA-256 小写十六进制摘要。 English summary: The lowercase SHA-256 hex digest. */
    private static String sha256Hex(String value) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    /** 中文说明：把页面与其可见修订投影成完整 VO：五个状态列描述的都是所返回的那一版， 页面自身的乐观 revision 与它分列输出，绝不互相顶替。 English summary: Projects a page with its visible revision into the full VO: the five status columns describe the returned revision while the page's own optimistic revision stays a separate field, never substituting for the other. */
    private static WikiPageVO view(WikiPageBO page, WikiRevisionBO revision, boolean stale) {
        return WikiPageVO.builder()
                .id(page.getId())
                .kbId(page.getKbId())
                .slug(page.getSlug())
                .title(revision.getTitle())
                .draftRevisionId(page.getDraftRevisionId())
                .publishedRevisionId(page.getPublishedRevisionId())
                .publicationStatus(revision.getPublicationStatus())
                .reviewStatus(revision.getReviewStatus())
                .publicationPolicySnapshot(revision.getPublicationPolicySnapshot())
                .publicationVersion(revision.getPublicationVersion())
                .publicationErrorCode(revision.getPublicationErrorCode())
                .contentRevisionId(revision.getId())
                .markdown(revision.getMarkdown())
                .tags(revision.getTags())
                .links(revision.getLinks())
                .sources(revision.getSources())
                .stale(stale)
                .revision(page.getRevision())
                .build();
    }

    /** 中文说明：按页面归集当前已发布修订（一页至多一条，重复者保留先读到的那条），并按 id 稳定排序， 因此同一请求的裁剪结果不会因数据库返回顺序而漂移。 English summary: Collects the current published revision per page (one row each, the first read winning) and orders it stably by id, so trimming cannot drift with the database's return order. */
    private Map<String, WikiRevisionBO> publishedByPage(String kbId, int nodeLimit) {
        Map<String, WikiRevisionBO> byPage = new TreeMap<>(DECIMAL_ID_ORDER);
        for (WikiRevisionBO revision : wikiRepository.listPublishedRevisions(kbId, nodeLimit)) {
            byPage.putIfAbsent(revision.getPageId(), revision);
        }
        return byPage;
    }

    /** 中文说明：一跳邻域裁剪的补充判定：中心页面声明的链接中有多少没有对应的可见节点， 即有多少关联被裁掉——它只数「中心自己的出链」，因此不需要额外的逐链接查询。 English summary: The one-hop trim test: how many of the centre's own links lack a visible node, i.e. how many relations were trimmed. Counting only the centre's outgoing links keeps this free of a per-link lookup. */
    private boolean retainOnlyVisibleLinks(Map<String, WikiRevisionBO> published,
                                           WikiPageBO centre,
                                           Set<String> neighbourhood) {
        WikiRevisionBO centreRevision = published.get(centre.getId());
        if (centreRevision == null) {
            return !neighbourhood.isEmpty();
        }
        long dropped = centreRevision.getLinks().stream()
                .filter(link -> !neighbourhood.contains(link))
                .count();
        return dropped > 0L;
    }

    /**
     * 中文说明：{@code Evidence} 是一次请求内的来源判定缓存：按 revision id 与 document id 各缓存一次读取，
     * 因为目录当页至多 20 页、每页至多 100 条来源，逐条重读会把一次列表变成上千次查询。
     * 判定口径与 §7.3.4 一致：来源行不存在、不再是 READY 或其文档已不可读即「不可读」（正文一律不给）；
     * 仍可读但已不是文档的当前活动版本、或冻结摘要与现值不符即 {@code stale}，历史仍可看只是不得进入新回答。
     * English summary: {@code Evidence} caches the source judgements of one request, keyed once per revision id and once per
     * document id, because a catalog page holds up to 20 pages of up to 100 sources each and re-reading them row by row would
     * turn one list into a thousand queries. The rule is §7.3.4's: a missing source row, one that stopped being READY, or a
     * document that is no longer readable means unreadable (no body at all), while a still-readable source that is no longer
     * its document's active version — or whose frozen digest no longer matches — means {@code stale}: history stays viewable
     * but it may not enter a new answer.
     */
    private final class Evidence {

        private final String kbId;

        private final Map<String, KnowledgeDocumentRevisionBO> revisions = new HashMap<>();

        private final Map<String, KnowledgeDocumentBO> documents = new HashMap<>();

        private final Set<String> missingRevisions = new LinkedHashSet<>();

        private final Set<String> missingDocuments = new LinkedHashSet<>();

        private Evidence(String kbId) {
            this.kbId = kbId;
        }

        /** 中文说明：来源集合是否整体可读（有一条不可读即整体不可读）。 English summary: Whether the whole source set is readable, one unreadable source making it so. */
        private boolean readable(WikiRevisionBO revision) {
            for (WikiSourceDTO source : revision.getSources()) {
                if (sourceRow(source) == null) {
                    return false;
                }
            }
            return true;
        }

        /** 中文说明：来源集合是否全部仍是当前活动版本且摘要相符。 English summary: Whether every source is still its document's active version at the frozen digest. */
        private boolean stale(WikiRevisionBO revision) {
            for (WikiSourceDTO source : revision.getSources()) {
                KnowledgeDocumentRevisionBO row = sourceRow(source);
                if (row == null) {
                    return true;
                }
                KnowledgeDocumentBO document = documentRow(row.getDocumentId());
                if (document == null
                        || !StringUtils.equals(document.getActiveRevisionId(), row.getId())
                        || !StringUtils.equals(row.getContentHash(), source.getSourceHash())) {
                    return true;
                }
            }
            return false;
        }

        /** 中文说明：读取来源 revision 行：不存在或非 READY 都记为不可读并缓存为 {@code null}。 English summary: Reads the source revision row, recording a missing or non-READY one as unreadable by caching null. */
        private KnowledgeDocumentRevisionBO sourceRow(WikiSourceDTO source) {
            String revisionId = source.getDocumentRevisionId();
            if (missingRevisions.contains(revisionId)) {
                return null;
            }
            if (!revisions.containsKey(revisionId)) {
                KnowledgeDocumentRevisionBO row = knowledgeRepository.findRevision(kbId, revisionId)
                        .filter(found -> found.getStatus() == KnowledgeRevisionStatusEnum.READY)
                        .orElse(null);
                revisions.put(revisionId, row);
                if (row == null) {
                    missingRevisions.add(revisionId);
                }
            }
            return revisions.get(revisionId);
        }

        /** 中文说明：读取来源所属文档行，软删或跨库都读不到，同样缓存为 {@code null}。 English summary: Reads the owning document row, a soft-deleted or cross-base one being unreadable and cached as null too. */
        private KnowledgeDocumentBO documentRow(String documentId) {
            if (missingDocuments.contains(documentId)) {
                return null;
            }
            if (!documents.containsKey(documentId)) {
                KnowledgeDocumentBO row = knowledgeRepository.findDocument(kbId, documentId).orElse(null);
                documents.put(documentId, row);
                if (row == null) {
                    missingDocuments.add(documentId);
                }
            }
            return documents.get(documentId);
        }
    }
}
