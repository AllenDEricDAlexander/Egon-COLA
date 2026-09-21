package top.egon.cola.archetype.source.web.facade.teaching;

import jakarta.validation.constraints.NotNull;
import top.egon.cola.archetype.source.web.facade.proto.CreateGradeRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.GetGradeRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.GradeRpcResponse;
import top.egon.cola.archetype.source.web.facade.proto.GradeServiceGrpc;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;

/** Native unary contract of the web-owned Grade facade. */
@EgonRpcService(grpcClass = GradeServiceGrpc.class, group = "student-management-organization", version = "1.0.0", retries = 0)
public interface GradeFacade {

    @EgonRpcMethod(name = "CreateGrade", idempotent = false)
    GradeRpcResponse createGrade(@NotNull CreateGradeRpcRequest request);

    @EgonRpcMethod(name = "GetGrade", idempotent = true)
    GradeRpcResponse getGrade(@NotNull GetGradeRpcRequest request);
}
