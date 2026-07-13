#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.facade.impl.course;

import ${package}.adapter.converter.course.CourseFacadeConverter;
import ${package}.adapter.handler.GlobalFacadeExceptionHandler;
import ${package}.adapter.validators.course.CourseFacadeValidator;
import ${package}.application.course.command.CreateCourseCommand;
import ${package}.application.course.manage.CourseManage;
import ${package}.application.course.query.GetCourseQuery;
import ${package}.application.course.query.PageCourseQuery;
import ${package}.facade.course.CourseFacade;
import ${package}.facade.dto.PageResponse;
import ${package}.facade.dto.SingleResponse;
import ${package}.facade.course.dto.CourseResponse;
import ${package}.facade.course.dto.CourseScheduleResponse;
import ${package}.facade.course.dto.CreateCourseRequest;
import ${package}.facade.course.dto.GetCourseRequest;
import ${package}.facade.course.dto.PageCourseRequest;
import ${package}.facade.course.dto.ScheduleCourseRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;

@DubboService(interfaceClass = CourseFacade.class, version = "1.0.0", group = "course")
@Validated
@RequiredArgsConstructor
public class CourseFacadeImpl implements CourseFacade {
    @Qualifier("courseManage") private final CourseManage courseManage;
    private final CourseFacadeConverter converter;
    private final CourseFacadeValidator validator;
    private final GlobalFacadeExceptionHandler exceptionHandler;

    public SingleResponse<CourseResponse> create(CreateCourseRequest request) {
        try { validator.require(request); return SingleResponse.of(converter.toResponse(
                courseManage.create(new CreateCourseCommand(request.code(), request.name(), request.credit())))); }
        catch (RuntimeException failure) { return exceptionHandler.toFailure(failure); }
    }
    public SingleResponse<CourseScheduleResponse> scheduleCourse(ScheduleCourseRequest request) {
        try { validator.require(request); return SingleResponse.of(converter.toResponse(
                courseManage.schedule(converter.toCommand(request)))); }
        catch (RuntimeException failure) { return exceptionHandler.toFailure(failure); }
    }
    public SingleResponse<CourseResponse> getCourse(GetCourseRequest request) {
        try { validator.require(request); return SingleResponse.of(converter.toResponse(
                courseManage.get(new GetCourseQuery(request.courseId())))); }
        catch (RuntimeException failure) { return exceptionHandler.toFailure(failure); }
    }
    public SingleResponse<PageResponse<CourseResponse>> pageCourses(PageCourseRequest request) {
        try {
            validator.require(request);
            var page = courseManage.page(new PageCourseQuery(request.currentPage(), request.pageSize()));
            List<CourseResponse> records = page.records().stream().map(converter::toResponse).toList();
            return SingleResponse.of(PageResponse.of(records, page.currentPage(), page.totalPages(), page.pageSize(), page.totalCount()));
        } catch (RuntimeException failure) { return exceptionHandler.toFailure(failure); }
    }
}
