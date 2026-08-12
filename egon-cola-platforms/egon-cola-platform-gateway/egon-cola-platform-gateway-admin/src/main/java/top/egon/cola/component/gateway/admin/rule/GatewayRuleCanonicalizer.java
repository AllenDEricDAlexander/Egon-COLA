package top.egon.cola.component.gateway.admin.rule;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

/**
 * 中文说明：{@code GatewayRuleCanonicalizer} 是类型，位于当前 Gateway 模块的相关包中，负责网关规则Canonicalizer相关的职责与边界。
 * English summary: {@code GatewayRuleCanonicalizer} is a type in the current Gateway module; it owns the gateway rule canonicalizer-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayRuleCanonicalizer {

    /**
     * 中文说明：保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code GatewayRuleCanonicalizer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code GatewayRuleCanonicalizer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRuleCanonicalizer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleCanonicalizer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .build();

    /**
     * 中文说明：执行 canonicalBytes 操作；该方法是 {@code GatewayRuleCanonicalizer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the canonical bytes operation; this method is the invocation entry point on {@code GatewayRuleCanonicalizer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleCanonicalizer.canonicalBytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 canonicalBytes 的处理结果；returns the result of the operation.
     */
    public byte[] canonicalBytes(Object value) {
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
     * 中文说明：执行 snapshot 操作；该方法是 {@code GatewayRuleCanonicalizer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the snapshot operation; this method is the invocation entry point on {@code GatewayRuleCanonicalizer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleCanonicalizer.snapshot(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @param generatedAt 参数 generatedAt；parameter generated at。
     * @param content 参数 content；parameter content。
     * @return 返回 snapshot 的处理结果；returns the result of the operation.
     */
    public GatewayRuleSnapshot snapshot(
            String releaseId,
            Instant generatedAt,
            GatewayRuleContent content) {
        byte[] contentBytes = canonicalBytes(content);
        String contentSha = sha256(contentBytes);
        Map<String, Object> material = Map.of(
                "content", content,
                "generatedAt", generatedAt,
                "releaseId", releaseId,
                "ruleContentSha256", contentSha,
                "ruleSchemaVersion", "v1"
        );
        String artifactSha = sha256(canonicalBytes(material));
        return new GatewayRuleSnapshot(
                "v1",
                releaseId,
                generatedAt,
                contentSha,
                artifactSha,
                content
        );
    }

    /**
     * 中文说明：执行 verify 操作；该方法是 {@code GatewayRuleCanonicalizer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the verify operation; this method is the invocation entry point on {@code GatewayRuleCanonicalizer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleCanonicalizer.verify(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param snapshot 参数 snapshot；parameter snapshot。
     */
    public void verify(GatewayRuleSnapshot snapshot) {
        String contentSha = sha256(canonicalBytes(snapshot.content()));
        if (!contentSha.equals(snapshot.ruleContentSha256())) {
            throw new IllegalArgumentException(
                    "GATEWAY_RULE_CHECKSUM_MISMATCH: content"
            );
        }
        Map<String, Object> material = Map.of(
                "content", snapshot.content(),
                "generatedAt", snapshot.generatedAt(),
                "releaseId", snapshot.releaseId(),
                "ruleContentSha256", snapshot.ruleContentSha256(),
                "ruleSchemaVersion", snapshot.ruleSchemaVersion()
        );
        if (!sha256(canonicalBytes(material))
                .equals(snapshot.artifactSha256())) {
            throw new IllegalArgumentException(
                    "GATEWAY_RULE_CHECKSUM_MISMATCH: artifact"
            );
        }
    }

    /**
     * 中文说明：执行 json 操作；该方法是 {@code GatewayRuleCanonicalizer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the json operation; this method is the invocation entry point on {@code GatewayRuleCanonicalizer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleCanonicalizer.json(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 json 的处理结果；returns the result of the operation.
     */
    public String json(Object value) {
        return new String(canonicalBytes(value), StandardCharsets.UTF_8);
    }

    /**
     * 中文说明：执行 object映射器 操作；该方法是 {@code GatewayRuleCanonicalizer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the object mapper operation; this method is the invocation entry point on {@code GatewayRuleCanonicalizer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleCanonicalizer.objectMapper(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 object映射器 的处理结果；returns the result of the operation.
     */
    public ObjectMapper objectMapper() {
        return objectMapper.copy();
    }

    /**
     * 中文说明：执行 sha256 操作；该方法是 {@code GatewayRuleCanonicalizer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the sha256 operation; this method is the invocation entry point on {@code GatewayRuleCanonicalizer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleCanonicalizer.sha256(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
}
