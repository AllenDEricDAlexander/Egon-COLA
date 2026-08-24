package top.egon.cola.platform.idp.admin.oauth.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Typed OAuth Authorization Server metadata response. */
public record OAuthAuthorizationServerMetadataVO(
        String issuer,
        @JsonProperty("token_endpoint") String tokenEndpoint,
        @JsonProperty("revocation_endpoint") String revocationEndpoint,
        @JsonProperty("jwks_uri") String jwksUri,
        @JsonProperty("grant_types_supported") List<String> grantTypesSupported,
        @JsonProperty("token_endpoint_auth_methods_supported")
        List<String> tokenEndpointAuthMethodsSupported) {
}
