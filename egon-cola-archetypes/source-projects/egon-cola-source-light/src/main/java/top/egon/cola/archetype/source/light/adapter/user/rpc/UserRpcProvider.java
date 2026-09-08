package top.egon.cola.archetype.source.light.adapter.user.rpc;

import top.egon.cola.archetype.source.light.facade.user.UserFacade;
import top.egon.cola.archetype.source.light.facade.user.exceptions.UserFacadeException;
import top.egon.cola.archetype.source.light.facade.rpc.UserRpcService;
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
import top.egon.cola.archetype.source.light.facade.rpc.proto.CreateUserRpcRequest;
import top.egon.cola.archetype.source.light.facade.rpc.proto.UserRpcResponse;
import top.egon.cola.archetype.source.light.facade.rpc.proto.AssignRoleRpcRequest;
import top.egon.cola.archetype.source.light.facade.rpc.proto.GetUserRpcRequest;

/** Native unary adapter for the existing User facade. */
@EgonRpcProvider
@Component("userRpcProvider")
@RequiredArgsConstructor
@Slf4j
public class UserRpcProvider implements UserRpcService {
    @Qualifier("userFacadeImpl")
    private final UserFacade delegate;
    @Qualifier("lightRpcConverter")
    private final LightRpcConverter converter;
    @Qualifier("nativeRpcValidation")
    private final ValidationUtils validation;

    @Override
    public UserRpcResponse createUser(CreateUserRpcRequest request) {
        var input = validation.validate(converter.toSource(request), NativeRpcValidationGroup.class);
        try {
            return converter.userSuccess(Objects.requireNonNull(delegate.createUser(input), "facade returned null"));
        } catch (UserFacadeException exception) {
            log.debug("createUser rejected: {}", exception.getCode());
            return converter.userFailure(exception.getCode(), exception.getMessage(), null);
        }
    }

    @Override
    public UserRpcResponse assignRole(AssignRoleRpcRequest request) {
        var input = validation.validate(converter.toSource(request), NativeRpcValidationGroup.class);
        try {
            return converter.userSuccess(Objects.requireNonNull(delegate.assignRole(input), "facade returned null"));
        } catch (UserFacadeException exception) {
            log.debug("assignRole rejected: {}", exception.getCode());
            return converter.userFailure(exception.getCode(), exception.getMessage(), null);
        }
    }

    @Override
    public UserRpcResponse getUser(GetUserRpcRequest request) {
        var input = validation.validate(new RpcIdQuery(request.hasUserId() ? request.getUserId() : null));
        try {
            return converter.userSuccess(Objects.requireNonNull(delegate.getUser(input.id()), "facade returned null"));
        } catch (UserFacadeException exception) {
            log.debug("getUser rejected: {}", exception.getCode());
            return converter.userFailure(exception.getCode(), exception.getMessage(), null);
        }
    }
}
