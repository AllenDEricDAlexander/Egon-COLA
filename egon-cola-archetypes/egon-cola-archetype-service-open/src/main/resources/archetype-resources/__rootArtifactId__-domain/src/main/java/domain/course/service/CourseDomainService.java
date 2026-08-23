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

    Course createCourse(long id, CourseCode code, String name, int credit);

    CourseSchedule scheduleCourse(
            long id,
            Course course,
            long classId,
            Instant startsAt,
            Instant endsAt,
            List<CourseSchedule> overlaps);
}
