package top.egon.cola.component.yuheng.admin.mcp.repository.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import top.egon.cola.component.yuheng.admin.mcp.domain.enums.McpCapabilityKindEnum;

/**
 * 中文说明：{@code McpCapabilityBinding} 是 MCP 能力草稿在持久边界的按种类内容形态载体：五类能力共用同一端口，
 * 但各自把 {@code content} 映射的哪些键展开到哪些专属列、哪些键必填、哪些键可空并不相同，本类按 {@link McpCapabilityKindEnum}
 * 固化这份「键序 + 必填/可空划分」，并按旧实现的列序产出取值结果，使 {@link MpMcpCapabilityDraftRepository} 只需把有序值
 * 交给各表自己的类型化列。本类不再生成任何 SQL 片段——原样携带的 {@code columns}/{@code updateAssignments} 字符串片段已被
 * 类型化列引用取代而失去消费者，故按计划的死代码清理要求移除，只保留仍被消费的业务形态信息。
 * English summary: {@code McpCapabilityBinding} is the per-kind content-shape carrier used by the MCP capability draft persistence
 * boundary: the five capability kinds share one port, yet each expands different {@code content} keys into its own dedicated columns
 * with its own required/optional split. This class fixes that key order and required/optional partition per {@link
 * McpCapabilityKindEnum} and yields the values in the legacy column order, so {@link MpMcpCapabilityDraftRepository} only has to hand
 * the ordered values to each table's typed columns. It no longer emits any SQL: the {@code columns} and {@code updateAssignments}
 * fragments it used to carry lost their consumer once typed column references replaced them, so they are dropped as the plan's dead
 * helper cleanup requires and only the still-consumed business shape survives.
 *
 * 用法 / Usage: 仅由 {@link MpMcpCapabilityDraftRepository} 通过 {@link #of(McpCapabilityKindEnum)} 取得，不作为 Spring bean，
 * 也不对端口或服务暴露；产出的有序值与旧实现绑定的参数位置一一对应，异常文案沿用旧实现。
 * Obtained only by {@link MpMcpCapabilityDraftRepository} through {@link #of(McpCapabilityKindEnum)}; neither a Spring bean nor
 * visible to ports or services. The ordered values line up one-to-one with the legacy bound parameter positions and the exception
 * wording is carried over unchanged.
 */
public class McpCapabilityBinding {

    /**
     * 中文说明：本绑定服务的能力种类，决定专属列集合与名称列。
     * English summary: The capability kind this binding serves, which decides the dedicated column set and the name column.
     */
    private final McpCapabilityKindEnum kind;

    /**
     * 中文说明：必须非空的内容键，按旧实现的专属列顺序排列。
     * English summary: The content keys that must be non-blank, in the legacy dedicated-column order.
     */
    private final List<String> requiredContentKeys;

    /**
     * 中文说明：允许为空的内容键，按旧实现的专属列顺序排列，紧随必填键之后。
     * English summary: The content keys that may stay null, in the legacy dedicated-column order, following the required keys.
     */
    private final List<String> optionalContentKeys;

    /**
     * 中文说明：创建 {@code McpCapabilityBinding} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须
     * 满足的输入契约。列表在构造时做不可变拷贝，避免调用方事后改写种类形态。
     * English summary: Creates an instance of {@code McpCapabilityBinding} from the dependencies or initial data required at
     * construction time; its parameters define the initialization contract. Both key lists are copied immutably so a caller cannot
     * rewrite the kind's shape afterwards.
     *
     * 用法 / Usage: 由 {@link #of(McpCapabilityKindEnum)} 内部调用。/ Invoked internally by {@link #of(McpCapabilityKindEnum)}.
     * @param kind 参数 能力种类；parameter capability kind。
     * @param requiredContentKeys 参数 必填内容键；parameter required content keys。
     * @param optionalContentKeys 参数 可空内容键；parameter optional content keys。
     */
    McpCapabilityBinding(
            McpCapabilityKindEnum kind,
            List<String> requiredContentKeys,
            List<String> optionalContentKeys) {
        this.kind = kind;
        this.requiredContentKeys = Collections.unmodifiableList(
                new ArrayList<>(requiredContentKeys)
        );
        this.optionalContentKeys = Collections.unmodifiableList(
                new ArrayList<>(optionalContentKeys)
        );
    }

    /**
     * 中文说明：执行 of 操作，按能力种类给出其内容形态，逐字复刻旧 {@code legacy JDBC CapabilityDraft facade.binding(...)} 的
     * switch 分支与键序。
     * English summary: Executes the of operation, returning the content shape of a capability kind and reproducing the switch branches
     * and key order of the legacy {@code legacy JDBC CapabilityDraft facade.binding(...)} verbatim.
     *
     * 用法 / Usage: {@code McpCapabilityBinding.of(draft.getKind())}。
     * @param kind 参数 能力种类；parameter capability kind。
     * @return 返回 内容形态绑定；returns the content shape binding.
     */
    public static McpCapabilityBinding of(McpCapabilityKindEnum kind) {
        return switch (java.util.Objects.requireNonNull(kind, "kind")) {
            case RESOURCE -> new McpCapabilityBinding(
                    kind,
                    List.of("uri", "driverType"),
                    List.of("operationId", "remoteMountId")
            );
            case RESOURCE_TEMPLATE -> new McpCapabilityBinding(
                    kind,
                    List.of("uriTemplate", "driverType"),
                    List.of("operationId", "remoteMountId")
            );
            case PROMPT -> new McpCapabilityBinding(
                    kind,
                    List.of("sourceType"),
                    List.of("operationId", "remoteMountId")
            );
            case TASK_POLICY -> new McpCapabilityBinding(
                    kind,
                    List.of(),
                    List.of()
            );
            case APP_BINDING -> new McpCapabilityBinding(
                    kind,
                    List.of("appArtifactId"),
                    List.of()
            );
        };
    }

    /**
     * 中文说明：执行 kind 操作，回读本绑定服务的能力种类。
     * English summary: Executes the kind operation, reading back the capability kind this binding serves.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code binding.kind()}。
     * @return 返回 能力种类；returns the capability kind.
     */
    public McpCapabilityKindEnum kind() {
        return kind;
    }

    /**
     * 中文说明：执行 nameColumn 操作，回读该种类的草稿名称列名，用于与旧实现的列语义对齐。
     * English summary: Executes the nameColumn operation, reading back the kind's draft name column so the mapping stays aligned with
     * the legacy column semantics.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code binding.nameColumn()}。
     * @return 返回 名称列名；returns the name column.
     */
    public String nameColumn() {
        return kind.nameColumn();
    }

    /**
     * 中文说明：执行 size 操作，返回专属列取值个数，等于必填键与可空键之和，供仓储校验有序值长度。
     * English summary: Executes the size operation, returning how many dedicated-column values this kind carries, the sum of the
     * required and optional keys, which the repository uses to check the ordered value length.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code binding.size()}。
     * @return 返回 取值个数；returns the value count.
     */
    public int size() {
        return requiredContentKeys.size() + optionalContentKeys.size();
    }

    /**
     * 中文说明：执行 values 操作，按旧实现的专属列顺序产出内容取值：必填键缺失或空白即抛
     * {@code capability content <key> is required}，可空键缺失或空白如实产出 {@code null}，其余取值去首尾空白——与旧
     * {@code requiredContent}/{@code optionalContent} 完全一致。
     * English summary: Executes the values operation, producing the content values in the legacy dedicated-column order: a missing or
     * blank required key raises {@code capability content <key> is required}, a missing or blank optional key truthfully yields
     * {@code null}, and every other value is trimmed — identical to the legacy {@code requiredContent} and {@code optionalContent}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code binding.values(draft.getContent())}，返回值按位置交给该种类的各类型化列。
     * @param content 参数 能力内容映射；parameter capability content map。
     * @return 返回 有序取值；returns the ordered values.
     */
    public List<String> values(Map<String, Object> content) {
        List<String> values = new ArrayList<>(size());
        for (String key : requiredContentKeys) {
            values.add(required(content, key));
        }
        for (String key : optionalContentKeys) {
            values.add(optional(content, key));
        }
        return Collections.unmodifiableList(values);
    }

    /**
     * 中文说明：执行 required 操作，按旧实现语义读取必填内容键。
     * English summary: Executes the required operation, reading a mandatory content key with the legacy semantics.
     *
     * 用法 / Usage: 仅由 {@link #values(Map)} 调用。
     * @param content 参数 能力内容映射；parameter capability content map。
     * @param key 参数 键；parameter key。
     * @return 返回 非空文本；returns the non-blank text.
     */
    private static String required(Map<String, Object> content, String key) {
        Object value = content == null ? null : content.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException(
                    "capability content " + key + " is required"
            );
        }
        return value.toString().trim();
    }

    /**
     * 中文说明：执行 optional 操作，按旧实现语义读取可空内容键。
     * English summary: Executes the optional operation, reading an optional content key with the legacy semantics.
     *
     * 用法 / Usage: 仅由 {@link #values(Map)} 调用。
     * @param content 参数 能力内容映射；parameter capability content map。
     * @param key 参数 键；parameter key。
     * @return 返回 可空文本；returns the optional text.
     */
    private static String optional(Map<String, Object> content, String key) {
        Object value = content == null ? null : content.get(key);
        return value == null || value.toString().isBlank()
                ? null
                : value.toString().trim();
    }
}
