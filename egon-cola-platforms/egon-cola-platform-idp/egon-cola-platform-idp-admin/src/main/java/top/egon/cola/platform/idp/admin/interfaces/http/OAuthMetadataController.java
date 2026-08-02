package top.egon.cola.platform.idp.admin.interfaces.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.platform.idp.admin.token.infrastructure.Rs256TokenService;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
public class OAuthMetadataController {

    private final String issuer;
    private final Rs256TokenService tokens;

    public OAuthMetadataController(
            @Value("${egon.idp.oauth.issuer}")
            String issuer,
            Rs256TokenService tokens
    ) {
        this.issuer = normalizedIssuer(issuer);
        this.tokens = Objects.requireNonNull(tokens, "tokens");
    }

    @GetMapping("/.well-known/oauth-authorization-server")
    public Map<String, Object> metadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("issuer", issuer);
        metadata.put("authorization_endpoint", issuer + "/oauth2/authorize");
        metadata.put("token_endpoint", issuer + "/oauth2/token");
        metadata.put("revocation_endpoint", issuer + "/oauth2/revoke");
        metadata.put("jwks_uri", issuer + "/oauth2/jwks");
        metadata.put("response_types_supported", List.of("code"));
        metadata.put("grant_types_supported", List.of(
                "authorization_code",
                "refresh_token"
        ));
        metadata.put("code_challenge_methods_supported", List.of("S256"));
        metadata.put("token_endpoint_auth_methods_supported", List.of("none"));
        return Map.copyOf(metadata);
    }

    @GetMapping("/oauth2/jwks")
    public Map<String, Object> jwks() {
        return tokens.jwkSet();
    }

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
