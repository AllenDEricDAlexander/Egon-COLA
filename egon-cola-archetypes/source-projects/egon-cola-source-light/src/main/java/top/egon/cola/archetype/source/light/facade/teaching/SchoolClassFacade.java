package top.egon.cola.archetype.source.light.facade.teaching;

import jakarta.validation.constraints.NotNull;
import top.egon.cola.archetype.source.light.facade.proto.CreateSchoolClassRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.GetSchoolClassRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.ScheduleCourseRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.SchoolClassRpcResponse;
import top.egon.cola.archetype.source.light.facade.proto.SchoolClassServiceGrpc;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;

/** Native unary contract for the SchoolClass use cases. */
@EgonRpcService(grpcClass = SchoolClassServiceGrpc.class, group = "teaching", version = "1.0.0", retries = 0)
public interface SchoolClassFacade {

    @EgonRpcMethod(name = "CreateSchoolClass", idempotent = false)
    SchoolClassRpcResponse createSchoolClass(@NotNull CreateSchoolClassRpcRequest request);

    @EgonRpcMethod(name = "ScheduleCourse", idempotent = false)
    SchoolClassRpcResponse scheduleCourse(@NotNull ScheduleCourseRpcRequest request);

    @EgonRpcMethod(name = "GetSchoolClass", idempotent = true)
    SchoolClassRpcResponse getSchoolClass(@NotNull GetSchoolClassRpcRequest request);
}
