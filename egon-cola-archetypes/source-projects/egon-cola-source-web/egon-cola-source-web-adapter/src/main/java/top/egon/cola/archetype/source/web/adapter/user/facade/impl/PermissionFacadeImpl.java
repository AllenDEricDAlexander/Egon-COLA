package top.egon.cola.archetype.source.web.adapter.user.facade.impl;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import top.egon.cola.archetype.source.web.adapter.facade.impl.OrganizationFacadeSupport;
import top.egon.cola.archetype.source.web.adapter.pojo.convertor.OrganizationFacadeConverter;
import top.egon.cola.archetype.source.web.adapter.pojo.dto.RpcIdQuery;
import top.egon.cola.archetype.source.web.application.user.manage.PermissionManage;
import top.egon.cola.archetype.source.web.application.user.query.PermissionTreeQuery;
import top.egon.cola.archetype.source.web.application.user.result.PermissionTreeResult;
import top.egon.cola.archetype.source.web.facade.proto.GetPermissionTreeRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.GrantPermissionRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.PermissionTreeRpcResponse;
import top.egon.cola.archetype.source.web.facade.proto.RpcResponse;
import top.egon.cola.archetype.source.web.facade.user.PermissionFacade;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;

/** Native unary provider of the web-owned Permission facade; maps Protobuf onto the use cases. */
@Service("permissionFacade")
@EgonRpcProvider
@RequiredArgsConstructor
@Slf4j
public class PermissionFacadeImpl implements PermissionFacade {

    @Qualifier("permissionManage")
    private final PermissionManage permissionManage;
    @Qualifier("organizationFacadeConverter")
    private final OrganizationFacadeConverter converter;
    @Qualifier("nativeRpcValidation")
    private final ValidationUtils validation;

    @Override
    public RpcResponse grantPermission(GrantPermissionRpcRequest request) {
        var command = validation.validate(
                converter.grantPermissionCommand(request, OrganizationFacadeSupport.requestId()));
        return OrganizationFacadeSupport.invoke(
                () -> {
                    permissionManage.grantPermission(command);
                    return converter.response(true, "SUCCESS", "success", null);
                },
                (code, message, traceId) -> reject("grantPermission", code, message, traceId));
    }

    @Override
    public PermissionTreeRpcResponse getPermissionTree(GetPermissionTreeRpcRequest request) {
        var input = validation.validate(new RpcIdQuery(request.hasUserId() ? request.getUserId() : null));
        return OrganizationFacadeSupport.invoke(
                () -> converter.permissionTreeSuccess(require(
                        permissionManage.getPermissionTree(new PermissionTreeQuery(input.id())))),
                (code, message, traceId) -> rejectTree("getPermissionTree", code, message, traceId));
    }

    private static PermissionTreeResult require(PermissionTreeResult result) {
        return Objects.requireNonNull(result, "facade returned null");
    }

    private RpcResponse reject(String operation, String code, String message, String traceId) {
        log.debug("{} rejected: {}", operation, code);
        return converter.response(false, code, message, traceId);
    }

    private PermissionTreeRpcResponse rejectTree(String operation, String code, String message, String traceId) {
        log.debug("{} rejected: {}", operation, code);
        return converter.permissionTreeFailure(code, message, traceId);
    }
}
