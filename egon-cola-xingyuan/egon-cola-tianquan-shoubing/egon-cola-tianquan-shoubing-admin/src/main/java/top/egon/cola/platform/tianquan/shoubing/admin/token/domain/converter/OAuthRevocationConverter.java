package top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthRevocationBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.po.OAuthRevocationPO;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OAuthRevocationConverter extends BaseConverter<OAuthRevocationPO, OAuthRevocationBO> {

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "targetType", source = "targetType")
    @Mapping(target = "targetKey", source = "targetKey")
    @Mapping(target = "reasonCode", source = "reasonCode")
    @Mapping(target = "revokedAt", source = "revokedAt")
    @Mapping(target = "expiresAt", source = "expiresAt")
    @Mapping(target = "revision", source = "revision")
    @Mapping(target = "projectionStatus", source = "projectionStatus")
    OAuthRevocationBO toTarget(OAuthRevocationPO source);

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "targetType", source = "targetType")
    @Mapping(target = "targetKey", source = "targetKey")
    @Mapping(target = "reasonCode", source = "reasonCode")
    @Mapping(target = "revokedAt", source = "revokedAt")
    @Mapping(target = "expiresAt", source = "expiresAt")
    @Mapping(target = "revision", source = "revision")
    @Mapping(target = "projectionStatus", source = "projectionStatus")
    OAuthRevocationPO toSource(OAuthRevocationBO target);
}
