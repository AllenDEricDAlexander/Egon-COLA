package top.egon.cola.archetype.source.service.facade.course;

import jakarta.validation.constraints.NotNull;
import top.egon.cola.archetype.source.service.facade.proto.CourseRpcResponse;
import top.egon.cola.archetype.source.service.facade.proto.CourseScheduleRpcResponse;
import top.egon.cola.archetype.source.service.facade.proto.CourseServiceGrpc;
import top.egon.cola.archetype.source.service.facade.proto.CreateCourseRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.GetCourseRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.PageCourseRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.PageCourseRpcResponse;
import top.egon.cola.archetype.source.service.facade.proto.ScheduleCourseRpcRequest;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;

/** Native unary contract of the service-owned Course facade. */
@EgonRpcService(grpcClass = CourseServiceGrpc.class, group = "course", version = "1.0.0", retries = 0)
public interface CourseFacade {

    @EgonRpcMethod(name = "CreateCourse", idempotent = false)
    CourseRpcResponse createCourse(@NotNull CreateCourseRpcRequest request);

    @EgonRpcMethod(name = "ScheduleCourse", idempotent = false)
    CourseScheduleRpcResponse scheduleCourse(@NotNull ScheduleCourseRpcRequest request);

    @EgonRpcMethod(name = "GetCourse", idempotent = true)
    CourseRpcResponse getCourse(@NotNull GetCourseRpcRequest request);

    @EgonRpcMethod(name = "PageCourses", idempotent = true)
    PageCourseRpcResponse pageCourses(@NotNull PageCourseRpcRequest request);
}
