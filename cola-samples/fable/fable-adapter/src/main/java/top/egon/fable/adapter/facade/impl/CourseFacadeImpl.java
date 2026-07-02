package top.egon.fable.adapter.facade.impl;

import top.egon.fable.adapter.convertor.CourseAdapterConvertor;
import top.egon.fable.adapter.handler.ServiceExceptionHandler;
import top.egon.fable.application.manage.course.CourseManage;
import top.egon.fable.application.view.course.CourseView;
import top.egon.fable.common.exception.BizException;
import top.egon.fable.facade.api.CourseFacade;
import top.egon.fable.facade.dto.SingleResponse;
import top.egon.fable.facade.dto.course.CourseDTO;
import top.egon.fable.facade.dto.course.CreateCourseRequest;
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
