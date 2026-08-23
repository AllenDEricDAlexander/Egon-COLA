#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.course.repo.jpa;

import ${package}.infrastructure.course.repo.po.CourseSchedulePo;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseScheduleJpaRepository extends JpaRepository<CourseSchedulePo, String> {
    Optional<CourseSchedulePo> findByCourseIdAndId(String courseId, String id);

    List<CourseSchedulePo> findByCourseIdAndClassIdAndStartsAtLessThanAndEndsAtGreaterThan(
            String courseId, String classId, Instant endsAt, Instant startsAt);
}
