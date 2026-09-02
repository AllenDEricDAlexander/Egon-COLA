package top.egon.cola.archetype.source.serviceopen.domain.course.aggregates;

import top.egon.cola.archetype.source.serviceopen.domain.course.entities.Course;
import top.egon.cola.archetype.source.serviceopen.domain.course.entities.CourseSchedule;
import java.util.List;

public record CourseAggregate(Course course, List<CourseSchedule> schedules) {

    public CourseAggregate {
        schedules = List.copyOf(schedules);
    }
}
