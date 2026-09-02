package top.egon.cola.archetype.source.service.domain.course.event;

import top.egon.cola.archetype.source.service.domain.course.entities.CourseSchedule;

public interface CourseEventPublisher {

    void courseScheduled(CourseSchedule schedule);
}
