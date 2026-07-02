#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.facade.api;

import ${package}.facade.dto.SingleResponse;
import ${package}.facade.dto.course.CourseDTO;
import ${package}.facade.dto.course.CreateCourseRequest;

public interface CourseFacade {

    SingleResponse<CourseDTO> createCourse(CreateCourseRequest request);

    SingleResponse<CourseDTO> getCourse(String courseId);
}
