package top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthAuthorizationAttributesBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthPendingConsentBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthAuthorizationBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.po.OAuthAuthorizationPO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthPayloadCipherComponent;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OAuthAuthorizationConverter extends BaseConverter<OAuthAuthorizationPO, OAuthAuthorizationBO> {

    @Override
    default OAuthAuthorizationBO toTarget(OAuthAuthorizationPO source) {
        throw new IllegalStateException("TRUSTED_REALM_REQUIRED_FOR_AUTHORIZATION_CONVERSION");
    }

    @Override
    default OAuthAuthorizationPO toSource(OAuthAuthorizationBO target) {
        throw new IllegalStateException("TRUSTED_REALM_REQUIRED_FOR_AUTHORIZATION_CONVERSION");
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "clientPk", source = "clientPk")
    @Mapping(target = "principalName", source = "principalName")
    @Mapping(target = "principalType", source = "principalType")
    @Mapping(target = "grantType", source = "grantType")
    @Mapping(target = "authorizedScopes", source = "authorizedScopes")
    @Mapping(target = "audiences", source = "audiences")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "authTime", source = "authTime")
    @Mapping(target = "acr", source = "acr")
    @Mapping(target = "attributes", source = "attributesCiphertext",
            qualifiedByName = "authorizationAttributesFromStorage")
    @Mapping(target = "absoluteExpiresAt", source = "absoluteExpiresAt")
    @Mapping(target = "revokedAt", source = "revokedAt")
    OAuthAuthorizationBO toTarget(
            @Context TrustedRealmBO trustedRealm,
            @Context OAuthPayloadCipherComponent payloadCipher,
            @Context ObjectMapper objectMapper,
            @Context String authorizationId,
            OAuthAuthorizationPO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "clientPk", source = "clientPk")
    @Mapping(target = "principalName", source = "principalName")
    @Mapping(target = "principalType", source = "principalType")
    @Mapping(target = "grantType", source = "grantType")
    @Mapping(target = "authorizedScopes", source = "authorizedScopes")
    @Mapping(target = "audiences", source = "audiences")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "authTime", source = "authTime")
    @Mapping(target = "acr", source = "acr")
    @Mapping(target = "attributesCiphertext", source = "attributes",
            qualifiedByName = "authorizationAttributesToStorage")
    @Mapping(target = "absoluteExpiresAt", source = "absoluteExpiresAt")
    @Mapping(target = "revokedAt", source = "revokedAt")
    OAuthAuthorizationPO toSource(
            @Context TrustedRealmBO trustedRealm,
            @Context OAuthPayloadCipherComponent payloadCipher,
            @Context ObjectMapper objectMapper,
            @Context String authorizationId,
            OAuthAuthorizationBO target);

    @Named("authorizationAttributesFromStorage")
    default OAuthAuthorizationAttributesBO authorizationAttributesFromStorage(
            String ciphertext,
            @Context TrustedRealmBO trustedRealm,
            @Context OAuthPayloadCipherComponent payloadCipher,
            @Context ObjectMapper objectMapper,
            @Context String authorizationId) {
        String json = payloadCipher.decrypt(trustedRealm, "authorization-attributes", authorizationId, ciphertext);
        try {
            OAuthAuthorizationAttributesBO attributes =
                    storageMapper(objectMapper).readValue(json, OAuthAuthorizationAttributesBO.class);
            requireAttributesSchema(attributes);
            return attributes;
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("OAUTH_AUTHORIZATION_ATTRIBUTES_JSON_INVALID", failure);
        }
    }

    @Named("authorizationAttributesToStorage")
    default String authorizationAttributesToStorage(
            OAuthAuthorizationAttributesBO attributes,
            @Context TrustedRealmBO trustedRealm,
            @Context OAuthPayloadCipherComponent payloadCipher,
            @Context ObjectMapper objectMapper,
            @Context String authorizationId) {
        requireAttributesSchema(attributes);
        try {
            return payloadCipher.encrypt(trustedRealm, "authorization-attributes", authorizationId,
                    storageMapper(objectMapper).writeValueAsString(attributes));
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("OAUTH_AUTHORIZATION_ATTRIBUTES_JSON_ENCODE_FAILED", failure);
        }
    }
    private static void requireAttributesSchema(OAuthAuthorizationAttributesBO attributes) {
        if (attributes == null || attributes.getSchemaVersion() < 1 || attributes.getSchemaVersion() > 2
                || attributes.getSchemaVersion() == 1 && attributes.getPendingConsent() != null) {
            throw new IllegalArgumentException("OAUTH_AUTHORIZATION_ATTRIBUTES_SCHEMA_UNSUPPORTED");
        }
    }

    private static ObjectMapper storageMapper(ObjectMapper managedMapper) {
        return managedMapper.copy().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .addMixIn(OAuthAuthorizationAttributesBO.class, AuthorizationStorageMixin.class)
                .addMixIn(OAuthPendingConsentBO.class, PendingConsentStorageMixin.class);
    }

    abstract class AuthorizationStorageMixin {
        @JsonIgnore(false)
        @JsonProperty("pendingConsent")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        abstract OAuthPendingConsentBO getPendingConsent();
    }

    abstract class PendingConsentStorageMixin {
        @JsonIgnore(false)
        @JsonProperty("clientState")
        abstract String getClientState();
        @JsonIgnore(false)
        @JsonProperty("consentState")
        abstract String getConsentState();
        @JsonIgnore(false)
        @JsonProperty("sessionBindingDigest")
        abstract String getSessionBindingDigest();
    }

}
