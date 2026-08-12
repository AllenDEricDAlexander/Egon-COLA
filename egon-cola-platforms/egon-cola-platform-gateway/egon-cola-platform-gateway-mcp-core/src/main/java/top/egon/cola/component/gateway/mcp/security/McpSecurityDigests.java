package top.egon.cola.component.gateway.mcp.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * 中文说明：{@code McpSecurityDigests} 是类型，位于当前 Gateway 模块的相关包中，负责MCP安全Digests相关的职责与边界。
 * English summary: {@code McpSecurityDigests} is a type in the current Gateway module; it owns the mcp security digests-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpSecurityDigests {

    /**
     * 中文说明：创建 {@code McpSecurityDigests} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code McpSecurityDigests} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     */
    private McpSecurityDigests() {
    }

    /**
     * 中文说明：执行 token 操作；该方法是 {@code McpSecurityDigests} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the token operation; this method is the invocation entry point on {@code McpSecurityDigests} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSecurityDigests.token(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param token 参数 token；parameter token。
     * @return 返回 token 的处理结果；returns the result of the operation.
     */
    public static String token(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("approval token is required");
        }
        return sha256(token.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 中文说明：执行 arguments 操作；该方法是 {@code McpSecurityDigests} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the arguments operation; this method is the invocation entry point on {@code McpSecurityDigests} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSecurityDigests.arguments(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param arguments 参数 arguments；parameter arguments。
     * @return 返回 arguments 的处理结果；returns the result of the operation.
     */
    public static String arguments(
            ObjectMapper objectMapper,
            Map<String, Object> arguments) {
        Objects.requireNonNull(objectMapper, "objectMapper");
        Objects.requireNonNull(arguments, "arguments");
        try {
            JsonNode tree = objectMapper.valueToTree(arguments);
            return sha256(objectMapper.writeValueAsBytes(canonical(tree)));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "MCP arguments cannot be canonicalized",
                    exception
            );
        }
    }

    /**
     * 中文说明：执行 canonical 操作；该方法是 {@code McpSecurityDigests} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the canonical operation; this method is the invocation entry point on {@code McpSecurityDigests} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSecurityDigests.canonical(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 canonical 的处理结果；returns the result of the operation.
     */
    private static Object canonical(JsonNode value) {
        if (value.isObject()) {
            TreeMap<String, Object> sorted = new TreeMap<>();
            value.properties().forEach(entry -> sorted.put(
                    entry.getKey(),
                    canonical(entry.getValue())
            ));
            return sorted;
        }
        if (value.isArray()) {
            ArrayList<Object> items = new ArrayList<>(value.size());
            value.forEach(item -> items.add(canonical(item)));
            return items;
        }
        if (value.isNull()) {
            return null;
        }
        if (value.isTextual()) {
            return value.textValue();
        }
        if (value.isBoolean()) {
            return value.booleanValue();
        }
        if (value.isIntegralNumber()) {
            return value.bigIntegerValue();
        }
        if (value.isFloatingPointNumber()) {
            return value.decimalValue();
        }
        if (value.isBinary()) {
            try {
                return value.binaryValue();
            } catch (java.io.IOException exception) {
                throw new IllegalArgumentException(
                        "MCP binary argument cannot be canonicalized",
                        exception
                );
            }
        }
        return value.asText();
    }

    /**
     * 中文说明：执行 sha256 操作；该方法是 {@code McpSecurityDigests} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the sha256 operation; this method is the invocation entry point on {@code McpSecurityDigests} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpSecurityDigests.sha256(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 sha256 的处理结果；returns the result of the operation.
     */
    private static String sha256(byte[] value) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value)
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
