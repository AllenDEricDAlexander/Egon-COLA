package top.egon.cola.component.yuheng.admin.knowledge.domain.bo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeSourceModeEnum;

/**
 * 中文说明：{@code KnowledgeSearchQueryBO} 是一次授权召回的全部有界输入：目标知识库、请求者身份
 * （SQL 侧成员谓词用它，绝不由控制器代答）、知识库冻结的嵌入空间与维度、可选的查询向量与关键词、
 * 本路候选上限，以及取证范围。它刻意把「成员与活动版本条件写进 SQL」这件事需要的字段收在一起，
 * 使 {@code KnowledgeSearchStrategy} 的三个实现都只依赖这一个类型化入参而不是散落的原始参数。
 * English summary: {@code KnowledgeSearchQueryBO} carries every bounded input of one authorized recall: the target
 * knowledge base, the requesting identity (which the SQL-side membership predicate consumes rather than a controller
 * answer), the knowledge base's frozen embedding space and dimensions, the optional query vector and keyword, this
 * leg's candidate ceiling, and the evidence scope. It deliberately groups the fields that "put the member and
 * active-version conditions into the SQL" needs, so all three {@code KnowledgeSearchStrategy} implementations depend on
 * this single typed argument instead of loose primitives.
 *
 * 用法 / Usage: 由 {@code KnowledgeRetrievalServiceImpl} 在权限与状态复核之后构造，交给注册表选出的策略；
 * {@code queryVector} 与 {@code keyword} 都可为空，由实际需要它们的那一路在使用前失败关闭——
 * {@code VECTOR}/{@code HYBRID} 需要 {@code queryVector}，{@code KEYWORD} 与 {@code HYBRID} 需要 {@code keyword}，
 * 且关键词模式永远不触达本地 embedding（不因选了 KEYWORD 就顺带产生一次嵌入调用）。
 * 向量与关键词都不带 {@code @NotNull}，是因为同一载体服务两种互斥算法；跨层校验仍由注解承担，
 * 策略入口只补这条「本路必需字段」的判定。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class KnowledgeSearchQueryBO {

    /** 目标知识库十进制字符串 id / decimal-string target knowledge base id. */
    @NotBlank
    @Pattern(regexp = "^[1-9][0-9]{0,19}$")
    private String kbId;

    /** 请求者身份，用于 SQL 侧成员谓词；varchar(128) 与成员表列同界 / requesting identity feeding the SQL membership predicate, bounded by the same varchar(128) as the member column. */
    @NotBlank
    @Size(max = 128)
    private String actorId;

    /** 知识库首次上传时冻结的嵌入空间标识，禁止跨空间混用；两路都用它收口 SQL 谓词 / embedding space frozen at the first upload, never mixed across spaces; both legs bound the SQL predicate with it. */
    @NotBlank
    @Size(max = 128)
    private String embeddingSpaceId;

    /** 冻结维度，正整数且不超过列上界，两路都必须与向量长度同值 / frozen dimensions, a positive number inside the column bound and equal to the vector length on both legs. */
    @NotNull
    @Min(1)
    @Max(16_000)
    private Integer dimensions;

    /** 本地嵌入产出的查询向量，长度须等于 {@code dimensions}；KEYWORD 路不设置 / query vector from the LOCAL embedding, exactly {@code dimensions} long; unset by the KEYWORD leg. */
    @Size(min = 1, max = 16_000)
    private float[] queryVector;

    /** 字面关键词（即用户问题本身），KEYWORD/HYBRID 路使用并在 SQL 中转义 {@code %} 与 {@code _} / literal keyword (the question itself), used by the KEYWORD/HYBRID legs and escaped for {@code %} and {@code _} in SQL. */
    @Size(max = 8_000)
    private String keyword;

    /** 本路候选上限，1–50（HYBRID 两路各取 50 后融合）/ this leg's candidate ceiling, 1–50 (each HYBRID leg takes up to 50 before fusion). */
    @Min(1)
    @Max(50)
    private int candidateLimit;

    /** 取证范围，决定页面血缘列与「必须有已发布页面」谓词 / evidence scope deciding the page-lineage column and the published-page requirement. */
    @NotNull
    private KnowledgeSourceModeEnum sourceMode;

    /**
     * 中文说明：查询向量的防御性读取副本，避免调用方拿到载体内部数组后改写已校验过的向量。
     * English summary: A defensive read copy of the query vector so a caller cannot mutate a validated vector through
     * the carrier.
     *
     * 用法 / Usage: {@code query.getQueryVector()}；返回 {@code null} 表示本路不使用向量。
     * @return 返回 向量副本或 {@code null}；returns the vector copy or {@code null}.
     */
    public float[] getQueryVector() {
        return queryVector == null ? null : queryVector.clone();
    }

    /**
     * 中文说明：写入查询向量时先拷贝入参，使载体与调用方数组脱钩。
     * English summary: Copies the argument on write so the carrier and the caller's array are decoupled.
     *
     * 用法 / Usage: {@code query.setQueryVector(vector)}（链式返回 {@code this}）。
     * @param queryVector 参数 查询向量，可为 {@code null}；parameter the query vector, possibly {@code null}.
     * @return 返回 本载体链式句柄；returns this carrier for chaining.
     */
    public KnowledgeSearchQueryBO setQueryVector(float[] queryVector) {
        this.queryVector = queryVector == null ? null : queryVector.clone();
        return this;
    }
}
