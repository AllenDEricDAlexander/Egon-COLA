#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.repo.course.converter;

import ${package}.domain.course.entities.CourseSchedule;
import ${package}.domain.course.enums.CourseScheduleStatus;
import ${package}.domain.course.vos.CourseId;
import ${package}.infrastructure.repo.course.po.CourseSchedulePo;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class CourseScheduleConverter {
    public CourseSchedulePo toPo(CourseSchedule schedule) {
        Instant now = Instant.now();
        return new CourseSchedulePo(
                schedule.getId(), schedule.getCourseId().value(), schedule.getClassId(),
                schedule.getStartsAt(), schedule.getEndsAt(), schedule.getStatus().name(), now, now);
    }

    public CourseSchedule toDomain(CourseSchedulePo po) {
        return new CourseSchedule(
                po.getId(), new CourseId(po.getCourseId()), po.getClassId(),
                po.getStartsAt(), po.getEndsAt(), CourseScheduleStatus.valueOf(po.getStatus()));
    }
}
