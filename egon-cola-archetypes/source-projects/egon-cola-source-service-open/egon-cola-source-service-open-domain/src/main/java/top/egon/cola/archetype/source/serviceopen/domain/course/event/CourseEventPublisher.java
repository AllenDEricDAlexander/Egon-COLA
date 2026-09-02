package top.egon.cola.archetype.source.serviceopen.domain.course.event;

import top.egon.cola.archetype.source.serviceopen.domain.course.entities.CourseSchedule;

public interface CourseEventPublisher {

    void courseScheduled(CourseSchedule schedule);
}
