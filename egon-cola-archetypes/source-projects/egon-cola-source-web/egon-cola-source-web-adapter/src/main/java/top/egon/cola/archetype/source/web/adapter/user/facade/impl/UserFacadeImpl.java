package top.egon.cola.archetype.source.web.adapter.user.facade.impl;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.web.adapter.facade.impl.OrganizationFacadeSupport;
import top.egon.cola.archetype.source.web.adapter.pojo.convertor.OrganizationFacadeConverter;
import top.egon.cola.archetype.source.web.adapter.pojo.dto.RpcIdQuery;
import top.egon.cola.archetype.source.web.application.user.manage.UserManage;
import top.egon.cola.archetype.source.web.application.user.query.UserDetailQuery;
import top.egon.cola.archetype.source.web.application.user.result.UserDetailResult;
import top.egon.cola.archetype.source.web.facade.proto.CreateUserRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.GetUserRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.UserRpcResponse;
import top.egon.cola.archetype.source.web.facade.user.UserFacade;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;

/** Native unary provider of the web-owned User facade; maps Protobuf onto the use cases. */
@Component("userFacade")
@EgonRpcProvider
@RequiredArgsConstructor
@Slf4j
public class UserFacadeImpl implements UserFacade {

    @Qualifier("userManage")
    private final UserManage userManage;
    @Qualifier("organizationFacadeConverter")
    private final OrganizationFacadeConverter converter;
    @Qualifier("nativeRpcValidation")
    private final ValidationUtils validation;

    @Override
    public UserRpcResponse createUser(CreateUserRpcRequest request) {
        var command = validation.validate(
                converter.createUserCommand(request, OrganizationFacadeSupport.requestId()));
        return OrganizationFacadeSupport.invoke(
                () -> converter.userSuccess(require(userManage.createUser(command))),
                (code, message, traceId) -> reject("createUser", code, message, traceId));
    }

    @Override
    public UserRpcResponse getUser(GetUserRpcRequest request) {
        var input = validation.validate(new RpcIdQuery(request.hasUserId() ? request.getUserId() : null));
        return OrganizationFacadeSupport.invoke(
                () -> converter.userSuccess(require(userManage.getUser(new UserDetailQuery(input.id())))),
                (code, message, traceId) -> reject("getUser", code, message, traceId));
    }

    private static UserDetailResult require(UserDetailResult result) {
        return Objects.requireNonNull(result, "facade returned null");
    }

    private UserRpcResponse reject(String operation, String code, String message, String traceId) {
        log.debug("{} rejected: {}", operation, code);
        return converter.userFailure(code, message, traceId);
    }
}
