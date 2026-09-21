package top.egon.cola.archetype.source.web.facade.user;

import jakarta.validation.constraints.NotNull;
import top.egon.cola.archetype.source.web.facade.proto.AssignRoleRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.RoleServiceGrpc;
import top.egon.cola.archetype.source.web.facade.proto.RpcResponse;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;

/** Native unary contract of the web-owned Role facade. */
@EgonRpcService(grpcClass = RoleServiceGrpc.class, group = "student-management-organization", version = "1.0.0", retries = 0)
public interface RoleFacade {

    @EgonRpcMethod(name = "AssignRole", idempotent = false)
    RpcResponse assignRole(@NotNull AssignRoleRpcRequest request);
}
