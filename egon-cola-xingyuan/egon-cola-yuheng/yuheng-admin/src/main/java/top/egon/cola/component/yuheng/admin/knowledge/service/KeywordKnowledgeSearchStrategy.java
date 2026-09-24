package top.egon.cola.component.yuheng.admin.knowledge.service;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeRetrievalHitBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeSearchQueryBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeSearchModeEnum;
import top.egon.cola.component.yuheng.admin.knowledge.repository.KnowledgeRepository;

/**
 * 中文说明：{@code KeywordKnowledgeSearchStrategy} 是 {@code KEYWORD} 召回路，与向量路共用同一套授权联结
 * （租户、知识库成员、文档活动修订、冻结嵌入空间与维度、软删），只把匹配换成 SQL 内的字面
 * {@code ILIKE ... ESCAPE '\'} 子串命中；{@code %} 与 {@code _} 的转义在持久边界完成，本类既不拼 SQL，
 * 也不宣称 PostgreSQL {@code simple} 分词具备中文全文能力——子串匹配就是首版如实交付的能力。
 * 子串命中没有相似度刻度，SQL 因此不产出分数，本类按「已授权候选列表中的位次」补一个与混合路同尺度的
 * 倒数排名分（{@code 1/(60+rank)}），它表达的是这条证据在确定性排序中的位置，而不是任何真实性置信度；
 * 该位次来自 SQL 的分块主键升序，绝不来自结果列表的下标猜测。
 * English summary: {@code KeywordKnowledgeSearchStrategy} is the {@code KEYWORD} recall path, sharing the very same
 * authorized joins as the vector path (tenant, knowledge base membership, the document's active revision, the frozen
 * embedding space and dimensions, soft-delete) and replacing only the match with a literal {@code ILIKE ... ESCAPE '\'}
 * substring inside SQL; {@code %} and {@code _} are escaped at the persistence boundary, this class concatenates no SQL
 * and never claims that PostgreSQL's {@code simple} tokenizer performs Chinese full-text search, since substring
 * matching is exactly what the first release honestly delivers. A substring hit carries no similarity scale, so SQL
 * produces no score and this class supplies a reciprocal rank score on the same scale as the hybrid fusion
 * ({@code 1/(60+rank)}) describing where the evidence sits in the deterministic ordering, not any confidence in its
 * truthfulness; that ordering is the SQL chunk-primary-key ascending sort and never a guess from a result-list index.
 *
 * 用法 / Usage: bean 名 {@code keywordKnowledgeSearchStrategy}，由注册表按模式选中或被 {@code HybridKnowledgeSearchStrategy}
 * 作为另一腿复用；关键词缺失时按 {@code 422} 失败，绝不退化成无过滤的宽松检索。
 * Injected as {@code keywordKnowledgeSearchStrategy}, selected by mode through the registry or reused as the other leg of
 * {@code HybridKnowledgeSearchStrategy}; an absent keyword is a {@code 422} rather than a relaxation into an unfiltered
 * search.
 */
@Slf4j
@Validated
@Service("keywordKnowledgeSearchStrategy")
@RequiredArgsConstructor
public class KeywordKnowledgeSearchStrategy implements KnowledgeSearchStrategy {

    @Qualifier("knowledgeRepository")
    private final KnowledgeRepository knowledgeRepository;

    /**
     * 中文说明：执行 mode 操作，声明本策略服务 {@code KEYWORD} 模式。
     * English summary: Executes the mode operation, declaring this strategy serves {@code KEYWORD}.
     *
     * 用法 / Usage: {@code keywordKnowledgeSearchStrategy.mode()}。
     * @return 返回 {@link KnowledgeSearchModeEnum#KEYWORD}；returns {@link KnowledgeSearchModeEnum#KEYWORD}.
     */
    @Override
    public KnowledgeSearchModeEnum mode() {
        return KnowledgeSearchModeEnum.KEYWORD;
    }

    /**
     * 中文说明：执行 search 操作，先要求关键词非空（空关键词等于放弃过滤，属于命令不成立），
     * 再委派给守卫仓储的授权子串查询，并把 SQL 的确定性位次折算成倒数排名分。
     * English summary: Executes the search operation, first requiring a non-blank keyword (a blank keyword would drop the
     * filter altogether and is therefore an invalid command), then delegating to the guarded repository's authorized
     * substring query and turning SQL's deterministic ordering into reciprocal rank scores.
     *
     * 用法 / Usage: {@code keywordKnowledgeSearchStrategy.search(query)}；本路不发任何嵌入请求。
     * @param query 参数 类型化召回入参；parameter the typed recall argument.
     * @return 返回 按分块主键升序的授权候选；returns the authorized candidates ordered by chunk primary key.
     */
    @Override
    public List<KnowledgeRetrievalHitBO> search(KnowledgeSearchQueryBO query) {
        if (StringUtils.isBlank(query.getKeyword())) {
            throw new CommonException(
                    422,
                    "KNOWLEDGE_VALIDATION_FAILED",
                    "keyword recall requires a non-blank keyword"
            );
        }
        List<KnowledgeRetrievalHitBO> rows = knowledgeRepository.searchKeyword(query);
        List<KnowledgeRetrievalHitBO> ranked = new ArrayList<>(rows.size());
        int rank = 1;
        for (KnowledgeRetrievalHitBO row : rows) {
            ranked.add(row.setScore(KnowledgeRetrievalHitBO.rankScore(rank++)));
        }
        log.debug("keyword knowledge recall kbId={} keywordLength={} hits={}",
                query.getKbId(), query.getKeyword().length(), ranked.size());
        return List.copyOf(ranked);
    }
}
