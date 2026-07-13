#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.course.repos;

import ${package}.domain.course.entities.CourseSchedule;
import ${package}.domain.course.vos.CourseId;
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
