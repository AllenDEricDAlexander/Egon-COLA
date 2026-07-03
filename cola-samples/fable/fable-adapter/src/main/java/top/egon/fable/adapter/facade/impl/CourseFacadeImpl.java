package top.egon.fable.adapter.facade.impl;

import top.egon.fable.adapter.convertor.CourseAdapterConvertor;
import top.egon.fable.adapter.handler.ServiceExceptionHandler;
import top.egon.fable.application.manage.course.CourseManage;
import top.egon.fable.common.exception.BizException;
import top.egon.fable.domain.entities.course.Course;
import top.egon.fable.facade.api.CourseFacade;
import top.egon.fable.facade.dto.PageResponse;
import top.egon.fable.facade.dto.SingleResponse;
import top.egon.fable.facade.dto.course.CourseDTO;
import top.egon.fable.facade.dto.course.CreateCourseRequest;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Qualifier;

@DubboService(
        interfaceClass = CourseFacade.class,
        version = "1.0.0",
        group = "course"
)
@RequiredArgsConstructor
public class CourseFacadeImpl implements CourseFacade {

    @Qualifier("courseManage")
    private final CourseManage courseManage;

    @Qualifier("courseAdapterConvertor")
    private final CourseAdapterConvertor courseAdapterConvertor;

    @Qualifier("serviceExceptionHandler")
    private final ServiceExceptionHandler serviceExceptionHandler;

    @Override
    public SingleResponse<CourseDTO> createCourse(CreateCourseRequest request) {
        if (request == null) {
            return serviceExceptionHandler.handleSingle(new BizException("create course request must not be null"));
        }
        try {
            Course course = courseManage.create(request.name(), request.credit());
            return SingleResponse.of(courseAdapterConvertor.toDTO(course));
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

    @Override
    public SingleResponse<PageResponse<CourseDTO>> getCourses(int currentPage, int pageSize) {
        try {
            return SingleResponse.of(courseAdapterConvertor.toPageResponse(courseManage.getPage(currentPage, pageSize)));
        } catch (BizException exception) {
            return serviceExceptionHandler.handleSingle(exception);
        } catch (Exception exception) {
            return serviceExceptionHandler.handleSingle(exception);
        }
    }
}
