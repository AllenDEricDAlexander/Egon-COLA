package top.egon.cola.platform.tianquan.shoubing.admin.identity.domain.converter;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.domain.po.IdentityCredentialPO;
import top.egon.cola.platform.tianquan.shoubing.core.identity.PasswordCredential;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface IdentityCredentialConverter extends BaseConverter<IdentityCredentialPO, PasswordCredential> {

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "identitySub", source = "identitySub")
    @Mapping(target = "passwordHash", source = "passwordHash")
    @Mapping(target = "passwordChangedAt", source = "passwordChangedAt")
    @Mapping(target = "mustChangePassword", source = "mustChangePassword")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "version", source = "version")
    PasswordCredential toTarget(IdentityCredentialPO source);

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "legacyId", source = "identitySub")
    @Mapping(target = "identitySub", source = "identitySub")
    @Mapping(target = "credentialType", constant = "PASSWORD")
    @Mapping(target = "passwordHash", source = "passwordHash")
    @Mapping(target = "passwordChangedAt", source = "passwordChangedAt")
    @Mapping(target = "mustChangePassword", source = "mustChangePassword")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "version", source = "version")
    IdentityCredentialPO toSource(PasswordCredential target);
}
