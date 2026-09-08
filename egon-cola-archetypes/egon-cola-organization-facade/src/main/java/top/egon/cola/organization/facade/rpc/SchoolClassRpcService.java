package top.egon.cola.organization.facade.rpc;

import jakarta.validation.constraints.NotNull;
import top.egon.cola.organization.facade.rpc.proto.AssignUserRpcRequest;
import top.egon.cola.organization.facade.rpc.proto.CreateSchoolClassRpcRequest;
import top.egon.cola.organization.facade.rpc.proto.GetSchoolClassRpcRequest;
import top.egon.cola.organization.facade.rpc.proto.RpcResponse;
import top.egon.cola.organization.facade.rpc.proto.SchoolClassRpcResponse;
import top.egon.cola.organization.facade.rpc.proto.SchoolClassServiceGrpc;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;

/** Native unary transport for the existing SchoolClass facade. */
@EgonRpcService(grpcClass = SchoolClassServiceGrpc.class, group = "student-management-organization", version = "1.0.0", retries = 0)
public interface SchoolClassRpcService {

    @EgonRpcMethod(name = "CreateSchoolClass", idempotent = false)
    SchoolClassRpcResponse createSchoolClass(@NotNull CreateSchoolClassRpcRequest request);

    @EgonRpcMethod(name = "GetSchoolClass", idempotent = true)
    SchoolClassRpcResponse getSchoolClass(@NotNull GetSchoolClassRpcRequest request);

    @EgonRpcMethod(name = "AssignUser", idempotent = false)
    RpcResponse assignUser(@NotNull AssignUserRpcRequest request);
}
