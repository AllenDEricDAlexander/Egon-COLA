package top.egon.cola.component.yuheng.admin.knowledge.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeRetrievalHitBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeSearchQueryBO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeSearchModeEnum;

import java.util.List;

/**
 * 中文说明：{@code KnowledgeSearchStrategy} 是 API-022 三种召回算法的策略契约（Rule 9）：
 * 每个实现声明自己服务的 {@link KnowledgeSearchModeEnum}，并只接受一个类型化的
 * {@link KnowledgeSearchQueryBO}、只回候选 {@link KnowledgeRetrievalHitBO} 列表。
 * 契约的核心不是「怎么算相似」而是「谁有权被召回」：租户、知识库成员、文档活动修订、
 * 冻结嵌入空间与维度这些条件必须在 SQL 里先行成立，因此实现只能经
 * {@code KnowledgeRepository} 的具名语句取数，绝不自己算余弦、也绝不在权限条件之外取回再过滤。
 * English summary: {@code KnowledgeSearchStrategy} is the strategy contract for the three API-022 recall algorithms
 * (Rule 9): every implementation declares which {@link KnowledgeSearchModeEnum} it serves, accepts exactly one typed
 * {@link KnowledgeSearchQueryBO} and returns exactly a list of candidate {@link KnowledgeRetrievalHitBO}. The heart of
 * the contract is not how similarity is computed but who may be recalled at all: tenant, knowledge base membership, the
 * document's active revision and the frozen embedding space and dimensions must already hold inside the SQL, so an
 * implementation may only fetch through named {@code KnowledgeRepository} statements — it never computes cosine
 * similarity itself and never pulls rows back to filter afterwards.
 *
 * 用法 / Usage: 实现由容器收集，{@code KnowledgeConfiguration} 归出
 * {@code knowledgeSearchStrategyRegistry}（{@code Map<KnowledgeSearchModeEnum, KnowledgeSearchStrategy>}），
 * {@code KnowledgeRetrievalServiceImpl} 只按枚举查表分发，任何地方都不得对召回模式做 {@code switch}；
 * 表中缺一个成员属于装配缺陷，检索按 422/503 如实失败而不是退回另一种算法。
 * 空结果表示「本路无证据」，与依赖故障必须区分：上游或数据库失败一律抛出异常，不许返回空列表冒充无证据。
 */
@Validated
public interface KnowledgeSearchStrategy {

    /**
     * 中文说明：本实现服务的召回模式，是注册表唯一的键；返回 {@code null} 属于装配缺陷，
     * {@code KnowledgeConfiguration} 在启动期即失败关闭。
     * English summary: The recall mode this implementation serves, the registry's only key; a {@code null} is an
     * assembly defect that {@code KnowledgeConfiguration} fails closed on at startup.
     *
     * 用法 / Usage: 注册表构建时读取，业务侧不据此分支。/ Read while building the registry, never branched on by callers.
     * @return 返回 召回模式；returns the recall mode.
     */
    KnowledgeSearchModeEnum mode();

    /**
     * 中文说明：在授权范围内做一次有界召回：入参已带齐知识库、请求者身份、冻结空间与维度、
     * 本路候选上限与取证范围；返回列表按本路的排序口径给出，条数不超过 {@code candidateLimit}。
     * English summary: Performs one bounded recall inside the authorized scope: the argument already carries the
     * knowledge base, requesting identity, frozen space and dimensions, this leg's candidate ceiling and the evidence
     * scope. The list comes back in this leg's own order and never exceeds {@code candidateLimit}.
     *
     * 用法 / Usage: {@code knowledgeSearchStrategyRegistry.get(mode).search(query)}；
     * 实现必须在事务与锁之外调用（读取用受守卫的只读语句），且不得调用生成模型。
     * @param query 参数 类型化召回入参；parameter the typed recall argument.
     * @return 返回 候选证据列表，空列表表示本路无证据；returns the candidate evidence list, empty meaning this leg found nothing.
     */
    List<@Valid @NotNull KnowledgeRetrievalHitBO> search(@Valid @NotNull KnowledgeSearchQueryBO query);
}
