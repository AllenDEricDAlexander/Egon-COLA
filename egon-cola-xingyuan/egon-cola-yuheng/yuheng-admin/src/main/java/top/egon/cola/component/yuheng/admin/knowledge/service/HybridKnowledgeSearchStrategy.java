package top.egon.cola.component.yuheng.admin.knowledge.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeRetrievalHitBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeSearchQueryBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeSearchModeEnum;

/**
 * 中文说明：{@code HybridKnowledgeSearchStrategy} 是 {@code HYBRID} 召回路，只做编排不做取数：
 * 把同一个已授权查询分别交给向量路与关键词路（两腿各自受 {@code candidateLimit<=50} 约束，且共用同一套
 * SQL 授权谓词），再按 Reciprocal Rank Fusion（{@code k=60}，常量落在 {@link KnowledgeRetrievalHitBO#RRF_K}）融合。
 * 去重以分块主键的稳定十进制文本为键，同一分块只留一条证据、分数为两腿贡献之和；排序是融合分降序、
 * 并列再按分块主键的十进制数值序升序，因此结果与两条腿的到达顺序无关，可重复验证。
 * 本类不新增第三种匹配算法，也不在某腿缺输入时退回单腿：缺向量或缺关键词都由对应腿如实抛 {@code 422}。
 * English summary: {@code HybridKnowledgeSearchStrategy} is the {@code HYBRID} recall path and only orchestrates, never
 * reads: it hands one already-authorized query to the vector leg and the keyword leg (each bounded by
 * {@code candidateLimit<=50} and sharing the same SQL authorization predicates), then fuses them with Reciprocal Rank
 * Fusion ({@code k=60}, the constant living on {@link KnowledgeRetrievalHitBO#RRF_K}). Deduplication keys on the chunk's
 * stable decimal primary key, so one chunk contributes exactly one piece of evidence whose score is the sum of both
 * legs' contributions; ordering is fused score descending with ties broken by the decimal numeric order of the chunk
 * identifier, making the result independent of leg arrival order and therefore repeatable. No third matching algorithm
 * appears here and a leg never falls back to the other when its input is missing, since the leg itself fails honestly
 * with {@code 422}.
 *
 * 用法 / Usage: bean 名 {@code hybridKnowledgeSearchStrategy}；两条腿按 bean 名显式注入而非注入注册表，
 * 以免与 {@code knowledgeSearchStrategyRegistry} 形成构造环。只读、可在事务之外调用。
 * Injected as {@code hybridKnowledgeSearchStrategy}; the two legs are injected by bean name rather than through the
 * registry so no construction cycle forms with {@code knowledgeSearchStrategyRegistry}. Read-only and callable outside a
 * transaction.
 */
@Slf4j
@Validated
@Service("hybridKnowledgeSearchStrategy")
@RequiredArgsConstructor
public class HybridKnowledgeSearchStrategy implements KnowledgeSearchStrategy {

    @Qualifier("vectorKnowledgeSearchStrategy")
    private final KnowledgeSearchStrategy vectorStrategy;

    @Qualifier("keywordKnowledgeSearchStrategy")
    private final KnowledgeSearchStrategy keywordStrategy;

    /**
     * 中文说明：执行 mode 操作，声明本策略服务 {@code HYBRID} 模式。
     * English summary: Executes the mode operation, declaring this strategy serves {@code HYBRID}.
     *
     * 用法 / Usage: {@code hybridKnowledgeSearchStrategy.mode()}。
     * @return 返回 {@link KnowledgeSearchModeEnum#HYBRID}；returns {@link KnowledgeSearchModeEnum#HYBRID}.
     */
    @Override
    public KnowledgeSearchModeEnum mode() {
        return KnowledgeSearchModeEnum.HYBRID;
    }

    /**
     * 中文说明：执行 search 操作，先取两腿的授权候选再做 RRF 融合；两腿使用同一查询对象，
     * 因此租户、成员、活动修订、冻结空间与维度以及取证范围（是否要求已发布页面）在两腿间完全一致，
     * 混合结果不可能混入只被其中一路授权的内容。
     * English summary: Executes the search operation, reading both legs' authorized candidates before the RRF fusion; both
     * legs receive the same query object, so tenant, membership, active revision, frozen space and dimensions and the
     * evidence scope (whether a published page is required) are identical across them and the fusion can never mix in
     * content authorized by only one leg.
     *
     * 用法 / Usage: {@code hybridKnowledgeSearchStrategy.search(query)}。
     * @param query 参数 类型化召回入参；parameter the typed recall argument.
     * @return 返回 融合后的授权候选；returns the fused authorized candidates.
     */
    @Override
    public List<KnowledgeRetrievalHitBO> search(KnowledgeSearchQueryBO query) {
        List<KnowledgeRetrievalHitBO> fused = fuse(
                vectorStrategy.search(query),
                keywordStrategy.search(query)
        );
        log.debug("hybrid knowledge recall kbId={} fused={}", query.getKbId(), fused.size());
        return fused;
    }

    /**
     * 中文说明：按 RRF(k=60) 融合两条已授权腿，以分块主键去重、分数取两腿贡献之和，
     * 再按融合分降序与分块主键数值序升序确定性排序。包内可见以便同包策略复用同一融合口径。
     * English summary: Fuses two authorized legs with RRF(k=60), deduplicating on the chunk primary key, summing each leg's
     * contribution and ordering deterministically by fused score descending then chunk identifier numerically ascending.
     * Package-private so a same-package strategy reuses the very same fusion.
     * @param vectorLeg 参数 向量腿候选；parameter the vector leg.
     * @param keywordLeg 参数 关键词腿候选；parameter the keyword leg.
     * @return 返回 融合候选；returns the fused candidates.
     */
    static List<KnowledgeRetrievalHitBO> fuse(
            List<KnowledgeRetrievalHitBO> vectorLeg,
            List<KnowledgeRetrievalHitBO> keywordLeg) {
        Map<String, KnowledgeRetrievalHitBO> evidenceByChunk = new LinkedHashMap<>();
        Map<String, Double> scoreByChunk = new LinkedHashMap<>();
        accumulate(vectorLeg, evidenceByChunk, scoreByChunk);
        accumulate(keywordLeg, evidenceByChunk, scoreByChunk);
        List<KnowledgeRetrievalHitBO> fused = new ArrayList<>(evidenceByChunk.size());
        for (Map.Entry<String, KnowledgeRetrievalHitBO> entry : evidenceByChunk.entrySet()) {
            fused.add(entry.getValue().setScore(scoreByChunk.get(entry.getKey())));
        }
        fused.sort(Comparator
                .comparingDouble(KnowledgeRetrievalHitBO::getScore).reversed()
                .thenComparing(KnowledgeRetrievalHitBO::getChunkId, HybridKnowledgeSearchStrategy::compareAsDecimalNumber));
        return List.copyOf(fused);
    }

    /**
     * 中文说明：累计一条腿的贡献，第 {@code rank} 名贡献 {@code 1/(60+rank)}；同一分块第二次出现只加分不换证据，
     * 保证一个分块在提示词与引用列表里只有一份文本。
     * English summary: Accumulates one leg's contributions where rank {@code rank} adds {@code 1/(60+rank)}; a repeated chunk
     * only adds score without replacing its evidence, keeping one chunk to a single text in both prompt and citations.
     * @param leg 参数 本腿候选；parameter this leg's candidates.
     * @param evidenceByChunk 参数 分块到证据；parameter chunk to evidence.
     * @param scoreByChunk 参数 分块到融合分；parameter chunk to fused score.
     */
    private static void accumulate(
            List<KnowledgeRetrievalHitBO> leg,
            Map<String, KnowledgeRetrievalHitBO> evidenceByChunk,
            Map<String, Double> scoreByChunk) {
        int rank = 1;
        for (KnowledgeRetrievalHitBO hit : leg) {
            String chunkId = hit.getChunkId();
            evidenceByChunk.putIfAbsent(chunkId, hit);
            scoreByChunk.merge(chunkId, KnowledgeRetrievalHitBO.rankScore(rank++), Double::sum);
        }
    }

    /**
     * 中文说明：分块主键按十进制文本比较（先比长度再比字典序），与 SQL 的数值升序同序，
     * 避免把长 id 当字符串比较时得出与数据库不同的并列次序。
     * English summary: Compares chunk identifiers as decimal text, length before lexicographic order, matching SQL's numeric
     * ascending sort so a tie cannot land in a different order than the database produced.
     * @param left 参数 十进制标识；parameter a decimal identifier.
     * @param right 参数 十进制标识；parameter a decimal identifier.
     * @return 返回 比较结果；returns the comparison.
     */
    private static int compareAsDecimalNumber(String left, String right) {
        int byLength = Integer.compare(left.length(), right.length());
        return byLength != 0 ? byLength : left.compareTo(right);
    }
}
