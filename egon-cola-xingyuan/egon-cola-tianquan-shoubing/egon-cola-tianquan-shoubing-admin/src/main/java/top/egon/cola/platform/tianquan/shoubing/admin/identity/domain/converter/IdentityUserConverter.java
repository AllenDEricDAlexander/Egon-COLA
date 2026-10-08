package top.egon.cola.platform.tianquan.shoubing.admin.identity.domain.converter;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.domain.po.IdentityUserPO;
import top.egon.cola.platform.tianquan.shoubing.core.identity.IdentityUser;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface IdentityUserConverter extends BaseConverter<IdentityUserPO, IdentityUser> {

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "identitySub")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "normalizedUsername", source = "usernameNormalized")
    @Mapping(target = "displayName", source = "displayName")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "failedLoginCount", source = "failedLoginCount")
    @Mapping(target = "lockedUntil", source = "lockedUntil")
    @Mapping(target = "lastLoginAt", source = "lastLoginAt")
    @Mapping(target = "version", source = "version")
    IdentityUser toTarget(IdentityUserPO source);

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "identitySub", source = "id")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "usernameNormalized", source = "normalizedUsername")
    @Mapping(target = "displayName", source = "displayName")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "failedLoginCount", source = "failedLoginCount")
    @Mapping(target = "lockedUntil", source = "lockedUntil")
    @Mapping(target = "lastLoginAt", source = "lastLoginAt")
    @Mapping(target = "version", source = "version")
    IdentityUserPO toSource(IdentityUser target);
}
