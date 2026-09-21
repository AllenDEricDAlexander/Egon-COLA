package top.egon.cola.archetype.source.light.facade.user;

import jakarta.validation.constraints.NotNull;
import top.egon.cola.archetype.source.light.facade.proto.GetUserPermissionsRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.GrantPermissionRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.PermissionListRpcResponse;
import top.egon.cola.archetype.source.light.facade.proto.PermissionRpcResponse;
import top.egon.cola.archetype.source.light.facade.proto.PermissionServiceGrpc;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;

/** Native unary contract for the Permission use cases. */
@EgonRpcService(grpcClass = PermissionServiceGrpc.class, group = "user", version = "1.0.0", retries = 0)
public interface PermissionFacade {

    @EgonRpcMethod(name = "GrantPermission", idempotent = false)
    PermissionRpcResponse grantPermission(@NotNull GrantPermissionRpcRequest request);

    @EgonRpcMethod(name = "GetUserPermissions", idempotent = true)
    PermissionListRpcResponse getUserPermissions(@NotNull GetUserPermissionsRpcRequest request);
}
