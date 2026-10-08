package top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthTokenBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthTokenMetadataBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.po.OAuthTokenPO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthPayloadCipherComponent;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OAuthTokenConverter extends BaseConverter<OAuthTokenPO, OAuthTokenBO> {

    @Override
    default OAuthTokenBO toTarget(OAuthTokenPO source) {
        throw new IllegalStateException("TRUSTED_REALM_REQUIRED_FOR_TOKEN_CONVERSION");
    }

    @Override
    default OAuthTokenPO toSource(OAuthTokenBO target) {
        throw new IllegalStateException("TRUSTED_REALM_REQUIRED_FOR_TOKEN_CONVERSION");
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "authorizationPk", source = "authorizationPk")
    @Mapping(target = "tokenType", source = "tokenType")
    @Mapping(target = "tokenDigest", source = "tokenDigest")
    @Mapping(target = "tokenValue", source = "tokenCiphertext", qualifiedByName = "tokenValueFromStorage")
    @Mapping(target = "jti", source = "jti")
    @Mapping(target = "issuedAt", source = "issuedAt")
    @Mapping(target = "expiresAt", source = "expiresAt")
    @Mapping(target = "consumedAt", source = "consumedAt")
    @Mapping(target = "revokedAt", source = "revokedAt")
    @Mapping(target = "metadata", source = "metadataCiphertext", qualifiedByName = "metadataFromStorage")
    OAuthTokenBO toTarget(
            @Context TrustedRealmBO trustedRealm,
            @Context OAuthPayloadCipherComponent payloadCipher,
            @Context ObjectMapper objectMapper,
            @Context String tokenDigest,
            OAuthTokenPO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "authorizationPk", source = "authorizationPk")
    @Mapping(target = "tokenType", source = "tokenType")
    @Mapping(target = "tokenDigest", source = "tokenDigest")
    @Mapping(target = "tokenCiphertext", source = "tokenValue", qualifiedByName = "tokenValueToStorage")
    @Mapping(target = "jti", source = "jti")
    @Mapping(target = "issuedAt", source = "issuedAt")
    @Mapping(target = "expiresAt", source = "expiresAt")
    @Mapping(target = "consumedAt", source = "consumedAt")
    @Mapping(target = "revokedAt", source = "revokedAt")
    @Mapping(target = "metadataCiphertext", source = "metadata", qualifiedByName = "metadataToStorage")
    OAuthTokenPO toSource(
            @Context TrustedRealmBO trustedRealm,
            @Context OAuthPayloadCipherComponent payloadCipher,
            @Context ObjectMapper objectMapper,
            @Context String tokenDigest,
            OAuthTokenBO target);

    @Named("tokenValueFromStorage")
    default String tokenValueFromStorage(
            String ciphertext,
            @Context TrustedRealmBO trustedRealm,
            @Context OAuthPayloadCipherComponent payloadCipher,
            @Context String tokenDigest) {
        return ciphertext == null ? null : payloadCipher.decrypt(trustedRealm, "token-value", tokenDigest, ciphertext);
    }

    @Named("tokenValueToStorage")
    default String tokenValueToStorage(
            String tokenValue,
            @Context TrustedRealmBO trustedRealm,
            @Context OAuthPayloadCipherComponent payloadCipher,
            @Context String tokenDigest) {
        return tokenValue == null ? null : payloadCipher.encrypt(trustedRealm, "token-value", tokenDigest, tokenValue);
    }

    @Named("metadataFromStorage")
    default OAuthTokenMetadataBO metadataFromStorage(
            String ciphertext,
            @Context TrustedRealmBO trustedRealm,
            @Context OAuthPayloadCipherComponent payloadCipher,
            @Context ObjectMapper objectMapper,
            @Context String tokenDigest) {
        String json = payloadCipher.decrypt(trustedRealm, "token-metadata", tokenDigest, ciphertext);
        try {
            OAuthTokenMetadataBO metadata = objectMapper.readValue(json, OAuthTokenMetadataBO.class);
            if (metadata.getSchemaVersion() != 1) {
                throw new IllegalArgumentException("OAUTH_TOKEN_METADATA_SCHEMA_UNSUPPORTED");
            }
            return metadata;
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("OAUTH_TOKEN_METADATA_JSON_INVALID", failure);
        }
    }

    @Named("metadataToStorage")
    default String metadataToStorage(
            OAuthTokenMetadataBO metadata,
            @Context TrustedRealmBO trustedRealm,
            @Context OAuthPayloadCipherComponent payloadCipher,
            @Context ObjectMapper objectMapper,
            @Context String tokenDigest) {
        if (metadata == null || metadata.getSchemaVersion() != 1) {
            throw new IllegalArgumentException("OAUTH_TOKEN_METADATA_SCHEMA_UNSUPPORTED");
        }
        try {
            return payloadCipher.encrypt(trustedRealm, "token-metadata", tokenDigest,
                    objectMapper.writeValueAsString(metadata));
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("OAUTH_TOKEN_METADATA_JSON_ENCODE_FAILED", failure);
        }
    }
}
