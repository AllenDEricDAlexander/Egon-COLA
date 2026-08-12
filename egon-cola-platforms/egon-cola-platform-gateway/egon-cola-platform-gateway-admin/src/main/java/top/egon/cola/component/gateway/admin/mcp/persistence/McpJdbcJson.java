package top.egon.cola.component.gateway.admin.mcp.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * 中文说明：{@code McpJdbcJson} 是类型，位于当前 Gateway 模块的相关包中，负责MCPJdbcJson相关的职责与边界。
 * English summary: {@code McpJdbcJson} is a type in the current Gateway module; it owns the mcp jdbc json-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
final class McpJdbcJson {

    /**
     * 中文说明：表示 MAP 这一固定值；它属于 {@code McpJdbcJson} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value map; it is a state, type, or protocol value of {@code McpJdbcJson} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code McpJdbcJson} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpJdbcJson}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final TypeReference<Map<String, Object>> MAP =
            new TypeReference<>() {
            };

    /**
     * 中文说明：表示 STRINGSET 这一固定值；它属于 {@code McpJdbcJson} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value string set; it is a state, type, or protocol value of {@code McpJdbcJson} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code McpJdbcJson} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpJdbcJson}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final TypeReference<Set<String>> STRING_SET =
            new TypeReference<>() {
            };

    /**
     * 中文说明：保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code McpJdbcJson} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code McpJdbcJson} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code McpJdbcJson} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpJdbcJson}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：创建 {@code McpJdbcJson} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpJdbcJson} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param objectMapper 参数 object映射器；parameter object mapper。
     */
    McpJdbcJson(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy();
    }

    /**
     * 中文说明：执行 write 操作；该方法是 {@code McpJdbcJson} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the write operation; this method is the invocation entry point on {@code McpJdbcJson} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpJdbcJson.write(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 write 的处理结果；returns the result of the operation.
     */
    String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException(
                    "MCP persistence value cannot be serialized",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 map 操作；该方法是 {@code McpJdbcJson} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the map operation; this method is the invocation entry point on {@code McpJdbcJson} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpJdbcJson.map(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 map 的处理结果；returns the result of the operation.
     */
    Map<String, Object> map(String value) {
        try {
            return Map.copyOf(objectMapper.readValue(value, MAP));
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException(
                    "stored MCP persistence value is invalid",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 stringSet 操作；该方法是 {@code McpJdbcJson} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the string set operation; this method is the invocation entry point on {@code McpJdbcJson} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpJdbcJson.stringSet(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 stringSet 的处理结果；returns the result of the operation.
     */
    Set<String> stringSet(String value) {
        try {
            return Set.copyOf(objectMapper.readValue(value, STRING_SET));
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException(
                    "stored MCP string set is invalid",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 timestamp 操作；该方法是 {@code McpJdbcJson} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the timestamp operation; this method is the invocation entry point on {@code McpJdbcJson} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpJdbcJson.timestamp(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 timestamp 的处理结果；returns the result of the operation.
     */
    static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code McpJdbcJson} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code McpJdbcJson} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpJdbcJson.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
