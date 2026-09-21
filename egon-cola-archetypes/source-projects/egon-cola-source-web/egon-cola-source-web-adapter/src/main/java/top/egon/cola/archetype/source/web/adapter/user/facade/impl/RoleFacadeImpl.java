package top.egon.cola.archetype.source.web.adapter.user.facade.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import top.egon.cola.archetype.source.web.adapter.facade.impl.OrganizationFacadeSupport;
import top.egon.cola.archetype.source.web.adapter.pojo.convertor.OrganizationFacadeConverter;
import top.egon.cola.archetype.source.web.application.user.manage.RoleManage;
import top.egon.cola.archetype.source.web.facade.proto.AssignRoleRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.RpcResponse;
import top.egon.cola.archetype.source.web.facade.user.RoleFacade;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;

/** Native unary provider of the web-owned Role facade; maps Protobuf onto the use cases. */
@Service("roleFacade")
@EgonRpcProvider
@RequiredArgsConstructor
@Slf4j
public class RoleFacadeImpl implements RoleFacade {

    @Qualifier("roleManage")
    private final RoleManage roleManage;
    @Qualifier("organizationFacadeConverter")
    private final OrganizationFacadeConverter converter;
    @Qualifier("nativeRpcValidation")
    private final ValidationUtils validation;

    @Override
    public RpcResponse assignRole(AssignRoleRpcRequest request) {
        var command = validation.validate(
                converter.assignRoleCommand(request, OrganizationFacadeSupport.requestId()));
        return OrganizationFacadeSupport.invoke(
                () -> {
                    roleManage.assignRole(command);
                    return converter.response(true, "SUCCESS", "success", null);
                },
                (code, message, traceId) -> reject("assignRole", code, message, traceId));
    }

    private RpcResponse reject(String operation, String code, String message, String traceId) {
        log.debug("{} rejected: {}", operation, code);
        return converter.response(false, code, message, traceId);
    }
}
