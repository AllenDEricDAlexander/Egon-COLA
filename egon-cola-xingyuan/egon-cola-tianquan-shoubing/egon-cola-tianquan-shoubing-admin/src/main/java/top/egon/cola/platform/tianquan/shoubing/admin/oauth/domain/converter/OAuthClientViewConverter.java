package top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import org.mapstruct.BeanMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.OAuthClientVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.CreatedOAuthClientVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.RotatedClientSecretVO;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OAuthClientViewConverter extends BaseForwardConverter<OAuthClientBO, OAuthClientVO> {

    @Override
    default OAuthClientVO toTarget(OAuthClientBO source) {
        throw new IllegalStateException("OAUTH_CLIENT_VIEW_TRUSTED_ISSUER_REQUIRED");
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "clientId", source = "clientId")
    @Mapping(target = "appId", source = "appId")
    @Mapping(target = "clientName", source = "clientName")
    @Mapping(target = "clientType", expression = "java(source.getClientType().name())")
    @Mapping(target = "purpose", expression = "java(source.getPurpose().name())")
    @Mapping(target = "status", expression = "java(source.getStatus().name())")
    @Mapping(target = "pkceRequired", expression = "java(source.getPurpose() == top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.OAuthClientPurposeEnum.USER)")
    @Mapping(target = "accessTokenTtlSeconds", source = "accessTtlSeconds")
    @Mapping(target = "refreshTokenTtlSeconds", source = "refreshTtlSeconds")
    @Mapping(target = "redirectUris", source = "redirectUris")
    @Mapping(target = "resourceUris", source = "resourceUris")
    @Mapping(target = "allowedScopes", source = "allowedScopes")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "secretHint", source = "secretHint")
    @Mapping(target = "secretStatus", expression = "java(source.getSecretHash() == null ? null : \"ACTIVE\")")
    @Mapping(target = "issuer", expression = "java(issuer.toASCIIString())")
    OAuthClientVO toView(OAuthClientBO source, @Context URI issuer);

    default OAuthClientVO toTarget(OAuthClientBO source, URI issuer) {
        return toView(Objects.requireNonNull(source, "source"), Objects.requireNonNull(issuer, "issuer"));
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "clientId", source = "source.clientId")
    @Mapping(target = "appId", source = "source.appId")
    @Mapping(target = "clientName", source = "source.clientName")
    @Mapping(target = "clientType", expression = "java(source.getClientType().name())")
    @Mapping(target = "purpose", expression = "java(source.getPurpose().name())")
    @Mapping(target = "status", expression = "java(source.getStatus().name())")
    @Mapping(target = "clientSecret", expression = "java(secret)")
    @Mapping(target = "secretHint", source = "source.secretHint")
    @Mapping(target = "version", source = "source.version")
    @Mapping(target = "createdAt", source = "source.createdAt")
    @Mapping(target = "issuer", expression = "java(issuer.toASCIIString())")
    CreatedOAuthClientVO toCreatedView(OAuthClientBO source, @Context URI issuer, @Context String secret);

    default CreatedOAuthClientVO toCreatedTarget(OAuthClientBO source, URI issuer, String secret) {
        return toCreatedView(Objects.requireNonNull(source, "source"),
                Objects.requireNonNull(issuer, "issuer"), secret);
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "clientId", source = "source.clientId")
    @Mapping(target = "appId", source = "source.appId")
    @Mapping(target = "clientSecret", expression = "java(secret)")
    @Mapping(target = "secretHint", source = "source.secretHint")
    @Mapping(target = "version", source = "source.version")
    @Mapping(target = "rotatedAt", expression = "java(rotatedAt)")
    @Mapping(target = "issuer", expression = "java(issuer.toASCIIString())")
    RotatedClientSecretVO toRotatedView(
            OAuthClientBO source, @Context URI issuer, @Context String secret, @Context Instant rotatedAt);

    default RotatedClientSecretVO toRotatedTarget(
            OAuthClientBO source, URI issuer, String secret, Instant rotatedAt) {
        return toRotatedView(Objects.requireNonNull(source, "source"),
                Objects.requireNonNull(issuer, "issuer"), secret, Objects.requireNonNull(rotatedAt, "rotatedAt"));
    }
}
