package top.egon.cola.component.yuheng.admin.knowledge.dao;

import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeRetrievalHitBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.po.KnowledgeChunkPO;

import java.util.List;

/**
 * 知识分块表的 MyBatis-Plus Mapper，复用 EgonColaMapper 的租户内活跃读取与版本化软删除。
 * MyBatis-Plus mapper for knowledge chunks; inherits tenant-scoped active reads and versioned soft-delete.
 * 用法 / Usage: 仅声明 Spec §11.2 访问路径所需的具名类型化查询，通用 CRUD 一律经对应持久化仓储调用。
 * 除继承的通用方法外，本接口只承载 Step 13 授权检索的两条具名语句（{@code searchVectorCandidates}、
 * {@code searchKeywordCandidates}）：跨 {@code gateway_knowledge_revision}/{@code gateway_knowledge_document}/
 * {@code gateway_knowledge_base} 的联结、jsonb 成员谓词、pgvector {@code cosine_distance} 排序与已发布 Wiki
 * 血缘都无法由 {@code Wrapper} 表达，只能落在具名 SQL 上（Spec §11.2.6、修订 §7.3.4）。返回类型是业务载体
 * {@link KnowledgeRetrievalHitBO} 而不是行模型：检索出站只需要引用那一组列，向量与审计列一律不进出站面。
 * Beyond the inherited generic methods this interface carries only the two Step 13 retrieval statements
 * ({@code searchVectorCandidates}, {@code searchKeywordCandidates}): the joins onto
 * {@code gateway_knowledge_revision}/{@code gateway_knowledge_document}/{@code gateway_knowledge_base}, the jsonb
 * membership predicate, the pgvector {@code cosine_distance} ordering and the published-wiki lineage cannot be expressed by
 * a {@code Wrapper}, so they live in named SQL (Spec §11.2.6, amendment §7.3.4). The return type is the business carrier
 * {@link KnowledgeRetrievalHitBO} rather than the row model, because retrieval outbound needs only the citation column set
 * and never the vector or the audit columns.
 */
public interface KnowledgeChunkDAO extends EgonColaMapper<KnowledgeChunkPO> {

    /**
     * 中文说明：向量路的授权召回——在同一冻结嵌入空间与维度内，按 pgvector
     * {@code cosine_distance} 升序取至多 {@code candidateLimit} 条候选，并同时把租户、知识库成员、
     * 文档活动修订、修订空间/维度一致与活跃谓词写进 SQL；分数取 {@code 1 - cosine_distance}，
     * 距离相同的并列按分块 id 升序确定化。{@code attachPageLineage} 为真时额外带出当前已发布 Wiki 页面 id，
     * {@code requirePublishedPage} 为真时只保留被该页面引用的分块。
     * English summary: The authorized vector recall — inside one frozen embedding space and dimension it takes at most
     * {@code candidateLimit} candidates ascending by pgvector {@code cosine_distance}, while the SQL itself carries the
     * tenant, knowledge-base membership, active-document, revision-space/dimension and active-row predicates. The score is
     * {@code 1 - cosine_distance} and equal distances break deterministically by ascending chunk id. When
     * {@code attachPageLineage} holds it also reads out the currently published wiki page id, and when
     * {@code requirePublishedPage} holds only chunks that page cites survive.
     *
     * 用法 / Usage: 只由 {@code MpKnowledgeRepository.searchVector(KnowledgeSearchQueryBO)} 调用，
     * 即只由 {@code VectorKnowledgeSearchStrategy} 与 {@code HybridKnowledgeSearchStrategy} 间接调用；
     * 查询向量必须来自本知识库冻结的 LOCAL 嵌入 alias（无云端兜底），维度不符即 0 行而不是跨空间取数。
     * @param kbId 目标知识库主键；target knowledge base key.
     * @param actorId 请求者身份，喂给 jsonb 成员谓词；requesting identity feeding the jsonb membership predicate.
     * @param embeddingSpaceId 知识库冻结的嵌入空间标识；the knowledge base's frozen embedding space.
     * @param dimensions 冻结维度，与向量长度同值；the frozen dimensions, equal to the vector length.
     * @param queryVector 本地嵌入产出的查询向量，经 {@code GatewayVectorTypeHandler} 渲染为 vector 字面量；the local query vector, rendered as a vector literal through {@code GatewayVectorTypeHandler}.
     * @param attachPageLineage 是否带出已发布页面 id；whether to read out the published page id.
     * @param requirePublishedPage 是否要求候选被已发布页面引用；whether a candidate must be cited by a published page.
     * @param candidateLimit 本路候选上限；this leg's candidate ceiling.
     * @return 命中候选载体列表，按距离升序、id 确定化；the candidate carriers ordered by ascending distance with a deterministic id tie-break.
     */
    List<KnowledgeRetrievalHitBO> searchVectorCandidates(
            @Param("kbId") long kbId,
            @Param("actorId") String actorId,
            @Param("embeddingSpaceId") String embeddingSpaceId,
            @Param("dimensions") int dimensions,
            @Param("queryVector") float[] queryVector,
            @Param("attachPageLineage") boolean attachPageLineage,
            @Param("requirePublishedPage") boolean requirePublishedPage,
            @Param("candidateLimit") int candidateLimit
    );

    /**
     * 中文说明：关键词路的授权召回——与向量路同一套联结与谓词，命中条件换成对分块正文的
     * {@code ILIKE} 字面子串匹配，{@code keywordPattern} 由仓储把已转义 {@code \}、{@code %}、{@code _}
     * 的关键词加上两端通配后绑定，反斜杠作为 ESCAPE 字符；因此用户输入的 {@code %} 永远不会变成通配符，
     * 也不存在“权限条件先取全表再 Java 过滤”的退路。没有全文评分，故排序按分块 id 升序确定化，
     * 名次分由服务侧按 RRF 折算。
     * English summary: The authorized keyword recall — the same joins and predicates as the vector leg, with the match
     * replaced by a literal {@code ILIKE} substring over the chunk body. {@code keywordPattern} is bound by the repository
     * after escaping {@code \}, {@code %} and {@code _} and wrapping wildcards around it, with backslash as the ESCAPE
     * character, so a user-supplied {@code %} never becomes a wildcard and there is no path that reads the whole table and
     * filters in Java. Without a full-text ranking the order is deterministic ascending chunk id, and the rank score comes
     * from the service's reciprocal-rank fusion.
     *
     * 用法 / Usage: 只由 {@code MpKnowledgeRepository.searchKeyword(KnowledgeSearchQueryBO)} 调用，
     * 即只由 {@code KeywordKnowledgeSearchStrategy} 与 {@code HybridKnowledgeSearchStrategy} 间接调用；
     * 本路不触达嵌入模型，KEYWORD 模式一次嵌入调用都不该发生。
     * @param kbId 目标知识库主键；target knowledge base key.
     * @param actorId 请求者身份，喂给 jsonb 成员谓词；requesting identity feeding the jsonb membership predicate.
     * @param embeddingSpaceId 知识库冻结的嵌入空间标识；the knowledge base's frozen embedding space.
     * @param dimensions 冻结维度；the frozen dimensions.
     * @param keywordPattern 已转义并加通配的匹配模式；the escaped, wildcard-wrapped match pattern.
     * @param attachPageLineage 是否带出已发布页面 id；whether to read out the published page id.
     * @param requirePublishedPage 是否要求候选被已发布页面引用；whether a candidate must be cited by a published page.
     * @param candidateLimit 本路候选上限；this leg's candidate ceiling.
     * @return 命中候选载体列表，按分块 id 升序；the candidate carriers ordered by ascending chunk id.
     */
    List<KnowledgeRetrievalHitBO> searchKeywordCandidates(
            @Param("kbId") long kbId,
            @Param("actorId") String actorId,
            @Param("embeddingSpaceId") String embeddingSpaceId,
            @Param("dimensions") int dimensions,
            @Param("keywordPattern") String keywordPattern,
            @Param("attachPageLineage") boolean attachPageLineage,
            @Param("requirePublishedPage") boolean requirePublishedPage,
            @Param("candidateLimit") int candidateLimit
    );
}
