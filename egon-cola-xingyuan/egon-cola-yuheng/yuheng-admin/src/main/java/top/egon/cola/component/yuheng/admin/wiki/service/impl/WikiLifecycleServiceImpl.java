package top.egon.cola.component.yuheng.admin.wiki.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeBaseBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeDocumentBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeDocumentRevisionBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeMemberDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeMemberRoleEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeRevisionStatusEnum;
import top.egon.cola.component.yuheng.admin.knowledge.repository.KnowledgeRepository;
import top.egon.cola.component.yuheng.admin.observability.domain.bo.GatewayAuditLogBO;
import top.egon.cola.component.yuheng.admin.observability.repository.GatewayAuditLogRepository;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;
import top.egon.cola.component.yuheng.admin.shared.domain.enums.AdminActorTypeEnum;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminNotFoundException;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminRevisionConflictException;
import top.egon.cola.component.yuheng.admin.shared.domain.validation.ExecuteGroup;
import top.egon.cola.component.yuheng.admin.wiki.domain.bo.WikiPageBO;
import top.egon.cola.component.yuheng.admin.wiki.domain.bo.WikiRevisionBO;
import top.egon.cola.component.yuheng.admin.wiki.domain.dto.WikiSourceDTO;
import top.egon.cola.component.yuheng.admin.wiki.domain.dto.WikiTransitionCommandDTO;
import top.egon.cola.component.yuheng.admin.wiki.domain.enums.WikiPublicationPolicyEnum;
import top.egon.cola.component.yuheng.admin.wiki.domain.enums.WikiPublicationStatusEnum;
import top.egon.cola.component.yuheng.admin.wiki.domain.enums.WikiReviewStatusEnum;
import top.egon.cola.component.yuheng.admin.wiki.domain.enums.WikiTransitionEventEnum;
import top.egon.cola.component.yuheng.admin.wiki.domain.vo.WikiTransitionResultVO;
import top.egon.cola.component.yuheng.admin.wiki.repository.WikiRepository;
import top.egon.cola.component.yuheng.admin.wiki.service.WikiLifecycleService;
import top.egon.cola.component.yuheng.admin.wiki.service.WikiPublicationPolicyStrategy;

/**
 * 中文说明：{@code WikiLifecycleServiceImpl} 是 §7.3.7 Wiki 状态机与 INTERNAL-001 的唯一实现，也是全模块唯一持有
 * 「哪些迁移合法」这一知识的地方。迁移表是一张 {@code EnumMap} 常量：{@code DIRECT_PUBLISH} 把 DRAFT 推到
 * PUBLISHING，{@code COMMIT_PUBLICATION} 把 PUBLISHING 推到 PUBLISHED 并顺带旧化上一页面上的已发布行、切换页面指针、
 * 清掉同一个 revision 留下的草稿指针，{@code PUBLICATION_FAILURE} 只把 PUBLISHING 拉回 DRAFT 并记下安全错误码，
 * {@code REPEAT_PUBLICATION} 描述「请求重复了已经生效的发布」，{@code REPLACE_DRAFT} 与 {@code UNPUBLISH} 分别把
 * 草稿与当前发布归档。表里没有的事件（{@code CREATE_REVISION}/{@code RESTORE_CONTENT}）是<b>放置</b>事件而不是迁移：
 * 它们要写入正文，而 INTERNAL-001 的命令载体里根本没有正文，因此从 {@link #transition} 进入只会得到
 * {@code WIKI_STATE_CONFLICT}，真正的落位由 {@link #saveDraft} 与 {@link #publishGenerated} 承担。
 * 三个不变量在本类收口：其一，<b>评审维度不归属于状态机</b>——评审列的取值与放行完全交给
 * {@code wikiPublicationPolicyStrategyRegistry} 按 revision 冻结策略快照查出的策略，DIRECT 恒答 {@code NOT_REQUIRED}
 * 且拒绝全部评审事件，而 REVIEW_REQUIRED 根本没有实现可登记，于是它的任何评审或发布都失败关闭为
 * {@code WIKI_REVIEW_ADAPTER_NOT_CONFIGURED}（503），系统绝不伪造一次批准，也绝不创造一个假审核员；其二，
 * <b>两个版本各自为政</b>——{@code page.revision} 只在指针真的移动时 +1，其余事件只校验调用方观察到的值，
 * {@code publicationVersion} 才是状态 CAS 版本，零行命中一律换算成 {@code GatewayAdminRevisionConflictException}
 * 而不是成功；其三，<b>一页之内要么全成要么全无</b>——旧化、状态迁移、指针切换与审计写进同一条调用序列并汇入
 * 调用方事务（绝不 {@code REQUIRES_NEW}），且旧发布一定先被置为 SUPERSEDED 再让新行进入 PUBLISHED，
 * 因为 {@code uq_wiki_revision_published} 部分唯一索引只允许一页存在一个已发布版本。
 * English summary: {@code WikiLifecycleServiceImpl} is the only implementation of the §7.3.7 Wiki state machine and of
 * INTERNAL-001, and the only place in the module that knows which migrations are legal. The table is an {@code EnumMap}
 * constant: {@code DIRECT_PUBLISH} walks DRAFT into PUBLISHING, {@code COMMIT_PUBLICATION} walks PUBLISHING into PUBLISHED
 * while superseding whatever the page published before, moving the page pointer and clearing the draft pointer left by that
 * same revision, {@code PUBLICATION_FAILURE} only pulls PUBLISHING back to DRAFT and records a safe error code,
 * {@code REPEAT_PUBLICATION} describes a request repeating an already effective publication, and {@code REPLACE_DRAFT} and
 * {@code UNPUBLISH} archive a draft and the current publication respectively. Events missing from the table
 * ({@code CREATE_REVISION}/{@code RESTORE_CONTENT}) are placement rather than migration: they write a body, and the
 * INTERNAL-001 command carrier holds no body at all, so entering through {@link #transition} yields
 * {@code WIKI_STATE_CONFLICT} while {@link #saveDraft} and {@link #publishGenerated} perform the placement. Three invariants
 * close here. First, the review dimension is not the machine's: the review column's value and its permission come from the
 * strategy the registry resolves for the revision's frozen policy snapshot, DIRECT always answering {@code NOT_REQUIRED} and
 * refusing every review event, while REVIEW_REQUIRED has no implementation to register at all, so any review or publication
 * of it fails closed as {@code WIKI_REVIEW_ADAPTER_NOT_CONFIGURED} (503) — the system never fabricates an approval and never
 * conjures a fake reviewer. Second, the two versions stay in their own lanes: {@code page.revision} advances only when a
 * pointer really moves, every other event merely verifies the value the caller observed, {@code publicationVersion} is the
 * status compare-and-set version, and a zero-row effect is always a {@code GatewayAdminRevisionConflictException} rather than
 * success. Third, one page either fully lands or not at all: superseding, migrating, moving the pointer and the audit row
 * form a single call sequence inside the caller's transaction (never {@code REQUIRES_NEW}), and the previous publication is
 * superseded <em>before</em> the new row enters PUBLISHED because the partial unique index
 * {@code uq_wiki_revision_published} tolerates exactly one published revision per page.
 *
 * 用法 / Usage: 以 bean 名 {@code wikiLifecycleServiceImpl} 注册，由 {@code WikiServiceImpl}（API-026/027/031）与
 * {@code WikiGenerationStrategy}（worker 侧批次）以 {@code @Qualifier} 注入；协作者是 {@code wikiRepository}、
 * {@code knowledgeRepository}（成员角色与来源现价复核）、{@code gatewayAuditLogRepository}、
 * {@code wikiPublicationPolicyStrategyRegistry} 与 {@code knowledgeClock}。判定顺序固定为「注解校验 → 读页面（404）→
 * 编辑者角色（403）→ 读修订并复核归属（404）→ 策略与事件放行（503）→ 双版本（409）→ 幂等返回 → 迁移表（409）→
 * 来源现价（409 {@code WIKI_SOURCE_STALE}）→ 写入」，权限先于状态，避免用 409/503 泄漏存在性；
 * 每一次真实改变都写一条 {@code WIKI_PAGE} 管理审计，幂等返回与失败都不写。本类不出网、不做模型调用。
 * / Registered as {@code wikiLifecycleServiceImpl} and injected by qualifier into the HTTP-facing service (API-026/027/031)
 * and into the generation strategy on the worker side; its collaborators are {@code wikiRepository},
 * {@code knowledgeRepository} (membership and source currency), {@code gatewayAuditLogRepository},
 * {@code wikiPublicationPolicyStrategyRegistry} and {@code knowledgeClock}. The decision order is fixed: annotation
 * validation, read the page (404), EDITOR membership (403), read the revision and re-check ownership (404), policy and event
 * admission (503), the two versions (409), the idempotent return, the migration table (409), source currency (409
 * {@code WIKI_SOURCE_STALE}), then the writes — permission before state so neither a 409 nor a 503 leaks existence. Every real
 * change writes one {@code WIKI_PAGE} management audit; an idempotent return and any failure write none. Nothing here leaves
 * the process.
 */
@Slf4j
@Validated
@Service("wikiLifecycleServiceImpl")
@RequiredArgsConstructor
public class WikiLifecycleServiceImpl implements WikiLifecycleService {

    /** 角色不足时的机器码，与知识面同一口径 / the machine code of an insufficient role, shared with the knowledge face. */
    private static final String FORBIDDEN = "KNOWLEDGE_FORBIDDEN";

    /** 行不可读时的机器码，跨租户与软删都归入它 / the machine code of an unreadable row, foreign tenant and soft delete included. */
    private static final String NOT_FOUND = "KNOWLEDGE_RESOURCE_NOT_FOUND";

    /** 状态或迁移不成立时的机器码（409）/ the machine code (409) of an impossible state or migration. */
    private static final String STATE_CONFLICT = "WIKI_STATE_CONFLICT";

    /** 来源不再现网可读时的机器码（409）/ the machine code (409) of a source that is no longer current. */
    private static final String SOURCE_STALE = "WIKI_SOURCE_STALE";

    /** 评审环节未接入时的机器码（503），失败关闭而不伪造批准 / the machine code (503) of the unwired review stage, failing closed instead of faking approval. */
    private static final String REVIEW_ADAPTER_UNAVAILABLE = "WIKI_REVIEW_ADAPTER_NOT_CONFIGURED";

    /** 审计资源类型 / the audited resource type. */
    private static final String AUDIT_RESOURCE_TYPE = "WIKI_PAGE";

    /** 管理面入口的审计来源 / the audit source of the management entry point. */
    private static final String MANAGEMENT_SOURCE = "MANAGEMENT_API";

    /** worker 批次入口的审计来源，区别于人工点击发布 / the audit source of the worker batch, distinct from a human publishing. */
    private static final String GENERATION_SOURCE = "KNOWLEDGE_JOB";

    /** DIRECT 路径写入审计的评审决定码，恒为无需评审 / the review decision code the DIRECT path audits, always "not required". */
    private static final String NOT_REQUIRED_DECISION = "NOT_REQUIRED";

    /** 新建页面的业务 revision 初值，与 DDL 的 {@code DEFAULT 1} 同值 / the business revision a new page row starts at, matching the DDL default of one. */
    private static final long FIRST_PAGE_REVISION = 1L;

    /** 生成批次落位与发布使用的稳定原因码 / the stable reason code a generation batch places and publishes under. */
    private static final String GENERATED_REASON = "WIKI_GENERATED_PUBLICATION";

    /** 人工保存草稿使用的稳定原因码 / the stable reason code a hand-written draft is placed under. */
    private static final String DRAFT_REASON = "WIKI_DRAFT_SAVED";

    /** HTTP 状态码：越权 / the HTTP status of insufficient permission. */
    private static final int FORBIDDEN_STATUS = 403;

    /** HTTP 状态码：状态冲突 / the HTTP status of a state conflict. */
    private static final int CONFLICT_STATUS = 409;

    /** HTTP 状态码：依赖未接入 / the HTTP status of an unwired dependency. */
    private static final int UNAVAILABLE_STATUS = 503;

    /** 允许推进 Wiki 草稿与发布的角色：EDITOR 及以上（API-026/027/031 的授权口径）/ the roles allowed to advance a draft or publication: EDITOR or above, the API-026/027/031 authorization. */
    private static final Set<KnowledgeMemberRoleEnum> EDITOR_OR_ABOVE = EnumSet.of(
            KnowledgeMemberRoleEnum.EDITOR,
            KnowledgeMemberRoleEnum.OWNER
    );

    /** 发布前必须复核来源现价的迁移：只有把内容推向公开的事件才需要 / the migrations that must re-check source currency, i.e. only those pushing content outward. */
    private static final Set<WikiTransitionEventEnum> SOURCE_CURRENCY_REQUIRED = EnumSet.of(
            WikiTransitionEventEnum.DIRECT_PUBLISH,
            WikiTransitionEventEnum.COMMIT_PUBLICATION
    );

    /**
     * 中文说明：§7.3.7 迁移表的不可变常量形态，键是事件、值是该事件允许的唯一起点与目标终点，
     * 外加指针效果与痕迹效果两个正交维度；新增一个事件必须在此登记，否则从 {@code transition} 进入即 409。
     * English summary: The immutable §7.3.7 table, keying an event onto its one legal from-state and target state plus the two
     * orthogonal effects (pointer and trace); an event absent from here is a 409 the moment it arrives.
     *
     * 用法 / Usage: 只由 {@link #migrate} 查表使用。/ Looked up by {@link #migrate} only.
     */
    private static final Map<WikiTransitionEventEnum, Migration> MIGRATIONS = migrations();

    @Qualifier("wikiRepository")
    private final WikiRepository wikiRepository;

    @Qualifier("knowledgeRepository")
    private final KnowledgeRepository knowledgeRepository;

    @Qualifier("gatewayAuditLogRepository")
    private final GatewayAuditLogRepository gatewayAuditLogRepository;

    @Qualifier("wikiPublicationPolicyStrategyRegistry")
    private final Map<WikiPublicationPolicyEnum, WikiPublicationPolicyStrategy> wikiPublicationPolicyStrategyRegistry;

    @Qualifier("knowledgeClock")
    private final Clock clock;

    /**
     * 中文说明：执行 transition 操作（INTERNAL-001 原签名）：按固定顺序校验、判权、读行、放行、比版本、查表、
     * 复核来源，然后一次性完成「旧化旧发布 + 迁移新行 + 移动指针 + 写审计」。零行 CAS 一律换算为
     * {@code GatewayAdminRevisionConflictException}，非法迁移为 409 {@code WIKI_STATE_CONFLICT}，
     * 未接入的评审事件为 503 {@code WIKI_REVIEW_ADAPTER_NOT_CONFIGURED}。
     * English summary: Executes the transition operation, the pinned INTERNAL-001 signature: validate, authorize, read, admit,
     * compare both versions, look the event up and re-check the sources in that fixed order, then perform
     * 「supersede, migrate, move the pointer, audit」 in one go. A zero-row compare-and-set always becomes
     * {@code GatewayAdminRevisionConflictException}, an illegal migration a 409 {@code WIKI_STATE_CONFLICT}, and an unwired
     * review event a 503 {@code WIKI_REVIEW_ADAPTER_NOT_CONFIGURED}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code wikiLifecycleService.transition(command, actor)}，命令按
     * {@code ExecuteGroup} 分组校验；本方法汇入调用方事务，绝不新开（{@code REQUIRES_NEW} 会把一页拆成两次提交）。
     * / Parameters validate under {@code ExecuteGroup}; the call joins the caller's transaction and never forks a new one,
     * which would split one page across two commits.
     * @param command 参数 迁移命令；parameter the migration command.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @return 返回 迁移后的事实；returns the facts after the migration.
     */
    @Override
    @Validated(ExecuteGroup.class)
    @Transactional(rollbackFor = Exception.class)
    public WikiTransitionResultVO transition(WikiTransitionCommandDTO command, AdminActor actor) {
        return migrate(command, actor.actorId(), actor.actorType().name(), MANAGEMENT_SOURCE);
    }

    /**
     * 中文说明：执行 transitionAll 操作，在同一事务内按顺序推进多条命令，任一失败即整批回滚；
     * 发布入口用它把 {@code [DIRECT_PUBLISH, COMMIT_PUBLICATION]} 成对推进，也让崩溃重试以
     * {@code [COMMIT_PUBLICATION]} 单独自愈（PUBLISHING 是 COMMIT 的合法起点）。
     * English summary: Executes the transitionAll operation, advancing several commands inside one transaction so any failure
     * rolls the whole batch back; the publication entry uses it to walk {@code [DIRECT_PUBLISH, COMMIT_PUBLICATION]} as a
     * pair, and a re-drive after a crash self-heals with {@code [COMMIT_PUBLICATION]} alone since PUBLISHING is a legal
     * starting state for it.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code wikiLifecycleService.transitionAll(commands, actor)}。
     * @param commands 参数 有序迁移命令；parameter the ordered migration commands.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @return 返回 每条命令迁移后的事实；returns the facts after each command.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<WikiTransitionResultVO> transitionAll(List<WikiTransitionCommandDTO> commands, AdminActor actor) {
        List<WikiTransitionResultVO> results = new ArrayList<>(commands.size());
        for (WikiTransitionCommandDTO command : commands) {
            results.add(transition(command, actor));
        }
        return List.copyOf(results);
    }

    /**
     * 中文说明：执行 saveDraft 操作，原子落位一个草稿：新页先建行（两个指针皆空），已有草稿先按
     * {@code REPLACE_DRAFT} 归档，再插入 DRAFT 修订并把草稿指针切过去；本方法从不发布，
     * 发布是一次显式的状态迁移，因此「保存草稿」不可能绕过发布路径把内容推到公开面。
     * English summary: Executes the saveDraft operation, placing one draft atomically: a new page row goes in with both
     * pointers empty, an existing draft is archived under {@code REPLACE_DRAFT} first, then the DRAFT revision is inserted
     * and the draft pointer moves onto it. Nothing is published here — publication is an explicit migration, so saving a
     * draft can never push content outward behind the publication path's back.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code wikiLifecycleService.saveDraft(page, draft, actor)}；
     * {@code draft} 必须是 {@code WikiRevisionBO.newDraft} 的产物（状态钉在 DRAFT、策略与摘要已冻结）。
     * / The draft must come from {@code WikiRevisionBO.newDraft}, its state pinned to DRAFT with policy and digest frozen.
     * @param page 参数 目标页面载体，新页主键为空；parameter the target page carrier, its key empty for a new page.
     * @param draft 参数 待落位草稿；parameter the draft to place.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @return 返回 草稿落位后的事实；returns the facts after the draft landed.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WikiTransitionResultVO saveDraft(WikiPageBO page, WikiRevisionBO draft, AdminActor actor) {
        return place(page, draft, actor.actorId(), actor.actorType().name(), MANAGEMENT_SOURCE, DRAFT_REASON, false);
    }

    /**
     * 中文说明：执行 publishGenerated 操作，生成 worker 唯一的写入入口：按索引配对落位每一页与每一条草稿，
     * 再按该草稿冻结的策略推进发布；整批在同一事务内完成，任一页零行命中即整批回滚，
     * 因此不存在「一半页面已公开」的中间态。授权按作业提交者的成员角色在执行时重新判定，
     * 提交后撤权的批次一律失败而不是继续发布。审计的来源记 {@code KNOWLEDGE_JOB}、actorType 记
     * {@code SERVICE}，如实表达「由 worker 代人执行」，不冒充某次人工点击。
     * English summary: Executes the publishGenerated operation, the generation worker's only write entry: each page pairs
     * with its draft by index, gets placed, and then advances under that draft's frozen policy. The whole batch runs in one
     * transaction and any zero row rolls all of it back, so a half-published set of pages cannot exist. Authorization is
     * re-decided at execution time from the submitting actor's membership, so a batch whose actor lost the role fails
     * instead of publishing. Its audits carry source {@code KNOWLEDGE_JOB} and actor type {@code SERVICE}, stating honestly
     * that the worker acted for a human rather than pretending a click happened.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code wikiLifecycleService.publishGenerated(pages, drafts, job.getActorId())}，
     * 且必须在模型调用之外、事务之内由 worker 调用一次。/ Call it once from the worker, outside the model call and inside
     * the transaction.
     * @param pages 参数 与草稿同序的页面载体列表；parameter the page carriers in draft order.
     * @param drafts 参数 待落位并发布的草稿载体列表；parameter the draft carriers to place and publish.
     * @param actorId 参数 作业提交者主体标识；parameter the submitting actor of the job.
     * @return 返回 每次发布迁移后的事实；returns the facts after each publication.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<WikiTransitionResultVO> publishGenerated(List<WikiPageBO> pages,
                                                         List<WikiRevisionBO> drafts,
                                                         String actorId) {
        if (pages.size() != drafts.size()) {
            throw new IllegalArgumentException("Wiki publication batches pair one page carrier with one draft carrier");
        }
        List<WikiTransitionResultVO> results = new ArrayList<>(drafts.size());
        for (int index = 0; index < drafts.size(); index++) {
            results.add(place(pages.get(index), drafts.get(index), actorId,
                    AdminActorTypeEnum.SERVICE.name(), GENERATION_SOURCE, GENERATED_REASON, true));
        }
        return List.copyOf(results);
    }

    /**
     * 中文说明：落位一条草稿并按需发布，是 {@link #saveDraft} 与 {@link #publishGenerated} 共用的内部通路：
     * 新页建行、旧草稿归档、插入修订、把草稿指针切到新行，然后（仅当需要发布时）按冻结策略查出的
     * {@code DIRECT_PUBLISH → COMMIT_PUBLICATION} 序列推进。每一步都只用一次指针 CAS 推进 {@code page.revision}，
     * 因此调用方观察到的版本与实际推进次数严格一致。
     * English summary: Places one draft and publishes it when asked, the shared internal route behind {@link #saveDraft} and
     * {@link #publishGenerated}: insert the page when new, archive the previous draft, insert the revision, move the draft
     * pointer onto it, then — only when publication is requested — walk the {@code DIRECT_PUBLISH → COMMIT_PUBLICATION}
     * sequence the frozen policy admits. Every step advances {@code page.revision} through at most one pointer
     * compare-and-set, so what the caller observed and how far the version actually advanced stay strictly consistent.
     * @param page 参数 目标页面载体，新页主键为空；parameter the target page carrier.
     * @param draft 参数 待落位草稿；parameter the draft to place.
     * @param actorId 参数 主体标识；parameter the acting actor.
     * @param actorType 参数 主体类型名，写入审计；parameter the actor type name, audited.
     * @param auditSource 参数 审计来源；parameter the audit source.
     * @param reasonCode 参数 稳定原因码；parameter the stable reason code.
     * @param publish 参数 落位后是否按冻结策略推进发布；parameter whether the frozen policy then advances publication.
     * @return 返回 最后一次改变后的事实；returns the facts after the last change.
     */
    private WikiTransitionResultVO place(WikiPageBO page,
                                         WikiRevisionBO draft,
                                         String actorId,
                                         String actorType,
                                         String auditSource,
                                         String reasonCode,
                                         boolean publish) {
        WikiPageBO placed = StringUtils.isBlank(page.getId())
                ? wikiRepository.insertPage(newPage(page, draft))
                : requirePlacedPage(page);
        requireEditor(placed.getKbId(), actorId);
        if (StringUtils.isNotBlank(placed.getDraftRevisionId())) {
            migrate(WikiTransitionCommandDTO.builder()
                    .pageId(placed.getId())
                    .revisionId(placed.getDraftRevisionId())
                    .event(WikiTransitionEventEnum.REPLACE_DRAFT)
                    .expectedPageRevision(placed.getRevision())
                    .expectedPublicationVersion(requireRevision(placed, placed.getDraftRevisionId()).getPublicationVersion())
                    .reasonCode(reasonCode)
                    .build(), actorId, actorType, auditSource);
            placed = requirePlacedPage(placed);
        }
        WikiRevisionBO inserted = wikiRepository.insertRevision(draft.setPageId(placed.getId()));
        WikiTransitionResultVO placement = placeDraftPointer(placed, inserted, actorId, actorType, auditSource);
        if (!publish) {
            return placement;
        }
        WikiTransitionResultVO publishing = migrate(command(WikiTransitionEventEnum.DIRECT_PUBLISH,
                inserted, placement), actorId, actorType, auditSource);
        return migrate(command(WikiTransitionEventEnum.COMMIT_PUBLICATION,
                inserted, publishing), actorId, actorType, auditSource);
    }

    /**
     * 中文说明：把草稿指针切到刚落位的修订，是 {@code CREATE_REVISION} 这一放置事件的落地形态：
     * 只移动草稿指针、页面 revision +1、写一条放置审计，绝不改动任何状态列（新行本来就在 DRAFT）。
     * English summary: Moves the draft pointer onto the freshly placed revision, the landing form of the CREATE_REVISION
     * placement event: the draft pointer moves, the page revision advances by one and one placement audit is written, while
     * no status column is touched at all (the new row already sits in DRAFT).
     * @param page 参数 已落位页面载体；parameter the placed page carrier.
     * @param revision 参数 已插入的修订载体；parameter the inserted revision carrier.
     * @param actorId 参数 主体标识；parameter the acting actor.
     * @param actorType 参数 主体类型名；parameter the actor type name.
     * @param auditSource 参数 审计来源；parameter the audit source.
     * @return 返回 指针切换后的事实；returns the facts after the pointer moved.
     */
    private WikiTransitionResultVO placeDraftPointer(WikiPageBO page,
                                                     WikiRevisionBO revision,
                                                     String actorId,
                                                     String actorType,
                                                     String auditSource) {
        WikiPageBO target = PointerMove.DRAFT_CURRENT_REVISION.apply(page, revision.getId());
        if (!wikiRepository.movePagePointers(target, page.getRevision(),
                page.getPublishedRevisionId(), page.getDraftRevisionId())) {
            throw new GatewayAdminRevisionConflictException(page.getRevision());
        }
        audit(actorId, actorType, auditSource, WikiTransitionEventEnum.CREATE_REVISION, page, revision, target);
        return WikiTransitionResultVO.builder()
                .pageId(page.getId())
                .revisionId(revision.getId())
                .publicationStatus(revision.getPublicationStatus())
                .reviewStatus(revision.getReviewStatus())
                .publicationVersion(revision.getPublicationVersion())
                .pageRevision(target.getRevision())
                .changed(Boolean.TRUE)
                .build();
    }

    /**
     * 中文说明：执行一次状态迁移，是本类唯一读写状态的地方；顺序即不变量，任何一步不成立都在写入之前退出。
     * English summary: Performs one migration, the only place in this class that reads or writes state; the order is the
     * invariant and every step that fails does so before anything is written.
     * @param command 参数 迁移命令；parameter the migration command.
     * @param actorId 参数 主体标识；parameter the acting actor.
     * @param actorType 参数 主体类型名，写入审计；parameter the actor type name, audited.
     * @param auditSource 参数 审计来源；parameter the audit source.
     * @return 返回 迁移后的事实；returns the facts after the migration.
     */
    private WikiTransitionResultVO migrate(WikiTransitionCommandDTO command,
                                           String actorId,
                                           String actorType,
                                           String auditSource) {
        WikiPageBO page = requirePage(command.getPageId());
        requireEditor(page.getKbId(), actorId);
        WikiRevisionBO revision = requireRevision(page, command.getRevisionId());
        WikiPublicationPolicyStrategy strategy = requireStrategy(revision);
        if (!strategy.executable(command.getEvent())) {
            log.warn("wiki event {} refused for revision {} of page {}: its frozen policy {} is not executable, "
                            + "no audit row was written",
                    command.getEvent().wireValue(), revision.getId(), page.getId(),
                    revision.getPublicationPolicySnapshot() == null ? "absent"
                            : revision.getPublicationPolicySnapshot().wireValue());
            throw new CommonException(UNAVAILABLE_STATUS, REVIEW_ADAPTER_UNAVAILABLE,
                    "the wiki review stage this policy depends on is not configured");
        }
        Migration migration = MIGRATIONS.get(command.getEvent());
        if (migration == null) {
            throw new CommonException(CONFLICT_STATUS, STATE_CONFLICT,
                    "the wiki event " + command.getEvent().wireValue() + " places a revision and is not a state migration");
        }
        if (revision.getPublicationVersion() != command.getExpectedPublicationVersion()) {
            throw new GatewayAdminRevisionConflictException(revision.getPublicationVersion());
        }
        if (page.getRevision() != command.getExpectedPageRevision()) {
            throw new GatewayAdminRevisionConflictException(page.getRevision());
        }
        if (migration.alreadyEffective(page, revision)) {
            log.info("wiki event {} on revision {} of page {} repeats an effective state, nothing advanced",
                    command.getEvent().wireValue(), revision.getId(), page.getId());
            return WikiTransitionResultVO.builder()
                    .pageId(page.getId())
                    .revisionId(revision.getId())
                    .publicationStatus(revision.getPublicationStatus())
                    .reviewStatus(revision.getReviewStatus())
                    .publicationVersion(revision.getPublicationVersion())
                    .pageRevision(page.getRevision())
                    .changed(Boolean.FALSE)
                    .build();
        }
        if (!migration.fromPublicationStatus().equals(revision.getPublicationStatus())) {
            throw new CommonException(CONFLICT_STATUS, STATE_CONFLICT,
                    "the wiki revision sits in " + revision.getPublicationStatus().wireValue()
                            + ", which " + command.getEvent().wireValue() + " cannot migrate");
        }
        if (SOURCE_CURRENCY_REQUIRED.contains(command.getEvent())) {
            requireSourcesCurrent(page, revision);
        }

        Instant now = clock.instant();
        WikiPublicationStatusEnum fromPublicationStatus = revision.getPublicationStatus();
        WikiReviewStatusEnum fromReviewStatus = revision.getReviewStatus();
        long expectedPublicationVersion = revision.getPublicationVersion();
        if (WikiPublicationStatusEnum.PUBLISHED.equals(migration.toPublicationStatus())) {
            supersedeCurrentPublication(page, revision);
        }
        revision.setPublicationStatus(migration.toPublicationStatus())
                .setReviewStatus(strategy.reviewStatusAfter(command.getEvent(), fromReviewStatus))
                .setPublicationVersion(expectedPublicationVersion + 1);
        migration.trace().apply(revision, actorId, now, command.getReasonCode());
        if (!wikiRepository.transitionRevision(revision, expectedPublicationVersion,
                fromPublicationStatus, fromReviewStatus)) {
            throw new GatewayAdminRevisionConflictException(expectedPublicationVersion);
        }
        WikiPageBO target = page;
        if (migration.pointerMove() != PointerMove.NONE) {
            target = migration.pointerMove().apply(page, revision.getId());
            if (!wikiRepository.movePagePointers(target, page.getRevision(),
                    page.getPublishedRevisionId(), page.getDraftRevisionId())) {
                throw new GatewayAdminRevisionConflictException(page.getRevision());
            }
        }
        audit(actorId, actorType, auditSource, command.getEvent(), page, revision, target);
        log.info("wiki page {} revision {} migrated from {} to {} at publication version {}",
                page.getId(), revision.getId(), fromPublicationStatus.wireValue(),
                revision.getPublicationStatus().wireValue(), revision.getPublicationVersion());
        return WikiTransitionResultVO.builder()
                .pageId(page.getId())
                .revisionId(revision.getId())
                .publicationStatus(revision.getPublicationStatus())
                .reviewStatus(revision.getReviewStatus())
                .publicationVersion(revision.getPublicationVersion())
                .pageRevision(target.getRevision())
                .changed(Boolean.TRUE)
                .build();
    }

    /**
     * 中文说明：构造一条内部迁移命令，供放置与批次复用，避免各处手搓 builder。两个期望版本一律取上一步
     * 刚刚报告的事实，而不是手里那份可能已经过期的修订载体：{@code publicationVersion} 只在状态迁移时 +1，
     * 载体在插入后不会被回填，若拿载体的值做第二次 CAS 就会自己制造一个假冲突。
     * English summary: Builds an internal migration command for placement and batches so no caller hand-rolls the builder.
     * Both expected versions come from what the previous step just reported rather than from a carrier that may already be
     * stale: {@code publicationVersion} advances only on a status transition and the carrier is never back-filled after an
     * insert, so CAS-ing a second transition against the carrier would manufacture a conflict against oneself.
     * @param event 参数 迁移事件；parameter the migration event.
     * @param revision 参数 刚被推进的修订载体，只提供主键定位；parameter the revision carrier, supplying identity only.
     * @param previous 参数 上一步报告的事实；parameter the facts reported by the previous step.
     * @return 返回 迁移命令；returns the migration command.
     */
    private static WikiTransitionCommandDTO command(WikiTransitionEventEnum event,
                                                    WikiRevisionBO revision,
                                                    WikiTransitionResultVO previous) {
        return WikiTransitionCommandDTO.builder()
                .pageId(revision.getPageId())
                .revisionId(revision.getId())
                .event(event)
                .expectedPageRevision(previous.getPageRevision())
                .expectedPublicationVersion(previous.getPublicationVersion())
                .reasonCode(GENERATED_REASON)
                .build();
    }

    /**
     * 中文说明：按迁移表要求的指针效果构造待插入的新页面载体：只带知识库与 slug，两个指针留空、
     * 业务 revision 取 DDL 初值，租户与审计列由受守卫的持久边界补齐。
     * English summary: Builds the new page carrier under the pointer shape the table requires: knowledge base and slug only,
     * both pointers left empty, the business revision at its DDL default, and tenant plus audit columns filled by the guarded
     * persistence boundary.
     * @param page 参数 调用方给出的页面意图；parameter the caller's page intent.
     * @param draft 参数 携带所属知识库的草稿；parameter the draft carrying the owning base.
     * @return 返回 待插入页面载体；returns the page carrier to insert.
     */
    private static WikiPageBO newPage(WikiPageBO page, WikiRevisionBO draft) {
        return WikiPageBO.builder()
                .kbId(StringUtils.defaultIfBlank(page.getKbId(), draft.getKbId()))
                .slug(page.getSlug())
                .revision(FIRST_PAGE_REVISION)
                .build();
    }

    /**
     * 中文说明：读取并复核一个已经落库的页面载体（批次会把上一轮的权威值传回来），读不到即 404。
     * English summary: Reads and re-checks a page carrier that is already stored (a batch hands back the authoritative value
     * from the previous step), an unreadable one being a 404.
     * @param page 参数 带主键的页面载体；parameter the page carrier holding a key.
     * @return 返回 权威页面载体；returns the authoritative page carrier.
     */
    private WikiPageBO requirePlacedPage(WikiPageBO page) {
        return requirePage(page.getId());
    }

    /**
     * 中文说明：读取页面，读不到（含跨租户与软删）按 404 收束。
     * English summary: Reads the page, an unreadable one (foreign tenant or soft-deleted included) being a 404.
     * @param pageId 参数 页面十进制字符串 id；parameter decimal-string page id.
     * @return 返回 页面载体；returns the page carrier.
     */
    private WikiPageBO requirePage(String pageId) {
        return wikiRepository.findPage(pageId)
                .orElseThrow(() -> new GatewayAdminNotFoundException(NOT_FOUND + " wiki page was not found"));
    }

    /**
     * 中文说明：读取修订并复核它确实属于给定页面，跨页或跨库一律按 404 而不是 403，
     * 免得用「存在但无权限」泄漏别人的页面结构。
     * English summary: Reads the revision and re-checks it really belongs to the given page, a cross-page or cross-base read
     * being a 404 rather than a 403 so nobody learns another base's page structure from a permission answer.
     * @param page 参数 所属页面载体；parameter the owning page carrier.
     * @param revisionId 参数 修订十进制字符串 id；parameter decimal-string revision id.
     * @return 返回 修订载体；returns the revision carrier.
     */
    private WikiRevisionBO requireRevision(WikiPageBO page, String revisionId) {
        WikiRevisionBO revision = wikiRepository.findRevision(page.getKbId(), revisionId)
                .orElseThrow(() -> new GatewayAdminNotFoundException(NOT_FOUND + " wiki revision was not found"));
        if (!StringUtils.equals(revision.getPageId(), page.getId())) {
            throw new GatewayAdminNotFoundException(NOT_FOUND + " wiki revision was not found");
        }
        return revision;
    }

    /**
     * 中文说明：按 revision 冻结的策略快照查策略；查不到即返回不了一次正确的发布，
     * 因为本期 {@code REVIEW_REQUIRED} 无实现可登记，缺失必须失败关闭而不是当作 DIRECT 放行。
     * English summary: Resolves the strategy from the revision's frozen policy snapshot; a miss cannot yield a correct
     * publication, because {@code REVIEW_REQUIRED} has no implementation to register this release and its absence has to fail
     * closed rather than be waved through as DIRECT.
     * @param revision 参数 待发布或迁移的修订；parameter the revision being migrated.
     * @return 返回 策略实现；returns the strategy.
     */
    private WikiPublicationPolicyStrategy requireStrategy(WikiRevisionBO revision) {
        WikiPublicationPolicyStrategy strategy = revision.getPublicationPolicySnapshot() == null
                ? null
                : wikiPublicationPolicyStrategyRegistry.get(revision.getPublicationPolicySnapshot());
        if (strategy == null) {
            throw new CommonException(UNAVAILABLE_STATUS, REVIEW_ADAPTER_UNAVAILABLE,
                    "the wiki publication policy this revision froze is not configured");
        }
        return strategy;
    }

    /**
     * 中文说明：判定编辑者权限：知识库行不可见按 404，可见但角色不足按 403，顺序固定以免 403 泄漏存在性；
     * 角色口径与 {@code KnowledgeServiceImpl}、{@code KnowledgeJobServiceImpl} 完全一致（owner 优先，其次成员最高角色）。
     * English summary: Decides EDITOR permission: an invisible base is a 404, a visible one with an insufficient role a 403,
     * always in that order so a 403 leaks no existence; the role rule matches {@code KnowledgeServiceImpl} and
     * {@code KnowledgeJobServiceImpl} exactly (owner first, otherwise the highest membership role).
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param actorId 参数 主体稳定标识；parameter the stable actor identifier.
     */
    private void requireEditor(String kbId, String actorId) {
        KnowledgeBaseBO base = knowledgeRepository.findBase(kbId)
                .orElseThrow(() -> new GatewayAdminNotFoundException(NOT_FOUND + " knowledge base was not found"));
        KnowledgeMemberRoleEnum role = deriveRole(base, actorId);
        if (role == null || !EDITOR_OR_ABOVE.contains(role)) {
            throw new CommonException(FORBIDDEN_STATUS, FORBIDDEN,
                    "the presenting identity holds no sufficient role on this knowledge base");
        }
    }

    /**
     * 中文说明：派生当前主体的角色，与知识面同一实现口径（本模块第四处，登记为已知重复，
     * 收敛需要改动知识面的公开行为，故留给独立的重构步骤）。
     * English summary: Derives the presenting identity's role under the same rule the knowledge face applies (the fourth copy
     * in this module, registered as known duplication because collapsing it would change published knowledge behaviour and
     * belongs in its own refactor).
     * @param base 参数 知识库载体；parameter the knowledge base carrier.
     * @param actorId 参数 主体稳定标识；parameter the stable actor identifier.
     * @return 返回 角色或 {@code null}；returns the role or {@code null}.
     */
    private static KnowledgeMemberRoleEnum deriveRole(KnowledgeBaseBO base, String actorId) {
        if (StringUtils.equals(base.getOwnerActorId(), actorId)) {
            return KnowledgeMemberRoleEnum.OWNER;
        }
        List<KnowledgeMemberDTO> members = base.getMembers() == null ? List.of() : base.getMembers();
        KnowledgeMemberRoleEnum derived = null;
        for (KnowledgeMemberDTO member : members) {
            if (member == null || !StringUtils.equals(member.getActorId(), actorId) || member.getRole() == null) {
                continue;
            }
            if (derived == null || member.getRole().getCode() > derived.getCode()) {
                derived = member.getRole();
            }
        }
        return derived;
    }

    /**
     * 中文说明：复核修订引用的每条来源在发布这一刻仍然有效：资料 revision 仍活跃、状态 READY、摘要一致，
     * 且它仍是所属文档的当前激活版本；任一条不成立都按 409 {@code WIKI_SOURCE_STALE} 收束，
     * 因为把已经改过的资料当作现网事实发布出去，比拒绝发布危险得多。空来源同样拒绝（DDL 要求至少一条）。
     * English summary: Re-checks every source the revision cites at the moment of publication: the material revision is still
     * active, READY and digest-equal, and it is still the document's activated revision; any failure answers 409
     * {@code WIKI_SOURCE_STALE}, because shipping changed material as current fact is far more dangerous than refusing. An
     * empty source list is refused the same way (the DDL requires at least one).
     * @param page 参数 所属页面载体；parameter the owning page carrier.
     * @param revision 参数 待发布的修订；parameter the revision being published.
     */
    private void requireSourcesCurrent(WikiPageBO page, WikiRevisionBO revision) {
        List<WikiSourceDTO> sources = revision.getSources();
        if (sources.isEmpty()) {
            throw new CommonException(CONFLICT_STATUS, SOURCE_STALE,
                    "the wiki revision cites no source and cannot be published");
        }
        for (WikiSourceDTO source : sources) {
            KnowledgeDocumentRevisionBO row = knowledgeRepository
                    .findRevision(page.getKbId(), source.getDocumentRevisionId())
                    .orElseThrow(() -> new CommonException(CONFLICT_STATUS, SOURCE_STALE,
                            "a cited wiki source is no longer readable"));
            if (!KnowledgeRevisionStatusEnum.READY.equals(row.getStatus())
                    || !StringUtils.equals(row.getContentHash(), source.getSourceHash())) {
                throw new CommonException(CONFLICT_STATUS, SOURCE_STALE,
                        "a cited wiki source is no longer ready or no longer matches its digest");
            }
            KnowledgeDocumentBO document = knowledgeRepository
                    .findDocument(page.getKbId(), row.getDocumentId())
                    .orElseThrow(() -> new CommonException(CONFLICT_STATUS, SOURCE_STALE,
                            "the document behind a cited wiki source is no longer readable"));
            if (!StringUtils.equals(document.getActiveRevisionId(), source.getDocumentRevisionId())) {
                throw new CommonException(CONFLICT_STATUS, SOURCE_STALE,
                        "a cited wiki source is no longer the document's active revision");
            }
        }
    }

    /**
     * 中文说明：把页面当前已发布的修订旧化。顺序要紧：部分唯一索引 {@code uq_wiki_revision_published}
     * 只允许一页存在一个 PUBLISHED 行，所以旧行必须先变成 SUPERSEDED，新行才能进入 PUBLISHED。
     * 指针指向的行已读不到时按状态冲突收束（宁可拒绝也不留下两个已发布版本），已不处于 PUBLISHED 时如实跳过。
     * 旧行的发布与评审痕迹一律保留，历史从不被改写。
     * English summary: Supersedes the page's current publication, and the order matters: the partial unique index
     * {@code uq_wiki_revision_published} allows one PUBLISHED row per page, so the old row has to become SUPERSEDED before the
     * new one may enter PUBLISHED. A pointer whose row can no longer be read is refused as a state conflict — better a
     * rejection than two publications — and a row no longer PUBLISHED is skipped truthfully. The old row keeps its
     * publication and review trace; history is never rewritten.
     * @param page 参数 页面载体；parameter the page carrier.
     * @param revision 参数 即将成为新版本发布的修订；parameter the revision about to be published.
     */
    private void supersedeCurrentPublication(WikiPageBO page, WikiRevisionBO revision) {
        String currentPublishedId = page.getPublishedRevisionId();
        if (StringUtils.isBlank(currentPublishedId) || currentPublishedId.equals(revision.getId())) {
            return;
        }
        WikiRevisionBO previous = wikiRepository.findRevision(page.getKbId(), currentPublishedId)
                .orElseThrow(() -> new CommonException(CONFLICT_STATUS, STATE_CONFLICT,
                        "the wiki page points at a published revision that can no longer be read"));
        if (!previous.isPublished()) {
            log.warn("wiki page {} points at revision {} which already left PUBLISHED, nothing to supersede",
                    page.getId(), previous.getId());
            return;
        }
        if (!wikiRepository.supersedePrevious(page.getKbId(), previous.getId(),
                revision.getId(), previous.getPublicationVersion())) {
            throw new GatewayAdminRevisionConflictException(previous.getPublicationVersion());
        }
    }

    /**
     * 中文说明：在调用方事务内写一条 {@code WIKI_PAGE} 管理审计；摘要只含稳定标识与状态值，
     * 正文、markdown 与来源内容一律不落审计列。DIRECT 路径的 {@code reviewDecisionCode} 恒为
     * {@code NOT_REQUIRED}，因此审计里也不会出现假批准。
     * English summary: Writes one {@code WIKI_PAGE} management audit inside the caller's transaction; the summaries carry stable
     * identifiers and status values only, never the body, the markdown or the source content. On the DIRECT path
     * {@code reviewDecisionCode} is always {@code NOT_REQUIRED}, so no fake approval reaches the audit either.
     * @param actorId 参数 主体标识；parameter the acting actor.
     * @param actorType 参数 主体类型名；parameter the actor type name.
     * @param auditSource 参数 审计来源；parameter the audit source.
     * @param event 参数 已执行的事件；parameter the event applied.
     * @param page 参数 迁移前的页面载体；parameter the page carrier before the migration.
     * @param revision 参数 迁移后的修订载体；parameter the revision carrier after the migration.
     * @param target 参数 指针切换后的页面载体（未切换时即迁移前的页面）；parameter the page carrier after the pointer moved, the same page when nothing moved.
     */
    private void audit(String actorId,
                       String actorType,
                       String auditSource,
                       WikiTransitionEventEnum event,
                       WikiPageBO page,
                       WikiRevisionBO revision,
                       WikiPageBO target) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("pageRevision", page.getRevision());
        before.put("draftRevisionId", StringUtils.defaultString(page.getDraftRevisionId()));
        before.put("publishedRevisionId", StringUtils.defaultString(page.getPublishedRevisionId()));
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("pageRevision", target.getRevision());
        after.put("draftRevisionId", StringUtils.defaultString(target.getDraftRevisionId()));
        after.put("publishedRevisionId", StringUtils.defaultString(target.getPublishedRevisionId()));
        after.put("toPublicationStatus", revision.getPublicationStatus().wireValue());
        after.put("reviewStatus", revision.getReviewStatus().wireValue());
        after.put("reviewDecisionCode", NOT_REQUIRED_DECISION);
        after.put("publicationVersion", revision.getPublicationVersion());
        after.put("publicationErrorCode", StringUtils.defaultString(revision.getPublicationErrorCode()));
        gatewayAuditLogRepository.save(new GatewayAuditLogBO(
                SnowflakeIdGenerator.nextId(),
                actorId,
                actorType,
                auditSource,
                null,
                null,
                AUDIT_RESOURCE_TYPE,
                page.getId(),
                event.wireValue(),
                GatewayAuditLogBO.sanitized(before),
                GatewayAuditLogBO.sanitized(after),
                null,
                null,
                true,
                null,
                clock.instant()
        ));
    }

    /**
     * 中文说明：构造一个页面副本，供指针切换与 CAS 使用；受守卫的行模型永不越过持久边界，
     * 而副本保证零行命中时不会把脏指针留在调用方读到的载体上。
     * English summary: Builds a page copy for a pointer swap and its compare-and-set; no guarded row model ever crosses the
     * persistence boundary, and the copy keeps a dirty pointer off the carrier the caller read when the hit comes back empty.
     * @param page 参数 源页面载体；parameter the source page carrier.
     * @return 返回 等值副本；returns the equal copy.
     */
    private static WikiPageBO copyOf(WikiPageBO page) {
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
     * 中文说明：构造 §7.3.7 迁移表的常量形态。
     * English summary: Builds the constant form of the §7.3.7 migration table.
     *
     * 用法 / Usage: 静态初始化唯一入口，返回只读 {@code EnumMap}。/ The single static initialiser, returning a read-only
     * {@code EnumMap}.
     * @return 返回 事件到迁移规则的只读表；returns the read-only event-to-migration table.
     */
    private static Map<WikiTransitionEventEnum, Migration> migrations() {
        Map<WikiTransitionEventEnum, Migration> table = new EnumMap<>(WikiTransitionEventEnum.class);
        table.put(WikiTransitionEventEnum.DIRECT_PUBLISH, new Migration(
                WikiPublicationStatusEnum.DRAFT, WikiPublicationStatusEnum.PUBLISHING,
                PointerMove.NONE, Trace.NONE));
        table.put(WikiTransitionEventEnum.PUBLICATION_FAILURE, new Migration(
                WikiPublicationStatusEnum.PUBLISHING, WikiPublicationStatusEnum.DRAFT,
                PointerMove.NONE, Trace.FAILURE));
        table.put(WikiTransitionEventEnum.COMMIT_PUBLICATION, new Migration(
                WikiPublicationStatusEnum.PUBLISHING, WikiPublicationStatusEnum.PUBLISHED,
                PointerMove.PUBLISH_CURRENT_REVISION, Trace.PUBLICATION));
        table.put(WikiTransitionEventEnum.REPEAT_PUBLICATION, new Migration(
                WikiPublicationStatusEnum.PUBLISHED, WikiPublicationStatusEnum.PUBLISHED,
                PointerMove.PUBLISH_CURRENT_REVISION, Trace.NONE));
        table.put(WikiTransitionEventEnum.REPLACE_DRAFT, new Migration(
                WikiPublicationStatusEnum.DRAFT, WikiPublicationStatusEnum.ARCHIVED,
                PointerMove.NONE, Trace.ARCHIVAL));
        table.put(WikiTransitionEventEnum.UNPUBLISH, new Migration(
                WikiPublicationStatusEnum.PUBLISHED, WikiPublicationStatusEnum.ARCHIVED,
                PointerMove.CLEAR_PUBLISHED, Trace.ARCHIVAL));
        return Collections.unmodifiableMap(table);
    }

    /**
     * 中文说明：{@code Migration} 是迁移表的一行：一个事件唯一的起点、终点，加上指针与痕迹两个正交效果。
     * 它是只读值对象而非业务载体，因此按 Rule 3 用不可变 record 表达。
     * English summary: {@code Migration} is one row of the table: a migration event's single from-state and to-state plus the
     * pointer and trace effects. It is a read-only value rather than a business carrier, which is why Rule 3's record exemption fits.
     */
    private record Migration(WikiPublicationStatusEnum fromPublicationStatus,
                             WikiPublicationStatusEnum toPublicationStatus,
                             PointerMove pointerMove,
                             Trace trace) {

        /**
         * 中文说明：判断本次请求是否只是重复了一个已经生效的状态：目标状态已到位且指针也已在目标位置，
         * 此时返回 {@code changed=false} 而不推进任何版本、不写任何审计，让发布入口可安全重放。
         * English summary: Decides whether the request merely repeats an effective state: the target status is in place and the
         * pointer already sits where the migration would put it, which answers {@code changed=false} without advancing any
         * version or writing any audit, so a publication entry point stays safely replayable.
         * @param page 参数 页面载体；parameter the page carrier.
         * @param revision 参数 修订载体；parameter the revision carrier.
         * @return 返回 是否已经生效；returns whether the state is already effective.
         */
        private boolean alreadyEffective(WikiPageBO page, WikiRevisionBO revision) {
            return toPublicationStatus.equals(revision.getPublicationStatus())
                    && pointerMove.alreadyAtTarget(page, revision.getId());
        }
    }

    /**
     * 中文说明：{@code PointerMove} 用一个事件对页面两个指针的影响表达全部页面级改变，
     * 同时是「什么算已经生效」的判据；每个枚举常量自己实现两个方法，因此新增效果不改动调用点（Rule 9）。
     * English summary: {@code PointerMove} states every page-level effect of an event on the two pointers and is at the same
     * time the test for "already effective". Each constant implements both methods itself, so a new effect needs no new call
     * site (Rule 9).
     */
    private enum PointerMove {

        /** 不移动任何指针：DIRECT_PUBLISH、PUBLICATION_FAILURE、REPLACE_DRAFT / no pointer at all: DIRECT_PUBLISH, PUBLICATION_FAILURE, REPLACE_DRAFT. */
        NONE {
            @Override
            boolean alreadyAtTarget(WikiPageBO page, String revisionId) {
                return true;
            }

            @Override
            WikiPageBO apply(WikiPageBO page, String revisionId) {
                return copyOf(page);
            }
        },

        /** 把已发布指针切到本修订并清掉同源的草稿指针：COMMIT_PUBLICATION、REPEAT_PUBLICATION / the published pointer takes this revision and its own draft pointer clears: COMMIT_PUBLICATION, REPEAT_PUBLICATION. */
        PUBLISH_CURRENT_REVISION {
            @Override
            boolean alreadyAtTarget(WikiPageBO page, String revisionId) {
                return revisionId.equals(page.getPublishedRevisionId())
                        && !revisionId.equals(page.getDraftRevisionId());
            }

            @Override
            WikiPageBO apply(WikiPageBO page, String revisionId) {
                WikiPageBO target = copyOf(page);
                target.setPublishedRevisionId(revisionId);
                if (revisionId.equals(page.getDraftRevisionId())) {
                    target.setDraftRevisionId(null);
                }
                target.setRevision(page.getRevision() + 1);
                return target;
            }
        },

        /** 清空已发布指针而保留草稿指针：UNPUBLISH / the published pointer clears while the draft stays: UNPUBLISH. */
        CLEAR_PUBLISHED {
            @Override
            boolean alreadyAtTarget(WikiPageBO page, String revisionId) {
                return page.getPublishedRevisionId() == null;
            }

            @Override
            WikiPageBO apply(WikiPageBO page, String revisionId) {
                WikiPageBO target = copyOf(page);
                target.setPublishedRevisionId(null);
                target.setRevision(page.getRevision() + 1);
                return target;
            }
        },

        /** 把草稿指针切到刚插入的修订：CREATE_REVISION 的落地形态 / the draft pointer takes the freshly inserted revision: the landing form of CREATE_REVISION. */
        DRAFT_CURRENT_REVISION {
            @Override
            boolean alreadyAtTarget(WikiPageBO page, String revisionId) {
                return revisionId.equals(page.getDraftRevisionId());
            }

            @Override
            WikiPageBO apply(WikiPageBO page, String revisionId) {
                WikiPageBO target = copyOf(page);
                target.setDraftRevisionId(revisionId);
                target.setRevision(page.getRevision() + 1);
                return target;
            }
        };

        /**
         * 中文说明：判断指针是否已经在该效果的目标位置。
         * English summary: Decides whether the pointers already sit where this effect would put them.
         * @param page 参数 页面载体；parameter the page carrier.
         * @param revisionId 参数 本事件涉及的修订；parameter the revision the event concerns.
         * @return 返回 是否已到位；returns whether the pointers are in place.
         */
        abstract boolean alreadyAtTarget(WikiPageBO page, String revisionId);

        /**
         * 中文说明：产出应用该效果后的页面副本（revision 只在指针真的移动时 +1）。
         * English summary: Produces the page copy after this effect, its revision advancing only when a pointer really moves.
         * @param page 参数 页面载体；parameter the page carrier.
         * @param revisionId 参数 本事件涉及的修订；parameter the revision the event concerns.
         * @return 返回 目标页面副本；returns the target page copy.
         */
        abstract WikiPageBO apply(WikiPageBO page, String revisionId);
    }

    /**
     * 中文说明：{@code Trace} 表达迁移对时间戳与操作者痕迹列的影响，是状态机的第三个正交维度：
     * 发布留发布人与发布时刻并把 {@code everPublished} 钉成永久为真，归档留归档人与归档时刻，
     * 失败只留一个安全错误码。痕迹从不互相覆盖（下线不会抹掉历史发布时刻）。
     * English summary: {@code Trace} is the state machine's third orthogonal dimension, the one governing the timestamp and
     * operator columns: publication stamps publisher and instant and pins {@code everPublished} permanently true, archival
     * stamps archivist and instant, and a failure leaves nothing but a safe error code. One trace never overwrites another
     * (unpublishing does not erase when something was once published).
     */
    private enum Trace {

        /** 不留任何痕迹：DIRECT_PUBLISH / no trace at all: DIRECT_PUBLISH. */
        NONE {
            @Override
            void apply(WikiRevisionBO revision, String actorId, Instant now, String reasonCode) {
                revision.setPublicationErrorCode(null);
            }
        },

        /** 留下发布痕迹：COMMIT_PUBLICATION / the publication trace: COMMIT_PUBLICATION. */
        PUBLICATION {
            @Override
            void apply(WikiRevisionBO revision, String actorId, Instant now, String reasonCode) {
                revision.setPublishedAt(now)
                        .setPublishedByActorId(actorId)
                        .setEverPublished(Boolean.TRUE)
                        .setPublicationErrorCode(null);
            }
        },

        /** 留下归档痕迹：UNPUBLISH、REPLACE_DRAFT / the archival trace: UNPUBLISH, REPLACE_DRAFT. */
        ARCHIVAL {
            @Override
            void apply(WikiRevisionBO revision, String actorId, Instant now, String reasonCode) {
                revision.setArchivedAt(now).setArchivedByActorId(actorId);
            }
        },

        /** 留下失败痕迹，只写安全码：PUBLICATION_FAILURE / the failure trace, a safe code only: PUBLICATION_FAILURE. */
        FAILURE {
            @Override
            void apply(WikiRevisionBO revision, String actorId, Instant now, String reasonCode) {
                revision.setPublicationErrorCode(reasonCode);
            }
        };

        /**
         * 中文说明：把本痕迹落到即将提交 CAS 的修订载体上。
         * English summary: Applies this trace to the revision carrier about to be compare-and-set.
         * @param revision 参数 修订载体；parameter the revision carrier.
         * @param actorId 参数 操作主体；parameter the acting actor.
         * @param now 参数 统一时钟下的时刻；parameter the instant from the shared clock.
         * @param reasonCode 参数 命令携带的安全原因码；parameter the command's safe reason code.
         */
        abstract void apply(WikiRevisionBO revision, String actorId, Instant now, String reasonCode);
    }
}
