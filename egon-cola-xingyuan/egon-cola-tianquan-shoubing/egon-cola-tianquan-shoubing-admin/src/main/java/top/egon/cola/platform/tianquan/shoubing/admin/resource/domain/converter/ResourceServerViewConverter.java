package top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.converter;

import java.util.Objects;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthResourceBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.vo.ResourceServerVO;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ResourceServerViewConverter extends BaseForwardConverter<OAuthResourceBO, ResourceServerVO> {

    @Override
    default ResourceServerVO toTarget(OAuthResourceBO source) {
        return toView(Objects.requireNonNull(source, "source"));
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "resourceServerId", source = "resourceServerId")
    @Mapping(target = "resourceUri", expression = "java(source.getResourceUri().toASCIIString())")
    @Mapping(target = "bizCode", source = "bizCode")
    @Mapping(target = "appCode", source = "appCode")
    @Mapping(target = "environment", source = "environment")
    @Mapping(target = "displayName", source = "displayName")
    @Mapping(target = "managementClientId", source = "managementClientId")
    @Mapping(target = "rbacApplicationCode", source = "rbacApplicationCode")
    @Mapping(target = "entryPermissionCode", source = "entryPermissionCode")
    @Mapping(target = "status", expression = "java(source.getStatus().name())")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    ResourceServerVO toView(OAuthResourceBO source);
}
