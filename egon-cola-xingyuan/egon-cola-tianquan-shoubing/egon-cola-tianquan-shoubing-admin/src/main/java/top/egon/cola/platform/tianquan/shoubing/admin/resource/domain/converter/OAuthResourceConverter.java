package top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.converter;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthResourceBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.po.OAuthResourcePO;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus;

import java.net.URI;
import java.time.Duration;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OAuthResourceConverter extends BaseConverter<OAuthResourcePO, OAuthResourceBO> {

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "resourceServerId", source = "resourceServerId")
    @Mapping(target = "resourceUri", source = "resourceUri", qualifiedByName = "resourceUriFromStorage")
    @Mapping(target = "bizCode", source = "bizCode")
    @Mapping(target = "appCode", source = "appCode")
    @Mapping(target = "environment", source = "environment")
    @Mapping(target = "displayName", source = "displayName")
    @Mapping(target = "managementClientId", source = "managementClientId")
    @Mapping(target = "managementClientPk", source = "managementClientPk")
    @Mapping(target = "rbacApplicationCode", source = "rbacApplicationCode")
    @Mapping(target = "entryPermissionCode", source = "entryPermissionCode")
    @Mapping(target = "status", source = "status", qualifiedByName = "resourceStatusFromStorage")
    @Mapping(target = "admissionTicketTtl", source = "admissionTicketTtlSeconds",
            qualifiedByName = "ticketTtlFromStorage")
    @Mapping(target = "createdAt", source = "createTime")
    @Mapping(target = "updatedAt", source = "updateTime")
    OAuthResourceBO toTarget(OAuthResourcePO source);

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "resourceServerId", source = "resourceServerId")
    @Mapping(target = "resourceUri", source = "resourceUri", qualifiedByName = "resourceUriToStorage")
    @Mapping(target = "bizCode", source = "bizCode")
    @Mapping(target = "appCode", source = "appCode")
    @Mapping(target = "environment", source = "environment")
    @Mapping(target = "displayName", source = "displayName")
    @Mapping(target = "managementClientId", source = "managementClientId")
    @Mapping(target = "managementClientPk", source = "managementClientPk")
    @Mapping(target = "rbacApplicationCode", source = "rbacApplicationCode")
    @Mapping(target = "entryPermissionCode", source = "entryPermissionCode")
    @Mapping(target = "status", source = "status", qualifiedByName = "resourceStatusToStorage")
    @Mapping(target = "admissionTicketTtlSeconds", source = "admissionTicketTtl",
            qualifiedByName = "ticketTtlToStorage")
    OAuthResourcePO toSource(OAuthResourceBO target);

    @Named("resourceUriFromStorage")
    default URI resourceUriFromStorage(String value) {
        return value == null ? null : URI.create(value);
    }

    @Named("resourceUriToStorage")
    default String resourceUriToStorage(URI value) {
        return value == null ? null : value.toASCIIString();
    }

    @Named("resourceStatusFromStorage")
    default ResourceServerStatus resourceStatusFromStorage(String value) {
        return value == null ? null : ResourceServerStatus.valueOf(value);
    }

    @Named("resourceStatusToStorage")
    default String resourceStatusToStorage(ResourceServerStatus value) {
        return value == null ? null : value.name();
    }

    @Named("ticketTtlFromStorage")
    default Duration ticketTtlFromStorage(Integer value) {
        return value == null ? null : Duration.ofSeconds(value);
    }

    @Named("ticketTtlToStorage")
    default Integer ticketTtlToStorage(Duration value) {
        if (value == null) {
            return null;
        }
        if (value.getNano() != 0) {
            throw new IllegalArgumentException("ADMISSION_TICKET_TTL_MUST_USE_WHOLE_SECONDS");
        }
        return Math.toIntExact(value.getSeconds());
    }
}
