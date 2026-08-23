#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.course.event;

import ${package}.domain.course.entities.CourseSchedule;

public interface CourseEventPublisher {

    void courseScheduled(CourseSchedule schedule);
}
