package top.egon.cola.archetype.source.light.facade.rpc;

import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import org.mapstruct.NullValueMappingStrategy;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/** Maps native Protobuf fields while preserving the existing facade DTO contract. */
@Mapper(collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface LightRpcConverter extends BaseConverter<
        top.egon.cola.archetype.source.light.facade.teaching.dto.CreateCourseDTO,
        top.egon.cola.archetype.source.light.facade.rpc.proto.CreateCourseRpcRequest> {

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "operatorId", source = "operatorId")
    @Mapping(target = "requestId", source = "requestId")
    top.egon.cola.archetype.source.light.facade.rpc.proto.CreateCourseRpcRequest toTarget(top.egon.cola.archetype.source.light.facade.teaching.dto.CreateCourseDTO source);

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "operatorId", source = "operatorId")
    @Mapping(target = "requestId", source = "requestId")
    top.egon.cola.archetype.source.light.facade.teaching.dto.CreateCourseDTO toSource(top.egon.cola.archetype.source.light.facade.rpc.proto.CreateCourseRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "semester", source = "semester")
    @Mapping(target = "operatorId", source = "operatorId")
    @Mapping(target = "requestId", source = "requestId")
    top.egon.cola.archetype.source.light.facade.rpc.proto.CreateSchoolClassRpcRequest toTarget(top.egon.cola.archetype.source.light.facade.teaching.dto.CreateSchoolClassDTO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "semester", source = "semester")
    @Mapping(target = "operatorId", source = "operatorId")
    @Mapping(target = "requestId", source = "requestId")
    top.egon.cola.archetype.source.light.facade.teaching.dto.CreateSchoolClassDTO toSource(top.egon.cola.archetype.source.light.facade.rpc.proto.CreateSchoolClassRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "schoolClassId", source = "schoolClassId")
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "startsAt", source = "startsAt")
    @Mapping(target = "endsAt", source = "endsAt")
    @Mapping(target = "operatorId", source = "operatorId")
    @Mapping(target = "requestId", source = "requestId")
    top.egon.cola.archetype.source.light.facade.rpc.proto.ScheduleCourseRpcRequest toTarget(top.egon.cola.archetype.source.light.facade.teaching.dto.ScheduleCourseDTO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "schoolClassId", source = "schoolClassId")
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "startsAt", source = "startsAt")
    @Mapping(target = "endsAt", source = "endsAt")
    @Mapping(target = "operatorId", source = "operatorId")
    @Mapping(target = "requestId", source = "requestId")
    top.egon.cola.archetype.source.light.facade.teaching.dto.ScheduleCourseDTO toSource(top.egon.cola.archetype.source.light.facade.rpc.proto.ScheduleCourseRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "externalId", source = "externalId")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "operatorId", source = "operatorId")
    @Mapping(target = "requestId", source = "requestId")
    top.egon.cola.archetype.source.light.facade.rpc.proto.CreateUserRpcRequest toTarget(top.egon.cola.archetype.source.light.facade.user.dto.CreateUserDTO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "externalId", source = "externalId")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "operatorId", source = "operatorId")
    @Mapping(target = "requestId", source = "requestId")
    top.egon.cola.archetype.source.light.facade.user.dto.CreateUserDTO toSource(top.egon.cola.archetype.source.light.facade.rpc.proto.CreateUserRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "roleCode", source = "roleCode")
    @Mapping(target = "operatorId", source = "operatorId")
    @Mapping(target = "requestId", source = "requestId")
    top.egon.cola.archetype.source.light.facade.rpc.proto.AssignRoleRpcRequest toTarget(top.egon.cola.archetype.source.light.facade.user.dto.AssignRoleDTO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "roleCode", source = "roleCode")
    @Mapping(target = "operatorId", source = "operatorId")
    @Mapping(target = "requestId", source = "requestId")
    top.egon.cola.archetype.source.light.facade.user.dto.AssignRoleDTO toSource(top.egon.cola.archetype.source.light.facade.rpc.proto.AssignRoleRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "roleCode", source = "roleCode")
    @Mapping(target = "permissionCode", source = "permissionCode")
    @Mapping(target = "operatorId", source = "operatorId")
    @Mapping(target = "requestId", source = "requestId")
    top.egon.cola.archetype.source.light.facade.rpc.proto.GrantPermissionRpcRequest toTarget(top.egon.cola.archetype.source.light.facade.user.dto.GrantPermissionDTO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "roleCode", source = "roleCode")
    @Mapping(target = "permissionCode", source = "permissionCode")
    @Mapping(target = "operatorId", source = "operatorId")
    @Mapping(target = "requestId", source = "requestId")
    top.egon.cola.archetype.source.light.facade.user.dto.GrantPermissionDTO toSource(top.egon.cola.archetype.source.light.facade.rpc.proto.GrantPermissionRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "status", source = "status")
    top.egon.cola.archetype.source.light.facade.rpc.proto.CourseResponse toTarget(top.egon.cola.archetype.source.light.facade.teaching.dto.CourseDTO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "status", source = "status")
    top.egon.cola.archetype.source.light.facade.teaching.dto.CourseDTO toSource(top.egon.cola.archetype.source.light.facade.rpc.proto.CourseResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "semester", source = "semester")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "scheduleCount", source = "scheduleCount")
    top.egon.cola.archetype.source.light.facade.rpc.proto.SchoolClassResponse toTarget(top.egon.cola.archetype.source.light.facade.teaching.dto.SchoolClassDetailDTO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "semester", source = "semester")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "scheduleCount", source = "scheduleCount")
    top.egon.cola.archetype.source.light.facade.teaching.dto.SchoolClassDetailDTO toSource(top.egon.cola.archetype.source.light.facade.rpc.proto.SchoolClassResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "status", source = "status")
    top.egon.cola.archetype.source.light.facade.rpc.proto.UserResponse toTarget(top.egon.cola.archetype.source.light.facade.user.dto.UserDetailDTO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "status", source = "status")
    top.egon.cola.archetype.source.light.facade.user.dto.UserDetailDTO toSource(top.egon.cola.archetype.source.light.facade.rpc.proto.UserResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "roleCode", source = "roleCode")
    @Mapping(target = "permissionCode", source = "permissionCode")
    @Mapping(target = "status", source = "status")
    top.egon.cola.archetype.source.light.facade.rpc.proto.PermissionResponse toTarget(top.egon.cola.archetype.source.light.facade.user.dto.PermissionDTO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "roleCode", source = "roleCode")
    @Mapping(target = "permissionCode", source = "permissionCode")
    @Mapping(target = "status", source = "status")
    top.egon.cola.archetype.source.light.facade.user.dto.PermissionDTO toSource(top.egon.cola.archetype.source.light.facade.rpc.proto.PermissionResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    top.egon.cola.archetype.source.light.facade.rpc.proto.PermissionDetailResponse toTarget(top.egon.cola.archetype.source.light.facade.user.dto.PermissionDetailDTO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "children", source = "childrenList")
    top.egon.cola.archetype.source.light.facade.user.dto.PermissionDetailDTO toSource(top.egon.cola.archetype.source.light.facade.rpc.proto.PermissionDetailResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", constant = "true")
    @Mapping(target = "code", constant = "SUCCESS")
    @Mapping(target = "message", constant = "success")
    @Mapping(target = "data", source = "source")
    top.egon.cola.archetype.source.light.facade.rpc.proto.CourseRpcResponse courseSuccess(top.egon.cola.archetype.source.light.facade.teaching.dto.CourseDTO source);

    @BeanMapping(ignoreByDefault = true, nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    @Mapping(target = "success", constant = "false")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "traceId", source = "traceId")
    top.egon.cola.archetype.source.light.facade.rpc.proto.CourseRpcResponse courseFailure(String code, String message, String traceId);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", constant = "true")
    @Mapping(target = "code", constant = "SUCCESS")
    @Mapping(target = "message", constant = "success")
    @Mapping(target = "data", source = "source")
    top.egon.cola.archetype.source.light.facade.rpc.proto.SchoolClassRpcResponse schoolClassSuccess(top.egon.cola.archetype.source.light.facade.teaching.dto.SchoolClassDetailDTO source);

    @BeanMapping(ignoreByDefault = true, nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    @Mapping(target = "success", constant = "false")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "traceId", source = "traceId")
    top.egon.cola.archetype.source.light.facade.rpc.proto.SchoolClassRpcResponse schoolClassFailure(String code, String message, String traceId);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", constant = "true")
    @Mapping(target = "code", constant = "SUCCESS")
    @Mapping(target = "message", constant = "success")
    @Mapping(target = "data", source = "source")
    top.egon.cola.archetype.source.light.facade.rpc.proto.UserRpcResponse userSuccess(top.egon.cola.archetype.source.light.facade.user.dto.UserDetailDTO source);

    @BeanMapping(ignoreByDefault = true, nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    @Mapping(target = "success", constant = "false")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "traceId", source = "traceId")
    top.egon.cola.archetype.source.light.facade.rpc.proto.UserRpcResponse userFailure(String code, String message, String traceId);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", constant = "true")
    @Mapping(target = "code", constant = "SUCCESS")
    @Mapping(target = "message", constant = "success")
    @Mapping(target = "data", source = "source")
    top.egon.cola.archetype.source.light.facade.rpc.proto.PermissionRpcResponse permissionSuccess(top.egon.cola.archetype.source.light.facade.user.dto.PermissionDTO source);

    @BeanMapping(ignoreByDefault = true, nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    @Mapping(target = "success", constant = "false")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "traceId", source = "traceId")
    top.egon.cola.archetype.source.light.facade.rpc.proto.PermissionRpcResponse permissionFailure(String code, String message, String traceId);

    @BeanMapping(ignoreByDefault = true, nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    @Mapping(target = "success", source = "success")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "traceId", source = "traceId")
    top.egon.cola.archetype.source.light.facade.rpc.proto.PermissionListRpcResponse permissionListResponse(
            List<top.egon.cola.archetype.source.light.facade.user.dto.PermissionDetailDTO> data, boolean success,
            String code, String message, String traceId);

    @AfterMapping
    default void appendPermissionList(
            List<top.egon.cola.archetype.source.light.facade.user.dto.PermissionDetailDTO> data, boolean success,
            @MappingTarget top.egon.cola.archetype.source.light.facade.rpc.proto.PermissionListRpcResponse.Builder target) {
        if (success) {
            Objects.requireNonNull(data, "permission result must not be null");
        }
        if (data != null) {
            target.addAllData(data.stream().map(this::toTarget).toList());
        }
    }

    /** Protobuf exposes immutable list getters; append through its builder API. */
    @AfterMapping
    default void appendPermissionDetailResponseCollections(
            top.egon.cola.archetype.source.light.facade.user.dto.PermissionDetailDTO source,
            @MappingTarget top.egon.cola.archetype.source.light.facade.rpc.proto.PermissionDetailResponse.Builder target) {
        target.addAllChildren(source.children().stream().map(this::toTarget).toList());
    }

    default String toProtoTime(LocalDateTime value) {
        return value == null ? null : DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(value);
    }

    default LocalDateTime fromProtoTime(String value) {
        return value == null ? null : LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
