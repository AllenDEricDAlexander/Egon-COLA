package top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.converter;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.mapstruct.BeanMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthGrantBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.vo.ClientResourceGrantVO;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ClientResourceGrantViewConverter extends BaseForwardConverter<OAuthGrantBO, ClientResourceGrantVO> {

    @Override
    default ClientResourceGrantVO toTarget(OAuthGrantBO source) {
        throw new IllegalStateException("OAUTH_GRANT_VIEW_CLIENT_AND_REALM_REQUIRED");
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "clientId", expression = "java(client.getClientId())")
    @Mapping(target = "resourceServerId", source = "source.resourceServerId")
    @Mapping(target = "grantType", expression = "java(source.getGrantType().name())")
    @Mapping(target = "tenantId", expression = "java(source.getGrantType() == top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceGrantType.USER_DELEGATION || realm.getBusinessTenantId() == null ? null : realm.getBusinessTenantId().toString())")
    @Mapping(target = "allowedScopes", source = "source.allowedScopes", qualifiedByName = "scopeSet")
    @Mapping(target = "status", expression = "java(source.getStatus().name())")
    @Mapping(target = "version", source = "source.version")
    ClientResourceGrantVO toView(
            OAuthGrantBO source,
            @Context TrustedRealmBO realm,
            @Context OAuthClientBO client);

    default ClientResourceGrantVO toTarget(OAuthGrantBO source, TrustedRealmBO realm, OAuthClientBO client) {
        Objects.requireNonNull(realm, "realm").validateTrustedPartition();
        Objects.requireNonNull(client, "client");
        return toView(Objects.requireNonNull(source, "source"), realm, client);
    }

    @Named("scopeSet")
    default Set<String> scopeSet(List<String> scopes) {
        return scopes == null ? Set.of() : Set.copyOf(scopes);
    }
}
