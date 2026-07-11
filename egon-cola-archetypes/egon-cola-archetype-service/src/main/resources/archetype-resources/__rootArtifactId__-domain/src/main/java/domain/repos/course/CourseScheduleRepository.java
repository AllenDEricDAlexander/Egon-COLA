#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.repos.course;

import ${package}.domain.entities.course.CourseSchedule;
import ${package}.domain.vos.course.CourseId;
import java.time.Instant;
import java.util.List;

public interface CourseScheduleRepository {

    CourseSchedule save(CourseSchedule schedule);

    List<CourseSchedule> findOverlapping(
            CourseId courseId,
            String classId,
            Instant startsAt,
            Instant endsAt);
}
