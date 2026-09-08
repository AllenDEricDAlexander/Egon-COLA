package top.egon.cola.organization.facade.rpc;

import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import org.mapstruct.NullValueMappingStrategy;

/** Maps native Protobuf fields while preserving the existing facade DTO contract. */
@Mapper(collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface OrganizationRpcConverter extends BaseConverter<
        top.egon.cola.organization.facade.user.dto.CreateUserDTO,
        top.egon.cola.organization.facade.rpc.proto.CreateUserRpcRequest> {

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    top.egon.cola.organization.facade.rpc.proto.CreateUserRpcRequest toTarget(top.egon.cola.organization.facade.user.dto.CreateUserDTO source);

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    top.egon.cola.organization.facade.user.dto.CreateUserDTO toSource(top.egon.cola.organization.facade.rpc.proto.CreateUserRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "roleCode", source = "roleCode")
    top.egon.cola.organization.facade.rpc.proto.AssignRoleRpcRequest toTarget(top.egon.cola.organization.facade.user.dto.AssignRoleDTO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "roleCode", source = "roleCode")
    top.egon.cola.organization.facade.user.dto.AssignRoleDTO toSource(top.egon.cola.organization.facade.rpc.proto.AssignRoleRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "roleCode", source = "roleCode")
    @Mapping(target = "permissionCode", source = "permissionCode")
    top.egon.cola.organization.facade.rpc.proto.GrantPermissionRpcRequest toTarget(top.egon.cola.organization.facade.user.dto.GrantPermissionDTO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "roleCode", source = "roleCode")
    @Mapping(target = "permissionCode", source = "permissionCode")
    top.egon.cola.organization.facade.user.dto.GrantPermissionDTO toSource(top.egon.cola.organization.facade.rpc.proto.GrantPermissionRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    top.egon.cola.organization.facade.rpc.proto.CreateGradeRpcRequest toTarget(top.egon.cola.organization.facade.teaching.dto.CreateGradeDTO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    top.egon.cola.organization.facade.teaching.dto.CreateGradeDTO toSource(top.egon.cola.organization.facade.rpc.proto.CreateGradeRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "gradeCode", source = "gradeCode")
    top.egon.cola.organization.facade.rpc.proto.CreateSchoolClassRpcRequest toTarget(top.egon.cola.organization.facade.teaching.dto.CreateSchoolClassDTO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "gradeCode", source = "gradeCode")
    top.egon.cola.organization.facade.teaching.dto.CreateSchoolClassDTO toSource(top.egon.cola.organization.facade.rpc.proto.CreateSchoolClassRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "gradeId", source = "gradeId")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "schoolClassId", source = "schoolClassId")
    top.egon.cola.organization.facade.rpc.proto.AssignUserRpcRequest toTarget(top.egon.cola.organization.facade.teaching.dto.AssignUserToClassDTO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "gradeId", source = "gradeId")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "schoolClassId", source = "schoolClassId")
    top.egon.cola.organization.facade.teaching.dto.AssignUserToClassDTO toSource(top.egon.cola.organization.facade.rpc.proto.AssignUserRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "status", source = "status")
    top.egon.cola.organization.facade.rpc.proto.UserResponse toTarget(top.egon.cola.organization.facade.user.dto.UserDetailDTO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "roleCodes", source = "roleCodesList")
    top.egon.cola.organization.facade.user.dto.UserDetailDTO toSource(top.egon.cola.organization.facade.rpc.proto.UserResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "userId", source = "userId")
    top.egon.cola.organization.facade.rpc.proto.PermissionTreeResponse toTarget(top.egon.cola.organization.facade.user.dto.PermissionTreeDTO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "permissionCodes", source = "permissionCodesList")
    top.egon.cola.organization.facade.user.dto.PermissionTreeDTO toSource(top.egon.cola.organization.facade.rpc.proto.PermissionTreeResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "status", source = "status")
    top.egon.cola.organization.facade.rpc.proto.GradeResponse toTarget(top.egon.cola.organization.facade.teaching.dto.GradeDetailDTO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "status", source = "status")
    top.egon.cola.organization.facade.teaching.dto.GradeDetailDTO toSource(top.egon.cola.organization.facade.rpc.proto.GradeResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "gradeCode", source = "gradeCode")
    @Mapping(target = "gradeName", source = "gradeName")
    @Mapping(target = "status", source = "status")
    top.egon.cola.organization.facade.rpc.proto.SchoolClassResponse toTarget(top.egon.cola.organization.facade.teaching.dto.SchoolClassDetailDTO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "gradeCode", source = "gradeCode")
    @Mapping(target = "gradeName", source = "gradeName")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "userIds", source = "userIdsList")
    top.egon.cola.organization.facade.teaching.dto.SchoolClassDetailDTO toSource(top.egon.cola.organization.facade.rpc.proto.SchoolClassResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", constant = "true")
    @Mapping(target = "code", constant = "SUCCESS")
    @Mapping(target = "message", constant = "success")
    @Mapping(target = "data", source = "source")
    top.egon.cola.organization.facade.rpc.proto.UserRpcResponse userSuccess(top.egon.cola.organization.facade.user.dto.UserDetailDTO source);

    @BeanMapping(ignoreByDefault = true, nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    @Mapping(target = "success", constant = "false")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "traceId", source = "traceId")
    top.egon.cola.organization.facade.rpc.proto.UserRpcResponse userFailure(String code, String message, String traceId);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", constant = "true")
    @Mapping(target = "code", constant = "SUCCESS")
    @Mapping(target = "message", constant = "success")
    @Mapping(target = "data", source = "source")
    top.egon.cola.organization.facade.rpc.proto.PermissionTreeRpcResponse permissionTreeSuccess(top.egon.cola.organization.facade.user.dto.PermissionTreeDTO source);

    @BeanMapping(ignoreByDefault = true, nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    @Mapping(target = "success", constant = "false")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "traceId", source = "traceId")
    top.egon.cola.organization.facade.rpc.proto.PermissionTreeRpcResponse permissionTreeFailure(String code, String message, String traceId);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", constant = "true")
    @Mapping(target = "code", constant = "SUCCESS")
    @Mapping(target = "message", constant = "success")
    @Mapping(target = "data", source = "source")
    top.egon.cola.organization.facade.rpc.proto.GradeRpcResponse gradeSuccess(top.egon.cola.organization.facade.teaching.dto.GradeDetailDTO source);

    @BeanMapping(ignoreByDefault = true, nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    @Mapping(target = "success", constant = "false")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "traceId", source = "traceId")
    top.egon.cola.organization.facade.rpc.proto.GradeRpcResponse gradeFailure(String code, String message, String traceId);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", constant = "true")
    @Mapping(target = "code", constant = "SUCCESS")
    @Mapping(target = "message", constant = "success")
    @Mapping(target = "data", source = "source")
    top.egon.cola.organization.facade.rpc.proto.SchoolClassRpcResponse schoolClassSuccess(top.egon.cola.organization.facade.teaching.dto.SchoolClassDetailDTO source);

    @BeanMapping(ignoreByDefault = true, nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    @Mapping(target = "success", constant = "false")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "traceId", source = "traceId")
    top.egon.cola.organization.facade.rpc.proto.SchoolClassRpcResponse schoolClassFailure(String code, String message, String traceId);

    @BeanMapping(ignoreByDefault = true, nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    @Mapping(target = "success", source = "success")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "traceId", source = "traceId")
    top.egon.cola.organization.facade.rpc.proto.RpcResponse response(boolean success, String code, String message, String traceId);

    /** Protobuf exposes immutable list getters; append through its builder API. */
    @AfterMapping
    default void appendUserResponseCollections(
            top.egon.cola.organization.facade.user.dto.UserDetailDTO source,
            @MappingTarget top.egon.cola.organization.facade.rpc.proto.UserResponse.Builder target) {
        target.addAllRoleCodes(source.roleCodes());
    }

    /** Protobuf exposes immutable list getters; append through its builder API. */
    @AfterMapping
    default void appendPermissionTreeResponseCollections(
            top.egon.cola.organization.facade.user.dto.PermissionTreeDTO source,
            @MappingTarget top.egon.cola.organization.facade.rpc.proto.PermissionTreeResponse.Builder target) {
        target.addAllPermissionCodes(source.permissionCodes());
    }

    /** Protobuf exposes immutable list getters; append through its builder API. */
    @AfterMapping
    default void appendSchoolClassResponseCollections(
            top.egon.cola.organization.facade.teaching.dto.SchoolClassDetailDTO source,
            @MappingTarget top.egon.cola.organization.facade.rpc.proto.SchoolClassResponse.Builder target) {
        target.addAllUserIds(source.userIds());
    }
}
