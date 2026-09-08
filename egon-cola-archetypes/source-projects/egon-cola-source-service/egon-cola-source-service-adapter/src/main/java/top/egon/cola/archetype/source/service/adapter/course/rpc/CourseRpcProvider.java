package top.egon.cola.archetype.source.service.adapter.course.rpc;

import top.egon.cola.component.rpc.annotation.EgonRpcProvider;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.evaluation.facade.rpc.EvaluationRpcConverter;
import top.egon.cola.evaluation.facade.rpc.CourseRpcService;
import top.egon.cola.evaluation.facade.course.CourseFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.util.Objects;
import top.egon.cola.evaluation.facade.rpc.proto.CreateCourseRpcRequest;
import top.egon.cola.evaluation.facade.rpc.proto.CourseRpcResponse;
import top.egon.cola.evaluation.facade.rpc.proto.ScheduleCourseRpcRequest;
import top.egon.cola.evaluation.facade.rpc.proto.CourseScheduleRpcResponse;
import top.egon.cola.evaluation.facade.rpc.proto.GetCourseRpcRequest;
import top.egon.cola.evaluation.facade.rpc.proto.PageCourseRpcRequest;
import top.egon.cola.evaluation.facade.rpc.proto.PageCourseRpcResponse;

/** Adapts the existing Course facade to the native unary contract. */
@Component("courseRpcProvider")
@EgonRpcProvider
@RequiredArgsConstructor
@Slf4j
public class CourseRpcProvider implements CourseRpcService {
    @Qualifier("courseFacadeImpl")
    private final CourseFacade delegate;
    @Qualifier("evaluationRpcConverter")
    private final EvaluationRpcConverter converter;
    @Qualifier("nativeRpcValidation")
    private final ValidationUtils validation;

    @Override
    public CourseRpcResponse createCourse(CreateCourseRpcRequest request) {
        var input = validation.validate(converter.toSource(request));
        var result = Objects.requireNonNull(delegate.create(input), "facade returned null");
        if (!result.isSuccess()) {
            log.debug("createCourse rejected: {}", result.getCode());
        }
        return converter.toCourseRpcResponse(result);
    }

    @Override
    public CourseScheduleRpcResponse scheduleCourse(ScheduleCourseRpcRequest request) {
        var input = validation.validate(converter.toSource(request));
        var result = Objects.requireNonNull(delegate.scheduleCourse(input), "facade returned null");
        if (!result.isSuccess()) {
            log.debug("scheduleCourse rejected: {}", result.getCode());
        }
        return converter.toCourseScheduleRpcResponse(result);
    }

    @Override
    public CourseRpcResponse getCourse(GetCourseRpcRequest request) {
        var input = validation.validate(converter.toSource(request));
        var result = Objects.requireNonNull(delegate.getCourse(input), "facade returned null");
        if (!result.isSuccess()) {
            log.debug("getCourse rejected: {}", result.getCode());
        }
        return converter.toCourseRpcResponse(result);
    }

    @Override
    public PageCourseRpcResponse pageCourses(PageCourseRpcRequest request) {
        var input = validation.validate(converter.toSource(request));
        var result = Objects.requireNonNull(delegate.pageCourses(input), "facade returned null");
        if (!result.isSuccess()) {
            log.debug("pageCourses rejected: {}", result.getCode());
        }
        return converter.toPageCourseRpcResponse(result);
    }
}
