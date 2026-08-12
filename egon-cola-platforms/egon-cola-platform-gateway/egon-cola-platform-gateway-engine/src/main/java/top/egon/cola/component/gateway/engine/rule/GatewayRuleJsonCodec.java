package top.egon.cola.component.gateway.engine.rule;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleActivation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

/**
 * 中文说明：{@code GatewayRuleJsonCodec} 是类型，位于当前 Gateway 模块的相关包中，负责网关规则JsonCodec相关的职责与边界。
 * English summary: {@code GatewayRuleJsonCodec} is a type in the current Gateway module; it owns the gateway rule json codec-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayRuleJsonCodec {

    /**
     * 中文说明：保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code GatewayRuleJsonCodec} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code GatewayRuleJsonCodec} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRuleJsonCodec} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleJsonCodec}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .build();

    /**
     * 中文说明：执行 readActivation 操作；该方法是 {@code GatewayRuleJsonCodec} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the read activation operation; this method is the invocation entry point on {@code GatewayRuleJsonCodec} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleJsonCodec.readActivation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param json 参数 json；parameter json。
     * @return 返回 readActivation 的处理结果；returns the result of the operation.
     */
    public GatewayRuleActivation readActivation(String json) {
        return read(json.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                GatewayRuleActivation.class);
    }

    /**
     * 中文说明：执行 readSnapshot 操作；该方法是 {@code GatewayRuleJsonCodec} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the read snapshot operation; this method is the invocation entry point on {@code GatewayRuleJsonCodec} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleJsonCodec.readSnapshot(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param json 参数 json；parameter json。
     * @return 返回 readSnapshot 的处理结果；returns the result of the operation.
     */
    public GatewayRuleSnapshot readSnapshot(byte[] json) {
        return read(json, GatewayRuleSnapshot.class);
    }

    /**
     * 中文说明：执行 write 操作；该方法是 {@code GatewayRuleJsonCodec} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the write operation; this method is the invocation entry point on {@code GatewayRuleJsonCodec} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleJsonCodec.write(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 write 的处理结果；returns the result of the operation.
     */
    public byte[] write(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException(
                    "gateway rule cannot be serialized",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 verify 操作；该方法是 {@code GatewayRuleJsonCodec} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the verify operation; this method is the invocation entry point on {@code GatewayRuleJsonCodec} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleJsonCodec.verify(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param snapshot 参数 snapshot；parameter snapshot。
     */
    public void verify(GatewayRuleSnapshot snapshot) {
        if (!"v1".equals(snapshot.ruleSchemaVersion())) {
            throw new IllegalArgumentException(
                    "GATEWAY_RULE_SCHEMA_UNSUPPORTED"
            );
        }
        Object checksumContent = checksumContent(snapshot);
        Map<String, Object> material = Map.of(
                "content", checksumContent,
                "generatedAt", snapshot.generatedAt(),
                "releaseId", snapshot.releaseId(),
                "ruleContentSha256", snapshot.ruleContentSha256(),
                "ruleSchemaVersion", snapshot.ruleSchemaVersion()
        );
        if (!sha256(write(material)).equals(snapshot.artifactSha256())) {
            throw new IllegalArgumentException(
                    "GATEWAY_RULE_CHECKSUM_MISMATCH: artifact"
            );
        }
    }

    /**
     * 中文说明：执行 checksumContent 操作；该方法是 {@code GatewayRuleJsonCodec} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the checksum content operation; this method is the invocation entry point on {@code GatewayRuleJsonCodec} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleJsonCodec.checksumContent(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param snapshot 参数 snapshot；parameter snapshot。
     * @return 返回 checksumContent 的处理结果；returns the result of the operation.
     */
    private Object checksumContent(GatewayRuleSnapshot snapshot) {
        String expected = snapshot.ruleContentSha256();
        if (sha256(write(snapshot.content())).equals(expected)) {
            return snapshot.content();
        }
        GatewayRuleContent content = snapshot.content();
        Map<String, Object> legacy = legacyContent(content);
        if (content.mcp().equals(McpRuleContent.empty())
                && sha256(write(legacy)).equals(expected)) {
            return legacy;
        }
        throw new IllegalArgumentException(
                "GATEWAY_RULE_CHECKSUM_MISMATCH: content"
        );
    }

    /**
     * 中文说明：执行 legacyContent 操作；该方法是 {@code GatewayRuleJsonCodec} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the legacy content operation; this method is the invocation entry point on {@code GatewayRuleJsonCodec} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleJsonCodec.legacyContent(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     * @return 返回 legacyContent 的处理结果；returns the result of the operation.
     */
    private Map<String, Object> legacyContent(GatewayRuleContent content) {
        return Map.ofEntries(
                Map.entry("gatewayGroupId", content.gatewayGroupId()),
                Map.entry("gatewayGroupCode", content.gatewayGroupCode()),
                Map.entry("env", content.env()),
                Map.entry("namespace", content.namespace()),
                Map.entry("operations", content.operations()),
                Map.entry("routes", content.routes()),
                Map.entry("providerPolicies", content.providerPolicies()),
                Map.entry("trafficPolicies", content.trafficPolicies()),
                Map.entry("securityPolicies", content.securityPolicies()),
                Map.entry("corsPolicies", content.corsPolicies()),
                Map.entry("rpcDescriptors", content.rpcDescriptors())
        );
    }

    /**
     * 中文说明：执行 sha256 操作；该方法是 {@code GatewayRuleJsonCodec} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the sha256 operation; this method is the invocation entry point on {@code GatewayRuleJsonCodec} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleJsonCodec.sha256(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 sha256 的处理结果；returns the result of the operation.
     */
    public static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value)
            );
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /**
     * 中文说明：执行 read 操作；该方法是 {@code GatewayRuleJsonCodec} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the read operation; this method is the invocation entry point on {@code GatewayRuleJsonCodec} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleJsonCodec.read(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param json 参数 json；parameter json。
     * @param type 参数 type；parameter type。
     * @return 返回 read 的处理结果；returns the result of the operation.
     */
    private <T> T read(byte[] json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception failure) {
            throw new IllegalArgumentException(
                    "gateway rule JSON is invalid",
                    failure
            );
        }
    }
}
