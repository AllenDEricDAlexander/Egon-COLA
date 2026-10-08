package top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.po.OAuthRealmPO;

import java.net.URI;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OAuthRealmConverter extends BaseConverter<OAuthRealmPO, OAuthRealmBO> {

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "issuer", source = "issuer", qualifiedByName = "issuerFromStorage")
    @Mapping(target = "realmKey", source = "realmKey")
    @Mapping(target = "realmType", source = "realmType")
    @Mapping(target = "businessTenantId", source = "businessTenantId")
    @Mapping(target = "storagePartitionId", source = "storagePartitionId")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "activeKid", source = "activeKid")
    @Mapping(target = "trustVersion", source = "trustVersion")
    OAuthRealmBO toTarget(OAuthRealmPO source);

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "version", source = "version")
    @Mapping(target = "issuer", source = "issuer", qualifiedByName = "issuerToStorage")
    @Mapping(target = "realmKey", source = "realmKey")
    @Mapping(target = "realmType", source = "realmType")
    @Mapping(target = "businessTenantId", source = "businessTenantId")
    @Mapping(target = "storagePartitionId", source = "storagePartitionId")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "activeKid", source = "activeKid")
    @Mapping(target = "trustVersion", source = "trustVersion")
    OAuthRealmPO toSource(OAuthRealmBO target);

    @Named("issuerFromStorage")
    default URI issuerFromStorage(String issuer) {
        return issuer == null ? null : URI.create(issuer);
    }

    @Named("issuerToStorage")
    default String issuerToStorage(URI issuer) {
        return issuer == null ? null : issuer.toASCIIString();
    }
}
