#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.facade.impl.course;

import ${package}.adapter.converter.course.CourseFacadeConverter;
import ${package}.adapter.convertor.CourseAdapterConvertor;
import ${package}.adapter.handler.GlobalFacadeExceptionHandler;
import ${package}.adapter.handler.ServiceExceptionHandler;
import ${package}.adapter.validation.ValidatorUtils;
import ${package}.adapter.validators.course.CourseFacadeValidator;
import ${package}.application.command.course.CreateCourseCommand;
import ${package}.application.manage.course.CourseManage;
import ${package}.application.query.course.GetCourseQuery;
import ${package}.application.query.course.PageCourseQuery;
import ${package}.application.result.course.CourseResult;
import ${package}.common.exception.BizException;
import ${package}.domain.entities.course.Course;
import ${package}.facade.api.CourseFacade;
import ${package}.facade.dto.PageResponse;
import ${package}.facade.dto.SingleResponse;
import ${package}.facade.dto.course.CourseDTO;
import ${package}.facade.dto.course.CourseResponse;
import ${package}.facade.dto.course.CourseScheduleResponse;
import ${package}.facade.dto.course.CreateCourseRequest;
import ${package}.facade.dto.course.GetCourseRequest;
import ${package}.facade.dto.course.PageCourseRequest;
import ${package}.facade.dto.course.ScheduleCourseRequest;
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
    @Qualifier("courseAdapterConvertor") private final CourseAdapterConvertor legacyConverter;
    @Qualifier("serviceExceptionHandler") private final ServiceExceptionHandler legacyHandler;
    @Qualifier("validatorUtils") private final ValidatorUtils validatorUtils;
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

    public SingleResponse<CourseDTO> createCourse(CreateCourseRequest request) {
        if (request == null) {
            return legacyHandler.handleSingle(new BizException("create course request must not be null"));
        }
        try { validatorUtils.validate(request); Course course = courseManage.create(request.name(), request.credit());
            return SingleResponse.of(legacyConverter.toDTO(course)); }
        catch (Exception failure) { return legacyHandler.handleSingle(failure); }
    }
    public SingleResponse<CourseDTO> getCourse(String courseId) {
        if (courseId == null || courseId.isBlank()) {
            return legacyHandler.handleSingle(new BizException("course id must not be blank"));
        }
        try { return SingleResponse.of(legacyConverter.toDTO(courseManage.getById(courseId))); }
        catch (Exception failure) { return legacyHandler.handleSingle(failure); }
    }
    public SingleResponse<PageResponse<CourseDTO>> getCourses(int currentPage, int pageSize) {
        try { return SingleResponse.of(legacyConverter.toPageResponse(courseManage.getPage(currentPage, pageSize))); }
        catch (Exception failure) { return legacyHandler.handleSingle(failure); }
    }
}
