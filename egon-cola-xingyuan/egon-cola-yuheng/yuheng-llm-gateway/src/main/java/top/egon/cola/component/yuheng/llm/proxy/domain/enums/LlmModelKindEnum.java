package top.egon.cola.component.yuheng.llm.proxy.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import top.egon.cola.component.common.core.enums.EgonEnum;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 中文说明：模型种类；跨 admin 与 llm-gateway 两个进程以相同常量、code 与 wireValue 各自声明一份。
 * English summary: Kind of an LLM model; declared separately with identical constants, codes and wire values in the admin and llm-gateway processes.
 *
 * 用法 / Usage: 持久化列与 JSON 都只使用 {@code wireValue}，{@code code} 仅是组件内部稳定标识且按声明顺序取十位间隔，禁止 ordinal；
 * both the persisted VARCHAR column and the JSON payload carry {@code wireValue} while {@code code} is an internal stable identifier assigned on a tens interval, never an ordinal.
 */
public enum LlmModelKindEnum implements EgonEnum {

    CHAT(10, "conversational model kind", "CHAT"),

    EMBEDDING(20, "embedding model kind", "EMBEDDING");

    private static final Map<String, LlmModelKindEnum> BY_WIRE_VALUE = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(LlmModelKindEnum::wireValue, kind -> kind));

    private final int code;

    private final String message;

    @EnumValue
    private final String wireValue;

    LlmModelKindEnum(int code, String message, String wireValue) {
        this.code = code;
        this.message = message;
        this.wireValue = wireValue;
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
     * 中文说明：仅按不可变 wire 表精确解析，未知值与 null 一律拒绝。
     * English summary: Resolves through the immutable wire map only; unknown values and null are rejected.
     */
    @JsonCreator
    public static LlmModelKindEnum fromWire(String wireValue) {
        LlmModelKindEnum kind = wireValue == null ? null : BY_WIRE_VALUE.get(wireValue);
        if (kind == null) {
            throw new IllegalArgumentException("Unsupported llm model kind wire value: " + wireValue);
        }
        return kind;
    }
}
