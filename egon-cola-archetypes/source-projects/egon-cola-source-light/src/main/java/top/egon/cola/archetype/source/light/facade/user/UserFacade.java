package top.egon.cola.archetype.source.light.facade.user;

import jakarta.validation.constraints.NotNull;
import top.egon.cola.archetype.source.light.facade.proto.AssignRoleRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.CreateUserRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.GetUserRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.UserRpcResponse;
import top.egon.cola.archetype.source.light.facade.proto.UserServiceGrpc;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;

/** Native unary contract for the User use cases. */
@EgonRpcService(grpcClass = UserServiceGrpc.class, group = "user", version = "1.0.0", retries = 0)
public interface UserFacade {

    @EgonRpcMethod(name = "CreateUser", idempotent = false)
    UserRpcResponse createUser(@NotNull CreateUserRpcRequest request);

    @EgonRpcMethod(name = "AssignRole", idempotent = false)
    UserRpcResponse assignRole(@NotNull AssignRoleRpcRequest request);

    @EgonRpcMethod(name = "GetUser", idempotent = true)
    UserRpcResponse getUser(@NotNull GetUserRpcRequest request);
}
