package top.egon.cola.component.yuheng.admin.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.yuheng.admin.knowledge.converter.KnowledgeCitationConverter;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeBaseBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeDocumentBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeRetrievalHitBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeSearchQueryBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeAnswerCommandDTO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeEgressPolicyEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeSearchModeEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeSourceModeEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeAnswerVO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeCitationVO;
import top.egon.cola.component.yuheng.admin.knowledge.repository.KnowledgeRepository;
import top.egon.cola.component.yuheng.admin.knowledge.service.HybridKnowledgeSearchStrategy;
import top.egon.cola.component.yuheng.admin.knowledge.service.KeywordKnowledgeSearchStrategy;
import top.egon.cola.component.yuheng.admin.knowledge.service.KnowledgeModelClientService;
import top.egon.cola.component.yuheng.admin.knowledge.service.KnowledgeSearchStrategy;
import top.egon.cola.component.yuheng.admin.knowledge.service.VectorKnowledgeSearchStrategy;
import top.egon.cola.component.yuheng.admin.knowledge.service.impl.KnowledgeRetrievalServiceImpl;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;
import top.egon.cola.component.yuheng.admin.shared.domain.enums.AdminActorTypeEnum;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminNotFoundException;

/**
 * 中文说明：{@code KnowledgeRetrievalServiceTest} 固定 Step 13 的可观察契约——「先授权、再取证据、交付前再授权、
 * 只有证据才生成」。它用脚本化的 {@link KnowledgeRepository} 与 {@link KnowledgeModelClientService} 直接驱动
 * 生产实现与三条真实策略（{@code VECTOR}/{@code KEYWORD}/{@code HYBRID} 走的是与容器注册表同一张枚举表），
 * 因此本层证明的是端口顺序、调用次数、参数折算、RRF 融合、当前性复核与出站形态这些<b>源码级</b>事实。
 * 刻意不用 Spring 上下文、不用 Mockito：本层要固定的正是「谁先被调用、被调用几次、带了什么参数」，
 * 而 mock 的默认值会把「没调用」与「调用后返回空」混成同一件假绿。
 * 不在本层证明的（登记为 Runtime unverified）：SQL 内的租户、成员 jsonb、活动修订、冻结空间与维度谓词，
 * {@code cosine_distance} 排序，{@code ILIKE} 转义，wiki {@code sources @>} 血缘与 stale 过滤，
 * 以及 pgvector 与 GIN 索引行为——那些需要授权的隔离 PostgreSQL，属 mapper/guard 层的运行期证据。
 * English summary: This test pins Step 13's observable contract, authorize first, then recall, re-authorize before
 * delivery, generate only on evidence. It drives the production implementation and the three real strategies over the
 * same mode-to-strategy table the container registry builds, using a scripted {@link KnowledgeRepository} and
 * {@link KnowledgeModelClientService}, so what this layer proves is the source-level set: port order, call counts,
 * argument translation, RRF fusion, currency re-authorization and the outbound shape. Neither a Spring context nor
 * Mockito is used on purpose, because this layer pins who is called first, how often and with what, where a mock's
 * defaults would merge "never called" and "called and returned nothing" into one false green. Deliberately out of reach
 * here, and registered as runtime unverified: the tenant, membership jsonb, active-revision and frozen space/dimension
 * predicates, {@code cosine_distance} ordering, {@code ILIKE} escaping, the wiki {@code sources @>} lineage and its
 * stale filter, and pgvector and GIN behaviour, all of which need an authorized isolated PostgreSQL and belong to the
 * mapper and guard layers' runtime evidence.
 *
 * 用法 / Usage: 与 Step 12 的 {@code KnowledgeJobWorkerTest} 同层同风格；
 * 运行 {@code ./mvnw -pl ...yuheng-admin -Dtest=KnowledgeRetrievalServiceTest test}。
 */
class KnowledgeRetrievalServiceTest {

    private static final String KB_ID = "7000000000000000001";
    private static final String OWNER = "actor-owner";
    private static final String STRANGER = "actor-stranger";
    private static final String SPACE = "sp-local-1024";
    private static final String QUESTION = "授权检索的合同是什么?";
    private static final int DIMENSIONS = 4;
    private static final String HASH = "a".repeat(64);

    @Test
    @DisplayName("授权读取与冻结取证范围在嵌入与取数之前完成，并完整落到召回参数上")
    void authorizationAndFrozenScopeReachTheQueryBeforeModelCalls() {
        Fixture fixture = new Fixture(base(OWNER));
        fixture.vectorLeg.add(hit("9001", "8001", "8101", null, 0.82));

        fixture.answer(command("VECTOR", null, null));

        assertThat(fixture.port.calls)
                .as("membership is settled before any model or read call, then settled again before delivery")
                .containsExactly("findBase", "searchVector", "findBase", "findDocument");
        assertThat(fixture.port.vectorQueries).hasSize(1);
        KnowledgeSearchQueryBO query = fixture.port.vectorQueries.get(0);
        assertThat(query.getKbId()).isEqualTo(KB_ID);
        assertThat(query.getActorId())
                .as("the presenting identity is what SQL checks against the membership jsonb")
                .isEqualTo(OWNER);
        assertThat(query.getEmbeddingSpaceId()).isEqualTo(SPACE);
        assertThat(query.getDimensions()).isEqualTo(DIMENSIONS);
        assertThat(query.getSourceMode()).isEqualTo(KnowledgeSourceModeEnum.DOCUMENTS);
        assertThat(query.getCandidateLimit()).as("each leg stays inside the declared bound of fifty").isEqualTo(50);
        assertThat(query.getQueryVector()).hasSize(DIMENSIONS);
        assertThat(fixture.models.embedCalls)
                .singleElement()
                .satisfies(call -> {
                    assertThat(call.kbId()).isEqualTo(KB_ID);
                    assertThat(call.texts()).containsExactly(QUESTION);
                    assertThat(call.expectedDimensions()).isEqualTo(DIMENSIONS);
                });
        assertThat(fixture.models.generateCalls).hasSize(1);
    }

    @Test
    @DisplayName("KEYWORD 召回不产生任何嵌入调用，也不因为缺向量而退化成别的算法")
    void keywordRecallNeverEmbeds() {
        Fixture fixture = new Fixture(base(OWNER));
        fixture.keywordLeg.add(hit("9001", "8001", "8101", null, null));

        KnowledgeAnswerVO answer = fixture.answer(command("KEYWORD", null, null));

        assertThat(fixture.models.embedCalls).as("substring recall needs no vector").isEmpty();
        assertThat(fixture.port.vectorQueries).isEmpty();
        assertThat(fixture.port.keywordQueries).hasSize(1);
        assertThat(fixture.port.keywordQueries.get(0).getKeyword()).isEqualTo(QUESTION);
        assertThat(answer.getOutcome()).isEqualTo("ANSWERED");
        assertThat(answer.getCitations()).singleElement()
                .satisfies(citation -> assertThat(citation.getScore())
                        .as("a substring hit carries a reciprocal rank position, not a similarity")
                        .isEqualTo(KnowledgeRetrievalHitBO.rankScore(1)));
    }

    @Test
    @DisplayName("HYBRID 用同一份授权入参跑两条腿，按稳定分块标识去重并按 RRF(k=60) 融合排序")
    void hybridFusesTwoAuthorizedLegsWithReciprocalRankFusion() {
        Fixture fixture = new Fixture(base(OWNER));
        fixture.vectorLeg.add(hit("9001", "8001", "8101", null, 0.82));
        fixture.vectorLeg.add(hit("9002", "8002", "8102", null, 0.71));
        fixture.keywordLeg.add(hit("9002", "8002", "8102", null, null));
        fixture.keywordLeg.add(hit("9003", "8003", "8103", null, null));

        KnowledgeAnswerVO answer = fixture.answer(command("HYBRID", null, null));

        assertThat(fixture.port.vectorQueries).hasSize(1);
        assertThat(fixture.port.keywordQueries).hasSize(1);
        assertThat(fixture.port.keywordQueries.get(0))
                .as("both legs read under the very same authorization argument")
                .isSameAs(fixture.port.vectorQueries.get(0));
        assertThat(fixture.models.embedCalls).hasSize(1);
        assertThat(answer.getCitations())
                .as("chunk 9002 is in both legs and must stay exactly one traceable citation")
                .extracting(KnowledgeCitationVO::getChunkId)
                .containsExactly("9002", "9001", "9003");
        assertThat(citation(answer, "9002").getScore())
                .isEqualTo(KnowledgeRetrievalHitBO.rankScore(2) + KnowledgeRetrievalHitBO.rankScore(1));
        assertThat(citation(answer, "9001").getScore()).isEqualTo(KnowledgeRetrievalHitBO.rankScore(1));
        assertThat(citation(answer, "9003").getScore()).isEqualTo(KnowledgeRetrievalHitBO.rankScore(2));
    }

    @Test
    @DisplayName("没有可用证据时如实返回 NO_EVIDENCE，生成调用为零且不伪造引用")
    void withoutEvidenceTheAnswerIsUnsupportedWithZeroGenerationCalls() {
        Fixture fixture = new Fixture(base(OWNER));

        KnowledgeAnswerVO answer = fixture.answer(command("VECTOR", null, null));

        assertThat(answer.getOutcome()).isEqualTo("NO_EVIDENCE");
        assertThat(answer.getAnswer()).isEqualTo("未找到可引用资料");
        assertThat(answer.getCitations()).isEmpty();
        assertThat(answer.getModel()).isEqualTo("local-chat-alias");
        assertThat(fixture.models.generateCalls).as("an uncited generation is never a fallback").isEmpty();
        assertThat(fixture.models.embedCalls).hasSize(1);
    }

    @Test
    @DisplayName("依赖不可用仍是 503，绝不与「没有证据」混同，也不伪装成空成功")
    void unavailableDependencyStays503AndIsNotEvidenceAbsence() {
        Fixture fixture = new Fixture(base(OWNER));
        fixture.models.failEmbedding = true;

        assertThatExceptionOfType(CommonException.class)
                .isThrownBy(() -> fixture.answer(command("VECTOR", null, null)))
                .satisfies(error -> {
                    assertThat(error.getCode()).isEqualTo(503);
                    assertThat(error.getStatus()).isEqualTo("KNOWLEDGE_MODEL_UNAVAILABLE");
                });
        assertThat(fixture.port.vectorQueries).as("a dead embedding alias never reaches the recall SQL").isEmpty();
        assertThat(fixture.models.generateCalls).isEmpty();
    }

    @Test
    @DisplayName("生成前重读成员资格，期间被撤权就停在这里且生成调用为零")
    void revokedMembershipStopsBeforeGeneration() {
        Fixture fixture = new Fixture(base(OWNER));
        fixture.revokeBeforeDelivery = true;
        fixture.vectorLeg.add(hit("9001", "8001", "8101", null, 0.82));

        assertThatExceptionOfType(CommonException.class)
                .isThrownBy(() -> fixture.answer(command("VECTOR", null, null)))
                .satisfies(error -> {
                    assertThat(error.getCode()).isEqualTo(403);
                    assertThat(error.getStatus()).isEqualTo("KNOWLEDGE_FORBIDDEN");
                });
        assertThat(fixture.models.embedCalls).hasSize(1);
        assertThat(fixture.models.generateCalls).isEmpty();
    }

    @Test
    @DisplayName("并发发布替换了活动修订时该条证据在交付前被丢弃，而不是产出混合作答")
    void aSupersededPublicationIsDroppedFromTheCitationList() {
        Fixture fixture = new Fixture(base(OWNER));
        fixture.vectorLeg.add(hit("9001", "8001", "8101", null, 0.82));
        fixture.vectorLeg.add(hit("9002", "8002", "8102", null, 0.71));
        fixture.publishedRevisions.put("8002", "8999");

        KnowledgeAnswerVO answer = fixture.answer(command("VECTOR", null, null));

        assertThat(answer.getOutcome()).isEqualTo("ANSWERED");
        assertThat(answer.getCitations())
                .as("the citation whose document moved on never reaches the answer")
                .extracting(KnowledgeCitationVO::getChunkId)
                .containsExactly("9001");
        assertThat(fixture.models.generateCalls.get(0)).doesNotContain("8102");
    }

    @Test
    @DisplayName("文档在召回之后已不可见时同样被丢弃，跨库同 id 不构成证据")
    void anInvisibleDocumentIsDroppedBeforeDelivery() {
        Fixture fixture = new Fixture(base(OWNER));
        fixture.vectorLeg.add(hit("9001", "8001", "8101", null, 0.82));
        fixture.vectorLeg.add(hit("9002", "8002", "8102", null, 0.71));
        fixture.invisibleDocuments.add("8002");

        KnowledgeAnswerVO answer = fixture.answer(command("VECTOR", null, null));

        assertThat(answer.getCitations()).extracting(KnowledgeCitationVO::getChunkId).containsExactly("9001");
    }

    @Test
    @DisplayName("topK 缺省 8、上限 20，且裁剪发生在当前性复核之后")
    void topKDefaultsAndCeilingApplyAfterCurrencyChecks() {
        Fixture fixture = new Fixture(base(OWNER));
        for (int index = 1; index <= 25; index++) {
            fixture.vectorLeg.add(hit("9" + String.format("%02d", index), "8001", "8101", null, 0.9 - index / 100.0));
        }

        assertThat(fixture.answer(command("VECTOR", null, null)).getCitations()).hasSize(8);
        assertThat(fixture.answer(command("VECTOR", 20, null)).getCitations()).hasSize(20);
        assertThat(fixture.answer(command("VECTOR", 3, null)).getCitations()).hasSize(3);
        assertThat(fixture.models.generateCalls).hasSize(3);
    }

    @Test
    @DisplayName("取证范围只作为一份入参下沉到 SQL，pageId 只在带页面血缘的范围下出站")
    void evidenceScopeTravelsAsOneArgumentAndOnlyLineageScopesCarryPageId() {
        Fixture wiki = new Fixture(base(OWNER));
        wiki.vectorLeg.add(hit("9001", "8001", "8101", "6001", 0.82));
        KnowledgeAnswerVO wikiAnswer = wiki.answer(command("VECTOR", null, "WIKI"));
        assertThat(wikiAnswer.getCitations()).singleElement()
                .satisfies(citation -> assertThat(citation.getPageId()).isEqualTo("6001"));
        assertThat(wiki.port.vectorQueries.get(0).getSourceMode()).isEqualTo(KnowledgeSourceModeEnum.WIKI);

        Fixture both = new Fixture(base(OWNER));
        both.vectorLeg.add(hit("9001", "8001", "8101", "6001", 0.82));
        assertThat(both.answer(command("VECTOR", null, "BOTH")).getCitations()).singleElement()
                .satisfies(citation -> assertThat(citation.getPageId()).isEqualTo("6001"));

        Fixture documents = new Fixture(base(OWNER));
        documents.vectorLeg.add(hit("9001", "8001", "8101", null, 0.82));
        KnowledgeAnswerVO documentAnswer = documents.answer(command("VECTOR", null, "DOCUMENTS"));
        assertThat(documentAnswer.getCitations()).singleElement()
                .satisfies(citation -> assertThat(citation.getPageId()).isNull());
        assertThat(documentAnswer.getCitations()).singleElement()
                .satisfies(citation -> assertThat(citation.getDocumentRevisionId()).isEqualTo("8101"));
    }

    @Test
    @DisplayName("未知模式字面量与缺失冻结空间都按 422 失败，绝不回落到默认算法或无范围召回")
    void invalidModeAndMissingFrozenScopeFailClosedAt422() {
        Fixture fixture = new Fixture(base(OWNER));

        assertThatExceptionOfType(CommonException.class)
                .isThrownBy(() -> fixture.answer(command("SEMANTIC", null, null)))
                .satisfies(error -> {
                    assertThat(error.getCode()).isEqualTo(422);
                    assertThat(error.getStatus()).isEqualTo("KNOWLEDGE_VALIDATION_FAILED");
                });
        assertThatExceptionOfType(CommonException.class)
                .isThrownBy(() -> fixture.answer(command("VECTOR", null, "EVERYTHING")))
                .satisfies(error -> assertThat(error.getCode()).isEqualTo(422));
        assertThat(fixture.port.vectorQueries).isEmpty();
        assertThat(fixture.models.embedCalls).isEmpty();

        Fixture unindexed = new Fixture(base(OWNER).setEmbeddingSpaceId(null).setDimensions(null));
        unindexed.vectorLeg.add(hit("9001", "8001", "8101", null, 0.82));
        assertThatExceptionOfType(CommonException.class)
                .isThrownBy(() -> unindexed.answer(command("VECTOR", null, null)))
                .satisfies(error -> assertThat(error.getCode()).isEqualTo(422));
        assertThat(unindexed.port.vectorQueries).as("no frozen scope means no searchable index").isEmpty();
        assertThat(unindexed.models.embedCalls).isEmpty();
    }

    @Test
    @DisplayName("知识库不可见按 404、可见但角色不足按 403，两者都发生在任何模型调用之前")
    void invisibleBaseIs404AndInsufficientRoleIs403BeforeAnyModelCall() {
        Fixture missing = new Fixture(null);
        assertThatExceptionOfType(GatewayAdminNotFoundException.class)
                .isThrownBy(() -> missing.answer(command("VECTOR", null, null)));
        assertThat(missing.models.embedCalls).isEmpty();
        assertThat(missing.port.vectorQueries).isEmpty();

        Fixture foreign = new Fixture(base(OWNER));
        assertThatExceptionOfType(CommonException.class)
                .isThrownBy(() -> foreign.answer(STRANGER, command("VECTOR", null, null)))
                .satisfies(error -> {
                    assertThat(error.getCode()).isEqualTo(403);
                    assertThat(error.getStatus()).isEqualTo("KNOWLEDGE_FORBIDDEN");
                });
        assertThat(foreign.port.vectorQueries).isEmpty();
    }

    @Test
    @DisplayName("出站引用只带可追溯业务事实，节选受声明上限约束且提示词不越过端口上限")
    void outboundCitationsCarryOnlyTraceableFactsWithinDeclaredBounds() {
        Fixture fixture = new Fixture(base(OWNER));
        fixture.vectorLeg.add(hit("9001", "8001", "8101", null, 0.82));
        fixture.vectorLeg.add(hit("9002", "8002", "8102", null, "b".repeat(1_500), 0.66));

        KnowledgeAnswerVO answer = fixture.answer(command("HYBRID", null, "BOTH"));

        assertThat(answer.getCitations()).hasSize(2);
        assertThat(answer.getCitations()).allSatisfy(citation -> {
            assertThat(citation.getDocumentRevisionId()).isNotBlank();
            assertThat(citation.getSourceHash()).hasSize(64);
            assertThat(citation.getDocumentId()).isNotBlank();
            assertThat(citation.getFileName()).isNotBlank();
            assertThat(citation.getExcerpt()).hasSizeLessThanOrEqualTo(1_000);
            assertThat(citation.getScore()).isNotNull();
        });
        assertThat(citation(answer, "9002").getExcerpt())
                .as("an over-long body is cut to the declared bound, never padded and never echoed in full")
                .hasSize(1_000);
        assertThat(fixture.models.generateCalls.get(0))
                .as("the prompt carries authorized excerpts and identifiers only, never a vector or a stored byte blob")
                .hasSizeLessThanOrEqualTo(60_000)
                .doesNotContain("embedding", "rawBytes", "metadata", "0.82");
    }

    private static KnowledgeAnswerCommandDTO command(String searchMode, Integer topK, String sourceMode) {
        return new KnowledgeAnswerCommandDTO()
                .setQuestion(QUESTION)
                .setSearchMode(searchMode)
                .setTopK(topK)
                .setSourceMode(sourceMode);
    }

    private static AdminActor actor(String actorId) {
        return new AdminActor(actorId, AdminActorTypeEnum.USER, Set.of(), Set.of());
    }

    private static KnowledgeBaseBO base(String ownerActorId) {
        return new KnowledgeBaseBO()
                .setId(KB_ID)
                .setName("授权检索")
                .setDescription("Step 13 固定其检索合同")
                .setOwnerActorId(ownerActorId)
                .setMembers(List.of())
                .setEgressPolicy(KnowledgeEgressPolicyEnum.LOCAL_ONLY)
                .setChatModel("local-chat-alias")
                .setEmbeddingModel("local-embedding-alias")
                .setEmbeddingSpaceId(SPACE)
                .setDimensions(DIMENSIONS)
                .setRevision(1L);
    }

    private static KnowledgeRetrievalHitBO hit(String chunkId, String documentId, String revisionId,
                                               String pageId, Double score) {
        return hit(chunkId, documentId, revisionId, pageId, "分块正文 " + chunkId, score);
    }

    private static KnowledgeRetrievalHitBO hit(String chunkId, String documentId, String revisionId, String pageId,
                                               String content, Double score) {
        return new KnowledgeRetrievalHitBO()
                .setKbId(KB_ID)
                .setChunkId(chunkId)
                .setDocumentId(documentId)
                .setDocumentRevisionId(revisionId)
                .setPageId(pageId)
                .setFileName("document-" + documentId + ".md")
                .setSourceHash(HASH)
                .setContent(content)
                .setScore(score);
    }

    private static KnowledgeCitationVO citation(KnowledgeAnswerVO answer, String chunkId) {
        return answer.getCitations().stream()
                .filter(entry -> entry.getChunkId().equals(chunkId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no citation kept for chunk " + chunkId));
    }

    /**
     * 中文说明：一次问答的装配台——真实策略、真实转换器加脚本化端口，协作者顺序与 Spring 注入顺序一致，
     * 注册表按 {@code mode()} 归出枚举表，与 {@code knowledgeSearchStrategyRegistry} 的口径相同。
     * English summary: The assembly of one answer, real strategies and the real converter over a scripted port, built in the
     * collaborator order Spring injects and with the registry keyed by {@code mode()} exactly as
     * {@code knowledgeSearchStrategyRegistry} keys it.
     */
    private static final class Fixture {

        private final ScriptedKnowledgePort port;
        private final ScriptedModelClient models = new ScriptedModelClient();
        private final KnowledgeRetrievalServiceImpl service;
        private final List<KnowledgeRetrievalHitBO> vectorLeg = new ArrayList<>();
        private final List<KnowledgeRetrievalHitBO> keywordLeg = new ArrayList<>();
        private final Map<String, String> publishedRevisions = new LinkedHashMap<>();
        private final List<String> invisibleDocuments = new ArrayList<>();
        private final KnowledgeBaseBO visibleBase;
        private boolean revokeBeforeDelivery;

        private Fixture(KnowledgeBaseBO visibleBase) {
            this.visibleBase = visibleBase;
            this.port = new ScriptedKnowledgePort(this);
            Map<KnowledgeSearchModeEnum, KnowledgeSearchStrategy> registry =
                    new EnumMap<>(KnowledgeSearchModeEnum.class);
            for (KnowledgeSearchStrategy strategy : List.of(
                    new VectorKnowledgeSearchStrategy(port.proxy()),
                    new KeywordKnowledgeSearchStrategy(port.proxy()),
                    new HybridKnowledgeSearchStrategy(
                            new VectorKnowledgeSearchStrategy(port.proxy()),
                            new KeywordKnowledgeSearchStrategy(port.proxy())))) {
                registry.put(strategy.mode(), strategy);
            }
            this.service = new KnowledgeRetrievalServiceImpl(
                    port.proxy(), models, Collections.unmodifiableMap(registry), new KnowledgeCitationConverter());
        }

        private KnowledgeAnswerVO answer(KnowledgeAnswerCommandDTO command) {
            return answer(OWNER, command);
        }

        private KnowledgeAnswerVO answer(String actorId, KnowledgeAnswerCommandDTO command) {
            return service.answer(actor(actorId), KB_ID, command);
        }

        /** 候选所属文档的当前活动修订：脚本可覆盖，未覆盖即等于候选冻结的那一版。 */
        private String activeRevisionOf(String documentId) {
            if (invisibleDocuments.contains(documentId)) {
                return null;
            }
            String overridden = publishedRevisions.get(documentId);
            if (overridden != null) {
                return overridden;
            }
            return java.util.stream.Stream.concat(vectorLeg.stream(), keywordLeg.stream())
                    .filter(hit -> hit.getDocumentId().equals(documentId))
                    .map(KnowledgeRetrievalHitBO::getDocumentRevisionId)
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * 中文说明：脚本化的 {@link KnowledgeRepository}：只应答本层要固定的四个读方法，其余一律抛错，
     * 因为「触达了不该触达的端口」必须是失败而不是空值；读序被记录下来，用于断言授权先于取数与生成。
     * English summary: The scripted {@link KnowledgeRepository}: it answers only the four reads this layer pins and fails
     * every other call, since reaching a port that must stay untouched has to be a failure rather than a null, and the
     * read order is recorded to prove authorization precedes reading and generation.
     */
    private static final class ScriptedKnowledgePort {

        private final Fixture fixture;
        private final List<String> calls = new ArrayList<>();
        private final List<KnowledgeSearchQueryBO> vectorQueries = new ArrayList<>();
        private final List<KnowledgeSearchQueryBO> keywordQueries = new ArrayList<>();
        private final KnowledgeRepository proxy;
        private int baseReads;

        private ScriptedKnowledgePort(Fixture fixture) {
            this.fixture = fixture;
            this.proxy = (KnowledgeRepository) Proxy.newProxyInstance(
                    KnowledgeRepository.class.getClassLoader(),
                    new Class<?>[] {KnowledgeRepository.class},
                    this::dispatch);
        }

        private KnowledgeRepository proxy() {
            return proxy;
        }

        private Object dispatch(Object instance, java.lang.reflect.Method method, Object[] arguments) {
            String name = method.getName();
            calls.add(name);
            return switch (name) {
                case "findBase" -> readBase();
                case "findDocument" -> readDocument((String) arguments[0], (String) arguments[1]);
                case "searchVector" -> readLeg((KnowledgeSearchQueryBO) arguments[0], vectorQueries, fixture.vectorLeg);
                case "searchKeyword" -> readLeg((KnowledgeSearchQueryBO) arguments[0], keywordQueries, fixture.keywordLeg);
                default -> throw new AssertionError(
                        "KnowledgeRepository." + name + " must not be reached by authorized retrieval");
            };
        }

        private Optional<KnowledgeBaseBO> readBase() {
            baseReads++;
            if (fixture.visibleBase == null) {
                return Optional.empty();
            }
            if (fixture.revokeBeforeDelivery && baseReads > 1) {
                return Optional.of(base(STRANGER));
            }
            return Optional.of(fixture.visibleBase);
        }

        private Optional<KnowledgeDocumentBO> readDocument(String kbId, String documentId) {
            String activeRevisionId = fixture.activeRevisionOf(documentId);
            if (activeRevisionId == null) {
                return Optional.empty();
            }
            return Optional.of(new KnowledgeDocumentBO()
                    .setId(documentId)
                    .setKbId(kbId)
                    .setFileName("document-" + documentId + ".md")
                    .setActiveRevisionId(activeRevisionId)
                    .setRevision(1L));
        }

        private List<KnowledgeRetrievalHitBO> readLeg(KnowledgeSearchQueryBO query,
                                                      List<KnowledgeSearchQueryBO> seen,
                                                      List<KnowledgeRetrievalHitBO> rows) {
            seen.add(query);
            return new ArrayList<>(rows);
        }
    }

    /**
     * 中文说明：脚本化的模型端口，记录每次嵌入与生成的完整入参，并可被置为失败以固定 {@code 503} 路径。
     * English summary: The scripted model port, recording every embedding and generation argument and switchable to failure
     * to pin the {@code 503} path.
     */
    private static final class ScriptedModelClient implements KnowledgeModelClientService {

        private record Embedding(String kbId, List<String> texts, int expectedDimensions) {
        }

        private final List<Embedding> embedCalls = new ArrayList<>();
        private final List<String> generateCalls = new ArrayList<>();
        private boolean failEmbedding;

        @Override
        public List<float[]> embed(String kbId, List<String> texts, int expectedDimensions) {
            embedCalls.add(new Embedding(kbId, List.copyOf(texts), expectedDimensions));
            if (failEmbedding) {
                throw new CommonException(503, "KNOWLEDGE_MODEL_UNAVAILABLE", "the local embedding alias is down");
            }
            float[] vector = new float[expectedDimensions];
            for (int index = 0; index < expectedDimensions; index++) {
                vector[index] = 0.25f + index * 0.1f;
            }
            return List.of(vector);
        }

        @Override
        public String generate(String kbId, String prompt, int maxTokens) {
            generateCalls.add(prompt);
            return "依据资料 1 的回答";
        }
    }
}
