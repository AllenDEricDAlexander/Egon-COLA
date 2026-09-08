package top.egon.cola.organization.facade.rpc;

import jakarta.validation.constraints.NotNull;
import top.egon.cola.organization.facade.rpc.proto.CreateGradeRpcRequest;
import top.egon.cola.organization.facade.rpc.proto.GetGradeRpcRequest;
import top.egon.cola.organization.facade.rpc.proto.GradeRpcResponse;
import top.egon.cola.organization.facade.rpc.proto.GradeServiceGrpc;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;

/** Native unary transport for the existing Grade facade. */
@EgonRpcService(grpcClass = GradeServiceGrpc.class, group = "student-management-organization", version = "1.0.0", retries = 0)
public interface GradeRpcService {

    @EgonRpcMethod(name = "CreateGrade", idempotent = false)
    GradeRpcResponse createGrade(@NotNull CreateGradeRpcRequest request);

    @EgonRpcMethod(name = "GetGrade", idempotent = true)
    GradeRpcResponse getGrade(@NotNull GetGradeRpcRequest request);
}
