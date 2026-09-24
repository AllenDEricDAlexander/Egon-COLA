package top.egon.cola.component.yuheng.admin.knowledge.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import top.egon.cola.component.common.core.enums.EgonEnum;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 中文说明：{@code KnowledgeSourceModeEnum} 是 API-022 取证范围词汇，三者都只在当前知识库内取证：
 * {@code DOCUMENTS} 只引用活动修订的分块，{@code WIKI} 只引用被<b>当前已发布</b> Wiki 页面所引用的那批分块，
 * {@code BOTH} 引用全部候选并为已发布页面补上 {@code pageId}。
 * 关键约束（Spec §7.3.4）：Wiki 侧<b>没有第二套页面向量</b>，检索永远先在授权活动资料分块上做，
 * 再用 {@code gateway_wiki_revision.sources} 的 {@code documentRevisionId + chunkId} 血缘扩展到当前发布版本；
 * 已归档或被更替的发布一律不算证据，stale 页面绝不进答案。
 * English summary: {@code KnowledgeSourceModeEnum} is the API-022 evidence-scope vocabulary and all three scopes stay
 * inside the one knowledge base: {@code DOCUMENTS} cites only chunks of active revisions, {@code WIKI} cites only those
 * chunks that a <b>currently published</b> wiki page draws on, and {@code BOTH} cites every candidate while attaching the
 * {@code pageId} of a published page when one exists. The decisive constraint (Spec §7.3.4) is that the wiki side carries
 * <b>no second set of page embeddings</b>: retrieval always runs first over authorized active-document chunks and only then
 * extends to the current publication through the {@code documentRevisionId + chunkId} lineage in
 * {@code gateway_wiki_revision.sources}; an archived or superseded publication is never evidence, so a stale page cannot
 * reach the answer.
 *
 * 用法 / Usage: {@code command.getSourceMode() == null ? DOCUMENTS : fromWire(...)}；
 * 检索侧只调 {@link #attachesPageLineage()} 与 {@link #requiresPublishedPage()} 两个谓词，
 * 不对本枚举做 {@code switch}（Rule 9）。
 */
public enum KnowledgeSourceModeEnum implements EgonEnum {

    /** 仅资料分块 / document chunks only. */
    DOCUMENTS(10, "DOCUMENTS", "仅资料"),

    /** 仅被已发布 Wiki 引用的资料 / only chunks drawn on by a published wiki page. */
    WIKI(20, "WIKI", "仅Wiki"),

    /** 资料与Wiki并集 / the union of documents and wiki. */
    BOTH(30, "BOTH", "资料与Wiki");

    private static final Map<String, KnowledgeSourceModeEnum> WIRE_VALUES = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(KnowledgeSourceModeEnum::wireValue, Function.identity()));

    private final int code;

    @EnumValue
    private final String wireValue;

    private final String message;

    KnowledgeSourceModeEnum(int code, String wireValue, String message) {
        this.code = code;
        this.wireValue = wireValue;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    /**
     * 中文说明：出站与解析用的稳定字面量，绝不使用 {@code ordinal()} 作为业务编码。
     * English summary: the stable wire token used for output and parsing; {@code ordinal()} is never a business code.
     *
     * 用法 / Usage: Jackson 序列化与 {@link #fromWire(String)} 的互逆面。/ the inverse face of {@link #fromWire(String)}.
     * @return 返回 大写字面量；returns the upper-case token.
     */
    @JsonValue
    public String wireValue() {
        return wireValue;
    }

    /**
     * 中文说明：是否需要在结果上补 {@code pageId}：{@code DOCUMENTS} 恒为假，{@code WIKI} 与 {@code BOTH} 为真，
     * 因为两者都要走发布血缘。该谓词直接成为检索 SQL 的可选列，避免服务里出现按模式的分支。
     * English summary: Whether a {@code pageId} must be attached: always false for {@code DOCUMENTS} and true for
     * {@code WIKI} and {@code BOTH}, since both travel through publication lineage. The predicate becomes an optional column
     * of the retrieval SQL itself, which is what keeps mode branching out of the service.
     *
     * 用法 / Usage: {@code sourceMode.attachesPageLineage()}。
     * @return 返回 是否需要页面血缘列；returns whether the page-lineage column is needed.
     */
    public boolean attachesPageLineage() {
        return this == WIKI || this == BOTH;
    }

    /**
     * 中文说明：是否要求候选分块必须被当前已发布页面引用：只有 {@code WIKI} 为真，
     * 因此 {@code BOTH} 保留无页面血缘的纯资料证据，{@code DOCUMENTS} 完全不查血缘。
     * English summary: Whether a candidate chunk must be drawn on by a currently published page: true for {@code WIKI}
     * only, which is exactly why {@code BOTH} keeps document-only evidence as well and {@code DOCUMENTS} skips lineage
     * altogether.
     *
     * 用法 / Usage: {@code sourceMode.requiresPublishedPage()}。
     * @return 返回 是否加已发布谓词；returns whether the published-page predicate applies.
     */
    public boolean requiresPublishedPage() {
        return this == WIKI;
    }

    /**
     * 中文说明：按字面量解析取证范围，{@code null} 与未知值一律 {@link IllegalArgumentException}；
     * {@code DOCUMENTS} 缺省由调用方在解析前显式决定。
     * English summary: Parses an evidence scope by token, rejecting {@code null} and unknown values with
     * {@link IllegalArgumentException}; the {@code DOCUMENTS} default is decided explicitly by the caller beforehand.
     *
     * 用法 / Usage: {@code KnowledgeSourceModeEnum.fromWire("BOTH")}。
     * @param wireValue 参数 请求里的字面量；parameter the token carried by the request.
     * @return 返回 对应枚举；returns the matching constant.
     */
    @JsonCreator
    public static KnowledgeSourceModeEnum fromWire(String wireValue) {
        KnowledgeSourceModeEnum resolved = wireValue == null ? null : WIRE_VALUES.get(wireValue);
        if (resolved == null) {
            throw new IllegalArgumentException("Unknown knowledge source mode: " + wireValue);
        }
        return resolved;
    }
}
