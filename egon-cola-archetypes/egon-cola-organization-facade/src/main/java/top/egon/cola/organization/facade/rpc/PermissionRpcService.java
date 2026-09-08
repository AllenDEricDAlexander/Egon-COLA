package top.egon.cola.organization.facade.rpc;

import jakarta.validation.constraints.NotNull;
import top.egon.cola.organization.facade.rpc.proto.GetPermissionTreeRpcRequest;
import top.egon.cola.organization.facade.rpc.proto.GrantPermissionRpcRequest;
import top.egon.cola.organization.facade.rpc.proto.PermissionServiceGrpc;
import top.egon.cola.organization.facade.rpc.proto.PermissionTreeRpcResponse;
import top.egon.cola.organization.facade.rpc.proto.RpcResponse;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;

/** Native unary transport for the existing Permission facade. */
@EgonRpcService(grpcClass = PermissionServiceGrpc.class, group = "student-management-organization", version = "1.0.0", retries = 0)
public interface PermissionRpcService {

    @EgonRpcMethod(name = "GrantPermission", idempotent = false)
    RpcResponse grantPermission(@NotNull GrantPermissionRpcRequest request);

    @EgonRpcMethod(name = "GetPermissionTree", idempotent = true)
    PermissionTreeRpcResponse getPermissionTree(@NotNull GetPermissionTreeRpcRequest request);
}
