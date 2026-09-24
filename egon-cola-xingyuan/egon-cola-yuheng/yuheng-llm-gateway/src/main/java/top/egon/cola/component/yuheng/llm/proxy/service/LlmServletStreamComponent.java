package top.egon.cola.component.yuheng.llm.proxy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import top.egon.cola.component.yuheng.llm.config.LlmGatewayProperties;
import top.egon.cola.component.yuheng.llm.proxy.domain.dto.LlmInvocationCommandDTO;
import top.egon.cola.component.yuheng.llm.proxy.domain.exception.LlmInvocationException;
import top.egon.cola.component.yuheng.llm.proxy.domain.vo.LlmInvocationResultVO;

/**
 * 中文说明：{@code LlmServletStreamComponent} 是 engine 里<b>唯一</b>把协议 Strategy 的字节变成 Servlet 响应字节的
 * 出网点，只做两件事：① {@link #writeUnary} 写已经路由完的非流式原生 JSON 文档（状态与安全头已在
 * {@link LlmInvocationResultVO} 上，body 是同协议 {@code decodeUnaryResponse}/{@code encodeError} 产出的
 * {@link ObjectNode}）；② {@link #writeStream} 驱动一条 {@code Flux<DataBuffer>} 增量 SSE。它<b>不</b>解析帧语义、
 * <b>不</b>判断协议、<b>不</b>拼任何原生错误形状——四种协议的帧文法与错误 shape 全在 Strategy 里，本类只按调用方
 * 传入的编码器（即 {@code LlmProtocolStrategy::encodeError}）把 {@link LlmInvocationException} 换成
 * {@link ObjectNode} 再写出，因此不会出现第五套错误格式。
 * 三条硬边界都在这里落地：<b>有界背压</b>——用显式 {@link BaseSubscriber} 只 {@code request(1)}，上一帧写完并释放后才要
 * 下一帧，绝不 {@code toIterable()}/{@code collectList()}，也绝不把整条流攒成 {@code byte[]}；单帧超
 * {@code yuheng.llm.max-frame-bytes} 或累计超 {@code yuheng.llm.max-request-bytes} 立即收束；<b>恰好一次释放</b>——
 * 每帧的所有权由「交付槽」这一个字段承载，取出即置空，因此写出后释放、终态（完成/错误/取消）兜底释放与移交失败时
 * 由 {@link BaseSubscriber} 就地释放三条路径互不重叠，既不漏放也不重放；写失败必定向上传播取消；<b>提交屏障</b>——
 * 首帧通过上限校验之前不碰响应状态与头部，此时失败仍可如实写成原生错误状态加原生错误正文；一旦 {@code flush} 出首字节
 * 响应即提交，此后只允许结束本流：不改 HTTP 状态、不写第二个状态行、不伪造 {@code data: [DONE]} 之类的终态帧。
 * 写出工作跑在具名的 {@code llmStreamingExecutor}（线程数取 {@code yuheng.llm.streaming-threads}、队列容量 0）上，
 * 因此饱和时 Spring 立刻抛 {@code TaskRejectedException}（{@link RejectedExecutionException} 的子类），本类把它翻译成
 * <b>提交前</b>的 429 {@code rate_limit_exceeded}——既不排队，也绝不表现为挂住；等待写手结束用有界期限
 * （{@link #STREAM_DEADLINE} 加 {@link #WRITER_GRACE}），期限到即取消上游、结束本流，永不 {@code block()} 无界等待。
 * 头部与媒体类型同样收口：SSE 用 {@code text/event-stream;charset=UTF-8} 加 {@code Cache-Control: no-store} 与
 * {@code X-Accel-Buffering: no}（禁止代理缓冲），非流式正文用 {@code application/json;charset=UTF-8}，回显只允许白名单
 * 头名（实际只剩 {@code Retry-After}），baseUrl、secretRef、凭据、上游真实模型与任何供应商凭据头都不可能离网。
 * 日志只记 alias、协议、帧数、字节数、是否已提交与终态原因，绝不记帧内容、prompt 或工具参数。
 * English summary: {@code LlmServletStreamComponent} is the <b>only</b> place in the engine where a protocol Strategy's
 * bytes become servlet response bytes, and it does exactly two things: (1) {@link #writeUnary} writes an already-routed
 * non-stream native JSON document (its status and safe headers already sit on {@link LlmInvocationResultVO}, the body is
 * the {@link ObjectNode} that the same-protocol {@code decodeUnaryResponse} or {@code encodeError} produced) and
 * (2) {@link #writeStream} drives a {@code Flux<DataBuffer>} out as incremental SSE. It parses no frame grammar, chooses
 * no protocol and assembles <b>no</b> native error shape of its own — the four protocols' frame grammars and error
 * shapes live in the Strategies, so this class only turns an {@link LlmInvocationException} into an {@link ObjectNode}
 * through the caller-supplied encoder (namely {@code LlmProtocolStrategy::encodeError}); a fifth error format cannot
 * appear here. Three hard boundaries land in this file: <b>bounded backpressure</b> — an explicit
 * {@link BaseSubscriber} that only ever {@code request(1)}s, asking for the next frame after the previous one was
 * written and released, never {@code toIterable()} or {@code collectList()} and never accumulating a whole stream into
 * a {@code byte[]}, with one frame capped by {@code yuheng.llm.max-frame-bytes} and the response capped by
 * {@code yuheng.llm.max-request-bytes}; <b>exactly-once release</b> — ownership of a frame lives in the single
 * delivery-slot field, which is nulled when taken, so releasing after writing, the terminal (complete/error/cancel)
 * sweep and the in-place release on a failed hand-off cannot overlap, which rules out both a leak and a double release,
 * while a failed write always propagates cancellation upstream; and the <b>commit barrier</b> — the response status and
 * headers stay untouched until the first frame passes the ceiling checks, so an earlier failure may still be written
 * honestly as a native error status with a native error body, whereas after the first flushed byte only ending the
 * stream is allowed: no HTTP status change, no second status line and no fabricated terminal frame such as
 * {@code data: [DONE]}. The write-out runs on the named {@code llmStreamingExecutor} (threads from
 * {@code yuheng.llm.streaming-threads}, queue capacity 0), so saturation raises Spring's {@code TaskRejectedException}
 * (a subclass of {@link RejectedExecutionException}) immediately and this class turns it into a <b>pre-commit</b> 429
 * {@code rate_limit_exceeded}: it never queues and never presents as a hang; waiting for the writer is bounded by
 * {@link #STREAM_DEADLINE} plus {@link #WRITER_GRACE}, after which upstream is cancelled and the stream ends rather than
 * waiting on an unbounded {@code block()}. Media type and headers are equally closed: SSE uses
 * {@code text/event-stream;charset=UTF-8} with {@code Cache-Control: no-store} and {@code X-Accel-Buffering: no} so no
 * proxy may buffer, a non-stream body uses {@code application/json;charset=UTF-8}, and only whitelisted header names may
 * be echoed (in practice only {@code Retry-After}), so no base URL, secretRef, credential, upstream model or vendor
 * credential header can ever leave the process. Logs carry only the alias, protocol, frame count, byte count, whether
 * the response was already committed and the terminal reason — never frame content, prompts or tool arguments.
 *
 * 用法 / Usage: 由 {@code LlmApiController}（Step 11 File 8）注入：非流式在 {@code exchange} 之后调
 * {@code writeUnary(command, routed, nativeBody, response)}；流式调
 * {@code writeStream(command, routed, routed.getBody(), strategy::encodeError, response)}——错误编码器必须由调用方
 * 按路由协议传入，本类不猜协议。{@code TaskRejectedException} 翻译出的 429 与提交前的其它原生错误都经安全头白名单
 * 与协议原生正文出网；已提交后本方法只结束流，调用方无需再写任何字节。
 * / Injected by {@code LlmApiController} (Step 11 File 8): after {@code exchange}, a non-stream call uses
 * {@code writeUnary(command, routed, nativeBody, response)} while a stream uses
 * {@code writeStream(command, routed, routed.getBody(), strategy::encodeError, response)} — the error encoder must be
 * supplied by the caller per route protocol because this class guesses none. The 429 translated from
 * {@code TaskRejectedException} and any other pre-commit native error leave through the header whitelist with the
 * protocol-native body; after commit only the stream end happens, so callers write no further bytes.
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@Component("llmServletStreamComponent")
public class LlmServletStreamComponent {

    /** 中文说明：SSE 媒体类型，显式带 UTF-8，并与 {@code no-store}/{@code X-Accel-Buffering: no} 一起保证客户端看到增量帧。 English summary: the SSE media type, explicitly UTF-8, paired with no-store and no proxy buffering so the client sees incremental frames. */
    private static final String SSE_MEDIA_TYPE = "text/event-stream;charset=UTF-8";

    /** 中文说明：非流式正文与原生错误正文共用的 JSON 媒体类型；永不回显 routed 里的 SSE 媒体类型。 English summary: the JSON media type shared by a non-stream body and a native error body; the routed SSE media type is never echoed. */
    private static final String JSON_MEDIA_TYPE = "application/json;charset=UTF-8";

    /** 中文说明：允许离网的头名白名单（小写比较）；内容类型与缓存头由本类自己给，因此实际能被回显的只有 Retry-After 与 X-Accel-Buffering。 English summary: the outbound header whitelist (lower-cased); this class supplies content type and cache control itself, so in practice only Retry-After and X-Accel-Buffering can be echoed. */
    private static final Set<String> SAFE_HEADER_NAMES = Set.of(
            "content-type", "cache-control", "retry-after", "x-accel-buffering");

    /** 中文说明：反缓冲头，禁止中间代理攒住 SSE。 English summary: the anti-buffering header that stops an intermediating proxy from holding SSE back. */
    private static final String ACCEL_BUFFERING_HEADER = "X-Accel-Buffering";

    /** 中文说明：整条流式写出的绝对期限。{@code yuheng.llm.*} 只暴露字节/次数/线程数四把闸（max-request-bytes、max-frame-bytes、maximum-attempts、streaming-threads），没有任何时长键，因此这里按主业务 Spec「模型调用总 120s」把同一个预算取成固定上限，绝不做无界等待，也不新增需要用户配置的键。 English summary: the absolute ceiling of one streaming write-out. yuheng.llm.* exposes only four gates (max-request-bytes, max-frame-bytes, maximum-attempts, streaming-threads) and no duration key at all, so this reuses the primary business Spec's total model-call deadline of 120s as a fixed ceiling: never an unbounded wait, and never a new key the operator must supply. */
    private static final Duration STREAM_DEADLINE = Duration.ofSeconds(120);

    /** 中文说明：等待方在写手自身期限之外多给的收尾余量，保证正常情况下由写手自己收束。 English summary: the extra grace the waiting caller grants beyond the writer's own deadline so that normally the writer closes things itself. */
    private static final Duration WRITER_GRACE = Duration.ofSeconds(5);

    /** 中文说明：终态原因——首帧之后上游正常完成。 English summary: terminal reason, upstream completed after at least one frame was delivered. */
    private static final String REASON_COMPLETED = "completed";

    /** 中文说明：终态原因——上游以协议错误终止（提交前已按原生错误写出）。 English summary: terminal reason, the upstream terminated with a protocol error, natively written when still pre-commit. */
    private static final String REASON_UPSTREAM_ERROR = "upstream_error";

    /** 中文说明：终态原因——写手自身期限已到，已取消上游。 English summary: terminal reason, the writer's own deadline elapsed after cancelling the upstream. */
    private static final String REASON_DEADLINE = "deadline";

    /** 中文说明：终态原因——等待方超时后强制结束本流。 English summary: terminal reason, the waiting caller ended the stream after its own bounded wait expired. */
    private static final String REASON_WAIT_TIMEOUT = "wait_timeout";

    /** 中文说明：终态原因——单帧超过 {@code yuheng.llm.max-frame-bytes}。 English summary: terminal reason, one frame exceeded yuheng.llm.max-frame-bytes. */
    private static final String REASON_FRAME_LIMIT = "frame_limit";

    /** 中文说明：终态原因——累计字节超过 {@code yuheng.llm.max-request-bytes}。 English summary: terminal reason, the response exceeded yuheng.llm.max-request-bytes in total. */
    private static final String REASON_TOTAL_LIMIT = "total_limit";

    /** 中文说明：终态原因——流一帧未出就结束了，按协议失败收束而非空成功。 English summary: terminal reason, the stream ended without a single frame, closed as a protocol failure rather than an empty success. */
    private static final String REASON_EMPTY_STREAM = "empty_stream";

    /** 中文说明：终态原因——写出抛 IO 异常（客户端断连或容器失败）。 English summary: terminal reason, the write-out raised an IO failure, a client disconnect or a container fault. */
    private static final String REASON_CLIENT_ABORT = "client_abort";

    /** 中文说明：流式线程池饱和（队列容量 0，无空闲线程），只能在提交前表现为 429。 English summary: the streaming pool is saturated, queue capacity 0 with no idle thread, which can only present as a 429 before commit. */
    private static final String REASON_SATURATED = "saturated";

    /** 中文说明：本进程的出网与响应边界（帧上限与整响应上限），不提供任何宽松默认值。 English summary: this process's egress and response boundary (the frame ceiling and the whole-response ceiling) with no permissive defaults. */
    @Qualifier(LlmGatewayProperties.BEAN_NAME)
    private final LlmGatewayProperties gatewayProperties;

    /** 中文说明：有界流式写出执行器（线程数上限 64、队列容量 0），本类是它唯一的消费者；饱和即拒，绝不排队。 English summary: the bounded streaming executor (at most 64 threads and a zero-capacity queue), whose only consumer is this class; saturation rejects at once and nothing ever queues. */
    @Qualifier("llmStreamingExecutor")
    private final ThreadPoolTaskExecutor llmStreamingExecutor;

    /** 中文说明：容器唯一的 Jackson 栈，只用于把原生 {@link ObjectNode} 序列化成字节，不开第二套 JSON 实现。 English summary: the container's single Jackson stack, used only to serialize the native ObjectNode into bytes so no second JSON implementation can appear. */
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：写一个<b>已经路由完</b>的非流式原生正文：状态取 {@code routed.getStatus()}，头部只按白名单回显
     * {@code routed} 上的安全头并强制 {@code application/json;charset=UTF-8} 与 {@code Cache-Control: no-store}、
     * {@code X-Accel-Buffering: no}，正文是调用方（同协议 Strategy 的 {@code decodeUnaryResponse} 或
     * {@code encodeError}）产出的 {@link ObjectNode} 的原样序列化结果——本类不重排、不裁剪、不改写任何字段。
     * 序列化后的字节数先过 {@code yuheng.llm.max-request-bytes}：越界即在写第一个字节之前抛 413
     * {@code request_too_large}，因此绝不会「写了一半再报错」。写出失败按 {@link IOException} 如实抛出，交给容器与
     * 调用方处理，本方法不吞异常也不伪造成功。本方法是提交前失败的原生错误出口，故 {@code writeStream} 的提交前
     * 收束也走同一条写出路径（状态换成 {@link LlmInvocationException#getStatus()}）。
     * English summary: Writes an <b>already-routed</b> non-stream native body: the status comes from
     * {@code routed.getStatus()}, the headers are only the whitelisted safe headers of {@code routed} with
     * {@code application/json;charset=UTF-8}, {@code Cache-Control: no-store} and {@code X-Accel-Buffering: no} enforced,
     * and the body is the verbatim serialization of the {@link ObjectNode} the caller produced (through the same-protocol
     * Strategy's {@code decodeUnaryResponse} or {@code encodeError}) — this method reorders nothing, trims nothing and
     * rewrites no field. The serialized length is checked against {@code yuheng.llm.max-request-bytes} first, so an
     * overflow raises a 413 {@code request_too_large} before a single byte is written rather than half-writing and then
     * failing. A write failure propagates honestly as {@link IOException} for the container and caller to handle; nothing
     * is swallowed and no success is fabricated. Because this is also the pre-commit native error exit, {@link #writeStream}
     * reuses it for its own pre-commit closure, only swapping in {@link LlmInvocationException#getStatus()}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code writeUnary(command, routed, nativeBody, response)}，由
     * {@code LlmApiController} 在非流式分支上于 {@code exchange} 之后调用。
     * @param command 参数 本次调用命令，只用于日志里的 alias 与协议两项稳定标识；parameter this invocation's command, used only for the stable alias and protocol identities in the log.
     * @param routed 参数 已路由结果，提供原生状态与可回显的安全响应头；parameter the routed native result supplying the native status and the echoable safe headers.
     * @param body 参数 协议原生 JSON 文档（成功文档或该协议的原生错误文档）；parameter the protocol-native JSON document, either a success document or that protocol's native error document.
     * @param response 参数 当前 Servlet 响应；parameter the current servlet response.
     * @throws LlmInvocationException 413 {@code request_too_large}：原生文档超过整响应上限，尚未写出任何字节；a 413 {@code request_too_large} when the native document passes the whole-response ceiling, raised before any byte leaves.
     * @throws IOException 容器或客户端写出失败；raised when the container or the client refuses the write.
     */
    public void writeUnary(@NotNull LlmInvocationCommandDTO command,
            @NotNull LlmInvocationResultVO routed,
            @NotNull ObjectNode body,
            @NotNull HttpServletResponse response) throws IOException {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(routed, "routed");
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(response, "response");
        writeNativeJson(command, routed, response, body, routed.getStatus());
    }

    /**
     * 中文说明：把一条 {@code Flux<DataBuffer>} 驱动成增量 SSE。订阅只用显式 {@link BaseSubscriber} 的
     * {@code request(1)}：首帧之前要一帧，每「写完并释放」一帧之后再要一帧，因此预取恒不超过 1，既不
     * {@code toIterable()} 缓冲也不 {@code collectList()} 攒成 {@code byte[]}；每帧先过
     * {@code yuheng.llm.max-frame-bytes}、累计再过 {@code yuheng.llm.max-request-bytes}，写完立即 {@code flush}
     * 让客户端看到增量。首帧落地前响应不被触碰（提交屏障），此时任何失败——上游错误信号、期限已到、单帧或总量越界、
     * 以及「一帧未出就结束了」——都由 {@code errorEncoder}（调用方按路由协议给的
     * {@code LlmProtocolStrategy::encodeError}）换成原生 {@link ObjectNode} 后按原生状态写出；首帧一旦 {@code flush}
     * 即视为提交，此后只能结束本流：不改状态、不写第二个状态行、不伪造 {@code [DONE]} 或任何协议的终态事件。
     * 每个 {@link DataBuffer} 恰好释放一次：帧的所有权只存在于交付槽这一个字段里，取出即置空，所以「写出后释放」「终态
     * （完成/错误/取消）清扫」「移交失败时就地释放」三条路互不重叠；写出抛 {@link IOException} 时必定
     * {@code cancel()} 上游并把异常回抛给等待方。整个写出跑在具名 {@code llmStreamingExecutor} 上，队列容量 0，
     * 因此饱和时 {@code TaskRejectedException} 会被翻译成本方法的 429 {@code rate_limit_exceeded}（提交前、不排队、
     * 不挂起）；等待写手结束用 {@link #STREAM_DEADLINE} 加 {@link #WRITER_GRACE} 的有界期限，期限到就取消上游并清扫
     * 未写帧，永不无界 {@code block()}。一帧未出的流按 502 {@code upstream_protocol_error} 收束，绝不伪装成空成功。
     * English summary: Drives a {@code Flux<DataBuffer>} out as incremental SSE. Subscription uses only an explicit
     * {@link BaseSubscriber}'s {@code request(1)} — one frame before the first, and one more only after each frame has
     * been written <i>and</i> released — so prefetch never exceeds one, with neither {@code toIterable()} buffering nor
     * {@code collectList()} accumulating a {@code byte[]}; every frame passes {@code yuheng.llm.max-frame-bytes} and the
     * running total passes {@code yuheng.llm.max-request-bytes}, and each frame is flushed as it lands so the client sees
     * increments. Nothing touches the response before the first frame (the commit barrier), so any failure up to then —
     * an upstream error signal, the deadline, a frame or total overflow, and even a stream that ends without producing a
     * frame — is turned by {@code errorEncoder} (the {@code LlmProtocolStrategy::encodeError} the caller supplies for the
     * route protocol) into a native {@link ObjectNode} written under its native status; once the first frame is flushed the
     * response is committed and afterwards only ending the stream remains possible: no status change, no second status
     * line, no fabricated {@code [DONE]} or any protocol's terminal event. Every {@link DataBuffer} is released exactly
     * once because a frame's ownership lives in the single delivery-slot field and is nulled when taken, so releasing
     * after writing, the terminal (complete/error/cancel) sweep and the in-place release on a failed hand-off can never
     * overlap; an {@link IOException} during a write always {@code cancel()}s the upstream and is rethrown to the waiting
     * caller. The whole write-out runs on the named {@code llmStreamingExecutor} whose queue capacity is 0, so saturation
     * raises a {@code TaskRejectedException} that this method translates into a 429 {@code rate_limit_exceeded}
     * (pre-commit, never queued, never a hang), and the wait for the writer is bounded by {@link #STREAM_DEADLINE} plus
     * {@link #WRITER_GRACE}, after which upstream is cancelled and any unwritten frame swept rather than waiting on an
     * unbounded {@code block()}. A stream that yields no frame at all closes as a 502
     * {@code upstream_protocol_error} and is never presented as an empty success.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code writeStream(command, routed, routed.getBody(), strategy::encodeError, response)}，
     * 由 {@code LlmApiController} 在 {@code command.stream} 为真时调用；返回即表示本流已结束，调用方不得再写任何字节。
     * @param command 参数 本次调用命令，提供日志用的 alias 与协议；parameter this invocation's command, supplying the alias and protocol for logging.
     * @param routed 参数 已路由结果，提供原生状态与安全响应头（媒体类型由本方法按 SSE 强制）；parameter the routed native result supplying the native status and safe headers, with the media type forced to SSE here.
     * @param frames 参数 同协议 Strategy 编码后的客户端字节流，必须已有界；parameter the client byte flux encoded by the same-protocol Strategy, already expected to be bounded.
     * @param errorEncoder 参数 调用方按路由协议给出的原生错误编码器；parameter the caller's native error encoder for the route protocol.
     * @param response 参数 当前 Servlet 响应；parameter the current servlet response.
     * @throws LlmInvocationException 提交前 429 {@code rate_limit_exceeded}：流式线程池饱和（队列容量 0）；a pre-commit 429 {@code rate_limit_exceeded} when the streaming pool is saturated (zero-capacity queue).
     * @throws IOException 提交后写出失败（客户端断连或容器故障），此时状态已不可更改；raised when writing fails after the response was committed, where the status can no longer change.
     */
    public void writeStream(@NotNull LlmInvocationCommandDTO command,
            @NotNull LlmInvocationResultVO routed,
            @NotNull Flux<DataBuffer> frames,
            @NotNull Function<LlmInvocationException, ObjectNode> errorEncoder,
            @NotNull HttpServletResponse response) throws IOException {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(routed, "routed");
        Objects.requireNonNull(frames, "frames");
        Objects.requireNonNull(errorEncoder, "errorEncoder");
        Objects.requireNonNull(response, "response");

        // 每请求局部状态：交付槽 + 终态标志，全部由 handoff 监视器守护，不进数据库也不跨请求复用。
        final Object handoff = new Object();
        final DataBuffer[] slot = new DataBuffer[1];
        final boolean[] ended = new boolean[1];
        final boolean[] abandoned = new boolean[1];
        final Throwable[] signal = new Throwable[1];
        final AtomicBoolean committed = new AtomicBoolean();
        final AtomicReference<IOException> ioFailure = new AtomicReference<>();
        final CountDownLatch finished = new CountDownLatch(1);
        final Instant deadline = Instant.now().plus(STREAM_DEADLINE);

        final BaseSubscriber<DataBuffer> subscriber = new BaseSubscriber<DataBuffer>() {

            @Override
            protected void hookOnSubscribe(Subscription subscription) {
                request(1);
            }

            @Override
            protected void hookOnNext(DataBuffer frame) {
                synchronized (handoff) {
                    if (abandoned[0] || slot[0] != null) {
                        // 移交不出去的所有权仍在本回调手里：就地释放，写手之后再也拿不到它，故不会重放。
                        DataBufferUtils.release(frame);
                        return;
                    }
                    slot[0] = frame;
                    handoff.notifyAll();
                }
            }

            @Override
            protected void hookOnError(Throwable failure) {
                synchronized (handoff) {
                    signal[0] = failure;
                    ended[0] = true;
                    handoff.notifyAll();
                }
            }

            @Override
            protected void hookOnComplete() {
                synchronized (handoff) {
                    ended[0] = true;
                    handoff.notifyAll();
                }
            }

            @Override
            protected void hookOnCancel() {
                // 取消路径的终态清扫：仍在槽里的帧由这里释放，已被写手取走的帧与它无关。
                releasePending(handoff, slot);
            }
        };

        frames.subscribe(subscriber);
        try {
            llmStreamingExecutor.execute(() -> {
                try {
                    driveFrames(command, routed, response, subscriber, errorEncoder,
                            handoff, slot, ended, abandoned, signal, committed, deadline);
                } catch (IOException failure) {
                    ioFailure.set(failure);
                } catch (RuntimeException failure) {
                    log.error("llm stream writer failed alias={} protocol={} type={}",
                            command.getModel(), command.getProtocol(), failure.getClass().getSimpleName());
                } finally {
                    subscriber.cancel();
                    releasePending(handoff, slot);
                    finished.countDown();
                }
            });
        } catch (RejectedExecutionException saturation) {
            subscriber.cancel();
            releasePending(handoff, slot);
            log.warn("llm streaming executor saturated alias={} protocol={} reason={}",
                    command.getModel(), command.getProtocol(), REASON_SATURATED);
            throw new LlmInvocationException(429, "rate_limit_exceeded", null,
                    "The llm gateway is serving its full complement of streaming responses; retry after a backoff",
                    true);
        }

        boolean drained = awaitQuietly(finished, Duration.between(Instant.now(), deadline).plus(WRITER_GRACE));
        if (!drained) {
            // 写手没在期限内收束（多半是 socket 写住死）：取消上游、清扫未写帧并结束本流，状态已提交与否都不再补写。
            synchronized (handoff) {
                abandoned[0] = true;
                handoff.notifyAll();
            }
            subscriber.cancel();
            releasePending(handoff, slot);
            awaitQuietly(finished, WRITER_GRACE);
            log.warn("llm stream ended by the waiting caller alias={} protocol={} committed={} reason={}",
                    command.getModel(), command.getProtocol(), committed.get(), REASON_WAIT_TIMEOUT);
        }
        IOException failure = ioFailure.get();
        if (failure != null) {
            throw failure;
        }
    }

    /**
     * 中文说明：逐帧写出的主体，跑在 {@code llmStreamingExecutor} 的线程上：只在交付槽真给出帧时才第一次触碰响应，
     * 每帧写完并释放后才 {@code request(1)} 要下一帧，收束时按提交屏障决定「原生错误写出」还是「只结束本流」。
     * English summary: The frame-by-frame write-out, running on an llmStreamingExecutor thread: the response is touched for
     * the first time only when the delivery slot actually yields a frame, the next frame is requested only after the
     * previous one was written and released, and the closure either writes a native error or merely ends the stream,
     * depending on the commit barrier.
     */
    private void driveFrames(LlmInvocationCommandDTO command,
            LlmInvocationResultVO routed,
            HttpServletResponse response,
            BaseSubscriber<DataBuffer> subscriber,
            Function<LlmInvocationException, ObjectNode> errorEncoder,
            Object handoff,
            DataBuffer[] slot,
            boolean[] ended,
            boolean[] abandoned,
            Throwable[] signal,
            AtomicBoolean committed,
            Instant deadline) throws IOException {
        long maxFrameBytes = gatewayProperties.getMaxFrameBytes();
        long maxTotalBytes = gatewayProperties.getMaxRequestBytes();
        long frameCount = 0;
        long byteCount = 0;
        String reason = REASON_COMPLETED;
        LlmInvocationException preCommitFailure = null;
        ServletOutputStream out = null;
        try {
            while (true) {
                DataBuffer frame = null;
                boolean timedOut = false;
                try {
                    frame = nextFrame(handoff, slot, ended, abandoned, deadline);
                } catch (TimeoutException elapsed) {
                    timedOut = true;
                }
                if (frame == null) {
                    Throwable failure;
                    boolean gaveUp;
                    boolean closed;
                    synchronized (handoff) {
                        // 终态三要素在同一把锁里读齐，避免看到「已结束却丢了错误信号」的半途状态。
                        failure = signal[0];
                        gaveUp = abandoned[0];
                        closed = ended[0];
                    }
                    if (timedOut || (gaveUp && !closed)) {
                        reason = timedOut ? REASON_DEADLINE : REASON_WAIT_TIMEOUT;
                        preCommitFailure = new LlmInvocationException(504, "upstream_timeout", "stream",
                                "The upstream llm stream produced no frame within the request deadline", false);
                    } else if (failure != null) {
                        reason = REASON_UPSTREAM_ERROR;
                        preCommitFailure = maskAsNative(failure, command);
                    } else if (frameCount == 0L) {
                        reason = REASON_EMPTY_STREAM;
                        preCommitFailure = new LlmInvocationException(502, "upstream_protocol_error", "stream",
                                "The upstream llm stream produced no frame at all", false);
                    } else {
                        reason = REASON_COMPLETED;
                    }
                    break;
                }
                try {
                    int frameBytes = frame.readableByteCount();
                    if (frameBytes > maxFrameBytes) {
                        reason = REASON_FRAME_LIMIT;
                        preCommitFailure = new LlmInvocationException(413, "request_too_large", "stream",
                                "An upstream llm stream frame exceeds yuheng.llm.max-frame-bytes", false);
                        break;
                    }
                    if (byteCount + frameBytes > maxTotalBytes) {
                        reason = REASON_TOTAL_LIMIT;
                        preCommitFailure = new LlmInvocationException(413, "request_too_large", "stream",
                                "The upstream llm stream exceeds yuheng.llm.max-request-bytes in total", false);
                        break;
                    }
                    if (!committed.get()) {
                        beginSse(response, routed);
                        out = response.getOutputStream();
                        committed.set(true);
                    }
                    ByteBuffer view = frame.toByteBuffer();
                    byte[] chunk = new byte[view.remaining()];
                    view.get(chunk);
                    out.write(chunk, 0, chunk.length);
                    out.flush();
                    frameCount++;
                    byteCount += chunk.length;
                } catch (IOException clientGone) {
                    reason = REASON_CLIENT_ABORT;
                    throw clientGone;
                } finally {
                    // 所有权已随取出离开交付槽：写完（或越界、或写失败）都在这里恰好释放一次。
                    DataBufferUtils.release(frame);
                }
                subscriber.request(1);
            }
            if (preCommitFailure != null && !committed.get() && !response.isCommitted()) {
                writeNativeError(command, routed, response, errorEncoder, preCommitFailure);
            }
        } finally {
            // 终态日志在 finally：客户端断连而抛 IOException 时同样要留下帧数、字节数与原因这三项稳定标识。
            log.info("llm stream finished alias={} protocol={} frames={} bytes={} committed={} reason={}",
                    command.getModel(), command.getProtocol(), frameCount, byteCount, committed.get(), reason);
        }
    }

    /**
     * 中文说明：唯一的交付槽读取点——先取槽里的帧（取出即置空），槽空且未结束才在剩余期限内 {@code wait}；
     * 期限到或被中断就标 {@code abandoned} 并抛 {@link TimeoutException}，因此这里既是有界等待也是取消的起点。
     * English summary: The single delivery-slot read: take whatever frame the slot holds (nulling it first) and only then
     * wait within the remaining budget; a budget that has elapsed, or an interrupt, marks the request abandoned and
     * raises {@link TimeoutException}, which makes this both the bounded wait and the point where cancellation begins.
     */
    private static DataBuffer nextFrame(Object handoff, DataBuffer[] slot, boolean[] ended, boolean[] abandoned,
            Instant deadline) throws TimeoutException {
        synchronized (handoff) {
            while (slot[0] == null && !ended[0] && !abandoned[0]) {
                long remainingMillis = remainingMillis(deadline);
                if (remainingMillis <= 0L) {
                    abandoned[0] = true;
                    throw new TimeoutException("the llm stream deadline elapsed");
                }
                try {
                    handoff.wait(remainingMillis);
                } catch (InterruptedException interruption) {
                    Thread.currentThread().interrupt();
                    abandoned[0] = true;
                    throw new TimeoutException("the llm stream writer was interrupted");
                }
            }
            DataBuffer frame = slot[0];
            slot[0] = null;
            return frame;
        }
    }

    /**
     * 中文说明：恰好一次释放的收口：把交付槽里的帧原子地取走再释放，槽空时无事可做，因此写手清扫、取消回调与
     * 移交失败三处调用永远不会重复释放同一帧。
     * English summary: The exactly-once release seam: take the frame out of the delivery slot atomically and release it, so
     * an empty slot is a no-op and the writer's sweep, the cancel callback and a failed hand-off can never release one
     * frame twice.
     */
    private static void releasePending(Object handoff, DataBuffer[] slot) {
        synchronized (handoff) {
            DataBuffer pending = slot[0];
            slot[0] = null;
            if (pending != null) {
                DataBufferUtils.release(pending);
            }
        }
    }

    /** 中文说明：有界等待写手收束；被中断时恢复中断位并当作期限已到。 English summary: Waits bounded for the writer to close; an interrupt restores the flag and counts as an expired deadline. */
    private static boolean awaitQuietly(CountDownLatch latch, Duration budget) {
        long millis = Math.max(1L, budget.toMillis());
        try {
            return latch.await(millis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException interruption) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** 中文说明：剩余毫秒数，最小为 0，保证任何等待都有界。 English summary: The remaining milliseconds floored at zero, which is what keeps every wait here bounded. */
    private static long remainingMillis(Instant deadline) {
        Duration remaining = Duration.between(Instant.now(), deadline);
        return remaining.isNegative() || remaining.isZero() ? 0L : remaining.toMillis();
    }

    /**
     * 中文说明：首帧的提交动作——状态与 SSE 头部只在此处第一次落地：媒体类型强制为
     * {@code text/event-stream;charset=UTF-8}，缓存强制 {@code no-store}，并显式禁止代理缓冲；routed 的其它头名只有白名单内
     * 才会被回显，因此 baseUrl、secretRef、凭据与上游真实模型名不可能出现在响应里。
     * English summary: The commit act of the first frame, where the status and SSE headers land for the very first time:
     * the media type is forced to {@code text/event-stream;charset=UTF-8}, caching to {@code no-store} and proxy
     * buffering is explicitly disabled, while any other routed header name only leaves if whitelisted, so no base URL,
     * secretRef, credential or upstream model name can ever appear in the response.
     */
    private void beginSse(HttpServletResponse response, LlmInvocationResultVO routed) {
        response.setStatus(routed.getStatus());
        applySafeHeaders(response, routed, SSE_MEDIA_TYPE);
    }

    /**
     * 中文说明：提交前失败的原生出口：把 {@link LlmInvocationException} 交给调用方提供的编码器换成该协议的原生
     * {@link ObjectNode}，再按异常自带的原生状态写出；编码器本身失败或写出失败时只记稳定标识并结束本流，
     * 因为这里已经没有任何机会改状态。已提交的响应一律直接跳过——绝不写第二个状态行。
     * English summary: The native exit for a pre-commit failure: the {@link LlmInvocationException} is handed to the
     * caller-supplied encoder to become that protocol's native {@link ObjectNode} and is written under the status the
     * exception itself carries. If the encoder or the write fails, only stable identities are logged and the stream simply
     * ends, because nothing can change a status any more; an already committed response is skipped outright, never given
     * a second status line.
     */
    private void writeNativeError(LlmInvocationCommandDTO command, LlmInvocationResultVO routed,
            HttpServletResponse response, Function<LlmInvocationException, ObjectNode> errorEncoder,
            LlmInvocationException failure) {
        if (response.isCommitted()) {
            return;
        }
        try {
            ObjectNode document = errorEncoder.apply(failure);
            writeNativeJson(command, routed, response, document, failure.getStatus());
            log.info("llm stream closed before commit alias={} protocol={} status={} code={}",
                    command.getModel(), command.getProtocol(), failure.getStatus(), failure.getCode());
        } catch (IOException | RuntimeException unsuccessful) {
            log.warn("llm stream native error could not be written alias={} protocol={} code={} type={}",
                    command.getModel(), command.getProtocol(), failure.getCode(),
                    unsuccessful.getClass().getSimpleName());
        }
    }

    /**
     * 中文说明：原生 JSON 的唯一写出点：先按整响应上限校验序列化长度（越界即在写第一个字节前抛 413），再落状态与
     * 白名单头部，最后一次性写出并 {@code flush}；序列化失败按容器异常如实抛出，绝不退化成空正文。
     * English summary: The single native JSON write-out: the serialized length is checked against the whole-response
     * ceiling first (an overflow raises a 413 before the first byte), then the status and the whitelisted headers land,
     * and the document is written once and flushed; a serialization failure propagates as it is rather than degrading into
     * an empty body.
     */
    private void writeNativeJson(LlmInvocationCommandDTO command, LlmInvocationResultVO routed,
            HttpServletResponse response, ObjectNode document, int status) throws IOException {
        byte[] payload = objectMapper.writeValueAsBytes(document);
        if (status < 100 || status > 599) {
            // 与 LlmInvocationException 同一形态约束：状态不成形就不写任何字节，绝不退化成 200 或空正文。
            throw new LlmInvocationException(502, "upstream_protocol_error", null,
                    "The llm result carries no usable native HTTP status", false);
        }
        if (payload.length > gatewayProperties.getMaxRequestBytes()) {
            throw new LlmInvocationException(413, "request_too_large", null,
                    "The native llm document exceeds yuheng.llm.max-request-bytes", false);
        }
        response.setStatus(status);
        applySafeHeaders(response, routed, JSON_MEDIA_TYPE);
        ServletOutputStream out = response.getOutputStream();
        out.write(payload);
        out.flush();
        log.info("llm native json written alias={} protocol={} status={} bytes={}",
                command.getModel(), command.getProtocol(), status, payload.length);
    }

    /**
     * 中文说明：头部收口：只按白名单回显 routed 的安全头（内容类型与缓存头除外，两者一律由本类给出），
     * 字符集固定 UTF-8。
     * English summary: The header seam: only whitelisted routed headers are echoed (content type and cache control
     * excluded, since this class always supplies them), on a fixed UTF-8 charset.
     */
    private void applySafeHeaders(HttpServletResponse response, LlmInvocationResultVO routed, String mediaType) {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Map<String, String> headers = routed.getHeaders();
        if (headers != null) {
            headers.forEach((name, value) -> {
                if (name != null && value != null && SAFE_HEADER_NAMES.contains(name.toLowerCase(Locale.ROOT))
                        && !HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(name)
                        && !HttpHeaders.CACHE_CONTROL.equalsIgnoreCase(name)) {
                    response.setHeader(name, value);
                }
            });
        }
        response.setHeader(HttpHeaders.CONTENT_TYPE, mediaType);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader(ACCEL_BUFFERING_HEADER, "no");
    }

    /**
     * 中文说明：把非原生失败遮成安全原生错误：只保留 {@link LlmInvocationException} 自带的四个安全字段，
     * 其它任何异常一律按 502 {@code upstream_protocol_error} 收束，其 message 与 cause 都不出网，日志也只记类型名。
     * English summary: Masks a non-native failure as the safe native error: only the four safe fields an
     * {@link LlmInvocationException} already carries survive, anything else collapses into a 502
     * {@code upstream_protocol_error} whose message and cause never leave, with only the type name logged.
     */
    private static LlmInvocationException maskAsNative(Throwable failure, LlmInvocationCommandDTO command) {
        if (failure instanceof LlmInvocationException nativeFailure) {
            return nativeFailure;
        }
        log.warn("llm stream raised a non-native failure alias={} protocol={} type={}",
                command.getModel(), command.getProtocol(), failure.getClass().getSimpleName());
        return new LlmInvocationException(502, "upstream_protocol_error", "stream",
                "The upstream llm stream failed and could not be delivered", false);
    }
}
