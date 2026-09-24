package top.egon.cola.component.yuheng.admin.knowledge.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeRetrievalHitBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeSearchQueryBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeSearchModeEnum;
import top.egon.cola.component.yuheng.admin.knowledge.repository.KnowledgeRepository;

/**
 * 中文说明：{@code VectorKnowledgeSearchStrategy} 是 {@code VECTOR} 召回路，把「查询向量 -> 同空间最近邻」这一段
 * 完全交给一条具名 SQL：租户、知识库成员、文档活动修订、冻结嵌入空间与维度、软删这些授权谓词全部在
 * {@code KnowledgeChunkDAO.searchVectorCandidates} 里生效，排序是 {@code cosine_distance} 升序、
 * 分数即 {@code 1 - distance}、并列按分块主键确定性收尾。本类只做两件事：入参完整性复核与取数委派，
 * 既不在 Java 里算余弦相似度，也不拼原始 SQL——那会绕过 SQL 侧的授权屏障。
 * 向量缺失时按 {@code 422} 如实失败，绝不悄悄降级成关键词或全表扫描。
 * English summary: {@code VectorKnowledgeSearchStrategy} is the {@code VECTOR} recall path and hands the whole
 * "query vector to nearest neighbours inside the same frozen space" step to one named SQL statement: tenant, knowledge
 * base membership, the document's active revision, the frozen embedding space and dimensions and soft-delete all apply
 * inside {@code KnowledgeChunkDAO.searchVectorCandidates}, ordering is ascending {@code cosine_distance}, the score is
 * {@code 1 - distance} and ties close deterministically on the chunk primary key. This class does two things only,
 * re-checking argument completeness and delegating the read, so it never computes cosine similarity in Java nor
 * concatenates SQL, either of which would bypass the SQL-side authorization barrier. A missing vector fails honestly as
 * {@code 422} instead of silently degrading to keywords or a full scan.
 *
 * 用法 / Usage: bean 名 {@code vectorKnowledgeSearchStrategy}，由 {@code KnowledgeRetrievalServiceImpl} 经
 * {@code knowledgeSearchStrategyRegistry} 按模式选中，也被 {@code HybridKnowledgeSearchStrategy} 作为混合路的一腿复用；
 * 只读、不加锁、可在事务之外调用。返回空列表表示本路无证据，与依赖不可用的 {@code 503} 是两种结果。
 * Injected under the bean name {@code vectorKnowledgeSearchStrategy}, selected by mode through
 * {@code knowledgeSearchStrategyRegistry} in {@code KnowledgeRetrievalServiceImpl} and reused as one leg of
 * {@code HybridKnowledgeSearchStrategy}; read-only, unlocked and callable outside a transaction. An empty list means this
 * leg found no evidence, which is a different outcome from an unavailable dependency at {@code 503}.
 */
@Slf4j
@Validated
@Service("vectorKnowledgeSearchStrategy")
@RequiredArgsConstructor
public class VectorKnowledgeSearchStrategy implements KnowledgeSearchStrategy {

    @Qualifier("knowledgeRepository")
    private final KnowledgeRepository knowledgeRepository;

    /**
     * 中文说明：执行 mode 操作，声明本策略服务的召回模式，注册表据此建键，运行期不再有 {@code if}/{@code switch} 选路。
     * English summary: Executes the mode operation, declaring which recall mode this strategy serves so the registry keys on
     * it and no runtime {@code if}/{@code switch} selects a path any more.
     *
     * 用法 / Usage: {@code vectorKnowledgeSearchStrategy.mode()}。
     * @return 返回 {@link KnowledgeSearchModeEnum#VECTOR}；returns {@link KnowledgeSearchModeEnum#VECTOR}.
     */
    @Override
    public KnowledgeSearchModeEnum mode() {
        return KnowledgeSearchModeEnum.VECTOR;
    }

    /**
     * 中文说明：执行 search 操作，先确认查询向量确已产出（长度与冻结维度相符由嵌入端口与 SQL 谓词共同保证），
     * 再把授权与取数一次性委派给守卫仓储；缺少向量属于命令不成立，按 {@code 422 KNOWLEDGE_VALIDATION_FAILED} 失败，
     * 不返回空成功、也不换用别的召回路。
     * English summary: Executes the search operation, first proving a query vector was actually produced (its length matching
     * the frozen dimensions is held by the embedding port plus the SQL predicate) and then delegating authorization and
     * reading in one call to the guarded repository; an absent vector is an invalid command, a
     * {@code 422 KNOWLEDGE_VALIDATION_FAILED} rather than an empty success or a different recall path.
     *
     * 用法 / Usage: {@code vectorKnowledgeSearchStrategy.search(query)}，候选上限已由调用方写入
     * {@code query.candidateLimit}（每路不超过 50）。
     * @param query 参数 类型化召回入参；parameter the typed recall argument.
     * @return 返回 按余弦距离升序的授权候选；returns the authorized candidates ordered by ascending cosine distance.
     */
    @Override
    public List<KnowledgeRetrievalHitBO> search(KnowledgeSearchQueryBO query) {
        float[] vector = query.getQueryVector();
        if (vector == null || vector.length == 0) {
            throw new CommonException(
                    422,
                    "KNOWLEDGE_VALIDATION_FAILED",
                    "vector recall requires a query embedding in the knowledge base's frozen space"
            );
        }
        List<KnowledgeRetrievalHitBO> hits = knowledgeRepository.searchVector(query);
        log.debug("vector knowledge recall kbId={} candidates={} hits={}", query.getKbId(), vector.length, hits.size());
        return List.copyOf(hits);
    }
}
