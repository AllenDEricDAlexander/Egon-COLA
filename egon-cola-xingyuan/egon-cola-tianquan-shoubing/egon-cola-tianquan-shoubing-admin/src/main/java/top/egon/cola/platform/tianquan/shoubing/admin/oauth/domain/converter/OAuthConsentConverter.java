package top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthConsentBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.po.OAuthConsentPO;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OAuthConsentConverter extends BaseConverter<OAuthConsentPO, OAuthConsentBO> {

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "clientPk", source = "clientPk")
    @Mapping(target = "subject", source = "subject")
    @Mapping(target = "authorities", source = "authorities")
    @Mapping(target = "grantedAt", source = "grantedAt")
    @Mapping(target = "status", source = "status")
    OAuthConsentBO toTarget(OAuthConsentPO source);

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "clientPk", source = "clientPk")
    @Mapping(target = "subject", source = "subject")
    @Mapping(target = "authorities", source = "authorities")
    @Mapping(target = "grantedAt", source = "grantedAt")
    @Mapping(target = "status", source = "status")
    OAuthConsentPO toSource(OAuthConsentBO target);
}
