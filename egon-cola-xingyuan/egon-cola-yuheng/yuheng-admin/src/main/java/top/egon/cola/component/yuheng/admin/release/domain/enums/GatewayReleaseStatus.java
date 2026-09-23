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
 * 中文说明：{@code GatewayReleaseStatus} 是枚举类型，位于当前 Gateway 模块的相关包中，负责网关发布 Status 相关的职责与边界，并保持原 wire 字符串不变。
 * English summary: {@code GatewayReleaseStatus} is an enumeration in the current Gateway module; it owns the gateway release status-related responsibility and boundary while keeping the original wire values.
 *
 * 用法 / Usage: 数据库列与 JSON 均只使用 {@code wireValue}，{@code code} 仅为发布后不变的内部标识，禁止以 ordinal 作为业务编码；/ Persist and serialize {@code wireValue} only; {@code code} is an internal identifier that never changes after release and ordinal is never a business value.
 */
public enum GatewayReleaseStatus implements EgonEnum {

    /** 中文说明：发布已创建；English summary: the release was created. */
    CREATED(10, "CREATED", "已创建"),
    /** 中文说明：发布校验中；English summary: the release is being validated. */
    VALIDATING(20, "VALIDATING", "校验中"),
    /** 中文说明：发布已就绪；English summary: the release is ready. */
    READY(30, "READY", "已就绪"),
    /** 中文说明：发布执行中；English summary: the release is being published. */
    PUBLISHING(40, "PUBLISHING", "发布中"),
    /** 中文说明：发布成功；English summary: the release succeeded. */
    SUCCESS(50, "SUCCESS", "发布成功"),
    /** 中文说明：发布失败；English summary: the release failed. */
    FAILED(60, "FAILED", "发布失败"),
    /** 中文说明：发布超时；English summary: the release timed out. */
    TIMEOUT(70, "TIMEOUT", "发布超时"),
    /** 中文说明：发布结果未知；English summary: the release result is unknown. */
    UNKNOWN(80, "UNKNOWN", "结果未知"),
    /** 中文说明：发布已被取代；English summary: the release was superseded. */
    SUPERSEDED(90, "SUPERSEDED", "已被取代");

    private static final Map<String, GatewayReleaseStatus> WIRE_VALUES = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(GatewayReleaseStatus::wireValue, Function.identity()));

    private final int code;

    @EnumValue
    private final String wireValue;

    private final String message;

    GatewayReleaseStatus(int code, String wireValue, String message) {
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
    public static GatewayReleaseStatus fromWire(String wireValue) {
        if (wireValue == null) {
            throw new IllegalArgumentException("GatewayReleaseStatus wire value is required");
        }
        GatewayReleaseStatus matched = WIRE_VALUES.get(wireValue);
        if (matched == null) {
            throw new IllegalArgumentException("Unknown GatewayReleaseStatus wire value: " + wireValue);
        }
        return matched;
    }
}
