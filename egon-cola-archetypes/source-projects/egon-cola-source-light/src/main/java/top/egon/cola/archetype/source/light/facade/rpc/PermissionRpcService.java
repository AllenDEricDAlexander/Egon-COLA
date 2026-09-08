package top.egon.cola.archetype.source.light.facade.rpc;

import jakarta.validation.constraints.NotNull;
import top.egon.cola.archetype.source.light.facade.rpc.proto.GetUserPermissionsRpcRequest;
import top.egon.cola.archetype.source.light.facade.rpc.proto.GrantPermissionRpcRequest;
import top.egon.cola.archetype.source.light.facade.rpc.proto.PermissionListRpcResponse;
import top.egon.cola.archetype.source.light.facade.rpc.proto.PermissionRpcResponse;
import top.egon.cola.archetype.source.light.facade.rpc.proto.PermissionServiceGrpc;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;

/** Native unary transport for the existing Permission facade. */
@EgonRpcService(grpcClass = PermissionServiceGrpc.class, group = "user", version = "1.0.0", retries = 0)
public interface PermissionRpcService {

    @EgonRpcMethod(name = "GrantPermission", idempotent = false)
    PermissionRpcResponse grantPermission(@NotNull GrantPermissionRpcRequest request);

    @EgonRpcMethod(name = "GetUserPermissions", idempotent = true)
    PermissionListRpcResponse getUserPermissions(@NotNull GetUserPermissionsRpcRequest request);
}
