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
 * 中文说明：{@code McpCapabilityKindEnum} 是枚举类型，位于当前 Gateway 模块的相关包中，负责 CapabilityKind 相关的职责与边界，并保持原 wire 字符串不变。
 * English summary: {@code McpCapabilityKindEnum} is an enumeration in the current Gateway module; it owns the capability kind-related responsibility and boundary while keeping the original wire values.
 *
 * 用法 / Usage: 数据库列与 JSON 均只使用 {@code wireValue}，{@code code} 仅为发布后不变的内部标识，禁止以 ordinal 作为业务编码；/ Persist and serialize {@code wireValue} only; {@code code} is an internal identifier that never changes after release and ordinal is never a business value.
 */
public enum McpCapabilityKindEnum implements EgonEnum {

    /** 中文说明：资源草稿能力；English summary: the resource draft capability. */
    RESOURCE(10, "RESOURCE", "资源", "gateway_mcp_resource_draft", "resource_name"),
    /** 中文说明：资源模板草稿能力；English summary: the resource template draft capability. */
    RESOURCE_TEMPLATE(
            20,
            "RESOURCE_TEMPLATE",
            "资源模板",
            "gateway_mcp_resource_template_draft",
            "template_name"
    ),
    /** 中文说明：提示词草稿能力；English summary: the prompt draft capability. */
    PROMPT(30, "PROMPT", "提示词", "gateway_mcp_prompt_draft", "prompt_name"),
    /** 中文说明：任务策略草稿能力；English summary: the task policy draft capability. */
    TASK_POLICY(40, "TASK_POLICY", "任务策略", "gateway_mcp_task_policy_draft", "tool_name"),
    /** 中文说明：应用绑定草稿能力；English summary: the app binding draft capability. */
    APP_BINDING(50, "APP_BINDING", "应用绑定", "gateway_mcp_app_binding_draft", "tool_name");

    private static final Map<String, McpCapabilityKindEnum> WIRE_VALUES = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(McpCapabilityKindEnum::wireValue, Function.identity()));

    private final int code;

    @EnumValue
    private final String wireValue;

    private final String message;

    private final String table;

    private final String nameColumn;

    McpCapabilityKindEnum(int code, String wireValue, String message, String table, String nameColumn) {
        this.code = code;
        this.wireValue = wireValue;
        this.message = message;
        this.table = table;
        this.nameColumn = nameColumn;
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
    public static McpCapabilityKindEnum fromWire(String wireValue) {
        if (wireValue == null) {
            throw new IllegalArgumentException("McpCapabilityKindEnum wire value is required");
        }
        McpCapabilityKindEnum matched = WIRE_VALUES.get(wireValue);
        if (matched == null) {
            throw new IllegalArgumentException("Unknown McpCapabilityKindEnum wire value: " + wireValue);
        }
        return matched;
    }

    public String table() {
        return table;
    }

    public String nameColumn() {
        return nameColumn;
    }
}
