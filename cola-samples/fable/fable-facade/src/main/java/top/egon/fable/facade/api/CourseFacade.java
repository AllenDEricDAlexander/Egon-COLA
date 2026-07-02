package top.egon.fable.facade.api;

import top.egon.fable.common.response.SingleResponse;
import top.egon.fable.facade.dto.course.CourseDTO;
import top.egon.fable.facade.dto.course.CreateCourseRequest;

public interface CourseFacade {

    SingleResponse<CourseDTO> createCourse(CreateCourseRequest request);

    SingleResponse<CourseDTO> getCourse(String courseId);
}
