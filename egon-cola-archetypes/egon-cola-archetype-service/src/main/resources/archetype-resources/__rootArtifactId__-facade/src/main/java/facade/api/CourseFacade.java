#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.facade.api;

import ${package}.facade.dto.PageResponse;
import ${package}.facade.dto.SingleResponse;
import ${package}.facade.dto.course.CreateCourseRequest;
import ${package}.facade.dto.course.CourseResponse;
import ${package}.facade.dto.course.CourseScheduleResponse;
import ${package}.facade.dto.course.GetCourseRequest;
import ${package}.facade.dto.course.PageCourseRequest;
import ${package}.facade.dto.course.ScheduleCourseRequest;

public interface CourseFacade {

    SingleResponse<CourseResponse> create(CreateCourseRequest request);

    SingleResponse<CourseScheduleResponse> scheduleCourse(ScheduleCourseRequest request);

    SingleResponse<CourseResponse> getCourse(GetCourseRequest request);

    SingleResponse<PageResponse<CourseResponse>> pageCourses(PageCourseRequest request);
}
