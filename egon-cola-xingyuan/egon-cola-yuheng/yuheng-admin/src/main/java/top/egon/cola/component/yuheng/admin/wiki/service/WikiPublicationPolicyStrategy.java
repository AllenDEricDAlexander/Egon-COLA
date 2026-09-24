package top.egon.cola.component.yuheng.admin.wiki.service;

import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.wiki.domain.enums.WikiPublicationPolicyEnum;
import top.egon.cola.component.yuheng.admin.wiki.domain.enums.WikiReviewStatusEnum;
import top.egon.cola.component.yuheng.admin.wiki.domain.enums.WikiTransitionEventEnum;

/**
 * 中文说明：{@code WikiPublicationPolicyStrategy} 是 §7.3.7 状态机里「评审维度」这一半的策略契约：状态机自身只持有
 * 发布维度的迁移表（DRAFT/PUBLISHING/PUBLISHED/SUPERSEDED/ARCHIVED），一个冻结在 revision 上的发布策略该从什么评审状态
 * 起步、哪些事件在当前接入水平下真的可执行、迁移之后评审列该落什么值，全部由本契约回答。这样做的目的不是形式上的
 * 多态，而是把「审核流程本期未接入」这一事实收在唯一的扩展点上：新增一个 {@code REVIEW_REQUIRED} 实现即接入评审，
 * 状态机、控制器与 worker 一行都不用改；反之在没有实现时，任何走评审事件的请求都只能失败关闭。
 * English summary: {@code WikiPublicationPolicyStrategy} is the strategy contract for the review half of the §7.3.7 state
 * machine: the machine itself owns only the publication dimension (DRAFT/PUBLISHING/PUBLISHED/SUPERSEDED/ARCHIVED), while a
 * policy frozen onto the revision answers which review status it starts at, which events are executable at the current
 * integration level, and which review value a migration leaves behind. The point is polymorphism for a reason rather than in
 * name: adding a {@code REVIEW_REQUIRED} implementation wires review without touching the machine, a controller or the
 * worker, and while no such implementation exists every review event can only fail closed.
 *
 * 用法 / Usage: 由 {@code wikiPublicationPolicyStrategyRegistry}（{@code Map<WikiPublicationPolicyEnum,
 * WikiPublicationPolicyStrategy>}）按策略枚举登记，{@code WikiLifecycleServiceImpl} 只查表不调分支；
 * 注册表里缺某个策略就意味着该策略不可执行，属于刻意保留的失败关闭而不是遗漏。实现必须是无状态单例，
 * 绝不读写数据库或事务。
 * Registered per policy enum in {@code wikiPublicationPolicyStrategyRegistry} and looked up rather than branched on by
 * {@code WikiLifecycleServiceImpl}; a missing strategy means that policy is not executable, which is a deliberate fail-closed
 * choice and not an omission. An implementation stays a stateless singleton and never touches the database or a transaction.
 */
@Validated
public interface WikiPublicationPolicyStrategy {

    /**
     * 中文说明：声明本策略服务哪一个冻结策略，注册表以此键入表，键重复即启动失败。
     * English summary: Declares which frozen policy this strategy serves; the registry keys on it and a duplicate key fails startup.
     * @return 返回 策略枚举；returns the policy.
     */
    WikiPublicationPolicyEnum policy();

    /**
     * 中文说明：给出该策略下新建草稿的评审状态初值，与 {@code WikiRevisionBO.newDraft} 同一口径。
     * English summary: Gives the review status a freshly drafted revision starts at under this policy, the same rule
     * {@code WikiRevisionBO.newDraft} applies.
     * @return 返回 评审状态初值；returns the initial review status.
     */
    WikiReviewStatusEnum initialReviewStatus();

    /**
     * 中文说明：判断事件在本策略与当前接入水平下是否可执行；评审事件在没有真实适配器时一律返回 {@code false}，
     * 从而让状态机抛出 {@code WIKI_REVIEW_ADAPTER_NOT_CONFIGURED} 而不是替调用方伪造一次批准。
     * English summary: Decides whether an event is executable under this policy at the current integration level; a review event
     * answers {@code false} without a real adapter so the machine raises {@code WIKI_REVIEW_ADAPTER_NOT_CONFIGURED} instead of
     * fabricating an approval on the caller's behalf.
     * @param event 参数 待判定的迁移事件；parameter the event being judged.
     * @return 返回 是否可执行；returns whether the event may run.
     */
    boolean executable(WikiTransitionEventEnum event);

    /**
     * 中文说明：给出迁移之后评审列应当落的值。DIRECT 路径恒为 {@code NOT_REQUIRED}：本方法的存在就是为了
     * 让「发布」与「评审通过」两件事无法互相冒充——即使迁移成功，评审列也不会被写成 {@code APPROVED}。
     * English summary: Gives the review value a migration leaves behind. The DIRECT path always answers
     * {@code NOT_REQUIRED}: this method exists precisely so publication and approval cannot impersonate each other — a
     * successful migration still never writes {@code APPROVED}.
     * @param event 参数 正在执行的迁移事件；parameter the migration being applied.
     * @param current 参数 迁移前的评审状态；parameter the review status before the migration.
     * @return 返回 迁移后的评审状态；returns the review status after the migration.
     */
    WikiReviewStatusEnum reviewStatusAfter(WikiTransitionEventEnum event, WikiReviewStatusEnum current);
}
