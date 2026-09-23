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
 * 中文说明：{@code LlmDeploymentEnum} 是枚举类型，负责LLM 部署形态的职责与边界，并保持原 wire 字符串不变。
 * English summary: {@code LlmDeploymentEnum} is an enumeration that owns the LLM deployment form responsibility and boundary while keeping the original wire values.
 *
 * 用法 / Usage: 数据库列与 JSON 均只使用 {@code wireValue}，{@code code} 仅为发布后不变的内部标识，禁止以 ordinal 作为业务编码；/ Persist and serialize {@code wireValue} only; {@code code} is an internal identifier that never changes after release and ordinal is never a business value.
 */
public enum LlmDeploymentEnum implements EgonEnum {

    LOCAL(10, "LOCAL", "本地部署"),
    CLOUD(20, "CLOUD", "云端部署");

    private static final Map<String, LlmDeploymentEnum> WIRE_VALUES = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(LlmDeploymentEnum::wireValue, Function.identity()));

    private final int code;

    @EnumValue
    private final String wireValue;

    private final String message;

    LlmDeploymentEnum(int code, String wireValue, String message) {
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
    public static LlmDeploymentEnum fromWire(String wireValue) {
        if (wireValue == null) {
            throw new IllegalArgumentException("LlmDeploymentEnum wire value is required");
        }
        LlmDeploymentEnum matched = WIRE_VALUES.get(wireValue);
        if (matched == null) {
            throw new IllegalArgumentException("Unknown LlmDeploymentEnum wire value: " + wireValue);
        }
        return matched;
    }
}
