package top.egon.cola.component.yuheng.llm.proxy.service;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.llm.proxy.domain.dto.LlmInvocationCommandDTO;
import top.egon.cola.component.yuheng.llm.proxy.domain.vo.LlmInvocationResultVO;

/**
 * 中文说明：{@code LlmInvocationService} 是 LLM engine 唯一的模型调用入口：一次已校验、已带上可信身份封套的命令
 * 换来一份原生结果。它只暴露这一个业务动作，因此 controller 看不到任何持久化 PO、DAO 或 MyBatis 类型，也无法把
 * 「选了哪个渠道」这件事推给 web 层。授权、alias 启停、协议/能力准入、出域与本地 embedding 约束全部在实现内完成，
 * 顺序固定为「先过滤、后加权」；空候选集一律拒绝，绝不扩大到全部渠道。
 * English summary: {@code LlmInvocationService} is the LLM engine's single model invocation entry: one validated command
 * carrying the trusted identity envelope yields one native result. It exposes exactly that business action, so the
 * controller sees no persistence PO, DAO or MyBatis type and cannot push "which channel was chosen" into the web layer.
 * Authorization, alias enablement, protocol/capability admission, egress and the local-embedding constraint all happen
 * inside the implementation in a fixed filter-then-weight order; an empty candidate set is always rejected and never
 * widened to all channels.
 *
 * 用法 / Usage: 由 {@code LlmApiController}（Step 11）以默认校验组调用；实现抛出的
 * {@code CommonException#getCode()} 即原生 HTTP 状态，由路由对应的原生错误编码器出网，禁止复用 admin 业务错误 wrapper。
 */
@Validated
public interface LlmInvocationService {

    /**
     * 中文说明：按固定的求交顺序（已授权 alias ∩ alias 启用 ∩ 请求协议与端点能力 ∩ 出域限制 ∩ 显式 bindings ∩
     * 渠道启用/白名单）挑选候选，然后提交一次同协议尝试：短只读事务内取一致快照、事务结束后再发请求，整个请求只有一
     * 个 deadline 且最多两次安全尝试，已提交之后不重试。任何本地 embedding 失败都直接以原生错误终止，绝不转投云端；
     * 上游失败也不伪造空成功。
     * English summary: Selects candidates through the fixed intersection order (authorized alias ∩ alias enabled ∩
     * requested protocol and endpoint capabilities ∩ egress limit ∩ explicit bindings ∩ channel enablement/allow-list),
     * then commits one same-protocol attempt: a coherent snapshot taken in a short read-only transaction, the request
     * issued only after that transaction ends, one deadline for the whole request and at most two safe attempts with no
     * retry after commit. Any local embedding failure terminates natively and is never re-routed to a cloud channel, and
     * an upstream failure is never reported as an empty success.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code LlmInvocationService.invoke(command)}。
     * @param command 参数 已校验的调用命令（默认校验组，非 null）；parameter the validated command, default group, never null.
     * @return 返回 原生状态、安全响应头与有界 body publisher；returns the native status, safe headers and bounded body publisher.
     * @throws top.egon.cola.component.common.core.exception.CommonException 403 未授权 alias、404 alias 缺失或停用、
     * 422 embedding 被配成云端、503 无可用路由或引擎未启用、504 超出 deadline；carries the native HTTP status in its code.
     */
    LlmInvocationResultVO invoke(@Valid LlmInvocationCommandDTO command);
}
