package top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.OAuthAuthorizationServerMetadataVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.OAuthJwkSetVO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.Rs256TokenService;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 发布 OAuth Authorization Server Metadata 与 Tianquan-Shoubing 公钥 JWK Set。
 *
 * <p>Publishes OAuth Authorization Server Metadata and the Tianquan-Shoubing public JWK Set.</p>
 */
@RestController
@Tag(name = "tianquan-shoubing-oauth-metadata", description = "Tianquan-Shoubing OAuth 元数据接口组")
@EgonApiCatalog(
        businessDomainCode = "xingyuan",
        businessDomainName = "平台治理域",
        entityDomainCode = "oauth-protocol",
        entityDomainName = "OAuth 协议域",
        interfaceGroupCode = "tianquan-shoubing-oauth"
)

public class OAuthMetadataController {

    /** 规范化 Tianquan-Shoubing Issuer；normalized Tianquan-Shoubing issuer. */
    private final String issuer;

    /** RS256 Token 与公开 JWK 服务；RS256 token and public-JWK service. */
    private final Rs256TokenService tokens;

    /**
     * 创建 OAuth Metadata 控制器。
     *
     * <p>Creates the OAuth Metadata controller.</p>
     *
     * @param issuer Tianquan-Shoubing Issuer；Tianquan-Shoubing issuer
     * @param tokens RS256 Token 服务；RS256 token service
     */
    public OAuthMetadataController(
            @Value("${egon.tianquan-shoubing.oauth.issuer}")
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
    @Operation(
            operationId = "tianquan-shoubing-oauth-metadata-v1",
            summary = "查询 OAuth Authorization Server 元数据",
            tags = {"tianquan-shoubing", "oauth"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public OAuthAuthorizationServerMetadataVO metadata() {
        return new OAuthAuthorizationServerMetadataVO(
                issuer,
                issuer + "/oauth2/token",
                issuer + "/oauth2/revoke",
                issuer + "/oauth2/jwks",
                List.of("refresh_token", "client_credentials"),
                List.of("client_secret_basic")
        );
    }

    /**
     * 返回不含私钥材料的 Tianquan-Shoubing JWK Set。
     *
     * <p>Returns the Tianquan-Shoubing JWK Set without private key material.</p>
     *
     * @return 公开 JWK Set；public JWK Set
     */
    @GetMapping("/oauth2/jwks")
    @Operation(
            operationId = "tianquan-shoubing-oauth-jwks-v1",
            summary = "查询 Tianquan-Shoubing 公钥 JWK Set",
            tags = {"tianquan-shoubing", "oauth"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public OAuthJwkSetVO jwks() {
        Object rawKeys = tokens.jwkSet().get("keys");
        if (!(rawKeys instanceof List<?> keys)
                || keys.size() != 1
                || !(keys.getFirst() instanceof Map<?, ?> key)) {
            throw new IllegalStateException("Tianquan-Shoubing public JWK Set is invalid");
        }
        return new OAuthJwkSetVO(List.of(new OAuthJwkSetVO.Jwk(
                text(key, "kty"),
                text(key, "e"),
                text(key, "kid"),
                text(key, "n"),
                text(key, "alg"),
                optionalText(key, "use")
        )));
    }

    private static String text(Map<?, ?> values, String key) {
        String value = optionalText(values, key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Tianquan-Shoubing public JWK field is missing: " + key);
        }
        return value;
    }

    private static String optionalText(Map<?, ?> values, String key) {
        Object value = values.get(key);
        return value == null ? null : value.toString();
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
