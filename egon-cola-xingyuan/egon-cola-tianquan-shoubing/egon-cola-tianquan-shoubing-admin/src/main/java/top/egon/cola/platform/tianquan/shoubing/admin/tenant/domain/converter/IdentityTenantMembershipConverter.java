package top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.bo.IdentityTenantMembershipBO;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.po.IdentityTenantMembershipPO;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface IdentityTenantMembershipConverter
        extends BaseConverter<IdentityTenantMembershipPO, IdentityTenantMembershipBO> {

    @Override
    @Mapping(target = "createdBy", source = "createUserId")
    @Mapping(target = "updatedBy", source = "updateUserId")
    @Mapping(target = "createdAt", source = "createTime")
    @Mapping(target = "updatedAt", source = "updateTime")
    IdentityTenantMembershipBO toTarget(IdentityTenantMembershipPO source);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", source = "createdBy")
    @Mapping(target = "createTime", source = "createdAt")
    @Mapping(target = "updateUserId", source = "updatedBy")
    @Mapping(target = "updateTime", source = "updatedAt")
    @Mapping(target = "deletedAt", ignore = true)
    IdentityTenantMembershipPO toSource(IdentityTenantMembershipBO target);
}
