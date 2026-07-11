#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.repo.course.jpa;

import ${package}.infrastructure.repo.course.po.CourseSchedulePo;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseScheduleJpaRepository extends JpaRepository<CourseSchedulePo, String> {
    List<CourseSchedulePo> findByCourseIdAndClassIdAndStartsAtLessThanAndEndsAtGreaterThan(
            String courseId, String classId, Instant endsAt, Instant startsAt);
}
