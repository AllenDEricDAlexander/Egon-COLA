package top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter;

import java.net.URI;
import java.util.Objects;
import org.mapstruct.BeanMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthSigningKeyBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.vo.SigningKeyVO;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SigningKeyViewConverter extends BaseForwardConverter<OAuthSigningKeyBO, SigningKeyVO> {

    @Override
    default SigningKeyVO toTarget(OAuthSigningKeyBO source) {
        throw new IllegalStateException("OAUTH_SIGNING_KEY_VIEW_TRUSTED_ISSUER_REQUIRED");
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "kid", source = "source.kid")
    @Mapping(target = "algorithm", source = "source.algorithm")
    @Mapping(target = "publicJwk", expression = "java(source.getPublicJwk().toString())")
    @Mapping(target = "status", expression = "java(source.getStatus().name())")
    @Mapping(target = "runtimeServing", expression = "java(runtimeServing)")
    @Mapping(target = "activatedAt", source = "source.activatedAt")
    @Mapping(target = "retiredAt", source = "source.retiredAt")
    @Mapping(target = "version", source = "source.version")
    @Mapping(target = "createdAt", source = "source.createdAt")
    @Mapping(target = "updatedAt", source = "source.updatedAt")
    @Mapping(target = "issuer", expression = "java(issuer.toASCIIString())")
    SigningKeyVO toView(OAuthSigningKeyBO source, @Context URI issuer, @Context boolean runtimeServing);

    default SigningKeyVO toTarget(OAuthSigningKeyBO source, URI issuer, boolean runtimeServing) {
        return toView(Objects.requireNonNull(source, "source"), Objects.requireNonNull(issuer, "issuer"),
                runtimeServing);
    }
}
