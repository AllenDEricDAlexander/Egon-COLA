package top.egon.cola.archetype.source.service.infrastructure.client.organization;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.organization.facade.rpc.RpcIdQuery;
import top.egon.cola.organization.facade.rpc.RpcSchoolClassQuery;
import top.egon.cola.organization.facade.rpc.proto.GetUserRpcRequest;
import top.egon.cola.organization.facade.rpc.proto.GetSchoolClassRpcRequest;
import top.egon.cola.organization.facade.user.dto.UserDetailDTO;
import top.egon.cola.organization.facade.teaching.dto.SchoolClassDetailDTO;
import top.egon.cola.archetype.source.service.domain.client.organization.OrganizationUser;
import top.egon.cola.archetype.source.service.domain.client.organization.OrganizationSchoolClass;

/** Projects only the fields owned by the existing organization directory port. */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OrganizationDirectoryConverter extends BaseConverter<UserDetailDTO, OrganizationUser> {
    @Override
    OrganizationUser toTarget(UserDetailDTO source);

    /** Reverse projection has no email or roles because the domain port does not own them. */
    @Override
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "roleCodes", expression = "java(java.util.List.of())")
    UserDetailDTO toSource(OrganizationUser target);

    OrganizationSchoolClass toTarget(SchoolClassDetailDTO source);

    @Mapping(target = "gradeName", ignore = true)
    SchoolClassDetailDTO toSource(OrganizationSchoolClass target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "userId", source = "id")
    GetUserRpcRequest userRequest(RpcIdQuery source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "gradeId", source = "gradeId")
    @Mapping(target = "schoolClassId", source = "schoolClassId")
    GetSchoolClassRpcRequest schoolClassRequest(RpcSchoolClassQuery source);
}
