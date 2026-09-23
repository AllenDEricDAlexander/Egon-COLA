package top.egon.cola.component.yuheng.admin.observability.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import top.egon.cola.component.common.core.enums.EgonEnum;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 中文说明：{@code GatewayCallEventConsumeResultEnum} 是枚举类型，位于当前 Gateway 模块的相关包中，负责 CallEventConsumeResult 相关的职责与边界，并保持原 wire 字符串不变。
 * English summary: {@code GatewayCallEventConsumeResultEnum} is an enumeration in the current Gateway module; it owns the call event consume result-related responsibility and boundary while keeping the original wire values.
 *
 * 用法 / Usage: 数据库列与 JSON 均只使用 {@code wireValue}，{@code code} 仅为发布后不变的内部标识，禁止以 ordinal 作为业务编码；/ Persist and serialize {@code wireValue} only; {@code code} is an internal identifier that never changes after release and ordinal is never a business value.
 */
public enum GatewayCallEventConsumeResultEnum implements EgonEnum {

    /** 中文说明：事件已投影入库；English summary: the event was projected. */
    PROJECTED(10, "PROJECTED", "已投影"),
    /** 中文说明：事件重复被忽略；English summary: the event was a duplicate. */
    DUPLICATE(20, "DUPLICATE", "重复"),
    /** 中文说明：毒消息已记录；English summary: the poison event was recorded. */
    POISON_RECORDED(30, "POISON_RECORDED", "毒消息已记录");

    private static final Map<String, GatewayCallEventConsumeResultEnum> WIRE_VALUES = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(GatewayCallEventConsumeResultEnum::wireValue, Function.identity()));

    private final int code;

    @EnumValue
    private final String wireValue;

    private final String message;

    GatewayCallEventConsumeResultEnum(int code, String wireValue, String message) {
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
    public static GatewayCallEventConsumeResultEnum fromWire(String wireValue) {
        if (wireValue == null) {
            throw new IllegalArgumentException("GatewayCallEventConsumeResultEnum wire value is required");
        }
        GatewayCallEventConsumeResultEnum matched = WIRE_VALUES.get(wireValue);
        if (matched == null) {
            throw new IllegalArgumentException("Unknown GatewayCallEventConsumeResultEnum wire value: " + wireValue);
        }
        return matched;
    }
}
