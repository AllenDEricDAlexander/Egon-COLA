package top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.po.OAuthClientPO;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OAuthClientConverter extends BaseConverter<OAuthClientPO, OAuthClientBO> {

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "clientId", source = "clientId")
    @Mapping(target = "appId", source = "appId")
    @Mapping(target = "clientName", source = "clientName")
    @Mapping(target = "clientType", source = "clientType")
    @Mapping(target = "purpose", source = "purpose")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "secretHash", source = "secretHash")
    @Mapping(target = "secretHint", source = "secretHint")
    @Mapping(target = "credentialVersion", source = "credentialVersion")
    @Mapping(target = "redirectUris", source = "redirectUris", qualifiedByName = "copyStrings")
    @Mapping(target = "resourceUris", source = "resourceUris", qualifiedByName = "copyStrings")
    @Mapping(target = "allowedScopes", source = "allowedScopes", qualifiedByName = "copyStrings")
    @Mapping(target = "accessTtlSeconds", source = "accessTtlSeconds")
    @Mapping(target = "refreshTtlSeconds", source = "refreshTtlSeconds")
    @Mapping(target = "createdAt", source = "createTime")
    @Mapping(target = "updatedAt", source = "updateTime")
    OAuthClientBO toTarget(OAuthClientPO source);

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "clientId", source = "clientId")
    @Mapping(target = "appId", source = "appId")
    @Mapping(target = "clientName", source = "clientName")
    @Mapping(target = "clientType", source = "clientType")
    @Mapping(target = "purpose", source = "purpose")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "secretHash", source = "secretHash")
    @Mapping(target = "secretHint", source = "secretHint")
    @Mapping(target = "credentialVersion", source = "credentialVersion")
    @Mapping(target = "redirectUris", source = "redirectUris", qualifiedByName = "copyStrings")
    @Mapping(target = "resourceUris", source = "resourceUris", qualifiedByName = "copyStrings")
    @Mapping(target = "allowedScopes", source = "allowedScopes", qualifiedByName = "copyStrings")
    @Mapping(target = "accessTtlSeconds", source = "accessTtlSeconds")
    @Mapping(target = "refreshTtlSeconds", source = "refreshTtlSeconds")
    OAuthClientPO toSource(OAuthClientBO target);

    @Named("copyStrings")
    default List<String> copyStrings(List<String> values) {
        return values == null ? null : List.copyOf(values);
    }
}
