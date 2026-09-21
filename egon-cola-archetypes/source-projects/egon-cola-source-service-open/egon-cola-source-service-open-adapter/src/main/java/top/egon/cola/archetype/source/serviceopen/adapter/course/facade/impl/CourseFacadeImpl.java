package top.egon.cola.archetype.source.serviceopen.adapter.course.facade.impl;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.archetype.source.serviceopen.adapter.course.pojo.convertor.CourseFacadeConverter;
import top.egon.cola.archetype.source.serviceopen.adapter.course.validators.CourseFacadeValidator;
import top.egon.cola.archetype.source.serviceopen.adapter.handler.GlobalFacadeExceptionHandler;
import top.egon.cola.archetype.source.serviceopen.application.course.manage.CourseManage;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.Course;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.CourseSchedule;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.CreateCourseRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.DubboCourseServiceTriple;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.GetCourseRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.PageCourseResponse;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.PageCoursesRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.ScheduleCourseRequest;

/** Dubbo Triple provider of the open Course facade; maps Protobuf onto the use cases. */
@DubboService(version = "1.0.0", group = "course")
@RequiredArgsConstructor
@Slf4j
public class CourseFacadeImpl extends DubboCourseServiceTriple.CourseServiceImplBase {

    @Qualifier("courseManage")
    private final CourseManage courseManage;
    @Qualifier("courseFacadeConverterImpl")
    private final CourseFacadeConverter converter;
    @Qualifier("courseFacadeValidator")
    private final CourseFacadeValidator validator;
    @Qualifier("globalFacadeExceptionHandler")
    private final GlobalFacadeExceptionHandler exceptionHandler;

    @Override
    public Course createCourse(CreateCourseRequest request) {
        try {
            var input = validator.validateCarrier(converter.toCommand(request));
            return converter.toTarget(require(courseManage.create(input)));
        } catch (RuntimeException failure) {
            throw exceptionHandler.toStatus(failure);
        }
    }

    @Override
    public CourseSchedule scheduleCourse(ScheduleCourseRequest request) {
        try {
            var input = validator.validateCarrier(converter.toCommand(request));
            validator.require(request);
            return converter.toSchedule(require(courseManage.schedule(input)));
        } catch (RuntimeException failure) {
            throw exceptionHandler.toStatus(failure);
        }
    }

    @Override
    public Course getCourse(GetCourseRequest request) {
        try {
            var input = validator.validateCarrier(converter.toQuery(request));
            return converter.toTarget(require(courseManage.get(input)));
        } catch (RuntimeException failure) {
            throw exceptionHandler.toStatus(failure);
        }
    }

    @Override
    public PageCourseResponse pageCourses(PageCoursesRequest request) {
        try {
            var input = validator.validateCarrier(converter.toQuery(request));
            return converter.toPage(require(courseManage.page(input)));
        } catch (RuntimeException failure) {
            throw exceptionHandler.toStatus(failure);
        }
    }

    private static <T> T require(T result) {
        return Objects.requireNonNull(result, "result");
    }
}
