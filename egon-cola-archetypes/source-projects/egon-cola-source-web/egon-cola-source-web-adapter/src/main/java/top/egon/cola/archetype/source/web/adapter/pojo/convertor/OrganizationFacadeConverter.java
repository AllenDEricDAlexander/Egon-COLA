package top.egon.cola.archetype.source.web.adapter.pojo.convertor;

import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValueMappingStrategy;
import top.egon.cola.archetype.source.web.application.teaching.pojo.command.AssignUserToClassCommand;
import top.egon.cola.archetype.source.web.application.teaching.pojo.command.CreateGradeCommand;
import top.egon.cola.archetype.source.web.application.teaching.pojo.command.CreateSchoolClassCommand;
import top.egon.cola.archetype.source.web.application.teaching.pojo.result.GradeDetailResult;
import top.egon.cola.archetype.source.web.application.teaching.pojo.result.SchoolClassDetailResult;
import top.egon.cola.archetype.source.web.application.user.pojo.command.AssignRoleCommand;
import top.egon.cola.archetype.source.web.application.user.pojo.command.CreateUserCommand;
import top.egon.cola.archetype.source.web.application.user.pojo.command.GrantPermissionCommand;
import top.egon.cola.archetype.source.web.application.user.pojo.result.PermissionTreeResult;
import top.egon.cola.archetype.source.web.application.user.pojo.result.UserDetailResult;
import top.egon.cola.archetype.source.web.facade.proto.AssignRoleRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.AssignUserRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.CreateGradeRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.CreateSchoolClassRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.CreateUserRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.GradeResponse;
import top.egon.cola.archetype.source.web.facade.proto.GradeRpcResponse;
import top.egon.cola.archetype.source.web.facade.proto.GrantPermissionRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.PermissionTreeResponse;
import top.egon.cola.archetype.source.web.facade.proto.PermissionTreeRpcResponse;
import top.egon.cola.archetype.source.web.facade.proto.RpcResponse;
import top.egon.cola.archetype.source.web.facade.proto.SchoolClassResponse;
import top.egon.cola.archetype.source.web.facade.proto.SchoolClassRpcResponse;
import top.egon.cola.archetype.source.web.facade.proto.UserResponse;
import top.egon.cola.archetype.source.web.facade.proto.UserRpcResponse;
import top.egon.cola.component.common.core.converter.BaseConverter;

/**
 * Maps the web-owned Protobuf wire onto the application Commands, Queries and Results.
 *
 * <p>The reversible pair is the user projection because a Protobuf request cannot restore the
 * idempotency key an inbound write needs; the envelope constants are the values the retired transport
 * DTOs produced, so a success still reports {@code SUCCESS}/{@code success} and a rejection keeps
 * carrying its string code instead of an integer wire status.</p>
 */
@Mapper(collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface OrganizationFacadeConverter extends BaseConverter<UserDetailResult, UserResponse> {

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "status", source = "status")
    UserResponse toTarget(UserDetailResult source);

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "roleCodes", source = "roleCodesList")
    UserDetailResult toSource(UserResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "name", source = "request.name")
    @Mapping(target = "email", source = "request.email")
    CreateUserCommand createUserCommand(CreateUserRpcRequest request, String requestId);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "userId", source = "request.userId")
    @Mapping(target = "roleCode", source = "request.roleCode")
    AssignRoleCommand assignRoleCommand(AssignRoleRpcRequest request, String requestId);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "roleCode", source = "request.roleCode")
    @Mapping(target = "permissionCode", source = "request.permissionCode")
    GrantPermissionCommand grantPermissionCommand(GrantPermissionRpcRequest request, String requestId);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "code", source = "request.code")
    @Mapping(target = "name", source = "request.name")
    CreateGradeCommand createGradeCommand(CreateGradeRpcRequest request, String requestId);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "name", source = "request.name")
    @Mapping(target = "gradeCode", source = "request.gradeCode")
    CreateSchoolClassCommand createSchoolClassCommand(CreateSchoolClassRpcRequest request, String requestId);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "gradeId", source = "request.gradeId")
    @Mapping(target = "userId", source = "request.userId")
    @Mapping(target = "schoolClassId", source = "request.schoolClassId")
    AssignUserToClassCommand assignUserToClassCommand(AssignUserRpcRequest request, String requestId);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "status", source = "status")
    GradeResponse gradeResponse(GradeDetailResult source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "gradeCode", source = "gradeCode")
    @Mapping(target = "gradeName", source = "gradeName")
    @Mapping(target = "status", source = "status")
    SchoolClassResponse schoolClassResponse(SchoolClassDetailResult source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "userId", source = "userId")
    PermissionTreeResponse permissionTreeResponse(PermissionTreeResult source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", constant = "true")
    @Mapping(target = "code", constant = "SUCCESS")
    @Mapping(target = "message", constant = "success")
    @Mapping(target = "data", source = "source")
    UserRpcResponse userSuccess(UserDetailResult source);

    @BeanMapping(ignoreByDefault = true, nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    @Mapping(target = "success", constant = "false")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "traceId", source = "traceId")
    UserRpcResponse userFailure(String code, String message, String traceId);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", constant = "true")
    @Mapping(target = "code", constant = "SUCCESS")
    @Mapping(target = "message", constant = "success")
    @Mapping(target = "data", source = "source")
    GradeRpcResponse gradeSuccess(GradeDetailResult source);

    @BeanMapping(ignoreByDefault = true, nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    @Mapping(target = "success", constant = "false")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "traceId", source = "traceId")
    GradeRpcResponse gradeFailure(String code, String message, String traceId);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", constant = "true")
    @Mapping(target = "code", constant = "SUCCESS")
    @Mapping(target = "message", constant = "success")
    @Mapping(target = "data", source = "source")
    SchoolClassRpcResponse schoolClassSuccess(SchoolClassDetailResult source);

    @BeanMapping(ignoreByDefault = true, nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    @Mapping(target = "success", constant = "false")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "traceId", source = "traceId")
    SchoolClassRpcResponse schoolClassFailure(String code, String message, String traceId);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", constant = "true")
    @Mapping(target = "code", constant = "SUCCESS")
    @Mapping(target = "message", constant = "success")
    @Mapping(target = "data", source = "source")
    PermissionTreeRpcResponse permissionTreeSuccess(PermissionTreeResult source);

    @BeanMapping(ignoreByDefault = true, nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    @Mapping(target = "success", constant = "false")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "traceId", source = "traceId")
    PermissionTreeRpcResponse permissionTreeFailure(String code, String message, String traceId);

    @BeanMapping(ignoreByDefault = true, nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    @Mapping(target = "success", source = "success")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "traceId", source = "traceId")
    RpcResponse response(boolean success, String code, String message, String traceId);

    /** Protobuf exposes immutable list getters; append through its builder API. */
    @AfterMapping
    default void appendUserResponseCollections(UserDetailResult source, @MappingTarget UserResponse.Builder target) {
        target.addAllRoleCodes(source.roleCodes());
    }

    /** Protobuf exposes immutable list getters; append through its builder API. */
    @AfterMapping
    default void appendPermissionTreeResponseCollections(
            PermissionTreeResult source, @MappingTarget PermissionTreeResponse.Builder target) {
        target.addAllPermissionCodes(source.permissionCodes());
    }

    /** Protobuf exposes immutable list getters; append through its builder API. */
    @AfterMapping
    default void appendSchoolClassResponseCollections(
            SchoolClassDetailResult source, @MappingTarget SchoolClassResponse.Builder target) {
        target.addAllUserIds(source.userIds());
    }
}
