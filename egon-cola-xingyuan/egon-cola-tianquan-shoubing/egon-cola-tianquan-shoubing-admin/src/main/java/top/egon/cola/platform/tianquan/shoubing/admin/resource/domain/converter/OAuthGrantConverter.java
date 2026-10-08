package top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.converter;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthGrantBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.po.OAuthGrantPO;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OAuthGrantConverter extends BaseConverter<OAuthGrantPO, OAuthGrantBO> {

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "clientPk", source = "clientPk")
    @Mapping(target = "resourceServerId", source = "resourceServerId")
    @Mapping(target = "grantType", source = "grantType")
    @Mapping(target = "allowedScopes", source = "allowedScopes", qualifiedByName = "copyScopes")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "resourceVersion", source = "resourceVersion")
    OAuthGrantBO toTarget(OAuthGrantPO source);

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "clientPk", source = "clientPk")
    @Mapping(target = "resourceServerId", source = "resourceServerId")
    @Mapping(target = "grantType", source = "grantType")
    @Mapping(target = "allowedScopes", source = "allowedScopes", qualifiedByName = "copyScopes")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "resourceVersion", source = "resourceVersion")
    OAuthGrantPO toSource(OAuthGrantBO target);

    @Named("copyScopes")
    default List<String> copyScopes(List<String> values) {
        return values == null ? null : List.copyOf(values);
    }
}
