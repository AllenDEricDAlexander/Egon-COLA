package top.egon.cola.component.yuheng.admin.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
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
import top.egon.cola.component.yuheng.admin.knowledge.service.KnowledgeModelClientService;
import top.egon.cola.component.yuheng.admin.observability.domain.bo.GatewayAuditLogBO;
import top.egon.cola.component.yuheng.admin.observability.repository.GatewayAuditLogRepository;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;
import top.egon.cola.component.yuheng.admin.shared.domain.enums.AdminActorTypeEnum;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminNotFoundException;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminRevisionConflictException;
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
import top.egon.cola.component.yuheng.admin.wiki.service.DirectWikiPublicationPolicyStrategy;
import top.egon.cola.component.yuheng.admin.wiki.service.WikiGenerationStrategy;
import top.egon.cola.component.yuheng.admin.wiki.service.WikiLifecycleService;
import top.egon.cola.component.yuheng.admin.wiki.service.WikiPublicationPolicyStrategy;
import top.egon.cola.component.yuheng.admin.wiki.service.impl.WikiLifecycleServiceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 中文说明：{@code WikiLifecycleServiceTest} 固定 Step 14 的可观察结果——DIRECT 的完整状态迁移与批次发布原子性，
 * 以及「审核流程预留但未接入时不得假批准」。断言全部来自业务 Spec §7.3.7 的迁移表、INTERNAL-001 的双 version CAS
 * 与 §9.2.32 的错误语义，而不是实现之后的镜像断言：DIRECT 路径永远不写 APPROVED、审核人/审核实例/审核时刻必须为
 * NULL、审计的 {@code reviewDecisionCode} 固定 {@code NOT_REQUIRED}；COMMIT_PUBLICATION 必须在同一调用序列里
 * 把旧 PUBLISHED 置 SUPERSEDED、切换 page 指针并写审计；任一 CAS 零行都必须以 409 收束且不留下审计与半成品；
 * SUBMIT_REVIEW/APPROVE 等未接入入口必须失败关闭为 {@code WIKI_REVIEW_ADAPTER_NOT_CONFIGURED}。
 * English summary: This test pins Step 14's observable outcome — the complete DIRECT transition set plus batch
 * publication atomicity, and the rule that a reserved-but-unwired review flow may never fake an approval. Every
 * assertion comes from the transition table of Spec §7.3.7, the dual-version compare-and-set of INTERNAL-001 and the
 * error semantics of §9.2.32 rather than from a mirror of the implementation: DIRECT never writes APPROVED and keeps
 * reviewer, review instance and reviewed-at null while the audit carries {@code NOT_REQUIRED}; COMMIT_PUBLICATION must
 * supersede the previous published revision, move the page pointer and write the audit in one call sequence; any
 * zero-row compare-and-set must raise 409 without leaving an audit row or a half-finished publication; and
 * SUBMIT_REVIEW/APPROVE fail closed as {@code WIKI_REVIEW_ADAPTER_NOT_CONFIGURED}.
 *
 * 用法 / Usage: 只用脚本化 fake 与 {@link Proxy} 的「未触碰即失败」端口，不启动 Spring 容器、不连数据库；
 * 时刻由固定 {@link Clock} 提供，租户取可信上下文。SQL 侧的双 version 谓词、jsonb 来源、指针部分唯一索引与
 * 「整批同 commit」的回滚只能在授权隔离库上证明（回滚由 {@code @Transactional} 边界承担，本类只证到失败即中止），
 * 故这几项如实标注为 Runtime unverified。
 */
@DisplayName("Wiki 状态机与 DIRECT 自动发布")
class WikiLifecycleServiceTest {

    private static final String TENANT_KB = "9001";
    private static final String PAGE_ID = "70001";
    private static final String DRAFT_REVISION_ID = "77001";
    private static final String PUBLISHED_REVISION_ID = "77000";
    private static final String DOCUMENT_ID = "60001";
    private static final String SOURCE_REVISION_ID = "61001";
    private static final String SOURCE_HASH = "a".repeat(64);
    private static final String ACTOR_ID = "u-1";
    private static final Instant NOW = Instant.parse("2026-09-22T08:00:00Z");

    /** 中文说明：无容器测试按本模块惯例自行绑定雪花发生器。/ a container-free test binds the snowflake engine itself. */
    @BeforeAll
    static void bindIdGenerator() {
        SnowflakeIdGenerator.initialize(0L, Duration.ofMillis(5));
    }

    @Test
    @DisplayName("DIRECT 发布完整迁移但从不写成评审通过")
    void directPublicationNeverMarksTheRevisionApproved() {
        Fixture fixture = new Fixture();
        fixture.pages.put(PAGE_ID, page(DRAFT_REVISION_ID, PUBLISHED_REVISION_ID, 7L));
        fixture.revisions.put(DRAFT_REVISION_ID, revision(DRAFT_REVISION_ID,
                WikiPublicationStatusEnum.DRAFT, WikiReviewStatusEnum.NOT_REQUIRED, 1L,
                WikiPublicationPolicyEnum.DIRECT));
        fixture.revisions.put(PUBLISHED_REVISION_ID, revision(PUBLISHED_REVISION_ID,
                WikiPublicationStatusEnum.PUBLISHED, WikiReviewStatusEnum.NOT_REQUIRED, 4L,
                WikiPublicationPolicyEnum.DIRECT));

        fixture.lifecycle.transition(command(WikiTransitionEventEnum.DIRECT_PUBLISH,
                DRAFT_REVISION_ID, 7L, 1L), actor());
        WikiTransitionResultVO committed = fixture.lifecycle.transition(command(WikiTransitionEventEnum.COMMIT_PUBLICATION,
                DRAFT_REVISION_ID, 7L, 2L), actor());

        assertEquals(WikiPublicationStatusEnum.PUBLISHED, committed.getPublicationStatus());
        assertEquals(WikiReviewStatusEnum.NOT_REQUIRED, committed.getReviewStatus());
        assertEquals(3L, committed.getPublicationVersion());
        assertEquals(8L, committed.getPageRevision());
        WikiRevisionBO published = fixture.revisions.get(DRAFT_REVISION_ID);
        assertEquals(WikiPublicationStatusEnum.PUBLISHED, published.getPublicationStatus());
        assertEquals(WikiReviewStatusEnum.NOT_REQUIRED, published.getReviewStatus());
        assertEquals(NOW, published.getPublishedAt());
        assertEquals(ACTOR_ID, published.getPublishedByActorId());
        assertEquals(Boolean.TRUE, published.getEverPublished());
        assertNull(published.getReviewerActorId());
        assertNull(published.getReviewInstanceId());
        assertNull(published.getReviewedAt());
        assertNull(published.getArchivedAt());
        assertNull(published.getArchivedByActorId());
        assertEquals(WikiPublicationStatusEnum.SUPERSEDED,
                fixture.revisions.get(PUBLISHED_REVISION_ID).getPublicationStatus());
        assertEquals(PUBLISHED_REVISION_ID, fixture.superseded.get(DRAFT_REVISION_ID));
        WikiPageBO page = fixture.pages.get(PAGE_ID);
        assertEquals(DRAFT_REVISION_ID, page.getPublishedRevisionId());
        assertNull(page.getDraftRevisionId());
        assertEquals(8L, page.getRevision());
        Map<String, Object> audit = fixture.audits.get(fixture.audits.size() - 1).getAfterSummary();
        assertEquals("NOT_REQUIRED", audit.get("reviewDecisionCode"));
        assertFalse(fixture.audits.stream().anyMatch(row -> "APPROVED".equals(
                row.getAfterSummary().get("toPublicationStatus"))));
    }

    @Test
    @DisplayName("未接入审核的策略事件一律失败关闭而不是假批准")
    void unwiredReviewEventsFailClosedInsteadOfFakingApproval() {
        Fixture fixture = new Fixture();
        fixture.pages.put(PAGE_ID, page(DRAFT_REVISION_ID, null, 7L));
        fixture.revisions.put(DRAFT_REVISION_ID, revision(DRAFT_REVISION_ID,
                WikiPublicationStatusEnum.DRAFT, WikiReviewStatusEnum.NOT_SUBMITTED, 1L,
                WikiPublicationPolicyEnum.DIRECT));

        for (WikiTransitionEventEnum event : List.of(WikiTransitionEventEnum.SUBMIT_REVIEW,
                WikiTransitionEventEnum.APPROVE, WikiTransitionEventEnum.REJECT,
                WikiTransitionEventEnum.CANCEL_REVIEW, WikiTransitionEventEnum.REVIEWED_PUBLISH)) {
            CommonException failure = assertThrows(CommonException.class,
                    () -> fixture.lifecycle.transition(command(event, DRAFT_REVISION_ID, 7L, 1L), actor()),
                    event + " must not be executable without a real review adapter");
            assertEquals("WIKI_REVIEW_ADAPTER_NOT_CONFIGURED", failure.getStatus());
            assertEquals(503, failure.getCode());
        }
        assertEquals(WikiReviewStatusEnum.NOT_SUBMITTED,
                fixture.revisions.get(DRAFT_REVISION_ID).getReviewStatus());
        assertTrue(fixture.revisionWrites.isEmpty());
        assertTrue(fixture.audits.isEmpty());
    }

    @Test
    @DisplayName("非法迁移与零行 CAS 都以 409 收束且不留审计")
    void illegalTransitionAndLostCompareAndSetAnswerConflictWithoutAudit() {
        Fixture fixture = new Fixture();
        fixture.pages.put(PAGE_ID, page(DRAFT_REVISION_ID, PUBLISHED_REVISION_ID, 7L));
        fixture.revisions.put(DRAFT_REVISION_ID, revision(DRAFT_REVISION_ID,
                WikiPublicationStatusEnum.PUBLISHED, WikiReviewStatusEnum.NOT_REQUIRED, 1L,
                WikiPublicationPolicyEnum.DIRECT));
        fixture.revisions.put(PUBLISHED_REVISION_ID, revision(PUBLISHED_REVISION_ID,
                WikiPublicationStatusEnum.PUBLISHED, WikiReviewStatusEnum.NOT_REQUIRED, 4L,
                WikiPublicationPolicyEnum.DIRECT));

        CommonException illegal = assertThrows(CommonException.class,
                () -> fixture.lifecycle.transition(command(WikiTransitionEventEnum.DIRECT_PUBLISH,
                        DRAFT_REVISION_ID, 7L, 1L), actor()));
        assertEquals("WIKI_STATE_CONFLICT", illegal.getStatus());
        assertEquals(409, illegal.getCode());
        assertTrue(fixture.audits.isEmpty());
        assertTrue(fixture.revisionWrites.isEmpty());

        Fixture lost = new Fixture();
        lost.pages.put(PAGE_ID, page(DRAFT_REVISION_ID, null, 7L));
        lost.revisions.put(DRAFT_REVISION_ID, revision(DRAFT_REVISION_ID,
                WikiPublicationStatusEnum.DRAFT, WikiReviewStatusEnum.NOT_REQUIRED, 1L,
                WikiPublicationPolicyEnum.DIRECT));
        lost.failRevisionCas = true;
        assertThrows(GatewayAdminRevisionConflictException.class,
                () -> lost.lifecycle.transition(command(WikiTransitionEventEnum.DIRECT_PUBLISH,
                        DRAFT_REVISION_ID, 7L, 1L), actor()));
        assertTrue(lost.audits.isEmpty());
        assertTrue(lost.pageWrites.isEmpty());

        Fixture stalePage = new Fixture();
        stalePage.pages.put(PAGE_ID, page(DRAFT_REVISION_ID, null, 9L));
        stalePage.revisions.put(DRAFT_REVISION_ID, revision(DRAFT_REVISION_ID,
                WikiPublicationStatusEnum.DRAFT, WikiReviewStatusEnum.NOT_REQUIRED, 1L,
                WikiPublicationPolicyEnum.DIRECT));
        assertThrows(GatewayAdminRevisionConflictException.class,
                () -> stalePage.lifecycle.transition(command(WikiTransitionEventEnum.DIRECT_PUBLISH,
                        DRAFT_REVISION_ID, 7L, 1L), actor()));
        assertTrue(stalePage.audits.isEmpty());
    }

    @Test
    @DisplayName("重复发布幂等返回现值且不推进两个版本")
    void repeatedPublicationReturnsTheCurrentFactsWithoutBumpingVersions() {
        Fixture fixture = new Fixture();
        fixture.pages.put(PAGE_ID, page(null, DRAFT_REVISION_ID, 9L));
        fixture.revisions.put(DRAFT_REVISION_ID, revision(DRAFT_REVISION_ID,
                WikiPublicationStatusEnum.PUBLISHED, WikiReviewStatusEnum.NOT_REQUIRED, 3L,
                WikiPublicationPolicyEnum.DIRECT));

        WikiTransitionResultVO result = fixture.lifecycle.transition(command(WikiTransitionEventEnum.REPEAT_PUBLICATION,
                DRAFT_REVISION_ID, 9L, 3L), actor());

        assertEquals(Boolean.FALSE, result.getChanged());
        assertEquals(WikiPublicationStatusEnum.PUBLISHED, result.getPublicationStatus());
        assertEquals(3L, result.getPublicationVersion());
        assertEquals(9L, result.getPageRevision());
        assertTrue(fixture.revisionWrites.isEmpty());
        assertTrue(fixture.pageWrites.isEmpty());
        assertTrue(fixture.audits.isEmpty());
    }

    @Test
    @DisplayName("下线把当前发布归档并清空指针，无当前发布时幂等成功")
    void unpublishArchivesTheCurrentPublicationAndIsIdempotentAfterwards() {
        Fixture fixture = new Fixture();
        fixture.pages.put(PAGE_ID, page(DRAFT_REVISION_ID, PUBLISHED_REVISION_ID, 12L));
        fixture.revisions.put(PUBLISHED_REVISION_ID, revision(PUBLISHED_REVISION_ID,
                WikiPublicationStatusEnum.PUBLISHED, WikiReviewStatusEnum.NOT_REQUIRED, 4L,
                WikiPublicationPolicyEnum.DIRECT));
        fixture.revisions.put(DRAFT_REVISION_ID, revision(DRAFT_REVISION_ID,
                WikiPublicationStatusEnum.DRAFT, WikiReviewStatusEnum.NOT_REQUIRED, 1L,
                WikiPublicationPolicyEnum.DIRECT));

        WikiTransitionResultVO result = fixture.lifecycle.transition(command(WikiTransitionEventEnum.UNPUBLISH,
                PUBLISHED_REVISION_ID, 12L, 4L), actor());

        assertEquals(WikiPublicationStatusEnum.ARCHIVED, result.getPublicationStatus());
        assertEquals(NOW, fixture.revisions.get(PUBLISHED_REVISION_ID).getArchivedAt());
        assertEquals(ACTOR_ID, fixture.revisions.get(PUBLISHED_REVISION_ID).getArchivedByActorId());
        assertEquals(NOW, fixture.revisions.get(PUBLISHED_REVISION_ID).getPublishedAt());
        WikiPageBO page = fixture.pages.get(PAGE_ID);
        assertNull(page.getPublishedRevisionId());
        assertEquals(DRAFT_REVISION_ID, page.getDraftRevisionId());
        assertEquals(13L, page.getRevision());
        assertEquals(WikiPublicationStatusEnum.DRAFT,
                fixture.revisions.get(DRAFT_REVISION_ID).getPublicationStatus());

        Fixture already = new Fixture();
        already.pages.put(PAGE_ID, page(null, null, 13L));
        already.revisions.put(PUBLISHED_REVISION_ID, revision(PUBLISHED_REVISION_ID,
                WikiPublicationStatusEnum.ARCHIVED, WikiReviewStatusEnum.NOT_REQUIRED, 5L,
                WikiPublicationPolicyEnum.DIRECT));
        WikiTransitionResultVO idempotent = already.lifecycle.transition(command(WikiTransitionEventEnum.UNPUBLISH,
                PUBLISHED_REVISION_ID, 13L, 5L), actor());
        assertEquals(Boolean.FALSE, idempotent.getChanged());
        assertEquals(WikiPublicationStatusEnum.ARCHIVED, idempotent.getPublicationStatus());
        assertTrue(already.audits.isEmpty());
        assertTrue(already.pageWrites.isEmpty());
    }

    @Test
    @DisplayName("来源失效或成员撤权在写入之前拦住发布")
    void staleSourceAndRevokedMembershipStopThePublicationBeforeAnyWrite() {
        Fixture stale = new Fixture();
        stale.pages.put(PAGE_ID, page(DRAFT_REVISION_ID, null, 7L));
        stale.revisions.put(DRAFT_REVISION_ID, revision(DRAFT_REVISION_ID,
                WikiPublicationStatusEnum.DRAFT, WikiReviewStatusEnum.NOT_REQUIRED, 1L,
                WikiPublicationPolicyEnum.DIRECT));
        stale.activeRevision = "61002";
        CommonException failure = assertThrows(CommonException.class,
                () -> stale.lifecycle.transition(command(WikiTransitionEventEnum.DIRECT_PUBLISH,
                        DRAFT_REVISION_ID, 7L, 1L), actor()));
        assertEquals("WIKI_SOURCE_STALE", failure.getStatus());
        assertEquals(409, failure.getCode());
        assertTrue(stale.revisionWrites.isEmpty());
        assertTrue(stale.audits.isEmpty());

        Fixture revoked = new Fixture();
        revoked.pages.put(PAGE_ID, page(DRAFT_REVISION_ID, null, 7L));
        revoked.revisions.put(DRAFT_REVISION_ID, revision(DRAFT_REVISION_ID,
                WikiPublicationStatusEnum.DRAFT, WikiReviewStatusEnum.NOT_REQUIRED, 1L,
                WikiPublicationPolicyEnum.DIRECT));
        revoked.memberRole = KnowledgeMemberRoleEnum.READER;
        CommonException forbidden = assertThrows(CommonException.class,
                () -> revoked.lifecycle.transition(command(WikiTransitionEventEnum.DIRECT_PUBLISH,
                        DRAFT_REVISION_ID, 7L, 1L), actor()));
        assertEquals("KNOWLEDGE_FORBIDDEN", forbidden.getStatus());
        assertEquals(403, forbidden.getCode());
        assertTrue(revoked.audits.isEmpty());
        assertTrue(revoked.revisionWrites.isEmpty());
    }

    @Test
    @DisplayName("缺失页面或跨知识库版本按不存在处理")
    void missingPageAndCrossBaseRevisionReadAsNotFound() {
        Fixture fixture = new Fixture();

        assertThrows(GatewayAdminNotFoundException.class,
                () -> fixture.lifecycle.transition(command(WikiTransitionEventEnum.DIRECT_PUBLISH,
                        DRAFT_REVISION_ID, 7L, 1L), actor()));

        fixture.pages.put(PAGE_ID, page(DRAFT_REVISION_ID, null, 7L));
        assertThrows(GatewayAdminNotFoundException.class,
                () -> fixture.lifecycle.transition(command(WikiTransitionEventEnum.DIRECT_PUBLISH,
                        DRAFT_REVISION_ID, 7L, 1L), actor()));
        assertTrue(fixture.audits.isEmpty());
        assertTrue(fixture.revisionWrites.isEmpty());
    }

    @Test
    @DisplayName("批次发布逐页推进，一页 CAS 冲突即中止其余页面")
    void oneConflictingPageRollsBackTheWholePublicationBatch() {
        Fixture fixture = new Fixture();
        fixture.modelAnswer = """
                {"pages":[
                  {"slug":"alpha-page","title":"Alpha","markdown":"# Alpha","tags":["a"],
                   "links":[],"sources":["S1"]},
                  {"slug":"beta-page","title":"Beta","markdown":"# Beta","tags":["b"],
                   "links":[],"sources":["S2"]}
                ]}""";
        KnowledgeJobBO job = generationJob(generationPayload(null, 0L));

        KnowledgeJobBO result = fixture.generation.run(job, 5L);

        assertEquals(KnowledgeJobStatusEnum.SUCCEEDED, result.getStatus());
        assertEquals(2, fixture.publishedPages.size());
        assertEquals(2, fixture.pages.size());
        assertTrue(fixture.revisions.values().stream()
                .allMatch(row -> WikiPublicationStatusEnum.PUBLISHED.equals(row.getPublicationStatus())));
        assertEquals(List.of(), fixture.audits.stream().filter(row -> !row.isSuccessful()).toList());
        for (WikiPageBO page : fixture.pages.values()) {
            assertEquals(page.getPublishedRevisionId(), fixture.pages.get(page.getId()).getPublishedRevisionId());
            assertNull(page.getDraftRevisionId());
            assertEquals(3L, page.getRevision());
        }

        Fixture partial = new Fixture();
        partial.modelAnswer = fixture.modelAnswer;
        partial.failPageCasForPage = "1";
        KnowledgeJobBO partialResult = partial.generation.run(generationJob(generationPayload(null, 0L)), 5L);

        assertEquals(KnowledgeJobStatusEnum.FAILED, partialResult.getStatus());
        assertEquals("WIKI_STATE_CONFLICT", partialResult.getErrorCode());
        assertEquals(List.of("1"), partial.pageCasAttempts);
        assertTrue(partial.publishedPages.isEmpty());
        assertTrue(partial.pages.values().stream()
                .allMatch(page -> page.getPublishedRevisionId() == null));
        assertTrue(partial.revisions.values().stream()
                .allMatch(row -> WikiPublicationStatusEnum.DRAFT.equals(row.getPublicationStatus())));
    }

    @Test
    @DisplayName("生成结果超过二十页或引用越界都按校验失败收束")
    void oversizedOrForeignGenerationOutputIsRejected() {
        Fixture fixture = new Fixture();
        StringBuilder pages = new StringBuilder("{\"pages\":[");
        for (int index = 0; index < 21; index++) {
            pages.append(index == 0 ? "" : ",")
                    .append("{\"slug\":\"page-").append(index).append("\",\"title\":\"t")
                    .append(index).append("\",\"markdown\":\"m\",\"tags\":[],\"links\":[],\"sources\":[\"S1\"]}");
        }
        fixture.modelAnswer = pages.append("]}").toString();

        KnowledgeJobBO result = fixture.generation.run(generationJob(generationPayload(null, 0L)), 5L);

        assertEquals(KnowledgeJobStatusEnum.FAILED, result.getStatus());
        assertEquals("KNOWLEDGE_VALIDATION_FAILED", result.getErrorCode());
        assertTrue(fixture.pages.isEmpty());
        assertTrue(fixture.revisions.isEmpty());
        assertTrue(fixture.audits.isEmpty());

        Fixture foreign = new Fixture();
        foreign.modelAnswer = """
                {"pages":[{"slug":"alpha","title":"A","markdown":"m","tags":[],"links":[],
                 "sources":["S9"]}]}""";
        KnowledgeJobBO foreignResult = foreign.generation.run(generationJob(generationPayload(null, 0L)), 5L);
        assertEquals(KnowledgeJobStatusEnum.FAILED, foreignResult.getStatus());
        assertEquals("KNOWLEDGE_VALIDATION_FAILED", foreignResult.getErrorCode());
        assertTrue(foreign.pages.isEmpty());
    }

    @Test
    @DisplayName("策略快照在创建修订时冻结且不随未来配置改写")
    void publicationPolicySnapshotIsFrozenWhenTheRevisionIsCreated() {
        WikiRevisionBO created = WikiRevisionBO.newDraft(TENANT_KB, PAGE_ID, "Draft", "# draft",
                List.of(), List.of(), List.of(source()), ACTOR_ID, null,
                WikiPublicationPolicyEnum.DIRECT);

        assertEquals(WikiPublicationStatusEnum.DRAFT, created.getPublicationStatus());
        assertEquals(WikiReviewStatusEnum.NOT_REQUIRED, created.getReviewStatus());
        assertEquals(WikiPublicationPolicyEnum.DIRECT, created.getPublicationPolicySnapshot());
        assertEquals(1L, created.getPublicationVersion());
        assertEquals(1L, created.getRevision());
        assertNull(created.getReviewerActorId());
        assertNull(created.getReviewedAt());
        assertNull(created.getReviewInstanceId());
        assertNull(created.getPublishedAt());
        assertNull(created.getArchivedAt());
        assertEquals(Boolean.FALSE, created.getEverPublished());
    }

    private static WikiTransitionCommandDTO command(WikiTransitionEventEnum event,
                                                    String revisionId,
                                                    long pageRevision,
                                                    long publicationVersion) {
        return WikiTransitionCommandDTO.builder()
                .pageId(PAGE_ID)
                .revisionId(revisionId)
                .event(event)
                .expectedPageRevision(pageRevision)
                .expectedPublicationVersion(publicationVersion)
                .reasonCode("STEP14_" + event.wireValue())
                .build();
    }

    private static AdminActor actor() {
        return new AdminActor(ACTOR_ID, AdminActorTypeEnum.USER,
                java.util.Set.of("yuheng:knowledge:write"), java.util.Set.of("admin"));
    }

    private static WikiSourceDTO source() {
        return new WikiSourceDTO(SOURCE_REVISION_ID, "62001", SOURCE_HASH);
    }

    /** 中文说明：冻结修订的一条可引用分块。/ one citable chunk of the frozen revision. */
    private static KnowledgeChunkBO chunk(String id, int index) {
        return KnowledgeChunkBO.builder()
                .id(id).kbId(TENANT_KB).revisionId(SOURCE_REVISION_ID)
                .chunkIndex(index).content("alpha chunk " + index).contentHash(SOURCE_HASH)
                .embeddingSpaceId("space-1").dimensions(1024).revision(1L)
                .createdAt(NOW).updatedAt(NOW)
                .build();
    }

    /** 中文说明：作业载荷是提交时冻结的意图，与 {@code WikiServiceImpl} 写入的字段同名同形。/ a job payload is the
     * intent frozen at submission, same keys and shape as {@code WikiServiceImpl} writes. */
    private static String generationPayload(String pageId, long basePageRevision) {
        return "{\"kbId\":\"" + TENANT_KB + "\",\"pageId\":" + (pageId == null ? "null" : "\"" + pageId + "\"")
                + ",\"basePageRevision\":" + basePageRevision + ",\"sources\":[{\"documentRevisionId\":\""
                + SOURCE_REVISION_ID + "\",\"sourceHash\":\"" + SOURCE_HASH + "\"}]}";
    }

    private static WikiPageBO page(String draftRevisionId, String publishedRevisionId, long revision) {
        return WikiPageBO.builder()
                .id(PAGE_ID)
                .kbId(TENANT_KB)
                .slug("runbook-alpha")
                .draftRevisionId(draftRevisionId)
                .publishedRevisionId(publishedRevisionId)
                .revision(revision)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
    }

    private static WikiRevisionBO revision(String id,
                                           WikiPublicationStatusEnum status,
                                           WikiReviewStatusEnum reviewStatus,
                                           long publicationVersion,
                                           WikiPublicationPolicyEnum policy) {
        return WikiRevisionBO.builder()
                .id(id)
                .kbId(TENANT_KB)
                .pageId(PAGE_ID)
                .title("Alpha")
                .markdown("# Alpha body")
                .tags(List.of("a"))
                .links(List.of())
                .sources(List.of(source()))
                .contentHash("b".repeat(64))
                .authorActorId(ACTOR_ID)
                .publicationStatus(status)
                .reviewStatus(reviewStatus)
                .publicationPolicySnapshot(policy)
                .publicationVersion(publicationVersion)
                .everPublished(WikiPublicationStatusEnum.PUBLISHED.equals(status))
                .publishedAt(WikiPublicationStatusEnum.PUBLISHED.equals(status) ? NOW : null)
                .revision(1L)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
    }

    private static KnowledgeJobBO generationJob(String payload) {
        return KnowledgeJobBO.builder()
                .id("80001")
                .kbId(TENANT_KB)
                .type(KnowledgeJobTypeEnum.WIKI_GENERATE)
                .resourceId(SOURCE_REVISION_ID)
                .actorId(ACTOR_ID)
                .payload(readTree(payload))
                .status(KnowledgeJobStatusEnum.RUNNING)
                .stage(KnowledgeJobStageEnum.GENERATE)
                .attempt(1)
                .leaseOwner("worker-1")
                .leaseToken(5L)
                .leaseExpiresAt(NOW.plusSeconds(300))
                .revision(2L)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
    }

    private static com.fasterxml.jackson.databind.JsonNode readTree(String json) {
        try {
            return new ObjectMapper().readTree(json);
        } catch (com.fasterxml.jackson.core.JsonProcessingException failure) {
            throw new IllegalStateException(failure);
        }
    }

    /** 脚本化协作方装配出的真实状态机、真实 DIRECT 策略与真实生成批次逻辑 / the real state machine, DIRECT policy and batch generator wired over scripted collaborators. */
    private static final class Fixture {

        private final Map<String, WikiPageBO> pages = new LinkedHashMap<>();
        private final Map<String, WikiRevisionBO> revisions = new LinkedHashMap<>();
        private final Map<String, String> superseded = new LinkedHashMap<>();
        private final List<WikiRevisionBO> revisionWrites = new ArrayList<>();
        private final List<WikiPageBO> pageWrites = new ArrayList<>();
        private final List<String> pageCasAttempts = new ArrayList<>();
        private final List<GatewayAuditLogBO> audits = new ArrayList<>();
        private final List<String> publishedPages = new ArrayList<>();
        private final WikiRepository wikiRepository;
        private final KnowledgeRepository knowledgeRepository;
        private final GatewayAuditLogRepository auditRepository;
        private final WikiLifecycleService lifecycle;
        private final WikiGenerationStrategy generation;
        private String modelAnswer = "{\"pages\":[]}";
        private String activeRevision = SOURCE_REVISION_ID;
        private KnowledgeMemberRoleEnum memberRole = KnowledgeMemberRoleEnum.EDITOR;
        private boolean failRevisionCas;
        private String failPageCasForPage;

        private Fixture() {
            Clock fixed = Clock.fixed(NOW, ZoneOffset.UTC);
            Map<WikiPublicationPolicyEnum, WikiPublicationPolicyStrategy> registry =
                    new EnumMap<>(WikiPublicationPolicyEnum.class);
            registry.put(WikiPublicationPolicyEnum.DIRECT, new DirectWikiPublicationPolicyStrategy());
            this.wikiRepository = proxy(WikiRepository.class, new WikiScript());
            this.knowledgeRepository = proxy(KnowledgeRepository.class, new KnowledgeScript());
            this.auditRepository = proxy(GatewayAuditLogRepository.class, new AuditScript());
            this.lifecycle = new WikiLifecycleServiceImpl(
                    wikiRepository, knowledgeRepository, auditRepository, registry, fixed);
            this.generation = new WikiGenerationStrategy(
                    knowledgeRepository, wikiRepository, lifecycle,
                    proxy(KnowledgeModelClientService.class, new ModelScript()), registry,
                    new KnowledgeProperties(), new ObjectMapper(), fixed);
        }

        private final class WikiScript implements InvocationHandler {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String name = method.getName();
                return switch (name) {
                    case "findPage" -> Optional.ofNullable(pages.get((String) args[0]));
                    case "findPageBySlug" -> pages.values().stream()
                            .filter(row -> row.getSlug().equals(args[1]))
                            .findFirst();
                    case "findRevision" -> Optional.ofNullable(revisions.get((String) args[1]));
                    case "listRevisions" -> {
                        @SuppressWarnings("unchecked")
                        List<String> ids = (List<String>) args[1];
                        yield ids.stream().map(revisions::get).filter(Objects::nonNull).toList();
                    }
                    case "insertPage" -> {
                        WikiPageBO row = (WikiPageBO) args[0];
                        row.setId(Integer.toString(pages.size() + 1));
                        pages.put(row.getId(), row);
                        yield row;
                    }
                    case "insertRevision" -> {
                        WikiRevisionBO row = (WikiRevisionBO) args[0];
                        row.setId(Integer.toString(revisions.size() + 77_002));
                        revisions.put(row.getId(), row);
                        yield row;
                    }
                    case "transitionRevision" -> {
                        WikiRevisionBO target = (WikiRevisionBO) args[0];
                        if (failRevisionCas) {
                            yield false;
                        }
                        revisionWrites.add(target);
                        copyLifecycle(target, revisions.get(target.getId()));
                        yield true;
                    }
                    case "movePagePointers" -> {
                        WikiPageBO target = (WikiPageBO) args[0];
                        long expectedPageRevision = (Long) args[1];
                        pageCasAttempts.add(target.getId());
                        WikiPageBO stored = pages.get(target.getId());
                        if (target.getId().equals(failPageCasForPage)
                                || stored == null || stored.getRevision() != expectedPageRevision) {
                            yield false;
                        }
                        pageWrites.add(target);
                        stored.setPublishedRevisionId(target.getPublishedRevisionId());
                        stored.setDraftRevisionId(target.getDraftRevisionId());
                        stored.setRevision(target.getRevision());
                        if (target.getPublishedRevisionId() != null) {
                            publishedPages.add(target.getId());
                        }
                        yield true;
                    }
                    case "supersedePrevious" -> {
                        WikiRevisionBO previous = revisions.get((String) args[1]);
                        if (previous == null
                                || !WikiPublicationStatusEnum.PUBLISHED.equals(previous.getPublicationStatus())
                                || previous.getPublicationVersion() != (Long) args[3]) {
                            yield false;
                        }
                        superseded.put((String) args[2], (String) args[1]);
                        previous.setPublicationStatus(WikiPublicationStatusEnum.SUPERSEDED);
                        previous.setPublicationVersion(previous.getPublicationVersion() + 1);
                        yield true;
                    }
                    case "listPublishedRevisions" -> revisions.values().stream()
                            .filter(row -> WikiPublicationStatusEnum.PUBLISHED.equals(row.getPublicationStatus()))
                            .toList();
                    case "listPages" -> List.of();
                    case "countPages" -> 0L;
                    case "toString" -> "WikiScript";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new AssertionError("WikiRepository." + name
                            + " must not be reached by this test");
                };
            }
        }

        private final class KnowledgeScript implements InvocationHandler {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                return switch (method.getName()) {
                    case "findBase" -> Optional.of(KnowledgeBaseBO.builder()
                            .id(TENANT_KB)
                            .ownerActorId("other")
                            .members(List.of(new KnowledgeMemberDTO(ACTOR_ID, memberRole)))
                            .embeddingSpaceId("space-1")
                            .dimensions(1024)
                            .revision(3L)
                            .build());
                    case "findDocument" -> Optional.of(KnowledgeDocumentBO.builder()
                            .id(DOCUMENT_ID).kbId(TENANT_KB).fileName("alpha.md")
                            .activeRevisionId(activeRevision).revision(2L).build());
                    case "findRevision" -> Optional.of(KnowledgeDocumentRevisionBO.builder()
                            .id(SOURCE_REVISION_ID).kbId(TENANT_KB).documentId(DOCUMENT_ID)
                            .fileName("alpha.md").contentHash(SOURCE_HASH).byteCount(12L)
                            .extractedText("alpha source body")
                            .status(KnowledgeRevisionStatusEnum.READY)
                            .chunkCount(2).revision(2L).build());
                    case "listChunksOfRevision" -> List.of(chunk("62001", 0), chunk("62002", 1));
                    case "heartbeat" -> Long.valueOf(5L).equals(args[1]);
                    case "toString" -> "KnowledgeScript";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new AssertionError("KnowledgeRepository." + method.getName()
                            + " must not be reached by this test");
                };
            }
        }

        private final class AuditScript implements InvocationHandler {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                return switch (method.getName()) {
                    case "save" -> {
                        audits.add((GatewayAuditLogBO) args[0]);
                        yield args[0];
                    }
                    case "toString" -> "AuditScript";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new AssertionError("GatewayAuditLogRepository." + method.getName()
                            + " must not be reached by this test");
                };
            }
        }

        private final class ModelScript implements InvocationHandler {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                return switch (method.getName()) {
                    case "generate" -> modelAnswer;
                    case "toString" -> "ModelScript";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new AssertionError("KnowledgeModelClientService." + method.getName()
                            + " must not be reached by this test");
                };
            }
        }
    }

    private static void copyLifecycle(WikiRevisionBO from, WikiRevisionBO to) {
        if (to == null) {
            return;
        }
        to.setPublicationStatus(from.getPublicationStatus());
        to.setReviewStatus(from.getReviewStatus());
        to.setPublicationVersion(from.getPublicationVersion());
        to.setPublishedAt(from.getPublishedAt());
        to.setPublishedByActorId(from.getPublishedByActorId());
        to.setArchivedAt(from.getArchivedAt());
        to.setArchivedByActorId(from.getArchivedByActorId());
        to.setReviewDecisionCode(from.getReviewDecisionCode());
        to.setReviewerActorId(from.getReviewerActorId());
        to.setReviewedAt(from.getReviewedAt());
        to.setReviewInstanceId(from.getReviewInstanceId());
        to.setPublicationErrorCode(from.getPublicationErrorCode());
        to.setEverPublished(from.getEverPublished());
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> port, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(port.getClassLoader(), new Class<?>[]{port}, handler);
    }
}
