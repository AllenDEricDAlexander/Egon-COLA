package top.egon.cola.archetype.source.serviceopen.adapter.course.facade.impl;

import top.egon.cola.archetype.source.serviceopen.adapter.course.converter.CourseFacadeConverter;
import top.egon.cola.archetype.source.serviceopen.adapter.course.validators.CourseFacadeValidator;
import top.egon.cola.archetype.source.serviceopen.adapter.handler.GlobalFacadeExceptionHandler;
import top.egon.cola.archetype.source.serviceopen.application.course.command.CreateCourseCommand;
import top.egon.cola.archetype.source.serviceopen.application.course.manage.CourseManage;
import top.egon.cola.archetype.source.serviceopen.application.course.query.GetCourseQuery;
import top.egon.cola.archetype.source.serviceopen.application.course.query.PageCourseQuery;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.Course;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.CourseSchedule;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.CreateCourseRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.DubboCourseServiceTriple;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.GetCourseRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.PageCourseResponse;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.PageCoursesRequest;
import top.egon.cola.archetype.source.serviceopen.facade.evaluation.v1.ScheduleCourseRequest;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Qualifier;

@DubboService(version = "1.0.0", group = "course")
@RequiredArgsConstructor
public class CourseFacadeImpl extends DubboCourseServiceTriple.CourseServiceImplBase {

    @Qualifier("courseManage")
    private final CourseManage courseManage;
    private final CourseFacadeConverter converter;
    private final CourseFacadeValidator validator;
    private final GlobalFacadeExceptionHandler exceptionHandler;

    @Override
    public Course createCourse(CreateCourseRequest request) {
        try {
            validator.require(request);
            return converter.toResponse(courseManage.create(
                    new CreateCourseCommand(request.getCode(), request.getName(), request.getCredit())));
        } catch (RuntimeException failure) {
            throw exceptionHandler.toStatus(failure);
        }
    }

    @Override
    public CourseSchedule scheduleCourse(ScheduleCourseRequest request) {
        try {
            validator.require(request);
            return converter.toResponse(courseManage.schedule(converter.toCommand(request)));
        } catch (RuntimeException failure) {
            throw exceptionHandler.toStatus(failure);
        }
    }

    @Override
    public Course getCourse(GetCourseRequest request) {
        try {
            validator.require(request);
            return converter.toResponse(courseManage.get(new GetCourseQuery(request.getCourseId())));
        } catch (RuntimeException failure) {
            throw exceptionHandler.toStatus(failure);
        }
    }

    @Override
    public PageCourseResponse pageCourses(PageCoursesRequest request) {
        try {
            validator.require(request);
            return converter.toPage(courseManage.page(
                    new PageCourseQuery(request.getCurrentPage(), request.getPageSize())));
        } catch (RuntimeException failure) {
            throw exceptionHandler.toStatus(failure);
        }
    }
}
