package top.egon.cola.component.yuheng.llm.proxy.domain.exception;

import org.apache.commons.lang3.StringUtils;

import java.io.Serial;

/**
 * 中文说明：{@code LlmInvocationException} 是模型协议面唯一的<b>安全原生错误载体</b>，只携带四件事：协议原生 HTTP
 * 状态、稳定机器码、可选的协议参数名，以及可重试位。它是「可以直接出网」的类型——四个同协议 Strategy 与
 * {@code LlmApiController} 只允许从这四个字段构造原生错误对象（Chat/Embeddings/Responses 的 {@code error} 内层
 * {@code message}、{@code type}、{@code param}、{@code code}，与 Messages 的顶层 {@code type=error} 加
 * {@code error} 子对象），
 * 因此载体本身被设计成<b>不可能</b>泄漏敏感信息：没有 baseUrl、没有 secretRef、没有凭据值、没有上游响应体、
 * 没有 prompt 或工具参数，也刻意不持有 {@code cause}——上游的原始异常一旦成为本异常的字段，任何一次
 * {@code log.error("...", e)} 或全局异常处理器都会把它带上线，所以原始失败只允许在抛出点就地脱敏记录。
 * 稳定机器码取自主业务 Spec §9.2.1/§9.2.2/§9.2.29/§9.2.30 的错误表（例如 {@code unsupported_parameter}、
 * {@code model_forbidden}、{@code model_unavailable}、{@code request_too_large}、{@code rate_limit_exceeded}、
 * {@code upstream_protocol_error}、{@code upstream_timeout}），必须是 Spec 给定的字面量，不得由实现者即兴发明；
 * 消息必须是稳定的安全说明，绝不拼接上游原文。本异常<b>不是</b> admin 的业务错误 wrapper，也永不与
 * {@code code}/{@code data} 信封混用；{@code retryable} 只表达「能否在下一次请求中重试」，而 {@code 429} 的
 * {@code Retry-After} 数值不属于本载体，由协议面从被过滤后的安全响应头另行取得。
 * English summary: {@code LlmInvocationException} is the model protocol face's single <b>safe native error carrier</b>,
 * holding exactly four things: the protocol-native HTTP status, a stable machine code, an optional protocol parameter
 * name and the retryable flag. It is designed to be wire-able as-is — the four same-protocol Strategies and
 * {@code LlmApiController} may build a native error object (the {@code error.{message,type,param,code}} shape of
 * Chat/Embeddings/Responses and the {@code {type:"error",error:{type,message}}} shape of Messages) from these four
 * fields only — so the carrier itself cannot leak anything sensitive: no base URL, no secretRef, no credential value, no
 * upstream payload, no prompt or tool arguments, and deliberately no {@code cause}, because once a raw upstream failure
 * becomes a field of this exception any {@code log.error("...", e)} or global handler would put it on the wire; a raw
 * failure is therefore masked at its throw site only. Stable machine codes come from the primary business Spec's error
 * tables §9.2.1/§9.2.2/§9.2.29/§9.2.30 (for example {@code unsupported_parameter}, {@code model_forbidden},
 * {@code model_unavailable}, {@code request_too_large}, {@code rate_limit_exceeded}, {@code upstream_protocol_error},
 * {@code upstream_timeout}) and must be those literals rather than invented by an implementer; the message is a stable
 * safe description that never concatenates upstream text. This exception is <b>not</b> the admin business wrapper and is
 * never put into a {@code code}/{@code data} envelope; {@code retryable} only says whether a <i>next</i> request may be
 * retried, and the numeric {@code Retry-After} of a 429 is not part of this carrier — the protocol face takes it
 * separately from a filtered, safe response header.
 *
 * 用法 / Usage: 由同协议 Strategy 与 {@code LlmInvocationService} 的调用面抛出，由
 * {@code LlmApiController}/{@code LlmServletStreamComponent} 按路由协议就地编码；构造即校验，非法状态或空白机器码
 * 直接以 {@code IllegalArgumentException} 失败，绝不退化成 200 或空错误。
 * / Thrown by the same-protocol Strategy and by callers of {@code LlmInvocationService}, then encoded in place per route
 * protocol by {@code LlmApiController}/{@code LlmServletStreamComponent}; construction validates itself, so an illegal
 * status or a blank machine code fails immediately with {@code IllegalArgumentException} and never degrades into a 200
 * or an empty error.
 */
public final class LlmInvocationException extends RuntimeException {

    /** 中文说明：与 {@code CommonException} 一致的显式序列化标识；本类型不进入持久化，也不被反序列化。 English summary: the explicit serial version uid as in {@code CommonException}; this type is never persisted nor deserialized. */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 中文说明：协议原生 HTTP 状态，直接成为响应的状态行；100..599 之外的值在构造期即拒绝。 English summary: the protocol-native HTTP status that becomes the response status line verbatim, with anything outside 100..599 rejected at construction. */
    private final int status;

    /** 中文说明：稳定机器码（Spec 错误表给定的字面量），是错误对象 {@code code} 的唯一来源；必填且不可空白。 English summary: the stable machine code given literally by the Spec error table and the only source of the error object's {@code code}; required and never blank. */
    private final String code;

    /** 中文说明：可定位的协议参数名（如 {@code model}、{@code stream}、{@code input}），允许为 null；绝不放正文、路径或凭据。 English summary: the locatable protocol parameter name (such as {@code model}, {@code stream} or {@code input}), nullable, and never carrying content, a path or a credential. */
    private final String param;

    /** 中文说明：可重试位，只表达「下一次请求可否重试」，不表达「本次已提交流可重试」；后者永远为假。 English summary: the retryable flag, saying only whether a <i>next</i> request may retry and never that an already committed stream may, which stays false by contract. */
    private final boolean retryable;

    /**
     * 中文说明：唯一的公开构造入口，四个字段一次给全，因为没有字段可以事后补齐；本方法只做形态校验，
     * 不猜测任何默认状态码或默认消息。
     * English summary: The only public constructor, taking all four fields at once because none can be completed later;
     * it validates shape only and guesses no default status, code or message.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code new LlmInvocationException(503, "model_unavailable", null, "…", true)}。
     * @param status 参数 协议原生 HTTP 状态，必须落在 100..599；parameter the protocol-native HTTP status within 100..599.
     * @param code 参数 稳定机器码，非空白；parameter the stable machine code, never blank.
     * @param param 参数 可定位的协议参数名，可为 null，空白按 null 处理；parameter the locatable parameter name, null accepted and blank normalized to null.
     * @param message 参数 面向客户端的稳定安全说明，非空白且不含上游原文；parameter the stable safe description, never blank and never carrying raw upstream text.
     * @param retryable 参数 下一次请求可否重试；parameter whether a next request may retry.
     * @throws IllegalArgumentException 状态越界、机器码或说明为空白；raised for an out-of-range status or a blank code or message.
     */
    public LlmInvocationException(int status, String code, String param, String message, boolean retryable) {
        super(message);
        if (status < 100 || status > 599) {
            throw new IllegalArgumentException("A native llm error status must be a real HTTP status: " + status);
        }
        if (StringUtils.isBlank(code)) {
            throw new IllegalArgumentException("A native llm error requires a stable machine code");
        }
        if (StringUtils.isBlank(message)) {
            throw new IllegalArgumentException("A native llm error requires a safe description");
        }
        this.status = status;
        this.code = code;
        this.param = StringUtils.trimToNull(param);
        this.retryable = retryable;
    }

    /**
     * 中文说明：读取协议原生 HTTP 状态，供控制器直接写进状态行。
     * English summary: Reads the protocol-native HTTP status for the controller to write into the status line directly.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code LlmInvocationException.getStatus()}。
     * @return 返回 100..599 之间的状态；returns a status between 100 and 599.
     */
    public int getStatus() {
        return status;
    }

    /**
     * 中文说明：读取稳定机器码，它是原生错误对象 {@code code}（OpenAI 面）的唯一来源。
     * English summary: Reads the stable machine code, which is the only source of the native error object's {@code code}
     * on the OpenAI face.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code LlmInvocationException.getCode()}。
     * @return 返回 非空白机器码；returns a non-blank machine code.
     */
    public String getCode() {
        return code;
    }

    /**
     * 中文说明：读取可定位的协议参数名；缺失表示「无法归因到单个字段」，编码为原生 {@code param: null}，不是错误。
     * English summary: Reads the locatable parameter name; absence means the failure is not attributable to a single
     * field and encodes as a native {@code param: null}, which is not itself an error.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code LlmInvocationException.getParam()}。
     * @return 返回 参数名或 null；returns the parameter name or null.
     */
    public String getParam() {
        return param;
    }

    /**
     * 中文说明：读取可重试位；本方法与 {@code cause} 无关，因为载体不持有成因。
     * English summary: Reads the retryable flag, unrelated to any {@code cause} since the carrier holds no cause.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code LlmInvocationException.isRetryable()}。
     * @return 返回 下一次请求可否重试；returns whether a next request may retry.
     */
    public boolean isRetryable() {
        return retryable;
    }
}
