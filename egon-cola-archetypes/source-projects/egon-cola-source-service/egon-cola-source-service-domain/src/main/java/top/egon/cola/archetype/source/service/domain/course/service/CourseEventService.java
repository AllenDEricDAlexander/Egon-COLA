package top.egon.cola.archetype.source.service.domain.course.service;

import top.egon.cola.archetype.source.service.domain.course.entities.CourseSchedule;

public interface CourseEventService {

    void courseScheduled(CourseSchedule schedule);
}
