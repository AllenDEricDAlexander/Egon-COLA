#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.adapter.facade.impl;

import ${package}.adapter.convertor.CourseAdapterConvertor;
import ${package}.adapter.handler.ServiceExceptionHandler;
import ${package}.application.manage.course.CourseManage;
import ${package}.application.view.course.CourseView;
import ${package}.common.exception.BizException;
import ${package}.common.response.SingleResponse;
import ${package}.facade.api.CourseFacade;
import ${package}.facade.dto.course.CourseDTO;
import ${package}.facade.dto.course.CreateCourseRequest;
import org.springframework.stereotype.Component;

@Component
public class CourseFacadeImpl implements CourseFacade {

    private final CourseManage courseManage;

    private final CourseAdapterConvertor courseAdapterConvertor;

    private final ServiceExceptionHandler serviceExceptionHandler;

    public CourseFacadeImpl(CourseManage courseManage, CourseAdapterConvertor courseAdapterConvertor,
            ServiceExceptionHandler serviceExceptionHandler) {
        this.courseManage = courseManage;
        this.courseAdapterConvertor = courseAdapterConvertor;
        this.serviceExceptionHandler = serviceExceptionHandler;
    }

    @Override
    public SingleResponse<CourseDTO> createCourse(CreateCourseRequest request) {
        if (request == null) {
            return serviceExceptionHandler.handleSingle(new BizException("create course request must not be null"));
        }
        try {
            CourseView courseView = courseManage.create(request.name(), request.credit());
            return SingleResponse.of(courseAdapterConvertor.toDTO(courseView));
        } catch (BizException exception) {
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
        } catch (Exception exception) {
            return serviceExceptionHandler.handleSingle(exception);
        }
    }
}
