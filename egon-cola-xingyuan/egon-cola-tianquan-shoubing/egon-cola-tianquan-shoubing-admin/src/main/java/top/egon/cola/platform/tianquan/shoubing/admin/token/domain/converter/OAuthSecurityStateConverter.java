package top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthSecurityStateBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.po.OAuthSecurityStatePO;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OAuthSecurityStateConverter extends BaseConverter<OAuthSecurityStatePO, OAuthSecurityStateBO> {

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "targetType", source = "targetType")
    @Mapping(target = "targetKey", source = "targetKey")
    @Mapping(target = "generation", source = "generation")
    @Mapping(target = "enabled", source = "enabled")
    @Mapping(target = "revision", source = "revision")
    @Mapping(target = "reasonCode", source = "reasonCode")
    OAuthSecurityStateBO toTarget(OAuthSecurityStatePO source);

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "targetType", source = "targetType")
    @Mapping(target = "targetKey", source = "targetKey")
    @Mapping(target = "generation", source = "generation")
    @Mapping(target = "enabled", source = "enabled")
    @Mapping(target = "revision", source = "revision")
    @Mapping(target = "reasonCode", source = "reasonCode")
    OAuthSecurityStatePO toSource(OAuthSecurityStateBO target);
}
