package top.egon.cola.component.yuheng.admin.llm.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgePageVO;
import top.egon.cola.component.yuheng.admin.llm.domain.dto.LlmChannelCommandDTO;
import top.egon.cola.component.yuheng.admin.llm.domain.dto.LlmModelCommandDTO;
import top.egon.cola.component.yuheng.admin.llm.domain.vo.LlmChannelVO;
import top.egon.cola.component.yuheng.admin.llm.domain.vo.LlmModelVO;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;
import top.egon.cola.component.yuheng.admin.shared.domain.RequestAuditContext;

/**
 * 中文说明：{@code LlmConfigurationService} 是原业务 Spec §9.2.4–§9.2.7（API-004–007）的渠道与模型配置业务合同，
 * 只声明管理面真正使用的四条 typed 查询/命令；租户与操作者由守卫上下文（MDC {@code tenantId}/{@code userId}）决定，
 * 任何方法都不接受调用方自报租户，也不向 Controller 暴露持久化行模型。
 * English summary: {@code LlmConfigurationService} is the channel and model configuration business contract behind
 * API-004–007 of the primary Spec; it declares exactly the four typed query/command operations the management plane uses,
 * keeps tenancy and operator identity inside the guarded MDC context instead of caller-supplied parameters, and never
 * lets a persistence row model reach the controller.
 *
 * 用法 / Usage: 由 {@code LlmConfigurationController} 以限定名注入并按原路径调用；实现类持有写事务与限定协作者，
 * 接口只做声明式校验（default 组约束，因为这些载体未声明 Create/Update 分组）。/ Inject it by qualifier into the
 * controller and call it through the original paths; the implementation owns the write transaction and its qualified
 * collaborators while the interface carries only declarative validation in the default groups.
 */
@Validated
public interface LlmConfigurationService {

    /**
     * 中文说明：API-004 分页只读渠道配置，返回 {@code items/page/size/total} 直接 JSON，不追加 code/data wrapper；
     * 只输出 secretRef 引用名，绝不输出已解析密钥，也不触发任何健康探测。
     * English summary: API-004 reads channels page by page and answers with the bare {@code items/page/size/total} JSON;
     * only the secretRef name is projected, never a resolved secret, and no health probe is issued.
     *
     * 用法 / Usage: {@code llmConfigurationServiceImpl.listChannels(page, size)}；page 从 1 开始，size 有效范围 1–100，
     * 空页返回 {@code []} 而非 null。
     * @param page 参数 页码，从 1 开始；parameter one-based page number.
     * @param size 参数 页大小，1–100；parameter effective page size.
     * @return 返回 渠道分页投影；returns the paged channel projection.
     */
    KnowledgePageVO<LlmChannelVO> listChannels(
            @Min(1) int page,
            @Min(1)
            @Max(100) int size
    );

    /**
     * 中文说明：API-005 在同一短事务内完整保存渠道：路径 {@code channelKey} 与 {@code command.key} 必须一致，
     * 否则按原合同返回 409 版本/key/状态冲突；{@code expectedRevision=0} 且渠道不存在时创建，
     * 已存在时只有等于当前 revision 才允许完整替换，否则抛 409 并携带现值 revision；缺失且 {@code expectedRevision>0} 为 404。
     * 业务层复核 baseUrl 结构（绝对 http/https、无 userinfo/query/fragment、CLOUD 仅 HTTPS）、
     * {@code totalTimeoutMs} 不小于 connect/header/idle 三个 timeout 的关系，以及 LOCAL embedding 反向引用：
     * 任何指向该渠道的 EMBEDDING 模型必须继续落在 deployment=LOCAL 且协议仍在模型 protocols 集合内的渠道上，
     * 违反即状态冲突而不是静默放宽或转云。写操作与审计同事务，停用不删历史引用。
     * English summary: API-005 fully saves one channel inside one short transaction: the path {@code channelKey} and
     * {@code command.key} must agree or the documented 409 key/state conflict is raised; {@code expectedRevision=0}
     * creates a missing channel, an existing channel is only replaceable at its current revision (otherwise 409 carrying
     * that revision), and a missing channel with a positive expected revision is 404. The business layer re-checks the
     * baseUrl shape (absolute http/https without userinfo, query or fragment, HTTPS only for CLOUD), the rule that
     * {@code totalTimeoutMs} dominates the connect/header/idle timeouts, and the LOCAL embedding reverse references: every
     * EMBEDDING model routing at this channel must keep pointing at a LOCAL deployment channel whose protocol still sits
     * inside the model protocol set, and a violation is a state conflict rather than a silent widening or a cloud
     * fallback. The audit row shares the write transaction and disabling never deletes historical references.
     *
     * 用法 / Usage: {@code llmConfigurationServiceImpl.replaceChannel(channelKey, command, actor, request)}；
     * 返回体即已提交投影（{@code revision} 为服务端推进后的正整数），创建与替换由 {@code expectedRevision} 表达意图，
     * 控制器据此决定 201+Location 或 200。
     * @param channelKey 参数 路径渠道稳定 key；parameter channel stable key from the path.
     * @param command 参数 完整保存命令；parameter the full replace command.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param request 参数 请求审计上下文；parameter the request audit context.
     * @return 返回 已提交的渠道投影；returns the committed channel projection.
     */
    LlmChannelVO replaceChannel(
            @NotBlank
            @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,63}$") String channelKey,
            @Valid
            @NotNull LlmChannelCommandDTO command,
            @NotNull AdminActor actor,
            @NotNull RequestAuditContext request
    );

    /**
     * 中文说明：API-006 分页只读模型 alias 与能力映射；枚举按原 wire 字符串输出，routes 复用
     * {@code LlmRouteBindingDTO} 一条顶层载体，CHAT 的 dimensions/embeddingSpaceId 保持 null。
     * English summary: API-006 reads model aliases and their capability routing page by page; enums stay on their original
     * wire strings, routes reuse the top-level {@code LlmRouteBindingDTO}, and CHAT keeps dimensions and the embedding
     * space identifier null.
     *
     * 用法 / Usage: {@code llmConfigurationServiceImpl.listModels(page, size)}。
     * @param page 参数 页码，从 1 开始；parameter one-based page number.
     * @param size 参数 页大小，1–100；parameter effective page size.
     * @return 返回 模型分页投影；returns the paged model projection.
     */
    KnowledgePageVO<LlmModelVO> listModels(
            @Min(1) int page,
            @Min(1)
            @Max(100) int size
    );

    /**
     * 中文说明：API-007 在同一短事务内完整保存模型 alias：路径 {@code modelKey} 与 {@code command.key} 必须一致；
     * 版本、创建/替换与 404/409 规则与渠道完全同构。业务层在库内复核：每条 route 必须指向已存在且协议落在
     * {@code protocols} 集合内的渠道，EMBEDDING 模型的每条 route 还必须落在 deployment=LOCAL 的渠道上，
     * 失败按字段不合法 422 拒绝而不是扩大到全部渠道；已使用嵌入空间的模型不得改变
     * {@code embeddingSpaceId}/{@code dimensions} 语义，需要新 alias。写操作与审计同事务。
     * English summary: API-007 fully saves one model alias inside one short transaction and the path {@code modelKey} must
     * equal {@code command.key}; revision, create-versus-replace and the 404/409 rules are identical to the channel
     * operation. Against the database the business layer re-checks that every route addresses an existing channel whose
     * protocol belongs to {@code protocols}, that every route of an EMBEDDING model stays on a LOCAL deployment channel,
     * rejecting violations as a 422 field failure instead of widening to all channels, and that a model already bound to
     * an embedding space can neither change {@code embeddingSpaceId} nor {@code dimensions} but requires a new alias. The
     * audit row shares the write transaction.
     *
     * 用法 / Usage: {@code llmConfigurationServiceImpl.replaceModel(modelKey, command, actor, request)}。
     * @param modelKey 参数 路径模型稳定 key；parameter model stable key from the path.
     * @param command 参数 完整保存命令；parameter the full replace command.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param request 参数 请求审计上下文；parameter the request audit context.
     * @return 返回 已提交的模型投影；returns the committed model projection.
     */
    LlmModelVO replaceModel(
            @NotBlank
            @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,63}$") String modelKey,
            @Valid
            @NotNull LlmModelCommandDTO command,
            @NotNull AdminActor actor,
            @NotNull RequestAuditContext request
    );
}
