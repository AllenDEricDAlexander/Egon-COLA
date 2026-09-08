package top.egon.cola.archetype.source.web.adapter.user.rpc;

import top.egon.cola.component.rpc.annotation.EgonRpcProvider;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.organization.facade.rpc.OrganizationRpcConverter;
import top.egon.cola.organization.facade.rpc.RpcIdQuery;
import top.egon.cola.organization.facade.rpc.RpcSchoolClassQuery;
import top.egon.cola.organization.facade.exceptions.OrganizationFacadeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.util.Objects;
import top.egon.cola.organization.facade.rpc.UserRpcService;
import top.egon.cola.organization.facade.user.UserFacade;
import top.egon.cola.organization.facade.rpc.RoleRpcService;
import top.egon.cola.organization.facade.user.RoleFacade;
import top.egon.cola.organization.facade.rpc.PermissionRpcService;
import top.egon.cola.organization.facade.user.PermissionFacade;
import top.egon.cola.organization.facade.rpc.proto.CreateUserRpcRequest;
import top.egon.cola.organization.facade.rpc.proto.UserRpcResponse;
import top.egon.cola.organization.facade.rpc.proto.GetUserRpcRequest;
import top.egon.cola.organization.facade.rpc.proto.AssignRoleRpcRequest;
import top.egon.cola.organization.facade.rpc.proto.RpcResponse;
import top.egon.cola.organization.facade.rpc.proto.GrantPermissionRpcRequest;
import top.egon.cola.organization.facade.rpc.proto.GetPermissionTreeRpcRequest;
import top.egon.cola.organization.facade.rpc.proto.PermissionTreeRpcResponse;

/** Preserves organization facade behavior behind the native unary contracts. */
@Component("userRpcProvider")
@EgonRpcProvider
@RequiredArgsConstructor
@Slf4j
public class UserRpcProvider implements UserRpcService, RoleRpcService, PermissionRpcService {
    @Qualifier("userFacade")
    private final UserFacade userFacade;
    @Qualifier("roleFacade")
    private final RoleFacade roleFacade;
    @Qualifier("permissionFacade")
    private final PermissionFacade permissionFacade;
    @Qualifier("organizationRpcConverter")
    private final OrganizationRpcConverter converter;
    @Qualifier("nativeRpcValidation")
    private final ValidationUtils validation;

    @Override
    public UserRpcResponse createUser(CreateUserRpcRequest request) {
        var input = validation.validate(converter.toSource(request));
        try {
            return converter.userSuccess(Objects.requireNonNull(
                    userFacade.createUser(input), "facade returned null"));
        } catch (OrganizationFacadeException exception) {
            log.debug("createUser rejected: {}", exception.code());
            return converter.userFailure(exception.code(), exception.getMessage(), exception.traceId());
        }
    }

    @Override
    public UserRpcResponse getUser(GetUserRpcRequest request) {
        var input = validation.validate(new RpcIdQuery(request.hasUserId() ? request.getUserId() : null));
        try {
            return converter.userSuccess(Objects.requireNonNull(
                    userFacade.getUser(input.id()), "facade returned null"));
        } catch (OrganizationFacadeException exception) {
            log.debug("getUser rejected: {}", exception.code());
            return converter.userFailure(exception.code(), exception.getMessage(), exception.traceId());
        }
    }

    @Override
    public RpcResponse assignRole(AssignRoleRpcRequest request) {
        var input = validation.validate(converter.toSource(request));
        try {
            roleFacade.assignRole(input);
            return converter.response(true, "SUCCESS", "success", null);
        } catch (OrganizationFacadeException exception) {
            log.debug("assignRole rejected: {}", exception.code());
            return converter.response(false, exception.code(), exception.getMessage(), exception.traceId());
        }
    }

    @Override
    public RpcResponse grantPermission(GrantPermissionRpcRequest request) {
        var input = validation.validate(converter.toSource(request));
        try {
            permissionFacade.grantPermission(input);
            return converter.response(true, "SUCCESS", "success", null);
        } catch (OrganizationFacadeException exception) {
            log.debug("grantPermission rejected: {}", exception.code());
            return converter.response(false, exception.code(), exception.getMessage(), exception.traceId());
        }
    }

    @Override
    public PermissionTreeRpcResponse getPermissionTree(GetPermissionTreeRpcRequest request) {
        var input = validation.validate(new RpcIdQuery(request.hasUserId() ? request.getUserId() : null));
        try {
            return converter.permissionTreeSuccess(Objects.requireNonNull(
                    permissionFacade.getPermissionTree(input.id()), "facade returned null"));
        } catch (OrganizationFacadeException exception) {
            log.debug("getPermissionTree rejected: {}", exception.code());
            return converter.permissionTreeFailure(exception.code(), exception.getMessage(), exception.traceId());
        }
    }
}
