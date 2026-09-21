package top.egon.cola.archetype.source.web.facade.teaching;

import jakarta.validation.constraints.NotNull;
import top.egon.cola.archetype.source.web.facade.proto.AssignUserRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.CreateSchoolClassRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.GetSchoolClassRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.RpcResponse;
import top.egon.cola.archetype.source.web.facade.proto.SchoolClassRpcResponse;
import top.egon.cola.archetype.source.web.facade.proto.SchoolClassServiceGrpc;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;

/** Native unary contract of the web-owned SchoolClass facade. */
@EgonRpcService(grpcClass = SchoolClassServiceGrpc.class, group = "student-management-organization", version = "1.0.0", retries = 0)
public interface SchoolClassFacade {

    @EgonRpcMethod(name = "CreateSchoolClass", idempotent = false)
    SchoolClassRpcResponse createSchoolClass(@NotNull CreateSchoolClassRpcRequest request);

    @EgonRpcMethod(name = "GetSchoolClass", idempotent = true)
    SchoolClassRpcResponse getSchoolClass(@NotNull GetSchoolClassRpcRequest request);

    @EgonRpcMethod(name = "AssignUser", idempotent = false)
    RpcResponse assignUser(@NotNull AssignUserRpcRequest request);
}
