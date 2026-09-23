package top.egon.cola.component.yuheng.admin.release.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import top.egon.cola.component.common.core.enums.EgonEnum;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 中文说明：{@code GatewayPublicationStatusEnum} 是枚举类型，位于当前 Gateway 模块的相关包中，负责 GatewayPublicationStatusEnum 相关的职责与边界，并保持原 wire 字符串不变。
 * English summary: {@code GatewayPublicationStatusEnum} is an enumeration in the current Gateway module; it owns the publication status-related responsibility and boundary while keeping the original wire values.
 *
 * 用法 / Usage: 数据库列与 JSON 均只使用 {@code wireValue}，{@code code} 仅为发布后不变的内部标识，禁止以 ordinal 作为业务编码；/ Persist and serialize {@code wireValue} only; {@code code} is an internal identifier that never changes after release and ordinal is never a business value.
 */
public enum GatewayPublicationStatusEnum implements EgonEnum {

    /** 中文说明：发布计划已登记；English summary: the publication is planned. */
    PLANNED(10, "PLANNED", "已计划"),
    /** 中文说明：发布目标已解析；English summary: the publication targets are resolved. */
    RESOLVED(20, "RESOLVED", "已解析"),
    /** 中文说明：发布已提交；English summary: the publication was submitted. */
    SUBMITTED(30, "SUBMITTED", "已提交"),
    /** 中文说明：发布成功；English summary: the publication succeeded. */
    SUCCESS(40, "SUCCESS", "发布成功"),
    /** 中文说明：发布失败；English summary: the publication failed. */
    FAILED(50, "FAILED", "发布失败"),
    /** 中文说明：发布部分成功；English summary: the publication partially succeeded. */
    PARTIAL_SUCCESS(60, "PARTIAL_SUCCESS", "部分成功"),
    /** 中文说明：发布超时；English summary: the publication timed out. */
    TIMEOUT(70, "TIMEOUT", "发布超时"),
    /** 中文说明：发布结果未知；English summary: the publication result is unknown. */
    UNKNOWN(80, "UNKNOWN", "结果未知");

    private static final Map<String, GatewayPublicationStatusEnum> WIRE_VALUES = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(GatewayPublicationStatusEnum::wireValue, Function.identity()));

    private final int code;

    @EnumValue
    private final String wireValue;

    private final String message;

    GatewayPublicationStatusEnum(int code, String wireValue, String message) {
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
    public static GatewayPublicationStatusEnum fromWire(String wireValue) {
        if (wireValue == null) {
            throw new IllegalArgumentException("GatewayPublicationStatusEnum wire value is required");
        }
        GatewayPublicationStatusEnum matched = WIRE_VALUES.get(wireValue);
        if (matched == null) {
            throw new IllegalArgumentException("Unknown GatewayPublicationStatusEnum wire value: " + wireValue);
        }
        return matched;
    }

    /**
     * 中文说明：判断当前发布状态是否已是终态结果。
     * English summary: Reports whether the publication status is already a terminal result.
     *
     * @return 返回 terminalResult 的处理结果；returns the result of the operation.
     */
    public boolean terminalResult() {
        return switch (this) {
            case SUCCESS, FAILED, PARTIAL_SUCCESS, TIMEOUT, UNKNOWN ->
                    true;
            case PLANNED, RESOLVED, SUBMITTED -> false;
        };
    }
}
