package top.egon.cola.archetype.source.service.adapter.course.facade.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.service.adapter.handler.GlobalFacadeExceptionHandler;
import top.egon.cola.archetype.source.service.adapter.pojo.convertor.EvaluationFacadeConverter;
import top.egon.cola.archetype.source.service.adapter.pojo.dto.FacadeFailureDTO;
import top.egon.cola.archetype.source.service.application.course.manage.CourseManage;
import top.egon.cola.archetype.source.service.facade.course.CourseFacade;
import top.egon.cola.archetype.source.service.facade.proto.CourseRpcResponse;
import top.egon.cola.archetype.source.service.facade.proto.CourseScheduleRpcResponse;
import top.egon.cola.archetype.source.service.facade.proto.CreateCourseRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.GetCourseRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.PageCourseRpcRequest;
import top.egon.cola.archetype.source.service.facade.proto.PageCourseRpcResponse;
import top.egon.cola.archetype.source.service.facade.proto.ScheduleCourseRpcRequest;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;

import java.util.Objects;

/** Native unary provider of the service-owned Course facade; maps Protobuf onto the use cases. */
@Component("courseFacadeImpl")
@EgonRpcProvider
@RequiredArgsConstructor
@Slf4j
public class CourseFacadeImpl implements CourseFacade {

    @Qualifier("courseManage")
    private final CourseManage courseManage;
    @Qualifier("evaluationFacadeConverter")
    private final EvaluationFacadeConverter converter;
    @Qualifier("nativeRpcValidation")
    private final ValidationUtils validation;
    @Qualifier("globalFacadeExceptionHandler")
    private final GlobalFacadeExceptionHandler exceptionHandler;

    @Override
    public CourseRpcResponse createCourse(CreateCourseRpcRequest request) {
        var input = validation.validate(converter.toSource(request));
        try {
            return converter.courseSuccess(require(courseManage.create(input)));
        } catch (RuntimeException failure) {
            FacadeFailureDTO rejection = reject("createCourse", failure);
            return converter.courseFailure(rejection.code(), rejection.message(), null);
        }
    }

    @Override
    public CourseScheduleRpcResponse scheduleCourse(ScheduleCourseRpcRequest request) {
        var input = validation.validate(converter.toSource(request));
        try {
            return converter.courseScheduleSuccess(require(courseManage.schedule(input)));
        } catch (RuntimeException failure) {
            FacadeFailureDTO rejection = reject("scheduleCourse", failure);
            return converter.courseScheduleFailure(rejection.code(), rejection.message(), null);
        }
    }

    @Override
    public CourseRpcResponse getCourse(GetCourseRpcRequest request) {
        var input = validation.validate(converter.toSource(request));
        try {
            return converter.courseSuccess(require(courseManage.get(input)));
        } catch (RuntimeException failure) {
            FacadeFailureDTO rejection = reject("getCourse", failure);
            return converter.courseFailure(rejection.code(), rejection.message(), null);
        }
    }

    @Override
    public PageCourseRpcResponse pageCourses(PageCourseRpcRequest request) {
        var input = validation.validate(converter.toSource(request));
        try {
            return converter.pageCourseSuccess(require(courseManage.page(input)));
        } catch (RuntimeException failure) {
            FacadeFailureDTO rejection = reject("pageCourses", failure);
            return converter.pageCourseFailure(rejection.code(), rejection.message(), null);
        }
    }

    private static <T> T require(T result) {
        return Objects.requireNonNull(result, "facade returned null");
    }

    private FacadeFailureDTO reject(String operation, RuntimeException failure) {
        FacadeFailureDTO rejection = exceptionHandler.toFailure(failure);
        log.debug("{} rejected: {}", operation, rejection.code());
        return rejection;
    }
}
