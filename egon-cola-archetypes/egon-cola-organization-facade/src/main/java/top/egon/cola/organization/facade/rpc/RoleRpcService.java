package top.egon.cola.organization.facade.rpc;

import jakarta.validation.constraints.NotNull;
import top.egon.cola.organization.facade.rpc.proto.AssignRoleRpcRequest;
import top.egon.cola.organization.facade.rpc.proto.RoleServiceGrpc;
import top.egon.cola.organization.facade.rpc.proto.RpcResponse;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;

/** Native unary transport for the existing Role facade. */
@EgonRpcService(grpcClass = RoleServiceGrpc.class, group = "student-management-organization", version = "1.0.0", retries = 0)
public interface RoleRpcService {

    @EgonRpcMethod(name = "AssignRole", idempotent = false)
    RpcResponse assignRole(@NotNull AssignRoleRpcRequest request);
}
