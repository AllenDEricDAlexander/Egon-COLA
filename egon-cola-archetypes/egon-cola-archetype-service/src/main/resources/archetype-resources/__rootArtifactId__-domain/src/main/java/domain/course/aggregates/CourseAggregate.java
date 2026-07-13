#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.course.aggregates;

import ${package}.domain.course.entities.Course;
import ${package}.domain.course.entities.CourseSchedule;
import java.util.List;

public record CourseAggregate(Course course, List<CourseSchedule> schedules) {

    public CourseAggregate {
        schedules = List.copyOf(schedules);
    }
}
