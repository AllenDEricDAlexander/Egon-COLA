package top.egon.cola.archetype.source.serviceopen.domain.course.service;

import top.egon.cola.archetype.source.serviceopen.domain.course.entities.CourseSchedule;

public interface CourseEventService {

    void courseScheduled(CourseSchedule schedule);
}
