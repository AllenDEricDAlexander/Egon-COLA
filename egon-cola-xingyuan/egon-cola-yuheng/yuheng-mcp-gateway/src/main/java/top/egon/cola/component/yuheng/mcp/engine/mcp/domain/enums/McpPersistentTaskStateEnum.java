package top.egon.cola.component.yuheng.mcp.engine.mcp.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import top.egon.cola.component.common.core.enums.EgonEnum;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 中文说明：持久化 MCP 任务的生命周期状态；跨 admin 与 mcp-gateway 两个进程以相同常量、code 与 wireValue 各自声明一份。
 * English summary: Lifecycle state of a persisted MCP task; declared separately with identical constants, codes and wire values in the admin and mcp-gateway processes.
 *
 * 用法 / Usage: 持久化列与 JSON 都只使用 {@code wireValue}（DDL CHECK 的五个稳定字符串），{@code code} 仅是组件内部稳定标识且按声明顺序取十位间隔，禁止 ordinal；
 * both the persisted VARCHAR column and the JSON payload carry {@code wireValue} (the five stable CHECK strings) while {@code code} is an internal stable identifier assigned on a tens interval, never an ordinal.
 */
public enum McpPersistentTaskStateEnum implements EgonEnum {

    WORKING(10, "task is still executing", "WORKING"),

    INPUT_REQUIRED(20, "task waits for additional client input", "INPUT_REQUIRED"),

    COMPLETED(30, "task finished successfully", "COMPLETED"),

    FAILED(40, "task finished with an error", "FAILED"),

    CANCELLED(50, "task was cancelled by the client", "CANCELLED");

    private static final Map<String, McpPersistentTaskStateEnum> BY_WIRE_VALUE = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(
                    McpPersistentTaskStateEnum::wireValue, state -> state));

    private final int code;

    private final String message;

    @EnumValue
    private final String wireValue;

    McpPersistentTaskStateEnum(int code, String message, String wireValue) {
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
    public static McpPersistentTaskStateEnum fromWire(String wireValue) {
        McpPersistentTaskStateEnum state = wireValue == null ? null : BY_WIRE_VALUE.get(wireValue);
        if (state == null) {
            throw new IllegalArgumentException("Unsupported mcp persistent task state wire value: " + wireValue);
        }
        return state;
    }
}
