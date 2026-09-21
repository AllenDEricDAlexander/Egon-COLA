package top.egon.cola.archetype.source.web.facade.user;

import jakarta.validation.constraints.NotNull;
import top.egon.cola.archetype.source.web.facade.proto.GetPermissionTreeRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.GrantPermissionRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.PermissionServiceGrpc;
import top.egon.cola.archetype.source.web.facade.proto.PermissionTreeRpcResponse;
import top.egon.cola.archetype.source.web.facade.proto.RpcResponse;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;

/** Native unary contract of the web-owned Permission facade. */
@EgonRpcService(grpcClass = PermissionServiceGrpc.class, group = "student-management-organization", version = "1.0.0", retries = 0)
public interface PermissionFacade {

    @EgonRpcMethod(name = "GrantPermission", idempotent = false)
    RpcResponse grantPermission(@NotNull GrantPermissionRpcRequest request);

    @EgonRpcMethod(name = "GetPermissionTree", idempotent = true)
    PermissionTreeRpcResponse getPermissionTree(@NotNull GetPermissionTreeRpcRequest request);
}
