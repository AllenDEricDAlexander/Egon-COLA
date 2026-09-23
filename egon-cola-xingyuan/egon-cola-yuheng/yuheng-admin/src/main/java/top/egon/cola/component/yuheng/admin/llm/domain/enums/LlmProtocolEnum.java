package top.egon.cola.component.yuheng.admin.llm.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import top.egon.cola.component.common.core.enums.EgonEnum;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 中文说明：{@code LlmProtocolEnum} 是枚举类型，负责LLM 上游调用协议的职责与边界，并保持原 wire 字符串不变。
 * English summary: {@code LlmProtocolEnum} is an enumeration that owns the LLM upstream invocation protocol responsibility and boundary while keeping the original wire values.
 *
 * 用法 / Usage: 数据库列与 JSON 均只使用 {@code wireValue}，{@code code} 仅为发布后不变的内部标识，禁止以 ordinal 作为业务编码；/ Persist and serialize {@code wireValue} only; {@code code} is an internal identifier that never changes after release and ordinal is never a business value.
 */
public enum LlmProtocolEnum implements EgonEnum {

    OPENAI_CHAT(10, "OPENAI_CHAT", "OpenAI 对话协议"),
    OPENAI_EMBEDDING(20, "OPENAI_EMBEDDING", "OpenAI 向量协议"),
    OPENAI_RESPONSES(30, "OPENAI_RESPONSES", "OpenAI 响应协议"),
    ANTHROPIC_MESSAGES(40, "ANTHROPIC_MESSAGES", "Anthropic 消息协议");

    private static final Map<String, LlmProtocolEnum> WIRE_VALUES = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(LlmProtocolEnum::wireValue, Function.identity()));

    private final int code;

    @EnumValue
    private final String wireValue;

    private final String message;

    LlmProtocolEnum(int code, String wireValue, String message) {
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

    @JsonValue
    public String wireValue() {
        return wireValue;
    }

    /**
     * 中文说明：按原 wire 字符串在不可变查表中精确匹配，未知值与 null 一律拒绝而非静默兜底。
     * English summary: Looks the value up in the immutable wire map and rejects unknown or null input instead of coercing it.
     */
    @JsonCreator
    public static LlmProtocolEnum fromWire(String wireValue) {
        if (wireValue == null) {
            throw new IllegalArgumentException("LlmProtocolEnum wire value is required");
        }
        LlmProtocolEnum matched = WIRE_VALUES.get(wireValue);
        if (matched == null) {
            throw new IllegalArgumentException("Unknown LlmProtocolEnum wire value: " + wireValue);
        }
        return matched;
    }
}
