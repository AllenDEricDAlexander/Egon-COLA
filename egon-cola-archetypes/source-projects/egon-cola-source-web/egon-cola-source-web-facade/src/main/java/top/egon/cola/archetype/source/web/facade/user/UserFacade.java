package top.egon.cola.archetype.source.web.facade.user;

import jakarta.validation.constraints.NotNull;
import top.egon.cola.archetype.source.web.facade.proto.CreateUserRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.GetUserRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.UserRpcResponse;
import top.egon.cola.archetype.source.web.facade.proto.UserServiceGrpc;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;

/** Native unary contract of the web-owned User facade. */
@EgonRpcService(grpcClass = UserServiceGrpc.class, group = "student-management-organization", version = "1.0.0", retries = 0)
public interface UserFacade {

    @EgonRpcMethod(name = "CreateUser", idempotent = false)
    UserRpcResponse createUser(@NotNull CreateUserRpcRequest request);

    @EgonRpcMethod(name = "GetUser", idempotent = true)
    UserRpcResponse getUser(@NotNull GetUserRpcRequest request);
}
