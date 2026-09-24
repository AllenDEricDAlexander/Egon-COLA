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
 * 中文说明：{@code KnowledgeSearchModeEnum} 是 API-022 召回算法的机器词汇，只有
 * {@code VECTOR}（pgvector 余弦距离排序）、{@code KEYWORD}（转义后的字面子串匹配）与
 * {@code HYBRID}（两路各自有界召回后按 RRF k=60 融合）三个成员；缺省值是 {@code VECTOR}。
 * 它是 Rule 9 策略注册表唯一的键空间：三种实现各服务一个成员，领取不到对应实现即失败关闭，
 * 任何地方都不允许对召回算法做 {@code switch} 或字符串比较分支。
 * English summary: {@code KnowledgeSearchModeEnum} is the machine vocabulary of the API-022 recall algorithm, holding
 * exactly {@code VECTOR} (pgvector cosine-distance ordering), {@code KEYWORD} (escaped literal substring matching) and
 * {@code HYBRID} (two bounded authorized recalls fused by reciprocal rank fusion with k=60), defaulting to
 * {@code VECTOR}. It is the only key space of the Rule 9 strategy registry: three implementations serve one member
 * each and a missing implementation fails closed, so no {@code switch} or string comparison over the algorithm is
 * allowed anywhere.
 *
 * 用法 / Usage: {@code KnowledgeSearchModeEnum.fromWire(command.getSearchMode())}，{@code null} 入参由调用方
 * 先落到 {@code VECTOR} 缺省；未知字面量抛 {@link IllegalArgumentException}，由控制器Advice翻成 422。
 * 持久化侧该词汇不落任何列，只作为策略键存在，因此 {@code @EnumValue} 只保证一旦需要入库时写的是字符串。
 */
public enum KnowledgeSearchModeEnum implements EgonEnum {

    /** 向量余弦召回 / cosine vector recall. */
    VECTOR(10, "VECTOR", "向量召回"),

    /** 关键词子串召回 / keyword substring recall. */
    KEYWORD(20, "KEYWORD", "关键词召回"),

    /** 两路 RRF 融合召回 / reciprocal-rank-fusion recall. */
    HYBRID(30, "HYBRID", "混合召回");

    private static final Map<String, KnowledgeSearchModeEnum> WIRE_VALUES = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(KnowledgeSearchModeEnum::wireValue, Function.identity()));

    private final int code;

    @EnumValue
    private final String wireValue;

    private final String message;

    KnowledgeSearchModeEnum(int code, String wireValue, String message) {
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
     * 中文说明：本模式是否需要一次查询向量。把「要不要先嵌入」表达成枚举谓词而不是服务里的模式比较，
     * 新增模式时只需在此补一个事实，服务侧的编排不随之加分支。
     * English summary: whether this mode needs a query embedding. Carrying "embed first or not" as an enum predicate rather
     * than a mode comparison inside the service means a new mode adds one fact here instead of another service branch.
     *
     * 用法 / Usage: {@code searchMode.requiresQueryVector()}。
     * @return 返回 是否需要查询向量；returns whether a query vector is required.
     */
    public boolean requiresQueryVector() {
        return this == VECTOR || this == HYBRID;
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
     * 中文说明：按字面量解析召回模式，{@code null} 与未知值一律 {@link IllegalArgumentException}；
     * 缺省（{@code VECTOR}）由调用方在解析前显式决定，本方法不替调用方猜默认值。
     * English summary: Parses a recall mode by token, rejecting {@code null} and unknown values with
     * {@link IllegalArgumentException}; the {@code VECTOR} default is decided explicitly by the caller before parsing, so
     * this method never guesses on the caller's behalf.
     *
     * 用法 / Usage: {@code KnowledgeSearchModeEnum.fromWire("HYBRID")}。
     * @param wireValue 参数 请求里的字面量；parameter the token carried by the request.
     * @return 返回 对应枚举；returns the matching constant.
     */
    @JsonCreator
    public static KnowledgeSearchModeEnum fromWire(String wireValue) {
        KnowledgeSearchModeEnum resolved = wireValue == null ? null : WIRE_VALUES.get(wireValue);
        if (resolved == null) {
            throw new IllegalArgumentException("Unknown knowledge search mode: " + wireValue);
        }
        return resolved;
    }
}
