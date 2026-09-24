package top.egon.cola.component.yuheng.admin.knowledge.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 中文说明：{@code KnowledgeRetrievalHitBO} 是检索 SQL 已经完成了租户、成员、活动版本、嵌入空间与维度
 * 过滤之后交回的一条候选证据，字段正好等于出站引用所需要的那一组：稳定分块 id、所属知识库/文档/冻结修订、
 * 可选的已发布页面 id、冻结版本的文件显示名与内容哈希、用于组装提示词的证据正文，以及可空的排序分数。
 * 它不含向量、metadata、租户、审计或删除时刻——那些列要么泄漏内部信息，要么在授权后仍不该出站。
 * English summary: {@code KnowledgeRetrievalHitBO} is one candidate evidence row returned by retrieval SQL that has
 * already applied the tenant, membership, active-version, embedding-space and dimension filters. Its fields are exactly the
 * set an outbound citation needs: the stable chunk id, its knowledge base, document and frozen revision, the optional id of
 * a published page, the frozen version's display name and content hash, the evidence body used to build the prompt, and a
 * nullable ranking score. It carries no vector, metadata, tenant, audit or deletion column, since those either leak internal
 * structure or must not leave the server even after authorization.
 *
 * 用法 / Usage: {@code knowledgeRepository.searchVector(query)} 的返回元素，以及
 * {@code KnowledgeCitationConverter} 的输入类型；{@code score} 由服务侧统一折算（向量路为 SQL 给的
 * {@code 1 - cosine_distance}，关键词与混合路由名次折算的确定性排序分），因此这里可空。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class KnowledgeRetrievalHitBO {

    /** 命中所属知识库十进制字符串 id / decimal-string knowledge base the hit belongs to. */
    @NotBlank
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String kbId;

    /** 稳定分块十进制字符串 id，绝不用结果列表下标代替 / stable chunk id, never a result-list index. */
    @NotBlank
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String chunkId;

    /** 所属文档十进制字符串 id / owning document id. */
    @NotBlank
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String documentId;

    /** 冻结修订十进制字符串 id，即检索时的活动修订 / frozen revision id, the active revision as seen by the query. */
    @NotBlank
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String documentRevisionId;

    /** 已发布 Wiki 页面十进制字符串 id，无页面血缘时为 null / published wiki page id, null when no page lineage attaches. */
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String pageId;

    /** 冻结版本记录的原始文件显示名，1–255，不作为路径 / original display file name recorded on the frozen version, 1–255, never a path. */
    @NotBlank
    @Size(max = 255)
    private String fileName;

    /** 冻结来源内容哈希，出站作为 {@code sourceHash} 供 stale 检查 / frozen source content hash, emitted as {@code sourceHash} for staleness checks. */
    @NotBlank
    @Pattern(regexp = "^[0-9a-f]{64}$")
    private String sourceHash;

    /** 证据正文，SQL 侧已截到引用节选上限 1000 字符，同时就是进提示词的全部内容 / evidence body, already cut to the 1000-character excerpt bound by the SQL, which is also everything that reaches the prompt. */
    @NotBlank
    @Size(max = 1_000)
    private String content;

    /** 检索排序分数，非真实性置信概率；由服务侧统一折算后可空 / retrieval ranking score rather than a truth probability, nullable until the service normalizes it. */
    private Double score;

    /**
     * 中文说明：把命中折算成融合用的名次分：{@code rank} 从 1 开始，RRF 常数取 Spec §7.3.4 的 60，
     * 即 {@code 1/(60+rank)}。混合模式对两路各自的名次分求和，因此一个分块在两路都靠前时总分最高。
     * English summary: Converts a hit into its rank-based fusion score: {@code rank} starts at 1 and the reciprocal-rank
     * fusion constant comes from Spec §7.3.4 at 60, giving {@code 1/(60+rank)}. HYBRID sums the two legs' contributions, so a
     * chunk placed highly by both lists ends up on top.
     *
     * 用法 / Usage: {@code KnowledgeRetrievalHitBO.rankScore(3)} 用于关键词与混合两路的名次折算；
     * 纯向量路的分数直接用 SQL 的 {@code 1 - cosine_distance}，不走本方法。
     * @param rank 参数 从 1 开始的名次；parameter the one-based rank.
     * @return 返回 名次折算分；returns the rank-derived score.
     */
    public static double rankScore(int rank) {
        return 1.0 / (RRF_K + rank);
    }

    /** Spec §7.3.4 固定的 RRF 融合常数 / the reciprocal-rank-fusion constant fixed by Spec §7.3.4. */
    public static final int RRF_K = 60;
}
