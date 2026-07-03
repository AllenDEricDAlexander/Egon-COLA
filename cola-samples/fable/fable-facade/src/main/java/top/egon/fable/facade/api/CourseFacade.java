package top.egon.fable.facade.api;

import top.egon.fable.facade.dto.PageResponse;
import top.egon.fable.facade.dto.SingleResponse;
import top.egon.fable.facade.dto.course.CourseDTO;
import top.egon.fable.facade.dto.course.CreateCourseRequest;

public interface CourseFacade {

    SingleResponse<CourseDTO> createCourse(CreateCourseRequest request);

    SingleResponse<CourseDTO> getCourse(String courseId);

    SingleResponse<PageResponse<CourseDTO>> getCourses(int currentPage, int pageSize);
}
