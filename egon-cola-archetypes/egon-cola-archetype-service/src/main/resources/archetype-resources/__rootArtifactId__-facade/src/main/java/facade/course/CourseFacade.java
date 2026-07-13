#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.facade.course;

import ${package}.facade.dto.PageResponse;
import ${package}.facade.dto.SingleResponse;
import ${package}.facade.course.dto.CreateCourseRequest;
import ${package}.facade.course.dto.CourseResponse;
import ${package}.facade.course.dto.CourseScheduleResponse;
import ${package}.facade.course.dto.GetCourseRequest;
import ${package}.facade.course.dto.PageCourseRequest;
import ${package}.facade.course.dto.ScheduleCourseRequest;

public interface CourseFacade {

    SingleResponse<CourseResponse> create(CreateCourseRequest request);

    SingleResponse<CourseScheduleResponse> scheduleCourse(ScheduleCourseRequest request);

    SingleResponse<CourseResponse> getCourse(GetCourseRequest request);

    SingleResponse<PageResponse<CourseResponse>> pageCourses(PageCourseRequest request);
}
