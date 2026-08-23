#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.course.service;

import ${package}.domain.course.entities.Course;
import ${package}.domain.course.entities.CourseSchedule;
import ${package}.domain.course.vos.CourseCode;
import java.time.Instant;
import java.util.List;

public interface CourseDomainService {

    Course createCourse(String id, CourseCode code, String name, int credit);

    CourseSchedule scheduleCourse(
            String id,
            Course course,
            String classId,
            Instant startsAt,
            Instant endsAt,
            List<CourseSchedule> overlaps);
}
