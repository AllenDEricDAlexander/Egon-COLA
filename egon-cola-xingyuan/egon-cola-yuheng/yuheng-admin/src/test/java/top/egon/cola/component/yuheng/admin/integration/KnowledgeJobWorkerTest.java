package top.egon.cola.component.yuheng.admin.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.component.rag.api.RagExtractionService;
import top.egon.cola.component.rag.chunk.RagChunkingStrategy;
import top.egon.cola.component.rag.chunk.RagChunkingStrategyEnum;
import top.egon.cola.component.rag.chunk.RagChunkingStrategyFactory;
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
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeAnswerCommandDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeMemberDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeUploadCommandDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeEgressPolicyEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobStageEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobStatusEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobTypeEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeMemberRoleEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeRevisionStatusEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeAnswerVO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeDocumentVO;
import top.egon.cola.component.yuheng.admin.knowledge.repository.KnowledgeRepository;
import top.egon.cola.component.yuheng.admin.knowledge.service.DocumentIngestionStrategy;
import top.egon.cola.component.yuheng.admin.knowledge.service.KnowledgeJobStrategy;
import top.egon.cola.component.yuheng.admin.knowledge.service.KnowledgeModelClientService;
import top.egon.cola.component.yuheng.admin.knowledge.service.KnowledgeRetrievalService;
import top.egon.cola.component.yuheng.admin.knowledge.service.impl.KnowledgeJobServiceImpl;
import top.egon.cola.component.yuheng.admin.knowledge.service.impl.KnowledgeServiceImpl;
import top.egon.cola.component.yuheng.admin.llm.repository.LlmConfigurationRepository;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;
import top.egon.cola.component.yuheng.admin.shared.domain.enums.AdminActorTypeEnum;
import top.egon.cola.component.yuheng.admin.shared.repository.IdempotencyRepository;

/**
 * 中文说明：{@code KnowledgeJobWorkerTest} 固定 Step 12 的三条摄取与发布不变式，它们都取自业务 Spec 而不是实现：
 * （一）API-015 上传必须在<b>同一次写集合</b>里落「文档行 + 携带原件字节的 STAGING 修订行 + {@code DOCUMENT_INGEST}
 * 作业行」，被拒的请求必须零业务写入，且上传路径上一个模型调用都不许发生（Spec §7.3.3「任务与原件创建原子」
 * 「外部 LLM 不属于 PG 事务」）；（二）<b>过期租约不能发布</b>——租约被接管（令牌单调 +1）之后，旧持有者既不能
 * 切换活动修订也不能写终态，因为每一次写回都以 {@code id + lease_token + status = 'RUNNING' +
 * lease_expires_at > now()} 为条件，影响 0 行是「所有权已丢失」而不是成功（Spec §7.3.3 的 job claim 行、§11.2.7
 * 访问模式表）；（三）<b>嵌入部分失败时旧活动修订原样保留</b>——只有全部向量合格且 {@code stageChunks} 成功之后
 * 才允许 CAS 激活，暂存的分块永远不能让半成品索引对检索可见（Spec §7.2.1 的 Publish 门禁、§7.3.4）。
 * 持久侧由本类内部的 {@link ScriptedKnowledgeStore} 这个脚本化替身承担，它<b>真实执行</b> CAS 谓词：认领会推进令牌
 * 并换掉持有者，{@code heartbeat}/{@code finish}/{@code activateRevision} 一旦令牌或到期时刻不再匹配就返回
 * {@code false}，{@code stageChunks} 逐条校验向量长度/有限/非零并累计写入行数；断言只问「库里的最终事实」，
 * 不镜像实现的当前行为。
 * <b>为摄取用例补齐的输入</b>：{@code DocumentIngestionStrategy} 只认作业 payload 里的 {@code revisionId}/
 * {@code sourceRevisionId} 键（它自己的契约就这么写的），而 {@code KnowledgeServiceImpl.ingestPayload} 生成的载荷
 * 里<b>没有</b>这两个键（它只冻结 baseId/contentHash/dimensions/embeddingSpace/fileName/mediaType/model），因此真实
 * 上传产出的 {@code DOCUMENT_INGEST} 行在现状态下必然以 {@code KNOWLEDGE_VALIDATION_FAILED} 收束。该缺陷只如实上报、
 * 不改别人的文件；为了让屏障与围栏语义有可断言的对象，{@link Fixture} 在真实上传之后用
 * {@link ScriptedKnowledgeStore#freezeSourceRevisionInPayload} 只往那一行的 payload 里补上「本作业对应的修订 id」，
 * 三行本身仍然全部由生产代码写出。除该键名之外，本类不对 payload 结构作任何断言。
 * English summary: {@code KnowledgeJobWorkerTest} pins Step 12's three ingestion and publication invariants, each taken
 * from the business Spec rather than from the implementation: (1) API-015 must commit the document row, the STAGING
 * revision row holding the original bytes and the {@code DOCUMENT_INGEST} job row in one single write set, a rejected
 * request must write nothing at all, and no model call may happen on the upload path (Spec §7.3.3 "task and original are
 * created atomically", "the external LLM is not part of the PG transaction"); (2) a stale lease cannot publish — after a
 * take-over monotonically advances the token, the previous holder may neither switch the active revision nor write a
 * terminal state, because every write-back is conditioned on {@code id + lease_token + status = 'RUNNING' +
 * lease_expires_at > now()} and a zero-row effect means lost ownership, not success; (3) a partial embedding failure
 * leaves the previous active revision untouched — activation is allowed only after every vector validates and
 * {@code stageChunks} succeeded, so staged rows can never expose a half-built index to retrieval. The persistence side
 * is the scripted fake below, which genuinely evaluates those CAS predicates instead of mirroring production behaviour.
 * Two cross-writer contract breaks are reported rather than patched: the upload payload carries no source revision
 * pointer even though the ingestion strategy reads exactly that key (so {@link Fixture} supplies it for the ingestion
 * cases only), and the chunking configuration is frozen under the name {@code FIXED_WINDOW}, which is not a member of
 * the closed {@link RagChunkingStrategyEnum} vocabulary, so ingestion always degrades to the {@code TOKEN} fallback.
 *
 * 用法 / Usage: 纯 JUnit 5 + AssertJ，直接 {@code new} 生产类，不启动 Spring 容器、不用 Mockito、不用 H2、不联网：
 * {@code ./mvnw -pl egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin -am -Dtest=KnowledgeJobWorkerTest
 * -Dsurefire.failIfNoSpecifiedTests=false test}。
 * <b>覆盖范围的如实声明</b>：Spec 要求的真实 SQL 路径是 MyBatis→租户/审计守卫→ShardingSphere→PostgreSQL，
 * 并且明确禁止用 mock 取代它；本会话未被授权任何 PostgreSQL 实例，所以本类只固定「编排、租约围栏与
 * staging/激活屏障」这一层的语义。{@code selectClaimable}/{@code claimJob}/{@code heartbeatJob}/{@code finishJob}/
 * {@code activateRevision} 这五条具名语句在真库上的认领与发布路径，上传三行的<b>单次提交</b>（由
 * {@code @Transactional} 保证）以及租户/审计/软删列的补齐，都仍是运行期未验证事实，须在 Step 16 的
 * {@code GatewayManagedSchemaIT} 用隔离数据库闭环。断言刻意不碰只有 {@code @SpringBootTest} 才看得见的事实
 * （bean 装配、事务代理、拦截器链），因为它们不在本层的证据范围内。
 * / Plain JUnit 5 plus AssertJ: the production classes are constructed directly, with no Spring context, no Mockito, no
 * H2 and no network. Stated honestly: the Spec demands the real MyBatis→guard→ShardingSphere→PostgreSQL path and
 * forbids replacing it with mocks, and no PostgreSQL is authorized in this session, so this class fixes only the
 * orchestration, lease-fencing and staging/activation barrier layer. The five named statements
 * {@code selectClaimable}/{@code claimJob}/{@code heartbeatJob}/{@code finishJob}/{@code activateRevision} against a
 * real database, the single-commit property of the upload trio (guaranteed by {@code @Transactional}) and the stamping
 * of tenant, audit and soft-delete columns all stay runtime-unverified until an isolated database is authorized in Step
 * 16's {@code GatewayManagedSchemaIT}. Assertions deliberately avoid anything only observable under
 * {@code @SpringBootTest} (bean assembly, transaction proxies, interceptor chains) since those are not this layer's
 * evidence.
 */
class KnowledgeJobWorkerTest {

    /** 受控时钟的起点：所有「此刻」都从这里推进，绝不读真实时间。 / The controlled clock's origin; every "now" advances from here and never reads wall time. */
    private static final Instant BASE = Instant.parse("2026-09-25T02:00:00Z");

    private static final int DIMENSIONS = 8;

    private static final String EMBEDDING_SPACE = "local:bge-m3-v1";

    private static final AdminActor OWNER = new AdminActor(
            "svc:knowledge-owner", AdminActorTypeEnum.USER, Set.of("KNOWLEDGE_ADMIN"), Set.of("KNOWLEDGE_ADMIN"));

    private static final AdminActor EDITOR = new AdminActor(
            "svc:wiki-editor", AdminActorTypeEnum.USER, Set.of("KNOWLEDGE_EDIT"), Set.of("KNOWLEDGE_EDIT"));

    private static final AdminActor STRANGER = new AdminActor(
            "svc:stranger", AdminActorTypeEnum.USER, Set.of("NONE"), Set.of("NONE"));

    @BeforeAll
    static void bindTheProcessWideSnowflake() {
        SnowflakeIdGenerator.initialize(0L, Duration.ofMillis(5));
    }

    // -------------------------------------------------------------------------------------------------------------
    // (a) 上传：原件字节 + 修订 + 作业是同一次写集合；被拒即零写入；事务内不碰模型
    // -------------------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("API-015 上传把原件字节、STAGING 修订与 DOCUMENT_INGEST 作业落在同一次写集合里，且全程零模型调用")
    void uploadCommitsOriginalBytesRevisionAndJobAsOneWriteSetWithoutTouchingTheModel() {
        Fixture fixture = new Fixture();
        byte[] original = "yuheng runbook\nrestart order: tianshu, xingyuan, yuheng\n"
                .getBytes(StandardCharsets.UTF_8);

        KnowledgeDocumentVO uploaded = fixture.upload(EDITOR, "runbook.md", "text/markdown", original,
                "upload-intent-00000001");

        // 一次上传恰好三行业务数据：不多（不重复排队），不少（不留孤立修订或孤立作业）。
        assertThat(fixture.store.documentCount()).isEqualTo(1);
        assertThat(fixture.store.revisionCount()).isEqualTo(1);
        assertThat(fixture.store.jobCount()).isEqualTo(1);

        KnowledgeDocumentBO document = fixture.store.onlyDocument();
        KnowledgeDocumentRevisionBO revision = fixture.store.onlyRevision();
        KnowledgeJobBO job = fixture.store.onlyJob();

        // 修订承载的就是原件本身：逐字节相同、长度自洽、内容哈希是可复算的小写 SHA-256。
        assertThat(revision.getRawBytes()).containsExactly(original);
        assertThat(revision.getByteCount()).isEqualTo((long) original.length);
        assertThat(revision.getContentHash()).isEqualTo(sha256Hex(original));
        assertThat(revision.getDocumentId()).isEqualTo(document.getId());
        assertThat(revision.getKbId()).isEqualTo(fixture.kbId());
        assertThat(revision.getFileName()).isEqualTo("runbook.md");
        assertThat(revision.getMediaType()).isEqualTo("text/markdown");
        // PARSE 之前既没有抽取正文也没有分块，而且不是 READY，所以检索侧不可能看到它。
        assertThat(revision.getStatus()).isEqualTo(KnowledgeRevisionStatusEnum.STAGING);
        assertThat(revision.getExtractedText()).isNull();
        assertThat(revision.getChunkCount()).isZero();
        // 嵌入空间与维度按知识库的冻结值快照写入，杜绝「这次上传换了另一套维度口径」。
        assertThat(revision.getEmbeddingSpaceId()).isEqualTo(EMBEDDING_SPACE);
        assertThat(revision.getDimensions()).isEqualTo(DIMENSIONS);

        // 作业行与文档行互相指向：resourceId 是文档、文档的 latestJobId 是这条作业，客户端据此轮询 API-019。
        // 作业与「哪一条修订」的绑定本应落在 payload，但真实上传的载荷里没有该键（见类注释上报的缺陷），
        // 所以这里只固定确实存在的闭环链接，不去断言一个尚未被写出的键，也不把这个缺口当成正确行为。
        assertThat(job.getType()).isEqualTo(KnowledgeJobTypeEnum.DOCUMENT_INGEST);
        assertThat(job.getStatus()).isEqualTo(KnowledgeJobStatusEnum.QUEUED);
        assertThat(job.getStage()).isEqualTo(KnowledgeJobStageEnum.QUEUED);
        assertThat(job.getResourceId()).isEqualTo(document.getId());
        assertThat(job.getKbId()).isEqualTo(fixture.kbId());
        assertThat(job.getActorId()).isEqualTo(EDITOR.actorId());
        assertThat(job.getAttempt()).isZero();
        assertThat(job.getIdempotencyKey()).isEqualTo("upload-intent-00000001");
        assertThat(job.getRequestHash()).hasSize(64);
        // 排队中的作业不得持有任何租约，否则一个还没开始的任务就已被 fencing 判成「已认领」。
        assertThat(job.getLeaseToken()).isZero();
        assertThat(job.getLeaseOwner()).isNull();
        assertThat(job.getLeaseExpiresAt()).isNull();
        // 首发可认领时刻就是受控时钟的当下：既没有凭空的未来排程，也没有靠真实时间逃逸可控时钟。
        assertThat(job.getNextAttemptAt()).isEqualTo(fixture.clock.instant());

        // 文档指针：latestJobId 已闭环，activeRevisionId 仍是旧值，所以上传永不把半成品索引暴露给检索。
        assertThat(document.getLatestJobId()).isEqualTo(job.getId());
        assertThat(document.getActiveRevisionId()).isNull();
        assertThat(uploaded.getId()).isEqualTo(document.getId());
        assertThat(uploaded.getLatestJobId()).isEqualTo(job.getId());
        assertThat(uploaded.getActiveRevisionId()).isNull();

        // Spec §7.3.3「外部 LLM 不属于 PG 事务」：上传路径上模型端口一次都没被碰过。
        assertThat(fixture.model.embedCalls()).isZero();
        assertThat(fixture.model.generateCalls()).isZero();
    }

    @Test
    @DisplayName("上传被拒时零业务写入：非成员 403 之后 document/revision/job 三张表都还是空的")
    void rejectedUploadLeavesNoDocumentRevisionOrJobBehind() {
        Fixture fixture = new Fixture();
        byte[] original = "must not be persisted".getBytes(StandardCharsets.UTF_8);

        assertThatExceptionOfType(CommonException.class)
                .isThrownBy(() -> fixture.upload(STRANGER, "secret.md", "text/markdown", original,
                        "upload-intent-00000002"))
                .satisfies(error -> {
                    assertThat(error.getCode()).isEqualTo(403);
                    assertThat(error.getStatus()).isEqualTo("KNOWLEDGE_FORBIDDEN");
                });

        assertThat(fixture.store.documentCount()).isZero();
        assertThat(fixture.store.revisionCount()).isZero();
        assertThat(fixture.store.jobCount()).isZero();
        // 事件日志里唯一允许的 INSERT 是 Fixture 自己 arrange 出来的知识库行：被拒的请求不得写下任何文档、修订或作业。
        assertThat(fixture.store.eventLog()).noneMatch(event -> event.startsWith("INSERT:document:")
                || event.startsWith("INSERT:revision:")
                || event.startsWith("INSERT:job:"));
        assertThat(fixture.model.embedCalls()).isZero();
    }

    // -------------------------------------------------------------------------------------------------------------
    // (b) 租约：claim 只按空闲槽、令牌单调；被接管之后旧持有者既写不了终态也激活不了修订
    // -------------------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("claim 从不超过空闲槽，且每次认领（含到期接管）都把该行的租约令牌单调 +1")
    void claimIsBoundedByTheFreeSlotsAndAdvancesTheLeaseTokenMonotonically() {
        Fixture fixture = new Fixture();
        fixture.upload(EDITOR, "one.md", "text/markdown", bytes("one"), "claim-intent-0000000001");
        fixture.upload(EDITOR, "two.md", "text/markdown", bytes("two"), "claim-intent-0000000002");
        fixture.upload(EDITOR, "three.md", "text/markdown", bytes("three"), "claim-intent-0000000003");
        fixture.store.leaseOwner("worker-a");

        // 没有空闲槽就没有任何一行属于本轮：slots = 0 必须返回空，而不是「顺手领一行」。
        assertThat(fixture.jobs.claimAvailable(0)).isEmpty();

        List<KnowledgeJobBO> single = fixture.jobs.claimAvailable(1);
        assertThat(single).hasSize(1);
        assertThat(single.get(0).getStatus()).isEqualTo(KnowledgeJobStatusEnum.RUNNING);
        // claim 不改变阶段：阶段推进是策略的职责，认领只更换所有权。
        assertThat(single.get(0).getStage()).isEqualTo(KnowledgeJobStageEnum.QUEUED);
        assertThat(single.get(0).getLeaseToken()).isEqualTo(1L);
        assertThat(single.get(0).getLeaseOwner()).isEqualTo("worker-a");
        assertThat(single.get(0).getLeaseExpiresAt()).isEqualTo(fixture.clock.instant().plus(fixture.lease()));
        // 其余两行仍然是 QUEUED 且令牌为 0：没有「顺手多领」，也没有把未领取的行伪造成 RUNNING。
        // 「每次认领不超过空闲槽」就体现在这里——它是库里可数的行事实，而不是对查询形状的猜测。
        assertThat(fixture.store.unclaimedJobCount()).isEqualTo(2);

        // 空闲槽多于可领行时按行封顶，而不是伪造出第三行。
        assertThat(fixture.jobs.claimAvailable(5)).hasSize(2);
        assertThat(fixture.store.unclaimedJobCount()).isZero();
        // 三行都在 RUNNING 且未过期 ⇒ SKIP LOCKED 的可见结果集为空 ⇒ 再领一次仍然一行都没有。
        assertThat(fixture.jobs.claimAvailable(2)).isEmpty();

        // 到期接管：同一行的令牌必须严格大于它自己的旧值，旧 worker 的迟到写回才必然 0 行。
        fixture.clock.advance(fixture.lease());
        fixture.store.leaseOwner("worker-b");
        List<KnowledgeJobBO> takenOver = fixture.jobs.claimAvailable(1);
        assertThat(takenOver).hasSize(1);
        assertThat(takenOver.get(0).getId()).isEqualTo(single.get(0).getId());
        assertThat(takenOver.get(0).getLeaseToken()).isEqualTo(2L);
        assertThat(takenOver.get(0).getLeaseOwner()).isEqualTo("worker-b");
        assertThat(fixture.store.leaseTakeovers()).isEqualTo(1);
        // 旧载体还在手上，但它那一行的所有权已经易主：心跳返回 false，不是「成功但没延长」。
        assertThat(fixture.jobs.heartbeat(single.get(0))).isFalse();
        assertThat(fixture.store.job(single.get(0).getId()).getLeaseOwner()).isEqualTo("worker-b");
    }

    @Test
    @DisplayName("所有权丢失：被接管后旧持有者的终态写回影响 0 行，终态只由新持有者恰一次写入")
    void aLostLeaseCannotWriteTheTerminalStateAndOnlyOneHolderPublishesIt() {
        Fixture fixture = new Fixture();
        fixture.upload(EDITOR, "runbook.md", "text/markdown", bytes("runbook body"), "fence-intent-0000000001");

        fixture.store.leaseOwner("worker-a");
        KnowledgeJobBO holderA = fixture.onlyClaimedJob();
        fixture.clock.advance(fixture.lease());
        fixture.store.leaseOwner("worker-b");
        KnowledgeJobBO holderB = fixture.onlyClaimedJob();
        long revisionAfterTakeover = fixture.store.job(holderB.getId()).getRevision();
        assertThat(holderB.getLeaseToken()).isEqualTo(holderA.getLeaseToken() + 1);

        // 旧持有者试图写 SUCCEEDED：谓词不成立，库里那一行必须一个字节都没被改过。
        fixture.jobs.publishTerminal(holderA.setStatus(KnowledgeJobStatusEnum.SUCCEEDED)
                .setStage(KnowledgeJobStageEnum.DONE), holderA.getLeaseToken());

        assertThat(fixture.store.finishAttempts()).isEqualTo(1);
        assertThat(fixture.store.finishHits()).isZero();
        KnowledgeJobBO stillHeldByB = fixture.store.job(holderA.getId());
        assertThat(stillHeldByB.getStatus()).isEqualTo(KnowledgeJobStatusEnum.RUNNING);
        assertThat(stillHeldByB.getLeaseToken()).isEqualTo(holderB.getLeaseToken());
        assertThat(stillHeldByB.getLeaseOwner()).isEqualTo("worker-b");
        assertThat(stillHeldByB.getRevision()).isEqualTo(revisionAfterTakeover);
        // 写终态这条路也绝不允许顺带切换活动修订。
        assertThat(fixture.store.activationAttempts()).isZero();

        // 新持有者写终态：恰一次命中，并按「非 RUNNING 不保留到期时刻」收口租约列。
        KnowledgeJobBO published = fixture.jobs.publishTerminal(holderB.setStatus(KnowledgeJobStatusEnum.SUCCEEDED)
                .setStage(KnowledgeJobStageEnum.DONE), holderB.getLeaseToken());

        assertThat(fixture.store.finishHits()).isEqualTo(1);
        KnowledgeJobBO finished = fixture.store.job(holderB.getId());
        assertThat(finished.getStatus()).isEqualTo(KnowledgeJobStatusEnum.SUCCEEDED);
        assertThat(finished.getStage()).isEqualTo(KnowledgeJobStageEnum.DONE);
        assertThat(finished.getRevision()).isGreaterThan(revisionAfterTakeover);
        assertThat(published.getStatus()).isEqualTo(KnowledgeJobStatusEnum.SUCCEEDED);
        // 终态即所有权终结：哪怕把时钟推过原来的到期时刻，这一行也不可能再被任何人领走。
        fixture.clock.advance(fixture.lease().multipliedBy(2));
        assertThat(fixture.jobs.claimAvailable(2)).extracting(KnowledgeJobBO::getId).doesNotContain(holderB.getId());
    }

    @Test
    @DisplayName("租约被接管后，旧 worker 即使把向量与 staging 全做完也切换不了活动修订")
    void ingestionUnderATakenOverLeaseNeverActivatesTheRevision() {
        Fixture fixture = new Fixture();
        Published first = fixture.publishFirstRevisionHealthy("yuheng runbook v1\nrestart order: tianshu, xingyuan\n"
                .repeat(12));
        Queued second = fixture.ingestibleUpload("runbook v2\n".repeat(40), "second-intent-0000000001",
                first.documentId());

        // 基线已经活动过一次，所以负例只能看「增量」，否则「旧 active 被再次切换」会被累计计数掩盖。
        long activationsBefore = fixture.store.activations();
        long stagedBefore = fixture.store.stagedInserts();
        long heartbeatHitsBefore = fixture.store.heartbeatHits();
        long finishHitsBefore = fixture.store.finishHits();

        fixture.store.leaseOwner("worker-a");
        KnowledgeJobBO stale = fixture.claim(second.jobId());
        fixture.clock.advance(fixture.lease());
        fixture.store.leaseOwner("worker-b");
        KnowledgeJobBO current = fixture.claim(second.jobId());
        assertThat(current.getLeaseToken()).isEqualTo(stale.getLeaseToken() + 1);

        // 模型侧完全健康：能让激活发生的唯一差别就是租约，所以激活增量一旦不为零就是围栏本身失效。
        fixture.model.answerHealthily();
        KnowledgeJobBO outcome = fixture.ingestion.run(stale, stale.getLeaseToken());

        assertThat(fixture.store.activations()).isEqualTo(activationsBefore);
        assertThat(fixture.store.stagedInserts()).isEqualTo(stagedBefore);
        // 所有权证明失败得比任何一次模型调用都早：这就是「过期租约连向量都不该去要」。
        assertThat(fixture.store.heartbeatHits()).isEqualTo(heartbeatHitsBefore);
        assertThat(fixture.store.heartbeatRejections()).isEqualTo(1L);
        assertThat(fixture.store.document(first.documentId()).getActiveRevisionId()).isEqualTo(first.revisionId());
        assertThat(fixture.store.revision(second.revisionId()).getStatus())
                .isEqualTo(KnowledgeRevisionStatusEnum.STAGING);
        assertThat(outcome.getStatus()).isNotEqualTo(KnowledgeJobStatusEnum.SUCCEEDED);

        // 旧持有者的结论连终态都写不下：库里仍是新持有者在 RUNNING，发布权没有被覆盖第二次。
        // 先确认这确实是一个「结论」而不是还在 RUNNING，否则下面的写不回落就无法归因给租约围栏。
        assertThat(outcome.getStatus()).isNotEqualTo(KnowledgeJobStatusEnum.RUNNING);
        fixture.jobs.publishTerminal(outcome, stale.getLeaseToken());
        assertThat(fixture.store.finishHits()).isEqualTo(finishHitsBefore);
        KnowledgeJobBO row = fixture.store.job(second.jobId());
        assertThat(row.getStatus()).isEqualTo(KnowledgeJobStatusEnum.RUNNING);
        assertThat(row.getLeaseToken()).isEqualTo(current.getLeaseToken());
        assertThat(row.getLeaseOwner()).isEqualTo("worker-b");
        assertThat(fixture.store.document(first.documentId()).getActiveRevisionId()).isEqualTo(first.revisionId());
    }

    // -------------------------------------------------------------------------------------------------------------
    // (c) staging/激活屏障：暂存与向量全部合格才 CAS 激活；部分失败保留旧活动修订
    // -------------------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("健康摄取：staging 与向量校验全部通过之后才 CAS 激活，并且恰一次切换活动修订")
    void healthyIngestionStagesBeforeItActivatesTheRevisionExactlyOnce() {
        Fixture fixture = new Fixture();
        String body = "yuheng runbook\nrestart order: tianshu, xingyuan, yuheng\n".repeat(20);
        Queued queued = fixture.ingestibleUpload(body, "ingest-intent-0000000001", null);
        fixture.store.leaseOwner("worker-a");
        KnowledgeJobBO claimed = fixture.claim(queued.jobId());

        fixture.model.answerHealthily();
        KnowledgeJobBO outcome = fixture.ingestion.run(claimed, claimed.getLeaseToken());
        fixture.jobs.publishTerminal(outcome, claimed.getLeaseToken());

        KnowledgeDocumentRevisionBO revision = fixture.store.revision(queued.revisionId());
        KnowledgeDocumentBO document = fixture.store.document(queued.documentId());

        // 屏障顺序：最后一次 staging 必须早于唯一一次激活；激活命中即发布完成（恰一次，没有半切换）。
        assertThat(fixture.store.stagedInserts()).isPositive();
        assertThat(fixture.store.lastEventIndex("STAGE:")).isGreaterThanOrEqualTo(0);
        assertThat(fixture.store.firstEventIndex("ACTIVATE:")).isGreaterThan(fixture.store.lastEventIndex("STAGE:"));
        assertThat(fixture.store.activationAttempts()).isEqualTo(1);
        assertThat(fixture.store.activations()).isEqualTo(1);
        assertThat(document.getActiveRevisionId()).isEqualTo(revision.getId());
        assertThat(revision.getStatus()).isEqualTo(KnowledgeRevisionStatusEnum.READY);

        // 只有 READY 且「核对过的分块数 == 实暂存行数」的修订才配得上活动指针；向量沿用冻结维度。
        assertThat(revision.getChunkCount()).isEqualTo(expectedChunks(body));
        assertThat(fixture.store.chunksOf(revision.getId())).hasSize(revision.getChunkCount());
        assertThat(fixture.store.chunksOf(revision.getId())).allSatisfy(chunk -> {
            assertThat(chunk.getDimensions()).isEqualTo(DIMENSIONS);
            assertThat(chunk.getEmbeddingSpaceId()).isEqualTo(EMBEDDING_SPACE);
            assertThat(chunk.getEmbedding()).hasSize(DIMENSIONS);
            // 被索引的每一个字都来自上传的原件：内容不是占位符，摘要就是这段 UTF-8 的 SHA-256。
            assertThat(body.contains(chunk.getContent())).isTrue();
            assertThat(chunk.getContentHash()).isEqualTo(sha256Hex(bytes(chunk.getContent())));
        });
        assertThat(fixture.store.chunksOf(revision.getId())).extracting(KnowledgeChunkBO::getChunkIndex)
                .containsExactlyElementsOf(IntStream.range(0, revision.getChunkCount()).boxed().toList());

        // 文档只经本地嵌入 alias 产出向量：批次不超 64，维度恒为冻结值；终态由本轮持有者的 CAS 写入。
        assertThat(fixture.model.embedCalls()).isPositive();
        assertThat(fixture.model.batchSizes()).allSatisfy(size -> assertThat(size).isLessThanOrEqualTo(64));
        assertThat(fixture.model.requestedDimensions()).containsOnly(DIMENSIONS);
        assertThat(outcome.getStatus()).isEqualTo(KnowledgeJobStatusEnum.SUCCEEDED);
        assertThat(fixture.store.job(claimed.getId()).getStatus()).isEqualTo(KnowledgeJobStatusEnum.SUCCEEDED);
    }

    @Test
    @DisplayName("嵌入部分失败：第一批向量已到手也不能把新修订推上位，旧活动修订与其分块、状态原样保留")
    void partialEmbeddingFailureKeepsThePreviousActiveRevisionIntact() {
        Fixture fixture = new Fixture();
        Published first = fixture.publishFirstRevisionHealthy("yuheng runbook v1\n".repeat(12));
        // 100 个非空行 ⇒ 在 embedBatchSize=64 下必然分成两批，于是「第二批缺一行」才是真正的部分成功。
        String body = "runbook v2 restart order line\n".repeat(100);
        Queued second = fixture.ingestibleUpload(body, "second-intent-0000000001", first.documentId());
        fixture.store.leaseOwner("worker-a");
        KnowledgeJobBO claimed = fixture.claim(second.jobId());

        long activationsBefore = fixture.store.activations();
        long activationAttemptsBefore = fixture.store.activationAttempts();
        long stagedBefore = fixture.store.stagedInserts();
        int embedCallsBefore = fixture.model.embedCalls();

        // 第一批照常供给，第二批少给一条：数量不再等于分块数，这才是「部分成功」而不是「整批全失败」。
        fixture.model.answerFailingFromBatch(2);
        KnowledgeJobBO outcome = fixture.ingestion.run(claimed, claimed.getLeaseToken());

        // 部分成功的向量既没有换来一次暂存，也没有换来一次激活尝试——更没换来指针切换。
        assertThat(fixture.store.stagedInserts()).isEqualTo(stagedBefore);
        assertThat(fixture.store.activationAttempts()).isEqualTo(activationAttemptsBefore);
        assertThat(fixture.store.activations()).isEqualTo(activationsBefore);
        assertThat(fixture.store.document(first.documentId()).getActiveRevisionId()).isEqualTo(first.revisionId());

        // 旧修订的行数与状态没有被这次摄取触碰；新修订拿不到 READY，也拿不到分块计数。
        assertThat(fixture.store.chunksOf(first.revisionId())).hasSize(first.chunkCount());
        assertThat(fixture.store.revision(first.revisionId()).getStatus())
                .isEqualTo(KnowledgeRevisionStatusEnum.READY);
        assertThat(fixture.store.revision(second.revisionId()).getStatus())
                .isNotEqualTo(KnowledgeRevisionStatusEnum.READY);
        assertThat(fixture.store.revision(second.revisionId()).getChunkCount()).isZero();
        assertThat(outcome.getStatus()).isNotEqualTo(KnowledgeJobStatusEnum.SUCCEEDED);

        // 批次事实本身就说明了部分性：本次摄取被分成 64 + 36 两批，断点落在第二批（前一批完全合格）。
        // 早先 publishFirstRevisionHealthy 的那一批是 arrange 阶段的既有调用，故只看结尾两批与增量。
        assertThat(fixture.model.batchSizes()).endsWith(64, 36);
        assertThat(fixture.model.embedCalls()).isEqualTo(embedCallsBefore + 2);
        // 作业状态与文档可用性是两件事：这一轮失败之后，旧 active 依然原样在册。
        assertThat(fixture.store.document(first.documentId()).getActiveRevisionId()).isEqualTo(first.revisionId());
    }

    // -------------------------------------------------------------------------------------------------------------
    // 共享夹具
    // -------------------------------------------------------------------------------------------------------------

    /**
     * 中文说明：一条已「上传→认领→摄取→激活」完整走通的修订投影，作为过期租约与部分失败两个负例的旧 active 基线。
     * English summary: A revision that already completed upload, claim, ingestion and activation, used as the previous
     * active revision by the stale-lease and partial-embedding negatives.
     */
    private record Published(String documentId, String revisionId, int chunkCount) {
    }

    /**
     * 中文说明：一次上传落库后的三行主键，测试据此驱动认领与摄取，而不必去猜作业载荷的其它内部结构。
     * English summary: The three keys an upload commits, so a test can drive the claim and the ingestion without
     * guessing any further structure of the job payload.
     */
    private record Queued(String documentId, String revisionId, String jobId) {
    }

    /**
     * 中文说明：本层的被测面：脚本化仓储 + 脚本化模型端口 + 脚本化 RAG 抽取/切分 + 真实生产 Service、Strategy 与
     * 作业服务，全部直接 {@code new}；受控时钟由夹具独享，所以「此刻」在生产代码与断言里是同一个值，跨 120 秒租约
     * 不需要睡真实时间。{@code KnowledgeServiceImpl} 的别名解析与幂等记录两个协作者在本层的任何一条路径上都不该被
     * 触碰（它们只在 API-008/009 的知识库创建与替换里起作用），因此注入的是「一旦被调用就让测试失败」的动态代理，
     * 而不是一个会安静返回空值的假实现。
     * English summary: The face under test: the scripted store, model port and RAG extractor/chunker plus the real
     * production service, strategy and job service, all constructed directly and sharing one controlled clock, so "now"
     * is the same value inside production code and inside the assertions without sleeping through a 120-second lease. The
     * alias-resolution and idempotency-record collaborators of {@code KnowledgeServiceImpl} must not be touched on any
     * path this class exercises (they belong to knowledge base creation and replacement only), so what is injected is a
     * dynamic proxy that fails the test when called rather than a stub answering empty values quietly.
     */
    private final class Fixture {

        private final MutableClock clock = new MutableClock(BASE);

        private final KnowledgeProperties properties = new KnowledgeProperties();

        private final ScriptedKnowledgeStore store = new ScriptedKnowledgeStore(clock, properties);

        private final ScriptedModelClient model = new ScriptedModelClient();

        private final ScriptedRagExtractor extractor = new ScriptedRagExtractor();

        private final ObjectMapper json = new ObjectMapper();

        private final DocumentIngestionStrategy ingestion = new DocumentIngestionStrategy(
                store,
                model,
                extractor,
                new RagChunkingStrategyFactory(List.of(new ScriptedChunkingStrategy())),
                properties,
                json,
                clock);

        private final KnowledgeServiceImpl knowledgeService = new KnowledgeServiceImpl(
                store,
                new UnusedRetrievalPort(),
                properties,
                untouched(LlmConfigurationRepository.class),
                untouched(IdempotencyRepository.class),
                json,
                clock);

        private final KnowledgeJobServiceImpl jobs = new KnowledgeJobServiceImpl(
                store,
                registryOf(ingestion),
                properties,
                json,
                clock);

        private final String kbId;

        private Fixture() {
            this.kbId = store.insertBase(seedBase()).getId();
        }

        private String kbId() {
            return kbId;
        }

        private Duration lease() {
            return properties.getLease();
        }

        /** 中文说明：纯粹走一遍生产上传路径，不做任何补偿，供原子性与零写入断言使用。 English summary: The production upload path exactly as it is, without compensation, for the atomicity and zero-write assertions. */
        private KnowledgeDocumentVO upload(AdminActor actor, String fileName, String mediaType, byte[] content,
                String idempotencyKey) {
            return knowledgeService.uploadDocument(actor, kbId, KnowledgeUploadCommandDTO.builder()
                    .fileName(fileName)
                    .mediaType(mediaType)
                    .content(content)
                    .build(), idempotencyKey);
        }

        /**
         * 中文说明：走完生产上传，再只为作业行补上「本作业摄取哪一条修订」这一个键，返回三行主键。
         * 文档行、修订行（含原件字节、SHA-256、冻结空间/维度）与作业行全部由 {@code KnowledgeServiceImpl} 写出；
         * 唯一被测试补齐的是生产载荷缺失的 {@code revisionId} 键，因为 {@code DocumentIngestionStrategy} 的契约
         * 就是从这两个键里读来源修订（详见类注释上报的跨写者缺陷）。
         * English summary: Runs the production upload and then supplies only the one key the committed payload is
         * missing — which revision this job ingests — returning the three keys. The document row, the revision row (raw
         * bytes, SHA-256, frozen space and dimensions) and the job row are all written by {@code KnowledgeServiceImpl};
         * the only compensation is the absent {@code revisionId} key, because the strategy's contract reads the source
         * revision out of exactly those two keys (see the cross-writer defect recorded in the class note).
         */
        private Queued ingestibleUpload(String body, String intent, String documentId) {
            KnowledgeDocumentBO current = documentId == null ? null : store.document(documentId);
            KnowledgeDocumentVO uploaded = knowledgeService.uploadDocument(EDITOR, kbId,
                    KnowledgeUploadCommandDTO.builder()
                            .fileName(current == null ? "runbook.md" : current.getFileName())
                            .mediaType("text/markdown")
                            .content(bytes(body))
                            .documentId(documentId)
                            .expectedRevision(current == null ? null : current.getRevision())
                            .build(), intent);
            KnowledgeDocumentRevisionBO revision = store.newestRevisionOf(uploaded.getId());
            store.freezeSourceRevisionInPayload(uploaded.getLatestJobId(), revision.getId());
            return new Queued(uploaded.getId(), revision.getId(), uploaded.getLatestJobId());
        }

        /** 中文说明：领走本知识库里唯一一条可认领作业。/ Takes the only claimable job of this knowledge base. */
        private KnowledgeJobBO onlyClaimedJob() {
            List<KnowledgeJobBO> claimed = jobs.claimAvailable(properties.getClaimSize());
            assertThat(claimed).hasSize(1);
            return claimed.get(0);
        }

        private KnowledgeJobBO claim(String jobId) {
            return jobs.claimAvailable(properties.getWorkerConcurrency()).stream()
                    .filter(job -> job.getId().equals(jobId))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("the job was never claimed: " + jobId));
        }

        /**
         * 中文说明：把第一次上传完整摄取成功，得到一条活动修订，作为后续负例里「旧 active」的具体所指。
         * English summary: Ingests the first upload all the way to success, producing the active revision the negatives
         * then assert is preserved.
         */
        private Published publishFirstRevisionHealthy(String baselineBody) {
            Queued first = ingestibleUpload(baselineBody, "baseline-intent-00000001", null);
            store.leaseOwner("worker-a");
            KnowledgeJobBO claimed = claim(first.jobId());
            model.answerHealthily();
            KnowledgeJobBO outcome = ingestion.run(claimed, claimed.getLeaseToken());
            jobs.publishTerminal(outcome, claimed.getLeaseToken());
            assertThat(store.document(first.documentId()).getActiveRevisionId()).isEqualTo(first.revisionId());
            return new Published(first.documentId(), first.revisionId(),
                    store.chunksOf(first.revisionId()).size());
        }
    }

    /**
     * 中文说明：{@link KnowledgeRetrievalService} 是本 Step 提前声明的端口，实现属 Step 13，API-022 也不在本层证据
     * 范围内，所以这里注入一个「一旦被调用就让测试失败」的占位实现，而不是伪造一个能回答的检索器。
     * English summary: {@link KnowledgeRetrievalService} is a port this Step declares ahead of Step 13 and API-022 is
     * outside this layer's evidence, so the fixture injects a placeholder that fails the test when touched rather than a
     * fabricated answerer.
     */
    private static final class UnusedRetrievalPort implements KnowledgeRetrievalService {

        @Override
        public KnowledgeAnswerVO answer(AdminActor actor, String kbId, KnowledgeAnswerCommandDTO command) {
            throw new AssertionError("retrieval must not be reached by upload, lease or ingestion assertions");
        }
    }

    /**
     * 中文说明：注册表按 Step 12 的方式构造（只登记 {@code DOCUMENT_INGEST} 的 {@code EnumMap}），因为 Rule 9 规定
     * 类型到逻辑的唯一路由机制就是它；Wiki 生成属 Step 14，这里刻意不注册。
     * English summary: The registry is built the way Step 12 builds it (an {@code EnumMap} holding only
     * {@code DOCUMENT_INGEST}), because Rule 9 makes it the single routing mechanism; wiki generation is Step 14's.
     */
    private static Map<KnowledgeJobTypeEnum, KnowledgeJobStrategy> registryOf(DocumentIngestionStrategy ingestion) {
        Map<KnowledgeJobTypeEnum, KnowledgeJobStrategy> registry = new EnumMap<>(KnowledgeJobTypeEnum.class);
        registry.put(KnowledgeJobTypeEnum.DOCUMENT_INGEST, ingestion);
        return Collections.unmodifiableMap(registry);
    }

    /**
     * 中文说明：造一个「碰一下就失败」的端口实例，用于本层根本不该触达的协作者。之所以不写一个返回空值的假实现：
     * 空值会让生产代码走进某条静默分支，把「没触达」这件事变成一次无法察觉的假成功，而这里要的是相反的取向——
     * 真触达就让测试红。
     * English summary: Builds a port instance that fails the moment it is touched, for a collaborator this layer must
     * never reach. A stub answering null was rejected on purpose: null would send production code down some quiet branch
     * and turn "never reached" into an unnoticed fake success, whereas the opposite posture is what is being pinned here.
     */
    @SuppressWarnings("unchecked")
    private static <T> T untouched(Class<T> port) {
        return (T) Proxy.newProxyInstance(
                port.getClassLoader(),
                new Class<?>[] {port},
                (instance, method, arguments) -> {
                    throw new AssertionError(port.getSimpleName() + '.' + method.getName()
                            + " must not be reached by upload, lease or ingestion assertions");
                });
    }

    /**
     * 中文说明：{@link RagExtractionService} 的脚本化抽取：只做「把提交进来的字节按 UTF-8 解码」这一件事，
     * 因为本层要固定的是「索引内容确实来自上传的原件」，而 PDF/DOCX 解析器属 RAG starter 自己的证据范围。
     * English summary: The scripted {@link RagExtractionService}: it does exactly one thing, decoding the submitted bytes
     * as UTF-8, because what this layer pins is that indexed content really comes from the uploaded original, while PDF
     * and DOCX parsers belong to the RAG starter's own evidence.
     */
    private static final class ScriptedRagExtractor implements RagExtractionService {

        private int calls;

        private int calls() {
            return calls;
        }

        @Override
        public ExtractedDocumentBO extract(RagExtractionCommand command) {
            calls++;
            try {
                return new ExtractedDocumentBO(
                        new String(command.content().readAllBytes(), StandardCharsets.UTF_8),
                        command.fileName(),
                        command.mimeType(),
                        Map.of());
            } catch (IOException unreadable) {
                throw new UncheckedIOException("the scripted extractor reads an in-memory stream", unreadable);
            }
        }
    }

    /**
     * 中文说明：{@link RagChunkingStrategy} 的脚本化实现，注册为 {@code TOKEN}：按行确定性切分并丢掉空行，
     * 于是「分块数 == 原件的非空行数」成为一条可断言的事实，而块内容必须逐字来自原件。真实分词器属 RAG starter，
     * 本类不断言它的切分质量。
     * English summary: The scripted {@link RagChunkingStrategy} registered as {@code TOKEN}: a deterministic line split
     * that drops blanks, which turns "the chunk count equals the original's non-blank line count" into an assertable fact
     * and keeps every chunk a verbatim piece of the original. The real tokenizer belongs to the RAG starter and its
     * splitting quality is not what this class pins.
     */
    private static final class ScriptedChunkingStrategy implements RagChunkingStrategy {

        @Override
        public RagChunkingStrategyEnum strategy() {
            return RagChunkingStrategyEnum.TOKEN;
        }

        @Override
        public List<RagChunkBO> split(ExtractedDocumentBO document, RagChunkingConfigDTO config) {
            List<RagChunkBO> pieces = new ArrayList<>();
            for (String line : document.text().split("\n")) {
                if (!line.isBlank()) {
                    pieces.add(new RagChunkBO(pieces.size(), line, Map.of("title", String.valueOf(document.title()))));
                }
            }
            return List.copyOf(pieces);
        }
    }

    /** 中文说明：与 {@link ScriptedChunkingStrategy} 同口径的期望分块数：原件的非空行数。 English summary: The expected chunk count under the same rule as the scripted chunker: the original's non-blank lines. */
    private static int expectedChunks(String body) {
        int lines = 0;
        for (String line : body.split("\n")) {
            if (!line.isBlank()) {
                lines++;
            }
        }
        return lines;
    }

    /**
     * 中文说明：受控 UTC 时钟；测试用 {@link #advance(Duration)} 跨过 120 秒租约而不睡真实时间，生产类与脚本化
     * 仓储共享同一实例，因此「已过期」在两边是同一件事。
     * English summary: A controlled UTC clock: {@link #advance(Duration)} crosses the 120-second lease without sleeping,
     * and because the production classes and the scripted store share one instance, "expired" is the same fact on both
     * sides.
     */
    private static final class MutableClock extends Clock {

        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        private void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    /**
     * 中文说明：{@link KnowledgeModelClientService} 的脚本化替身，只承担本层需要的事实：记录每一批的条数与请求维度
     * （据此断言「按 ≤64 分批」与「沿用知识库冻结维度」），并按脚本地理答——全部合格向量、从指定某一批起少给一条
     * 向量的部分结果（默认状态是 {@code 503 KNOWLEDGE_MODEL_UNAVAILABLE}，好让不跑摄取的用例一旦被触达就立刻显形）。
     * 合格向量是「长度等于维度、全部有限且非全零」的真数组，因为 Spec 要求发布前逐条校验这些性质；它不猜真实模型
     * 输出，也不冒充网络路径——模型协议本身属 Step 11 与 engine 的证据范围。
     * English summary: The scripted {@link KnowledgeModelClientService}: it records every batch size and requested
     * dimension (which is how "batched within 64" and "the frozen dimensions are reused" get asserted) and answers as
     * scripted — fully conforming vectors, or one vector short from a named batch on — with the resting state being
     * {@code 503 KNOWLEDGE_MODEL_UNAVAILABLE} so any case that was not supposed to reach the model shows up at once. A
     * conforming vector is a real array of exactly {@code dimensions} finite, not-all-zero components, because the Spec
     * requires those properties to be verified before publishing. It neither guesses real model output nor stands in for
     * the network path, which belongs to Step 11 and the engine's evidence.
     */
    private static final class ScriptedModelClient implements KnowledgeModelClientService {

        private enum Answer {
            HEALTHY, UNAVAILABLE
        }

        private final List<Integer> batchSizes = new ArrayList<>();

        private final List<Integer> requestedDimensions = new ArrayList<>();

        private Answer answer = Answer.UNAVAILABLE;

        /** 中文说明：从第几批（1 起）开始少给一条向量；负数表示永不退化。序号以 {@link #batchBaseline} 为原点， 所以「第 2 批」指的总是本次摄取的第 2 批，而不是整个测试累计的第 2 次调用。 English summary: Which one-based batch starts answering one vector short; negative means the script never degrades. The index is counted from {@link #batchBaseline}, so "batch 2" is always the second batch of the run being armed rather than the test's second-ever call. */
        private int failFromBatch = -1;

        /** 中文说明：布防时刻已发生的批次数，用来把批号换算成「本次摄取内的第几批」。 English summary: How many batches had already happened when the script was armed, turning a batch number into one relative to the run under test. */
        private int batchBaseline;

        private int embedCalls;

        private int generateCalls;

        private void answerHealthily() {
            answer = Answer.HEALTHY;
            failFromBatch = -1;
        }

        /** 中文说明：让本次摄取的前 {@code batch - 1} 批完全合格、第 {@code batch} 批起少一条向量，用来造「部分成功」。 English summary: Keeps the first {@code batch - 1} batches of this run conforming and drops one vector from batch {@code batch} on, which is how a genuinely partial success is scripted. */
        private void answerFailingFromBatch(int batch) {
            answer = Answer.HEALTHY;
            batchBaseline = batchSizes.size();
            failFromBatch = batch;
        }

        private int embedCalls() {
            return embedCalls;
        }

        private int generateCalls() {
            return generateCalls;
        }

        private List<Integer> batchSizes() {
            return List.copyOf(batchSizes);
        }

        private List<Integer> requestedDimensions() {
            return List.copyOf(requestedDimensions);
        }

        @Override
        public List<float[]> embed(String kbId, List<String> texts, int expectedDimensions) {
            embedCalls++;
            batchSizes.add(texts.size());
            requestedDimensions.add(expectedDimensions);
            if (answer == Answer.UNAVAILABLE) {
                throw new CommonException(503, "KNOWLEDGE_MODEL_UNAVAILABLE", "the local embedding alias is down");
            }
            int vectors = answer == Answer.HEALTHY && failFromBatch == batchSizes.size() - batchBaseline
                    ? texts.size() - 1
                    : texts.size();
            List<float[]> result = new ArrayList<>();
            for (int index = 0; index < vectors; index++) {
                result.add(vectorOf(expectedDimensions, index));
            }
            return result;
        }

        @Override
        public String generate(String kbId, String prompt, int maxTokens) {
            generateCalls++;
            throw new CommonException(503, "KNOWLEDGE_MODEL_UNAVAILABLE", "generation is not part of ingestion");
        }

        private static float[] vectorOf(int dimensions, int salt) {
            float[] vector = new float[dimensions];
            for (int index = 0; index < dimensions; index++) {
                vector[index] = 0.25f + ((index + salt) % 7) * 0.1f;
            }
            return vector;
        }
    }

    /**
     * 中文说明：{@link KnowledgeRepository} 的脚本化替身，也是本类的核心：它不「记录调用然后一律成功」，而是按
     * Spec §11.2.7 与 Step 12 §2 的谓词<b>真实判定</b>每一次写：
     * <ul>
     *   <li>{@link #claimNext(int)} 只领取「{@code QUEUED}/{@code RETRY_WAIT} 且 {@code next_attempt_at <= now}」或
     *       「{@code RUNNING} 且租约已到期」的行，按 {@code next_attempt_at, id} 升序、至多 {@code slots} 行，每次认领把
     *       令牌推进为观察值 + 1——这正是接管与「迟到写回必然 0 行」的共同来源；</li>
     *   <li>{@link #heartbeat(String, long, Instant)} 与 {@link #finish(KnowledgeJobBO, long)} 的条件是
     *       {@code id + lease_token + status = 'RUNNING' + lease_expires_at > now}，不成立即返回 {@code false}；</li>
     *   <li>{@link #activateRevision(String, String, String, long)} 以 {@code kb_id + 文档 + revision =
     *       expectedRevision} 为条件，命中才换指针并推进 revision，并把「到达 CAS 的次数」与「命中次数」分开计数，
     *       使「根本没去激活」与「激活没命中」在断言里可区分；</li>
     *   <li>{@link #stageChunks(String, String, List)} 先清掉该修订此前的暂存行再逐条插入，行数累加进
     *       {@link #stagedInserts()}，并按 pgvector 的口径拒绝长度不符、非有限或全零的向量。</li>
     * </ul>
     * 读取一律返回副本，因此手上还握着旧载体的 worker 不可能靠改自己那份对象伪造库里事实——这正是真实 JDBC 围栏的行为。
     * 租户与审计列在真实路径上由 starter 守卫补齐，本替身用「看不见的行一律读作不存在」表达同一效果，不复现 SQL。
     * English summary: The scripted {@link KnowledgeRepository} and the heart of this class: rather than recording calls
     * and always succeeding, it genuinely adjudicates every write under the predicates of Spec §11.2.7 and Step 12 §2 —
     * {@link #claimNext(int)} takes only {@code QUEUED}/{@code RETRY_WAIT} rows that are due or {@code RUNNING} rows
     * whose lease lapsed, ordered by {@code next_attempt_at, id}, bounded by {@code slots}, and advances the token to
     * observed + 1 on every claim (the shared source of both the take-over and the "a late write-back must hit zero
     * rows"); {@link #heartbeat(String, long, Instant)} and {@link #finish(KnowledgeJobBO, long)} are conditioned on
     * {@code id + lease_token + status = 'RUNNING' + lease_expires_at > now} and return {@code false} otherwise;
     * {@link #activateRevision(String, String, String, long)} moves the pointer only under {@code kb_id + document +
     * revision = expectedRevision} and counts arrivals separately from hits, so "never tried to activate" stays
     * distinguishable from "tried and missed"; {@link #stageChunks(String, String, List)} clears the revision's earlier
     * staged rows, inserts the rest while tallying {@link #stagedInserts()}, and refuses vectors whose length is wrong
     * or whose components are non-finite or all zero, the way pgvector would. Every read returns a copy, so a worker
     * still holding an old carrier cannot fake database truth by mutating its own object, which is exactly how the real
     * JDBC fencing behaves. Tenancy and the audit columns are stamped by the starter guard on the real path; this
     * substitute expresses the same effect as "a row the guard cannot see reads as absent" and does not reproduce SQL.
     */
    private static final class ScriptedKnowledgeStore implements KnowledgeRepository {

        /**
         * 中文说明：存储层约束被违反时的类型（父行缺失、向量不合格、幂等意图唯一键竞争），对应真库的 FK/CHECK/UNIQUE。
         * English summary: Raised when a storage-level constraint breaks — a missing parent row, an unusable vector, a
         * clashing idempotency intent — mirroring the real FK, CHECK and UNIQUE constraints.
         */
        private static final class ScriptedConstraintViolation extends RuntimeException {

            private ScriptedConstraintViolation(String message) {
                super(message);
            }
        }

        private final MutableClock clock;

        private final KnowledgeProperties properties;

        private final Map<String, KnowledgeBaseBO> bases = new LinkedHashMap<>();

        private final Map<String, KnowledgeDocumentBO> documents = new LinkedHashMap<>();

        private final Map<String, KnowledgeDocumentRevisionBO> revisions = new LinkedHashMap<>();

        private final Map<String, List<KnowledgeChunkBO>> chunks = new LinkedHashMap<>();

        private final Map<String, KnowledgeJobBO> jobs = new LinkedHashMap<>();

        private final List<String> events = new ArrayList<>();

        private long sequence = 71_000L;

        private long leaseTakeovers;

        private long heartbeatHits;

        private long heartbeatRejections;

        private long finishAttempts;

        private long finishHits;

        private long activationAttempts;

        private long activations;

        private long stagedInserts;

        private String leaseOwnerTag = "worker-unlabelled";

        private ScriptedKnowledgeStore(MutableClock clock, KnowledgeProperties properties) {
            this.clock = clock;
            this.properties = properties;
        }

        private void leaseOwner(String leaseOwnerTag) {
            this.leaseOwnerTag = leaseOwnerTag;
        }

        private List<String> eventLog() {
            return List.copyOf(events);
        }

        private int firstEventIndex(String prefix) {
            for (int index = 0; index < events.size(); index++) {
                if (events.get(index).startsWith(prefix)) {
                    return index;
                }
            }
            return -1;
        }

        private int lastEventIndex(String prefix) {
            for (int index = events.size() - 1; index >= 0; index--) {
                if (events.get(index).startsWith(prefix)) {
                    return index;
                }
            }
            return -1;
        }

        private int documentCount() {
            return documents.size();
        }

        private int revisionCount() {
            return revisions.size();
        }

        private int jobCount() {
            return jobs.size();
        }

        private int unclaimedJobCount() {
            return (int) jobs.values().stream().filter(job -> job.getLeaseToken() == 0L).count();
        }

        private KnowledgeDocumentBO onlyDocument() {
            return copy(one(List.copyOf(documents.values())));
        }

        private KnowledgeDocumentRevisionBO onlyRevision() {
            return copy(one(List.copyOf(revisions.values())));
        }

        private KnowledgeJobBO onlyJob() {
            return copy(one(List.copyOf(jobs.values())));
        }

        private KnowledgeDocumentBO document(String id) {
            return documents.containsKey(id) ? copy(documents.get(id)) : null;
        }

        private KnowledgeDocumentRevisionBO revision(String id) {
            return revisions.containsKey(id) ? copy(revisions.get(id)) : null;
        }

        private KnowledgeJobBO job(String id) {
            return jobs.containsKey(id) ? copy(jobs.get(id)) : null;
        }

        private List<KnowledgeChunkBO> chunksOf(String revisionId) {
            return chunks.getOrDefault(revisionId, List.of()).stream()
                    .map(ScriptedKnowledgeStore::copy)
                    .sorted(Comparator.comparing(KnowledgeChunkBO::getChunkIndex))
                    .toList();
        }

        /** 中文说明：该文档最新提交的一条修订（雪花 id 单调，故按 id 取最大），测试据此认出刚上传的修订。 English summary: The newest revision committed for a document — snowflake ids are monotonic, so the greatest id is the freshest — which is how a test recognises the revision it just uploaded. */
        private KnowledgeDocumentRevisionBO newestRevisionOf(String documentId) {
            return revisions.values().stream()
                    .filter(revision -> revision.getDocumentId().equals(documentId))
                    .max(Comparator.comparing(KnowledgeDocumentRevisionBO::getId))
                    .map(ScriptedKnowledgeStore::copy)
                    .orElseThrow(() -> new AssertionError("no revision was committed for " + documentId));
        }

        /**
         * 中文说明：只往已提交的作业行 payload 里补写「本作业摄取的修订 id」，不新建行、不改动任何租约/状态/尝试列。
         * 这是对本 Step 已知缺陷的输入补偿：{@code KnowledgeServiceImpl.ingestPayload} 没写出该键，而
         * {@code DocumentIngestionStrategy} 的契约要求从 payload 读到它（类注释已上报，改的应当是生产代码）。
         * English summary: Adds only the "revision this job ingests" key to the already committed job row's payload,
         * touching no new row and no lease, status or attempt column. It is an input compensation for a known defect of
         * this Step: {@code KnowledgeServiceImpl.ingestPayload} never writes that key while the strategy's contract reads
         * the source revision out of the payload (reported in the class note; production code is what should change).
         */
        private void freezeSourceRevisionInPayload(String jobId, String revisionId) {
            KnowledgeJobBO row = jobId == null ? null : jobs.get(jobId);
            if (row == null || !(row.getPayload() instanceof ObjectNode payload)) {
                throw new AssertionError("the committed job row carries no object payload to complete: " + jobId);
            }
            payload.put("revisionId", revisionId);
            events.add("UPDATE:jobpayload:" + row.getId());
        }

        private long leaseTakeovers() {
            return leaseTakeovers;
        }

        private long heartbeatHits() {
            return heartbeatHits;
        }

        private long heartbeatRejections() {
            return heartbeatRejections;
        }

        private long finishAttempts() {
            return finishAttempts;
        }

        private long finishHits() {
            return finishHits;
        }

        private long activationAttempts() {
            return activationAttempts;
        }

        private long activations() {
            return activations;
        }

        private long stagedInserts() {
            return stagedInserts;
        }

        // ------------------------------------------------------------------------------------------- 基与资料

        @Override
        public Optional<KnowledgeBaseBO> findBase(String kbId) {
            return Optional.ofNullable(bases.get(kbId)).map(ScriptedKnowledgeStore::copy);
        }

        @Override
        public List<KnowledgeBaseBO> listBasesOfActor(String actorId, int page, int size) {
            return page(bases.values().stream()
                    .filter(base -> visibleTo(base, actorId))
                    .map(ScriptedKnowledgeStore::copy)
                    .toList(), page, size);
        }

        @Override
        public long countBasesOfActor(String actorId) {
            return bases.values().stream().filter(base -> visibleTo(base, actorId)).count();
        }

        @Override
        public KnowledgeBaseBO insertBase(KnowledgeBaseBO base) {
            KnowledgeBaseBO row = copy(base);
            stampNew(row.getId(), row::setId);
            row.setRevision(1L).setCreatedAt(clock.instant()).setUpdatedAt(clock.instant());
            bases.put(row.getId(), row);
            events.add("INSERT:base:" + row.getId());
            return copy(row);
        }

        @Override
        public boolean replaceBase(KnowledgeBaseBO base, long expectedRevision) {
            KnowledgeBaseBO row = bases.get(base.getId());
            if (row == null || row.getRevision() != expectedRevision) {
                return false;
            }
            row.setName(base.getName())
                    .setDescription(base.getDescription())
                    .setMembers(base.getMembers())
                    .setEgressPolicy(base.getEgressPolicy())
                    .setChatModel(base.getChatModel())
                    .setEmbeddingModel(base.getEmbeddingModel())
                    .setRevision(expectedRevision + 1)
                    .setUpdatedAt(clock.instant());
            events.add("UPDATE:base:" + row.getId());
            return true;
        }

        @Override
        public Optional<KnowledgeDocumentBO> findDocument(String kbId, String documentId) {
            KnowledgeDocumentBO row = documents.get(documentId);
            return row == null || !row.getKbId().equals(kbId) ? Optional.empty() : Optional.of(copy(row));
        }

        @Override
        public List<KnowledgeDocumentBO> listDocuments(String kbId, int page, int size) {
            return page(documents.values().stream()
                    .filter(document -> document.getKbId().equals(kbId))
                    .map(ScriptedKnowledgeStore::copy)
                    .toList(), page, size);
        }

        @Override
        public long countDocuments(String kbId) {
            return documents.values().stream().filter(document -> document.getKbId().equals(kbId)).count();
        }

        @Override
        public KnowledgeDocumentBO insertDocument(KnowledgeDocumentBO document) {
            if (!bases.containsKey(document.getKbId())) {
                throw new ScriptedConstraintViolation("fk gateway_knowledge_document.kb_id");
            }
            KnowledgeDocumentBO row = copy(document);
            stampNew(row.getId(), row::setId);
            row.setRevision(1L).setCreatedAt(clock.instant()).setUpdatedAt(clock.instant());
            documents.put(row.getId(), row);
            events.add("INSERT:document:" + row.getId());
            return copy(row);
        }

        @Override
        public boolean updateDocument(KnowledgeDocumentBO document, long expectedRevision) {
            KnowledgeDocumentBO row = documents.get(document.getId());
            if (row == null || row.getRevision() != expectedRevision) {
                events.add("REJECT:updateDocument:" + document.getId());
                return false;
            }
            row.setFileName(document.getFileName())
                    .setActiveRevisionId(document.getActiveRevisionId())
                    .setLatestJobId(document.getLatestJobId())
                    .setRevision(expectedRevision + 1)
                    .setUpdatedAt(clock.instant());
            events.add("UPDATE:document:" + row.getId());
            return true;
        }

        /**
         * 中文说明：带版本的逻辑删除：只把文档行变为不可见（findDocument/listDocuments 从此查不到），
         * 修订、分块与作业行原样留在其他表里，与真实 SQL 的 {@code SET deleted_at} 语义一致。
         * English summary: The versioned logic delete: only the document row becomes invisible (findDocument/listDocuments miss it
         * from now on) while its revisions, chunks and jobs stay in the other tables, matching the real {@code SET deleted_at}.
         */
        @Override
        public boolean softDeleteDocument(String kbId, String documentId, long expectedRevision) {
            KnowledgeDocumentBO row = documents.get(documentId);
            if (row == null || !row.getKbId().equals(kbId) || row.getRevision() != expectedRevision) {
                events.add("REJECT:softDeleteDocument:" + documentId);
                return false;
            }
            documents.remove(documentId);
            events.add("SOFT_DELETE:document:" + documentId);
            return true;
        }

        @Override
        public boolean activateRevision(String kbId, String documentId, String activeRevisionId,
                long expectedRevision) {
            activationAttempts++;
            KnowledgeDocumentBO row = documents.get(documentId);
            KnowledgeDocumentRevisionBO target = revisions.get(activeRevisionId);
            if (row == null || !row.getKbId().equals(kbId) || target == null
                    || !documentId.equals(target.getDocumentId()) || !target.getKbId().equals(kbId)
                    || row.getRevision() != expectedRevision) {
                events.add("ACTIVATE:miss:" + documentId + ":" + activeRevisionId);
                return false;
            }
            row.setActiveRevisionId(activeRevisionId).setRevision(expectedRevision + 1).setUpdatedAt(clock.instant());
            activations++;
            events.add("ACTIVATE:hit:" + documentId + ":" + activeRevisionId);
            return true;
        }

        // ------------------------------------------------------------------------------------------- 修订与分块

        @Override
        public Optional<KnowledgeDocumentRevisionBO> findRevision(String kbId, String revisionId) {
            KnowledgeDocumentRevisionBO row = revisions.get(revisionId);
            return row == null || !row.getKbId().equals(kbId) ? Optional.empty() : Optional.of(copy(row));
        }

        @Override
        public List<KnowledgeDocumentRevisionBO> listRevisions(String kbId, String documentId, int page, int size) {
            return page(revisions.values().stream()
                    .filter(revision -> revision.getKbId().equals(kbId) && revision.getDocumentId().equals(documentId))
                    .map(ScriptedKnowledgeStore::copy)
                    .toList(), page, size);
        }

        @Override
        public KnowledgeDocumentRevisionBO insertRevision(KnowledgeDocumentRevisionBO revision) {
            if (!documents.containsKey(revision.getDocumentId())) {
                throw new ScriptedConstraintViolation("fk gateway_knowledge_revision.document_id");
            }
            KnowledgeDocumentRevisionBO row = copy(revision);
            stampNew(row.getId(), row::setId);
            row.setRevision(1L).setCreatedAt(clock.instant()).setUpdatedAt(clock.instant());
            revisions.put(row.getId(), row);
            events.add("INSERT:revision:" + row.getId());
            return copy(row);
        }

        @Override
        public boolean updateRevision(KnowledgeDocumentRevisionBO revision, long expectedRevision) {
            KnowledgeDocumentRevisionBO row = revisions.get(revision.getId());
            if (row == null || row.getRevision() != expectedRevision) {
                events.add("REJECT:updateRevision:" + revision.getId());
                return false;
            }
            row.setExtractedText(revision.getExtractedText())
                    .setStatus(revision.getStatus())
                    .setChunkCount(revision.getChunkCount())
                    .setRevision(expectedRevision + 1)
                    .setUpdatedAt(clock.instant());
            events.add("UPDATE:revision:" + row.getId() + ":" + row.getStatus());
            return true;
        }

        @Override
        public int stageChunks(String kbId, String revisionId, List<KnowledgeChunkBO> staged) {
            KnowledgeDocumentRevisionBO revision = revisions.get(revisionId);
            if (revision == null || !revision.getKbId().equals(kbId)) {
                throw new ScriptedConstraintViolation("fk gateway_knowledge_chunk.revision_id");
            }
            List<KnowledgeChunkBO> rows = new ArrayList<>();
            for (KnowledgeChunkBO chunk : staged) {
                KnowledgeChunkBO row = copy(chunk);
                validateVector(revision, row);
                stampNew(row.getId(), row::setId);
                row.setRevision(1L).setCreatedAt(clock.instant()).setUpdatedAt(clock.instant());
                rows.add(row);
            }
            chunks.put(revisionId, rows);
            stagedInserts += rows.size();
            events.add("STAGE:" + revisionId + ":" + rows.size());
            return rows.size();
        }

        @Override
        public List<KnowledgeChunkBO> listChunksOfRevision(String revisionId) {
            return chunksOf(revisionId);
        }

        // ------------------------------------------------------------------------------------------- 作业与租约

        @Override
        public Optional<KnowledgeJobBO> findJob(String jobId) {
            return Optional.ofNullable(jobs.get(jobId)).map(ScriptedKnowledgeStore::copy);
        }

        @Override
        public Optional<KnowledgeJobBO> findJobByIntent(String kbId, String actorId, KnowledgeJobTypeEnum type,
                String idempotencyKey) {
            return jobs.values().stream()
                    .filter(job -> job.getKbId().equals(kbId) && job.getActorId().equals(actorId)
                            && job.getType() == type && job.getIdempotencyKey().equals(idempotencyKey))
                    .findFirst()
                    .map(ScriptedKnowledgeStore::copy);
        }

        @Override
        public KnowledgeJobBO insertJob(KnowledgeJobBO job) {
            if (!bases.containsKey(job.getKbId())) {
                throw new ScriptedConstraintViolation("fk gateway_knowledge_job.kb_id");
            }
            boolean clash = jobs.values().stream().anyMatch(row -> row.getKbId().equals(job.getKbId())
                    && row.getActorId().equals(job.getActorId()) && row.getType() == job.getType()
                    && row.getIdempotencyKey().equals(job.getIdempotencyKey()));
            if (clash) {
                throw new ScriptedConstraintViolation("uq_knowledge_job_intent");
            }
            KnowledgeJobBO row = copy(job);
            stampNew(row.getId(), row::setId);
            row.setRevision(1L).setCreatedAt(clock.instant()).setUpdatedAt(clock.instant());
            jobs.put(row.getId(), row);
            events.add("INSERT:job:" + row.getId());
            return copy(row);
        }

        @Override
        public boolean updateJob(KnowledgeJobBO job, long expectedRevision) {
            KnowledgeJobBO row = jobs.get(job.getId());
            if (row == null || row.getRevision() != expectedRevision) {
                events.add("REJECT:updateJob:" + job.getId());
                return false;
            }
            row.setStatus(job.getStatus())
                    .setStage(job.getStage())
                    .setAttempt(job.getAttempt())
                    .setNextAttemptAt(job.getNextAttemptAt())
                    .setErrorCode(job.getErrorCode())
                    .setResult(job.getResult())
                    .setRevision(expectedRevision + 1)
                    .setUpdatedAt(clock.instant());
            events.add("UPDATE:job:" + row.getId() + ":" + row.getStatus());
            return true;
        }

        @Override
        public List<KnowledgeJobBO> listJobs(String kbId, KnowledgeJobStatusEnum status, int page, int size) {
            return page(jobs.values().stream()
                    .filter(job -> job.getKbId().equals(kbId))
                    .filter(job -> status == null || job.getStatus() == status)
                    .map(ScriptedKnowledgeStore::copy)
                    .toList(), page, size);
        }

        @Override
        public long countJobs(String kbId, KnowledgeJobStatusEnum status) {
            return jobs.values().stream()
                    .filter(job -> job.getKbId().equals(kbId))
                    .filter(job -> status == null || job.getStatus() == status)
                    .count();
        }

        @Override
        public List<KnowledgeJobBO> claimNext(int slots) {
            Instant now = clock.instant();
            List<KnowledgeJobBO> claimed = new ArrayList<>();
            jobs.values().stream()
                    .filter(job -> isClaimable(job, now))
                    .sorted(Comparator.comparing(KnowledgeJobBO::getNextAttemptAt).thenComparing(KnowledgeJobBO::getId))
                    .limit(slots)
                    .forEach(row -> {
                        if (row.getStatus() == KnowledgeJobStatusEnum.RUNNING) {
                            leaseTakeovers++;
                        }
                        row.setStatus(KnowledgeJobStatusEnum.RUNNING)
                                .setLeaseOwner(leaseOwnerTag)
                                .setLeaseToken(row.getLeaseToken() + 1)
                                .setLeaseExpiresAt(now.plus(properties.getLease()))
                                .setRevision(row.getRevision() + 1)
                                .setUpdatedAt(now);
                        events.add("CLAIM:" + row.getId() + ":" + row.getLeaseToken() + ":" + leaseOwnerTag);
                        claimed.add(copy(row));
                    });
            return claimed;
        }

        @Override
        public boolean heartbeat(String jobId, long leaseToken, Instant leaseExpiresAt) {
            KnowledgeJobBO row = jobs.get(jobId);
            if (row == null || !holdsLease(row, leaseToken)) {
                heartbeatRejections++;
                events.add("HEARTBEAT:miss:" + jobId + ":" + leaseToken);
                return false;
            }
            row.setLeaseExpiresAt(leaseExpiresAt).setUpdatedAt(clock.instant());
            heartbeatHits++;
            events.add("HEARTBEAT:hit:" + jobId + ":" + leaseToken);
            return true;
        }

        @Override
        public boolean finish(KnowledgeJobBO job, long leaseToken) {
            finishAttempts++;
            KnowledgeJobBO row = jobs.get(job.getId());
            if (row == null || !holdsLease(row, leaseToken) || !isConclusion(job.getStatus())) {
                events.add("FINISH:miss:" + job.getId() + ":" + leaseToken);
                return false;
            }
            row.setStatus(job.getStatus())
                    .setStage(job.getStage())
                    .setErrorCode(job.getErrorCode())
                    .setResult(job.getResult())
                    .setLeaseOwner(null)
                    .setLeaseExpiresAt(null)
                    .setRevision(row.getRevision() + 1)
                    .setUpdatedAt(clock.instant());
            finishHits++;
            events.add("FINISH:hit:" + row.getId() + ":" + row.getStatus());
            return true;
        }

        // ------------------------------------------------------------------------------------------- 谓词与工具

        private boolean isClaimable(KnowledgeJobBO row, Instant now) {
            if (row.getStatus() == KnowledgeJobStatusEnum.QUEUED || row.getStatus() == KnowledgeJobStatusEnum.RETRY_WAIT) {
                return !row.getNextAttemptAt().isAfter(now);
            }
            return row.getStatus() == KnowledgeJobStatusEnum.RUNNING
                    && row.getLeaseExpiresAt() != null
                    && !row.getLeaseExpiresAt().isAfter(now);
        }

        /**
         * 中文说明：具名语句 {@code heartbeatJob}/{@code finishJob} 的 WHERE 谓词本体：令牌被推进过、状态已离开
         * RUNNING、或到期时刻已不大于 now，任何一种都意味着 0 行。
         * English summary: The WHERE body of the named {@code heartbeatJob}/{@code finishJob} statements: an advanced
         * token, a status that already left RUNNING, or an expiry no longer greater than now each mean zero rows.
         */
        private boolean holdsLease(KnowledgeJobBO row, long leaseToken) {
            return row.getLeaseToken() == leaseToken
                    && row.getStatus() == KnowledgeJobStatusEnum.RUNNING
                    && row.getLeaseExpiresAt() != null
                    && row.getLeaseExpiresAt().isAfter(clock.instant());
        }

        /**
         * 中文说明：{@code finishJob} 只接受「一次执行得出的结论」：null 与 RUNNING 都不算结论——把一行留在
         * RUNNING 的写回等于什么都没发生，真实语义里这种写回不该存在。终态（SUCCEEDED/FAILED/STALE/CANCELLED）与
         * 可重试排程（RETRY_WAIT/QUEUED）都是合法结论。
         * English summary: {@code finishJob} accepts only a conclusion produced by one execution: neither null nor
         * RUNNING qualifies, since a write-back that leaves the row RUNNING says nothing happened. The terminal states
         * (SUCCEEDED/FAILED/STALE/CANCELLED) and a retryable reschedule (RETRY_WAIT/QUEUED) are all valid conclusions.
         */
        private static boolean isConclusion(KnowledgeJobStatusEnum status) {
            return status != null && status != KnowledgeJobStatusEnum.RUNNING;
        }

        /**
         * 中文说明：pgvector 与模型端口合同在存储侧的表达：向量长度必须等于冻结维度、全部有限且不能全零，
         * 否则这一行在真库里根本写不进去；嵌入空间也必须与该修订一致。
         * English summary: The storage-side form of the pgvector plus model-port contract: a vector must be exactly the
         * frozen dimensions long, fully finite and not all zero, or the row could never have been written at all, and its
         * embedding space must agree with the revision's.
         */
        private void validateVector(KnowledgeDocumentRevisionBO revision, KnowledgeChunkBO chunk) {
            if (chunk.getEmbedding() == null || chunk.getEmbedding().length != revision.getDimensions().intValue()
                    || !revision.getEmbeddingSpaceId().equals(chunk.getEmbeddingSpaceId())
                    || !revision.getDimensions().equals(chunk.getDimensions())) {
                throw new ScriptedConstraintViolation("chunk vector disagrees with the frozen space");
            }
            if (chunk.getRevisionId() == null || !chunk.getRevisionId().equals(revision.getId())) {
                throw new ScriptedConstraintViolation("fk gateway_knowledge_chunk.revision_id");
            }
            boolean nonZero = false;
            for (float value : chunk.getEmbedding()) {
                if (!Float.isFinite(value)) {
                    throw new ScriptedConstraintViolation("pgvector rejects non-finite components");
                }
                nonZero |= value != 0f;
            }
            if (!nonZero) {
                throw new ScriptedConstraintViolation("pgvector rejects an all-zero vector");
            }
        }

        private static boolean visibleTo(KnowledgeBaseBO base, String actorId) {
            return base.getOwnerActorId().equals(actorId) || base.getMembers() != null && base.getMembers().stream()
                    .anyMatch(member -> member.getActorId().equals(actorId));
        }

        private void stampNew(String givenId, Consumer<String> idSetter) {
            if (givenId == null || givenId.isBlank()) {
                idSetter.accept(String.valueOf(++sequence));
            }
        }

        /** 中文说明：读出「恰好一行」，多一行或少一行都直接让测试失败——这些读法本身就是基数断言。 English summary: Reads exactly one row, failing the test on either more or fewer, so these readers are themselves cardinality assertions. */
        private static <T> T one(List<T> rows) {
            if (rows.size() != 1) {
                throw new AssertionError("expected exactly one row but found " + rows.size());
            }
            return rows.get(0);
        }

        private static <T> List<T> page(List<T> rows, int page, int size) {
            int from = Math.min((page - 1) * size, rows.size());
            return List.copyOf(rows.subList(from, Math.min(from + size, rows.size())));
        }

        private static KnowledgeBaseBO copy(KnowledgeBaseBO row) {
            return KnowledgeBaseBO.builder()
                    .id(row.getId())
                    .name(row.getName())
                    .description(row.getDescription())
                    .ownerActorId(row.getOwnerActorId())
                    .members(row.getMembers())
                    .egressPolicy(row.getEgressPolicy())
                    .chatModel(row.getChatModel())
                    .embeddingModel(row.getEmbeddingModel())
                    .embeddingSpaceId(row.getEmbeddingSpaceId())
                    .dimensions(row.getDimensions())
                    .revision(row.getRevision())
                    .createdAt(row.getCreatedAt())
                    .updatedAt(row.getUpdatedAt())
                    .build();
        }

        private static KnowledgeDocumentBO copy(KnowledgeDocumentBO row) {
            return KnowledgeDocumentBO.builder()
                    .id(row.getId())
                    .kbId(row.getKbId())
                    .fileName(row.getFileName())
                    .activeRevisionId(row.getActiveRevisionId())
                    .latestJobId(row.getLatestJobId())
                    .revision(row.getRevision())
                    .createdAt(row.getCreatedAt())
                    .updatedAt(row.getUpdatedAt())
                    .build();
        }

        private static KnowledgeDocumentRevisionBO copy(KnowledgeDocumentRevisionBO row) {
            return KnowledgeDocumentRevisionBO.builder()
                    .id(row.getId())
                    .kbId(row.getKbId())
                    .documentId(row.getDocumentId())
                    .fileName(row.getFileName())
                    .mediaType(row.getMediaType())
                    .rawBytes(row.getRawBytes())
                    .byteCount(row.getByteCount())
                    .contentHash(row.getContentHash())
                    .extractedText(row.getExtractedText())
                    .embeddingSpaceId(row.getEmbeddingSpaceId())
                    .dimensions(row.getDimensions())
                    .chunkingConfig(row.getChunkingConfig())
                    .status(row.getStatus())
                    .chunkCount(row.getChunkCount())
                    .revision(row.getRevision())
                    .createdAt(row.getCreatedAt())
                    .updatedAt(row.getUpdatedAt())
                    .build();
        }

        private static KnowledgeChunkBO copy(KnowledgeChunkBO row) {
            return KnowledgeChunkBO.builder()
                    .id(row.getId())
                    .kbId(row.getKbId())
                    .revisionId(row.getRevisionId())
                    .chunkIndex(row.getChunkIndex())
                    .content(row.getContent())
                    .metadata(row.getMetadata())
                    .contentHash(row.getContentHash())
                    .embeddingSpaceId(row.getEmbeddingSpaceId())
                    .dimensions(row.getDimensions())
                    .embedding(row.getEmbedding())
                    .revision(row.getRevision())
                    .createdAt(row.getCreatedAt())
                    .updatedAt(row.getUpdatedAt())
                    .build();
        }

        private static KnowledgeJobBO copy(KnowledgeJobBO row) {
            return KnowledgeJobBO.builder()
                    .id(row.getId())
                    .kbId(row.getKbId())
                    .type(row.getType())
                    .resourceId(row.getResourceId())
                    .actorId(row.getActorId())
                    .payload(row.getPayload())
                    .idempotencyKey(row.getIdempotencyKey())
                    .requestHash(row.getRequestHash())
                    .status(row.getStatus())
                    .stage(row.getStage())
                    .attempt(row.getAttempt())
                    .nextAttemptAt(row.getNextAttemptAt())
                    .leaseOwner(row.getLeaseOwner())
                    .leaseToken(row.getLeaseToken())
                    .leaseExpiresAt(row.getLeaseExpiresAt())
                    .errorCode(row.getErrorCode())
                    .result(row.getResult())
                    .retryOfJobId(row.getRetryOfJobId())
                    .revision(row.getRevision())
                    .createdAt(row.getCreatedAt())
                    .updatedAt(row.getUpdatedAt())
                    .build();
        }
    }

    /**
     * 中文说明：知识库种子：恰好一个 OWNER 加一个 EDITOR，嵌入空间与维度已由「首次上传」冻结，因此本类固定的是
     * 「后续上传必须沿用已冻结口径」，而首次冻结本身的派生逻辑属 {@code KnowledgeServiceImpl}。
     * English summary: The seeded knowledge base: exactly one OWNER plus one EDITOR, with the embedding space and
     * dimensions already frozen by its first upload, so what this class pins is that later uploads must reuse the frozen
     * values, while the derivation of that very first freeze stays inside {@code KnowledgeServiceImpl}.
     */
    private static KnowledgeBaseBO seedBase() {
        return KnowledgeBaseBO.builder()
                .name("yuheng runbooks")
                .description("摄取与发布合同用的最小知识库")
                .ownerActorId(OWNER.actorId())
                .members(List.of(new KnowledgeMemberDTO(EDITOR.actorId(), KnowledgeMemberRoleEnum.EDITOR)))
                .egressPolicy(KnowledgeEgressPolicyEnum.LOCAL_ONLY)
                .chatModel("chat-local")
                .embeddingModel("embedding-local")
                .embeddingSpaceId(EMBEDDING_SPACE)
                .dimensions(DIMENSIONS)
                .build();
    }

    private static byte[] bytes(String body) {
        return body.getBytes(StandardCharsets.UTF_8);
    }

    private static String sha256Hex(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 is mandated by the Spec", unavailable);
        }
    }
}
