package top.egon.cola.evaluation.facade.course;

import top.egon.cola.evaluation.facade.dto.PageResponse;
import top.egon.cola.evaluation.facade.dto.SingleResponse;
import top.egon.cola.evaluation.facade.course.dto.CreateCourseRequest;
import top.egon.cola.evaluation.facade.course.dto.CourseResponse;
import top.egon.cola.evaluation.facade.course.dto.CourseScheduleResponse;
import top.egon.cola.evaluation.facade.course.dto.GetCourseRequest;
import top.egon.cola.evaluation.facade.course.dto.PageCourseRequest;
import top.egon.cola.evaluation.facade.course.dto.ScheduleCourseRequest;

public interface CourseFacade {

    SingleResponse<CourseResponse> create(CreateCourseRequest request);

    SingleResponse<CourseScheduleResponse> scheduleCourse(ScheduleCourseRequest request);

    SingleResponse<CourseResponse> getCourse(GetCourseRequest request);

    SingleResponse<PageResponse<CourseResponse>> pageCourses(PageCourseRequest request);
}
