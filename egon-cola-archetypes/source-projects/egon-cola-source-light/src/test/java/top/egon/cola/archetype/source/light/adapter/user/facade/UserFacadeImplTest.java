package top.egon.cola.archetype.source.light.adapter.user.facade;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import top.egon.cola.archetype.source.light.adapter.pojo.convertor.LightFacadeConverter;
import top.egon.cola.archetype.source.light.adapter.user.facade.impl.PermissionFacadeImpl;
import top.egon.cola.archetype.source.light.adapter.user.facade.impl.UserFacadeImpl;
import top.egon.cola.archetype.source.light.application.user.manage.PermissionManage;
import top.egon.cola.archetype.source.light.application.user.manage.RoleManage;
import top.egon.cola.archetype.source.light.application.user.manage.UserManage;
import top.egon.cola.archetype.source.light.application.user.pojo.command.AssignRoleCommand;
import top.egon.cola.archetype.source.light.application.user.pojo.command.CreateUserCommand;
import top.egon.cola.archetype.source.light.application.user.pojo.command.GrantPermissionCommand;
import top.egon.cola.archetype.source.light.application.user.pojo.query.GetUserPermissionsQuery;
import top.egon.cola.archetype.source.light.application.user.pojo.query.GetUserQuery;
import top.egon.cola.archetype.source.light.application.user.pojo.result.PermissionDetailResult;
import top.egon.cola.archetype.source.light.application.user.pojo.result.PermissionResult;
import top.egon.cola.archetype.source.light.application.user.pojo.result.UserResult;
import top.egon.cola.archetype.source.light.common.exception.UserUseCaseException;
import top.egon.cola.archetype.source.light.facade.proto.AssignRoleRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.CreateUserRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.GetUserPermissionsRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.GetUserRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.PermissionListRpcResponse;
import top.egon.cola.archetype.source.light.facade.proto.PermissionRpcResponse;
import top.egon.cola.archetype.source.light.facade.proto.UserRpcResponse;
import top.egon.cola.archetype.source.light.facade.user.dto.PermissionDetailDTO;
import top.egon.cola.archetype.source.light.facade.user.dto.UserDetailDTO;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The single Protobuf chain replaces the former DTO facade plus RPC provider wrapper. */
class UserFacadeImplTest {

    private final LightFacadeConverter converter = Mappers.getMapper(LightFacadeConverter.class);
    private final jakarta.validation.ValidatorFactory validators = Validation.buildDefaultValidatorFactory();
    private final ValidationUtils validation = new ValidationUtils(validators.getValidator());
    private final UserManage userManage = mock(UserManage.class);
    private final RoleManage roleManage = mock(RoleManage.class);
    private final PermissionManage permissionManage = mock(PermissionManage.class);
    private final UserFacadeImpl userFacade =
            new UserFacadeImpl(userManage, roleManage, converter, validation);
    private final PermissionFacadeImpl permissionFacade =
            new PermissionFacadeImpl(permissionManage, converter, validation);

    @AfterEach
    void closeValidationFactory() {
        validators.close();
    }

    @Test
    void converts_create_user_request_into_the_use_case_result() {
        when(userManage.create(any())).thenReturn(new UserResult(1001L, "Mario", "mario@example.com", "ACTIVE"));

        UserRpcResponse response = userFacade.createUser(CreateUserRpcRequest.newBuilder()
                .setExternalId("ext-1")
                .setName("Mario")
                .setEmail("mario@example.com")
                .setOperatorId("operator-1")
                .setRequestId("request-1")
                .build());

        assertThat(response.getSuccess()).isTrue();
        assertThat(converter.toSource(response.getData()))
                .isEqualTo(new UserDetailDTO(1001L, "Mario", "mario@example.com", "ACTIVE"));
        verify(userManage).create(new CreateUserCommand(
                "ext-1", "Mario", "mario@example.com", "operator-1", "request-1"));
    }

    @Test
    void maps_use_case_error_onto_the_wire_code_without_leaking_the_cause() {
        when(userManage.create(any())).thenThrow(new UserUseCaseException(
                "USER_EXISTS", "User already exists", new IllegalStateException("internal")));

        UserRpcResponse response = userFacade.createUser(CreateUserRpcRequest.newBuilder()
                .setExternalId("ext-1")
                .setName("Mario")
                .setEmail("mario@example.com")
                .setOperatorId("operator-1")
                .setRequestId("request-1")
                .build());

        assertThat(response.getSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo("USER_EXISTS");
        assertThat(response.getMessage()).isEqualTo("User already exists");
    }

    @Test
    void delegates_assign_role_to_the_role_use_case() {
        when(roleManage.assignRole(any())).thenReturn(new UserResult(1001L, "Mario", "mario@example.com", "ACTIVE"));

        UserRpcResponse response = userFacade.assignRole(AssignRoleRpcRequest.newBuilder()
                .setUserId(1001L)
                .setRoleCode("STUDENT")
                .setOperatorId("operator-1")
                .setRequestId("request-1")
                .build());

        assertThat(response.getSuccess()).isTrue();
        verify(roleManage).assignRole(new AssignRoleCommand(1001L, "STUDENT", "operator-1", "request-1"));
    }

    @Test
    void delegates_user_query_to_the_use_case() {
        when(userManage.get(any())).thenReturn(new UserResult(1001L, "Mario", "mario@example.com", "ACTIVE"));

        UserRpcResponse response = userFacade.getUser(GetUserRpcRequest.newBuilder().setUserId(1001L).build());

        assertThat(response.getSuccess()).isTrue();
        verify(userManage).get(new GetUserQuery(1001L));
    }

    @Test
    void rejects_an_absent_identifier_before_reaching_the_use_case() {
        assertThatThrownBy(() -> userFacade.getUser(GetUserRpcRequest.newBuilder().build()))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void delegates_grant_permission_to_the_authorization_use_case() {
        when(permissionManage.grantPermission(any()))
                .thenReturn(new PermissionResult("STUDENT", "course:read", "ACTIVE"));

        PermissionRpcResponse response = permissionFacade.grantPermission(
                top.egon.cola.archetype.source.light.facade.proto.GrantPermissionRpcRequest.newBuilder()
                        .setRoleCode("STUDENT")
                        .setPermissionCode("course:read")
                        .setOperatorId("operator-1")
                        .setRequestId("request-1")
                        .build());

        assertThat(response.getSuccess()).isTrue();
        verify(permissionManage).grantPermission(new GrantPermissionCommand(
                "STUDENT", "course:read", "operator-1", "request-1"));
    }

    @Test
    void delegates_permission_query_to_the_authorization_use_case() {
        when(permissionManage.getByUser(any())).thenReturn(List.of(
                new PermissionDetailResult("course:read", "Read courses")));

        PermissionListRpcResponse response = permissionFacade.getUserPermissions(
                GetUserPermissionsRpcRequest.newBuilder().setUserId(1001L).build());

        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getDataList().stream().map(converter::toSource).toList())
                .isEqualTo(List.of(new PermissionDetailDTO("course:read", "Read courses", List.of())));
        verify(permissionManage).getByUser(new GetUserPermissionsQuery(1001L));
    }
}
