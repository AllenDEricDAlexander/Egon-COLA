package top.egon.cola.organization.facade.rpc;

import jakarta.validation.constraints.NotNull;
import top.egon.cola.organization.facade.rpc.proto.CreateUserRpcRequest;
import top.egon.cola.organization.facade.rpc.proto.GetUserRpcRequest;
import top.egon.cola.organization.facade.rpc.proto.UserRpcResponse;
import top.egon.cola.organization.facade.rpc.proto.UserServiceGrpc;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;

/** Native unary transport for the existing User facade. */
@EgonRpcService(grpcClass = UserServiceGrpc.class, group = "student-management-organization", version = "1.0.0", retries = 0)
public interface UserRpcService {

    @EgonRpcMethod(name = "CreateUser", idempotent = false)
    UserRpcResponse createUser(@NotNull CreateUserRpcRequest request);

    @EgonRpcMethod(name = "GetUser", idempotent = true)
    UserRpcResponse getUser(@NotNull GetUserRpcRequest request);
}
