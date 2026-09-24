package top.egon.cola.component.yuheng.admin.wiki.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;
import top.egon.cola.component.yuheng.admin.wiki.domain.bo.WikiPageBO;
import top.egon.cola.component.yuheng.admin.wiki.domain.bo.WikiRevisionBO;
import top.egon.cola.component.yuheng.admin.wiki.domain.dto.WikiTransitionCommandDTO;
import top.egon.cola.component.yuheng.admin.wiki.domain.vo.WikiTransitionResultVO;

/**
 * 中文说明：{@code WikiLifecycleService} 是 §7.3.7 状态机与 INTERNAL-001 的公开业务端口。核心入口
 * {@link #transition} 保持原合同签名：入参是携带双版本（页面 revision 与修订状态版本）与 {@code ExecuteGroup}
 * 分组校验的迁移命令，出参是迁移后的事实；权限、租约、来源可发布性与状态合法性都在这里判定，
 * 迁移表只在实现内以枚举键化的只读表求值，绝不散落到控制器或仓储的 {@code if} 分支。
 * 本端口始终汇入调用方已有的 PostgreSQL 事务，绝不使用 {@code REQUIRES_NEW}：一个页面的
 * 「旧行旧化 + 新行状态 + 页面指针 + 审计」必须同 commit 或同回滚。{@link #transitionAll} 将若干条命令收进
 * 同一事务（发布与自愈重试需要成对推进），{@link #saveDraft} 承担页面与草稿的原子落位，
 * {@link #publishGenerated} 是生成 worker 的唯一写入入口（建页、起草、按冻结策略发布，整批一次 commit）。
 * English summary: {@code WikiLifecycleService} is the business port of the §7.3.7 state machine and of INTERNAL-001. The core
 * {@link #transition} keeps the original contract signature: a migration command carrying both versions (the page revision and
 * the revision's status version) under {@code ExecuteGroup} validation in, the post-migration facts out. Permission, lease,
 * source publishability and legality are settled here, the migration table being an enum-keyed read-only table inside the
 * implementation and never a branch in a controller or a repository. This port always joins the caller's PostgreSQL
 * transaction and never {@code REQUIRES_NEW}: one page's 「superseding the old row, migrating the new status, moving the pointers
 * and writing the audit」 commits or rolls back together. {@link #transitionAll} keeps several commands in that same transaction,
 * {@link #saveDraft} places a page and its draft atomically and {@link #publishGenerated} is the generation worker's only write
 * entry — create, draft and publish under the frozen policy, the whole batch in one commit.
 *
 * 用法 / Usage: 由 {@code WikiServiceImpl} 与 {@code WikiGenerationStrategy} 调用，不直接暴露为 HTTP 端点；
 * 任何未接入的评审事件一律 {@code WIKI_REVIEW_ADAPTER_NOT_CONFIGURED} 失败关闭，零行 CAS 一律换算为 409。
 * Reached by the HTTP-facing service and by the generation strategy, never published as an endpoint itself; an unwired review
 * event fails closed and a zero-row compare-and-set always answers a 409.
 */
@Validated
public interface WikiLifecycleService {

    /**
     * 中文说明：执行一次状态迁移（INTERNAL-001 原签名）。
     * English summary: Performs one state migration, the pinned INTERNAL-001 signature.
     * @param command 参数 迁移命令，携带双版本与原因码；parameter the migration command carrying both versions and the reason code.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @return 返回 迁移后的事实；returns the facts after the migration.
     */
    WikiTransitionResultVO transition(
            @Valid @NotNull WikiTransitionCommandDTO command,
            @NotNull AdminActor actor);

    /**
     * 中文说明：在同一事务内按顺序推进若干条迁移命令，任一命令失败即整批回滚。
     * English summary: Advances several migration commands in one transaction, any failure rolling the whole batch back.
     * @param commands 参数 有序迁移命令；parameter the ordered migration commands.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @return 返回 每条命令迁移后的事实；returns the facts after each command.
     */
    List<WikiTransitionResultVO> transitionAll(
            @NotEmpty List<@Valid @NotNull WikiTransitionCommandDTO> commands,
            @NotNull AdminActor actor);

    /**
     * 中文说明：原子落位一个草稿修订：页面主键为空时先建页，再插入 DRAFT 修订并移动草稿指针；
     * 已有草稿时按 REPLACE_DRAFT 归档旧草稿。本方法不发布，发布是显式的状态迁移。
     * English summary: Places one draft revision atomically: creating the page when its key is empty, inserting the DRAFT
     * revision and moving the draft pointer; an existing draft is archived under REPLACE_DRAFT. Nothing is published here,
     * publication being an explicit migration.
     * @param page 参数 目标页面载体，新页主键为空；parameter the target page carrier, its key empty for a new page.
     * @param draft 参数 已冻结内容与策略的草稿载体；parameter the draft carrier with content and policy frozen.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @return 返回 草稿落位后的事实；returns the facts after the draft landed.
     */
    WikiTransitionResultVO saveDraft(
            @NotNull WikiPageBO page,
            @Valid @NotNull WikiRevisionBO draft,
            @NotNull AdminActor actor);

    /**
     * 中文说明：生成 worker 的唯一写入入口：{@code pages} 与 {@code drafts} 按索引一一对应，
     * 页面主键为空表示新建页面；整批在同一事务内建页、起草并按各自冻结策略发布，任一页零行即整批回滚。
     * English summary: The generation worker's only write entry: {@code pages} pairs with {@code drafts} by index, an empty page
     * key meaning a new page; the whole batch creates, drafts and publishes under each revision's frozen policy inside one
     * transaction, any zero row rolling all of it back.
     * @param pages 参数 与草稿同序的页面载体列表；parameter the page carriers in draft order.
     * @param drafts 参数 待落位并发布的草稿载体列表；parameter the draft carriers to place and publish.
     * @param actorId 参数 作业提交者主体标识（撤权即整批失败）；parameter the submitting actor, a revoked membership failing the batch.
     * @return 返回 每次发布迁移后的事实；returns the facts after each publication.
     */
    List<WikiTransitionResultVO> publishGenerated(
            @NotEmpty List<@NotNull WikiPageBO> pages,
            @NotEmpty List<@Valid @NotNull WikiRevisionBO> drafts,
            @NotBlank String actorId);
}
