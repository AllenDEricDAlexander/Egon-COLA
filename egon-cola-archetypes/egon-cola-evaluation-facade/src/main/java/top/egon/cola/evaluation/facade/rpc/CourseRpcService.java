package top.egon.cola.evaluation.facade.rpc;

import jakarta.validation.constraints.NotNull;
import top.egon.cola.evaluation.facade.rpc.proto.CourseRpcResponse;
import top.egon.cola.evaluation.facade.rpc.proto.CourseScheduleRpcResponse;
import top.egon.cola.evaluation.facade.rpc.proto.CourseServiceGrpc;
import top.egon.cola.evaluation.facade.rpc.proto.CreateCourseRpcRequest;
import top.egon.cola.evaluation.facade.rpc.proto.GetCourseRpcRequest;
import top.egon.cola.evaluation.facade.rpc.proto.PageCourseRpcRequest;
import top.egon.cola.evaluation.facade.rpc.proto.PageCourseRpcResponse;
import top.egon.cola.evaluation.facade.rpc.proto.ScheduleCourseRpcRequest;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;

/** Native unary transport for the existing Course facade. */
@EgonRpcService(grpcClass = CourseServiceGrpc.class, group = "course", version = "1.0.0", retries = 0)
public interface CourseRpcService {

    @EgonRpcMethod(name = "CreateCourse", idempotent = false)
    CourseRpcResponse createCourse(@NotNull CreateCourseRpcRequest request);

    @EgonRpcMethod(name = "ScheduleCourse", idempotent = false)
    CourseScheduleRpcResponse scheduleCourse(@NotNull ScheduleCourseRpcRequest request);

    @EgonRpcMethod(name = "GetCourse", idempotent = true)
    CourseRpcResponse getCourse(@NotNull GetCourseRpcRequest request);

    @EgonRpcMethod(name = "PageCourses", idempotent = true)
    PageCourseRpcResponse pageCourses(@NotNull PageCourseRpcRequest request);
}
