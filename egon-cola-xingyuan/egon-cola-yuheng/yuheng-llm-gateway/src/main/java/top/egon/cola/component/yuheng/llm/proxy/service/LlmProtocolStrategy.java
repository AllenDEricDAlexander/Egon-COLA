package top.egon.cola.component.yuheng.llm.proxy.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;
import top.egon.cola.component.yuheng.llm.proxy.domain.bo.LlmModelSnapshotBO;
import top.egon.cola.component.yuheng.llm.proxy.domain.dto.LlmInvocationCommandDTO;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmProtocolEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.exception.LlmInvocationException;
import top.egon.cola.component.yuheng.llm.proxy.domain.vo.LlmInvocationResultVO;

/**
 * 中文说明：{@code LlmProtocolStrategy} 是模型入口的<b>协议面 SPI</b>：一个实现就是一个协议的适配器，四个实现
 * （{@code openAiChatProtocolStrategy}、{@code openAiEmbeddingProtocolStrategy}、
 * {@code openAiResponsesProtocolStrategy}、{@code anthropicMessagesProtocolStrategy}）由
 * {@code llmProtocolStrategyRegistry} 的四条固定映射按名限定，注册方在构造期用 {@link #protocol()} 建只读
 * EnumMap，重复或缺项即失败关闭——因此「按协议写 if/switch」在这条链上不可能重现（Rule 9）。本 SPI 把 Spec 交给
 * 协议面的四项职责拆成四个<b>显式操作</b>，加上身份与唯一的编排入口：
 * ① {@link #encodeRequest} 请求编码——在已解析但未按协议校验的原生 {@link ObjectNode} 上核对已知必需字段、
 * 拒绝未声明的请求扩展与任意 {@code upstream_url}/{@code credential}/{@code host} 之类自证字段，把客户端 alias
 * 改写为 route 的 {@code upstreamModel}，并按 {@code stream} 位产出协议原生请求体（Chat 的
 * {@code messages}/{@code tools}/{@code tool_choice}/{@code stream_options}、Embeddings 的有序
 * {@code input}+{@code encoding_format:"float"}、Responses 的 {@code input} items+{@code instructions}+
 * {@code store=false}、Messages 的顶层 {@code system}+content blocks）；
 * ② {@link #decodeUnaryResponse} 单播解码/透传——保持原生 JSON 保真：Chat 的 {@code choices[].message}/
 * {@code finish_reason}/{@code usage}、Embedding 的 {@code data[].embedding}+{@code index}（长度必须恰等于
 * alias 声明的 {@code dimensions}）、Responses 的类型化 {@code output} items 与
 * {@code function_call}/{@code function_call_output} 的 {@code call_id} 配对及 opaque {@code reasoning} 回放、
 * Messages 的 {@code content} blocks/{@code tool_use}/{@code thinking} 签名/{@code stop_reason}/{@code usage}，
 * 未知但安全的响应字段一律保留、绝不静默丢弃，并把 {@code model} 回写为客户端 alias；
 * ③ {@link #encodeStream} 流式帧解码与编码——解析上游帧文法再按同协议重新出帧：Chat 用
 * {@code data:} 分片、{@code delta.tool_calls[].index} 与终态 {@code finish_reason} 之后的 {@code data: [DONE]}，
 * Responses 用带 {@code type}/{@code sequence_number} 的类型化事件并以 {@code response.completed}/
 * {@code response.incomplete}/{@code response.failed} 为终态，Messages 用
 * {@code message_start}→{@code content_block_start}/{@code content_block_delta}/
 * {@code content_block_stop}→{@code message_delta}→{@code message_stop}（允许 {@code ping}，
 * {@code input_json_delta.partial_json} 不得半段强解析），并在 EOF 缺少本协议终态标记时判为 ABORTED；
 * ④ {@link #encodeError} 原生错误编码——按协议产出错误对象：OpenAI 三面的错误对象只有顶层 {@code error} 一个键，
 * 其内层携带 {@code message}、{@code type}、{@code param} 与 {@code code}；Messages 的顶层 {@code type} 为
 * {@code error}，其 {@code error} 子对象携带 {@code type} 与 {@code message}，且永不套上 OpenAI 的 error/choices 形状，
 * 全部只由 {@link LlmInvocationException} 的四个安全字段驱动，禁止使用 admin 业务错误 wrapper，
 * 禁止回显 token、密钥、baseUrl、upstreamModel 或完整请求正文。
 * 编排入口 {@link #exchange} 只此一个：它负责用渠道的 {@code baseUrl}/{@code secretRef}/超时预算发起同协议上游调用，
 * 并<b>在提交前</b>完成「一个完整合法帧」的验证——首帧验证通过才进入 COMMITTED，此后任何失败都只能终止本流，
 * 既不得换模型或重试，也不得改变 HTTP 状态，更不得伪造 {@code [DONE]}；取消必须传播到上游，每个
 * {@link DataBuffer} 恰好释放一次（既不允许把整条流缓存成 {@code byte[]}，也不允许漏放）。单帧与整请求上限来自
 * {@code yuheng.llm.max-frame-bytes}/{@code yuheng.llm.max-request-bytes}，凭据只来自
 * {@code yuheng.llm.secrets-root} 下对 {@code secretRef} 的解析，两者都由实现自己按名注入。
 * 本接口是 SPI 契约而非业务 Bean：它<b>不</b>带 {@code @Service}/{@code @Component}、<b>不</b>带 Lombok 数据注解，
 * 事务、协作者与上游资源生命周期一律归实现，接口内不放任何默认业务分支。
 * English summary: {@code LlmProtocolStrategy} is the <b>protocol face SPI</b> of the model entry: one implementation is
 * one protocol's adapter, and the four implementations ({@code openAiChatProtocolStrategy},
 * {@code openAiEmbeddingProtocolStrategy}, {@code openAiResponsesProtocolStrategy},
 * {@code anthropicMessagesProtocolStrategy}) are qualified by name through the fixed four-entry
 * {@code llmProtocolStrategyRegistry}, whose consumer builds a read-only EnumMap from {@link #protocol()} at
 * construction time and fails closed on a duplicate or a missing entry — which is why "branch on protocol" cannot creep
 * back into this chain (Rule 9). This SPI splits the four duties the Spec assigns to the protocol face into four
 * <b>explicit operations</b>, plus an identity and exactly one orchestration entry: (1) {@link #encodeRequest} request
 * encoding — verify the known required fields on the parsed-but-not-yet-protocol-validated native {@link ObjectNode},
 * reject undeclared request extensions and self-asserted {@code upstream_url}/{@code credential}/{@code host} style
 * fields, rewrite the client alias into the route's {@code upstreamModel} and emit the native request body per the
 * {@code stream} flag (Chat's {@code messages}/{@code tools}/{@code tool_choice}/{@code stream_options}, the ordered
 * {@code input} plus {@code encoding_format:"float"} for Embeddings, {@code input} items plus {@code instructions} plus
 * {@code store=false} for Responses, and the top-level {@code system} plus content blocks for Messages);
 * (2) {@link #decodeUnaryResponse} unary decoding and pass-through with native JSON fidelity — Chat
 * {@code choices[].message}/{@code finish_reason}/{@code usage}, Embedding {@code data[].embedding} with
 * {@code index} whose length must equal the alias-declared {@code dimensions}, Responses typed {@code output} items with
 * {@code function_call}/{@code function_call_output} {@code call_id} pairing and opaque {@code reasoning} replay, and
 * Messages {@code content} blocks/{@code tool_use}/{@code thinking} signature/{@code stop_reason}/{@code usage};
 * unknown but safe response fields are always preserved and never silently dropped, and {@code model} is written back
 * to the client alias; (3) {@link #encodeStream} streaming frame decoding and encoding — parse the upstream frame
 * grammar and re-emit it in the same protocol: Chat {@code data:} chunks with {@code delta.tool_calls[].index} and
 * {@code data: [DONE]} after the terminal {@code finish_reason}, Responses typed events carrying {@code type} and
 * {@code sequence_number} terminated by {@code response.completed}/{@code response.incomplete}/{@code response.failed},
 * Messages {@code message_start}→{@code content_block_start}/{@code content_block_delta}/{@code content_block_stop}→
 * {@code message_delta}→{@code message_stop} ({@code ping} allowed, {@code input_json_delta.partial_json} never
 * half-parsed), with an EOF that lacks this protocol's terminal marker judged ABORTED; and (4) {@link #encodeError}
 * native error encoding — an OpenAI error object whose only top-level key is {@code error} and whose inner object
 * carries {@code message}, {@code type}, {@code param} and {@code code} for the three OpenAI faces, and a Messages
 * object whose top-level {@code type} is {@code error} with an {@code error} sub-object carrying {@code type} and
 * {@code message}, never dressed in an
 * OpenAI error/choices shape, all driven solely by the four safe fields of {@link LlmInvocationException}, never by the
 * admin business wrapper and never echoing a token, secret, base URL, upstream model or the full request payload.
 * {@link #exchange} is the single orchestration entry: it issues the same-protocol upstream call with the channel's
 * {@code baseUrl}/{@code secretRef}/timeout budget and validates <b>one complete frame before commit</b> — only a
 * validated first frame enters COMMITTED, after which any failure may only end this stream, never switch model or
 * retry, never alter the HTTP status and never fabricate {@code [DONE]}; cancellation propagates upstream and every
 * {@link DataBuffer} is released exactly once (neither buffering a whole stream into {@code byte[]} nor leaking a
 * release is allowed). The per-frame and per-request ceilings come from
 * {@code yuheng.llm.max-frame-bytes}/{@code yuheng.llm.max-request-bytes} and credentials only from resolving
 * {@code secretRef} under {@code yuheng.llm.secrets-root}, both injected by name inside the implementation. This is an
 * SPI contract and not a business bean: it carries <b>no</b> {@code @Service}/{@code @Component}, <b>no</b> Lombok data
 * annotations, and no default business branches — transactions, collaborators and upstream resource lifecycle belong to
 * the implementation.
 *
 * 用法 / Usage: 由 {@code LlmApiController} 按 {@code LlmProtocolEnum} 从注册表取到具名实现后调用：先
 * {@code invoke(command)} 完成路由，再 {@code exchange(command, route, routed)} 打开同协议上游并按协议出网；
 * 四个编解码操作同时供 {@code LlmProtocolContractTest} 在无真实模型的情况下直接固定原生 wire shape。
 * 线程池饱和的 {@code TaskRejectedException} 必须由协议面在<b>提交前</b>翻译成 429，而不是排队等待。
 * / Resolved by {@code LlmApiController} from the registry by {@code LlmProtocolEnum}: {@code invoke(command)} routes
 * first, then {@code exchange(command, route, routed)} opens the same-protocol upstream and leaves natively; the four
 * codec operations are additionally driven directly by {@code LlmProtocolContractTest} to pin the native wire shape
 * without a live model. A saturated {@code TaskRejectedException} must be translated into a pre-commit 429 by the
 * protocol face rather than waited on.
 */
@Validated
public interface LlmProtocolStrategy {

    /**
     * 中文说明：本实现唯一负责的协议，是注册表自检与「一协议一适配器」不变式的依据：注册方必须用它把具名 bean
     * 归入 EnumMap，若某个 bean 声明的协议与其注册键不符（或同一协议出现两个实现）即启动失败，绝不回落 Chat。
     * English summary: The single protocol this implementation owns, which is the basis of the registry self-check and of
     * the one-protocol-one-adapter invariant: the consumer files each named bean into an EnumMap under this value, and a
     * bean whose declared protocol disagrees with its registry key, or two adapters for one protocol, fails startup
     * rather than falling back to Chat.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code LlmProtocolStrategy.protocol()}。
     * @return 返回 本实现服务的唯一协议，永不为 null；returns the single protocol this adapter serves, never null.
     */
    LlmProtocolEnum protocol();

    /**
     * 中文说明：职责①请求编码。在不动其它字段结构与数值表示的前提下，把 {@code payload.model} 从客户端 alias
     * 改写为 {@code route.upstreamModel}，核对协议已知必需字段（含各协议自己的输出预算字段），拒绝未声明能力的
     * 请求扩展与任何自证出网/凭据字段，并按 {@code command.stream} 产出该协议的原生请求体；整个文档大小必须在
     * {@code yuheng.llm.max-request-bytes} 之内。
     * English summary: Duty (1) request encoding. Without touching any other field's structure or numeric
     * representation, rewrite {@code payload.model} from the client alias to {@code route.upstreamModel}, verify the
     * protocol's known required fields (including that protocol's own output-budget field), reject request extensions
     * whose capability was not declared plus any self-asserted egress or credential field, and emit that protocol's
     * native request body according to {@code command.stream}; the whole document must stay within
     * {@code yuheng.llm.max-request-bytes}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code LlmProtocolStrategy.encodeRequest(command, route)}，返回体直接作为上游请求正文。
     * @param command 参数 已校验的调用命令，提供 alias、stream 位与原生请求文档；parameter the validated command carrying the alias, the stream flag and the native request document.
     * @param route 参数 已选定的候选 route，提供唯一被允许出现在出站请求里的 {@code upstreamModel} 与渠道事实；parameter the selected candidate route supplying the {@code upstreamModel} that is the only thing allowed to replace the alias egress, plus the channel facts.
     * @return 返回 协议原生请求文档，除 {@code model} 外与客户端提交保持同构；returns the native request document, structurally identical to what the client submitted apart from {@code model}.
     * @throws LlmInvocationException 400 {@code unsupported_parameter}（未声明能力或非法字段）、413 {@code request_too_large}；the native status travels with the error.
     */
    ObjectNode encodeRequest(@Valid LlmInvocationCommandDTO command, @Valid LlmModelSnapshotBO.RouteBO route);

    /**
     * 中文说明：职责②单播解码/透传。只处理成功文档：保持上游原生 JSON 的全部已知与未知安全字段（不缩减成自定义
     * VO、不重排数组、不把 {@code usage} 缺失伪造成 0、不把 Responses/Messages 的工具块降格成 Chat 的
     * {@code choices}/{@code tool_calls}），并仅把 {@code model} 回写为客户端 alias。非 2xx 上游响应不属于本操作，
     * 必须由 {@link #exchange} 交给 {@link #encodeError}，从而「上游失败」永远不可能变成「空成功」。
     * English summary: Duty (2) unary decoding and pass-through. Success documents only: preserve every known and unknown
     * safe field of the native upstream JSON (never reduced into a bespoke VO, never re-ordered, never fabricating a
     * missing {@code usage} as zero, never downgrading a Responses or Messages tool block into Chat's
     * {@code choices}/{@code tool_calls}), rewriting only {@code model} back to the client alias. A non-2xx upstream
     * response is not this operation's business and must be handed by {@link #exchange} to {@link #encodeError}, so that
     * an upstream failure can never become an empty success.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code LlmProtocolStrategy.decodeUnaryResponse(command, upstreamBody)}。
     * @param command 参数 本次调用命令，提供回写给客户端的 alias；parameter this invocation's command, supplying the alias to write back to the client.
     * @param upstreamBody 参数 上游成功响应的原生 JSON 文档；parameter the native JSON document of the successful upstream response.
     * @return 返回 保真后的原生响应文档，{@code model} 已恢复为客户端 alias；returns the fidelity-preserving native response document with {@code model} restored to the client alias.
     * @throws LlmInvocationException 502 {@code upstream_protocol_error}（缺少本协议必需结构，如 Embedding 的向量长度与声明维度不符）；raised when a structure this protocol requires is absent, such as an embedding whose length disagrees with the declared dimensions.
     */
    ObjectNode decodeUnaryResponse(@Valid LlmInvocationCommandDTO command, @NotNull ObjectNode upstreamBody);

    /**
     * 中文说明：职责③流式帧解码与编码。把上游原始字节按本协议解析成帧、验证结构、回写 alias 后再按同协议编码为
     * 客户端字节：正常顺序与终态标记由 {@link #protocol()} 决定（Chat 的 {@code [DONE]}、Responses 的
     * {@code response.completed}/{@code response.incomplete}/{@code response.failed}、Messages 的
     * {@code message_stop}），合法未知事件原样转发，EOF 缺少终态即 ABORTED，流内失败只能以本协议错误事件或断开收束，
     * 绝不补发伪终态帧。返回的 publisher 必须是有界的：预取不超过 1 帧，单帧受
     * {@code yuheng.llm.max-frame-bytes} 约束，下游取消必须传播为上游取消，且每个 {@link DataBuffer} 恰好释放一次。
     * English summary: Duty (3) streaming frame decoding and encoding. Parse the upstream raw bytes into this protocol's
     * frames, validate them, rewrite the alias and re-encode them in the same protocol into client bytes: the ordering
     * and the terminal marker are decided by {@link #protocol()} (Chat's {@code [DONE]}, Responses'
     * {@code response.completed}/{@code response.incomplete}/{@code response.failed}, Messages' {@code message_stop}),
     * legitimate unknown events are forwarded as-is, an EOF without a terminal marker is ABORTED, and an in-stream
     * failure may only end with this protocol's error event or a disconnect — never with a fabricated terminal frame.
     * The returned publisher must be bounded: prefetch no more than one frame, cap each frame at
     * {@code yuheng.llm.max-frame-bytes}, propagate a downstream cancellation into an upstream one, and release every
     * {@link DataBuffer} exactly once.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code LlmProtocolStrategy.encodeStream(command, upstreamFrames)}，交给
     * {@code LlmServletStreamComponent} 逐帧写出。/ hand the result to {@code LlmServletStreamComponent} which writes it out frame by frame.
     * @param command 参数 本次调用命令，提供逐帧回写 alias 所需的目标名；parameter this invocation's command, supplying the alias each frame must carry.
     * @param upstreamFrames 参数 上游响应体的有界字节流；parameter the bounded byte flux of the upstream response body.
     * @return 返回 同协议客户端字节流，含终态帧；returns the same-protocol client byte flux including its terminal frame.
     * @throws LlmInvocationException 提交前：502 {@code upstream_protocol_error}、504 {@code upstream_timeout}；提交后只能终止本流，不得改状态。
     *                               Pre-commit only: after commit a failure may only end the stream and never change the status.
     */
    Flux<DataBuffer> encodeStream(@Valid LlmInvocationCommandDTO command, @NotNull Flux<DataBuffer> upstreamFrames);

    /**
     * 中文说明：职责④原生错误编码。按本协议的原生形状编码安全错误：Chat/Embeddings/Responses 的错误对象只有顶层
     * {@code error} 一个键，其内层携带 {@code message}、{@code type}、{@code param} 与 {@code code}；Messages 的顶层
     * {@code type} 为 {@code error}，其 {@code error} 子对象携带 {@code type} 与 {@code message}，且不得借用 OpenAI 的
     * error/choices 结构；{@code type} 由协议自行映射，{@code code}/{@code param} 取自
     * {@link LlmInvocationException}，消息只用其自带的安全说明。本操作<b>不</b>产生 admin 的 {@code code}/{@code data}
     * 信封，也永不在任何字段里出现 token、密钥、baseUrl、upstreamModel 或上游正文片段。
     * English summary: Duty (4) native error encoding. Encode the safe error in this protocol's native shape: for
     * Chat/Embeddings/Responses an error object whose only top-level key is {@code error} and whose inner object carries
     * {@code message}, {@code type}, {@code param} and {@code code}; for Messages a top-level {@code type} of
     * {@code error} with an {@code error} sub-object carrying {@code type} and {@code message}, which must never borrow
     * the OpenAI error/choices structure; {@code type} is mapped by the protocol itself while {@code code} and
     * {@code param} come from the
     * {@link LlmInvocationException} and the message uses only its own safe description. This operation produces
     * <b>no</b> admin {@code code}/{@code data} envelope and never lets a token, secret, base URL, upstream model or a
     * fragment of the upstream payload appear in any field.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code LlmProtocolStrategy.encodeError(error)}，状态取 {@code error.getStatus()}；
     * 未认证与越权的原生错误同样由本方法产出（本模块无 Spring Security 错误编码器 bean）。
     * / the status is {@code error.getStatus()}; the native errors for unauthenticated and unauthorized requests are
     * produced by this same method, since this module has no Spring Security error-encoder bean.
     * @param error 参数 已脱敏的原生错误载体；parameter the masked native error carrier.
     * @return 返回 协议原生错误文档；returns the protocol-native error document.
     */
    ObjectNode encodeError(@NotNull LlmInvocationException error);

    /**
     * 中文说明：唯一的编排入口：用 {@code route.channel} 的 baseUrl、secretRef 与四段超时预算发起<b>同协议</b>上游调用，
     * 依次复用 {@link #encodeRequest}、成功时按 stream 位选择 {@link #decodeUnaryResponse} 或 {@link #encodeStream}、
     * 失败时选择 {@link #encodeError}，并把原生状态、过滤后的安全响应头与有界 body 写回 {@code routed} 后返回。
     * 提交屏障在此实现：只有验证过一个完整合法帧之后才允许进入 COMMITTED，此前失败仍可让调用方按候选顺序做下一
     * 一次安全尝试，此后禁止换模型、禁止重试、禁止改写 HTTP 状态；线程池饱和（{@code TaskRejectedException}）必须在
     * 提交前表现为 429，配置只读快照事务早已结束，本方法绝不在事务或锁内等待上游。
     * English summary: The single orchestration entry: issue a <b>same-protocol</b> upstream call using the
     * {@code route.channel} base URL, secretRef and four timeout budgets, reusing {@link #encodeRequest} and then
     * choosing {@link #decodeUnaryResponse} or {@link #encodeStream} by the stream flag on success and
     * {@link #encodeError} on failure, and return {@code routed} after writing back the native status, the filtered safe
     * headers and the bounded body. The commit barrier is implemented here: COMMITTED may only be entered after one
     * complete valid frame has been validated, so an earlier failure still lets the caller take the next safe candidate
     * attempt while a later one may neither switch model nor retry nor rewrite the HTTP status; pool saturation
     * ({@code TaskRejectedException}) must present as a 429 before submission, the read-only configuration snapshot
     * transaction has long since closed, and this method never waits on upstream inside a transaction or a lock.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code LlmProtocolStrategy.exchange(command, route, routed)}，由
     * {@code LlmApiController} 在 {@code LlmInvocationService.invoke(command)} 之后调用。
     * @param command 参数 已校验的调用命令；parameter the validated invocation command.
     * @param route 参数 已被路由策略放行的首条候选 route；parameter the head candidate route the routing strategy already admitted.
     * @param routed 参数 路由阶段产出的原生结果，携带安全响应头，本方法就地补全 body；parameter the routed native result carrying the safe headers, whose body this method completes in place.
     * @return 返回 已带状态、安全头与有界 body 的原生结果；returns the native result carrying status, safe headers and a bounded body.
     * @throws LlmInvocationException 首帧验证之前的任何上游失败（429/502/503/504 等），状态即协议原生状态；any upstream failure before the first frame validates, whose status is the protocol-native one.
     */
    LlmInvocationResultVO exchange(@Valid LlmInvocationCommandDTO command,
            @Valid LlmModelSnapshotBO.RouteBO route,
            @NotNull LlmInvocationResultVO routed);
}
