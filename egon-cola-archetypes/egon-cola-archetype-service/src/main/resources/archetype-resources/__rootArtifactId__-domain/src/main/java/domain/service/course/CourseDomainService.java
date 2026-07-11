#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.service.course;

import ${package}.domain.entities.course.Course;
import ${package}.domain.entities.course.CourseSchedule;
import ${package}.domain.vos.course.CourseCode;
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
