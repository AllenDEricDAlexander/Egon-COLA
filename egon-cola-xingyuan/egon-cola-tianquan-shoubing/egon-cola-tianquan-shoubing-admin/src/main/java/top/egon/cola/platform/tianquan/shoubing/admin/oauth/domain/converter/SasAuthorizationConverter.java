package top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthAuthorizationAttributesBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthAuthorizationBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthTokenBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthTokenMetadataBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.SignedTokenClaimsBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SasAuthorizationConverter extends BaseConverter<OAuthAuthorizationBO, OAuth2Authorization> {

    String ATTRIBUTES_ATTRIBUTE = "oauthAuthorizationAttributes";
    String PRINCIPAL_TYPE_ATTRIBUTE = "oauthPrincipalType";
    String AUTHORIZATION_STATUS_ATTRIBUTE = "oauthAuthorizationStatus";
    String ABSOLUTE_EXPIRES_AT_ATTRIBUTE = "oauthAbsoluteExpiresAt";
    String REVOKED_AT_ATTRIBUTE = "oauthRevokedAt";

    @Override
    default OAuth2Authorization toTarget(OAuthAuthorizationBO source) {
        throw new IllegalStateException("SAS_REGISTERED_CLIENT_AND_OBJECT_MAPPER_REQUIRED");
    }

    default OAuth2Authorization toTarget(OAuthAuthorizationBO source, RegisteredClient registeredClient) {
        return toTarget(source, registeredClient, null);
    }

    default OAuth2Authorization toTarget(
            OAuthAuthorizationBO source,
            RegisteredClient registeredClient,
            ObjectMapper objectMapper) {
        OAuthAuthorizationBO authorization = Objects.requireNonNull(source, "authorization");
        RegisteredClient client = Objects.requireNonNull(registeredClient, "registeredClient");
        Objects.requireNonNull(objectMapper, "objectMapper");
        requireAuthorization(authorization);
        if (!Long.toString(authorization.getClientPk()).equals(client.getId())) {
            throw new IllegalArgumentException("OAUTH_AUTHORIZATION_CLIENT_ID_MISMATCH");
        }

        OAuth2Authorization.Builder builder = OAuth2Authorization.withRegisteredClient(client)
                .id(Long.toString(authorization.getId()))
                .principalName(authorization.getPrincipalName())
                .authorizationGrantType(grantType(authorization.getGrantType()))
                .authorizedScopes(Set.copyOf(authorization.getAuthorizedScopes()))
                .attribute(ATTRIBUTES_ATTRIBUTE, authorization.getAttributes())
                .attribute(PRINCIPAL_TYPE_ATTRIBUTE, authorization.getPrincipalType())
                .attribute(AUTHORIZATION_STATUS_ATTRIBUTE, authorization.getStatus())
                .attribute(ABSOLUTE_EXPIRES_AT_ATTRIBUTE, authorization.getAbsoluteExpiresAt());
        if (authorization.getRevokedAt() != null) {
            builder.attribute(REVOKED_AT_ATTRIBUTE, authorization.getRevokedAt());
        }

        if (authorization.getTokens() != null) {
            if (authorization.getTokens().size() > 4) {
                throw new IllegalArgumentException("OAUTH_AUTHORIZATION_TOKEN_COUNT_INVALID");
            }
            authorization.getTokens().forEach(token -> addToken(
                    builder, token, authorization.getAuthorizedScopes(), objectMapper));
        }
        return builder.build();
    }

    @Override
    default OAuthAuthorizationBO toSource(OAuth2Authorization source) {
        throw new IllegalStateException("OBJECT_MAPPER_REQUIRED_FOR_SAS_AUTHORIZATION_CONVERSION");
    }

    default OAuthAuthorizationBO toSource(
            OAuth2Authorization source,
            OAuthAuthorizationBO persisted,
            ObjectMapper objectMapper) {
        OAuth2Authorization authorization = Objects.requireNonNull(source, "authorization");
        Objects.requireNonNull(objectMapper, "objectMapper");
        long clientPk = parsePositiveId(authorization.getRegisteredClientId(), "OAUTH_AUTHORIZATION_CLIENT_ID_INVALID");
        OAuthAuthorizationAttributesBO attributes = authorization.getAttribute(ATTRIBUTES_ATTRIBUTE);
        if (attributes == null && persisted != null) {
            attributes = persisted.getAttributes();
        }
        if (attributes == null || attributes.getSchemaVersion() != 1 || attributes.getSecuritySnapshot() == null) {
            throw new IllegalArgumentException("OAUTH_AUTHORIZATION_ATTRIBUTES_REQUIRED");
        }

        List<OAuthTokenBO> tokens = readTokens(authorization, objectMapper);
        Instant absoluteExpiresAt = authorization.getAttribute(ABSOLUTE_EXPIRES_AT_ATTRIBUTE);
        if (absoluteExpiresAt == null && persisted != null) {
            absoluteExpiresAt = persisted.getAbsoluteExpiresAt();
        }
        if (absoluteExpiresAt == null) {
            absoluteExpiresAt = tokens.stream().map(OAuthTokenBO::getExpiresAt)
                    .filter(Objects::nonNull).max(Instant::compareTo)
                    .orElseThrow(() -> new IllegalArgumentException("OAUTH_AUTHORIZATION_EXPIRY_REQUIRED"));
        }

        String principalType = authorization.getAttribute(PRINCIPAL_TYPE_ATTRIBUTE);
        if (principalType == null && persisted != null) {
            principalType = persisted.getPrincipalType();
        }
        if (principalType == null) {
            principalType = AuthorizationGrantType.CLIENT_CREDENTIALS.equals(authorization.getAuthorizationGrantType())
                    ? "SERVICE" : "USER";
        }
        String status = authorization.getAttribute(AUTHORIZATION_STATUS_ATTRIBUTE);
        if (status == null && persisted != null) {
            status = persisted.getStatus();
        }
        if (status == null) {
            status = "ACTIVE";
        }
        Instant revokedAt = authorization.getAttribute(REVOKED_AT_ATTRIBUTE);
        if (revokedAt == null && persisted != null) {
            revokedAt = persisted.getRevokedAt();
        }

        return OAuthAuthorizationBO.builder()
                .id(authorization.getId() == null ? null : parsePositiveId(
                        authorization.getId(), "OAUTH_AUTHORIZATION_ID_INVALID"))
                .version(persisted == null ? 0L : persisted.getVersion())
                .clientPk(clientPk)
                .principalName(authorization.getPrincipalName())
                .principalType(principalType)
                .grantType(authorization.getAuthorizationGrantType().getValue())
                .authorizedScopes(new ArrayList<>(authorization.getAuthorizedScopes()))
                .audiences(audiences(tokens))
                .status(status)
                .authTime(attributes.getAuthTime())
                .acr(attributes.getAcr())
                .attributes(attributes)
                .tokens(tokens)
                .absoluteExpiresAt(absoluteExpiresAt)
                .revokedAt(revokedAt)
                .build();
    }

    private static void addToken(
            OAuth2Authorization.Builder builder,
            OAuthTokenBO token,
            List<String> authorizedScopes,
            ObjectMapper objectMapper) {
        Objects.requireNonNull(token, "token");
        if (token.getTokenValue() == null || token.getTokenValue().isBlank()
                || token.getIssuedAt() == null || token.getExpiresAt() == null
                || token.getMetadata() == null || token.getMetadata().getSchemaVersion() != 1) {
            throw new IllegalArgumentException("OAUTH_AUTHORIZATION_TOKEN_PROJECTION_INVALID");
        }
        OAuth2Token sasToken = switch (token.getTokenType()) {
            case CODE -> new OAuth2AuthorizationCode(token.getTokenValue(), token.getIssuedAt(), token.getExpiresAt());
            case ACCESS -> new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, token.getTokenValue(),
                    token.getIssuedAt(), token.getExpiresAt(), Set.copyOf(authorizedScopes));
            case REFRESH -> new OAuth2RefreshToken(token.getTokenValue(), token.getIssuedAt(), token.getExpiresAt());
            case ID -> new OidcIdToken(token.getTokenValue(), token.getIssuedAt(), token.getExpiresAt(),
                    claimsMap(token.getMetadata().getClaims(), objectMapper));
        };
        builder.token(sasToken, metadata -> {
            metadata.put(OAuth2Authorization.Token.INVALIDATED_METADATA_NAME,
                    token.getMetadata().isInvalidated() || token.getMetadata().isCodeConsumed());
            Map<String, Object> claims = claimsMap(token.getMetadata().getClaims(), objectMapper);
            if (claims != null && !claims.isEmpty()) {
                metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, claims);
            }
        });
    }

    private static List<OAuthTokenBO> readTokens(OAuth2Authorization authorization, ObjectMapper objectMapper) {
        List<OAuthTokenBO> tokens = new ArrayList<>();
        OAuth2Authorization.Token<OAuth2AccessToken> access = authorization.getAccessToken();
        if (access != null) {
            tokens.add(toToken(access, OAuthTokenTypeEnum.ACCESS, objectMapper));
        }
        OAuth2Authorization.Token<OAuth2RefreshToken> refresh = authorization.getRefreshToken();
        if (refresh != null) {
            tokens.add(toToken(refresh, OAuthTokenTypeEnum.REFRESH, objectMapper));
        }
        OAuth2Authorization.Token<OAuth2AuthorizationCode> code = authorization.getToken(OAuth2AuthorizationCode.class);
        if (code != null) {
            tokens.add(toToken(code, OAuthTokenTypeEnum.CODE, objectMapper));
        }
        OAuth2Authorization.Token<OidcIdToken> id = authorization.getToken(OidcIdToken.class);
        if (id != null) {
            tokens.add(toToken(id, OAuthTokenTypeEnum.ID, objectMapper));
        }
        if (tokens.size() > 4) {
            throw new IllegalArgumentException("OAUTH_AUTHORIZATION_TOKEN_COUNT_INVALID");
        }
        return tokens;
    }

    private static OAuthTokenBO toToken(
            OAuth2Authorization.Token<? extends OAuth2Token> source,
            OAuthTokenTypeEnum tokenType,
            ObjectMapper objectMapper) {
        OAuth2Token token = source.getToken();
        Map<String, Object> claims = source.getClaims();
        SignedTokenClaimsBO typedClaims = claims == null || claims.isEmpty() || objectMapper == null
                ? null : objectMapper.convertValue(claims, SignedTokenClaimsBO.class);
        return OAuthTokenBO.builder()
                .tokenType(tokenType)
                .tokenValue(token.getTokenValue())
                .jti(typedClaims == null ? null : typedClaims.getJti())
                .issuedAt(token.getIssuedAt())
                .expiresAt(token.getExpiresAt())
                .metadata(OAuthTokenMetadataBO.builder()
                        .schemaVersion(1)
                        .claims(typedClaims)
                        .invalidated(source.isInvalidated())
                        .codeConsumed(tokenType == OAuthTokenTypeEnum.CODE && source.isInvalidated())
                        .build())
                .build();
    }

    private static Map<String, Object> claimsMap(SignedTokenClaimsBO claims, ObjectMapper objectMapper) {
        if (claims == null) {
            return Map.of();
        }
        return Objects.requireNonNull(objectMapper, "objectMapper")
                .convertValue(claims, new TypeReference<HashMap<String, Object>>() { });
    }

    private static List<String> audiences(List<OAuthTokenBO> tokens) {
        return tokens.stream()
                .map(OAuthTokenBO::getMetadata)
                .filter(Objects::nonNull)
                .map(OAuthTokenMetadataBO::getClaims)
                .filter(Objects::nonNull)
                .map(SignedTokenClaimsBO::getAudience)
                .filter(Objects::nonNull)
                .flatMap(Set::stream)
                .map(Object::toString)
                .distinct()
                .sorted()
                .toList();
    }

    private static AuthorizationGrantType grantType(String value) {
        if (AuthorizationGrantType.AUTHORIZATION_CODE.getValue().equals(value)) {
            return AuthorizationGrantType.AUTHORIZATION_CODE;
        }
        if (AuthorizationGrantType.CLIENT_CREDENTIALS.getValue().equals(value)) {
            return AuthorizationGrantType.CLIENT_CREDENTIALS;
        }
        throw new IllegalArgumentException("OAUTH_AUTHORIZATION_GRANT_TYPE_UNSUPPORTED");
    }

    private static void requireAuthorization(OAuthAuthorizationBO authorization) {
        if (authorization.getClientPk() == null || authorization.getClientPk() <= 0L
                || authorization.getId() == null || authorization.getId() <= 0L
                || authorization.getPrincipalName() == null || authorization.getPrincipalName().isBlank()
                || authorization.getPrincipalType() == null
                || !Set.of("USER", "SERVICE").contains(authorization.getPrincipalType())
                || authorization.getGrantType() == null || authorization.getAuthorizedScopes() == null
                || authorization.getAttributes() == null || authorization.getAttributes().getSchemaVersion() != 1
                || authorization.getAttributes().getSecuritySnapshot() == null
                || authorization.getAbsoluteExpiresAt() == null
                || authorization.getStatus() == null
                || !Set.of("ACTIVE", "REVOKED", "EXPIRED").contains(authorization.getStatus())) {
            throw new IllegalArgumentException("OAUTH_AUTHORIZATION_PROJECTION_INVALID");
        }
        grantType(authorization.getGrantType());
    }

    private static long parsePositiveId(String value, String errorCode) {
        try {
            long id = Long.parseLong(value);
            if (id <= 0L) {
                throw new NumberFormatException();
            }
            return id;
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(errorCode, failure);
        }
    }
}
