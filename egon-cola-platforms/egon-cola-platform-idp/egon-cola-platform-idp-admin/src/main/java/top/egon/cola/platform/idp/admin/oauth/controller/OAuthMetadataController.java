package top.egon.cola.platform.idp.admin.oauth.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.idp.admin.token.service.impl.Rs256TokenService;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 发布 OAuth Authorization Server Metadata 与 IdP 公钥 JWK Set。
 *
 * <p>Publishes OAuth Authorization Server Metadata and the IdP public JWK Set.</p>
 */
@RestController
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "oauth-protocol",
        entityDomainName = "OAuth 协议域",
        code = "idp-oauth-metadata",
        name = "IdP OAuth 元数据接口组")
public class OAuthMetadataController {

    /** 规范化 IdP Issuer；normalized IdP issuer. */
    private final String issuer;

    /** RS256 Token 与公开 JWK 服务；RS256 token and public-JWK service. */
    private final Rs256TokenService tokens;

    /**
     * 创建 OAuth Metadata 控制器。
     *
     * <p>Creates the OAuth Metadata controller.</p>
     *
     * @param issuer IdP Issuer；IdP issuer
     * @param tokens RS256 Token 服务；RS256 token service
     */
    public OAuthMetadataController(
            @Value("${egon.idp.oauth.issuer}")
            String issuer,
            Rs256TokenService tokens
    ) {
        this.issuer = normalizedIssuer(issuer);
        this.tokens = Objects.requireNonNull(tokens, "tokens");
    }

    /**
     * 返回浏览器 USER 与机器 SERVICE 流程的 Authorization Server Metadata。
     *
     * <p>Returns Authorization Server Metadata for browser USER and machine SERVICE flows.</p>
     *
     * @return OAuth Metadata；OAuth metadata
     */
    @GetMapping("/.well-known/oauth-authorization-server")
    @GatewayOperation(name = "idp-oauth-metadata-v1",
            summary = "查询 OAuth Authorization Server 元数据",
            externalAccessible = true,
            tags = {"idp", "oauth"})
    public Map<String, Object> metadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("issuer", issuer);
        metadata.put("token_endpoint", issuer + "/oauth2/token");
        metadata.put("revocation_endpoint", issuer + "/oauth2/revoke");
        metadata.put("jwks_uri", issuer + "/oauth2/jwks");
        metadata.put("grant_types_supported", List.of(
                "refresh_token",
                "client_credentials"
        ));
        metadata.put("token_endpoint_auth_methods_supported", List.of(
                "private_key_jwt"
        ));
        return Map.copyOf(metadata);
    }

    /**
     * 返回不含私钥材料的 IdP JWK Set。
     *
     * <p>Returns the IdP JWK Set without private key material.</p>
     *
     * @return 公开 JWK Set；public JWK Set
     */
    @GetMapping("/oauth2/jwks")
    @GatewayOperation(name = "idp-oauth-jwks-v1",
            summary = "查询 IdP 公钥 JWK Set",
            externalAccessible = true,
            tags = {"idp", "oauth"})
    public Map<String, Object> jwks() {
        return tokens.jwkSet();
    }

    /**
     * 校验并移除 Issuer 尾部斜杠。
     *
     * <p>Validates the issuer and removes its trailing slash.</p>
     *
     * @param value 原始 Issuer；raw issuer
     * @return 规范化 Issuer；normalized issuer
     */
    private static String normalizedIssuer(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("issuer is required");
        }
        URI uri = URI.create(value.trim());
        if (!uri.isAbsolute()
                || uri.getHost() == null
                || uri.getQuery() != null
                || uri.getFragment() != null) {
            throw new IllegalArgumentException("issuer must be an absolute URI");
        }
        String normalized = value.trim();
        return normalized.endsWith("/")
                ? normalized.substring(0, normalized.length() - 1)
                : normalized;
    }
}
