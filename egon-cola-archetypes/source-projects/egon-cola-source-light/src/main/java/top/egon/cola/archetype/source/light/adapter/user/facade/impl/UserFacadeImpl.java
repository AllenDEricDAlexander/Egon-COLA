package top.egon.cola.archetype.source.light.adapter.user.facade.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.light.adapter.pojo.convertor.LightFacadeConverter;
import top.egon.cola.archetype.source.light.adapter.pojo.dto.RpcIdQuery;
import top.egon.cola.archetype.source.light.facade.validation.NativeRpcValidationGroup;
import top.egon.cola.archetype.source.light.application.user.manage.RoleManage;
import top.egon.cola.archetype.source.light.application.user.manage.UserManage;
import top.egon.cola.archetype.source.light.application.user.pojo.command.AssignRoleCommand;
import top.egon.cola.archetype.source.light.application.user.pojo.command.CreateUserCommand;
import top.egon.cola.archetype.source.light.application.user.pojo.query.GetUserQuery;
import top.egon.cola.archetype.source.light.application.user.pojo.result.UserResult;
import top.egon.cola.archetype.source.light.common.exception.UserFacadeException;
import top.egon.cola.archetype.source.light.common.exception.UserUseCaseException;
import top.egon.cola.archetype.source.light.facade.proto.AssignRoleRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.CreateUserRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.GetUserRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.UserRpcResponse;
import top.egon.cola.archetype.source.light.facade.user.UserFacade;
import top.egon.cola.archetype.source.light.facade.user.dto.AssignRoleDTO;
import top.egon.cola.archetype.source.light.facade.user.dto.CreateUserDTO;
import top.egon.cola.archetype.source.light.facade.user.dto.UserDetailDTO;
import top.egon.cola.archetype.source.light.facade.user.utils.UserFacadeAssert;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;

/** Native unary provider of the User facade; maps Protobuf onto the user use cases. */
@EgonRpcProvider
@Component("userFacadeImpl")
@RequiredArgsConstructor
@Slf4j
public class UserFacadeImpl implements UserFacade {

    private static final String EMPTY_RESULT = "FACADE_EMPTY_RESULT";

    @Qualifier("userManageImpl")
    private final UserManage userManage;
    @Qualifier("roleManageImpl")
    private final RoleManage roleManage;
    @Qualifier("lightFacadeConverter")
    private final LightFacadeConverter converter;
    @Qualifier("nativeRpcValidation")
    private final ValidationUtils validation;

    @Override
    public UserRpcResponse createUser(CreateUserRpcRequest request) {
        CreateUserDTO input = validation.validate(converter.toSource(request), NativeRpcValidationGroup.class);
        try {
            return converter.userSuccess(create(input));
        } catch (UserFacadeException exception) {
            log.debug("createUser rejected: {}", exception.getStatus());
            return converter.userFailure(exception.getStatus(), exception.getMessage(), null);
        }
    }

    @Override
    public UserRpcResponse assignRole(AssignRoleRpcRequest request) {
        AssignRoleDTO input = validation.validate(converter.toSource(request), NativeRpcValidationGroup.class);
        try {
            return converter.userSuccess(assignRole(input));
        } catch (UserFacadeException exception) {
            log.debug("assignRole rejected: {}", exception.getStatus());
            return converter.userFailure(exception.getStatus(), exception.getMessage(), null);
        }
    }

    @Override
    public UserRpcResponse getUser(GetUserRpcRequest request) {
        RpcIdQuery input = validation.validate(new RpcIdQuery(request.hasUserId() ? request.getUserId() : null));
        try {
            return converter.userSuccess(get(input.id()));
        } catch (UserFacadeException exception) {
            log.debug("getUser rejected: {}", exception.getStatus());
            return converter.userFailure(exception.getStatus(), exception.getMessage(), null);
        }
    }

    private UserDetailDTO create(CreateUserDTO request) {
        try {
            return toDto(require(userManage.create(new CreateUserCommand(
                    request.externalId(), request.name(), request.email(),
                    request.operatorId(), request.requestId()))));
        } catch (UserUseCaseException exception) {
            throw new UserFacadeException(exception.getStatus(), exception.getMessage(), exception);
        }
    }

    private UserDetailDTO assignRole(AssignRoleDTO request) {
        try {
            return toDto(require(roleManage.assignRole(new AssignRoleCommand(
                    request.userId(), request.roleCode(), request.operatorId(), request.requestId()))));
        } catch (UserUseCaseException exception) {
            throw new UserFacadeException(exception.getStatus(), exception.getMessage(), exception);
        }
    }

    private UserDetailDTO get(Long userId) {
        try {
            return toDto(require(userManage.get(new GetUserQuery(userId))));
        } catch (UserUseCaseException exception) {
            throw new UserFacadeException(exception.getStatus(), exception.getMessage(), exception);
        }
    }

    private static UserResult require(UserResult result) {
        return UserFacadeAssert.notNull(result, EMPTY_RESULT, "facade returned null");
    }

    private static UserDetailDTO toDto(UserResult result) {
        return new UserDetailDTO(result.id(), result.name(), result.email(), result.status());
    }
}
