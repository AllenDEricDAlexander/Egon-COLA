package top.egon.cola.component.yuheng.admin.wiki.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import top.egon.cola.component.common.core.enums.EgonEnum;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 中文说明：{@code WikiTransitionEventEnum} 是枚举类型，负责Wiki 状态迁移事件的职责与边界，并保持原 wire 字符串不变。
 * English summary: {@code WikiTransitionEventEnum} is an enumeration that owns the Wiki transition event responsibility and boundary while keeping the original wire values.
 *
 * 用法 / Usage: 数据库列与 JSON 均只使用 {@code wireValue}，{@code code} 仅为发布后不变的内部标识，禁止以 ordinal 作为业务编码；/ Persist and serialize {@code wireValue} only; {@code code} is an internal identifier that never changes after release and ordinal is never a business value.
 */
public enum WikiTransitionEventEnum implements EgonEnum {

    CREATE_REVISION(10, "CREATE_REVISION", "创建修订版本"),
    DIRECT_PUBLISH(20, "DIRECT_PUBLISH", "直接发布"),
    COMMIT_PUBLICATION(30, "COMMIT_PUBLICATION", "提交发布结果"),
    PUBLICATION_FAILURE(40, "PUBLICATION_FAILURE", "发布失败"),
    REPEAT_PUBLICATION(50, "REPEAT_PUBLICATION", "重复发布"),
    REPLACE_DRAFT(60, "REPLACE_DRAFT", "替换草稿"),
    UNPUBLISH(70, "UNPUBLISH", "撤销发布"),
    RESTORE_CONTENT(80, "RESTORE_CONTENT", "恢复内容"),
    SUBMIT_REVIEW(90, "SUBMIT_REVIEW", "提交评审"),
    APPROVE(100, "APPROVE", "通过评审"),
    REJECT(110, "REJECT", "驳回评审"),
    CANCEL_REVIEW(120, "CANCEL_REVIEW", "取消评审"),
    REVIEWED_PUBLISH(130, "REVIEWED_PUBLISH", "评审后发布");

    private static final Map<String, WikiTransitionEventEnum> WIRE_VALUES = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(WikiTransitionEventEnum::wireValue, Function.identity()));

    private final int code;

    @EnumValue
    private final String wireValue;

    private final String message;

    WikiTransitionEventEnum(int code, String wireValue, String message) {
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
    public static WikiTransitionEventEnum fromWire(String wireValue) {
        if (wireValue == null) {
            throw new IllegalArgumentException("WikiTransitionEventEnum wire value is required");
        }
        WikiTransitionEventEnum matched = WIRE_VALUES.get(wireValue);
        if (matched == null) {
            throw new IllegalArgumentException("Unknown WikiTransitionEventEnum wire value: " + wireValue);
        }
        return matched;
    }
}
