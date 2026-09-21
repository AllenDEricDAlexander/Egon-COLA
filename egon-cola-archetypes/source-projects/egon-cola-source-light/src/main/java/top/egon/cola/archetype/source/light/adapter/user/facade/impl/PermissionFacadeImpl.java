package top.egon.cola.archetype.source.light.adapter.user.facade.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.light.adapter.pojo.convertor.LightFacadeConverter;
import top.egon.cola.archetype.source.light.adapter.pojo.dto.RpcIdQuery;
import top.egon.cola.archetype.source.light.facade.validation.NativeRpcValidationGroup;
import top.egon.cola.archetype.source.light.application.user.manage.PermissionManage;
import top.egon.cola.archetype.source.light.application.user.pojo.command.GrantPermissionCommand;
import top.egon.cola.archetype.source.light.application.user.pojo.query.GetUserPermissionsQuery;
import top.egon.cola.archetype.source.light.application.user.pojo.result.PermissionDetailResult;
import top.egon.cola.archetype.source.light.application.user.pojo.result.PermissionResult;
import top.egon.cola.archetype.source.light.common.exception.UserFacadeException;
import top.egon.cola.archetype.source.light.common.exception.UserUseCaseException;
import top.egon.cola.archetype.source.light.facade.proto.GrantPermissionRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.GetUserPermissionsRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.PermissionListRpcResponse;
import top.egon.cola.archetype.source.light.facade.proto.PermissionRpcResponse;
import top.egon.cola.archetype.source.light.facade.user.PermissionFacade;
import top.egon.cola.archetype.source.light.facade.user.dto.GrantPermissionDTO;
import top.egon.cola.archetype.source.light.facade.user.dto.PermissionDTO;
import top.egon.cola.archetype.source.light.facade.user.dto.PermissionDetailDTO;
import top.egon.cola.archetype.source.light.facade.user.utils.UserFacadeAssert;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;

import java.util.List;

/** Native unary provider of the Permission facade; maps Protobuf onto the authorization use cases. */
@EgonRpcProvider
@Component("permissionFacadeImpl")
@RequiredArgsConstructor
@Slf4j
public class PermissionFacadeImpl implements PermissionFacade {

    private static final String EMPTY_RESULT = "FACADE_EMPTY_RESULT";

    @Qualifier("permissionManageImpl")
    private final PermissionManage permissionManage;
    @Qualifier("lightFacadeConverter")
    private final LightFacadeConverter converter;
    @Qualifier("nativeRpcValidation")
    private final ValidationUtils validation;

    @Override
    public PermissionRpcResponse grantPermission(GrantPermissionRpcRequest request) {
        GrantPermissionDTO input = validation.validate(
                converter.toSource(request), NativeRpcValidationGroup.class);
        try {
            return converter.permissionSuccess(grantPermission(input));
        } catch (UserFacadeException exception) {
            log.debug("grantPermission rejected: {}", exception.getStatus());
            return converter.permissionFailure(exception.getStatus(), exception.getMessage(), null);
        }
    }

    @Override
    public PermissionListRpcResponse getUserPermissions(GetUserPermissionsRpcRequest request) {
        RpcIdQuery input = validation.validate(new RpcIdQuery(request.hasUserId() ? request.getUserId() : null));
        try {
            return converter.permissionListResponse(getByUser(input.id()), true, null, null, null);
        } catch (UserFacadeException exception) {
            log.debug("getUserPermissions rejected: {}", exception.getStatus());
            return converter.permissionListResponse(
                    null, false, exception.getStatus(), exception.getMessage(), null);
        }
    }

    private PermissionDTO grantPermission(GrantPermissionDTO request) {
        try {
            PermissionResult result = permissionManage.grantPermission(new GrantPermissionCommand(
                    request.roleCode(), request.permissionCode(), request.operatorId(), request.requestId()));
            PermissionResult checked = UserFacadeAssert.notNull(result, EMPTY_RESULT, "facade returned null");
            return new PermissionDTO(checked.roleCode(), checked.permissionCode(), checked.status());
        } catch (UserUseCaseException exception) {
            throw new UserFacadeException(exception.getStatus(), exception.getMessage(), exception);
        }
    }

    private List<PermissionDetailDTO> getByUser(Long userId) {
        try {
            List<PermissionDetailResult> results = permissionManage.getByUser(new GetUserPermissionsQuery(userId));
            return UserFacadeAssert.notNull(results, EMPTY_RESULT, "facade returned null").stream()
                    .map(permission -> new PermissionDetailDTO(
                            permission.code(), permission.name(), List.of()))
                    .toList();
        } catch (UserUseCaseException exception) {
            throw new UserFacadeException(exception.getStatus(), exception.getMessage(), exception);
        }
    }
}
