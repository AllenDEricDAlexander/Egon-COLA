#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.event.course;

import ${package}.domain.entities.course.CourseSchedule;

public interface CourseEventPublisher {

    void courseScheduled(CourseSchedule schedule);
}
