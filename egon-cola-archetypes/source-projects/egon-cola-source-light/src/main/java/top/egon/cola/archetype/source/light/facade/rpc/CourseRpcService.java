package top.egon.cola.archetype.source.light.facade.rpc;

import jakarta.validation.constraints.NotNull;
import top.egon.cola.archetype.source.light.facade.rpc.proto.CourseRpcResponse;
import top.egon.cola.archetype.source.light.facade.rpc.proto.CourseServiceGrpc;
import top.egon.cola.archetype.source.light.facade.rpc.proto.CreateCourseRpcRequest;
import top.egon.cola.archetype.source.light.facade.rpc.proto.GetCourseRpcRequest;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;

/** Native unary transport for the existing Course facade. */
@EgonRpcService(grpcClass = CourseServiceGrpc.class, group = "teaching", version = "1.0.0", retries = 0)
public interface CourseRpcService {

    @EgonRpcMethod(name = "CreateCourse", idempotent = false)
    CourseRpcResponse createCourse(@NotNull CreateCourseRpcRequest request);

    @EgonRpcMethod(name = "GetCourse", idempotent = true)
    CourseRpcResponse getCourse(@NotNull GetCourseRpcRequest request);
}
