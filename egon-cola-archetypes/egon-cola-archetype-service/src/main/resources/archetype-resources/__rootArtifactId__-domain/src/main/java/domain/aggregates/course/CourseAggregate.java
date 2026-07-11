#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.aggregates.course;

import ${package}.domain.entities.course.Course;
import ${package}.domain.entities.course.CourseSchedule;
import java.util.List;

public record CourseAggregate(Course course, List<CourseSchedule> schedules) {

    public CourseAggregate {
        schedules = List.copyOf(schedules);
    }
}
