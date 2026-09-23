package top.egon.cola.component.yuheng.admin.mcp.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import top.egon.cola.component.common.core.enums.EgonEnum;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 中文说明：{@code McpPersistentDialectEnum} 是枚举类型，负责MCP 持久任务协议方言的职责与边界，并保持原 wire 字符串不变。
 * English summary: {@code McpPersistentDialectEnum} is an enumeration that owns the MCP persistent task protocol dialect responsibility and boundary while keeping the original wire values.
 *
 * 用法 / Usage: 数据库列与 JSON 均只使用 {@code wireValue}，{@code code} 仅为发布后不变的内部标识，禁止以 ordinal 作为业务编码；/ Persist and serialize {@code wireValue} only; {@code code} is an internal identifier that never changes after release and ordinal is never a business value.
 */
public enum McpPersistentDialectEnum implements EgonEnum {

    STABLE_2025_11_25(10, "STABLE_2025_11_25", "稳定版 2025-11-25 方言"),
    RC_2026_07_28(20, "RC_2026_07_28", "候选版 2026-07-28 方言"),
    LEGACY_2024_SSE(30, "LEGACY_2024_SSE", "传统 2024 SSE 方言");

    private static final Map<String, McpPersistentDialectEnum> WIRE_VALUES = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(McpPersistentDialectEnum::wireValue, Function.identity()));

    private final int code;

    @EnumValue
    private final String wireValue;

    private final String message;

    McpPersistentDialectEnum(int code, String wireValue, String message) {
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
    public static McpPersistentDialectEnum fromWire(String wireValue) {
        if (wireValue == null) {
            throw new IllegalArgumentException("McpPersistentDialectEnum wire value is required");
        }
        McpPersistentDialectEnum matched = WIRE_VALUES.get(wireValue);
        if (matched == null) {
            throw new IllegalArgumentException("Unknown McpPersistentDialectEnum wire value: " + wireValue);
        }
        return matched;
    }
}
