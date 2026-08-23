#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.course.facade.impl;

import ${package}.adapter.course.converter.CourseFacadeConverter;
import ${package}.adapter.course.validators.CourseFacadeValidator;
import ${package}.adapter.handler.GlobalFacadeExceptionHandler;
import ${package}.application.course.command.CreateCourseCommand;
import ${package}.application.course.manage.CourseManage;
import ${package}.application.course.query.GetCourseQuery;
import ${package}.application.course.query.PageCourseQuery;
import ${package}.facade.evaluation.v1.Course;
import ${package}.facade.evaluation.v1.CourseSchedule;
import ${package}.facade.evaluation.v1.CreateCourseRequest;
import ${package}.facade.evaluation.v1.DubboCourseServiceTriple;
import ${package}.facade.evaluation.v1.GetCourseRequest;
import ${package}.facade.evaluation.v1.PageCourseResponse;
import ${package}.facade.evaluation.v1.PageCoursesRequest;
import ${package}.facade.evaluation.v1.ScheduleCourseRequest;
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
