package top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter;

import java.time.Duration;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.OAuthClientPurposeEnum;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SasRegisteredClientConverter extends BaseForwardConverter<OAuthClientBO, RegisteredClient> {

    @Override
    default RegisteredClient toTarget(OAuthClientBO source) {
        OAuthClientBO client = Objects.requireNonNull(source, "client");
        if (client.getId() == null || client.getId() <= 0L
                || client.getStatus() != OAuthClient.Status.ACTIVE
                || client.getClientType() == null || client.getPurpose() == null
                || client.getClientId() == null || client.getClientId().isBlank()) {
            throw new IllegalArgumentException("OAUTH_CLIENT_NOT_REGISTERABLE");
        }

        RegisteredClient.Builder builder = RegisteredClient.withId(Long.toString(client.getId()))
                .clientId(client.getClientId())
                .clientName(client.getClientName())
                .clientSecret(client.getSecretHash())
                .clientAuthenticationMethod(client.getClientType() == OAuthClient.ClientType.PUBLIC
                        ? ClientAuthenticationMethod.NONE
                        : ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(client.getPurpose() == OAuthClientPurposeEnum.USER)
                        .requireAuthorizationConsent(client.getPurpose() == OAuthClientPurposeEnum.USER)
                        .build())
                .tokenSettings(tokenSettings(client));

        if (client.getPurpose() == OAuthClientPurposeEnum.USER) {
            builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
            if (client.getClientType() == OAuthClient.ClientType.CONFIDENTIAL) {
                builder.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN);
            }
            if (client.getRedirectUris() == null || client.getRedirectUris().isEmpty()) {
                throw new IllegalArgumentException("OAUTH_USER_CLIENT_REDIRECT_URIS_REQUIRED");
            }
            client.getRedirectUris().forEach(builder::redirectUri);
        } else if (client.getPurpose() == OAuthClientPurposeEnum.SERVICE) {
            if (client.getClientType() != OAuthClient.ClientType.CONFIDENTIAL
                    || client.getSecretHash() == null || client.getSecretHash().isBlank()) {
                throw new IllegalArgumentException("OAUTH_SERVICE_CLIENT_SECRET_REQUIRED");
            }
            builder.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS);
        } else {
            throw new IllegalArgumentException("OAUTH_CLIENT_PURPOSE_UNSUPPORTED");
        }

        if (client.getAllowedScopes() == null) {
            throw new IllegalArgumentException("OAUTH_CLIENT_SCOPES_REQUIRED");
        }
        client.getAllowedScopes().forEach(builder::scope);
        return builder.build();
    }

    private static TokenSettings tokenSettings(OAuthClientBO client) {
        if (client.getAccessTtlSeconds() == null || client.getAccessTtlSeconds() < 300
                || client.getAccessTtlSeconds() > 600) {
            throw new IllegalArgumentException("OAUTH_CLIENT_ACCESS_TTL_INVALID");
        }
        TokenSettings.Builder builder = TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofSeconds(client.getAccessTtlSeconds()))
                .idTokenSignatureAlgorithm(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256)
                .reuseRefreshTokens(false);
        if (client.getPurpose() == OAuthClientPurposeEnum.USER
                && client.getClientType() == OAuthClient.ClientType.CONFIDENTIAL) {
            if (client.getRefreshTtlSeconds() == null || client.getRefreshTtlSeconds() <= 0) {
                throw new IllegalArgumentException("OAUTH_CLIENT_REFRESH_TTL_INVALID");
            }
            builder.refreshTokenTimeToLive(Duration.ofSeconds(client.getRefreshTtlSeconds()));
        }
        return builder.build();
    }
}
