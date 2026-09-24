package top.egon.cola.component.yuheng.admin.wiki.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeJobVO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgePageVO;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;
import top.egon.cola.component.yuheng.admin.wiki.domain.dto.WikiDraftCommandDTO;
import top.egon.cola.component.yuheng.admin.wiki.domain.dto.WikiGenerationCommandDTO;
import top.egon.cola.component.yuheng.admin.wiki.domain.dto.WikiPageQueryDTO;
import top.egon.cola.component.yuheng.admin.wiki.domain.dto.WikiPublicationCommandDTO;
import top.egon.cola.component.yuheng.admin.wiki.domain.vo.WikiGraphVO;
import top.egon.cola.component.yuheng.admin.wiki.domain.vo.WikiPageVO;
import top.egon.cola.component.yuheng.admin.wiki.service.WikiService;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;

/**
 * 中文说明：{@code WikiController} 是 Spec §9.2.23–§9.2.28 与 §9.2.31（API-023 至 API-028、API-031）的接口控制器，
 * 只做「原路径 + 原 operationId + 原状态码 + 裸 JSON shape」到 {@link WikiService} 的交付：目录返回
 * {@code items/page/size/total}，单页与草稿与发布返回完整 {@code WikiPageVO}，图返回 {@code nodes/edges/truncated}，
 * 生成作业返回 {@code 202} 并指向 API-019 的轮询地址，下线返回 {@code 204} 且无 body。
 * 身份由已认证主体解析为 {@link AdminActor} 作为业务合同的第一个入参，租户永远不是参数；READER/EDITOR 判定、
 * 幂等意图复用、双版本 CAS 与来源现价复核全部在业务合同层完成，本类不含权限分支、仓储访问或错误包装，
 * 也绝不接受调用方自报的 {@code publicationStatus}、{@code reviewStatus}、{@code reviewerActorId} 或任何时间戳。
 * English summary: {@code WikiController} is the interface controller of Spec §9.2.23–§9.2.28 and §9.2.31
 * (API-023 through API-028 and API-031) and only delivers the original paths, operationIds, status codes and bare JSON shapes
 * through {@link WikiService}: the catalog answers {@code items/page/size/total}, the page, draft and publication operations
 * answer with the full {@code WikiPageVO}, the graph answers {@code nodes/edges/truncated}, the generation job answers
 * {@code 202} pointing at API-019's polling address, and unpublishing answers {@code 204} with no body. The identity is
 * resolved from the authenticated principal into an {@link AdminActor} passed as the first business argument and tenancy never
 * becomes a parameter; the READER/EDITOR decision, idempotency reuse, the dual-version compare-and-sets and source currency all
 * live in the business contract, so this class holds no permission branch, no repository access and no error wrapper, and it
 * never accepts a caller-declared {@code publicationStatus}, {@code reviewStatus}, {@code reviewerActorId} or timestamp.
 *
 * 用法 / Usage: 读需 {@code yuheng:knowledge:read}，命令需 {@code yuheng:knowledge:write}；分页与图上限在本类补齐默认值
 * （载体字段是原始 {@code int}，缺省会变成 0 并被 422 误拒），其余约束由被调用的业务合同统一复核，本类不重复声明
 * 分组之外的规则、也不下沉任何业务判断。
 * Reads require {@code yuheng:knowledge:read} and commands {@code yuheng:knowledge:write}; paging and the graph ceiling are
 * defaulted here because the carrier fields are primitive {@code int}s where an absent value would reach the contract as zero,
 * while every other constraint is checked once by the business contract instead of being duplicated here.
 */
@Slf4j
@Validated
@RestController("wikiController")
@RequestMapping("/api/v1/yuheng/admin")
@PreAuthorize("hasAnyAuthority('CAP_yuheng:knowledge:read','CAP_*')")
@Tag(name = "yuheng-admin")
@EgonApiCatalog(
        businessDomainCode = "xingyuan",
        businessDomainName = "平台治理域",
        entityDomainCode = "yuheng-admin",
        entityDomainName = "Gateway Admin 管理实体域",
        interfaceGroupCode = "yuheng-admin")
@RequiredArgsConstructor
public class WikiController {

    /** 作业轮询绝对路径前缀，仅用于 202 的 {@code Location}，与 API-019 的映射保持一致 / the job polling path prefix used only for the 202 {@code Location}, aligned with the API-019 mapping. */
    private static final String KNOWLEDGE_JOB_PATH = "/api/v1/yuheng/admin/knowledge-jobs/";

    /** 原合同要求的 202 轮询间隔秒数 / the polling interval the contract attaches to a 202 answer. */
    private static final String RETRY_AFTER_SECONDS = "2";

    /** 目录页大小的合同默认值 / the contractual page-size default of the catalog. */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** 图节点上限的合同默认值 / the contractual default of the graph node ceiling. */
    private static final int DEFAULT_GRAPH_LIMIT = 100;

    /**
     * 中文说明：保存 Wiki 业务合同 对应的依赖值；字段类型为 {@code WikiService}，由
     * {@code WikiController} 在其生命周期内读取或更新。
     * English summary: Holds the dependency represented by the Wiki business contract; its type is
     * {@code WikiService}, and {@code WikiController} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code WikiController} 的公开入口使用；/ Access it through the public entry
     * points of {@code WikiController}.
     */
    @Qualifier("wikiServiceImpl")
    private final WikiService wikiService;

    /**
     * 中文说明：执行 listWikiPages 操作（API-023）；READER 可读，{@code includeDraft=true} 必须 EDITOR，否则按 403
     * 拒绝而不是悄悄降级为已发布视图；page 从 1 开始、size 有效范围 1–20（本接口因目录携带完整正文而收紧到 20），
     * 次序固定 {@code createdAt DESC, id DESC}，空页返回 {@code []} 且 {@code total=0}。
     * English summary: Executes the listWikiPages operation (API-023), READER, where {@code includeDraft = true} demands EDITOR
     * and is refused as 403 rather than quietly downgraded to the published view; page starts at one, size stays within 1–20
     * (this endpoint is tightened to 20 because a row carries its full body) and the order is fixed to
     * {@code createdAt DESC, id DESC}, an empty page answering {@code []} with {@code total = 0}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code wikiController.listWikiPages(actor, kbId, page, size, search, tag, includeDraft)}。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param page 参数 页码，默认 1；parameter one-based page number, defaulting to one.
     * @param size 参数 页大小，默认 20；parameter page size, defaulting to twenty.
     * @param search 参数 标题或 slug 的字面子串，可空；parameter an optional literal substring of title or slug.
     * @param tag 参数 精确标签，可空；parameter an optional exact tag.
     * @param includeDraft 参数 是否纳入草稿，默认 false；parameter whether drafts are included, defaulting to false.
     * @return 返回 页面分页投影；returns the paged page projection.
     */
    @Operation(operationId = "listWikiPages")
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/knowledge-bases/{kbId}/wiki/pages")
    public KnowledgePageVO<WikiPageVO> listWikiPages(
            AdminActor actor,
            @PathVariable
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "false") boolean includeDraft) {
        return wikiService.listPages(
                actor,
                kbId,
                pageQuery(page, size, search, tag, includeDraft)
        );
    }

    /**
     * 中文说明：执行 createWikiGenerationJob 操作（API-024）；EDITOR 且持有知识写能力，202 只表示作业已持久排队，
     * 不表示已经生成或发布；{@code Location} 指向该作业的 API-019 轮询地址并带 {@code Retry-After: 2}。
     * 请求体只有 {@code sourceRevisionIds/pageId/expectedRevision} 三项，来源必须同库且为当前激活版本，
     * 模型调用与批次发布都在 worker 侧发生，本请求线程内不做任何生成。
     * English summary: Executes the createWikiGenerationJob operation (API-024), EDITOR holding the knowledge write capability,
     * where a 202 only means the job is durably queued and neither generated nor published; the {@code Location} points at that
     * job's API-019 polling address with {@code Retry-After: 2}. The body carries only
     * {@code sourceRevisionIds/pageId/expectedRevision}, the sources must be this base's current active revisions, and both
     * the model call and the batch publication happen on the worker side rather than on this request thread.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code wikiController.createWikiGenerationJob(actor, kbId, command, idempotencyKey)}。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param command 参数 生成命令；parameter the generation command.
     * @param idempotencyKey 参数 {@code Idempotency-Key} 请求头；parameter the {@code Idempotency-Key} header.
     * @return 返回 带 202 受理与轮询地址的作业投影；returns the job projection in a 202 acceptance with its polling address.
     */
    @Operation(operationId = "createWikiGenerationJob")
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PreAuthorize("hasAnyAuthority('CAP_yuheng:knowledge:write','CAP_*')")
    @PostMapping("/knowledge-bases/{kbId}/wiki/generation-jobs")
    public ResponseEntity<KnowledgeJobVO> createWikiGenerationJob(
            AdminActor actor,
            @PathVariable
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @RequestBody @Valid WikiGenerationCommandDTO command,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        KnowledgeJobVO job = wikiService.createGenerationJob(actor, kbId, command, idempotencyKey);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .location(jobLocation(job.getId()))
                .header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
                .body(job);
    }

    /**
     * 中文说明：执行 getWikiPage 操作（API-025）；READER 可读当前发布版或曾发布过的历史版，草稿只对 EDITOR 开放；
     * {@code revisionId} 给出时必须属于该页面，跨库或跨页一律按 404 处理而不泄漏存在性。
     * English summary: Executes the getWikiPage operation (API-025): a READER may read the current publication and any
     * historically published version, a draft being EDITOR-only; a supplied {@code revisionId} must belong to that page and a
     * cross-base or cross-page read is a 404 so existence never leaks.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code wikiController.getWikiPage(actor, kbId, pageId, revisionId)}。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param pageId 参数 页面十进制字符串 id；parameter decimal-string page id.
     * @param revisionId 参数 可选的修订十进制字符串 id；parameter optional decimal-string revision id.
     * @return 返回 页面投影；returns the page projection.
     */
    @Operation(operationId = "getWikiPage")
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/knowledge-bases/{kbId}/wiki/pages/{pageId}")
    public WikiPageVO getWikiPage(
            AdminActor actor,
            @PathVariable
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @PathVariable
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String pageId,
            @RequestParam(required = false)
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String revisionId) {
        return wikiService.getPage(actor, kbId, pageId, revisionId);
    }

    /**
     * 中文说明：执行 replaceWikiDraft 操作（API-026）；EDITOR 方可为，完整替换体落成一个新草稿并把被替换的旧草稿
     * 归档，绝不直接改写已发布正文；{@code expectedRevision} 必须是调用方读到的页面 revision，不符即 409
     * {@code WIKI_STATE_CONFLICT} 并回报现值。slug 在此永不变更，避免内链漂移。
     * English summary: Executes the replaceWikiDraft operation (API-026), EDITOR: the full replacement body lands as a new draft
     * and the draft it replaces is archived, never rewriting a published body; {@code expectedRevision} must be the page
     * revision the caller read, a mismatch being 409 {@code WIKI_STATE_CONFLICT} reporting the current value. The slug never
     * changes here, so internal links cannot drift.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code wikiController.replaceWikiDraft(actor, kbId, pageId, command)}。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param pageId 参数 页面十进制字符串 id；parameter decimal-string page id.
     * @param command 参数 草稿命令；parameter the draft command.
     * @return 返回 保存后的页面投影；returns the page projection after the save.
     */
    @Operation(operationId = "replaceWikiDraft")
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PreAuthorize("hasAnyAuthority('CAP_yuheng:knowledge:write','CAP_*')")
    @PutMapping("/knowledge-bases/{kbId}/wiki/pages/{pageId}/draft")
    public WikiPageVO replaceWikiDraft(
            AdminActor actor,
            @PathVariable
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @PathVariable
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String pageId,
            @RequestBody @Valid WikiDraftCommandDTO command) {
        return wikiService.replaceDraft(actor, kbId, pageId, command);
    }

    /**
     * 中文说明：执行 publishWikiRevision 操作（API-027）；EDITOR 方可为，DIRECT 策略下由状态机一次事务内走完
     * DRAFT→PUBLISHING→PUBLISHED 并把旧发布版置 SUPERSEDED；来源失鲜按 409 {@code WIKI_SOURCE_STALE} 拒绝，
     * 同一草稿重复发布幂等地回报当前发布结果。响应里的 {@code reviewStatus} 恒为 {@code NOT_REQUIRED}——
     * 发布成功从不冒充评审通过。
     * English summary: Executes the publishWikiRevision operation (API-027), EDITOR: under the DIRECT policy the state machine
     * walks DRAFT→PUBLISHING→PUBLISHED in one transaction and marks the previous publication SUPERSEDED; a stale source is
     * refused as 409 {@code WIKI_SOURCE_STALE} and republishing the same draft idempotently reports the current publication.
     * The {@code reviewStatus} in the answer is always {@code NOT_REQUIRED} — a successful publication never poses as an
     * approval.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code wikiController.publishWikiRevision(actor, kbId, pageId, command)}。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param pageId 参数 页面十进制字符串 id；parameter decimal-string page id.
     * @param command 参数 发布命令；parameter the publication command.
     * @return 返回 发布后的页面投影；returns the page projection after publication.
     */
    @Operation(operationId = "publishWikiRevision")
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PreAuthorize("hasAnyAuthority('CAP_yuheng:knowledge:write','CAP_*')")
    @PostMapping("/knowledge-bases/{kbId}/wiki/pages/{pageId}/publications")
    public WikiPageVO publishWikiRevision(
            AdminActor actor,
            @PathVariable
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @PathVariable
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String pageId,
            @RequestBody @Valid WikiPublicationCommandDTO command) {
        return wikiService.publishRevision(actor, kbId, pageId, command);
    }

    /**
     * 中文说明：执行 getWikiGraph 操作（API-028）；READER 可见的节点只由当前已发布修订闭合，一跳之内至多
     * 100 节点 / 200 边，被裁掉时 {@code truncated=true}；未发布或越权的页面连标题都不出现。
     * English summary: Executes the getWikiGraph operation (API-028): a READER's nodes close over currently published revisions
     * only, at most 100 nodes and 200 edges within one hop, {@code truncated = true} when trimmed, and an unpublished or
     * unauthorized page contributing not even its title.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code wikiController.getWikiGraph(actor, kbId, pageId, limit)}。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param pageId 参数 可选的中心页面 id，缺省为整库图；parameter the optional centre page id, the whole base by default.
     * @param limit 参数 节点上限，默认 100，范围 1–100；parameter the node ceiling, defaulting to 100 within 1–100.
     * @return 返回 图投影；returns the graph projection.
     */
    @Operation(operationId = "getWikiGraph")
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/knowledge-bases/{kbId}/wiki/graph")
    public WikiGraphVO getWikiGraph(
            AdminActor actor,
            @PathVariable
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @RequestParam(required = false)
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String pageId,
            @RequestParam(defaultValue = "100")
            @Min(1) int limit) {
        return wikiService.graph(actor, kbId, pageId, limit);
    }

    /**
     * 中文说明：执行 unpublishWikiPage 操作（API-031）；EDITOR 且持有知识写能力，页面指针与修订状态在同一事务内
     * 原子切换为「无发布版 + ARCHIVED」，保留 publishedAt 与 everPublished 的历史痕迹，绝不把历史改回 PUBLISHED；
     * 成功后返回 204 且无 body，已无发布版时幂等地同样返回 204。
     * English summary: Executes the unpublishWikiPage operation (API-031), EDITOR holding the knowledge write capability: the
     * page pointer and the revision status switch atomically in one transaction to 「no publication plus ARCHIVED」, keeping the
     * publishedAt and everPublished trace and never rewriting history back to PUBLISHED; success answers 204 with no body and
     * a page with nothing published left is an idempotent 204 as well.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code wikiController.unpublishWikiPage(actor, kbId, pageId, expectedRevision)}。
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param pageId 参数 页面十进制字符串 id；parameter decimal-string page id.
     * @param expectedRevision 参数 调用方观察到的页面 revision；parameter the caller-observed page revision.
     * @return 返回 无 body 的 204 受理；returns the bodiless 204 acceptance.
     */
    @Operation(operationId = "unpublishWikiPage")
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @PreAuthorize("hasAnyAuthority('CAP_yuheng:knowledge:write','CAP_*')")
    @DeleteMapping("/knowledge-bases/{kbId}/wiki/pages/{pageId}/publications/current")
    public ResponseEntity<Void> unpublishWikiPage(
            AdminActor actor,
            @PathVariable
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @PathVariable
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String pageId,
            @RequestParam
            @Min(1) long expectedRevision) {
        wikiService.unpublish(actor, kbId, pageId, expectedRevision);
        return ResponseEntity.noContent().build();
    }

    /**
     * 中文说明：把补齐了默认值的分页参数装配成查询载体；页大小的上界与检索词的空白折叠都由业务合同复核，
     * 本方法只负责「缺省不等于零」这一件事。
     * English summary: Assembles the defaulted paging parameters into the query carrier; the page-size ceiling and the
     * blank-folding of the search term are both the business contract's, this method only ensuring 「absent does not mean
     * zero」.
     * @param page 参数 页码；parameter the page number.
     * @param size 参数 页大小；parameter the page size.
     * @param search 参数 检索词；parameter the search term.
     * @param tag 参数 标签；parameter the tag.
     * @param includeDraft 参数 是否纳入草稿；parameter whether drafts are included.
     * @return 返回 查询载体；returns the query carrier.
     */
    private static WikiPageQueryDTO pageQuery(
            int page,
            int size,
            String search,
            String tag,
            boolean includeDraft) {
        return WikiPageQueryDTO.builder()
                .page(page)
                .size(size == DEFAULT_PAGE_SIZE || size > 0 ? size : DEFAULT_PAGE_SIZE)
                .search(search)
                .tag(tag)
                .includeDraft(includeDraft)
                .build();
    }

    /**
     * 中文说明：为 202 受理拼装作业轮询的绝对 {@code Location}，只取当前部署的 contextPath 与合同路径，
     * 不猜测网关前缀；作业 id 已由业务合同按十进制正整数投影，因此无需再转义。
     * English summary: Builds the absolute polling {@code Location} of a 202 acceptance from the current deployment's
     * contextPath and the contract path without guessing a gateway prefix; the job id is already projected as a decimal
     * positive integer by the business contract, so nothing needs escaping.
     * @param jobId 参数 作业十进制字符串 id；parameter decimal-string job id.
     * @return 返回 轮询地址；returns the polling address.
     */
    private static URI jobLocation(String jobId) {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(KNOWLEDGE_JOB_PATH).path(jobId).build().toUri();
    }
}
