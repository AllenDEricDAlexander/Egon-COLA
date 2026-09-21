package top.egon.cola.archetype.source.service.infrastructure.client.organization;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.archetype.source.service.domain.client.organization.OrganizationSchoolClass;
import top.egon.cola.archetype.source.service.domain.client.organization.OrganizationUser;
import top.egon.cola.archetype.source.web.facade.proto.GetSchoolClassRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.GetUserRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.SchoolClassResponse;
import top.egon.cola.archetype.source.web.facade.proto.UserResponse;
import top.egon.cola.component.common.core.converter.BaseConverter;

/** Projects only the fields owned by the existing organization directory port. */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OrganizationDirectoryConverter extends BaseConverter<UserResponse, OrganizationUser> {

    /** Validates scalar identifiers after Protobuf presence has been decoded. */
    record UserQuery(@NotNull @Positive Long userId) {
    }

    /** Validates scalar identifiers after Protobuf presence has been decoded. */
    record SchoolClassQuery(@NotNull @Positive Long gradeId, @NotNull @Positive Long schoolClassId) {
    }

    @Override
    OrganizationUser toTarget(UserResponse source);

    /** Reverse projection has no email or roles because the domain port does not own them. */
    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "status", source = "status")
    UserResponse toSource(OrganizationUser target);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "gradeCode", source = "gradeCode")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "userIds", source = "userIdsList")
    OrganizationSchoolClass toTarget(SchoolClassResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "userId", source = "userId")
    GetUserRpcRequest userRequest(UserQuery source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "gradeId", source = "gradeId")
    @Mapping(target = "schoolClassId", source = "schoolClassId")
    GetSchoolClassRpcRequest schoolClassRequest(SchoolClassQuery source);
}
