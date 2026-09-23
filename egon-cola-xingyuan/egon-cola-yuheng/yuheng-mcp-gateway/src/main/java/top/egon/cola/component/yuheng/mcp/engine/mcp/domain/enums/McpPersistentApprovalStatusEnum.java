package top.egon.cola.component.yuheng.mcp.engine.mcp.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import top.egon.cola.component.common.core.enums.EgonEnum;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 中文说明：持久化 MCP 审批记录的状态；跨 admin 与 mcp-gateway 两个进程以相同常量、code 与 wireValue 各自声明一份。
 * English summary: State of a persisted MCP approval record; declared separately with identical constants, codes and wire values in the admin and mcp-gateway processes.
 *
 * 用法 / Usage: 持久化列与 JSON 都只使用 {@code wireValue}（DDL CHECK 的四个稳定字符串），{@code code} 仅是组件内部稳定标识且按声明顺序取十位间隔，禁止 ordinal；
 * both the persisted VARCHAR column and the JSON payload carry {@code wireValue} (the four stable CHECK strings) while {@code code} is an internal stable identifier assigned on a tens interval, never an ordinal.
 */
public enum McpPersistentApprovalStatusEnum implements EgonEnum {

    PENDING(10, "approval has not been decided yet", "PENDING"),

    CONSUMED(20, "approval was consumed by one execution", "CONSUMED"),

    EXPIRED(30, "approval passed its validity window", "EXPIRED"),

    REVOKED(40, "approval was revoked before use", "REVOKED");

    private static final Map<String, McpPersistentApprovalStatusEnum> BY_WIRE_VALUE = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(
                    McpPersistentApprovalStatusEnum::wireValue, status -> status));

    private final int code;

    private final String message;

    @EnumValue
    private final String wireValue;

    McpPersistentApprovalStatusEnum(int code, String message, String wireValue) {
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
    public static McpPersistentApprovalStatusEnum fromWire(String wireValue) {
        McpPersistentApprovalStatusEnum status = wireValue == null ? null : BY_WIRE_VALUE.get(wireValue);
        if (status == null) {
            throw new IllegalArgumentException("Unsupported mcp approval status wire value: " + wireValue);
        }
        return status;
    }
}
