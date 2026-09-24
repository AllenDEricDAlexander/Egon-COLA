package top.egon.cola.component.yuheng.llm.proxy.domain.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import top.egon.cola.component.yuheng.llm.proxy.domain.bo.LlmModelSnapshotBO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 中文说明：{@code LlmInvocationResultVO} 是一次模型调用的原生结果投影：上游状态码、可回显给客户端的安全响应头，
 * 以及<b>有界</b>的响应体 publisher。它刻意<b>不</b>复用 admin 的业务错误 wrapper，也不包 {@code code}/{@code data}：
 * 四种模型协议都必须按各自原生 shape 出网，包装一次就破坏兼容合同。安全响应头只允许内容类型与可定位的 alias 回写
 * （客户端看到的 model 必须仍是它自己的 alias），供应商身份、上游 URL、secretRef 与任何凭据都不在此出现。
 * English summary: {@code LlmInvocationResultVO} is the native result projection of one model invocation: the upstream
 * status, the response headers that are safe to echo, and a <b>bounded</b> body publisher. It deliberately does
 * <b>not</b> reuse the admin business error wrapper and adds no {@code code}/{@code data} envelope, because all four
 * model protocols must leave the process in their native shape and wrapping them once breaks the compatibility contract.
 * Safe headers carry only the content type and the locatable alias write-back (the client must still see its own alias
 * as {@code model}); vendor identity, upstream URLs, secretRef and any credential never appear here.
 *
 * 用法 / Usage: 由 {@code LlmApiController}（Step 11）直接展开为 {@code ResponseEntity<StreamingResponseBody>}；
 * body 由同协议 Strategy 在首帧验证后写入，一旦发出任何帧就不得再换模型或重试。/ Expanded straight into
 * {@code ResponseEntity<StreamingResponseBody>} by {@code LlmApiController} (Step 11); the body is attached by the
 * same-protocol Strategy once the first frame validates, and no model switch or retry may follow any emitted frame.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class LlmInvocationResultVO {

    /** 中文说明：上游原生 HTTP 状态，按原协议回显；非 2xx 不是「空成功」，必须如实传播。 English summary: the native upstream HTTP status echoed as-is; a non-2xx is never turned into an empty success. */
    @Min(100)
    @Max(599)
    private int status;

    /** 中文说明：可安全回显的响应头白名单结果，key 为原始大小写不敏感的头名。 English summary: the whitelisted headers safe to echo, keyed by case-insensitive header name. */
    @NotNull
    private Map<String, String> headers;

    /** 中文说明：有界响应体 publisher；单帧与整响应上限由 {@code yuheng.llm.*} 约束，禁止把整条流缓存成 byte[]。
     *  路由阶段（{@code LlmInvocationService}）只保证状态与安全头，publisher 由同协议 Strategy 在首帧验证后写入，
     *  因此这里可空：null 表示「已路由、尚无上游响应流」，绝不表示空成功。
     *  English summary: the bounded body publisher, frame- and response-capped by {@code yuheng.llm.*} and never buffered
     *  whole into a byte[]. The routing stage ({@code LlmInvocationService}) guarantees only status and safe headers, while
     *  the same-protocol Strategy attaches the publisher once the first frame validates, so null here means "routed, no
     *  upstream stream yet" and never an empty success. */
    private Flux<DataBuffer> body;

    /** 中文说明：本次路由产出的<b>有序候选 route</b>（优先级、权重已在路由阶段定序）。编排方从这里取首条候选并把其余候选
     *  留给至多 {@code yuheng.llm.maximum-attempts} 次安全尝试，因此<b>不需要</b>为了拿到 route 而重读快照或再抽一次签：
     *  一次请求一份配置、一次准入判定、一个加权结果。null/空表示「尚未路由」，此时必须先调用路由端口。
     *  English summary: the ordered candidate routes this routing pass produced, already ordered by priority and weight. The
     *  orchestrator takes the head from here and keeps the rest for its at most {@code yuheng.llm.maximum-attempts} safe
     *  attempts, so it never has to re-read the snapshot or draw the weighted pick a second time just to obtain a route:
     *  one configuration read, one admission decision and one weighted result per request. Null or empty means "not routed
     *  yet", in which case the routing port must be called first. */
    private List<LlmModelSnapshotBO.RouteBO> candidates;

    /**
     * 中文说明：返回 candidates 的不可变快照副本，避免写出阶段与重试循环共享同一条可变列表。
     * English summary: Returns an immutable snapshot of candidates so the write-out stage and the retry loop never share a
     * mutable list.
     */
    public List<LlmModelSnapshotBO.RouteBO> getCandidates() {
        return candidates == null ? null : Collections.unmodifiableList(new ArrayList<>(candidates));
    }

    /**
     * 中文说明：写入时复制 candidates，保持路由顺序且不与路由端口共享其内部列表。
     * English summary: Defensively copies candidates, preserving the routing order without sharing the port's own list.
     */
    public LlmInvocationResultVO setCandidates(List<LlmModelSnapshotBO.RouteBO> candidates) {
        this.candidates = candidates == null ? null : new ArrayList<>(candidates);
        return this;
    }

    /**
     * 中文说明：返回 headers 的不可变快照副本，避免下游写出阶段修改已进入异步任务的头集合。
     * English summary: Returns an immutable snapshot of the headers so the asynchronous write-out stage cannot mutate
     * headers already handed to a task.
     */
    public Map<String, String> getHeaders() {
        return headers == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(headers));
    }

    /**
     * 中文说明：写入时复制 headers，保持声明顺序且不与调用方共享可变 Map。
     * English summary: Defensively copies headers, keeping insertion order and never sharing the caller's map.
     */
    public LlmInvocationResultVO setHeaders(Map<String, String> headers) {
        this.headers = headers == null ? null : new LinkedHashMap<>(headers);
        return this;
    }
}
