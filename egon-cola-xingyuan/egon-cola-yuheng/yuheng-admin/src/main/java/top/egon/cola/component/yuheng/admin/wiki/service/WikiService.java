package top.egon.cola.component.yuheng.admin.wiki.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeJobVO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgePageVO;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;
import top.egon.cola.component.yuheng.admin.wiki.domain.dto.WikiDraftCommandDTO;
import top.egon.cola.component.yuheng.admin.wiki.domain.dto.WikiGenerationCommandDTO;
import top.egon.cola.component.yuheng.admin.wiki.domain.dto.WikiPageQueryDTO;
import top.egon.cola.component.yuheng.admin.wiki.domain.dto.WikiPublicationCommandDTO;
import top.egon.cola.component.yuheng.admin.wiki.domain.vo.WikiGraphVO;
import top.egon.cola.component.yuheng.admin.wiki.domain.vo.WikiPageVO;

/**
 * 中文说明：{@code WikiService} 是 Wiki 管理面唯一的业务端口，逐字承载 API-023 至 API-028 与 API-031 的
 * 合同形态：每个方法先接受已验证的 {@link AdminActor}，再按十进制字符串 id 与 typed DTO 进、class VO 出，
 * 行模型、MyBatis-Plus 包装器与持久化转换器都不出现在这里。成员的读取者角色（READER）与写入者角色
 * （EDITOR）由实现复核，端口本身只声明值域与形态；{@code expectedRevision} 一律是调用方观察到的乐观版本，
 * 不一致即 409 并回报现值，绝不静默覆盖。
 * 发布与下线都不接受调用方自报的 {@code publicationStatus}、{@code reviewerActorId} 或时间戳——那类字段
 * 只能由状态机与受守卫边界写入。
 * English summary: {@code WikiService} is the Wiki management side's only business port, carrying the API-023 through API-028
 * and API-031 contracts in their exact shape: every method takes the verified {@link AdminActor} first, then a decimal-string
 * id and a typed DTO in and hands back a class VO, with no row model, MyBatis-Plus wrapper or persistence converter appearing
 * here. Reader (READER) and writer (EDITOR) membership is re-checked by the implementation while the port declares value ranges
 * and shapes only; {@code expectedRevision} is always the caller-observed optimistic version, a mismatch being a 409 reporting
 * the current value and never a silent overwrite. Neither publication nor unpublishing accepts a caller-declared
 * {@code publicationStatus}, {@code reviewerActorId} or timestamp — those columns are written by the state machine and the
 * guarded boundary alone.
 *
 * 用法 / Usage: 只由 {@code WikiController} 按 {@code @Qualifier("wikiServiceImpl")} 注入调用；
 * 状态的原子迁移一律经 {@link WikiLifecycleService}，本端口不重复实现状态机。
 * Injected only by {@code WikiController}; every atomic status migration goes through {@link WikiLifecycleService} and this port
 * never re-implements the state machine.
 */
@Validated
public interface WikiService {

    /**
     * 中文说明：API-023 目录分页（READER）：{@code includeDraft=true} 必须 EDITOR，否则按 403 拒绝而不是悄悄降级；
     * 检索命中 slug 或标题的字面子串，标签为精确匹配，当页与总数共用同一谓词。
     * English summary: API-023 pages the catalog (READER): {@code includeDraft=true} demands EDITOR and is refused as 403
     * rather than quietly downgraded, search matches a literal substring of slug or title, the tag matches exactly, and the page
     * and the total share one predicate.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param query 参数 分页与筛选载体；parameter the paging and filter carrier.
     * @return 返回 页面分页投影；returns the paged page projection.
     */
    KnowledgePageVO<WikiPageVO> listPages(
            @NotNull AdminActor actor,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @Valid @NotNull WikiPageQueryDTO query
    );

    /**
     * 中文说明：API-024 排队一个生成作业（EDITOR）：来源集合必须是本库当前活动 revision 的子集，
     * 本方法只建立 {@code WIKI_GENERATE} 作业行并返回 {@code QUEUED} 投影，模型调用与批次发布都发生在
     * worker 侧，绝不在请求线程内同步生成。
     * English summary: API-024 queues a generation job (EDITOR): the source set must be a subset of this base's current active
     * revisions, and this method only creates the {@code WIKI_GENERATE} job row and returns its {@code QUEUED} projection — the
     * model call and the batch publication happen on the worker side and are never generated synchronously on the request
     * thread.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param command 参数 生成命令；parameter the generation command.
     * @param idempotencyKey 参数 {@code Idempotency-Key} 请求头值，16–64 ASCII；parameter the
     *                       {@code Idempotency-Key} header value, 16–64 ASCII characters.
     * @return 返回 已排队的作业投影；returns the queued job projection.
     */
    KnowledgeJobVO createGenerationJob(
            @NotNull AdminActor actor,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @Valid @NotNull WikiGenerationCommandDTO command,
            @NotBlank
            @Size(min = 16, max = 64)
            @Pattern(regexp = "^[ -~]{16,64}$") String idempotencyKey
    );

    /**
     * 中文说明：API-025 读取单页（READER）：{@code revisionId} 给出时必须属于该页面，缺省读当前发布版；
     * 草稿版只对 EDITOR 可读，来源被撤回时正文按 403/404 如实拒绝而不是返回空壳。
     * English summary: API-025 reads one page (READER): a supplied {@code revisionId} must belong to that page and the default
     * is the current publication, a draft being EDITOR-only and a withdrawn source refusing the body honestly as 403 or 404
     * rather than as an empty shell.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param pageId 参数 页面十进制字符串 id；parameter decimal-string page id.
     * @param revisionId 参数 可选的修订十进制字符串 id；parameter optional decimal-string revision id.
     * @return 返回 页面投影；returns the page projection.
     */
    WikiPageVO getPage(
            @NotNull AdminActor actor,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String pageId,
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String revisionId
    );

    /**
     * 中文说明：API-026 保存人工修订（EDITOR）：slug 不可改，新草稿经状态机落位并被替换的旧草稿归档，
     * {@code expectedRevision} 必须是调用方读到的页面版本。
     * English summary: API-026 stores a human revision (EDITOR): the slug never changes, the new draft is placed through the
     * state machine and the draft it replaces is archived, and {@code expectedRevision} must be the page version the caller read.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param pageId 参数 页面十进制字符串 id；parameter decimal-string page id.
     * @param command 参数 草稿命令；parameter the draft command.
     * @return 返回 保存后的页面投影；returns the page projection after the save.
     */
    WikiPageVO replaceDraft(
            @NotNull AdminActor actor,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String pageId,
            @Valid @NotNull WikiDraftCommandDTO command
    );

    /**
     * 中文说明：API-027 直接发布一个草稿（EDITOR）：DIRECT 策略下由状态机走
     * DRAFT→PUBLISHING→PUBLISHED 两步，来源失鲜按 409 {@code WIKI_SOURCE_STALE} 拒绝，
     * 同一草稿重复发布幂等地回报当前发布结果而不再次推进版本。
     * English summary: API-027 publishes one draft directly (EDITOR): under the DIRECT policy the state machine walks
     * DRAFT→PUBLISHING→PUBLISHED in two steps, a stale source is refused as 409 {@code WIKI_SOURCE_STALE}, and republishing the
     * same draft idempotently reports the current publication without advancing the version again.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param pageId 参数 页面十进制字符串 id；parameter decimal-string page id.
     * @param command 参数 发布命令；parameter the publication command.
     * @return 返回 发布后的页面投影；returns the page projection after publication.
     */
    WikiPageVO publishRevision(
            @NotNull AdminActor actor,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String pageId,
            @Valid @NotNull WikiPublicationCommandDTO command
    );

    /**
     * 中文说明：API-028 关联图（READER）：节点与边只由当前已发布修订闭合，一跳之内至多 100 节点 / 200 边，
     * 越权或未发布的页面连标题都不出现；被裁掉时 {@code truncated=true} 如实报告。
     * English summary: API-028 answers the graph (READER): nodes and edges close over currently published revisions only, at most
     * 100 nodes and 200 edges within one hop, an unauthorized or unpublished page contributes not even its title, and a trimmed
     * answer reports itself through {@code truncated=true}.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param pageId 参数 可选的中心页面 id；parameter optional centre page id.
     * @param limit 参数 节点上限，1–100；parameter the node ceiling, 1–100.
     * @return 返回 图投影；returns the graph projection.
     */
    WikiGraphVO graph(
            @NotNull AdminActor actor,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String pageId,
            @Min(1)
            @Max(100) int limit
    );

    /**
     * 中文说明：API-031 下线当前发布版（EDITOR 且持有知识写能力）：页面指针与修订状态在同一事务内原子切换，
     * 旧修订归档但不复活；已无发布版时幂等地什么都不做，{@code expectedRevision} 必须是页面现值。
     * English summary: API-031 withdraws the current publication (EDITOR holding the knowledge write capability): the page
     * pointer and the revision status switch atomically in one transaction and the retired revision is archived without ever
     * reviving; with no publication left the call is an idempotent no-op, and {@code expectedRevision} must be the page's current
     * value.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param kbId 参数 知识库十进制字符串 id；parameter decimal-string knowledge base id.
     * @param pageId 参数 页面十进制字符串 id；parameter decimal-string page id.
     * @param expectedRevision 参数 调用方观察到的页面 revision，至少 1；parameter the caller-observed page revision, at least one.
     */
    void unpublish(
            @NotNull AdminActor actor,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String kbId,
            @NotBlank
            @Pattern(regexp = "^[1-9][0-9]{0,19}$") String pageId,
            @Min(1) long expectedRevision
    );
}
