#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.application.converter.course;

import ${package}.application.result.course.CourseResult;
import ${package}.application.result.course.CourseScheduleResult;
import ${package}.domain.course.entities.Course;
import ${package}.domain.course.entities.CourseSchedule;
import org.springframework.stereotype.Component;

@Component
public class CourseApplicationConverter {
    public CourseResult toResult(Course course) {
        return new CourseResult(
                course.getId(),
                course.getCode() == null ? null : course.getCode().value(),
                course.getName(), course.getCredit(), course.getStatus().name());
    }

    public CourseScheduleResult toResult(CourseSchedule schedule) {
        return new CourseScheduleResult(
                schedule.getId(), schedule.getCourseId().value(), schedule.getClassId(),
                schedule.getStartsAt(), schedule.getEndsAt(), schedule.getStatus().name());
    }
}
