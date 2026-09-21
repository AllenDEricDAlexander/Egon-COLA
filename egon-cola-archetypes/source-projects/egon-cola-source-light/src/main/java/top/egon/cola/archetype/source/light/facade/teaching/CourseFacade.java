package top.egon.cola.archetype.source.light.facade.teaching;

import jakarta.validation.constraints.NotNull;
import top.egon.cola.archetype.source.light.facade.proto.CourseRpcResponse;
import top.egon.cola.archetype.source.light.facade.proto.CourseServiceGrpc;
import top.egon.cola.archetype.source.light.facade.proto.CreateCourseRpcRequest;
import top.egon.cola.archetype.source.light.facade.proto.GetCourseRpcRequest;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;

/** Native unary contract for the Course use cases. */
@EgonRpcService(grpcClass = CourseServiceGrpc.class, group = "teaching", version = "1.0.0", retries = 0)
public interface CourseFacade {

    @EgonRpcMethod(name = "CreateCourse", idempotent = false)
    CourseRpcResponse createCourse(@NotNull CreateCourseRpcRequest request);

    @EgonRpcMethod(name = "GetCourse", idempotent = true)
    CourseRpcResponse getCourse(@NotNull GetCourseRpcRequest request);
}
