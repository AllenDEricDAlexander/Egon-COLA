package top.egon.cola.component.yuheng.admin.wiki.service;

import java.util.EnumSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.wiki.domain.enums.WikiPublicationPolicyEnum;
import top.egon.cola.component.yuheng.admin.wiki.domain.enums.WikiReviewStatusEnum;
import top.egon.cola.component.yuheng.admin.wiki.domain.enums.WikiTransitionEventEnum;

/**
 * 中文说明：{@code DirectWikiPublicationPolicyStrategy} 是本期唯一登记的发布策略：草稿由编辑者自行发布，
 * 全程没有评审环节。它对状态机只回答三件事——评审初值是 {@code NOT_REQUIRED}、评审类事件一律不可执行、
 * 任何迁移之后评审列仍然是 {@code NOT_REQUIRED}。第三点是本类的全部价值所在：DIRECT 让一篇 Wiki 从 DRAFT 走到
 * PUBLISHED，却永远不会顺手把 {@code review_status} 写成 {@code APPROVED}，也不会补一个审核人或审核时刻，
 * 因为系统里根本没有做出过这个判断（Spec §7.3.7「不会创造一个假的系统审核员」）。
 * 因此审计里的 {@code reviewDecisionCode} 恒为 {@code NOT_REQUIRED}，下游任何按 APPROVED 判定可信度的逻辑
 * 都不可能被一次自动发布骗到。
 * English summary: {@code DirectWikiPublicationPolicyStrategy} is the only publication policy registered this release: an
 * editor publishes a draft directly and no review step exists. It answers exactly three questions for the state machine — the
 * review status starts at {@code NOT_REQUIRED}, review events are never executable, and every migration leaves the review
 * column at {@code NOT_REQUIRED}. That third answer is the whole point of the class: DIRECT walks a Wiki from DRAFT to
 * PUBLISHED without ever writing {@code APPROVED}, without inventing a reviewer and without inventing a review instant,
 * because no such judgement was made (Spec §7.3.7, "the system never conjures a fake reviewer"). The audit's
 * {@code reviewDecisionCode} is therefore always {@code NOT_REQUIRED}, so no downstream trust rule keyed on APPROVED can be
 * fooled by an automatic publication.
 *
 * 用法 / Usage: 以 bean 名 {@code directWikiPublicationPolicyStrategy} 注册，由
 * {@code wikiPublicationPolicyStrategyRegistry} 按 {@link #policy()} 归入 {@code EnumMap} 后被
 * {@code WikiLifecycleServiceImpl} 查表使用；本类无状态、不注入协作者，也不触碰数据库或事务。
 * REVIEW_REQUIRED 没有实现，故在注册表里缺席，其 revision 的评审事件与发布一律由状态机失败关闭。
 * Registered as {@code directWikiPublicationPolicyStrategy}, keyed into the {@code EnumMap} by {@link #policy()} and reached
 * through a lookup in the lifecycle service; the class is stateless, injects nothing and touches neither database nor
 * transaction. REVIEW_REQUIRED has no implementation and so stays absent from the registry, which is what makes its review
 * events and publications fail closed.
 */
@Slf4j
@Validated
@Service("directWikiPublicationPolicyStrategy")
public class DirectWikiPublicationPolicyStrategy implements WikiPublicationPolicyStrategy {

    /** 评审环节的事件集合，DIRECT 策略下一律不可执行 / the review-stage events, none of which the DIRECT policy may execute. */
    private static final Set<WikiTransitionEventEnum> REVIEW_EVENTS = EnumSet.of(
            WikiTransitionEventEnum.SUBMIT_REVIEW,
            WikiTransitionEventEnum.APPROVE,
            WikiTransitionEventEnum.REJECT,
            WikiTransitionEventEnum.CANCEL_REVIEW,
            WikiTransitionEventEnum.REVIEWED_PUBLISH
    );

    /**
     * 中文说明：声明本策略服务 {@code DIRECT}，注册表以此键入表。
     * English summary: Declares {@code DIRECT} as the policy served, which is the registry key.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code directWikiPublicationPolicyStrategy.policy()}。
     * @return 返回 策略枚举；returns the policy.
     */
    @Override
    public WikiPublicationPolicyEnum policy() {
        return WikiPublicationPolicyEnum.DIRECT;
    }

    /**
     * 中文说明：执行 initialReviewStatus 操作，DIRECT 草稿的评审初值是 {@code NOT_REQUIRED}，
     * 而不是「未提交」——本期没有评审环节，把它标成未提交会让一个永远不会发生的环节看起来近在眼前。
     * English summary: Executes the initialReviewStatus operation. A DIRECT draft starts at {@code NOT_REQUIRED} rather than
     * "not submitted": with no review stage in this release, "not submitted" would advertise a step that never happens.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code directWikiPublicationPolicyStrategy.initialReviewStatus()}，
     * 与 {@code WikiRevisionBO.newDraft} 的静态口径一致。/ The same value {@code WikiRevisionBO.newDraft} derives.
     * @return 返回 评审状态初值；returns the initial review status.
     */
    @Override
    public WikiReviewStatusEnum initialReviewStatus() {
        return WikiReviewStatusEnum.NOT_REQUIRED;
    }

    /**
     * 中文说明：执行 executable 操作，除评审类事件外全部可执行；评审事件返回 {@code false} 后由状态机
     * 抛出 {@code WIKI_REVIEW_ADAPTER_NOT_CONFIGURED}，本类不自己决定错误码，也不替调用方放行。
     * English summary: Executes the executable operation: everything but the review events, which answer {@code false} so the
     * machine raises {@code WIKI_REVIEW_ADAPTER_NOT_CONFIGURED}. This class neither picks the error code nor waves a caller
     * through on its own.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code directWikiPublicationPolicyStrategy.executable(event)}。
     * @param event 参数 待判定事件；parameter the event being judged.
     * @return 返回 是否可执行；returns whether the event may run.
     */
    @Override
    public boolean executable(WikiTransitionEventEnum event) {
        boolean executable = !REVIEW_EVENTS.contains(event);
        if (!executable) {
            log.debug("wiki publication policy DIRECT refuses the unwired review event {}", event.wireValue());
        }
        return executable;
    }

    /**
     * 中文说明：执行 reviewStatusAfter 操作，无论哪个迁移都原样保持 {@code NOT_REQUIRED}；
     * 迁移前的值只在被显式带过来时透传，DIRECT 从不产生 APPROVED/REJECTED/PENDING。
     * English summary: Executes the reviewStatusAfter operation, keeping {@code NOT_REQUIRED} whatever the migration; the value
     * before the migration is passed through only when it was set explicitly, and DIRECT never manufactures
     * APPROVED/REJECTED/PENDING.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code directWikiPublicationPolicyStrategy.reviewStatusAfter(event, current)}。
     * @param event 参数 正在执行的迁移事件；parameter the migration being applied.
     * @param current 参数 迁移前的评审状态；parameter the review status before the migration.
     * @return 返回 迁移后的评审状态；returns the review status after the migration.
     */
    @Override
    public WikiReviewStatusEnum reviewStatusAfter(WikiTransitionEventEnum event, WikiReviewStatusEnum current) {
        if (!WikiReviewStatusEnum.NOT_REQUIRED.equals(current)) {
            log.debug("wiki publication policy DIRECT normalizes review status {} to NOT_REQUIRED on {}",
                    current == null ? "absent" : current.wireValue(), event.wireValue());
        }
        return WikiReviewStatusEnum.NOT_REQUIRED;
    }
}
