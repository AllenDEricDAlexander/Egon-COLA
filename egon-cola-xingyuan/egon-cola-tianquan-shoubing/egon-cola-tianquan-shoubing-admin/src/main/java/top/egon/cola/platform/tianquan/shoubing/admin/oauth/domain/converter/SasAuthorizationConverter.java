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
import java.net.URI;
import java.security.Principal;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
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
import top.egon.cola.component.common.crypto.digest.Digests;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthAuthorizationAttributesBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthPendingConsentBO;
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
    String REQUESTED_AUDIENCES_ATTRIBUTE = "oauthRequestedAudiences";

    String PENDING_ISSUED_AT_ATTRIBUTE = "egon.pendingIssuedAt";
    String PENDING_EXPIRES_AT_ATTRIBUTE = "egon.pendingExpiresAt";

    OAuthAuthorizationAttributesBO copyAttributes(OAuthAuthorizationAttributesBO source);

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
                .attribute(ABSOLUTE_EXPIRES_AT_ATTRIBUTE, authorization.getAbsoluteExpiresAt())
                .attribute(REQUESTED_AUDIENCES_ATTRIBUTE, List.copyOf(authorization.getAudiences()));
        if (authorization.getRevokedAt() != null) {
            builder.attribute(REVOKED_AT_ATTRIBUTE, authorization.getRevokedAt());
        }

        if (authorization.getTokens() != null) {
            if (authorization.getTokens().size() > 5) {
                throw new IllegalArgumentException("OAUTH_AUTHORIZATION_TOKEN_COUNT_INVALID");
            }
            authorization.getTokens().forEach(token -> addToken(
                    builder, token, authorization.getAuthorizedScopes(), objectMapper));
        }
        return builder.build();
    }

    default OAuth2Authorization toTarget(
            OAuthAuthorizationBO source,
            RegisteredClient registeredClient,
            ObjectMapper objectMapper,
            URI issuer) {
        OAuth2Authorization authorization = toTarget(source, registeredClient, objectMapper);
        if (!AuthorizationGrantType.AUTHORIZATION_CODE.equals(authorization.getAuthorizationGrantType())) {
            return authorization;
        }
        OAuthAuthorizationAttributesBO attributes = Objects.requireNonNull(
                source.getAttributes(), "authorization.attributes");
        Map<String, Object> parameters = new HashMap<>();
        if (attributes.getNonce() != null) {
            parameters.put("nonce", attributes.getNonce());
        }
        if (attributes.getCodeChallenge() != null) {
            parameters.put("code_challenge", attributes.getCodeChallenge());
        }
        if (attributes.getCodeChallengeMethod() != null) {
            parameters.put("code_challenge_method", attributes.getCodeChallengeMethod());
        }
        List<String> audiences = source.getAudiences();
        if (audiences != null && audiences.size() == 1) {
            parameters.put("resource", audiences.getFirst());
        }
        OAuth2AuthorizationRequest request = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri(Objects.requireNonNull(issuer, "issuer").toASCIIString() + "/oauth2/authorize")
                .clientId(registeredClient.getClientId())
                .redirectUri(attributes.getRedirectUri() == null ? null : attributes.getRedirectUri().toASCIIString())
                .scopes(Set.copyOf(attributes.getPendingConsent() == null
                        ? source.getAuthorizedScopes() : attributes.getPendingConsent().getRequestedScopes()))
                .state(attributes.getPendingConsent() == null ? null : attributes.getPendingConsent().getClientState())
                .additionalParameters(parameters)
                .build();
        UsernamePasswordAuthenticationToken principal = UsernamePasswordAuthenticationToken.authenticated(
                source.getPrincipalName(), "", AuthorityUtils.createAuthorityList("ROLE_USER"));
        Map<String, Object> details = new HashMap<>();
        if (attributes.getAuthTime() != null) {
            details.put("authTime", attributes.getAuthTime());
        }
        if (attributes.getAcr() != null) {
            details.put("acr", attributes.getAcr());
        }
        principal.setDetails(details);
        return OAuth2Authorization.from(authorization)
                .attribute(OAuth2AuthorizationRequest.class.getName(), request)
                .attribute(Principal.class.getName(), principal)
                .build();
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
        OAuth2AuthorizationRequest authorizationRequest = authorization.getAttribute(
                OAuth2AuthorizationRequest.class.getName());
        if (attributes == null) {
            attributes = attributesFromRequest(authorizationRequest, authorization);
        }
        if (attributes == null || attributes.getSchemaVersion() < 1 || attributes.getSchemaVersion() > 2) {
            throw new IllegalArgumentException("OAUTH_AUTHORIZATION_ATTRIBUTES_REQUIRED");
        }
        attributes = copyAttributes(attributes);
        if (authorization.getAttribute("state") == null && authorization.getToken(OAuth2AuthorizationCode.class) != null) {
            attributes.setPendingConsent(null);
        }

        List<OAuthTokenBO> tokens = readTokens(authorization, objectMapper, attributes);
        Long authorizationId = authorization.getId() == null || isSasGeneratedUuid(authorization.getId())
                ? authorizationIdFromTokens(tokens)
                : Long.valueOf(parsePositiveId(authorization.getId(), "OAUTH_AUTHORIZATION_ID_INVALID"));
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

        List<String> requestedAudiences = authorization.getAttribute(REQUESTED_AUDIENCES_ATTRIBUTE);
        if (requestedAudiences == null && authorizationRequest != null) {
            requestedAudiences = requestedAudiences(authorizationRequest);
        }
        List<String> audiences = audiences(tokens);
        if (audiences.isEmpty() && persisted != null && persisted.getAudiences() != null) {
            audiences = persisted.getAudiences();
        }
        if (audiences.isEmpty() && requestedAudiences != null) {
            audiences = requestedAudiences;
        }

        return OAuthAuthorizationBO.builder()
                .id(authorizationId)
                .version(persisted == null ? 0L : persisted.getVersion())
                .clientPk(clientPk)
                .principalName(authorization.getPrincipalName())
                .principalType(principalType)
                .grantType(authorization.getAuthorizationGrantType().getValue())
                .authorizedScopes(new ArrayList<>(authorization.getAuthorizedScopes()))
                .audiences(audiences)
                .status(status)
                .authTime(attributes.getAuthTime())
                .acr(attributes.getAcr())
                .attributes(attributes)
                .tokens(tokens)
                .absoluteExpiresAt(absoluteExpiresAt)
                .revokedAt(revokedAt)
                .build();
    }

    private static OAuthAuthorizationAttributesBO attributesFromRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            OAuth2Authorization authorization) {
        if (authorizationRequest == null) {
            if (!AuthorizationGrantType.CLIENT_CREDENTIALS.equals(authorization.getAuthorizationGrantType())) {
                return null;
            }
            return OAuthAuthorizationAttributesBO.builder().schemaVersion(1).build();
        }
        Map<String, Object> parameters = authorizationRequest.getAdditionalParameters();
        String state = authorizationRequest.getState();
        Authentication principal = authorization.getAttribute(Principal.class.getName());
        Instant authTime = null;
        String acr = null;
        if (principal != null && principal.getDetails() instanceof Map<?, ?> details) {
            Object authTimeValue = details.get("authTime");
            authTime = authTimeValue instanceof Instant instant ? instant : null;
            Object acrValue = details.get("acr");
            acr = acrValue instanceof String value ? value : null;
        }
        OAuthPendingConsentBO pending = null;
        String consentState = authorization.getAttribute("state");
        if (consentState != null) {
            Object requestContext = RequestContextHolder.getRequestAttributes();
            if (!(requestContext instanceof ServletRequestAttributes servlet)
                    || servlet.getRequest().getSession(false) == null) {
                throw new IllegalArgumentException("OAUTH_CONSENT_SESSION_REQUIRED");
            }
            Instant pendingIssuedAt = authorization.getAttribute(PENDING_ISSUED_AT_ATTRIBUTE);
            Instant pendingExpiresAt = authorization.getAttribute(PENDING_EXPIRES_AT_ATTRIBUTE);
            if (pendingIssuedAt == null || pendingExpiresAt == null) {
                throw new IllegalArgumentException("OAUTH_CONSENT_LIFETIME_REQUIRED");
            }
            pending = OAuthPendingConsentBO.builder().clientState(state).consentState(consentState)
                    .requestedScopes(new ArrayList<>(authorizationRequest.getScopes()))
                    .sessionBindingDigest(Digests.sha256Hex(servlet.getRequest().getSession(false).getId()))
                    .issuedAt(pendingIssuedAt).expiresAt(pendingExpiresAt).build();
        }
        return OAuthAuthorizationAttributesBO.builder()
                .schemaVersion(2)
                .pendingConsent(pending)
                .nonce(text(parameters.get("nonce")))
                .codeChallenge(text(parameters.get("code_challenge")))
                .codeChallengeMethod(text(parameters.get("code_challenge_method")))
                .redirectUri(authorizationRequest.getRedirectUri() == null
                        ? null : java.net.URI.create(authorizationRequest.getRedirectUri()))
                .stateDigest(state == null ? null : Digests.sha256Hex(state))
                .authTime(authTime)
                .acr(acr)
                .build();
    }

    private static List<String> requestedAudiences(OAuth2AuthorizationRequest request) {
        Object resource = request.getAdditionalParameters().get("resource");
        if (resource == null) {
            return List.of();
        }
        if (resource instanceof String value) {
            return List.of(value);
        }
        if (resource instanceof java.net.URI value) {
            return List.of(value.toASCIIString());
        }
        throw new IllegalArgumentException("OAUTH_AUTHORIZATION_RESOURCE_INVALID");
    }

    private static String text(Object value) {
        return value instanceof String text ? text : null;
    }

    private static Long authorizationIdFromTokens(List<OAuthTokenBO> tokens) {
        return tokens.stream()
                .filter(token -> token.getTokenType() == OAuthTokenTypeEnum.ACCESS)
                .map(OAuthTokenBO::getMetadata)
                .filter(Objects::nonNull)
                .map(OAuthTokenMetadataBO::getClaims)
                .filter(Objects::nonNull)
                .map(SignedTokenClaimsBO::getAuthorizationId)
                .filter(Objects::nonNull)
                .map(value -> parsePositiveId(value, "OAUTH_AUTHORIZATION_ID_INVALID"))
                .findFirst()
                .orElse(null);
    }

    private static boolean isSasGeneratedUuid(String value) {
        return value != null && value.matches(
                "(?i)[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
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
        if (token.getTokenType() == OAuthTokenTypeEnum.CONSENT_STATE) {
            if (token.getMetadata() != null && !token.getMetadata().isInvalidated()
                    && token.getRevokedAt() == null && token.getConsumedAt() == null) {
                builder.attribute("state", token.getTokenValue());
            }
            return;
        }
        OAuth2Token sasToken = switch (token.getTokenType()) {
            case CODE -> new OAuth2AuthorizationCode(token.getTokenValue(), token.getIssuedAt(), token.getExpiresAt());
            case ACCESS -> new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, token.getTokenValue(),
                    token.getIssuedAt(), token.getExpiresAt(), token.getMetadata().getClaims() == null
                            ? Set.copyOf(authorizedScopes) : Set.copyOf(token.getMetadata().getClaims().getScope()));
            case REFRESH -> new OAuth2RefreshToken(token.getTokenValue(), token.getIssuedAt(), token.getExpiresAt());
            case CONSENT_STATE -> throw new IllegalArgumentException("OAUTH_CONSENT_STATE_IS_NOT_ISSUED_TOKEN");
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

    private static List<OAuthTokenBO> readTokens(
            OAuth2Authorization authorization, ObjectMapper objectMapper, OAuthAuthorizationAttributesBO attributes) {
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
        String consentState = authorization.getAttribute("state");
        if (consentState != null) {
            OAuthPendingConsentBO pending = attributes.getPendingConsent();
            if (pending == null || !consentState.equals(pending.getConsentState())) {
                throw new IllegalArgumentException("OAUTH_CONSENT_STATE_PAYLOAD_MISMATCH");
            }
            tokens.add(OAuthTokenBO.builder().tokenType(OAuthTokenTypeEnum.CONSENT_STATE)
                    .tokenValue(consentState).issuedAt(pending.getIssuedAt()).expiresAt(pending.getExpiresAt())
                    .metadata(OAuthTokenMetadataBO.builder().schemaVersion(1).build()).build());
        }
        if (tokens.size() > 5) {
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
        Map<String, Object> normalizedClaims = claims == null ? null : new HashMap<>(claims);
        if (normalizedClaims != null && normalizedClaims.get("scope") instanceof String scope) {
            List<String> values = List.of(scope.split(" ", -1));
            java.util.Set<String> scopes = new java.util.LinkedHashSet<>(values);
            if (values.isEmpty() || values.size() > 64 || scopes.size() != values.size()
                    || values.stream().anyMatch(value -> value.isEmpty() || value.length() > 128)) {
                throw new IllegalArgumentException("OAUTH_TOKEN_SCOPE_INVALID");
            }
            normalizedClaims.put("scope", scopes);
        }
        SignedTokenClaimsBO typedClaims = normalizedClaims == null || normalizedClaims.isEmpty() || objectMapper == null
                ? null : objectMapper.convertValue(normalizedClaims, SignedTokenClaimsBO.class);
        if (typedClaims != null) {
            typedClaims.setTokenUse(tokenType);
        }
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
        Map<String, Object> value = Objects.requireNonNull(objectMapper, "objectMapper")
                .convertValue(claims, new TypeReference<HashMap<String, Object>>() { });
        value.remove("_token_use");
        value.entrySet().removeIf(entry -> entry.getValue() == null);
        if (claims.getScope() != null) {
            value.put("scope", String.join(" ", claims.getScope()));
        }
        value.put("iat", claims.getIssuedAt());
        value.put("exp", claims.getExpiresAt());
        if (claims.getNotBefore() != null) {
            value.put("nbf", claims.getNotBefore());
        }
        if (claims.getAuthTime() != null) {
            value.put("auth_time", claims.getAuthTime());
        }
        return value;
    }

    private static List<String> audiences(List<OAuthTokenBO> tokens) {
        return tokens.stream()
                .filter(token -> token.getTokenType() == OAuthTokenTypeEnum.ACCESS)
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
                || authorization.getAttributes() == null || (authorization.getAttributes().getSchemaVersion() < 1 || authorization.getAttributes().getSchemaVersion() > 2)
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
