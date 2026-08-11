package top.egon.cola.platform.idp.gateway.security;

import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import top.egon.cola.platform.idp.core.resource.ResourceServerStatus;
import top.egon.cola.platform.idp.starter.state.IdentityResourceServerState;
import top.egon.cola.platform.idp.starter.state.IdentityResourceServerStateReader;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 使用 Gateway 可信路由身份解析当前唯一的 IdP Resource Server。
 * Resolves the sole current IdP Resource Server from trusted Gateway route identity.
 */
public final class GatewayResourceServerResolver {

    /** 三元组索引读取端口；triple/URI index reader. */
    private final IndexReader indexes;

    /** Resource 状态读取端口；Resource-state reader. */
    private final IdentityResourceServerStateReader states;

    /** 三元组索引前缀；triple-index prefix. */
    private final String scopeKeyPrefix;

    /** Resource URI 索引前缀；Resource-URI-index prefix. */
    private final String uriKeyPrefix;

    /**
     * 创建 Redis 支持的 Resource 路由解析器。
     * Creates a Redis-backed route Resource resolver.
     *
     * @param redisson Redis 客户端；Redis client
     * @param states Resource 状态读取器；Resource-state reader
     * @param scopeKeyPrefix 三元组索引前缀；triple-index prefix
     * @param uriKeyPrefix URI 索引前缀；URI-index prefix
     */
    public GatewayResourceServerResolver(
            RedissonClient redisson,
            IdentityResourceServerStateReader states,
            String scopeKeyPrefix,
            String uriKeyPrefix
    ) {
        this(key -> redisson.<String>getBucket(
                key, StringCodec.INSTANCE).get(), states,
                scopeKeyPrefix, uriKeyPrefix);
    }

    /** 包内测试构造器；package-private test constructor. */
    GatewayResourceServerResolver(
            IndexReader indexes,
            IdentityResourceServerStateReader states,
            String scopeKeyPrefix,
            String uriKeyPrefix
    ) {
        this.indexes = Objects.requireNonNull(indexes, "indexes");
        this.states = Objects.requireNonNull(states, "states");
        this.scopeKeyPrefix = required(scopeKeyPrefix, "scopeKeyPrefix");
        this.uriKeyPrefix = required(uriKeyPrefix, "uriKeyPrefix");
    }

    /**
     * 按可信路由三元组或 MCP Resource URI 解析 ACTIVE Resource。
     * Resolves an ACTIVE Resource from a trusted route triple or MCP Resource URI.
     *
     * @param attributes Gateway 服务端生成的路由属性；server-derived Gateway route attributes
     * @return 精确 Resource 投影；exact Resource projection
     */
    public IdentityResourceServerState resolve(Map<String, String> attributes) {
        Objects.requireNonNull(attributes, "attributes");
        String resourceUri = optional(attributes.get("idp.resource-uri"));
        String resourceId;
        try {
            if (resourceUri != null) {
                resourceId = indexes.read(uriKeyPrefix + sha256(resourceUri));
            } else {
                String bizCode = required(
                        attributes.get("idp.biz-code"), "idp.biz-code");
                String appCode = required(
                        attributes.get("idp.app-code"), "idp.app-code");
                String environment = required(
                        attributes.get("idp.env"), "idp.env");
                resourceId = indexes.read(scopeKeyPrefix + sha256(
                        bizCode + ":" + appCode + ":" + environment));
            }
        } catch (ResourceResolutionException invalid) {
            throw invalid;
        } catch (RuntimeException unavailable) {
            throw new ResourceResolutionException(
                    "IDP_RESOURCE_INDEX_UNAVAILABLE", unavailable);
        }
        if (resourceId == null || resourceId.isBlank()) {
            throw new ResourceResolutionException("IDP_RESOURCE_NOT_FOUND");
        }
        IdentityResourceServerState state;
        try {
            state = states.read(resourceId.trim()).orElseThrow(
                    () -> new ResourceResolutionException("IDP_RESOURCE_NOT_FOUND"));
        } catch (ResourceResolutionException invalid) {
            throw invalid;
        } catch (RuntimeException unavailable) {
            throw new ResourceResolutionException(
                    "IDP_RESOURCE_STATE_UNAVAILABLE", unavailable);
        }
        if (state.status() != ResourceServerStatus.ACTIVE) {
            throw new ResourceResolutionException("IDP_RESOURCE_NOT_ACTIVE");
        }
        if (resourceUri != null
                && !state.resourceUri().toString().equals(resourceUri)) {
            throw new ResourceResolutionException("IDP_RESOURCE_URI_MISMATCH");
        }
        if (resourceUri == null
                && (!state.bizCode().equals(attributes.get("idp.biz-code").trim())
                || !state.appCode().equals(attributes.get("idp.app-code").trim())
                || !state.environment().equals(attributes.get("idp.env").trim()))) {
            throw new ResourceResolutionException("IDP_RESOURCE_ROUTE_MISMATCH");
        }
        return state;
    }

    /** 计算索引使用的 SHA-256 十六进制摘要；Computes the index SHA-256 hex digest. */
    static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(required(value, "value").getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 is unavailable", unavailable);
        }
    }

    /** 校验必填文本；Validates required text. */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResourceResolutionException(field + " is required");
        }
        return value.trim();
    }

    /** 规范化可选文本；Normalizes optional text. */
    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Resource 索引读取端口；Resource-index reader. */
    @FunctionalInterface
    interface IndexReader {
        /** 读取索引值；Reads an index value. */
        String read(String key);
    }

    /** Resource 路由解析失败；Resource route-resolution failure. */
    public static final class ResourceResolutionException extends RuntimeException {
        /** 使用原因码创建异常；Creates an exception with a reason code. */
        public ResourceResolutionException(String message) {
            super(message);
        }

        /** 使用原因码和底层异常创建异常；Creates an exception with a reason and cause. */
        public ResourceResolutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
