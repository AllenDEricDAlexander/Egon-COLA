package top.egon.cola.archetype.source.light.adapter.user.rpc;

import top.egon.cola.archetype.source.light.facade.user.PermissionFacade;
import top.egon.cola.archetype.source.light.facade.user.exceptions.UserFacadeException;
import top.egon.cola.archetype.source.light.facade.rpc.PermissionRpcService;
import top.egon.cola.archetype.source.light.facade.rpc.LightRpcConverter;
import top.egon.cola.archetype.source.light.facade.rpc.NativeRpcValidationGroup;
import top.egon.cola.archetype.source.light.facade.rpc.RpcIdQuery;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.util.Objects;
import top.egon.cola.archetype.source.light.facade.rpc.proto.GrantPermissionRpcRequest;
import top.egon.cola.archetype.source.light.facade.rpc.proto.PermissionRpcResponse;
import top.egon.cola.archetype.source.light.facade.rpc.proto.GetUserPermissionsRpcRequest;
import top.egon.cola.archetype.source.light.facade.rpc.proto.PermissionListRpcResponse;

/** Native unary adapter for the existing Permission facade. */
@EgonRpcProvider
@Component("permissionRpcProvider")
@RequiredArgsConstructor
@Slf4j
public class PermissionRpcProvider implements PermissionRpcService {
    @Qualifier("permissionFacadeImpl")
    private final PermissionFacade delegate;
    @Qualifier("lightRpcConverter")
    private final LightRpcConverter converter;
    @Qualifier("nativeRpcValidation")
    private final ValidationUtils validation;

    @Override
    public PermissionRpcResponse grantPermission(GrantPermissionRpcRequest request) {
        var input = validation.validate(converter.toSource(request), NativeRpcValidationGroup.class);
        try {
            return converter.permissionSuccess(Objects.requireNonNull(delegate.grantPermission(input), "facade returned null"));
        } catch (UserFacadeException exception) {
            log.debug("grantPermission rejected: {}", exception.getCode());
            return converter.permissionFailure(exception.getCode(), exception.getMessage(), null);
        }
    }

    @Override
    public PermissionListRpcResponse getUserPermissions(GetUserPermissionsRpcRequest request) {
        var input = validation.validate(new RpcIdQuery(request.hasUserId() ? request.getUserId() : null));
        try {
            return converter.permissionListResponse(delegate.getUserPermissions(input.id()), true, null, null, null);
        } catch (UserFacadeException exception) {
            log.debug("getUserPermissions rejected: {}", exception.getCode());
            return converter.permissionListResponse(null, false, exception.getCode(), exception.getMessage(), null);
        }
    }
}
