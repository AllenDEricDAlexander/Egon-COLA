#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.facade.impl;

import ${package}.adapter.convertor.CourseAdapterConvertor;
import ${package}.adapter.handler.ServiceExceptionHandler;
import ${package}.adapter.validation.ValidatorUtils;
import ${package}.application.manage.course.CourseManage;
import ${package}.common.exception.BizException;
import ${package}.domain.entities.course.Course;
import ${package}.facade.api.CourseFacade;
import ${package}.facade.dto.PageResponse;
import ${package}.facade.dto.SingleResponse;
import ${package}.facade.dto.course.CourseDTO;
import ${package}.facade.dto.course.CreateCourseRequest;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;

@DubboService(
        interfaceClass = CourseFacade.class,
        version = "1.0.0",
        group = "course"
)
@Validated
@RequiredArgsConstructor
public class CourseFacadeImpl implements CourseFacade {

    @Qualifier("courseManage")
    private final CourseManage courseManage;

    @Qualifier("courseAdapterConvertor")
    private final CourseAdapterConvertor courseAdapterConvertor;

    @Qualifier("serviceExceptionHandler")
    private final ServiceExceptionHandler serviceExceptionHandler;

    @Qualifier("validatorUtils")
    private final ValidatorUtils validatorUtils;

    @Override
    public SingleResponse<CourseDTO> createCourse(CreateCourseRequest request) {
        if (request == null) {
            return serviceExceptionHandler.handleSingle(new BizException("create course request must not be null"));
        }
        try {
            validatorUtils.validate(request);
            Course course = courseManage.create(request.name(), request.credit());
            return SingleResponse.of(courseAdapterConvertor.toDTO(course));
        } catch (BizException exception) {
            return serviceExceptionHandler.handleSingle(exception);
        } catch (ValidationException exception) {
            return serviceExceptionHandler.handleSingle(exception);
        } catch (Exception exception) {
            return serviceExceptionHandler.handleSingle(exception);
        }
    }

    @Override
    public SingleResponse<CourseDTO> getCourse(String courseId) {
        if (courseId == null || courseId.isBlank()) {
            return serviceExceptionHandler.handleSingle(new BizException("course id must not be blank"));
        }
        try {
            return SingleResponse.of(courseAdapterConvertor.toDTO(courseManage.getById(courseId)));
        } catch (BizException exception) {
            return serviceExceptionHandler.handleSingle(exception);
        } catch (ValidationException exception) {
            return serviceExceptionHandler.handleSingle(exception);
        } catch (Exception exception) {
            return serviceExceptionHandler.handleSingle(exception);
        }
    }

    @Override
    public SingleResponse<PageResponse<CourseDTO>> getCourses(int currentPage, int pageSize) {
        try {
            return SingleResponse.of(courseAdapterConvertor.toPageResponse(courseManage.getPage(currentPage, pageSize)));
        } catch (BizException exception) {
            return serviceExceptionHandler.handleSingle(exception);
        } catch (ValidationException exception) {
            return serviceExceptionHandler.handleSingle(exception);
        } catch (Exception exception) {
            return serviceExceptionHandler.handleSingle(exception);
        }
    }
}
